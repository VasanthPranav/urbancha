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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urbancha.inventory.data.Shift
import com.urbancha.inventory.data.ShiftClock
import com.urbancha.inventory.data.fmtQty
import com.urbancha.inventory.ui.AppViewModel
import com.urbancha.inventory.ui.Header
import com.urbancha.inventory.ui.NumberField
import com.urbancha.inventory.ui.Panel
import com.urbancha.inventory.ui.theme.Muted
import com.urbancha.inventory.ui.theme.TeaGreen
import com.urbancha.inventory.ui.theme.TeaGreenSoft

@Composable
fun OpeningStockScreen(vm: AppViewModel, onDone: () -> Unit) {
    val materials by vm.materials.collectAsState()
    val shift by vm.currentShift.collectAsState()
    val received = remember { mutableStateMapOf<String, String>() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Opening stock", "Shift starts ${ShiftClock.rangeLabel(shift.id)}", onBack = onDone)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(container = TeaGreenSoft) {
                Text(
                    if (shift.status == Shift.STATUS_NOT_STARTED)
                        "Last shift's closing stock is carried forward. Add what arrived before 3 PM — anything later goes in Restock."
                    else "Opening stock is already done for this shift. Saving again adds the new amounts and updates the opening figures.",
                    color = TeaGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                )
            }
            Panel {
                Row {
                    Text("ITEM", Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                    Text("NOW", Modifier.width(64.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted, textAlign = TextAlign.End)
                    Text("+ RECEIVED", Modifier.width(110.dp).padding(start = 10.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                    Text("OPENING", Modifier.width(64.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted, textAlign = TextAlign.End)
                }
                materials.forEach { m ->
                    val add = received[m.id]?.toDoubleOrNull() ?: 0.0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.name, fontWeight = FontWeight.Bold)
                            Text(m.unit, color = Muted, fontSize = 11.sp)
                        }
                        Text(fmtQty(m.qty), Modifier.width(64.dp), textAlign = TextAlign.End, color = Muted)
                        NumberField(received[m.id] ?: "", { received[m.id] = it }, modifier = Modifier.width(110.dp).padding(start = 10.dp))
                        Text(fmtQty(m.qty + add), Modifier.width(64.dp), textAlign = TextAlign.End, fontWeight = FontWeight.ExtraBold, color = TeaGreen)
                    }
                }
            }
            Button(
                onClick = {
                    vm.startShift(received.mapValues { it.value.toDoubleOrNull() ?: 0.0 })
                    onDone()
                },
                enabled = materials.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 0.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Start the shift", fontWeight = FontWeight.ExtraBold) }
            Text(" ", Modifier.height(12.dp))
        }
    }
}
