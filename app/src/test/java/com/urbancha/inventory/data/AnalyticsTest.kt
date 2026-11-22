package com.urbancha.inventory.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsTest {

    private val cups = Material("cups", "Cups", "pcs", qty = 40.0, cost = 2.0)
    private val milk = Material("milk", "Milk", "L", qty = 6.0, cost = 50.0)
    private val sugar = Material("sugar", "Sugar", "kg", qty = 3.0) // no cost
    private val tea = Product("tea", "Tea", 15.0, stockItemId = "cups")
    private val dosa = Product("dosa", "Dosa", 50.0)

    private fun shift(
        id: String, sales: Double, bills: Long = 0, items: Map<String, ItemTotal> = emptyMap(),
        closing: Map<String, ClosingLine> = emptyMap(), status: String = Shift.STATUS_CLOSED,
        cash: Double = sales, upi: Double = 0.0,
    ) = Shift(
        id = id, status = status, salesTotal = sales, cashTotal = cash, upiTotal = upi, billCount = bills,
        itemCount = items.values.sumOf { it.qty }, items = items, closing = closing,
    )

    @Test fun periodIdsAreOldestFirstAndEndAtTheGivenShift() {
        assertEquals(listOf("2026-11-19", "2026-11-20", "2026-11-21"), Analytics.periodIds("2026-11-21", 3))
        assertEquals(listOf("2026-02-28", "2026-03-01"), Analytics.periodIds("2026-03-01", 2))
    }

    @Test fun totalsAveragesAndBestWeakestSkipDaysWithoutSales() {
        val ids = Analytics.periodIds("2026-11-21", 3)
        val r = Analytics.report(
            ids,
            listOf(
                shift("2026-11-19", 1000.0, bills = 10, cash = 600.0, upi = 400.0),
                // 2026-11-20: no document at all → zero sales
                shift("2026-11-21", 3000.0, bills = 20, cash = 1000.0, upi = 2000.0),
                shift("2026-11-15", 9999.0, bills = 99), // outside the period, ignored
            ),
            emptyList(), emptyList(),
        )
        assertEquals(4000.0, r.sales, 0.0)
        assertEquals(1600.0, r.cash, 0.0)
        assertEquals(2400.0, r.upi, 0.0)
        assertEquals(30L, r.bills)
        assertEquals(3, r.days.size)
        assertEquals(0.0, r.days[1].sales, 0.0)
        assertEquals(2, r.shiftsWithSales)
        assertEquals(4000.0 / 30, r.averageBill, 1e-9)
        assertEquals(2000.0, r.averagePerShift, 0.0)
        assertEquals("2026-11-21", r.best?.shiftId)
        assertEquals("2026-11-19", r.weakest?.shiftId)
    }

    @Test fun emptyPeriodHasNoDivideByZero() {
        val r = Analytics.report(Analytics.periodIds("2026-11-21", 7), emptyList(), listOf(milk), listOf(tea))
        assertEquals(0.0, r.sales, 0.0)
        assertEquals(0.0, r.averageBill, 0.0)
        assertEquals(0.0, r.averagePerShift, 0.0)
        assertNull(r.best)
        assertNull(r.materialCostShare)
        assertEquals(0, r.closedShiftCount)
        assertEquals(emptyList<MaterialStat>(), r.materials)
    }

    @Test fun productsMergeAcrossShiftsAndSortByRevenueThenName() {
        val ids = listOf("2026-11-20", "2026-11-21")
        val r = Analytics.report(
            ids,
            listOf(
                shift("2026-11-20", 250.0, items = mapOf("tea" to ItemTotal("Tea", 10, 150.0), "dosa" to ItemTotal("Dosa", 2, 100.0))),
                shift("2026-11-21", 250.0, items = mapOf("tea" to ItemTotal("Tea", 0, 0.0), "dosa" to ItemTotal("Dosa", 1, 50.0), "bun" to ItemTotal("Bun", 2, 50.0))),
            ),
            emptyList(), emptyList(),
        )
        assertEquals(listOf("Dosa", "Tea", "Bun"), r.products.map { it.name })
        // Tea 150 vs Dosa 150: tie on revenue → by name.
        val teaStat = r.products.first { it.id == "tea" }
        assertEquals(10L, teaStat.units)
        assertEquals(150.0 / 500.0, teaStat.share, 1e-9)
        assertEquals(10.0 / 15.0, teaStat.unitShare, 1e-9)
        assertEquals(15L, r.unitsTotal)
    }

    @Test fun rawMaterialsUseClosedShiftsOnly() {
        val ids = listOf("2026-11-20", "2026-11-21")
        val r = Analytics.report(
            ids,
            listOf(
                shift("2026-11-20", 1000.0, closing = mapOf(
                    "cups" to ClosingLine(counted = 40.0, expected = 45.0, used = 60.0),
                    "milk" to ClosingLine(counted = 6.0, expected = 9.0, used = 4.0),
                    "sugar" to ClosingLine(counted = 3.0, expected = 3.5, used = 0.5),
                )),
                // Still open: its sales are in the totals but not in raw-material numbers.
                shift("2026-11-21", 500.0, status = Shift.STATUS_OPEN),
            ),
            listOf(cups, milk, sugar), listOf(tea, dosa),
        )
        assertEquals(1500.0, r.sales, 0.0)
        assertEquals(1, r.closedShiftCount)
        assertEquals(1000.0, r.closedSales, 0.0)

        val c = r.materials.first { it.id == "cups" }
        assertEquals(60.0, c.used, 0.0)
        assertEquals(120.0, c.cost!!, 1e-9)
        assertEquals(0.12, c.shareOfSales!!, 1e-9)
        assertEquals(40.0 / 60.0, c.daysLeft!!, 1e-9)

        val m = r.materials.first { it.id == "milk" }
        assertEquals(200.0, m.cost!!, 1e-9)
        assertEquals(6.0 / 4.0, m.daysLeft!!, 1e-9)

        // Sorted by cost: Milk ₹200, Cups ₹120, then Sugar with no cost.
        assertEquals(listOf("Milk", "Cups", "Sugar"), r.materials.map { it.name })
        assertEquals(320.0, r.materialCost, 1e-9)
        assertEquals(680.0, r.margin, 1e-9)
        assertEquals(0.32, r.materialCostShare!!, 1e-9)
        assertEquals(listOf("Sugar"), r.materialsWithoutCost)

        // Missing stock only for linked items: cups expected 45, counted 40 → 5 missing.
        // Milk also has a gap, but it's loose, so that's ordinary usage.
        assertEquals(listOf("cups"), r.missing.map { it.id })
        assertEquals(5.0, c.shortfall, 0.0)
        assertEquals(5.0 / 60.0, c.shortfallShare!!, 1e-9)
        assertEquals(10.0, c.shortfallCost!!, 1e-9)
        assertEquals(0.0, m.shortfall, 0.0)
    }

    @Test fun changeIsNullWithoutPreviousData() {
        assertEquals(0.5, Analytics.change(150.0, 100.0)!!, 1e-9)
        assertEquals(-0.25, Analytics.change(75.0, 100.0)!!, 1e-9)
        assertNull(Analytics.change(100.0, 0.0))
    }

    @Test fun percentFormatting() {
        assertEquals("32%", fmtPercent(0.32))
        assertEquals("4.2%", fmtPercent(0.042))
        assertEquals("10%", fmtPercent(0.0997))
        assertEquals("100%", fmtPercent(1.0))
        assertEquals("+12%", fmtSignedPercent(0.12))
        assertEquals("−5.0%", fmtSignedPercent(-0.05))
    }
}
