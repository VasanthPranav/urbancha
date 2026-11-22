package com.urbancha.inventory.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urbancha.inventory.data.ShiftClock
import com.urbancha.inventory.data.fmtQty
import com.urbancha.inventory.data.fmtRupees
import com.urbancha.inventory.ui.AppViewModel
import com.urbancha.inventory.ui.Header
import com.urbancha.inventory.ui.Panel
import com.urbancha.inventory.ui.theme.Line
import com.urbancha.inventory.ui.theme.Muted
import com.urbancha.inventory.ui.theme.SaffronText
import com.urbancha.inventory.ui.theme.SurfaceBg
import com.urbancha.inventory.ui.theme.TeaGreen
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

@Composable
fun SellScreen(vm: AppViewModel) {
    val products by vm.products.collectAsState()
    val materials by vm.materials.collectAsState()
    val shiftId by vm.currentShiftId.collectAsState()
    val cart = remember { mutableStateMapOf<String, Int>() }
    var saved by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saved) { if (saved != null) { delay(2500); saved = null } }

    val lines = products.associateWith { cart[it.id] ?: 0 }.filterValues { it > 0 }
    val total = lines.entries.sumOf { (p, q) -> p.price * q }
    val count = lines.values.sum()

    // Live "stock after this bill" for the items this bill touches (e.g. cups for teas).
    val usedByMaterial = lines.entries.filter { it.key.stockItemId != null }
        .groupBy({ it.key.stockItemId!! }, { it.value }).mapValues { it.value.sum() }
    val linkedIds = products.mapNotNull { it.stockItemId }.toSet()
    val strip = materials.filter { it.id in linkedIds }

    Column(Modifier.fillMaxSize()) {
        Header("New sale", "Shift ${ShiftClock.rangeLabel(shiftId)}")

        if (strip.isNotEmpty()) {
            Panel(Modifier.padding(horizontal = 20.dp)) {
                Text("STOCK AFTER THIS BILL", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    strip.forEach { m ->
                        val used = usedByMaterial[m.id] ?: 0
                        Column(Modifier.weight(1f)) {
                            Text(m.name, fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold)
                            Text(
                                "${fmtQty(m.qty)} → ${fmtQty(m.qty - used)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (used > 0) SaffronText else Color.Unspecified,
                            )
                        }
                    }
                }
            }
        }

        if (products.isEmpty()) {
            Text("No products yet. Add them in the Products tab.", color = Muted, modifier = Modifier.padding(20.dp))
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(products, key = { it.id }) { p ->
                val q = cart[p.id] ?: 0
                Surface(shape = RoundedCornerShape(14.dp), color = SurfaceBg, border = BorderStroke(1.dp, if (q > 0) TeaGreen else Line)) {
                    Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(fmtRupees(p.price), color = Muted, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedIconButton(onClick = { if (q > 0) cart[p.id] = q - 1 }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Remove, contentDescription = "Remove one ${p.name}")
                        }
                        Text(q.toString(), Modifier.width(36.dp), textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        FilledIconButton(onClick = { cart[p.id] = q + 1 }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = "Add one ${p.name}")
                        }
                    }
                }
            }
        }

        Surface(color = SurfaceBg, shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(saved ?: "$count items", color = if (saved != null) TeaGreen else Muted, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(fmtRupees(total), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val pay: (String) -> Unit = { method ->
                        vm.recordSale(lines, method)
                        saved = "Saved ${fmtRupees(total)} ($method)"
                        cart.clear()
                    }
                    OutlinedButton(onClick = { pay("Cash") }, enabled = count > 0, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
                        Text("Cash", fontWeight = FontWeight.ExtraBold)
                    }
                    Button(onClick = { pay("UPI") }, enabled = count > 0, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
                        Text("UPI", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
