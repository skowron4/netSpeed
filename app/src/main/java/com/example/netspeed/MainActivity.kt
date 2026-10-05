package com.example.netspeed

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.netspeed.ui.theme.NetSpeedTheme
import androidx.activity.compose.rememberLauncherForActivityResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetSpeedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    // Zmienna trzymająca "kontekst" aplikacji (potrzebny do uruchamiania usług)
    val context = LocalContext.current

    // Zapamiętujemy stan przycisku (czy usługa jest włączona, czy wyłączona)
    var isServiceRunning by remember { mutableStateOf(false) }

    // Rejestrujemy systemowe okienko z prośbą o uprawnienia do powiadomień (dla Androida 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startSpeedService(context)
            isServiceRunning = true
        } else {
            // Tutaj w przyszłości można dodać komunikat, że bez uprawnień apka nie zadziała
        }
    }

    // Budujemy wygląd ekranu (Column układa elementy pionowo, jeden pod drugim)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Miernik Prędkości",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = {
                if (isServiceRunning) {
                    // Wyłączamy usługę
                    val intent = Intent(context, SpeedMonitorService::class.java)
                    context.stopService(intent)
                    isServiceRunning = false
                } else {
                    // Jeśli Android 13 lub nowszy -> najpierw zapytaj o powiadomienia
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Jeśli starszy Android -> uruchom od razu
                        startSpeedService(context)
                        isServiceRunning = true
                    }
                }
            }
        ) {
            // Tekst na przycisku zmienia się w zależności od stanu
            Text(text = if (isServiceRunning) "Zatrzymaj" else "Uruchom")
        }
    }
}

// Funkcja pomocnicza do bezpiecznego uruchamiania usługi działającej w tle
fun startSpeedService(context: Context) {
    val intent = Intent(context, SpeedMonitorService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}