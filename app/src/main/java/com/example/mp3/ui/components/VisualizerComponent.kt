package com.example.mp3.ui.components

import android.media.audiofx.Visualizer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Visualizador de Audio mejorado con múltiples estilos.
 * 
 * @param visualizerStyle 0: Barras, 1: Círculo, 2: Onda, 3: Puntos
 */
@Composable
fun AudioVisualizer(
    audioSessionId: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    visualizerStyle: Int = 0
) {
    if (audioSessionId <= 0) return

    // Número de elementos a dibujar (barras, puntos, etc.)
    val barCount = if (visualizerStyle == 1) 64 else 32 
    
    // Estado que mantiene las magnitudes de frecuencia actuales
    val magnitudes = remember { mutableStateOf(FloatArray(barCount) { 0.02f }) }
    
    // Configuración del motor de captura de audio (Android Visualizer API)
    DisposableEffect(audioSessionId) {
        val visualizer = try {
            Visualizer(audioSessionId).apply {
                // Capturamos el tamaño máximo para mayor precisión en frecuencias
                captureSize = Visualizer.getCaptureSizeRange()[1]
                
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft == null) return
                        
                        val current = magnitudes.value
                        val next = FloatArray(barCount)
                        val n = fft.size / 2
                        
                        // Procesamos los datos FFT para agruparlos en nuestras barras/puntos
                        for (i in 0 until barCount) {
                            // Escala logarítmica para que las frecuencias bajas (bajo) se vean más activas
                            val startPercent = (i.toFloat() / barCount).let { it * it }
                            val endPercent = ((i + 1).toFloat() / barCount).let { it * it }
                            
                            val startBin = (startPercent * n).toInt().coerceIn(0, n - 1)
                            val endBin = (endPercent * n).toInt().coerceIn(startBin + 1, n)
                            
                            var sum = 0f
                            var count = 0
                            for (j in startBin until endBin) {
                                val index = j * 2
                                if (index + 1 >= fft.size) break
                                val re = fft[index].toInt()
                                val im = fft[index + 1].toInt()
                                // Magnitud = raíz cuadrada de la suma de los cuadrados (Pitágoras)
                                sum += sqrt((re * re + im * im).toFloat())
                                count++
                            }
                            
                            val avg = if (count > 0) sum / count else 0f
                            // Normalizamos el valor y aplicamos un factor de escala
                            val rawMagnitude = (avg * 2.8f / 128f).coerceIn(0.02f, 1.0f)
                            
                            // Suavizado (Interpolación lineal): 30% anterior, 70% nuevo para fluidez
                            next[i] = current[i] * 0.3f + rawMagnitude * 0.7f
                        }
                        magnitudes.value = next
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) {
            null
        }

        onDispose {
            visualizer?.enabled = false
            visualizer?.release()
        }
    }

    // Colores tonales para el degradado del visualizador
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val width = size.width
        val height = size.height
        val currentMagnitudes = magnitudes.value

        when (visualizerStyle) {
            1 -> { // --- ESTILO: CÍRCULO RADIANTE ---
                val centerX = width / 2
                val centerY = height / 2
                val radius = height * 0.3f
                
                currentMagnitudes.forEachIndexed { index, magnitude ->
                    val angle = (index.toFloat() / barCount) * 2 * Math.PI
                    val lineLength = magnitude * height * 0.4f
                    
                    val startX = centerX + radius * cos(angle).toFloat()
                    val startY = centerY + radius * sin(angle).toFloat()
                    val endX = centerX + (radius + lineLength) * cos(angle).toFloat()
                    val endY = centerY + (radius + lineLength) * sin(angle).toFloat()
                    
                    drawLine(
                        color = color.copy(alpha = 0.8f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            2 -> { // --- ESTILO: ONDA SUAVE ---
                val points = mutableListOf<Offset>()
                currentMagnitudes.forEachIndexed { index, magnitude ->
                    val x = (index.toFloat() / (barCount - 1)) * width
                    val y = height / 2 + (if (index % 2 == 0) 1 else -1) * magnitude * height / 2
                    points.add(Offset(x, y))
                }
                
                for (i in 0 until points.size - 1) {
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(color, secondaryColor)),
                        start = points[i],
                        end = points[i+1],
                        strokeWidth = 6f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            3 -> { // --- ESTILO: PUNTOS DE ENERGÍA ---
                val dotSize = 8f
                currentMagnitudes.forEachIndexed { index, magnitude ->
                    val x = (index.toFloat() / (barCount - 1)) * width
                    val numDots = (magnitude * 10).toInt().coerceAtLeast(1)
                    
                    for (j in 0 until numDots) {
                        drawCircle(
                            color = color.copy(alpha = 1f - (j.toFloat() / 10f)),
                            radius = dotSize,
                            center = Offset(x, height - (j * 15f) - 10f)
                        )
                    }
                }
            }
            else -> { // --- ESTILO 0: BARRAS CLÁSICAS (Por defecto) ---
                val barWidth = width / (barCount * 1.5f)
                val space = barWidth / 2
                val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)

                currentMagnitudes.forEachIndexed { index, magnitude ->
                    val x = index * (barWidth + space)
                    val barHeight = height * magnitude
                    
                    val barColor = when {
                        index < barCount / 3 -> lerp(color, secondaryColor, index / (barCount / 3f))
                        index < 2 * barCount / 3 -> lerp(secondaryColor, tertiaryColor, (index - barCount / 3) / (barCount / 3f))
                        else -> lerp(tertiaryColor, color, (index - 2 * barCount / 3) / (barCount / 3f))
                    }

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor.copy(alpha = 0.9f), barColor.copy(alpha = 0.3f)),
                            startY = height - barHeight,
                            endY = height
                        ),
                        topLeft = Offset(x, height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }
    }
}
