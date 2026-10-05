package com.example.netspeed.utils

import android.net.TrafficStats
import android.os.SystemClock

class SpeedCalculator {
    // Prosta klasa do trzymania historii pomiarów (ile bajtów było o której godzinie)
    private class Measurement(val bytes: Long, val time: Long)

    // Lista, w której będziemy trzymać historię z ostatnich 3 sekund
    private val history = mutableListOf<Measurement>()

    init {
        // Zapisujemy pierwszy pomiar przy starcie
        history.add(Measurement(TrafficStats.getTotalRxBytes(), SystemClock.elapsedRealtime()))
    }

    fun getSpeed(): String {
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTime = SystemClock.elapsedRealtime()

        // 1. Dodajemy aktualny odczyt do historii
        history.add(Measurement(currentRxBytes, currentTime))

        // 2. Wyrzucamy z historii bardzo stare pomiary (starsze niż 3000 milisekund = 3 sekundy)
        history.removeAll { currentTime - it.time > 3000 }

        // 3. Bierzemy najstarszy punkt, jaki nam został w oknie 3-sekundowym
        val oldest = history.first()

        // 4. Obliczamy różnicę od tego najstarszego punktu do teraz
        val rxDiff = currentRxBytes - oldest.bytes
        val timeDiff = currentTime - oldest.time

        if (timeDiff == 0L) return "0K" // Zabezpieczenie

        // Obliczamy uśrednioną prędkość (Double dla precyzji)
        val speedKbps = (rxDiff.toDouble() * 1000.0) / timeDiff.toDouble() / 1024.0

        // Formatowanie wyniku
        return when {
            speedKbps >= 1048576.0 -> String.format("%.1f", speedKbps / 1048576.0) + "G"
            speedKbps >= 1024.0 -> String.format("%.1f", speedKbps / 1024.0) + "M"
            speedKbps > 0.0 -> "${speedKbps.toInt()}K"
            else -> "0K"
        }
    }
}