package com.example.mp3.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mp3.AppStrings
import com.example.mp3.LocalStrings
import com.example.mp3.PaletteStyle
import com.example.mp3.ui.components.SettingsSwitchCard
import com.example.mp3.ui.theme.blendWithNeutral

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleScreen(
    settings: PlayerSettings,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val isDark = when (settings.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val curatedPalettes = listOf(
        listOf(Color(0xFFB4C5FF), Color(0xFFDDE1FF), Color(0xFFFFD9E3)),
        listOf(Color(0xFFA2AD94), Color(0xFFCFD6C5), Color(0xFFE2E2E2)),
        listOf(Color(0xFFE4C4A3), Color(0xFFF3E5D8), Color(0xFFD4E4D1)),
        listOf(Color(0xFFA0D1D1), Color(0xFFC7E6E6), Color(0xFFF4D7D7)),
        listOf(Color(0xFFE8B7B7), Color(0xFFF5DADA), Color(0xFFDCE2F9)),
        listOf(Color(0xFF607D8B), Color(0xFF90A4AE), Color(0xFFCFD8DC))
    )

    // Obtenemos la SEMILLA de color pura (sin mezclar) para que los iconos de estilo sean estables
    val currentSeedColor = remember(settings.useArtDynamicColor, settings.selectedCuratedPalette, settings.artPrimaryColor, settings.customAccentColor) {
        when {
            settings.useArtDynamicColor -> settings.artPrimaryColor
            settings.selectedCuratedPalette != -1 -> curatedPalettes[settings.selectedCuratedPalette][0]
            else -> settings.customAccentColor
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.colorStyle, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- VISTA PREVIA DEL TEMA ---
            ThemePreviewCard(settings)
            
            Spacer(modifier = Modifier.height(32.dp))

            // --- SECCIÓN: ESTILOS DE PALETA ---
            Text(
                strings.paletteStyle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
            )

            PaletteGrid(settings, isDark, currentSeedColor)

            Spacer(modifier = Modifier.height(32.dp))

            // --- SECCIÓN: TONOS DEL TEMA (PRESETS) ---
            Text(
                strings.presetColors,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
            )
            CuratedPaletteList(settings, strings, isDark, curatedPalettes)

            Spacer(modifier = Modifier.height(32.dp))

            // --- SECCIÓN: PERSONALIZACIÓN EXTRA ---
            Text(
                strings.visualCustomization,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
            )

            SettingsSwitchCard(
                icon = Icons.Default.AutoAwesome,
                title = strings.unifiedLyricsBackground,
                subtitle = strings.unifiedLyricsBackgroundDesc,
                isChecked = settings.unifiedLyricsBackground,
                onCheckedChange = settings.onUnifiedLyricsBackgroundChange
            )
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ThemePreviewCard(settings: PlayerSettings) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val container = MaterialTheme.colorScheme.primaryContainer
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(32.dp),
        color = container,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo decorativo con gradiente sutil
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primary.copy(0.12f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = 450f
                        )
                    )
            )

            // Simulación de Interfaz
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // "Barra de estado" simulada
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(24.dp).background(primary.copy(0.2f), CircleShape))
                    Box(Modifier.width(80.dp).height(12.dp).background(primary.copy(0.1f), CircleShape))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(16.dp).background(secondary.copy(0.3f), CircleShape))
                        Box(Modifier.size(16.dp).background(tertiary.copy(0.3f), CircleShape))
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Elemento principal resaltado
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = primary,
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            null,
                            modifier = Modifier.padding(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.width(140.dp).height(14.dp).background(onContainer, CircleShape))
                        Box(Modifier.width(90.dp).height(10.dp).background(onContainer.copy(0.5f), CircleShape))
                    }
                }

                // Chips / Botones secundarios
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = secondary.copy(0.15f), border = BorderStroke(1.dp, secondary.copy(0.2f))) {
                        Box(Modifier.width(70.dp).height(28.dp))
                    }
                    Surface(shape = CircleShape, color = tertiary.copy(0.15f), border = BorderStroke(1.dp, tertiary.copy(0.2f))) {
                        Box(Modifier.width(70.dp).height(28.dp))
                    }
                }
            }

            // FAB (Floating Action Button) simulado
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Etiqueta de "Vista Previa"
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                color = primary.copy(0.1f),
                shape = CircleShape
            ) {
                Text(
                    "Vista Previa",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = primary
                )
            }
        }
    }
}


@Composable
fun PaletteGrid(settings: PlayerSettings, isDark: Boolean, baseColor: Color) {
    val styles = PaletteStyle.entries
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        styles.chunked(3).forEach { rowStyles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowStyles.forEach { style ->
                    PaletteItem(
                        style = style,
                        isSelected = settings.paletteStyle == style,
                        onClick = { settings.onPaletteStyleChange(style) },
                        isDark = isDark,
                        baseColor = baseColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowStyles.size < 3) {
                    repeat(3 - rowStyles.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun PaletteItem(
    style: PaletteStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    baseColor: Color,
    modifier: Modifier
) {
    // Sincronizamos la previsualización con la lógica real de blendWithNeutral
    val stylePreview = remember(style, baseColor, isDark) {
        val primary = blendWithNeutral(baseColor, isDark, style)
        
        // Derivamos tonos secundarios/terciarios para el icono basados en el primario real
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(primary.toArgb(), hsl)
        
        // Creamos una mini-paleta armónica para el icono circular
        val secondary = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hsl[0], (hsl[1] * 0.8f).coerceIn(0f, 1f), (hsl[2] * 1.1f).coerceIn(0f, 1f))))
        val tertiary = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hsl[0] + 20) % 360, (hsl[1] * 0.6f).coerceIn(0f, 1f), (hsl[2] * 1.2f).coerceAtMost(0.95f))))
        val neutral = if (isDark) Color(0xFF353535) else Color(0xFFF5F5F5)

        listOf(primary, secondary, tertiary, neutral)
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(76.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primaryColor,
                        radius = (size.minDimension / 2f) - 1.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }
            }
            
            Canvas(modifier = Modifier.size(54.dp)) {
                drawArc(stylePreview[0], 180f, 90f, true)
                drawArc(stylePreview[1], 270f, 90f, true)
                drawArc(stylePreview[2], 0f, 90f, true)
                drawArc(stylePreview[3], 90f, 90f, true)
            }
        }
        
        Spacer(Modifier.height(4.dp))
        Text(
            style.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) primaryColor else onSurface.copy(alpha = 0.6f),
            maxLines = 1
        )
    }
}

@Composable
fun CuratedPaletteList(settings: PlayerSettings, strings: AppStrings, isDark: Boolean, curatedPalettes: List<List<Color>>) {
    val neutral = if (isDark) Color(0xFF303030) else Color(0xFFE2E2E2)
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    val paletteNames = listOf(
        strings.paletteLavender,
        strings.paletteForest,
        strings.paletteDune,
        strings.paletteReef,
        strings.palettePetal,
        strings.paletteSlate
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Opción: Carátula (Dinámico)
        PaletteOptionItem(
            title = strings.albumArtColors,
            colors = listOf(settings.artPrimaryColor, settings.artSecondaryColor, settings.artTertiaryColor, neutral),
            isSelected = settings.useArtDynamicColor,
            onClick = { settings.onUseArtDynamicColorChange(true) },
            primary = primary,
            onSurface = onSurface
        )

        // Botón "Automático" (Nativo/Sistema)
        val isAuto = settings.selectedCuratedPalette == -1 && !settings.useArtDynamicColor
        val systemPreviewColors = if (isDark) {
            listOf(Color(0xFFD0BCFF), Color(0xFFCCC2DC), Color(0xFFEFB8C8), neutral)
        } else {
            listOf(Color(0xFF6750A4), Color(0xFF625B71), Color(0xFF7D5260), neutral)
        }
        
        PaletteOptionItem(
            title = "Automático",
            colors = systemPreviewColors,
            isSelected = isAuto,
            onClick = { 
                settings.onSelectedCuratedPaletteChange(-1)
                settings.onUseArtDynamicColorChange(false)
            },
            primary = primary,
            onSurface = onSurface,
            icon = Icons.Default.Palette
        )

        // Paletas Curadas
        curatedPalettes.forEachIndexed { index, palette ->
            val isSelected = settings.selectedCuratedPalette == index && !settings.useArtDynamicColor
            PaletteOptionItem(
                title = paletteNames.getOrElse(index) { "" },
                colors = palette + neutral,
                isSelected = isSelected,
                onClick = { 
                    settings.onSelectedCuratedPaletteChange(index)
                    settings.onUseArtDynamicColorChange(false)
                },
                primary = primary,
                onSurface = onSurface
            )
        }
    }
}

@Composable
fun PaletteOptionItem(
    title: String,
    colors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit,
    primary: Color,
    onSurface: Color,
    icon: ImageVector? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primary,
                        radius = (size.minDimension / 2f) - 1.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
            
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(54.dp)) {
                    drawArc(colors[0], 180f, 90f, true)
                    drawArc(colors[1], 270f, 90f, true)
                    drawArc(colors[2], 0f, 90f, true)
                    drawArc(colors[3], 90f, 90f, true)
                }
                if (icon != null) {
                    Icon(
                        icon, 
                        null, 
                        modifier = Modifier.size(20.dp), 
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) primary else onSurface.copy(alpha = 0.7f)
        )
    }
}
