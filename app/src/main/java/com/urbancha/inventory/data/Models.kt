package com.urbancha.inventory.data

/** Something on the menu. [stockItemId], when set, is a raw material reduced by 1 for each one sold (e.g. Tea → Cups). */
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val stockItemId: String? = null,
)

/** A raw material such as Milk or Cups. [qty] is the live stock level, shared by every device. */
data class Material(
    val id: String,
    val name: String,
    val unit: String,
    val qty: Double,
    val lowAlert: Double = 0.0,
) {
    val isLow: Boolean get() = lowAlert > 0 && qty <= lowAlert
}

data class ItemTotal(val name: String, val qty: Long, val amount: Double)

data class ClosingLine(val counted: Double, val expected: Double, val used: Double)

/** Everything about one 3 PM – 3 AM shift. Totals are kept up to date by atomic increments from every device. */
data class Shift(
    val id: String,
    val status: String = STATUS_NOT_STARTED,
    val opening: Map<String, Double> = emptyMap(),
    val restocked: Map<String, Double> = emptyMap(),
    val closing: Map<String, ClosingLine> = emptyMap(),
    val salesTotal: Double = 0.0,
    val cashTotal: Double = 0.0,
    val upiTotal: Double = 0.0,
    val billCount: Long = 0,
    val itemCount: Long = 0,
    val items: Map<String, ItemTotal> = emptyMap(),
) {
    companion object {
        const val STATUS_NOT_STARTED = "not_started"
        const val STATUS_OPEN = "open"
        const val STATUS_CLOSED = "closed"
    }
}

data class Restock(
    val id: String,
    val materialId: String,
    val name: String,
    val qty: Double,
    val unit: String,
    val atMillis: Long,
)

data class Bill(
    val id: String,
    val total: Double,
    val payment: String,
    val itemCount: Long,
    val summary: String,
    val atMillis: Long,
)

val UNITS = listOf("g", "kg", "ml", "L", "pcs")

fun fmtQty(v: Double): String {
    val r = Math.round(v * 100) / 100.0
    return if (r == Math.floor(r)) r.toLong().toString() else r.toString()
}

fun fmtRupees(v: Double): String {
    val whole = Math.round(v)
    val s = java.text.NumberFormat.getIntegerInstance(java.util.Locale("en", "IN")).format(whole)
    return "₹$s"
}
