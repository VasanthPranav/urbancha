package com.urbancha.inventory

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.urbancha.inventory.ui.LocalPrefs
import com.urbancha.inventory.ui.Prefs
import com.urbancha.inventory.ui.UrbanChaApp
import com.urbancha.inventory.ui.theme.UrbanChaTheme
import com.urbancha.inventory.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebaseReady = FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null
        val prefs = Prefs(applicationContext)
        setContent {
            val dark = prefs.theme.isDark()
            // Status/navigation bar icons follow the app's Light/Dark setting, not just the phone's.
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            CompositionLocalProvider(LocalPrefs provides prefs) {
                UrbanChaTheme(dark = dark) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (firebaseReady) UrbanChaApp() else FirebaseMissing()
                    }
                }
            }
        }
    }
}

@Composable
private fun FirebaseMissing() {
    Column(Modifier.systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Firebase is not set up yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Add app/google-services.json from your Firebase project and rebuild. The README explains the steps.")
    }
}
