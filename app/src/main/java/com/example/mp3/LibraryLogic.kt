package com.example.mp3

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.FileInputStream

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

// data models
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val data: String, // archive root
    val duration: Long,
    var lyrics: String? = null,
    val albumId: Long,
    val genre: String? = null
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(data)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setGenre(genre)
                .build()
        )
        .build()
}

data class Video(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String,
    val duration: Long,
    val resolution: String? = null,
    val bitrate: String? = null,
    val frameRate: String? = null,
    val thumbnailUri: String? = null
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(data)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setMediaType(MediaMetadata.MEDIA_TYPE_VIDEO)
                .build()
        )
        .build()
}

data class SongDetails(
    val bitrate: String = "---",
    val sampleRate: String = "---",
    val lyrics: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val year: String? = null,
    val trackNumber: String? = null,
    val genre: String? = null,
    val lyricsSource: String? = null, // new field to track if lyrics were found online or locally
    val resolution: String? = null,
    val frameRate: String? = null,
    val filePath: String = ""
)

// karaoke structure
data class LyricLine(
    val timeMs: Long, // time ms
    val text: String  // lyrics
)

suspend fun getAudioFiles(context: Context, onProgress: (String) -> Unit = {}): List<Song> = withContext(Dispatchers.IO) {
    val songs = mutableListOf<Song>()
    
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) MediaStore.Audio.Media.GENRE else MediaStore.Audio.Media.DATA // Fallback
    )

    val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
    val selectionArgs = arrayOf("10000") // only archives 10s

    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection, selection, selectionArgs, "${MediaStore.Audio.Media.TITLE} ASC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
        } else -1

        while (cursor.moveToNext()) {
            val mData = cursor.getString(dataCol) ?: ""
            val mTitle = cursor.getString(titleCol) ?: "Unknown"
            val mGenre = if (genreCol != -1) cursor.getString(genreCol) else null

            onProgress("🎵 $mTitle")
            // data
            songs.add(
                Song(
                    id = cursor.getLong(idCol),
                    title = mTitle,
                    artist = cursor.getString(artistCol) ?: "<Unknown>",
                    album = cursor.getString(albumCol) ?: "<Unknown>",
                    data = mData,
                    duration = cursor.getLong(durationCol),
                    lyrics = null, // Se cargarán después
                    albumId = cursor.getLong(albumIdCol),
                    genre = mGenre
                )
            )
        }
    }
    songs
}

suspend fun getVideoFiles(context: Context, extraPaths: List<String> = emptyList(), onProgress: (String) -> Unit = {}): List<Video> = withContext(Dispatchers.IO) {
    val videos = mutableListOf<Video>()
    
    // 1. Get videos from MediaStore
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.RESOLUTION,
        MediaStore.Video.Media.ARTIST
    )

    context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection, null, null, "${MediaStore.Video.Media.TITLE} ASC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val resCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)

        while (cursor.moveToNext()) {
            val title = cursor.getString(titleCol) ?: "Unknown Video"
            val videoPath = cursor.getString(dataCol) ?: ""
            onProgress("🎬 $title")
            
            videos.add(processVideoFile(videoPath, cursor.getLong(idCol), title, cursor.getString(artistCol), cursor.getLong(durationCol), cursor.getString(resCol)))
        }
    }

    // 2. Scan extra paths manually
    extraPaths.forEach { path ->
        val folder = File(path)
        if (folder.exists() && folder.isDirectory) {
            folder.listFiles { file -> 
                val ext = file.extension.lowercase()
                ext == "mp4" || ext == "mkv" || ext == "webm" || ext == "avi"
            }?.forEach { file ->
                if (videos.none { it.data == file.absolutePath }) {
                    onProgress("🎬 ${file.nameWithoutExtension}")
                    videos.add(processVideoFile(file.absolutePath, file.hashCode().toLong(), file.nameWithoutExtension, null, 0L, null))
                }
            }
        }
    }

    videos
}

private fun processVideoFile(
    videoPath: String,
    id: Long,
    title: String,
    artist: String?,
    duration: Long,
    resolution: String?
): Video {
    var extraBitrate: String? = null
    var extraFrameRate: String? = null
    var finalDuration = duration
    var finalRes = resolution

    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        
        if (finalDuration <= 0) {
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (dur != null) finalDuration = dur.toLong()
        }
        
        if (finalRes == null) {
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            if (w != null && h != null) finalRes = "${w}x${h}"
        }

        val br = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        if (br != null) {
            extraBitrate = (br.toLong() / 1000).toString() + " kbps"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            extraFrameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
        }
        retriever.release()
    } catch (e: Exception) {
        Log.e("VideoMetadata", "Error extracting extra metadata for $videoPath", e)
    }

    // Check for sidecar thumbnail
    val videoFile = File(videoPath)
    val parent = videoFile.parentFile
    val baseName = videoFile.nameWithoutExtension
    
    val thumbFile = parent?.listFiles { _, name ->
        val lower = name.lowercase()
        lower.startsWith(baseName.lowercase()) && (lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg") || lower.endsWith(".webp"))
    }?.firstOrNull()

    return Video(
        id = id,
        title = title,
        artist = artist ?: "Unknown Artist",
        data = videoPath,
        duration = finalDuration,
        resolution = finalRes,
        bitrate = extraBitrate,
        frameRate = extraFrameRate,
        thumbnailUri = thumbFile?.absolutePath
    )
}

fun getAudioMetadata(context: Context, path: String): SongDetails {
    val file = File(path)
    if (!file.exists()) return SongDetails()

    // cache lyrics
    val prefs = context.getSharedPreferences("lyrics_cache", Context.MODE_PRIVATE)
    var lyricsResult = prefs.getString(path, null) ?: prefs.getString(file.name, null)

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)

        // 1. Bitrate
        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        val kbps = if (bitrate != null) {
            val bit = try { bitrate.toInt() / 1000 } catch (e: Exception) { 0 }
            "$bit kbps"
        } else "---"

        // hz
        var hz = "---"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val sr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                if (sr != null) {
                    val khz = sr.toFloat() / 1000
                    hz = if (khz % 1 == 0f) "${khz.toInt()} kHz" else "$khz kHz"
                }
            }
            
            if (hz == "---") {
                val extractor = MediaExtractor()
                extractor.setDataSource(file.absolutePath)
                if (extractor.trackCount > 0) {
                    val format = extractor.getTrackFormat(0)
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        val srInt = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        val khz = srInt.toFloat() / 1000
                        hz = if (khz % 1 == 0f) "${khz.toInt()} kHz" else "$khz kHz"
                    }
                }
                extractor.release()
            }
        } catch (e: Exception) {
            hz = "44.1 kHz"
        }

        // 3. Otros Metadatos
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
        val track = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
        val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)

        // 3.1. Video metadata
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val resolution = if (width != null && height != null) "${width}x${height}" else null
        
        val frameRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
        } else null

        // lyrics search
        if (lyricsResult.isNullOrBlank()) {
            try {
                val parentDir = file.parentFile
                val fileNameWithoutExt = file.nameWithoutExtension
                
                val potentialLrc = parentDir?.listFiles { _, name ->
                    val lowerName = name.lowercase()
                    (lowerName.startsWith(fileNameWithoutExt.lowercase()) || fileNameWithoutExt.lowercase().startsWith(lowerName.substringBeforeLast("."))) && 
                    (lowerName.endsWith(".lrc") || lowerName.endsWith(".txt") || lowerName.endsWith(".srt"))
                }?.minByOrNull { it.name.length }

                if (potentialLrc != null && potentialLrc.exists()) {
                    val rawContent = potentialLrc.readText(Charsets.UTF_8).trim()
                    lyricsResult = if (potentialLrc.extension.lowercase() == "srt") {
                        convertSrtToLrc(rawContent)
                    } else {
                        rawContent
                    }
                    
                    if (!lyricsResult.isNullOrBlank()) {
                        prefs.edit().putString(path, lyricsResult).apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // metadata in archive
        if (lyricsResult.isNullOrBlank()) {
            lyricsResult = retriever.extractMetadata(28) // 28 es el código para METADATA_KEY_LYRIC
        }

        SongDetails(
            bitrate = kbps,
            sampleRate = hz,
            lyrics = lyricsResult,
            album = album,
            artist = artist,
            year = year,
            trackNumber = track,
            genre = genre,
            resolution = resolution,
            frameRate = frameRate,
            filePath = path
        )
    } catch (e: Exception) {
        SongDetails()
    } finally {
        retriever.release()
    }
}

suspend fun scanAndSaveLyrics(
    context: Context,
    songs: List<Song>,
    onResult: (String) -> Unit = {}
): Pair<Int, Int> = withContext(Dispatchers.IO) {
    var encontradas = 0
    val total = songs.size
    val prefs = context.getSharedPreferences("lyrics_cache", Context.MODE_PRIVATE)

    songs.forEachIndexed { index, song ->
        val savedLyrics = prefs.getString(song.data, null)
        if (!savedLyrics.isNullOrBlank()) {
            song.lyrics = savedLyrics
            encontradas++
            if (index % 5 == 0) onResult("✅ ${song.title}") 
        } else {
            try {
                // local archives lrc
                val baseFile = File(song.data)
                val lrcFile = File(song.data.substringBeforeLast(".") + ".lrc")
                val srtFile = File(song.data.substringBeforeLast(".") + ".srt")
                
                var localLyrics: String? = null
                if (lrcFile.exists()) {
                    localLyrics = lrcFile.readText()
                } else if (srtFile.exists()) {
                    localLyrics = convertSrtToLrc(srtFile.readText())
                }

                if (!localLyrics.isNullOrBlank()) {
                    song.lyrics = localLyrics
                    prefs.edit { putString(song.data, localLyrics) }
                    encontradas++
                    onResult("📄 ${song.title}")
                } else {
                    val details = getAudioMetadata(context, song.data)
                    if (!details.lyrics.isNullOrBlank()) {
                        song.lyrics = details.lyrics
                        prefs.edit { putString(song.data, details.lyrics) }
                        encontradas++
                        onResult("🎵 ${song.title}")
                    } else {
                        if (baseFile.exists()) {
                            val ext = baseFile.extension.lowercase()
                            if (ext != "opus" && ext != "aac" && ext != "m4a") {
                                try {
                                    val audioFile = AudioFileIO.read(baseFile)
                                    val tag = audioFile.tag
                                    val lyrics = tag?.getFirst(FieldKey.LYRICS) ?: tag?.getFirst(FieldKey.CUSTOM1)
                                    
                                    if (!lyrics.isNullOrBlank()) {
                                        song.lyrics = lyrics
                                        prefs.edit { putString(song.data, lyrics) }
                                        encontradas++
                                        onResult("🔍 ${song.title}")
                                    }
                                } catch (e: Exception) {
                                    Log.w("LyricsScan", "Jaudiotagger no pudo leer ${song.title}")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LyricsScan", "Error en ${song.title}: ${e.message}")
            }
        }
    }
    encontradas to total
}

// converts raw text into a list of lyric lines
fun parseSincronizedLyrics(rawLyrics: String): List<LyricLine> {
    val lines = mutableListOf<LyricLine>()
    // Busca cosas como [00:12.34] o [01:05]
    val regex = Regex("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?\\](.*)")

    rawLyrics.lines().forEach { line ->
        val match = regex.find(line)
        if (match != null) {
            val (min, sec, millisMatch, text) = match.destructured

            // calculate time in ms
            val millis = if (millisMatch.isNotEmpty()) {
                if (millisMatch.length == 2) millisMatch.toLong() * 10 else millisMatch.toLong()
            } else 0L

            val timeMs = (min.toLong() * 60 * 1000) + (sec.toLong() * 1000) + millis

            // save if the line contains text
            if (text.trim().isNotEmpty()) {
                lines.add(LyricLine(timeMs, text.trim()))
            }
        }
    }
    return lines.sortedBy { it.timeMs }
}

fun getAlbumArt(path: String): ByteArray? {
    val retriever = MediaMetadataRetriever()
    return try {
        val file = File(path)
        if (file.exists()) {
            val inputStream = FileInputStream(file)
            retriever.setDataSource(inputStream.fd)
            val art = retriever.embeddedPicture
            inputStream.close()
            art
        } else null
    } catch (e: Exception) { null } finally { retriever.release() }
}

fun getAlbumArtUri(albumId: Long): Uri {
    return ContentUris.withAppendedId(
        Uri.parse("content://media/external/audio/albumart"),
        albumId
    )
}

fun embedLyricsToFile(path: String, lyrics: String): Boolean {
    val file = File(path)
    if (!file.exists()) return false
    val ext = file.extension.lowercase()
    if (ext == "opus" || ext == "aac" || ext == "m4a") return false

    return try {
        val audioFile = AudioFileIO.read(file)
        var tag = audioFile.tag
        if (tag == null) {
             tag = ID3v23Tag()
             audioFile.tag = tag
        }
        
        tag.setField(FieldKey.LYRICS, lyrics)
        audioFile.commit()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun updateSongTags(
    path: String,
    title: String,
    artist: String,
    album: String,
    year: String,
    track: String,
    genre: String,
    artwork: ByteArray? = null
): Boolean {
    val file = File(path)
    if (!file.exists()) return false
    val ext = file.extension.lowercase()
    if (ext == "opus" || ext == "aac" || ext == "m4a") return false

    return try {
        val audioFile = AudioFileIO.read(file)
        var tag = audioFile.tag
        if (tag == null) {
            tag = if (path.lowercase().endsWith(".mp3")) ID3v23Tag() else audioFile.createDefaultTag()
            audioFile.tag = tag
        }

        tag.setField(FieldKey.TITLE, title)
        tag.setField(FieldKey.ARTIST, artist)
        tag.setField(FieldKey.ALBUM, album)
        tag.setField(FieldKey.YEAR, year)
        tag.setField(FieldKey.TRACK, track)
        tag.setField(FieldKey.GENRE, genre)

        if (artwork != null) {
            tag.deleteArtworkField()
            val tempFile = File.createTempFile("art", "jpg").apply {
                writeBytes(artwork)
            }
            val artworkObj = ArtworkFactory.createArtworkFromFile(tempFile)
            tag.setField(artworkObj)
            tempFile.delete()
        }

        audioFile.commit()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun convertSrtToLrc(srtContent: String): String {
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
            
            val finalMinutes = if (hours.isNotEmpty()) {
                (hours.toInt() * 60) + fullTime.substringBefore(":").toInt()
            } else {
                fullTime.substringBefore(":").toInt()
            }
            
            currentTimestamp = "[%02d:%s.%s]".format(finalMinutes, fullTime.substringAfter(":"), ms)
        } else if (!trimmed.contains("-->") && currentTimestamp.isNotEmpty()) {
            if (result.none { it.startsWith(currentTimestamp) && it.endsWith(trimmed) }) {
                result.add("$currentTimestamp $trimmed")
            }
        }
    }
    return if (result.isNotEmpty()) result.joinToString("\n") else srtContent
}
