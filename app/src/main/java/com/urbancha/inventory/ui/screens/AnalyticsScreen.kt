package com.urbancha.inventory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urbancha.inventory.data.Analytics
import com.urbancha.inventory.data.AnalyticsReport
import com.urbancha.inventory.data.ShiftClock
import com.urbancha.inventory.data.fmtPercent
import com.urbancha.inventory.data.fmtQty
import com.urbancha.inventory.data.fmtRupees
import com.urbancha.inventory.data.fmtSignedPercent
import com.urbancha.inventory.ui.AppViewModel
import com.urbancha.inventory.ui.Header
import com.urbancha.inventory.ui.LocalPrefs
import com.urbancha.inventory.ui.Panel
import com.urbancha.inventory.ui.Pill
import com.urbancha.inventory.ui.SectionTitle
import com.urbancha.inventory.ui.theme.ChipBg
import com.urbancha.inventory.ui.theme.Danger
import com.urbancha.inventory.ui.theme.Muted
import com.urbancha.inventory.ui.theme.Saffron
import com.urbancha.inventory.ui.theme.SaffronSoft
import com.urbancha.inventory.ui.theme.SaffronText
import com.urbancha.inventory.ui.theme.TeaGreen
import java.util.Locale

private val PERIODS = listOf(1 to "1 day", 7 to "7 days", 30 to "30 days", 90 to "90 days")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(vm: AppViewModel) {
    val prefs = LocalPrefs.current
    val n = prefs.analyticsDays.takeIf { d -> PERIODS.any { it.first == d } } ?: 7
    val percent = prefs.analyticsPercent
    val currentId by vm.currentShiftId.collectAsState()
    val materials by vm.materials.collectAsState()
    val products by vm.products.collectAsState()

    // 2N ids: the first N are the previous period (for comparisons), the last N the current one.
    val allIds = remember(currentId, n) { Analytics.periodIds(currentId, 2 * n) }
    val shiftsFlow = remember(allIds) { vm.shiftRange(allIds.first(), allIds.last()) }
    val shifts by shiftsFlow.collectAsState(initial = emptyList())

    val curIds = allIds.takeLast(n)
    val prevIds = allIds.take(n)
    val cur = Analytics.report(curIds, shifts, materials, products)
    val prev = Analytics.report(prevIds, shifts, materials, products)
    val prevLabel = if (n == 1) "previous day" else "previous $n days"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(
            "Analytics",
            if (n == 1) ShiftClock.rangeLabel(currentId)
            else "${ShiftClock.dayLabel(curIds.first())} – ${ShiftClock.dayLabel(curIds.last())}",
        )
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Panel {
                Text("PERIOD", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PERIODS.forEach { (d, label) -> Pill(label, d == n) { prefs.analyticsDays = d } }
                }
                Text("SHOW AS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("₹ Amount", !percent) { prefs.analyticsPercent = false }
                    Pill("% Percent", percent) { prefs.analyticsPercent = true }
                }
            }

            // 1. Overview
            SectionTitle("Overview")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OverviewCard("Total sales", cur.sales, prev.sales, ::fmtRupees, prev.bills, prevLabel, percent, Modifier.weight(1f))
                OverviewCard("Bills", cur.bills.toDouble(), prev.bills.toDouble(), { it.toLong().toString() }, prev.bills, prevLabel, percent, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OverviewCard("Average bill", cur.averageBill, prev.averageBill, ::fmtRupees, prev.bills, prevLabel, percent, Modifier.weight(1f))
                OverviewCard("Items sold", cur.items.toDouble(), prev.items.toDouble(), { it.toLong().toString() }, prev.bills, prevLabel, percent, Modifier.weight(1f))
            }

            // 2. Daily sales
            if (n > 1) {
                SectionTitle(if (percent) "Daily share of sales" else "Daily sales")
                DailySales(cur, percent)
            }

            // 3. Cash vs UPI
            if (cur.sales > 0) {
                SectionTitle("Cash vs UPI")
                CashUpi(cur, percent)
            }

            // 4. Raw material cost
            SectionTitle("Raw material cost")
            RawMaterials(cur, percent, n)

            // 5. Products
            if (cur.products.isNotEmpty()) {
                SectionTitle("Products")
                Panel {
                    cur.products.forEach { p ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (percent) "${fmtPercent(p.unitShare)} of items" else "${p.units} sold",
                                        color = Muted, fontSize = 12.sp,
                                    )
                                }
                                Text(if (percent) fmtPercent(p.share) else fmtRupees(p.revenue), fontWeight = FontWeight.ExtraBold)
                            }
                            ShareBar(p.share.toFloat(), TeaGreen)
                        }
                    }
                }
            }

            // 6. Stock outlook
            val outlook = cur.materials.filter { it.daysLeft != null }.sortedBy { it.daysLeft }
            if (outlook.isNotEmpty()) {
                SectionTitle("Stock outlook")
                Panel {
                    Text("At the average use per closed shift, current stock lasts:", color = Muted, fontSize = 12.sp)
                    outlook.forEach { m ->
                        val d = m.daysLeft!!
                        Row {
                            Text(m.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(
                                if (d < 1) "Less than a day" else "${String.format(Locale.ENGLISH, "%.1f", d)} days",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (d < 2) Danger else TeaGreen,
                            )
                        }
                    }
                }
            }

            // 7. Missing stock
            if (cur.missing.isNotEmpty()) {
                SectionTitle("Missing stock")
                Panel {
                    Text("Counted less than expected at Close shift, for items taken off per sale.", color = Muted, fontSize = 12.sp)
                    cur.missing.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(m.name, fontWeight = FontWeight.Bold)
                                val worth = when {
                                    m.shortfallCost == null -> null
                                    percent -> cur.closedSales.takeIf { it > 0 }?.let { "Worth about ${fmtPercent(m.shortfallCost / it)} of sales" }
                                    else -> "Worth about ${fmtRupees(m.shortfallCost)}"
                                }
                                if (worth != null) Text(worth, color = Muted, fontSize = 12.sp)
                            }
                            Text(
                                if (percent) "${m.shortfallShare?.let(::fmtPercent) ?: "–"} of ${m.name.lowercase()} used"
                                else "${fmtQty(m.shortfall)} ${m.unit}",
                                fontWeight = FontWeight.ExtraBold, color = Danger,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    value: Double,
    previous: Double,
    fmt: (Double) -> String,
    previousBills: Long,
    prevLabel: String,
    percent: Boolean,
    modifier: Modifier,
) {
    val change = Analytics.change(value, previous)
    val changeColor = when {
        change == null -> Muted
        change > 0 -> TeaGreen
        change < 0 -> Danger
        else -> Muted
    }
    Panel(modifier) {
        Text(title, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (percent) {
            Text(change?.let(::fmtSignedPercent) ?: "–", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = changeColor)
            Text("${fmt(value)} · was ${fmt(previous)}", color = Muted, fontSize = 12.sp)
        } else {
            Text(fmt(value), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                when {
                    change != null -> "${fmtSignedPercent(change)} vs $prevLabel"
                    previousBills == 0L -> "No bills in $prevLabel"
                    else -> "No change data for $prevLabel"
                },
                color = changeColor, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun DailySales(r: AnalyticsReport, percent: Boolean) {
    val max = r.days.maxOfOrNull { it.sales } ?: 0.0
    val axisTop = if (percent) (if (r.sales > 0) fmtPercent(max / r.sales) else "0%") else fmtRupees(max)
    Panel {
        Row(Modifier.fillMaxWidth().height(140.dp)) {
            Column(Modifier.fillMaxHeight().padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(axisTop, fontSize = 10.sp, color = Muted)
                Text(if (percent) "0%" else "₹0", fontSize = 10.sp, color = Muted)
            }
            Row(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(if (r.days.size > 40) 1.dp else 3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                r.days.forEach { d ->
                    val f = if (max > 0) (d.sales / max).toFloat() else 0f
                    Box(
                        Modifier.weight(1f)
                            .fillMaxHeight(f.coerceAtLeast(0.01f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (d == r.best) Saffron else TeaGreen),
                    )
                }
            }
        }
        Row {
            Text(ShiftClock.dayLabel(r.days.first().shiftId), fontSize = 10.sp, color = Muted, modifier = Modifier.weight(1f))
            Text(ShiftClock.dayLabel(r.days.last().shiftId), fontSize = 10.sp, color = Muted, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        fun value(v: Double) = if (percent) (if (r.sales > 0) fmtPercent(v / r.sales) else "0%") else fmtRupees(v)
        if (r.best != null && r.weakest != null) {
            Row {
                Text("Best: ${ShiftClock.dayLabel(r.best.shiftId)}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(value(r.best.sales), fontWeight = FontWeight.ExtraBold, color = TeaGreen)
            }
            Row {
                Text("Lowest: ${ShiftClock.dayLabel(r.weakest.shiftId)}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(value(r.weakest.sales), fontWeight = FontWeight.ExtraBold, color = SaffronText)
            }
            Text("Average ${fmtRupees(r.averagePerShift)} on the ${r.shiftsWithSales} days with sales", color = Muted, fontSize = 12.sp)
        } else {
            Text("No sales in this period yet.", color = Muted)
        }
    }
}

@Composable
private fun CashUpi(r: AnalyticsReport, percent: Boolean) {
    val cashShare = r.cash / r.sales
    val upiShare = r.upi / r.sales
    Panel {
        Row {
            Column(Modifier.weight(1f)) {
                Text("Cash", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (percent) fmtPercent(cashShare) else fmtRupees(r.cash), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TeaGreen)
                Text(if (percent) fmtRupees(r.cash) else fmtPercent(cashShare), color = Muted, fontSize = 12.sp)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("UPI", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (percent) fmtPercent(upiShare) else fmtRupees(r.upi), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SaffronText)
                Text(if (percent) fmtRupees(r.upi) else fmtPercent(upiShare), color = Muted, fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(999.dp)).background(ChipBg)) {
            if (r.cash > 0) Box(Modifier.weight(r.cash.toFloat()).fillMaxHeight().background(TeaGreen))
            if (r.upi > 0) Box(Modifier.weight(r.upi.toFloat()).fillMaxHeight().background(Saffron))
        }
    }
}

@Composable
private fun RawMaterials(r: AnalyticsReport, percent: Boolean, n: Int) {
    if (r.closedShiftCount == 0) {
        Panel {
            Text(
                (if (n == 1) "Close this shift to see it." else "Close a shift to see this.") +
                    " Raw-material usage comes from the count you enter at Close shift.",
                color = Muted,
            )
        }
        return
    }
    val noCosts = r.materialCost == 0.0 && r.materialsWithoutCost.isNotEmpty()
    val costShare = r.materialCostShare
    val marginShare = if (r.closedSales > 0) r.margin / r.closedSales else null
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Panel(Modifier.weight(1f)) {
            Text("Raw materials", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (noCosts) {
                Text("–", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Add costs in Stock", color = Muted, fontSize = 12.sp)
            } else {
                Text(if (percent) costShare?.let(::fmtPercent) ?: "–" else fmtRupees(r.materialCost), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (percent) fmtRupees(r.materialCost) else "${costShare?.let(::fmtPercent) ?: "–"} of sales", color = Muted, fontSize = 12.sp)
            }
        }
        Panel(Modifier.weight(1f)) {
            Text("Margin after materials", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (noCosts) {
                Text("–", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("Add costs in Stock", color = Muted, fontSize = 12.sp)
            } else {
                Text(
                    if (percent) marginShare?.let(::fmtPercent) ?: "–" else fmtRupees(r.margin),
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if (r.margin < 0) Danger else TeaGreen,
                )
                Text(if (percent) fmtRupees(r.margin) else "${marginShare?.let(::fmtPercent) ?: "–"} of sales", color = Muted, fontSize = 12.sp)
            }
        }
    }
    Text(
        "From ${r.closedShiftCount} closed shift${if (r.closedShiftCount == 1) "" else "s"}. A shift that's still open has no closing count yet.",
        color = Muted, fontSize = 12.sp,
    )
    if (r.materialsWithoutCost.isNotEmpty()) {
        Panel(container = SaffronSoft) {
            Text(
                "No cost set for ${r.materialsWithoutCost.joinToString(", ")}, so ${if (r.materialsWithoutCost.size == 1) "it's" else "they're"} left out. Add a cost in Stock → edit.",
                color = SaffronText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
    if (r.materials.isNotEmpty()) {
        Panel {
            Row {
                Text("MATERIAL", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted, modifier = Modifier.weight(1.4f))
                Text("USED", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                Text(if (percent) "% OF SALES" else "COST", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
            r.materials.forEach { m ->
                Row {
                    Text(m.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                    Text("${fmtQty(m.used)} ${m.unit}", textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    Text(
                        when {
                            m.cost == null -> "–"
                            percent -> m.shareOfSales?.let(::fmtPercent) ?: "–"
                            else -> fmtRupees(m.cost)
                        },
                        fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(ChipBg)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}
