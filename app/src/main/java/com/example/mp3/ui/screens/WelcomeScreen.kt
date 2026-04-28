package com.example.mp3.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mp3.Language
import com.example.mp3.LocalStrings
import com.example.mp3.ui.components.SettingsSwitchCard

data class WelcomeState(
    val useDynamicColor: Boolean,
    val onDynamicColorChange: (Boolean) -> Unit,
    val useArtDynamicColor: Boolean,
    val onUseArtDynamicColorChange: (Boolean) -> Unit,
    val karaokeEnabled: Boolean,
    val onKaraokeChange: (Boolean) -> Unit,
    val selectedLanguage: Language,
    val onLanguageChange: (Language) -> Unit,
    val isScanning: Boolean,
    val scanProgress: Float,
    val scanStatus: String,
    val onStartScan: () -> Unit,
    val onFinish: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    state: WelcomeState
) {
    var step by remember { mutableIntStateOf(1) }
    val strings = LocalStrings.current

    // Fondo difuminado o semitransparente que cubre la pantalla
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        // Ventana flotante (Card)
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 40.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val springSpec = spring<IntOffset>(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                        val scaleSpec = spring<Float>(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )

                        if (targetState > initialState) {
                            (slideInHorizontally(springSpec) { it } + fadeIn() + scaleIn(initialScale = 0.7f, animationSpec = scaleSpec))
                                .togetherWith(slideOutHorizontally(springSpec) { -it } + fadeOut() + scaleOut(targetScale = 0.7f, animationSpec = scaleSpec))
                        } else {
                            (slideInHorizontally(springSpec) { -it } + fadeIn() + scaleIn(initialScale = 0.7f, animationSpec = scaleSpec))
                                .togetherWith(slideOutHorizontally(springSpec) { it } + fadeOut() + scaleOut(targetScale = 0.7f, animationSpec = scaleSpec))
                        }.using(
                            SizeTransform(clip = false)
                        )
                    }, label = "WelcomeStep"
                ) { currentStep ->
                    when (currentStep) {
                        1 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Icono con resplandor (AHORA ES EL TIBURÓN)
                                Surface(
                                    modifier = Modifier.size(100.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    shadowElevation = 8.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = painterResource(id = com.example.mp3.R.drawable.shark),
                                            contentDescription = "Shark Logo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = strings.sharkPlayer,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = strings.welcomeSubtitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Button(
                                    onClick = { step = 2 },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(strings.start, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                    }
                                }
                            }
                        }
                        2 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { step = 1 }) {
                                        Icon(Icons.Default.ChevronLeft, null)
                                    }
                                    Text(
                                        text = strings.selectLanguage,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.width(48.dp))
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Language.entries.forEach { lang ->
                                    val isSelected = state.selectedLanguage == lang
                                    Surface(
                                        onClick = { state.onLanguageChange(lang) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                else Color.Transparent,
                                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                                 else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Translate,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = lang.label,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Button(
                                    onClick = { step = 3 },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(strings.next, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                    }
                                }
                            }
                        }
                        3 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { step = 2 }) {
                                        Icon(Icons.Default.ChevronLeft, null)
                                    }
                                    Text(
                                        text = "Descubre Shark Player",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.width(48.dp))
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                FeatureItem(
                                    icon = Icons.Default.GraphicEq,
                                    title = strings.equalizerPro,
                                    desc = strings.equalizerDesc,
                                    color = Color(0xFF4CAF50)
                                )
                                FeatureItem(
                                    icon = Icons.Default.BarChart,
                                    title = strings.audioVisualizer,
                                    desc = strings.audioVisualizerDesc,
                                    color = Color(0xFF2196F3)
                                )
                                FeatureItem(
                                    icon = Icons.Default.Download,
                                    title = strings.downloadMusic,
                                    desc = strings.downloadMusicDesc,
                                    color = Color(0xFFFF9800)
                                )
                                FeatureItem(
                                    icon = Icons.Default.AutoFixHigh,
                                    title = "Personalización",
                                    desc = "Efectos glass, fuentes y colores dinámicos.",
                                    color = Color(0xFFE91E63)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { step = 4 },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(strings.next, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                    }
                                }
                            }
                        }
                        4 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { step = 3 }) {
                                        Icon(Icons.Default.ChevronLeft, null)
                                    }
                                    Text(
                                        text = strings.appearance,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.width(48.dp))
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                SettingsSwitchCard(
                                    icon = Icons.Default.Palette,
                                    title = strings.dynamicColor,
                                    subtitle = strings.dynamicColorDesc,
                                    isChecked = state.useDynamicColor,
                                    onCheckedChange = {
                                        state.onDynamicColorChange(it)
                                        if (it) state.onUseArtDynamicColorChange(false)
                                    },
                                    withSurface = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsSwitchCard(
                                    icon = Icons.Default.ColorLens,
                                    title = strings.artDynamicColor,
                                    subtitle = strings.artDynamicColorDesc,
                                    isChecked = state.useArtDynamicColor,
                                    onCheckedChange = {
                                        state.onUseArtDynamicColorChange(it)
                                        if (it) state.onDynamicColorChange(false)
                                    },
                                    withSurface = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsSwitchCard(
                                    icon = Icons.Default.Lyrics,
                                    title = strings.karaokeMode,
                                    subtitle = strings.karaokeModeDesc,
                                    isChecked = state.karaokeEnabled,
                                    onCheckedChange = state.onKaraokeChange,
                                    withSurface = true
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Button(
                                    onClick = {
                                        step = 5
                                        state.onStartScan()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(strings.startScan, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        5 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (state.isScanning) strings.analyzingLibrary else strings.analysisCompleted,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(40.dp))

                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                                    CircularProgressIndicator(
                                        progress = { state.scanProgress },
                                        modifier = Modifier.fillMaxSize(),
                                        strokeWidth = 10.dp,
                                        strokeCap = StrokeCap.Round,
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${(state.scanProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        if (!state.isScanning) {
                                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.scanStatus,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .basicMarquee(),
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Button(
                                    onClick = state.onFinish,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    enabled = !state.isScanning,
                                    colors = if (!state.isScanning) ButtonDefaults.buttonColors()
                                    else ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = if (state.isScanning) strings.analyzingLibrary else strings.start,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

