package com.urbancha.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.urbancha.inventory.ui.UrbanChaApp
import com.urbancha.inventory.ui.theme.UrbanChaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebaseReady = FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null
        setContent {
            UrbanChaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (firebaseReady) UrbanChaApp() else FirebaseMissing()
                }
            }
        }
    }
}

@Composable
private fun FirebaseMissing() {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Firebase is not set up yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Add app/google-services.json from your Firebase project and rebuild. The README explains the steps.")
    }
}
