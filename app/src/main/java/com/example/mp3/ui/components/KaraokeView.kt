package com.example.mp3.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mp3.LyricLine

@Composable
fun KaraokeView(
    lyricsList: List<LyricLine>,
    currentPositionMs: Long,
    fontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    centered: Boolean = false,
    selectionMode: Boolean = false,
    selectedIndices: Set<Int> = emptySet(),
    showShadow: Boolean = true,
    onToggleSelection: (Int) -> Unit = {},
    onLyricClick: ((Long) -> Unit)? = null
) {
    val listState = rememberLazyListState()

    val activeIndex = lyricsList.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)

    LaunchedEffect(activeIndex) {
        if (lyricsList.isNotEmpty() && !selectionMode && !listState.isScrollInProgress) {
            val scrollTarget = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(scrollTarget)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp, start = 24.dp, end = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        itemsIndexed(lyricsList, key = { _, line -> line.timeMs }) { index, line ->
            val isActive = index == activeIndex
            val isSelected = selectedIndices.contains(index)
            
            val scale by animateFloatAsState(
                targetValue = if (selectionMode) {
                    if (isSelected) 1.05f else 0.95f
                } else {
                    if (isActive) 1.12f else 1f
                },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "LyricScale"
            )
            
            val alpha by animateFloatAsState(
                targetValue = if (selectionMode) {
                    if (isSelected) 1f else 0.3f
                } else {
                    if (isActive) 1f else 0.4f
                },
                animationSpec = tween(400),
                label = "LyricAlpha"
            )

            val modifier = if (selectionMode) {
                Modifier.clickable { onToggleSelection(index) }
            } else if (onLyricClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onLyricClick(line.timeMs)
                }
            } else {
                Modifier
            }

            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = if (isActive || isSelected) FontWeight.Black else FontWeight.ExtraBold,
                    fontSize = if (isActive && !selectionMode) (fontSize.value * 1.3f).sp else fontSize,
                    lineHeight = (fontSize.value * 1.5f).sp,
                    letterSpacing = if (isActive || isSelected) (-1.0).sp else (-0.5).sp,
                    shadow = if (showShadow) Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 8f
                    ) else null
                ),
                color = when {
                    selectionMode && isSelected -> MaterialTheme.colorScheme.primary
                    !selectionMode && isActive -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .animateContentSize()
                    .then(modifier)
            )
        }
    }
}
