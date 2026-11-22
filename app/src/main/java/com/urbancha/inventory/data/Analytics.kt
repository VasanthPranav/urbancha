package com.urbancha.inventory.data

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/*
 * Analytics over a run of shifts. Pure Kotlin so it can be unit-tested.
 * Must give the same numbers as the iOS app (ios/Core/Sources/UrbanChaCore/Analytics.swift),
 * since both read the same Firestore data.
 */

data class DaySales(val shiftId: String, val sales: Double)

data class ProductStat(
    val id: String,
    val name: String,
    val units: Long,
    val revenue: Double,
    /** revenue / period sales */
    val share: Double,
    /** units / all units sold */
    val unitShare: Double,
)

data class MaterialStat(
    val id: String,
    val name: String,
    val unit: String,
    /** Σ closing[id].used over closed shifts */
    val used: Double,
    /** used × cost per unit; null when the material has no cost */
    val cost: Double?,
    /** cost / closedSales */
    val shareOfSales: Double?,
    /** current stock / average use per closed shift; null with no usage */
    val daysLeft: Double?,
    /** Linked from a product (cups, buns): deducted per sale, so a count gap is missing stock. */
    val linked: Boolean,
    /** Σ max(0, expected − counted), linked materials only */
    val shortfall: Double,
    /** shortfall / used */
    val shortfallShare: Double?,
    /** shortfall × cost per unit */
    val shortfallCost: Double?,
)

data class AnalyticsReport(
    val shiftIds: List<String>,
    val sales: Double,
    val cash: Double,
    val upi: Double,
    val bills: Long,
    val items: Long,
    val days: List<DaySales>,
    val shiftsWithSales: Int,
    val averageBill: Double,
    val averagePerShift: Double,
    val best: DaySales?,
    val weakest: DaySales?,
    val products: List<ProductStat>,
    val closedShiftCount: Int,
    val closedSales: Double,
    /** Materials with used > 0, sorted by cost descending (no cost last). */
    val materials: List<MaterialStat>,
    val materialCost: Double,
    val margin: Double,
    val materialCostShare: Double?,
    val materialsWithoutCost: List<String>,
    /** Linked materials with shortfall > 0. */
    val missing: List<MaterialStat>,
) {
    val unitsTotal: Long get() = products.sumOf { it.units }
}

object Analytics {

    /** The last [n] shift ids ending at [lastId], oldest first. */
    fun periodIds(lastId: String, n: Int): List<String> {
        val ids = ArrayList<String>(n)
        var id = lastId
        repeat(n) { ids.add(id); id = ShiftClock.previous(id) }
        return ids.reversed()
    }

    private fun ratio(a: Double, b: Double): Double? = if (b > 0) a / b else null

    /** (current − previous) / previous; null when there is nothing to compare with. */
    fun change(current: Double, previous: Double): Double? =
        if (previous > 0) (current - previous) / previous else null

    /**
     * [shiftIds] are the period's shift ids, oldest first. [shifts] can hold any shifts; ids with no
     * document count as zero sales. [materials] gives current stock and cost per unit, [products]
     * tells which materials are linked (deducted per sale).
     */
    fun report(
        shiftIds: List<String>,
        shifts: List<Shift>,
        materials: List<Material>,
        products: List<Product>,
    ): AnalyticsReport {
        val byId = shifts.associateBy { it.id }
        val inPeriod = shiftIds.mapNotNull { byId[it] }

        val sales = inPeriod.sumOf { it.salesTotal }
        val bills = inPeriod.sumOf { it.billCount }
        val days = shiftIds.map { DaySales(it, byId[it]?.salesTotal ?: 0.0) }
        val withSales = days.filter { it.sales > 0 }

        // Products, merged by id across shifts. The newest name wins.
        data class Acc(var name: String, var units: Long, var revenue: Double)
        val acc = LinkedHashMap<String, Acc>()
        inPeriod.forEach { s ->
            s.items.forEach { (pid, it) ->
                val a = acc.getOrPut(pid) { Acc(it.name, 0, 0.0) }
                if (it.name.isNotEmpty()) a.name = it.name
                a.units += it.qty
                a.revenue += it.amount
            }
        }
        val allUnits = acc.values.sumOf { it.units }.toDouble()
        val productStats = acc.map { (id, a) ->
            ProductStat(id, a.name, a.units, a.revenue, ratio(a.revenue, sales) ?: 0.0, ratio(a.units.toDouble(), allUnits) ?: 0.0)
        }.sortedWith(compareByDescending<ProductStat> { it.revenue }.thenBy { it.name })

        // Raw materials: closed shifts only, since usage comes from the Close-shift count.
        val closed = inPeriod.filter { it.status == Shift.STATUS_CLOSED }
        val closedSales = closed.sumOf { it.salesTotal }
        val linkedIds = products.mapNotNull { it.stockItemId }.toSet()

        val stats = materials.map { m ->
            val used = closed.sumOf { it.closing[m.id]?.used ?: 0.0 }
            val cost = if (m.cost > 0) used * m.cost else null
            val perShift = if (closed.isNotEmpty()) used / closed.size else 0.0
            val linked = m.id in linkedIds
            val shortfall = if (linked) closed.sumOf { s -> s.closing[m.id]?.let { max(0.0, it.expected - it.counted) } ?: 0.0 } else 0.0
            MaterialStat(
                id = m.id,
                name = m.name,
                unit = m.unit,
                used = used,
                cost = cost,
                shareOfSales = cost?.let { ratio(it, closedSales) },
                daysLeft = if (perShift > 0) max(0.0, m.qty) / perShift else null,
                linked = linked,
                shortfall = shortfall,
                shortfallShare = ratio(shortfall, used),
                shortfallCost = if (m.cost > 0) shortfall * m.cost else null,
            )
        }
        val usedStats = stats.filter { it.used > 0 }
        val materialCost = stats.sumOf { it.cost ?: 0.0 }

        return AnalyticsReport(
            shiftIds = shiftIds,
            sales = sales,
            cash = inPeriod.sumOf { it.cashTotal },
            upi = inPeriod.sumOf { it.upiTotal },
            bills = bills,
            items = inPeriod.sumOf { it.itemCount },
            days = days,
            shiftsWithSales = withSales.size,
            averageBill = ratio(sales, bills.toDouble()) ?: 0.0,
            averagePerShift = ratio(withSales.sumOf { it.sales }, withSales.size.toDouble()) ?: 0.0,
            best = withSales.maxByOrNull { it.sales },
            weakest = withSales.minByOrNull { it.sales },
            products = productStats,
            closedShiftCount = closed.size,
            closedSales = closedSales,
            materials = usedStats.sortedWith(compareByDescending<MaterialStat> { it.cost ?: -1.0 }.thenBy { it.name }),
            materialCost = materialCost,
            margin = closedSales - materialCost,
            materialCostShare = ratio(materialCost, closedSales),
            materialsWithoutCost = usedStats.filter { it.cost == null }.map { it.name },
            missing = stats.filter { it.linked && it.shortfall > 0 },
        )
    }
}

/** "32%", or one decimal under 10%: "4.2%". [x] is a ratio (0.32 = 32%). */
fun fmtPercent(x: Double): String {
    val p = x * 100
    return if (abs(p) < 9.95) String.format(Locale.ENGLISH, "%.1f%%", p)
    else String.format(Locale.ENGLISH, "%.0f%%", p)
}

/** "+12%" or "−5.0%" (true minus sign, U+2212). */
fun fmtSignedPercent(x: Double): String {
    val body = fmtPercent(abs(x))
    return if (x < 0) "−$body" else "+$body"
}
