package com.example.mp3.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedMeshGradient(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
    alpha: Float = 0.6f,
    blurRadius: androidx.compose.ui.unit.Dp = 80.dp
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val infiniteTransition = rememberInfiniteTransition(label = "MeshGradient")
        val animX by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
            label = "animX"
        )
        val animY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse),
            label = "animY"
        )

        Canvas(modifier = Modifier.fillMaxSize().blur(blurRadius).alpha(alpha)) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(size.width * animX, size.height * (1f - animY)),
                    radius = size.width * 1.5f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryColor.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(size.width * (1f - animX), size.height * animY),
                    radius = size.width * 1.2f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(tertiaryColor.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.width * (0.5f + animX * 0.5f)
                )
            )
            // Capa extra de movimiento para mayor profundidad
            val angleRad = Math.toRadians((animX * 360f).toDouble()).toFloat()
            val offX = cos(angleRad.toDouble()).toFloat() * 150f
            val offY = sin(angleRad.toDouble()).toFloat() * 150f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(size.width / 2 + offX, size.height / 2 + offY),
                    radius = size.width * 0.8f
                )
            )
        }
    }
}
