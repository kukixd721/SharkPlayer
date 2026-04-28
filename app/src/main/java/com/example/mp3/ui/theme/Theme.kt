package com.example.mp3.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.example.mp3.ui.screens.PlayerSettings

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF00FF), // MAGENTA CHILLÓN PARA PROBAR
    onPrimary = Color.White,
    background = Color(0xFFFF00FF), // FONDO MAGENTA
    surface = Color(0xFFFF00FF),
    onSurface = Color.White
)

@Composable
fun Mp3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun rememberCustomColorScheme(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    isDark: Boolean,
    paletteStyle: com.example.mp3.PaletteStyle = com.example.mp3.PaletteStyle.TONAL_SPOT
): ColorScheme {
    return remember(primary, secondary, tertiary, isDark, paletteStyle) {
        val p = blendWithNeutral(primary, isDark, paletteStyle)
        val s = blendWithNeutral(secondary, isDark, paletteStyle)
        val t = blendWithNeutral(tertiary, isDark, paletteStyle)

        val (backgroundTone, surfaceBase, onSurfaceColor) = (if (isDark) {
            // Fondo con VIDA: Reducimos el lerp con negro para que el color real se note.
            val bg = lerp(p, Color.Black, 0.78f)
            val surf = lerp(p, Color.Black, 0.72f)
            // Calculamos onSurface basándonos en la luminancia del fondo para asegurar contraste
            // Aumentamos al 55% la inyección de color para que el tinte sea PROTAGONISTA
            val baseOnColor = if (bg.luminance() > 0.45f) Color.Black else Color.White
            val onSurf = lerp(baseOnColor, p, 0.55f)
            Triple(bg, surf, onSurf)
        } else {
            // Modo claro: Más vibrante y saturado
            val bg = lerp(p, Color.White, 0.92f)
            val surf = lerp(p, Color.White, 0.85f)
            val baseOnColor = if (bg.luminance() > 0.5f) Color.Black else Color.White
            val onSurf = lerp(baseOnColor, p, 0.45f)
            Triple(bg, surf, onSurf)
        })

        val contrastColor = { color: Color ->
            if (color.luminance() > 0.5f) Color.Black else Color.White
        }

        if (isDark) {
            val darkPrimary = lerp(p, Color.White, 0.35f)
            val darkSecondary = lerp(s, Color.White, 0.3f)
            val darkPrimaryContainer = p.copy(alpha = 0.45f)
            val darkSecondaryContainer = s.copy(alpha = 0.4f)
            
            darkColorScheme(
                primary = darkPrimary,
                onPrimary = contrastColor(darkPrimary),
                primaryContainer = darkPrimaryContainer,
                onPrimaryContainer = Color.White,
                secondary = darkSecondary,
                onSecondary = contrastColor(darkSecondary),
                secondaryContainer = darkSecondaryContainer,
                onSecondaryContainer = Color.White,
                tertiary = t,
                onTertiary = contrastColor(t),
                background = backgroundTone,
                surface = surfaceBase,
                surfaceVariant = lerp(s, surfaceBase, 0.45f),
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = lerp(surfaceBase, p, 0.15f),
                surfaceContainer = lerp(surfaceBase, p, 0.28f),
                surfaceContainerHigh = lerp(surfaceBase, p, 0.40f),
                surfaceContainerHighest = lerp(surfaceBase, p, 0.55f),
                onSurface = onSurfaceColor,
                onSurfaceVariant = onSurfaceColor.copy(alpha = 0.8f),
                onBackground = onSurfaceColor,
                outline = p.copy(alpha = 0.7f),
                outlineVariant = p.copy(alpha = 0.5f)
            )
        } else {
            val lightPrimaryContainer = lerp(p, Color.White, 0.65f)
            val lightSecondaryContainer = lerp(s, Color.White, 0.70f)
            
            lightColorScheme(
                primary = p,
                onPrimary = contrastColor(p),
                primaryContainer = lightPrimaryContainer,
                onPrimaryContainer = contrastColor(lightPrimaryContainer),
                secondary = s,
                onSecondary = contrastColor(s),
                secondaryContainer = lightSecondaryContainer,
                tertiary = t,
                onTertiary = contrastColor(t),
                background = backgroundTone,
                surface = surfaceBase,
                surfaceVariant = lerp(s, surfaceBase, 0.45f),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = lerp(surfaceBase, p, 0.08f),
                surfaceContainer = lerp(surfaceBase, p, 0.15f),
                surfaceContainerHigh = lerp(surfaceBase, p, 0.25f),
                surfaceContainerHighest = lerp(surfaceBase, p, 0.35f),
                onSurface = onSurfaceColor,
                onBackground = onSurfaceColor,
                onSurfaceVariant = lerp(p, Color.Black, 0.75f),
                outline = s.copy(alpha = 0.6f),
                outlineVariant = s.copy(alpha = 0.4f)
            )
        }
    }
}
