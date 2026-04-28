package com.example.mp3
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatScreen(onBack: () -> Unit) {
    var logs by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // read logcat
    fun updateLogs() {
        scope.launch {
            logs = withContext(Dispatchers.IO) {
                try {
                    // execute logcat
                    val process = Runtime.getRuntime().exec("logcat -d -v time *:E")
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val result = mutableListOf<String>()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        result.add(line!!)
                    }
                    // only 200 lines
                    result.takeLast(200).reversed()
                } catch (e: Exception) {
                    listOf("Error al leer logs: ${e.message}")
                }
            }
        }
    }

    // clear logcat
    fun clearLogs() {
        scope.launch(Dispatchers.IO) {
            try {
                Runtime.getRuntime().exec("logcat -c")
                withContext(Dispatchers.Main) {
                    logs = emptyList()
                    updateLogs()
                }
            } catch (e: Exception) { }
        }
    }


    LaunchedEffect(Unit) {
        updateLogs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitor de Errores (Logcat)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { updateLogs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    IconButton(onClick = { clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(logs) { log ->
                val color = when {
                    log.contains(" E ") || log.contains("Error") -> Color.Red
                    log.contains(" W ") -> Color.Yellow
                    else -> Color.Green
                }
                Text(
                    text = log,
                    color = color,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                Divider(color = Color.DarkGray, thickness = 0.5.dp)
            }
        }
    }
}
