package com.urbancha.inventory.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.urbancha.inventory.data.Product
import com.urbancha.inventory.data.fmtQty
import com.urbancha.inventory.data.fmtRupees
import com.urbancha.inventory.ui.AppViewModel
import com.urbancha.inventory.ui.Header
import com.urbancha.inventory.ui.NumberField
import com.urbancha.inventory.ui.Panel
import com.urbancha.inventory.ui.Pill
import com.urbancha.inventory.ui.theme.Danger
import com.urbancha.inventory.ui.theme.Ink
import com.urbancha.inventory.ui.theme.Muted
import com.urbancha.inventory.ui.theme.Saffron
import com.urbancha.inventory.ui.theme.TeaGreen

@Composable
fun ProductsScreen(vm: AppViewModel) {
    val products by vm.products.collectAsState()
    val materials by vm.materials.collectAsState()
    var editing by remember { mutableStateOf<Product?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Header("Products", "${products.size} items on the menu") {
            Button(
                onClick = { adding = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Saffron, contentColor = Ink),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add product", fontWeight = FontWeight.ExtraBold)
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            item {
                Panel {
                    if (products.isEmpty()) Text("No products yet.", color = Muted)
                    products.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                val linked = materials.firstOrNull { it.id == p.stockItemId }
                                if (linked != null) Text("Each sale uses 1 ${linked.name.lowercase()}", color = Muted, fontSize = 12.sp)
                            }
                            Text(fmtRupees(p.price), fontWeight = FontWeight.ExtraBold, color = TeaGreen)
                            IconButton(onClick = { editing = p }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit ${p.name}") }
                        }
                    }
                }
            }
        }
    }

    if (adding || editing != null) {
        ProductDialog(
            existing = editing,
            materials = materials,
            onDismiss = { adding = false; editing = null },
            onSave = { name, price, stockId ->
                vm.saveProduct(editing?.id, name, price, stockId)
                adding = false; editing = null
            },
            onDelete = editing?.let { p -> { vm.deleteProduct(p.id); editing = null } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDialog(
    existing: Product?,
    materials: List<Material>,
    onDismiss: () -> Unit,
    onSave: (name: String, price: Double, stockItemId: String?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var price by remember { mutableStateOf(existing?.price?.let { fmtQty(it) } ?: "") }
    var stockId by remember { mutableStateOf(existing?.stockItemId) }
    val countable = materials.filter { it.unit == "pcs" }
    val ok = name.isNotBlank() && (price.toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New product" else "Edit product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Product name") }, singleLine = true)
                NumberField(price, { price = it }, label = "Price (₹)")
                if (countable.isNotEmpty()) {
                    Text("Each one sold uses 1…", fontWeight = FontWeight.Bold, color = Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill("Nothing", stockId == null) { stockId = null }
                        countable.forEach { m -> Pill(m.name, stockId == m.id) { stockId = m.id } }
                    }
                    Text("For example, pick Cups for every tea so the cup count goes down with each sale.", color = Muted, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), price.toDouble(), stockId) }, enabled = ok) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = Danger) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
