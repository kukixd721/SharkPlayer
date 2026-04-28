package com.example.mp3.ui.components

import androidx.compose.runtime.*
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay

@Composable
fun rememberPlayerPosition(player: MediaController?): Long {
    var position by remember { mutableLongStateOf(0L) }

    LaunchedEffect(player) {
        while (true) {
            position = player?.currentPosition ?: 0L
            delay(100L)
        }
    }
    return position
}
