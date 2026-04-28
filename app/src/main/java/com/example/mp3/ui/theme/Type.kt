package com.example.mp3.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.mp3.R

// Usamos Google Sans directamente desde los archivos locales (res/font)
// ASEGÚRATE DE QUE LOS ARCHIVOS SE LLAMEN EXACTAMENTE ASÍ Y EN MINÚSCULAS
val GoogleSansFontFamily = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_bold, FontWeight.Bold)
)

fun getTypography(fontName: String): Typography {
    val fontFamily = when (fontName) {
        "Sistema", "System" -> FontFamily.Default
        "Google Sans", "Google Sans Flex", "Outfit" -> GoogleSansFontFamily
        "Monospace" -> FontFamily.Monospace
        "Serif" -> FontFamily.Serif
        else -> GoogleSansFontFamily
    }

    val baseStyle = TextStyle(fontFamily = fontFamily)

    return Typography(
        displayLarge = baseStyle,
        displayMedium = baseStyle,
        displaySmall = baseStyle,
        headlineLarge = baseStyle,
        headlineMedium = baseStyle,
        headlineSmall = baseStyle,
        titleLarge = baseStyle,
        titleMedium = baseStyle,
        titleSmall = baseStyle,
        bodyLarge = baseStyle,
        bodyMedium = baseStyle,
        bodySmall = baseStyle,
        labelLarge = baseStyle,
        labelMedium = baseStyle,
        labelSmall = baseStyle
    )
}

val Typography = getTypography("Google Sans")
