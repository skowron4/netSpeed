package com.example.netspeed

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.netspeed.utils.IconGenerator
import com.example.netspeed.utils.SpeedCalculator
import kotlinx.coroutines.*

class SpeedMonitorService : Service() {

    private val channelId = "SpeedMonitorChannel"
    private val notificationId = 1 // Unikalne ID naszego powiadomienia

    private lateinit var speedCalculator: SpeedCalculator
    private lateinit var notificationManager: NotificationManager

    // Zmienne do obsługi pętli w tle (Korutyn)
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        speedCalculator = SpeedCalculator()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Najpierw musimy stworzyć bazowe powiadomienie z wynikiem 0K
        val initialNotification = createNotification("0K")

        // Zgłaszamy systemowi, że to "ważna usługa" na pierwszym planie
        startForeground(notificationId, initialNotification)

        // Odpalamy naszą pętlę co sekundę
        startUpdatingSpeed()

        return START_STICKY // Jeśli system ubije usługę, ma ją zrestartować
    }

    private fun startUpdatingSpeed() {
        serviceScope.launch {
            while (isActive) {
                // 1. Policz prędkość
                val currentSpeed = speedCalculator.getSpeed()
                // 2. Podmień ikonę
                updateNotification(currentSpeed)
                // 3. Poczekaj 1000 milisekund (1 sekunda)
                delay(500)
            }
        }
    }

    private fun startUpdatingSpeed() {
        serviceScope.launch {
            while (isActive) {
                val currentSpeed = speedCalculator.getSpeed()

                // 1. Aktualizuj powiadomienie
                updateNotification(currentSpeed)

                // 2. Aktualizuj widżet na pulpicie
                updateWidget(currentSpeed)

                delay(500)
            }
        }
    }

    // Nowa funkcja do komunikacji z widżetem
    private fun updateWidget(speedText: String) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, SpeedWidgetProvider::class.java)

        // Tworzymy "paczkę" z nowym wyglądem dla Launchera
        val remoteViews = RemoteViews(packageName, R.layout.widget_speed)
        remoteViews.setTextViewText(R.id.widgetSpeedText, "$speedText/s")

        // Wysyłamy do systemu
        appWidgetManager.updateAppWidget(componentName, remoteViews)
    }

    private fun createNotification(speedText: String): Notification {
        val icon = IconGenerator.createSpeedIcon(this, speedText)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Prędkość sieci")
            .setContentText("$speedText/s")
            .setSmallIcon(icon)
            .setOngoing(true) // Nie da się usunąć przesunięciem palca
            .setOnlyAlertOnce(true) // Żeby nie wibrowało za każdym razem, gdy się aktualizuje!
            .build()
    }

    private fun updateNotification(speedText: String) {
        notificationManager.notify(notificationId, createNotification(speedText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Wskaźnik Prędkości",
                NotificationManager.IMPORTANCE_LOW // Ważne: Low, żeby na ekranie nie wyskakiwały okienka, tylko sama ikona
            ).apply {
                description = "Pokazuje aktualną prędkość internetu na pasku stanu"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Kiedy użytkownik zatrzyma usługę, przerywamy pętlę
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Nie używamy wiązania
    }
}