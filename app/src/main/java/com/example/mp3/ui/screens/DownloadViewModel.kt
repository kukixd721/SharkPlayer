package com.example.mp3.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import com.yausername.ffmpeg.FFmpeg
import com.example.mp3.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.net.URLEncoder
import androidx.core.content.edit
import java.util.Locale

data class DownloadInfo(
    val id: Long = System.currentTimeMillis(),
    val url: String,
    val title: String,
    val progress: Float = 0f,
    val status: String = "",
    val isFinished: Boolean = false,
    val isError: Boolean = false,
    val lyricsType: String? = null,
    val format: String? = null
)

data class SearchResult(
    val title: String,
    val url: String,
    val duration: String,
    val thumbnail: String?,
    val uploader: String?
)

class DownloadViewModel : ViewModel() {
    var downloads = mutableStateListOf<DownloadInfo>()
        private set

    var history = mutableStateListOf<DownloadInfo>()
        private set

    private val activeJobs = mutableMapOf<Long, Job>()
    
    private val _downloadEvents = MutableSharedFlow<String>()
    val downloadEvents = _downloadEvents.asSharedFlow()

    var isLibraryUpdating by mutableStateOf(false)
    var selectedFormat by mutableStateOf("mp3")
    var selectedQuality by mutableStateOf("320")
    var searchResults = mutableStateListOf<SearchResult>()
    var isSearching by mutableStateOf(false)
    var isUpdatingYtDlp by mutableStateOf(false)

    fun loadHistory(context: Context) {
        val prefs = context.getSharedPreferences("download_prefs", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("history", null) ?: return
        try {
            val jsonArray = org.json.JSONArray(jsonString)
            val loadedHistory = mutableListOf<DownloadInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                loadedHistory.add(DownloadInfo(
                    id = obj.optLong("id", System.currentTimeMillis()),
                    url = obj.getString("url"),
                    title = obj.getString("title"),
                    isFinished = true,
                    progress = 1f,
                    status = "Completado",
                    lyricsType = obj.optString("lyricsType", "").takeIf { it.isNotEmpty() },
                    format = obj.optString("format", "").takeIf { it.isNotEmpty() }
                ))
            }
            history.clear()
            history.addAll(loadedHistory)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveHistory(context: Context) {
        val prefs = context.getSharedPreferences("download_prefs", Context.MODE_PRIVATE)
        val jsonArray = org.json.JSONArray()
        history.take(100).forEach { info -> // Limit to last 100 entries
            val jsonObject = org.json.JSONObject().apply {
                put("id", info.id)
                put("url", info.url)
                put("title", info.title)
                put("lyricsType", info.lyricsType ?: "")
                put("format", info.format ?: "")
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit { putString("history", jsonArray.toString()) }
    }

    fun clearHistory(context: Context) {
        history.clear()
        context.getSharedPreferences("download_prefs", Context.MODE_PRIVATE).edit { remove("history") }
    }

    fun initYoutubeDL(context: Context) {
        loadHistory(context)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isUpdatingYtDlp = true
                YoutubeDL.getInstance().init(context)
                FFmpeg.getInstance().init(context)
                val result = YoutubeDL.getInstance().updateYoutubeDL(context)
                Log.d("YoutubeDL", "Update result: $result")
                isUpdatingYtDlp = false
            } catch (e: Exception) {
                e.printStackTrace()
                isUpdatingYtDlp = false
            }
        }
    }

    private fun getModernUserAgent(): String {
        val versions = listOf("17_4_1", "17_5", "16_6", "17_0")
        val version = versions.random()
        return "Mozilla/5.0 (iPhone; CPU iPhone OS $version like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/${version.replace("_", ".")} Mobile/15E148 Safari/604.1"
    }

    fun searchSong(query: String) {
        if (query.isBlank() || query.startsWith("http")) return
        isSearching = true
        searchResults.clear()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = YoutubeDLRequest("ytsearch10:$query")
                request.addOption("--dump-json")
                request.addOption("--flat-playlist")
                request.addOption("--no-playlist")
                request.addOption("--no-check-certificate")
                request.addOption("--no-cache-dir")
                request.addOption("--user-agent", getModernUserAgent())
                request.addOption("--extractor-args", "youtube:player_client=ios,mweb;player_skip=webpage")
                
                val info = try { YoutubeDL.getInstance().getInfo(request) } catch (e: Exception) { null }

                if (info != null) {
                    val entries = try {
                        val field = info.javaClass.getDeclaredField("entries")
                        field.isAccessible = true
                        field.get(info) as? List<*>
                    } catch (e: Exception) { null }

                    entries?.forEach { entry ->
                        if (entry is VideoInfo) addSearchResultFromVideoInfo(entry)
                    }
                }

                if (searchResults.isEmpty()) {
                    val response = try { YoutubeDL.getInstance().execute(request) } catch (e: Exception) { null }
                    response?.out?.lines()?.filter { it.isNotBlank() }?.forEach { line ->
                        try {
                            val json = org.json.JSONObject(line)
                            if (json.optString("_type") == "playlist") {
                                val entries = json.optJSONArray("entries")
                                entries?.let { 
                                    for (i in 0 until it.length()) addSearchResult(it.getJSONObject(i)) 
                                }
                            } else {
                                addSearchResult(json)
                            }
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSearching = false
            }
        }
    }

    private fun addSearchResultFromVideoInfo(entry: VideoInfo) {
        val videoUrl = "https://www.youtube.com/watch?v=${entry.id}"
        val durationSeconds = entry.duration
        val duration = if (durationSeconds > 0) {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            String.format(Locale.getDefault(), "%d:%02d", mins, secs)
        } else ""

        searchResults.add(SearchResult(entry.title ?: "Unknown", videoUrl, duration, entry.thumbnail, entry.uploader))
    }

    private fun addSearchResult(entry: org.json.JSONObject) {
        val id = entry.optString("id").ifEmpty { entry.optString("url") }
        val title = entry.optString("title", "Unknown")
        val uploader = entry.optString("uploader", "Unknown")
        val durationSeconds = entry.optInt("duration", 0)
        val duration = if (durationSeconds > 0) {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            String.format(Locale.getDefault(), "%d:%02d", mins, secs)
        } else ""
        
        var thumbnail = entry.optString("thumbnail", "")
        if (thumbnail.isEmpty()) {
            val thumbnails = entry.optJSONArray("thumbnails")
            if (thumbnails != null && thumbnails.length() > 0) {
                thumbnail = thumbnails.getJSONObject(thumbnails.length() - 1).optString("url", "")
            }
        }

        if (id.isNotEmpty()) {
            val videoUrl = if (id.startsWith("http")) id else "https://www.youtube.com/watch?v=$id"
            searchResults.add(SearchResult(title, videoUrl, duration, thumbnail, uploader))
        }
    }

    fun cancelDownload(downloadId: Long) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        val index = downloads.indexOfFirst { it.id == downloadId }
        if (index != -1) {
            downloads.removeAt(index)
        }
    }

    fun removeFromHistory(context: Context, downloadId: Long, deleteFile: Boolean = false) {
        val item = history.find { it.id == downloadId } ?: return
        history.remove(item)
        saveHistory(context)
        
        if (deleteFile) {
            val titleToDelete = item.title
            val formatToDelete = item.format
            viewModelScope.launch(Dispatchers.IO) {
                deletePhysicalFile(context, titleToDelete, formatToDelete)
            }
        }
    }

    private fun deletePhysicalFile(context: Context, title: String, format: String?) {
        val extensions = if (format != null) listOf(format) else listOf("mp3", "wav", "flac", "m4a", "opus", "aac", "mp4", "mkv", "webm", "avi")
        
        for (ext in extensions) {
            val fileName = "$title.$ext"
            val isVideo = ext in listOf("mp4", "mkv", "webm", "avi")
            val collection = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            
            val folder = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
            
            try {
                // Borrar archivo principal
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(fileName, "%$folder/SharkPlayer%")
                val deletedRows = context.contentResolver.delete(collection, selection, selectionArgs)
                
                if (deletedRows > 0) {
                    Log.d("DownloadViewModel", "Deleted physical file: $fileName")
                    // Borrar letra asociada si existe
                    val lrcName = "$title.lrc"
                    val lrcSelection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                    val lrcSelectionArgs = arrayOf(lrcName, "%$folder/SharkPlayer%")
                    context.contentResolver.delete(collection, lrcSelection, lrcSelectionArgs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startDownload(context: Context, url: String, strings: AppStrings, force: Boolean = false) {
        // Verificar si ya existe en descargas activas o completadas recientemente en memoria
        if (!force && (downloads.any { it.url == url && it.isFinished } || history.any { it.url == url })) {
            viewModelScope.launch { _downloadEvents.emit("DUPLICATE_FILE|$url") }
            return
        }

        val downloadId = System.currentTimeMillis()
        val newDownload = DownloadInfo(
            id = downloadId,
            url = url,
            title = if (url.contains("spotify")) "Spotify Link..." else strings.downloading,
            status = strings.pending
        )
        downloads.add(newDownload)

        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                fun updateInfo(status: String? = null, progress: Float? = null, title: String? = null, finished: Boolean? = null, lType: String? = null, format: String? = null) {
                    val index = downloads.indexOfFirst { it.id == downloadId }
                    if (index != -1) {
                        downloads[index] = downloads[index].copy(
                            status = status ?: downloads[index].status,
                            progress = progress ?: downloads[index].progress,
                            title = title ?: downloads[index].title,
                            isFinished = finished ?: downloads[index].isFinished,
                            lyricsType = lType ?: downloads[index].lyricsType,
                            format = format ?: downloads[index].format
                        )
                    }
                }

                var finalUrl = if (url.startsWith("http")) url else "ytsearch1:$url"

                // ... (lógica de Spotify omitida por brevedad en la visualización del chunk, pero se mantiene intacta)

                // --- LÓGICA PARA SPOTIFY (BYPASS DRM CHECK) ---
                if (url.contains("spotify.com")) {
                    if (url.contains("/playlist/") || url.contains("/album/")) {
                        val type = if (url.contains("/playlist/")) "playlist" else "álbum"
                        updateInfo(status = "Buscando canciones del $type...")
                        try {
                            // En lugar de usar yt-dlp directamente para el dump-json (que falla con 403),
                            // usamos el oEmbed de Spotify o simplemente scrapeamos el HTML si es posible.
                            // Para listas/álbumes, lo mejor es intentar obtener la lista de tracks vía API pública o scraping básico.
                            
                            val tracks = mutableListOf<String>()
                            val apiUrl = url.replace("open.spotify.com", "open.spotify.com/embed")
                            
                            val html = withContext(Dispatchers.IO) {
                                val connection = URL(apiUrl).openConnection() as java.net.HttpURLConnection
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                                connection.inputStream.bufferedReader().readText()
                            }
                            
                            // Regex para extraer metadata de los tracks en el embed
                            val regex = Regex("\\{\"name\":\"(.*?)\",.*?\"artists\":\\[(.*?)\\]")
                            regex.findAll(html).forEach { match ->
                                val title = match.groups[1]?.value ?: ""
                                val artistsJson = match.groups[2]?.value ?: ""
                                
                                val artistNames = Regex("\"name\":\"(.*?)\"").findAll(artistsJson)
                                    .map { it.groups[1]?.value ?: "" }
                                    .filter { it.isNotBlank() }
                                    .joinToString(", ")
                                
                                if (title.isNotBlank()) {
                                    tracks.add("$title $artistNames")
                                }
                            }
                            
                            if (tracks.isNotEmpty()) {
                                var addedCount = 0
                                tracks.distinct().forEach { trackName ->
                                    startDownload(context, trackName, strings)
                                    addedCount++
                                }
                                updateInfo(status = "$addedCount canciones añadidas", finished = true)
                                return@launch
                            } else {
                                // Fallback a yt-dlp si el embed no funciona
                                throw Exception("No se detectaron tracks")
                            }
                        } catch (e: Exception) {
                            Log.e("SpotifyPlaylist", "Error Scraping", e)
                            updateInfo(status = "Error: Spotify protege esta lista (DRM)", finished = true)
                            return@launch
                        }
                    } else {
                        updateInfo(status = "Bypassing DRM...")
                        try {
                            val oEmbedUrl = "https://open.spotify.com/oembed?url=${url.split("?")[0]}"
                            val response = withContext(Dispatchers.IO) { 
                                val connection = URL(oEmbedUrl).openConnection() as java.net.HttpURLConnection
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                                connection.inputStream.bufferedReader().readText()
                            }
                            val json = org.json.JSONObject(response)
                            val fullTitle = json.optString("title", "")
                            if (fullTitle.isNotBlank()) {
                                finalUrl = "ytsearch1:$fullTitle"
                                updateInfo(title = fullTitle, status = "Buscando en YouTube...")
                            } else {
                                throw Exception("No metadata")
                            }
                        } catch (e: Exception) {
                            Log.e("SpotifyBridge", "Error Bypass", e)
                            updateInfo(status = "Error: No se pudo obtener info de Spotify", finished = true)
                            return@launch
                        }
                    }
                }

                val infoRequest = YoutubeDLRequest(finalUrl)
                infoRequest.addOption("--no-check-certificate")
                infoRequest.addOption("--no-playlist")
                infoRequest.addOption("--no-cache-dir")
                infoRequest.addOption("--user-agent", getModernUserAgent())
                infoRequest.addOption("--extractor-args", "youtube:player_client=ios,mweb;player_skip=webpage")
                
                val videoInfo = try { YoutubeDL.getInstance().getInfo(infoRequest) } catch (e: Exception) { null }
                val displayTitle = videoInfo?.title ?: (downloads.find { it.id == downloadId }?.title ?: strings.downloading)
                val cleanTitle = displayTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                updateInfo(title = displayTitle)

                // Verificar existencia física del archivo antes de descargar
                if (!force && checkIfFileExists(context, cleanTitle, selectedFormat)) {
                    updateInfo(status = strings.completed, progress = 1f, finished = true, format = selectedFormat)
                    val existingInfo = downloads.find { it.id == downloadId }
                    if (existingInfo != null && history.none { it.url == existingInfo.url }) {
                        history.add(0, existingInfo.copy(title = cleanTitle, isFinished = true, progress = 1f, format = selectedFormat))
                        saveHistory(context)
                    }
                    withContext(Dispatchers.Main) { _downloadEvents.emit("DUPLICATE_FILE|$url") }
                    return@launch
                }

                val downloadDir = File(context.cacheDir, "downloads")
                if (!downloadDir.exists()) downloadDir.mkdirs()
                
                val tempFileName = "dl_${System.currentTimeMillis()}"
                val request = YoutubeDLRequest(videoInfo?.webpageUrl ?: finalUrl)
                request.addOption("--no-check-certificate")
                request.addOption("--no-cache-dir")
                request.addOption("--user-agent", getModernUserAgent())
                request.addOption("--extractor-args", "youtube:player_client=ios,mweb;player_skip=webpage")
                
                val isVideo = selectedFormat in listOf("mp4", "mkv", "webm", "avi")
                if (isVideo) {
                    val formatSelection = when(selectedFormat) {
                        "mp4" -> "bestvideo[height<=$selectedQuality][ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                        else -> "bestvideo[height<=$selectedQuality]+bestaudio/best"
                    }
                    request.addOption("--format", formatSelection)
                    request.addOption("--merge-output-format", selectedFormat)
                } else {
                    request.addOption("--format", "bestaudio/best")
                    request.addOption("--extract-audio")
                    request.addOption("--audio-format", selectedFormat)
                    
                    // Solo aplicar audio-quality si es mp3 o m4a (vbr/abr)
                    if (selectedFormat == "mp3" || selectedFormat == "m4a") {
                        val qValue = when(selectedQuality) {
                            "1000" -> "0"
                            "500" -> "1"
                            "320" -> "0"
                            "256" -> "2"
                            "192" -> "4"
                            "128" -> "5"
                            else -> "5"
                        }
                        request.addOption("--audio-quality", qValue)
                    }
                }
                
                request.addOption("--no-playlist")
                request.addOption("--embed-metadata")
                request.addOption("--embed-thumbnail")
                request.addOption("--convert-thumbnails", "jpg")
                request.addOption("--write-info-json")
                request.addOption("--restrict-filenames")
                request.addOption("-o", "${downloadDir.absolutePath}/$tempFileName.%(ext)s")

                updateInfo(status = strings.downloading)

                YoutubeDL.getInstance().execute(request) { progress, _, _ ->
                    updateInfo(progress = progress / 100f, status = "${strings.downloading} ${progress.toInt()}%")
                }

                val finalFile = downloadDir.listFiles()?.find { it.name.startsWith(tempFileName) && 
                    it.extension.lowercase() in listOf(selectedFormat, "m4a", "webm", "opus", "mp3") }

                if (finalFile != null && finalFile.exists()) {
                    val cleanTitle = displayTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    val renamedFile = File(downloadDir, "$cleanTitle.${finalFile.extension}")
                    finalFile.renameTo(renamedFile)
                    
                    val finalLrcFile = File(downloadDir, "$cleanTitle.lrc")
                    var lyricsToCache: String? = fetchExternalLyrics(displayTitle, videoInfo?.duration?.toInt())
                    
                    if (lyricsToCache.isNullOrBlank()) {
                        val subtitleFiles = downloadDir.listFiles()?.filter { it.name.startsWith(tempFileName) && it.extension.lowercase() in listOf("srt", "vtt", "lrc") }
                        subtitleFiles?.sortedByDescending { it.extension.lowercase() == "lrc" }?.firstOrNull()?.let { file ->
                            try {
                                val content = file.readText().trim()
                                lyricsToCache = if (file.extension.lowercase() == "lrc") content else convertSrtToLrc(content)
                            } catch (e: Exception) {}
                        }
                        subtitleFiles?.forEach { it.delete() }
                    }

                    if (lyricsToCache.isNullOrBlank()) {
                        downloadDir.listFiles()?.find { it.name.startsWith(tempFileName) && it.extension == "json" }?.let { jsonFile ->
                            try {
                                val json = org.json.JSONObject(jsonFile.readText())
                                lyricsToCache = json.optString("lyrics").ifBlank { extractLyricsFromDescription(json.optString("description")) }
                            } catch (e: Exception) {}
                            jsonFile.delete()
                        }
                    }
                    
                    if (lyricsToCache.isNullOrBlank() && videoInfo != null) lyricsToCache = extractLyricsFromDescription(videoInfo.description ?: "")
                    if (!lyricsToCache.isNullOrBlank()) try { finalLrcFile.writeText(lyricsToCache!!) } catch (e: Exception) {}
                    
                    val lyricsType = when {
                        lyricsToCache?.contains(Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]")) == true -> "LRC"
                        !lyricsToCache.isNullOrBlank() -> "TXT"
                        else -> null
                    }

                    val audioPath = moveFileToPublicMusic(context, renamedFile)
                    val lyricsPath = if (finalLrcFile.exists()) moveFileToPublicMusic(context, finalLrcFile) else null
                    
                    updateInfo(status = strings.completed, progress = 100f, finished = true, lType = lyricsType, format = selectedFormat)

                    // Añadir al historial y persistir
                    val completedInfo = downloads.find { it.id == downloadId }
                    if (completedInfo != null && history.none { it.url == completedInfo.url }) {
                        history.add(0, completedInfo)
                        saveHistory(context)
                    }

                    val pathsToScan = listOfNotNull(audioPath, lyricsPath).toTypedArray()
                    if (pathsToScan.isNotEmpty()) {
                        kotlinx.coroutines.delay(800)
                        MediaScannerConnection.scanFile(context, pathsToScan, null) { path, uri ->
                            Log.d("MediaScanner", "Scanned $path -> $uri")
                        }
                    }

                    audioPath?.let { path ->
                        if (!lyricsToCache.isNullOrBlank()) {
                            context.getSharedPreferences("lyrics_cache", Context.MODE_PRIVATE).edit {
                                putString(path, lyricsToCache)
                                putString(renamedFile.name, lyricsToCache)
                            }
                        }
                    }

                    updateInfo(title = cleanTitle, status = strings.completed, finished = true, progress = 1f, lType = lyricsType, format = selectedFormat)
                    isLibraryUpdating = true
                    
                    // Aseguramos que el emit ocurra en el hilo principal para la UI
                    withContext(Dispatchers.Main) {
                        showCompletionNotification(context, cleanTitle)
                        _downloadEvents.emit(cleanTitle)
                    }
                } else {
                    throw Exception(strings.downloadError)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
                val errorMsg = e.message ?: ""
                val userFriendlyError = when {
                    errorMsg.contains("Signature", ignoreCase = true) || errorMsg.contains("decipher", ignoreCase = true) -> strings.engineUpdateError
                    errorMsg.contains("403") -> "Acceso denegado (403). Prueba a actualizar el motor."
                    errorMsg.contains("429") -> strings.ipBlockError
                    errorMsg.contains("No space") -> "Sin espacio en disco."
                    else -> "${strings.downloadError}: ${errorMsg.take(50)}"
                }
                
                val index = downloads.indexOfFirst { it.id == downloadId }
                if (index != -1) {
                    downloads[index] = downloads[index].copy(status = userFriendlyError, isFinished = false, isError = true)
                }
            } finally {
                activeJobs.remove(downloadId)
            }
        }
        activeJobs[downloadId] = job
    }

    private fun checkIfFileExists(context: Context, cleanTitle: String, extension: String): Boolean {
        val fileName = "$cleanTitle.$extension"
        val isVideo = extension in listOf("mp4", "mkv", "webm", "avi")
        val collection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        
        val folder = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
        
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf(fileName, "%$folder/SharkPlayer%")

        return try {
            context.contentResolver.query(collection, arrayOf(MediaStore.MediaColumns._ID), selection, selectionArgs, null)?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun showCompletionNotification(context: Context, title: String) {
        val channelId = "downloads_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Descargas", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Descarga completada")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private suspend fun fetchExternalLyrics(title: String, duration: Int? = null): String? {
        return withContext(Dispatchers.IO) {
            try {
                val cleanTitle = title.replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "").replace(Regex("(?i)official video|video oficial|lyric video|full hd|4k|audio|hq|visualizer|remastered|feat\\.|ft\\."), "").replace(Regex("\\s+"), " ").trim()
                val separators = listOf(" - ", " — ", " | ", " : ", " – ")
                var artist: String? = null
                var track: String? = null
                for (sep in separators) {
                    if (title.contains(sep)) {
                        val p = title.split(sep, limit = 2)
                        artist = p[0].replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "").trim()
                        track = p[1].replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "").replace(Regex("(?i)official video|video oficial|lyric video|full hd|4k|audio|hq|visualizer|remastered"), "").trim()
                        break
                    }
                }
                val searchQueries = mutableListOf<String>()
                if (!artist.isNullOrBlank() && !track.isNullOrBlank()) searchQueries.add("https://lrclib.net/api/search?artist_name=${URLEncoder.encode(artist, "UTF-8")}&track_name=${URLEncoder.encode(track, "UTF-8")}")
                searchQueries.add("https://lrclib.net/api/search?q=${URLEncoder.encode(cleanTitle, "UTF-8")}")
                for (queryUrl in searchQueries) {
                    try {
                        val connection = URL(queryUrl).openConnection() as java.net.HttpURLConnection
                        if (connection.responseCode == 200) {
                            val jsonArray = org.json.JSONArray(connection.inputStream.bufferedReader().readText())
                            if (jsonArray.length() > 0) {
                                var bestMatchIndex = 0
                                if (duration != null) {
                                    for (i in 0 until jsonArray.length()) {
                                        if (java.lang.Math.abs(jsonArray.getJSONObject(i).optInt("duration", 0) - duration) <= 4) {
                                            bestMatchIndex = i; break
                                        }
                                    }
                                }
                                val match = jsonArray.getJSONObject(bestMatchIndex)
                                val synced = match.optString("syncedLyrics")
                                if (!synced.isNullOrBlank() && synced != "null") return@withContext synced
                                val plain = match.optString("plainLyrics")
                                if (!plain.isNullOrBlank() && plain != "null") return@withContext plain
                            }
                        }
                    } catch (e: Exception) { continue }
                }
            } catch (e: Exception) {}
            null
        }
    }

    private fun extractLyricsFromDescription(description: String): String? {
        if (description.isBlank()) return null
        val lines = description.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val startMarkers = listOf("Lyrics:", "Letra:", "LETRAS", "LYRICS", "[Lyrics]", "LETRA:")
        var startIndex = -1
        for (i in lines.indices) {
            if (startMarkers.any { lines[i].startsWith(it, ignoreCase = true) }) { startIndex = i; break }
        }
        if (startIndex != -1) {
            val result = mutableListOf<String>()
            for (i in (startIndex + 1) until lines.size) {
                if (lines[i].contains("http") || lines[i].contains("www.")) break
                result.add(lines[i])
            }
            if (result.size > 2) return result.joinToString("\n").trim()
        }
        return null
    }

    private fun convertSrtToLrc(srtContent: String): String {
        val lines = srtContent.lines()
        val result = mutableListOf<String>()
        val timeRegex = Regex("(\\d{2}:)?(\\d{2}:\\d{2})[,.](\\d{2,3})")
        var currentTimestamp = ""
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.all { it.isDigit() }) continue
            val match = timeRegex.find(trimmed)
            if (match != null) {
                val fullTime = match.groupValues[2]
                val ms = match.groupValues[3].take(2)
                val hours = match.groupValues[1].replace(":", "")
                val finalMinutes = if (hours.isNotEmpty()) (hours.toInt() * 60) + fullTime.substringBefore(":").toInt() else fullTime.substringBefore(":").toInt()
                currentTimestamp = "[%02d:%s.%s]".format(finalMinutes, fullTime.substringAfter(":"), ms)
            } else if (!trimmed.contains("-->") && currentTimestamp.isNotEmpty()) {
                result.add("$currentTimestamp $trimmed")
            }
        }
        return if (result.isNotEmpty()) result.joinToString("\n") else srtContent
    }

    private fun moveFileToPublicMusic(context: Context, file: File): String? {
        val resolver = context.contentResolver
        val extension = file.extension.lowercase()
        val isAudio = listOf("mp3", "wav", "flac", "m4a", "aac", "ogg", "opus").contains(extension)
        val isVideo = listOf("mp4", "mkv", "webm").contains(extension)
        val uri = if (isAudio) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Files.getContentUri("external")
        val folder = if (isAudio) Environment.DIRECTORY_MUSIC else if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_DOWNLOADS
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, when {
                isAudio -> "audio/mpeg"
                isVideo -> "video/mp4"
                extension == "lrc" -> "application/octet-stream"
                else -> "text/plain"
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/SharkPlayer")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val insertedUri = resolver.insert(uri, contentValues)
        insertedUri?.let { targetUri ->
            try {
                resolver.openOutputStream(targetUri)?.use { outputStream -> FileInputStream(file).use { it.copyTo(outputStream) } }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(targetUri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                }
                
                // Intentar obtener el path real para el scanner
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                resolver.query(targetUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                        file.delete()
                        return path
                    }
                }
                
                file.delete()
                return targetUri.toString() // Fallback a URI si no hay path directo
            } catch (e: Exception) { e.printStackTrace() }
        }
        return null
    }
}
