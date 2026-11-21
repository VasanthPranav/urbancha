package com.urbancha.inventory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urbancha.inventory.data.Shift
import com.urbancha.inventory.data.ShiftClock
import com.urbancha.inventory.data.fmtQty
import com.urbancha.inventory.data.fmtRupees
import com.urbancha.inventory.ui.AppViewModel
import com.urbancha.inventory.ui.Panel
import com.urbancha.inventory.ui.Screen
import com.urbancha.inventory.ui.SectionTitle
import com.urbancha.inventory.ui.theme.Danger
import com.urbancha.inventory.ui.theme.Ink
import com.urbancha.inventory.ui.theme.Muted
import com.urbancha.inventory.ui.theme.Saffron
import com.urbancha.inventory.ui.theme.TeaGreen
import com.urbancha.inventory.ui.theme.TeaGreenSoft

@Composable
fun HomeScreen(vm: AppViewModel, go: (Screen) -> Unit) {
    val shift by vm.currentShift.collectAsState()
    val materials by vm.materials.collectAsState()
    val products by vm.products.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Urban Cha", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TeaGreen)
                Text("Shift: ${ShiftClock.rangeLabel(shift.id)}", fontSize = 13.sp, color = Muted, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { vm.signOut() }) { Text("Sign out", color = Muted) }
        }

        val status = when (shift.status) {
            Shift.STATUS_OPEN -> "Shift open"
            Shift.STATUS_CLOSED -> "Shift closed"
            else -> "Opening stock not done yet"
        }
        Text(status, fontWeight = FontWeight.Bold, color = TeaGreen, fontSize = 13.sp,
            modifier = Modifier.padding(0.dp))

        if (products.isEmpty() && materials.isEmpty()) {
            Panel {
                Text("Your shop is empty", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("Add your products and raw materials, or load Urban Cha's usual items and edit the prices and quantities.", color = Muted)
                Button(onClick = { vm.loadStarterData() }) { Text("Load starter items") }
            }
        }

        Panel(container = TeaGreen) {
            Text("This shift's sales", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(fmtRupees(shift.salesTotal), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "${shift.billCount} bills · ${shift.itemCount} items · Cash ${fmtRupees(shift.cashTotal)} · UPI ${fmtRupees(shift.upiTotal)}",
                color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
            val top = shift.items.values.sortedByDescending { it.qty }.take(3)
            if (top.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    top.forEach { t ->
                        Column(Modifier.weight(1f)) {
                            Text(t.qty.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text(t.name, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        Button(
            onClick = { go(Screen.Sell) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Saffron, contentColor = Ink),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text("New sale", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }

        val low = materials.filter { it.isLow }
        if (low.isNotEmpty()) {
            SectionTitle("Running low")
            Panel {
                low.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${fmtQty(m.qty)} ${m.unit} left", color = Danger, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val btn = Modifier.weight(1f).height(52.dp)
            OutlinedButton(onClick = { go(Screen.Opening) }, modifier = btn, shape = RoundedCornerShape(14.dp)) { Text("Opening", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = { go(Screen.Stock) }, modifier = btn, shape = RoundedCornerShape(14.dp)) { Text("Restock", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = { go(Screen.Close) }, modifier = btn, shape = RoundedCornerShape(14.dp)) { Text("Close", fontWeight = FontWeight.Bold) }
        }

        Panel(container = TeaGreenSoft) {
            Text("Synced live: every phone signed in to this shop sees the same numbers. Works offline too — changes upload when the internet is back.",
                color = TeaGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
