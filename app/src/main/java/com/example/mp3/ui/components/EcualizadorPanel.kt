package com.example.mp3.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.mp3.LocalStrings
import com.example.mp3.PlaybackService
import java.util.Locale

@Composable
fun EcualizadorPanel() {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val audioPrefs = remember { context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE) }
    
    val notifyService: () -> Unit = {
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = "UPDATE_EQ"
        }
        context.startService(intent)
    }

    var isEnabled by remember { mutableStateOf(audioPrefs.getBoolean("eq_enabled", false)) }
    var resetTrigger by remember { mutableIntStateOf(0) }

    val presets = mapOf(
        "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Rock" to listOf(4f, 3f, -1f, -2f, 0f, 1f, 3f, 4f, 4f, 5f),
        "Pop" to listOf(-1f, 1f, 2f, 3f, 1f, -1f, -1f, 0f, 1f, 1f),
        "Jazz" to listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f),
        "Electronic" to listOf(5f, 4f, 1f, 0f, -2f, 2f, 1f, 2f, 4f, 5f),
        "Bass Boost" to listOf(6f, 5f, 4f, 2f, 0f, -1f, -2f, -1f, 0f, 1f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- HEADER EXPRESSIVE ---
        Surface(
            color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            null,
                            tint = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        strings.equalizerPro.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        if (isEnabled) strings.effectsActive else strings.effectsInactive,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                
                Switch(
                    checked = isEnabled,
                    onCheckedChange = {
                        isEnabled = it
                        audioPrefs.edit { putBoolean("eq_enabled", it) }
                        notifyService()
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = isEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                
                // --- PRESET SELECTOR ---
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    val presetList = presets.keys.toList()
                    items(presetList.size) { index ->
                        val presetName = presetList[index]
                        FilterChip(
                            selected = false,
                            onClick = {
                                val values = presets[presetName]!!
                                audioPrefs.edit {
                                    values.forEachIndexed { i, v -> putFloat("eq_band_$i", v) }
                                }
                                resetTrigger++
                                notifyService()
                            },
                            label = { Text(presetName) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // --- VISUAL CURVE PREVIEW ---
                EqCurvePreview(audioPrefs, resetTrigger)

                // --- BANDS MIXER ---
                BandMixer(audioPrefs, resetTrigger, notifyService)

                // --- ENHANCEMENTS GRID ---
                EnhancementCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = strings.superBass,
                    icon = Icons.Rounded.Waves,
                    value = audioPrefs.getInt("bass_strength", 0).toFloat(),
                    range = 0f..1000f,
                    onValueChange = {
                        audioPrefs.edit { putInt("bass_strength", it.toInt()) }
                        notifyService()
                    },
                    resetTrigger = resetTrigger,
                    color = MaterialTheme.colorScheme.primary
                )

                // --- DYNAMICS DECK ---
                DynamicsDeck(audioPrefs, resetTrigger, notifyService, strings)

                // --- MASTER RESET ---
                OutlinedButton(
                    onClick = {
                        audioPrefs.edit {
                            for (i in 0 until 10) putFloat("eq_band_$i", 0f)
                            putInt("bass_strength", 0)
                            putFloat("output_gain", 0f)
                            putFloat("limiter_threshold", -0.1f)
                        }
                        resetTrigger++
                        notifyService()
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(12.dp))
                    Text(strings.reset.uppercase(), fontWeight = FontWeight.Black)
                }
            }
        }

        if (!isEnabled) {
            EmptyState(strings.activateToAdjust)
        }
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun EqCurvePreview(prefs: android.content.SharedPreferences, resetTrigger: Int) {
    val levels = (0 until 10).map { i ->
        remember(i, resetTrigger) { mutableFloatStateOf(prefs.getFloat("eq_band_$i", 0f)) }.floatValue
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Surface(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        color = surfaceColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
            val width = size.width
            val height = size.height
            val midY = height / 2
            val stepX = width / (levels.size - 1)
            
            val path = Path().apply {
                levels.forEachIndexed { i, level ->
                    val y = midY - (level / 12f) * (height / 2)
                    if (i == 0) moveTo(0f, y) else {
                        val prevY = midY - (levels[i-1] / 12f) * (height / 2)
                        val prevX = (i - 1) * stepX
                        val currX = i * stepX
                        cubicTo(
                            (prevX + currX) / 2, prevY,
                            (prevX + currX) / 2, y,
                            currX, y
                        )
                    }
                }
            }

            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent)
                )
            )

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            
            levels.forEachIndexed { i, level ->
                val y = midY - (level / 12f) * (height / 2)
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(i * stepX, y)
                )
            }
        }
    }
}

@Composable
fun BandMixer(prefs: android.content.SharedPreferences, resetTrigger: Int, onUpdate: () -> Unit) {
    val frequencies = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                itemsIndexed(frequencies) { i, freq ->
                    var level by remember(i, resetTrigger) {
                        mutableFloatStateOf(prefs.getFloat("eq_band_$i", 0f))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(56.dp)
                    ) {
                        Text(
                            "${level.toInt()}dB",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (level != 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        
                        Box(
                            modifier = Modifier.height(200.dp).padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.width(8.dp).fillMaxHeight().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                            
                            Slider(
                                value = level,
                                onValueChange = { level = it },
                                onValueChangeFinished = {
                                    prefs.edit { putFloat("eq_band_$i", level) }
                                    onUpdate()
                                },
                                valueRange = -12f..12f,
                                modifier = Modifier.graphicsLayer { rotationZ = -90f }.width(200.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent
                                )
                            )
                        }
                        
                        Text(
                            freq,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancementCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    resetTrigger: Int,
    color: Color
) {
    var sliderValue by remember(value, resetTrigger) { mutableFloatStateOf(value) }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(2.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = color)
            }
            Spacer(Modifier.height(16.dp))
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onValueChange(sliderValue) },
                valueRange = range,
                colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
            )
            Text(
                "${((sliderValue / range.endInclusive) * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = color,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun DynamicsDeck(prefs: android.content.SharedPreferences, resetTrigger: Int, onUpdate: () -> Unit, strings: com.example.mp3.AppStrings) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(strings.dynamicsProcessing.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)

            DynamicControl(strings.gain, prefs.getFloat("output_gain", 0f), -10f..10f, resetTrigger, MaterialTheme.colorScheme.secondary) {
                prefs.edit { putFloat("output_gain", it) }
                onUpdate()
            }
            DynamicControl(strings.limit, prefs.getFloat("limiter_threshold", -0.1f), -40f..0f, resetTrigger, MaterialTheme.colorScheme.secondary) {
                prefs.edit { putFloat("limiter_threshold", it) }
                onUpdate()
            }
        }
    }
}

@Composable
fun DynamicControl(label: String, value: Float, range: ClosedFloatingPointRange<Float>, resetTrigger: Int, color: Color, onUpdate: (Float) -> Unit) {
    var sliderValue by remember(value, resetTrigger) { mutableFloatStateOf(value) }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("${String.format(Locale.getDefault(), "%.1f", sliderValue)}dB", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = color)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onUpdate(sliderValue) },
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
        )
    }
}

@Composable
fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Icon(
                Icons.Rounded.Tune,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
