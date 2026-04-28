package com.example.mp3.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.mp3.PaletteStyle

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// --- Colores para el tema rosa (Light Mode) ---
val PinkBackground = Color(0xFFFEF1F4)
val PinkContainer = Color(0xFFFCE4E9)
val PinkPrimary = Color(0xFFD81B60)
val PinkSecondary = Color(0xFFAD1457)
val PinkTertiary = Color(0xFF880E4F)

// Tonos neutros
val Slate900 = Color(0xFF0F172A)
val Slate100 = Color(0xFFF1F5F9)

/**
 * Ajusta el color para que sea FIEL a la carátula.
 * Se han eliminado las restricciones que hacían que el color se viera opaco o gris.
 */
fun blendWithNeutral(source: Color, isDark: Boolean, style: PaletteStyle = PaletteStyle.TONAL_SPOT): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(source.toArgb(), hsl)
    
    // Multiplicadores que respetan la intensidad original
    val (satMult, lumMult) = when (style) {
        PaletteStyle.VIBRANT -> 1.1f to 1.1f
        PaletteStyle.EXPRESSIVE -> 1.0f to 1.0f
        PaletteStyle.RAINBOW -> 1.0f to 1.2f
        PaletteStyle.FRUIT_SALAD -> 0.9f to 1.1f
        PaletteStyle.TONAL_SPOT -> 0.7f to 1.0f
        PaletteStyle.SPRITZ -> 0.3f to 1.0f
    }

    hsl[1] = (hsl[1] * satMult).coerceIn(0f, 1f)
    
    if (isDark) {
        // Aumentamos el rango de luminancia para que no se vea "opaco"
        // Permitimos que el color sea más brillante en modo oscuro
        hsl[2] = (hsl[2] * lumMult).coerceIn(0.15f, 0.65f)
    } else {
        // En modo claro, aseguramos que sea vibrante pero legible
        hsl[2] = (hsl[2] * lumMult).coerceIn(0.40f, 0.90f)
    }

    return Color(ColorUtils.HSLToColor(hsl))
}
