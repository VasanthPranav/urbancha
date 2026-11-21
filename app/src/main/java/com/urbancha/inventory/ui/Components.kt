package com.urbancha.inventory.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urbancha.inventory.ui.theme.Line
import com.urbancha.inventory.ui.theme.Muted
import com.urbancha.inventory.ui.theme.TeaGreen

@Composable
fun Header(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = if (onBack != null) 8.dp else 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = Muted, fontWeight = FontWeight.SemiBold)
        }
        trailing?.invoke()
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, container: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = if (container == Color.White) BorderStroke(1.dp, Line) else null,
    ) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TeaGreen,
            selectedLabelColor = Color.White,
        ),
    )
}

/** Decimal-number text field. Keeps the raw text so the user can type "2." on the way to "2.5". */
@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String? = null, suffix: String? = null, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.isEmpty() || v.matches(Regex("""\d*\.?\d*"""))) onChange(v) },
        label = label?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it, color = Muted) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    )
}

@Composable
fun StatusDot(color: Color) {
    Box(Modifier.size(8.dp).background(color, RoundedCornerShape(999.dp)))
}
