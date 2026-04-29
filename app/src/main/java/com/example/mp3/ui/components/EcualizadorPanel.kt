package com.example.mp3.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.mp3.LocalStrings
import com.example.mp3.PlaybackService
import kotlinx.coroutines.delay
import org.json.JSONObject

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EcualizadorPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val audioPrefs = remember { context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE) }
    
    // Sistema de actualización optimizado (Debounce)
    var lastUpdateTrigger by remember { mutableLongStateOf(0L) }
    
    val notifyService: () -> Unit = {
        lastUpdateTrigger = System.currentTimeMillis()
    }

    LaunchedEffect(lastUpdateTrigger) {
        if (lastUpdateTrigger > 0) {
            delay(60) // Evita saturar el servicio con intents innecesarios
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = "UPDATE_EQ"
            }
            context.startService(intent)
        }
    }

    // Estados para controles reactivos
    var isEnabled by remember { mutableStateOf(audioPrefs.getBoolean("eq_enabled", false)) }
    var masterGain by remember { mutableFloatStateOf(audioPrefs.getFloat("master_gain", 1.0f)) }
    var audioBalance by remember { mutableFloatStateOf(audioPrefs.getFloat("audio_balance", 0f)) }
    var limiterEnabled by remember { mutableStateOf(audioPrefs.getBoolean("limiter_enabled", true)) }
    
    var currentPresetName by remember { mutableStateOf(audioPrefs.getString("current_preset", "Flat") ?: "Flat") }
    var customPresetsJson by remember { mutableStateOf(audioPrefs.getString("custom_presets", "{}") ?: "{}") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var resetTrigger by remember { mutableIntStateOf(0) }

    // Lista de presets dinámica
    val defaultPresets = listOf("Flat", "Rock", "Pop", "Jazz", "Electronic")
    val allPresets = remember(customPresetsJson) {
        val customNames = try {
            val json = JSONObject(customPresetsJson)
            json.keys().asSequence().toList()
        } catch (e: Exception) { emptyList() }
        defaultPresets + customNames + listOf("Custom")
    }
    
    val expressiveColors = listOf(
        Color(0xFFD0BCFF), Color(0xFFEFB8C8), Color(0xFFA5D6A7), Color(0xFF81D4FA), Color(0xFFFFCC80)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080A))
            .padding(vertical = 16.dp)
            .pointerInput(Unit) { detectTapGestures { } },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(strings.equalizer, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
                Text("Motor Expressive v2", style = MaterialTheme.typography.labelSmall, color = if (isEnabled) Color(0xFFD0BCFF) else Color.DarkGray)
            }
            
            Row {
                IconButton(
                    onClick = { 
                        (0 until 10).forEach { audioPrefs.edit { putFloat("eq_band_$it", 0f) } }
                        currentPresetName = "Flat"
                        resetTrigger++
                        notifyService()
                    },
                    modifier = Modifier.background(Color(0xFF1E1F22), CircleShape)
                ) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
                
                Spacer(Modifier.width(8.dp))
                
                IconButton(
                    onClick = { 
                        isEnabled = !isEnabled
                        audioPrefs.edit { putBoolean("eq_enabled", isEnabled) }
                        notifyService()
                    },
                    modifier = Modifier.background(if (isEnabled) Color(0xFFD0BCFF) else Color(0xFF1E1F22), CircleShape)
                ) { Icon(Icons.Default.PowerSettingsNew, null, tint = if (isEnabled) Color.Black else Color.Gray) }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(Color(0xFF1E1F22), CircleShape)
                ) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
        }

        // --- Presets Dinámicos ---
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 24.dp)) {
            items(allPresets) { preset ->
                val isSelected = currentPresetName == preset
                Surface(
                    onClick = {
                        currentPresetName = preset
                        audioPrefs.edit { putString("current_preset", preset) }
                        applyPreset(preset, audioPrefs, customPresetsJson)
                        resetTrigger++
                        notifyService()
                    },
                    color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF121318),
                    shape = RoundedCornerShape(16.dp),
                    border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF1E1F22))
                ) { Text(preset, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Bold) }
            }
        }

        // --- Mixer Card (Sin scroll interno) ---
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).weight(1f, fill = false), color = Color(0xFF121318), shape = RoundedCornerShape(32.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ganancia de espectro", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    IconButton(onClick = { showSaveDialog = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Add, null, tint = Color(0xFFD0BCFF)) }
                }
                Spacer(Modifier.height(16.dp))
                BandMixerPaginated(audioPrefs, resetTrigger, expressiveColors, notifyService)
            }
        }

        // --- Controles inferiores fijos ---
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Master Gain
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF121318), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Salida maestra", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Slider(
                        value = masterGain,
                        onValueChange = { 
                            masterGain = it
                            audioPrefs.edit { putFloat("master_gain", it) }
                            notifyService()
                        },
                        valueRange = 0f..2f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF), inactiveTrackColor = Color(0xFF1E1F22))
                    )
                }
            }

            // Balance & Limitador
            Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(modifier = Modifier.weight(1f), color = Color(0xFF121318), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Balance", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = audioBalance,
                            onValueChange = { 
                                audioBalance = it
                                audioPrefs.edit { putFloat("audio_balance", it) }
                                notifyService()
                            },
                            valueRange = -1f..1f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFEFB8C8), activeTrackColor = Color(0xFFEFB8C8))
                        )
                    }
                }
                Surface(modifier = Modifier.weight(0.6f), color = Color(0xFF121318), shape = RoundedCornerShape(24.dp)) {
                    Row(modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Limit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Switch(
                            checked = limiterEnabled,
                            onCheckedChange = {
                                limiterEnabled = it
                                audioPrefs.edit { putBoolean("limiter_enabled", it) }
                                notifyService()
                            },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }

            // Efectos
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArcControl(modifier = Modifier.weight(1f), label = strings.superBass, value = audioPrefs.getInt("bass_strength", 0).toFloat(), range = 0f..1000f, color = Color(0xFFD0BCFF), onValueChange = { audioPrefs.edit { putInt("bass_strength", it.toInt()) }; notifyService() }, resetTrigger = resetTrigger)
                ArcControl(modifier = Modifier.weight(1f), label = strings.virtualizer, value = audioPrefs.getInt("virtualizer_strength", 0).toFloat(), range = 0f..1000f, color = Color(0xFFEFB8C8), onValueChange = { audioPrefs.edit { putInt("virtualizer_strength", it.toInt()) }; notifyService() }, resetTrigger = resetTrigger)
                ArcControl(modifier = Modifier.weight(1f), label = strings.loudness, value = audioPrefs.getInt("loudness_gain", 0).toFloat(), range = 0f..2000f, color = Color(0xFFA5D6A7), onValueChange = { audioPrefs.edit { putInt("loudness_gain", it.toInt()) }; notifyService() }, resetTrigger = resetTrigger)
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = Color(0xFF1E1F22),
            title = { Text(strings.saveProfile, color = Color.White) },
            text = {
                OutlinedTextField(value = newPresetName, onValueChange = { newPresetName = it }, label = { Text("Nombre del perfil") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD0BCFF), focusedLabelColor = Color(0xFFD0BCFF)))
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPresetName.isNotBlank()) {
                        try {
                            val json = JSONObject(customPresetsJson)
                            val array = org.json.JSONArray()
                            (0 until 10).forEach { i ->
                                array.put(audioPrefs.getFloat("eq_band_$i", 0f).toDouble())
                            }
                            json.put(newPresetName, array)
                            val updatedJson = json.toString()
                            
                            audioPrefs.edit { 
                                putString("custom_presets", updatedJson)
                                putString("current_preset", newPresetName) 
                            }
                            
                            customPresetsJson = updatedJson
                            currentPresetName = newPresetName
                            showSaveDialog = false
                            notifyService()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }) { Text(strings.save, color = Color(0xFFD0BCFF)) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BandMixerPaginated(prefs: android.content.SharedPreferences, resetTrigger: Int, colors: List<Color>, onBandChange: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val labels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(260.dp)) { page ->
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceAround) {
                val start = page * 5
                (start until start + 5).forEachIndexed { localIndex, globalIndex ->
                    VerticalSlider(
                        label = labels[globalIndex],
                        initialValue = prefs.getFloat("eq_band_$globalIndex", 0f),
                        resetTrigger = resetTrigger,
                        accentColor = colors[localIndex % colors.size],
                        onValueChange = { prefs.edit { putFloat("eq_band_$globalIndex", it) }; onBandChange() }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) { i ->
                val color = if (pagerState.currentPage == i) Color(0xFFD0BCFF) else Color(0xFF2A2B32)
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
fun VerticalSlider(modifier: Modifier = Modifier, label: String, initialValue: Float, resetTrigger: Int, accentColor: Color, onValueChange: (Float) -> Unit) {
    var value by remember(initialValue, resetTrigger) { mutableFloatStateOf(initialValue) }
    val range = -20f..20f
    
    val rotation = value * 18f 

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = Color(0xFF1E1F22), shape = CircleShape, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("${value.toInt()}", color = accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .height(180.dp).width(44.dp)
                .pointerInput(resetTrigger) {
                    detectDragGestures(onDrag = { change, _ ->
                        change.consume()
                        val newValue = (1f - (change.position.y / size.height.toFloat())) * (range.endInclusive - range.start) + range.start
                        value = newValue.coerceIn(range)
                        onValueChange(value)
                    })
                }
                .pointerInput(resetTrigger) {
                    detectTapGestures { offset ->
                        val newValue = (1f - (offset.y / size.height.toFloat())) * (range.endInclusive - range.start) + range.start
                        value = newValue.coerceIn(range)
                        onValueChange(value)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.fillMaxHeight().width(30.dp).clip(RoundedCornerShape(15.dp)).background(Brush.verticalGradient(listOf(Color(0xFF2A2B32), Color(0xFF121318)))).border(1.dp, Color(0xFF1E1F22), RoundedCornerShape(15.dp)))

            val verticalBias = 1f - ((value - range.start) / (range.endInclusive - range.start))
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(), contentAlignment = BiasAlignment(0f, verticalBias * 2f - 1f)) {
                Surface(
                    modifier = Modifier.size(36.dp).graphicsLayer { rotationZ = rotation },
                    shape = CircleShape, color = accentColor, shadowElevation = 8.dp
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = Color.Black, modifier = Modifier.size(20.dp)) } }
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcControl(modifier: Modifier = Modifier, label: String, value: Float, range: ClosedFloatingPointRange<Float>, color: Color, onValueChange: (Float) -> Unit, resetTrigger: Int) {
    val context = LocalContext.current
    val audioPrefs = remember { context.getSharedPreferences("audio_settings", Context.MODE_PRIVATE) }
    val effectKey = label.lowercase().replace(" ", "_")
    
    var currentValue by remember(value, resetTrigger) { mutableFloatStateOf(value) }
    var isEffectEnabled by remember { mutableStateOf(audioPrefs.getBoolean("${effectKey}_enabled", true)) }

    Surface(modifier = modifier.aspectRatio(0.85f), color = Color(0xFF121318), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFF1E1F22))) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 5.dp.toPx()
                    drawArc(color = Color(0xFF1E1F22), startAngle = 135f, sweepAngle = 270f, useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    drawArc(color = if (isEffectEnabled) color else Color.DarkGray, startAngle = 135f, sweepAngle = (currentValue / range.endInclusive) * 270f, useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round))
                }
                Text("${((currentValue / range.endInclusive) * 100).toInt()}%", color = if (isEffectEnabled) Color.White else Color.Gray, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Slider(value = currentValue, onValueChange = { if (isEffectEnabled) currentValue = it }, onValueChangeFinished = { if (isEffectEnabled) onValueChange(currentValue) }, valueRange = range, modifier = Modifier.fillMaxSize().alpha(0f))
            }
            Switch(checked = isEffectEnabled, onCheckedChange = { isEffectEnabled = it; audioPrefs.edit { putBoolean("${effectKey}_enabled", it) }; onValueChange(currentValue) }, modifier = Modifier.scale(0.6f))
            Text(label, fontSize = 9.sp, color = if (isEffectEnabled) Color.LightGray else Color.Gray, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

private fun applyPreset(name: String, prefs: android.content.SharedPreferences, customJson: String) {
    val values = when (name) {
        "Flat" -> List(10) { 0f }
        "Rock" -> listOf(4f, 3f, 2f, 0f, -1f, -1f, 0f, 1f, 2f, 3f)
        "Pop" -> listOf(-1f, 1f, 3f, 4f, 2f, -1f, -2f, -2f, -1f, -1f)
        "Jazz" -> listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 2f)
        "Electronic" -> listOf(4f, 3f, 0f, 0f, -2f, 1f, 0f, 1f, 3f, 4f)
        "Custom" -> return
        else -> {
            try {
                val json = JSONObject(customJson)
                if (json.has(name)) {
                    val arr = json.getJSONArray(name)
                    (0 until 10).map { arr.getDouble(it).toFloat() }
                } else List(10) { 0f }
            } catch (e: Exception) { List(10) { 0f } }
        }
    }
    prefs.edit { values.forEachIndexed { index, value -> putFloat("eq_band_$index", value) } }
}
