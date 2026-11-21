package com.urbancha.inventory.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * All shop data, stored in Cloud Firestore under shops/{shopId}.
 *
 * Firestore keeps a local copy on each phone, so the app works offline and every
 * change syncs to the other phones as soon as there is internet. Stock and sales totals
 * are changed with FieldValue.increment, so two phones billing at the same moment
 * never overwrite each other.
 *
 * Writes are not awaited on purpose: with no internet the write lands in the local cache
 * at once, shows on screen, and is sent to the server later.
 */
class ShopRepository(private val db: FirebaseFirestore, shopId: String) {

    private val shop = db.collection("shops").document(shopId)
    private val products = shop.collection("products")
    private val materials = shop.collection("materials")
    private val shifts = shop.collection("shifts")

    // ---------- Live reads ----------

    fun products(): Flow<List<Product>> = callbackFlow {
        val reg = products.orderBy("name").addSnapshotListener { snap, _ ->
            if (snap != null) trySend(snap.documents.map { it.toProduct() })
        }
        awaitClose { reg.remove() }
    }

    fun materials(): Flow<List<Material>> = callbackFlow {
        val reg = materials.orderBy("name").addSnapshotListener { snap, _ ->
            if (snap != null) trySend(snap.documents.map { it.toMaterial() })
        }
        awaitClose { reg.remove() }
    }

    fun shift(shiftId: String): Flow<Shift> = callbackFlow {
        val reg = shifts.document(shiftId).addSnapshotListener { snap, _ ->
            trySend(if (snap != null && snap.exists()) snap.toShift() else Shift(shiftId))
        }
        awaitClose { reg.remove() }
    }

    fun restocks(shiftId: String): Flow<List<Restock>> = callbackFlow {
        val reg = shifts.document(shiftId).collection("restocks")
            .orderBy("at", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) trySend(snap.documents.map {
                    Restock(
                        id = it.id,
                        materialId = it.getString("materialId") ?: "",
                        name = it.getString("name") ?: "",
                        qty = it.num("qty"),
                        unit = it.getString("unit") ?: "",
                        atMillis = it.getLong("at") ?: 0L,
                    )
                })
            }
        awaitClose { reg.remove() }
    }

    fun bills(shiftId: String): Flow<List<Bill>> = callbackFlow {
        val reg = shifts.document(shiftId).collection("sales")
            .orderBy("at", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snap, _ ->
                if (snap != null) trySend(snap.documents.map { d ->
                    @Suppress("UNCHECKED_CAST")
                    val lines = (d.get("items") as? List<Map<String, Any?>>).orEmpty()
                    Bill(
                        id = d.id,
                        total = d.num("total"),
                        payment = d.getString("payment") ?: "",
                        itemCount = lines.sumOf { (it["qty"] as? Number)?.toLong() ?: 0L },
                        summary = lines.joinToString(", ") { "${(it["qty"] as? Number)?.toLong() ?: 0} × ${it["name"]}" },
                        atMillis = d.getLong("at") ?: 0L,
                    )
                })
            }
        awaitClose { reg.remove() }
    }

    // ---------- Sales ----------

    /** Saves one bill: adds to the shift's totals and takes linked stock (e.g. cups) off straight away. */
    fun recordSale(lines: Map<Product, Int>, payment: String) {
        val sold = lines.filterValues { it > 0 }
        if (sold.isEmpty()) return
        val shiftId = ShiftClock.currentShiftId()
        val shiftRef = shifts.document(shiftId)
        val total = sold.entries.sumOf { (p, q) -> p.price * q }
        val count = sold.values.sum().toLong()

        val batch = db.batch()
        batch.set(shiftRef.collection("sales").document(), mapOf(
            "items" to sold.map { (p, q) -> mapOf("productId" to p.id, "name" to p.name, "price" to p.price, "qty" to q) },
            "total" to total,
            "payment" to payment,
            "at" to System.currentTimeMillis(),
        ))
        batch.set(shiftRef, mapOf(
            "salesTotal" to FieldValue.increment(total),
            (if (payment == "UPI") "upiTotal" else "cashTotal") to FieldValue.increment(total),
            "billCount" to FieldValue.increment(1),
            "itemCount" to FieldValue.increment(count),
            "items" to sold.entries.associate { (p, q) ->
                p.id to mapOf(
                    "name" to p.name,
                    "qty" to FieldValue.increment(q.toLong()),
                    "amount" to FieldValue.increment(p.price * q),
                )
            },
        ), SetOptions.merge())

        // 1 sold = 1 unit of the linked stock item (e.g. every tea uses one cup).
        sold.entries.filter { it.key.stockItemId != null }
            .groupBy({ it.key.stockItemId!! }, { it.value })
            .forEach { (matId, qtys) ->
                batch.set(materials.document(matId), mapOf("qty" to FieldValue.increment(-qtys.sum().toDouble())), SetOptions.merge())
            }
        batch.commit()
    }

    // ---------- Stock ----------

    /** Opening stock at 3 PM: adds what arrived before opening and records the opening level of every item. */
    fun startShift(all: List<Material>, received: Map<String, Double>) {
        val shiftId = ShiftClock.currentShiftId()
        val batch = db.batch()
        received.filterValues { it > 0 }.forEach { (id, q) ->
            batch.set(materials.document(id), mapOf("qty" to FieldValue.increment(q)), SetOptions.merge())
        }
        batch.set(shifts.document(shiftId), mapOf(
            "status" to Shift.STATUS_OPEN,
            "openedAt" to System.currentTimeMillis(),
            "opening" to all.associate { it.id to it.qty + (received[it.id] ?: 0.0) },
        ), SetOptions.merge())
        batch.commit()
    }

    /** Stock that arrives during the shift. */
    fun restock(material: Material, qty: Double) {
        if (qty <= 0) return
        val shiftRef = shifts.document(ShiftClock.currentShiftId())
        val batch = db.batch()
        batch.set(materials.document(material.id), mapOf("qty" to FieldValue.increment(qty)), SetOptions.merge())
        batch.set(shiftRef.collection("restocks").document(), mapOf(
            "materialId" to material.id,
            "name" to material.name,
            "qty" to qty,
            "unit" to material.unit,
            "at" to System.currentTimeMillis(),
        ))
        batch.set(shiftRef, mapOf("restocked" to mapOf(material.id to FieldValue.increment(qty))), SetOptions.merge())
        batch.commit()
    }

    /** End of shift: saves the physical count, sets stock to what was counted, and closes the shift. */
    fun closeShift(shift: Shift, all: List<Material>, counted: Map<String, Double>) {
        val batch = db.batch()
        val closing = all.associate { m ->
            val c = counted[m.id] ?: m.qty
            val inTotal = (shift.opening[m.id] ?: m.qty) + (shift.restocked[m.id] ?: 0.0)
            m.id to mapOf("counted" to c, "expected" to m.qty, "used" to (inTotal - c))
        }
        all.forEach { m ->
            val c = counted[m.id] ?: m.qty
            batch.set(materials.document(m.id), mapOf("qty" to c), SetOptions.merge())
        }
        batch.set(shifts.document(shift.id), mapOf(
            "status" to Shift.STATUS_CLOSED,
            "closedAt" to System.currentTimeMillis(),
            "closing" to closing,
        ), SetOptions.merge())
        batch.commit()
    }

    fun saveMaterial(id: String?, name: String, unit: String, qty: Double?, lowAlert: Double) {
        val ref = if (id == null) materials.document() else materials.document(id)
        val data = mutableMapOf<String, Any>("name" to name, "unit" to unit, "lowAlert" to lowAlert)
        if (qty != null) data["qty"] = qty
        ref.set(data, SetOptions.merge())
    }

    fun deleteMaterial(id: String) {
        materials.document(id).delete()
    }

    // ---------- Products ----------

    fun saveProduct(id: String?, name: String, price: Double, stockItemId: String?) {
        val ref = if (id == null) products.document() else products.document(id)
        ref.set(mapOf("name" to name, "price" to price, "stockItemId" to stockItemId))
    }

    fun deleteProduct(id: String) {
        products.document(id).delete()
    }

    /** Fills an empty shop with Urban Cha's usual items. Prices and quantities are placeholders to edit. */
    fun loadStarterData() {
        val batch = db.batch()
        val mats = listOf(
            Triple("Milk", "L", 5.0), Triple("Sugar", "kg", 1.0), Triple("Tea powder", "g", 200.0),
            Triple("Cups", "pcs", 100.0), Triple("Batter", "kg", 1.0), Triple("Buns", "pcs", 10.0),
            Triple("Butter", "g", 100.0), Triple("Nutella", "g", 200.0), Triple("Milkmaid", "g", 200.0),
        )
        val ids = mats.associate { (name, unit, alert) ->
            val ref = materials.document()
            batch.set(ref, mapOf("name" to name, "unit" to unit, "qty" to 0.0, "lowAlert" to alert))
            name to ref.id
        }
        listOf(
            Triple("Tea", 15.0, "Cups"), Triple("Ginger tea", 20.0, "Cups"),
            Triple("Butter bun", 30.0, "Buns"), Triple("Nutella bun", 50.0, "Buns"), Triple("Milkmaid bun", 45.0, "Buns"),
            Triple("Idli (2 pcs)", 40.0, null), Triple("Dosa", 50.0, null),
        ).forEach { (name, price, stock) ->
            batch.set(products.document(), mapOf("name" to name, "price" to price, "stockItemId" to stock?.let { ids[it] }))
        }
        batch.commit()
    }

    // ---------- Parsing ----------

    private fun DocumentSnapshot.num(field: String): Double = (get(field) as? Number)?.toDouble() ?: 0.0

    private fun DocumentSnapshot.toProduct() = Product(
        id = id,
        name = getString("name") ?: "",
        price = num("price"),
        stockItemId = getString("stockItemId"),
    )

    private fun DocumentSnapshot.toMaterial() = Material(
        id = id,
        name = getString("name") ?: "",
        unit = getString("unit") ?: "",
        qty = num("qty"),
        lowAlert = num("lowAlert"),
    )

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toShift(): Shift {
        fun numMap(field: String): Map<String, Double> =
            (get(field) as? Map<String, Any?>).orEmpty().mapValues { (it.value as? Number)?.toDouble() ?: 0.0 }
        val items = (get("items") as? Map<String, Map<String, Any?>>).orEmpty().mapValues { (_, v) ->
            ItemTotal(
                name = v["name"] as? String ?: "",
                qty = (v["qty"] as? Number)?.toLong() ?: 0L,
                amount = (v["amount"] as? Number)?.toDouble() ?: 0.0,
            )
        }
        val closing = (get("closing") as? Map<String, Map<String, Any?>>).orEmpty().mapValues { (_, v) ->
            ClosingLine(
                counted = (v["counted"] as? Number)?.toDouble() ?: 0.0,
                expected = (v["expected"] as? Number)?.toDouble() ?: 0.0,
                used = (v["used"] as? Number)?.toDouble() ?: 0.0,
            )
        }
        return Shift(
            id = id,
            status = getString("status") ?: Shift.STATUS_NOT_STARTED,
            opening = numMap("opening"),
            restocked = numMap("restocked"),
            closing = closing,
            salesTotal = num("salesTotal"),
            cashTotal = num("cashTotal"),
            upiTotal = num("upiTotal"),
            billCount = getLong("billCount") ?: 0L,
            itemCount = getLong("itemCount") ?: 0L,
            items = items,
        )
    }
}
