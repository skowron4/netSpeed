package com.example.netspeed.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat

object IconGenerator {
    fun createSpeedIcon(context: Context, speedText: String): IconCompat {
        // Zabezpieczenie na wypadek, gdyby kalkulator zwrócił samo "0"
        val safeText = if (speedText == "0") "0K" else speedText

        // Rozdzielamy tekst na liczbę i jednostkę
        // Np. z "45K" robimy number = "45" oraz unit = "K/s"
        val number = safeText.dropLast(1)
        val unit = safeText.takeLast(1) + "/s"

        // Tworzymy kwadratowy obrazek (128x128 dla lepszej ostrości)
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Pędzel do DUŻEJ liczby na górze
        val numberPaint = Paint().apply {
            color = Color.WHITE
            textSize = 70f // Duża czcionka
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Pędzel do MNIEJSZEJ jednostki na dole
        val unitPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f // Znacznie mniejsza czcionka
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Środek obrazka w poziomie
        val centerX = (canvas.width / 2).toFloat()

        // Pozycje pionowe (wskazują tzw. linię bazową, na której "siedzi" tekst)
        val numberY = 65f // Górna połowa kwadratu
        val unitY = 115f  // Dolna połowa kwadratu

        // Rysujemy obie linie na tym samym obrazku
        canvas.drawText(number, centerX, numberY, numberPaint)
        canvas.drawText(unit, centerX, unitY, unitPaint)

        return IconCompat.createWithBitmap(bitmap)
    }
}