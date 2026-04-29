package com.example.mp3.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mp3.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    paddingValues: PaddingValues,
    settings: PlayerSettings,
    viewModel: DownloadViewModel
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    var url by remember { mutableStateOf("") }
    var showWebView by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var itemToRemoveFromHistory by remember { mutableStateOf<DownloadInfo?>(null) }
    var duplicateUrl by remember { mutableStateOf<String?>(null) }
    var webUrl by remember { mutableStateOf("https://music.youtube.com") }
    var showSourceSelector by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Estados para los menús desplegables
    var typeExpanded by remember { mutableStateOf(false) }
    var formatExpanded by remember { mutableStateOf(false) }
    var qualityExpanded by remember { mutableStateOf(false) }

    val configurationVisible = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        configurationVisible.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        viewModel.initYoutubeDL(context)
    }

    LaunchedEffect(Unit) {
        viewModel.downloadEvents.collect { event ->
            if (event.startsWith("DUPLICATE_FILE|")) {
                duplicateUrl = event.substringAfter("|")
            }
        }
    }

    // DIÁLOGO DE CONFIRMACIÓN PARA BORRAR FÍSICAMENTE
    if (itemToRemoveFromHistory != null) {
        AlertDialog(
            onDismissRequest = { itemToRemoveFromHistory = null },
            title = { Text(strings.deleteSong, fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(strings.deleteSongConfirm)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { 
                            // Aquí se podría añadir un checkbox si se desea, 
                            // pero por ahora usaremos la lógica de preguntar si borrar físico
                        }
                    ) {
                        // Por simplicidad en este paso, el diálogo preguntará directamente.
                        // Podríamos mejorar esto con un Switch.
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToRemoveFromHistory?.let { viewModel.removeFromHistory(context, it.id, deleteFile = true) }
                        itemToRemoveFromHistory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.deleteSong)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    itemToRemoveFromHistory?.let { viewModel.removeFromHistory(context, it.id, deleteFile = false) }
                    itemToRemoveFromHistory = null
                }) {
                    Text(strings.clearHistory) // "Solo quitar del historial"
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // DIÁLOGO DE DUPLICADO
    if (duplicateUrl != null) {
        AlertDialog(
            onDismissRequest = { duplicateUrl = null },
            title = { Text(strings.fileExistsTitle, fontWeight = FontWeight.Black) },
            text = { Text(strings.fileExistsMessage) },
            confirmButton = {
                Button(onClick = { 
                    duplicateUrl?.let { viewModel.startDownload(context, it, strings, force = true) }
                    duplicateUrl = null
                }) {
                    Text(strings.redownload)
                }
            },
            dismissButton = {
                TextButton(onClick = { duplicateUrl = null }) {
                    Text(strings.cancel)
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // DIÁLOGO DE HISTORIAL
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.history, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    if (viewModel.history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory(context) }) {
                            Text(strings.clearHistory, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (viewModel.history.isEmpty()) {
                        Text(strings.noActiveDownloads, modifier = Modifier.padding(24.dp))
                    } else {
                        LazyColumn {
                            items(viewModel.history) { item ->
                                DownloadItemCard(
                                    download = item,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    onCancel = { viewModel.startDownload(context, item.url, strings, force = true) },
                                    onRemove = { itemToRemoveFromHistory = item }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHistoryDialog = false }) {
                    Text(strings.close)
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // DIÁLOGO DE AYUDA / GUÍA DE DESCARGA
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    strings.downloadStepsTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Text(
                    strings.downloadStepsContent,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(strings.accept, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // DIÁLOGO DE SELECCIÓN DE FUENTE (DISEÑO SÓLIDO LIMPIO)
    if (showSourceSelector) {
        Dialog(
            onDismissRequest = { showSourceSelector = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(42.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 12.dp,
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        strings.exploreMusic,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        strings.selectFavoritePlatform,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Grid de fuentes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SourceItem(
                            name = "YT Music",
                            icon = Icons.Default.MusicNote,
                            color = Color(0xFFFF0000),
                            modifier = Modifier.weight(1f)
                        ) {
                            webUrl = "https://music.youtube.com"
                            viewModel.selectedFormat = "mp3"
                            showSourceSelector = false
                            showWebView = true
                        }
                        SourceItem(
                            name = "Spotify",
                            icon = Icons.Default.LibraryMusic,
                            color = Color(0xFF1DB954),
                            modifier = Modifier.weight(1f)
                        ) {
                            webUrl = "https://open.spotify.com"
                            viewModel.selectedFormat = "mp3"
                            viewModel.selectedQuality = "320"
                            showSourceSelector = false
                            showWebView = true
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SourceItem(
                        name = "YouTube (Videos)",
                        icon = Icons.Default.PlayCircle,
                        color = Color(0xFFE53935),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        webUrl = "https://www.youtube.com"
                        viewModel.selectedFormat = "mp4"
                        viewModel.selectedQuality = "720"
                        showSourceSelector = false
                        showWebView = true
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { showSourceSelector = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Text(strings.close, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // NAVEGADOR INTEGRADO
    if (showWebView) {
        ModalBottomSheet(
            onDismissRequest = { showWebView = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showWebView = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                        val title = when {
                            webUrl.contains("music") -> "YT Music"
                            webUrl.contains("spotify") -> "Spotify"
                            else -> "YouTube"
                        }
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = { 
                                val currentWebUrl = webViewInstance?.url
                                if (!currentWebUrl.isNullOrBlank() && (currentWebUrl.contains("youtube") || currentWebUrl.contains("youtu.be") || currentWebUrl.contains("spotify"))) {
                                    url = currentWebUrl
                                } else {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipData = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (clipData.contains("youtube") || clipData.contains("youtu.be") || clipData.contains("spotify")) {
                                        url = clipData
                                    }
                                }
                                showWebView = false 
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.capture)
                        }
                    }
                }
                
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            webViewClient = WebViewClient()
                            this.settings.javaScriptEnabled = true
                            this.settings.domStorageEnabled = true
                            webViewInstance = this
                            loadUrl(webUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 32.dp, bottom = paddingValues.calculateBottomPadding() + 100.dp)
        ) {
            // ... contenido de la lista (icono, titulo, input, etc)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(170.dp).graphicsLayer { rotationZ = -10f },
                            shape = RoundedCornerShape(48.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            tonalElevation = 2.dp
                        ) {}
                        Surface(
                            modifier = Modifier.size(150.dp).graphicsLayer { rotationZ = 5f },
                            shape = RoundedCornerShape(42.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 4.dp,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CloudDownload, 
                                    null, 
                                    modifier = Modifier.size(72.dp), 
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Text(
                        text = strings.downloadMusic, 
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Black, 
                            letterSpacing = (-4).sp,
                            lineHeight = 56.sp
                        ), 
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(36.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(strings.searchOrUrl, fontWeight = FontWeight.Bold) },
                            trailingIcon = {
                                if (url.isNotBlank()) {
                                    IconButton(onClick = { url = "" }) { Icon(Icons.Default.Clear, null) }
                                } else {
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        url = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    }) { 
                                        Icon(Icons.Default.ContentPaste, null, tint = MaterialTheme.colorScheme.primary) 
                                    }
                                }
                            },
                            shape = RoundedCornerShape(28.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = { showSourceSelector = true },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp),
                            tonalElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = strings.configuration,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp).fillMaxWidth()
                )
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .graphicsLayer {
                            alpha = configurationVisible.value
                            scaleX = 0.92f + (0.08f * configurationVisible.value)
                            scaleY = 0.92f + (0.08f * configurationVisible.value)
                            translationY = 60 * (1f - configurationVisible.value)
                        },
                    shape = RoundedCornerShape(36.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune, 
                                null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                strings.configuration,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.weight(1f))
                            if (viewModel.isUpdatingYtDlp) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = { viewModel.initYoutubeDL(context) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = strings.updateEngine,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showHistoryDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = strings.history,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showHelpDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Ayuda",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // SELECCIÓN DE TIPO (MÚSICA / VIDEO) - DISEÑO TIPO PÍLDORA
                        val isVideo = viewModel.selectedFormat == "mp4"
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isVideo) Icons.Default.PlayCircle else Icons.Default.MusicNote,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.downloadType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(if (isVideo) strings.videos else strings.music, fontWeight = FontWeight.Bold)
                                    }
                                    val rotation by animateFloatAsState(if (typeExpanded) 180f else 0f)
                                    Icon(Icons.Default.ExpandMore, null, modifier = Modifier.graphicsLayer { rotationZ = rotation })
                                }
                            }
                            
                            DropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f),
                                properties = PopupProperties(focusable = true),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 3.dp,
                                shadowElevation = 12.dp,
                                shape = RoundedCornerShape(28.dp),
                                offset = DpOffset(0.dp, 8.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.music, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) },
                                    onClick = { 
                                        viewModel.selectedFormat = "mp3"
                                        viewModel.selectedQuality = "320"
                                        typeExpanded = false
                                    },
                                    leadingIcon = { 
                                        Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.videos, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) },
                                    onClick = { 
                                        viewModel.selectedFormat = "mp4"
                                        viewModel.selectedQuality = "720"
                                        typeExpanded = false
                                    },
                                    leadingIcon = { 
                                        Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.secondary)
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // SELECCIÓN DE FORMATO - DISEÑO MÁS REDONDO
                            val formats = if (isVideo) listOf("mp4", "mkv", "webm", "avi") else listOf("mp3", "wav", "flac", "m4a", "opus", "aac")
                            ExposedDropdownMenuBox(
                                expanded = formatExpanded,
                                onExpandedChange = { formatExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                        Text(strings.format, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(viewModel.selectedFormat.uppercase(), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                                            val rotation by animateFloatAsState(if (formatExpanded) 180f else 0f)
                                            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = rotation })
                                        }
                                    }
                                }
                                DropdownMenu(
                                    expanded = formatExpanded,
                                    onDismissRequest = { formatExpanded = false },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 3.dp,
                                    shadowElevation = 12.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    offset = DpOffset(0.dp, 8.dp)
                                ) {
                                    formats.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f.uppercase(), fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium) },
                                            onClick = { 
                                                viewModel.selectedFormat = f
                                                formatExpanded = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // SELECCIÓN DE CALIDAD - DISEÑO MÁS REDONDO
                            val qualities = if (isVideo) listOf("144", "240", "360", "480", "720", "1080", "1440", "2160") else listOf("64", "96", "128", "192", "256", "320", "500", "1000")
                            ExposedDropdownMenuBox(
                                expanded = qualityExpanded,
                                onExpandedChange = { qualityExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                        Text(strings.quality, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val label = if (isVideo) "${viewModel.selectedQuality}p" else if(viewModel.selectedFormat in listOf("flac", "wav")) strings.lossless else "${viewModel.selectedQuality}k"
                                            Text(label, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val rotation by animateFloatAsState(if (qualityExpanded) 180f else 0f)
                                            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = rotation })
                                        }
                                    }
                                }
                                DropdownMenu(
                                    expanded = qualityExpanded,
                                    onDismissRequest = { qualityExpanded = false },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 3.dp,
                                    shadowElevation = 12.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    offset = DpOffset(0.dp, 8.dp)
                                ) {
                                    qualities.forEach { q ->
                                        val label = if (isVideo) "${q}p" else if(viewModel.selectedFormat in listOf("flac", "wav")) "${strings.lossless} ($q)" else "${q}k"
                                        
                                        DropdownMenuItem(
                                            text = { 
                                                Text(label, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                            },
                                            onClick = { 
                                                viewModel.selectedQuality = q
                                                qualityExpanded = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { if(url.isNotBlank()) { viewModel.startDownload(context, url, strings); url = "" } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(80.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = url.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                ) {
                    Icon(Icons.Default.DownloadForOffline, null, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(strings.start, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
            }

            if (viewModel.downloads.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = strings.recentDownloads,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp).fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                itemsIndexed(viewModel.downloads.asReversed()) { _, download ->
                    DownloadItemCard(
                        download = download, 
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        onCancel = { 
                            if (download.isError) {
                                viewModel.cancelDownload(download.id)
                                viewModel.startDownload(context, download.url, strings)
                            } else {
                                viewModel.cancelDownload(download.id)
                            }
                        },
                        onRemove = if (download.isFinished) { { itemToRemoveFromHistory = download } } else null
                    )
                }
            }
        }
    }
}


@Composable
fun SourceItem(name: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun DownloadItemCard(
    download: DownloadInfo, 
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val progress by animateFloatAsState(
        targetValue = download.progress, 
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow)
    )
    
    Surface(
        modifier = modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            val accentColor = if(download.isFinished) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            val containerColor = if(download.isFinished) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp).graphicsLayer { rotationZ = -5f },
                    shape = RoundedCornerShape(20.dp),
                    color = containerColor,
                    border = BorderStroke(2.dp, accentColor)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if(download.isFinished) Icons.Default.CheckCircle else Icons.Default.Download, 
                            null, 
                            tint = accentColor, 
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        download.title, 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.5).sp
                    )
                    Surface(
                        modifier = Modifier.padding(top = 4.dp),
                        color = accentColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            download.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall, 
                            color = if(download.isFinished) Color.White else MaterialTheme.colorScheme.onPrimary, 
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                
                Surface(
                    onClick = {
                        if (download.isFinished && onRemove != null) {
                            onRemove()
                        } else {
                            onCancel()
                        }
                    },
                    shape = CircleShape,
                    color = if (download.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(40.dp),
                    border = BorderStroke(1.dp, if (download.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                download.isFinished -> Icons.Default.Delete
                                download.isError -> Icons.Default.Refresh
                                else -> Icons.Default.Close
                            },
                            contentDescription = "Acción",
                            tint = if (download.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.8f)) // A bit of gradient for depth but mostly solid
                            )
                        )
                )
            }
            
            if (!download.isFinished) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            }
        }
    }
}
