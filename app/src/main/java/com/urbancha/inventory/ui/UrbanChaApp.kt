package com.urbancha.inventory.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbancha.inventory.ui.screens.AnalyticsScreen
import com.urbancha.inventory.ui.screens.CloseShiftScreen
import com.urbancha.inventory.ui.screens.HomeScreen
import com.urbancha.inventory.ui.screens.OpeningStockScreen
import com.urbancha.inventory.ui.screens.ProductsScreen
import com.urbancha.inventory.ui.screens.ReportsScreen
import com.urbancha.inventory.ui.screens.SellScreen
import com.urbancha.inventory.ui.screens.StockScreen
import com.urbancha.inventory.ui.theme.SurfaceBg
import com.urbancha.inventory.ui.theme.TeaGreen

enum class Screen(val label: String, val icon: ImageVector?) {
    Home("Home", Icons.Outlined.Home),
    Sell("Sell", Icons.AutoMirrored.Outlined.ReceiptLong),
    Stock("Stock", Icons.Outlined.Inventory2),
    Reports("Reports", Icons.Outlined.BarChart),
    Analytics("Analytics", Icons.Outlined.PieChart),
    Products("Products", Icons.Outlined.LocalCafe),
    Opening("Opening stock", null),
    Close("Close shift", null),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UrbanChaApp(vm: AppViewModel = viewModel()) {
    val user by vm.user.collectAsState()
    if (user == null) {
        SignInScreen(vm)
        return
    }

    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    val go: (Screen) -> Unit = { screen = it }
    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    // While the keyboard is open, hide the tabs so they can't sit on top of the form being typed into.
    val keyboardOpen = WindowInsets.isImeVisible

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!keyboardOpen) NavigationBar(containerColor = SurfaceBg) {
                Screen.entries.filter { it.icon != null }.forEach { s ->
                    NavigationBarItem(
                        selected = screen == s,
                        onClick = { screen = s },
                        icon = { Icon(s.icon!!, contentDescription = null) },
                        label = { Text(s.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1) },
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.padding(inner).consumeWindowInsets(inner).imePadding().fillMaxSize()) {
            when (screen) {
                Screen.Home -> HomeScreen(vm, go)
                Screen.Sell -> SellScreen(vm)
                Screen.Stock -> StockScreen(vm, go)
                Screen.Reports -> ReportsScreen(vm)
                Screen.Analytics -> AnalyticsScreen(vm)
                Screen.Products -> ProductsScreen(vm)
                Screen.Opening -> OpeningStockScreen(vm, onDone = { screen = Screen.Home })
                Screen.Close -> CloseShiftScreen(vm, onDone = { screen = Screen.Reports })
            }
        }
    }
}

@Composable
private fun SignInScreen(vm: AppViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var create by rememberSaveable { mutableStateOf(false) }
    val busy by vm.authBusy.collectAsState()
    val error by vm.authError.collectAsState()

    // Scrolls, and pads for the keyboard, so the keyboard can't squeeze the form.
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Text("Urban Cha", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = TeaGreen)
        Text(
            if (create) "Create the shop account once. Then sign in with the same email on every phone so they all share the same stock and sales."
            else "Sign in with the shop account. Every phone using this account sees the same stock and sales.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Shop email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password (6+ characters)") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        Button(
            onClick = { vm.signIn(email, password, create) },
            enabled = !busy && email.isNotBlank() && password.length >= 6,
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            if (busy) CircularProgressIndicator(Modifier.height(22.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (create) "Create shop account" else "Sign in", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = { create = !create }, modifier = Modifier.fillMaxWidth()) {
            Text(if (create) "I already have an account" else "First time? Create the shop account")
        }
    }
}
