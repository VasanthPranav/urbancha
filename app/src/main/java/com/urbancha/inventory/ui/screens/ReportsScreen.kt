package com.urbancha.inventory.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urbancha.inventory.data.Material
import com.urbancha.inventory.data.Shift
import com.urbancha.inventory.data.ShiftClock
import com.urbancha.inventory.data.fmtQty
import com.urbancha.inventory.data.fmtRupees
import com.urbancha.inventory.ui.AppViewModel
import com.urbancha.inventory.ui.Header
import com.urbancha.inventory.ui.Panel
import com.urbancha.inventory.ui.SectionTitle
import com.urbancha.inventory.ui.theme.Danger
import com.urbancha.inventory.ui.theme.Line
import com.urbancha.inventory.ui.theme.Muted
import com.urbancha.inventory.ui.theme.Saffron
import com.urbancha.inventory.ui.theme.TeaGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(vm: AppViewModel) {
    val currentId by vm.currentShiftId.collectAsState()
    var shiftId by rememberSaveable { mutableStateOf(currentId) }
    val shiftFlow = remember(shiftId) { vm.shift(shiftId) }
    val billsFlow = remember(shiftId) { vm.bills(shiftId) }
    val shift by shiftFlow.collectAsState(initial = Shift(shiftId))
    val bills by billsFlow.collectAsState(initial = emptyList())
    val materials by vm.materials.collectAsState()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Sales report")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { shiftId = ShiftClock.previous(shiftId) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous shift")
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ShiftClock.dayLabel(shiftId), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(ShiftClock.rangeLabel(shiftId), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { shiftId = ShiftClock.next(shiftId) }, enabled = shiftId < currentId) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next shift")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Panel(Modifier.weight(1f), container = TeaGreen) {
                    Text("Total sales", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(fmtRupees(shift.salesTotal), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Cash ${fmtRupees(shift.cashTotal)} · UPI ${fmtRupees(shift.upiTotal)}", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                }
                Panel(Modifier.weight(1f)) {
                    Text("Items sold", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(shift.itemCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${shift.billCount} bills", color = Muted, fontSize = 11.sp)
                }
            }

            SectionTitle("Products sold")
            Panel {
                val rows = shift.items.values.sortedByDescending { it.qty }
                val max = rows.maxOfOrNull { it.qty }?.coerceAtLeast(1) ?: 1
                if (rows.isEmpty()) Text("No sales in this shift.", color = Muted)
                rows.forEach { r ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            Text(r.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(r.qty.toString(), fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 16.dp))
                            Text(fmtRupees(r.amount), color = Muted, fontWeight = FontWeight.SemiBold)
                        }
                        Box(Modifier.fillMaxWidth().height(6.dp).background(Line, RoundedCornerShape(999.dp))) {
                            Box(Modifier.fillMaxWidth(r.qty.toFloat() / max).fillMaxHeight().background(Saffron, RoundedCornerShape(999.dp)))
                        }
                    }
                }
            }

            if (shift.closing.isNotEmpty() || shift.restocked.isNotEmpty()) {
                SectionTitle("Stock")
                Panel {
                    materials.forEach { m -> StockLine(m, shift) }
                }
            }

            if (bills.isNotEmpty()) {
                SectionTitle("Latest bills")
                Panel {
                    val fmt = SimpleDateFormat("h:mm a", Locale.ENGLISH)
                    bills.forEach { b ->
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(b.summary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                                Text("${fmt.format(Date(b.atMillis))} · ${b.payment}", color = Muted, fontSize = 11.sp)
                            }
                            Text(fmtRupees(b.total), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reportText(shift, materials))
                    }
                    context.startActivity(Intent.createChooser(send, "Share report"))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 0.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Share report (WhatsApp, SMS…)", fontWeight = FontWeight.ExtraBold) }
            Text(" ", Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StockLine(m: Material, shift: Shift) {
    val closing = shift.closing[m.id]
    val opening = shift.opening[m.id]
    val restocked = shift.restocked[m.id] ?: 0.0
    Row {
        Column(Modifier.weight(1f)) {
            Text(m.name, fontWeight = FontWeight.Bold)
            Text(
                buildString {
                    if (opening != null) append("Open ${fmtQty(opening)}")
                    if (restocked > 0) append(" · +${fmtQty(restocked)} restock")
                    if (closing != null) append(" · used ${fmtQty(closing.used)}")
                },
                color = Muted, fontSize = 12.sp,
            )
        }
        if (closing != null) {
            val diff = closing.counted - closing.expected
            Column(horizontalAlignment = Alignment.End) {
                Text("${fmtQty(closing.counted)} ${m.unit} left", fontWeight = FontWeight.ExtraBold, color = TeaGreen)
                if (diff != 0.0) Text("${if (diff > 0) "+" else ""}${fmtQty(diff)} vs expected", color = Danger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun reportText(shift: Shift, materials: List<Material>): String = buildString {
    appendLine("Urban Cha — ${ShiftClock.dayLabel(shift.id)}")
    appendLine(ShiftClock.rangeLabel(shift.id))
    appendLine()
    appendLine("Sales: ${fmtRupees(shift.salesTotal)} (Cash ${fmtRupees(shift.cashTotal)}, UPI ${fmtRupees(shift.upiTotal)})")
    appendLine("Bills: ${shift.billCount} · Items: ${shift.itemCount}")
    appendLine()
    shift.items.values.sortedByDescending { it.qty }.forEach { appendLine("${it.name}: ${it.qty} — ${fmtRupees(it.amount)}") }
    if (shift.closing.isNotEmpty()) {
        appendLine()
        appendLine("Closing stock:")
        materials.forEach { m ->
            shift.closing[m.id]?.let { c -> appendLine("${m.name}: ${fmtQty(c.counted)} ${m.unit} (used ${fmtQty(c.used)})") }
        }
    }
}
