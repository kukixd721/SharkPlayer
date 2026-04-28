package com.example.mp3.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mp3.LocalStrings
import com.example.mp3.PaletteStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleScreen(
    settings: PlayerSettings,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(strings.colorStyle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Google Sans Flex", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // --- VISTA PREVIA ARTÍSTICA ---
            ThemePreviewCard(settings)
            
            Spacer(modifier = Modifier.height(32.dp))

            // --- SECCIÓN: ORIGEN DEL COLOR ---
            SectionHeader(strings.colorSource, Icons.Default.ColorLens)
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SourceCard(
                    title = strings.wallpaperColors,
                    subtitle = "System accent",
                    selected = !settings.useArtDynamicColor,
                    onClick = { 
                        settings.onDynamicColorChange(true)
                        settings.onUseArtDynamicColorChange(false) 
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Palette
                )
                SourceCard(
                    title = strings.albumArtColors,
                    subtitle = "From music",
                    selected = settings.useArtDynamicColor,
                    onClick = { 
                        settings.onDynamicColorChange(false)
                        settings.onUseArtDynamicColorChange(true) 
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Palette
                )
            }

            // --- OPCIÓN: ÁMBITO DEL COLOR DEL ÁLBUM ---
            if (settings.useArtDynamicColor) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    onClick = { settings.onUseArtDynamicColorOnlyPlayerChange(!settings.useArtDynamicColorOnlyPlayer) },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Solo en el reproductor", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("La app mantendrá los colores del sistema", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.useArtDynamicColorOnlyPlayer,
                            onCheckedChange = { settings.onUseArtDynamicColorOnlyPlayerChange(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- SECCIÓN: ESTILOS DE PALETA ---
            SectionHeader(strings.paletteStyle, Icons.Default.Palette)

            PaletteGrid(settings)
            
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ThemePreviewCard(settings: PlayerSettings) {
    // Animamos los colores para que el cambio sea suave
    val primary by animateColorAsState(MaterialTheme.colorScheme.primary, animationSpec = spring())
    val container by animateColorAsState(MaterialTheme.colorScheme.primaryContainer, animationSpec = spring())
    val onContainer by animateColorAsState(MaterialTheme.colorScheme.onPrimaryContainer, animationSpec = spring())
    val secondary by animateColorAsState(MaterialTheme.colorScheme.secondary, animationSpec = spring())

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(32.dp),
        color = container,
        tonalElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo decorativo con círculos animados
            Box(Modifier.offset(x = (-20).dp, y = (-20).dp).size(150.dp).background(primary.copy(0.1f), CircleShape))
            Box(Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp).size(180.dp).background(secondary.copy(0.1f), CircleShape))

            Column(
                modifier = Modifier.padding(24.dp).align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Barra de progreso simulada (reproductor)
                Box(Modifier.fillMaxWidth(0.8f).height(12.dp).background(onContainer.copy(0.1f), CircleShape)) {
                    Box(Modifier.fillMaxWidth(0.6f).fillMaxHeight().background(primary, CircleShape))
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Botones simulados
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(3) { i ->
                        Box(
                            Modifier
                                .size(if(i==1) 56.dp else 44.dp)
                                .background(if(i==1) primary else onContainer.copy(0.05f), CircleShape)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.fillMaxSize().background(if(i==1) Color.White.copy(0.9f) else primary.copy(0.6f), CircleShape))
                        }
                    }
                }
            }
            
            Text(
                "UI Preview",
                modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SourceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: ImageVector
) {
    val scale by animateFloatAsState(if (selected) 1.05f else 1f)
    val color by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.1f))

    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp).scale(scale),
        shape = RoundedCornerShape(24.dp),
        color = color,
        border = border
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, null, tint = if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PaletteGrid(settings: PlayerSettings) {
    val styles = PaletteStyle.entries
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        styles.chunked(2).forEach { rowStyles ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowStyles.forEach { style ->
                    PaletteItem(
                        style = style,
                        isSelected = settings.paletteStyle == style,
                        onClick = { settings.onPaletteStyleChange(style) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowStyles.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PaletteItem(
    style: PaletteStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val illustrationBrush = when(style) {
        PaletteStyle.TONAL_SPOT -> Brush.linearGradient(listOf(primary, secondary.copy(0.7f)))
        PaletteStyle.VIBRANT -> Brush.sweepGradient(listOf(primary, tertiary, primary))
        PaletteStyle.EXPRESSIVE -> Brush.radialGradient(listOf(secondary, primary))
        PaletteStyle.SPRITZ -> Brush.linearGradient(listOf(primary.copy(0.5f), secondary.copy(0.2f)))
        PaletteStyle.RAINBOW -> Brush.linearGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta))
        PaletteStyle.FRUIT_SALAD -> Brush.verticalGradient(listOf(Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFFF5722)))
    }

    val scale by animateFloatAsState(if (isSelected) 0.98f else 1f)

    Surface(
        onClick = onClick,
        modifier = modifier.height(150.dp).scale(scale),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 12.dp else 2.dp,
        border = if (isSelected) BorderStroke(3.dp, primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.1f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(illustrationBrush)
            ) {
                if (isSelected) {
                    Surface(
                        Modifier.align(Alignment.Center).size(36.dp),
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = primary,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
            Box(
                Modifier.fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    style.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if(isSelected) primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
