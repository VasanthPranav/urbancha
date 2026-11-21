package com.urbancha.inventory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urbancha.inventory.data.Shift
import com.urbancha.inventory.data.ShiftClock
import com.urbancha.inventory.data.fmtQty
import com.urbancha.inventory.data.fmtRupees
import com.urbancha.inventory.ui.AppViewModel
import com.urbancha.inventory.ui.Header
import com.urbancha.inventory.ui.NumberField
import com.urbancha.inventory.ui.Panel
import com.urbancha.inventory.ui.theme.Danger
import com.urbancha.inventory.ui.theme.Muted
import com.urbancha.inventory.ui.theme.SaffronText
import com.urbancha.inventory.ui.theme.TeaGreen

@Composable
fun CloseShiftScreen(vm: AppViewModel, onDone: () -> Unit) {
    val materials by vm.materials.collectAsState()
    val shift by vm.closableShift.collectAsState()
    val counted = remember { mutableStateMapOf<String, String>() }
    var confirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Close shift", ShiftClock.rangeLabel(shift.id), onBack = onDone)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Panel(Modifier.weight(1f)) {
                    Text("Sales", fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold)
                    Text(fmtRupees(shift.salesTotal), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Panel(Modifier.weight(1f)) {
                    Text("Items sold", fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold)
                    Text(shift.itemCount.toString(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            if (shift.status == Shift.STATUS_CLOSED) {
                Text("This shift is already closed. Closing again replaces the saved count.", color = Muted, fontSize = 13.sp)
            }
            Text(
                "“In” = opening stock + restocks. “Left” is what the app expects (1 tea = 1 cup). Type what you actually count.",
                color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
            Panel {
                Row {
                    Text("ITEM", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                    listOf("IN", "USED", "LEFT").forEach {
                        Text(it, Modifier.width(52.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted, textAlign = TextAlign.End)
                    }
                    Text("COUNTED", Modifier.width(96.dp).padding(start = 8.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                }
                materials.forEach { m ->
                    val inTotal = (shift.opening[m.id] ?: m.qty) + (shift.restocked[m.id] ?: 0.0)
                    val c = counted[m.id]?.toDoubleOrNull()
                    val usedNow = inTotal - (c ?: m.qty)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(m.unit, color = Muted, fontSize = 11.sp)
                            if (c != null && c != m.qty) {
                                Text("${if (c < m.qty) "" else "+"}${fmtQty(c - m.qty)} vs expected", color = Danger, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(fmtQty(inTotal), Modifier.width(52.dp), textAlign = TextAlign.End, color = Muted)
                        Text(fmtQty(usedNow), Modifier.width(52.dp), textAlign = TextAlign.End, color = SaffronText, fontWeight = FontWeight.Bold)
                        Text(fmtQty(m.qty), Modifier.width(52.dp), textAlign = TextAlign.End, color = TeaGreen, fontWeight = FontWeight.ExtraBold)
                        NumberField(counted[m.id] ?: "", { counted[m.id] = it }, modifier = Modifier.width(96.dp).padding(start = 8.dp))
                    }
                }
            }
            Button(
                onClick = { confirm = true },
                enabled = materials.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Close shift & carry stock forward", fontWeight = FontWeight.ExtraBold) }
            Text(" ", Modifier.height(12.dp))
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Close this shift?") },
            text = { Text("Stock will be set to the counted amounts (blank = the expected amount) and carried forward to the next shift.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.closeShift(counted.mapNotNull { (k, v) -> v.toDoubleOrNull()?.let { k to it } }.toMap())
                    confirm = false
                    onDone()
                }) { Text("Close shift") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}
