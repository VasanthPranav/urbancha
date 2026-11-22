package com.urbancha.inventory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urbancha.inventory.data.Material
import com.urbancha.inventory.data.ShiftClock
import com.urbancha.inventory.data.UNITS
import com.urbancha.inventory.data.fmtQty
import com.urbancha.inventory.ui.AppViewModel
import com.urbancha.inventory.ui.Header
import com.urbancha.inventory.ui.NumberField
import com.urbancha.inventory.ui.Panel
import com.urbancha.inventory.ui.Pill
import com.urbancha.inventory.ui.Screen
import com.urbancha.inventory.ui.SectionTitle
import com.urbancha.inventory.ui.theme.Danger
import com.urbancha.inventory.ui.theme.Muted
import com.urbancha.inventory.ui.theme.TeaGreen
import com.urbancha.inventory.ui.theme.TeaGreenSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockScreen(vm: AppViewModel, go: (Screen) -> Unit) {
    val materials by vm.materials.collectAsState()
    val restocks by vm.currentRestocks.collectAsState()
    val shiftId by vm.currentShiftId.collectAsState()
    var selectedId by remember { mutableStateOf<String?>(null) }
    var qtyText by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Material?>(null) }
    var adding by remember { mutableStateOf(false) }

    val selected = materials.firstOrNull { it.id == selectedId } ?: materials.firstOrNull()
    val qty = qtyText.toDoubleOrNull() ?: 0.0

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Stock", "Shift ${ShiftClock.rangeLabel(shiftId)}")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Panel {
                Text("Restock — what arrived?", fontWeight = FontWeight.ExtraBold, color = Muted, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    materials.forEach { m -> Pill(m.name, m.id == selected?.id) { selectedId = m.id; qtyText = "" } }
                    Pill("+ New raw material", false) { adding = true }
                }
                if (selected != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(qtyText, { qtyText = it }, label = "Quantity added", suffix = selected.unit, modifier = Modifier.weight(1f))
                        quickSteps(selected.unit).forEach { step ->
                            OutlinedButton(onClick = { qtyText = fmtQty(qty + step) }, shape = RoundedCornerShape(12.dp)) { Text("+${fmtQty(step)}") }
                        }
                    }
                    Panel(container = TeaGreenSoft) {
                        Row {
                            Text("${selected.name} in stock", color = TeaGreen, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${fmtQty(selected.qty)} → ${fmtQty(selected.qty + qty)} ${selected.unit}", color = TeaGreen, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Button(
                        onClick = { vm.restock(selected, qty); qtyText = "" },
                        enabled = qty > 0,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Add to stock", fontWeight = FontWeight.ExtraBold) }
                } else {
                    Text("No raw materials yet. Tap “+ New raw material”.", color = Muted)
                }
            }

            SectionTitle("Restocked this shift")
            Panel {
                if (restocks.isEmpty()) Text("Nothing restocked yet.", color = Muted)
                val fmt = SimpleDateFormat("h:mm a", Locale.ENGLISH)
                restocks.forEach { r ->
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, fontWeight = FontWeight.Bold)
                            Text(fmt.format(Date(r.atMillis)), color = Muted, fontSize = 12.sp)
                        }
                        Text("+${fmtQty(r.qty)} ${r.unit}", color = TeaGreen, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            SectionTitle("All raw materials")
            Panel {
                materials.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.name, fontWeight = FontWeight.Bold)
                            if (m.cost > 0) Text("₹${fmtQty(m.cost)} per ${m.unit}", color = Muted, fontSize = 12.sp)
                            if (m.lowAlert > 0) Text("Warn below ${fmtQty(m.lowAlert)} ${m.unit}", color = Muted, fontSize = 12.sp)
                        }
                        Text("${fmtQty(m.qty)} ${m.unit}", fontWeight = FontWeight.ExtraBold, color = if (m.isLow) Danger else TeaGreen)
                        IconButton(onClick = { editing = m }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit ${m.name}") }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                OutlinedButton(onClick = { go(Screen.Opening) }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("Opening stock") }
                OutlinedButton(onClick = { go(Screen.Close) }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) { Text("Close shift") }
            }
        }
    }

    if (adding || editing != null) {
        MaterialDialog(
            existing = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { name, unit, q, alert, cost ->
                vm.saveMaterial(editing?.id, name, unit, q, alert, cost)
                adding = false; editing = null
            },
            onDelete = editing?.let { m -> { vm.deleteMaterial(m.id); editing = null } },
        )
    }
}

private fun quickSteps(unit: String): List<Double> = when (unit) {
    "g", "ml" -> listOf(100.0, 500.0)
    "pcs" -> listOf(10.0, 50.0)
    else -> listOf(1.0, 5.0)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialDialog(
    existing: Material?,
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String, qty: Double?, lowAlert: Double, cost: Double) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "g") }
    var qty by remember { mutableStateOf(existing?.let { fmtQty(it.qty) } ?: "") }
    var alert by remember { mutableStateOf(existing?.lowAlert?.takeIf { it > 0 }?.let { fmtQty(it) } ?: "") }
    var cost by remember { mutableStateOf(existing?.cost?.takeIf { it > 0 }?.let { fmtQty(it) } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New raw material" else "Edit ${existing.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name (e.g. Ginger)") }, singleLine = true)
                Text("Measured in", fontWeight = FontWeight.Bold, color = Muted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UNITS.forEach { u -> Pill(u, unit == u) { unit = u } }
                }
                NumberField(qty, { qty = it }, label = if (existing == null) "In stock now" else "Correct stock to", suffix = unit)
                NumberField(alert, { alert = it }, label = "Warn me below (optional)", suffix = unit)
                NumberField(
                    cost, { cost = it },
                    label = "Cost per $unit (optional)",
                    suffix = "₹/$unit",
                    supportingText = "Used by Analytics to work out raw-material cost as a % of sales.",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newQty = qty.toDoubleOrNull()
                    // Only overwrite stock when it was changed, so a sale on another phone is not undone.
                    val qtyToSave = if (existing != null && fmtQty(existing.qty) == qty) null else (newQty ?: if (existing == null) 0.0 else null)
                    onSave(name.trim(), unit, qtyToSave, alert.toDoubleOrNull() ?: 0.0, cost.toDoubleOrNull() ?: 0.0)
                },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = Danger) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
