package com.example.mp3

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Calendar
import java.util.Locale

object StatisticsManager {
    
    private const val PREFS_NAME = "music_stats_v2"
    private const val CURRENT_VERSION = 1
    
    private var cachedRecentIds: List<String>? = null

    /**
     * Realiza comprobaciones básicas de integridad y migraciones.
     */
    fun performIntegrityCheck(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val version = prefs.getInt("version", 0)
        
        if (version < CURRENT_VERSION) {
            prefs.edit {
                putInt("version", CURRENT_VERSION)
                // Add migration logic here if needed
            }
        }
        
        // Basic corruption recovery: ensure global stats are not negative
        val all = prefs.all
        if (all.getSafeLong("total_global_time", 0) < 0 || all.getSafeInt("total_plays", 0) < 0) {
            prefs.edit {
                if (all.getSafeLong("total_global_time", 0) < 0) putLong("total_global_time", 0)
                if (all.getSafeInt("total_plays", 0) < 0) putInt("total_plays", 0)
            }
        }
    }

    private fun Map<String, *>.getSafeLong(key: String, defValue: Long): Long {
        val value = this[key] ?: return defValue
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            else -> defValue
        }
    }

    private fun Map<String, *>.getSafeInt(key: String, defValue: Int): Int {
        val value = this[key] ?: return defValue
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            else -> defValue
        }
    }

    // --- TRACKING CORE ---

    /**
     * Registra una reproducción completa o significativa.
     * Incrementa conteo de plays, tiempo, hábitos por hora/día y estadísticas de artista/género.
     */
    fun trackPlay(context: Context, song: Song, millis: Long) {
        if (millis <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        
        val songId = song.id.toString()
        val artist = song.artist
        val album = song.album
        val genre = song.genre ?: "Unknown Genre"
        
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY) // 0-23
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 (Sun) - 7 (Sat)
        
        // Formato para agregación temporal: YYYYMMDD
        val todayKey = String.format(Locale.US, "%04d%02d%02d",
            calendar.get(Calendar.YEAR), 
            calendar.get(Calendar.MONTH) + 1, 
            calendar.get(Calendar.DAY_OF_MONTH))

        prefs.edit {
            // Global & Song Time
            putLong("total_global_time", allData.getSafeLong("total_global_time", 0L) + millis)
            putLong("time_$songId", allData.getSafeLong("time_$songId", 0L) + millis)
            
            // Play Counts
            putInt("plays_$songId", allData.getSafeInt("plays_$songId", 0) + 1)
            putInt("total_plays", allData.getSafeInt("total_plays", 0) + 1)
            
            // Artist Stats
            putInt("artist_plays_$artist", allData.getSafeInt("artist_plays_$artist", 0) + 1)
            putLong("artist_time_$artist", allData.getSafeLong("artist_time_$artist", 0L) + millis)
            
            // Album Stats
            putInt("album_plays_$album", allData.getSafeInt("album_plays_$album", 0) + 1)
            putLong("album_time_$album", allData.getSafeLong("album_time_$album", 0L) + millis)
            
            // Genre Stats
            putInt("genre_plays_$genre", allData.getSafeInt("genre_plays_$genre", 0) + 1)
            
            // Temporal Aggregation (Today)
            putLong("time_day_$todayKey", allData.getSafeLong("time_day_$todayKey", 0L) + millis)
            putInt("plays_day_$todayKey", allData.getSafeInt("plays_day_$todayKey", 0) + 1)
            
            // Per-entity Daily Aggregation
            val dailySongTimeKey = "time_day_${todayKey}_song_$songId"
            val dailySongPlayKey = "plays_day_${todayKey}_song_$songId"
            putLong(dailySongTimeKey, allData.getSafeLong(dailySongTimeKey, 0L) + millis)
            putInt(dailySongPlayKey, allData.getSafeInt(dailySongPlayKey, 0) + 1)
            
            val dailyArtistTimeKey = "time_day_${todayKey}_artist_$artist"
            val dailyArtistPlayKey = "plays_day_${todayKey}_artist_$artist"
            putLong(dailyArtistTimeKey, allData.getSafeLong(dailyArtistTimeKey, 0L) + millis)
            putInt(dailyArtistPlayKey, allData.getSafeInt(dailyArtistPlayKey, 0) + 1)

            val dailyAlbumTimeKey = "time_day_${todayKey}_album_$album"
            val dailyAlbumPlayKey = "plays_day_${todayKey}_album_$album"
            putLong(dailyAlbumTimeKey, allData.getSafeLong(dailyAlbumTimeKey, 0L) + millis)
            putInt(dailyAlbumPlayKey, allData.getSafeInt(dailyAlbumPlayKey, 0) + 1)

            val dailyGenrePlayKey = "plays_day_${todayKey}_genre_$genre"
            putInt(dailyGenrePlayKey, allData.getSafeInt(dailyGenrePlayKey, 0) + 1)

            // Habits (Hourly/Daily)
            putInt("habit_hour_$hour", allData.getSafeInt("habit_hour_$hour", 0) + 1)
            putInt("habit_day_$dayOfWeek", allData.getSafeInt("habit_day_$dayOfWeek", 0) + 1)
            
            // Per-day Habits (for filtered timeline)
            putInt("habit_hour_${hour}_day_$todayKey", allData.getSafeInt("habit_hour_${hour}_day_$todayKey", 0) + 1)
            
            // Last played timestamp
            putLong("last_played_$songId", System.currentTimeMillis())
        }
        
        markAsRecent(context, songId)
    }

    /**
     * Registra el inicio o continuación de una sesión.
     */
    fun startSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val lastEnd = allData.getSafeLong("last_session_end", 0L)
        val now = System.currentTimeMillis()
        
        // Si han pasado más de 30 minutos desde la última actividad, es una sesión nueva
        if (now - lastEnd > 30 * 60 * 1000) {
            prefs.edit {
                putInt("total_sessions", allData.getSafeInt("total_sessions", 0) + 1)
                putLong("current_session_start", now)
            }
        }
    }

    fun endSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val start = allData.getSafeLong("current_session_start", 0L)
        val now = System.currentTimeMillis()
        if (start > 0) {
            val sessionDuration = now - start
            val longest = allData.getSafeLong("longest_session", 0L)
            prefs.edit {
                if (sessionDuration > longest) putLong("longest_session", sessionDuration)
                putLong("last_session_end", now)
            }
        }
    }

    fun trackSkip(context: Context, songId: String?) {
        if (songId == null) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        prefs.edit {
            putInt("skips_$songId", allData.getSafeInt("skips_$songId", 0) + 1)
            putInt("total_skips", allData.getSafeInt("total_skips", 0) + 1)
        }
    }

    // --- RECENT SONGS ---

    fun markAsRecent(context: Context, songId: String?) {
        if (songId == null) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        if (cachedRecentIds == null) {
            val recentString = prefs.getString("recent_songs_v2", "") ?: ""
            cachedRecentIds = recentString.split(",").filter { it.isNotEmpty() }
        }

        val recentList = cachedRecentIds!!.toMutableList()
        recentList.remove(songId)
        recentList.add(0, songId)
        
        val limitedList = recentList.take(50) // Aumentamos a 50
        cachedRecentIds = limitedList
        
        prefs.edit { putString("recent_songs_v2", limitedList.joinToString(",")) }
    }

    fun getRecentSongs(context: Context, allSongs: List<Song>, limit: Int = 10): List<Song> {
        if (cachedRecentIds == null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val recentString = prefs.getString("recent_songs_v2", "") ?: ""
            cachedRecentIds = recentString.split(",").filter { it.isNotEmpty() }
        }
        
        return cachedRecentIds!!.mapNotNull { id ->
            allSongs.find { it.id.toString() == id }
        }.take(limit)
    }

    // --- GETTERS (STATS) ---

    fun getTotalTimeFormatted(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalMillis = prefs.all.getSafeLong("total_global_time", 0L)
        return formatMillis(totalMillis)
    }

    data class DetailedSongStat(val song: Song, val timeMs: Long, val plays: Int)
    data class DetailedStat(val name: String, val timeMs: Long, val plays: Int)

    fun getTopSongsDetailed(context: Context, allSongs: List<Song>, limit: Int = 10, period: String = "All Time"): List<DetailedSongStat> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val keys = getPeriodKeys(period)
        
        return allSongs
            .asSequence() // Use sequence for better performance with large lists
            .map { song ->
                val songId = song.id.toString()
                var time = 0L
                var plays = 0
                
                if (keys.isEmpty()) {
                    time = allData.getSafeLong("time_$songId", 0L)
                    plays = allData.getSafeInt("plays_$songId", 0)
                } else {
                    for (key in keys) {
                        time += allData.getSafeLong("time_day_${key}_song_$songId", 0L)
                        plays += allData.getSafeInt("plays_day_${key}_song_$songId", 0)
                    }
                }
                DetailedSongStat(song, time, plays)
            }
            .filter { it.timeMs > 0 || it.plays > 0 }
            .sortedByDescending { it.timeMs }
            .take(limit)
            .toList()
    }

    fun getTopArtistsDetailed(context: Context, allSongs: List<Song>, limit: Int = 5, period: String = "All Time"): List<DetailedStat> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val artists = allSongs.asSequence().map { it.artist }.distinct()
        val keys = getPeriodKeys(period)
        return artists
            .map { artist ->
                var time = 0L
                var plays = 0
                if (keys.isEmpty()) {
                    time = allData.getSafeLong("artist_time_$artist", 0L)
                    plays = allData.getSafeInt("artist_plays_$artist", 0)
                } else {
                    for (key in keys) {
                        time += allData.getSafeLong("time_day_${key}_artist_$artist", 0L)
                        plays += allData.getSafeInt("plays_day_${key}_artist_$artist", 0)
                    }
                }
                DetailedStat(artist, time, plays)
            }
            .filter { it.timeMs > 0 || it.plays > 0 }
            .sortedByDescending { it.timeMs }
            .take(limit)
            .toList()
    }

    fun getTopAlbumsDetailed(context: Context, allSongs: List<Song>, limit: Int = 5, period: String = "All Time"): List<DetailedStat> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val albums = allSongs.asSequence().map { it.album }.distinct()
        val keys = getPeriodKeys(period)
        return albums
            .map { album ->
                var time = 0L
                var plays = 0
                if (keys.isEmpty()) {
                    time = allData.getSafeLong("album_time_$album", 0L)
                    plays = allData.getSafeInt("album_plays_$album", 0)
                } else {
                    for (key in keys) {
                        time += allData.getSafeLong("time_day_${key}_album_$album", 0L)
                        plays += allData.getSafeInt("plays_day_${key}_album_$album", 0)
                    }
                }
                DetailedStat(album, time, plays)
            }
            .filter { it.timeMs > 0 || it.plays > 0 }
            .sortedByDescending { it.timeMs }
            .take(limit)
            .toList()
    }

    fun getTopSongs(context: Context, allSongs: List<Song>, limit: Int = 10, period: String = "All Time"): List<Song> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val keys = getPeriodKeys(period)
        return allSongs
            .map { song ->
                val time = if (keys.isEmpty()) {
                    allData.getSafeLong("time_${song.id}", 0L)
                } else {
                    keys.sumOf { allData.getSafeLong("time_day_${it}_song_${song.id}", 0L) }
                }
                song to time
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
    }

    fun getTopArtists(context: Context, allSongs: List<Song>, limit: Int = 5): List<Pair<String, Int>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val artists = allSongs.map { it.artist }.distinct()
        return artists
            .map { it to allData.getSafeInt("artist_plays_$it", 0) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun getTopArtistsWithTime(context: Context, allSongs: List<Song>, limit: Int = 5, period: String = "All Time"): List<Pair<String, Long>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val artists = allSongs.map { it.artist }.distinct()
        val keys = getPeriodKeys(period)
        return artists
            .map { artist ->
                val time = if (keys.isEmpty()) {
                    allData.getSafeLong("artist_time_$artist", 0L)
                } else {
                    keys.sumOf { allData.getSafeLong("time_day_${it}_artist_$artist", 0L) }
                }
                artist to time
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun getTopAlbumsWithTime(context: Context, allSongs: List<Song>, limit: Int = 5, period: String = "All Time"): List<Pair<String, Long>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val albums = allSongs.map { it.album }.distinct()
        val keys = getPeriodKeys(period)
        return albums
            .map { album ->
                val time = if (keys.isEmpty()) {
                    allData.getSafeLong("album_time_$album", 0L)
                } else {
                    keys.sumOf { allData.getSafeLong("time_day_${it}_album_$album", 0L) }
                }
                album to time
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun getTopAlbums(context: Context, allSongs: List<Song>, limit: Int = 5): List<Pair<String, Int>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val albums = allSongs.map { it.album }.distinct()
        return albums
            .map { it to allData.getSafeInt("album_plays_$it", 0) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun getTopGenres(context: Context, allSongs: List<Song>, limit: Int = 5, period: String = "All Time"): List<Pair<String, Int>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val genres = allSongs.mapNotNull { it.genre }.distinct()
        val keys = getPeriodKeys(period)
        return genres
            .map { genre ->
                val plays = if (keys.isEmpty()) {
                    allData.getSafeInt("genre_plays_$genre", 0)
                } else {
                    keys.sumOf { allData.getSafeInt("plays_day_${it}_genre_$genre", 0) }
                }
                genre to plays
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun getHourlyHabits(context: Context, period: String = "All Time"): IntArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val habits = IntArray(24)
        val keys = getPeriodKeys(period)
        for (i in 0..23) {
            habits[i] = if (keys.isEmpty()) {
                allData.getSafeInt("habit_hour_$i", 0)
            } else {
                keys.sumOf { allData.getSafeInt("habit_hour_${i}_day_$it", 0) }
            }
        }
        return habits
    }

    fun getDailyHabits(context: Context): IntArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val habits = IntArray(7)
        for (i in 1..7) {
            habits[i-1] = allData.getSafeInt("habit_day_$i", 0)
        }
        return habits
    }

    fun getGeneralStats(context: Context, period: String = "All Time"): Map<String, Any> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val keys = getPeriodKeys(period)

        if (keys.isEmpty()) {
            val totalTime = allData.getSafeLong("total_global_time", 0L)
            val totalSessions = allData.getSafeInt("total_sessions", 0)
            return mapOf(
                "total_plays" to allData.getSafeInt("total_plays", 0),
                "total_skips" to allData.getSafeInt("total_skips", 0),
                "total_time_ms" to totalTime,
                "total_sessions" to totalSessions,
                "longest_session_ms" to allData.getSafeLong("longest_session", 0L),
                "avg_session_ms" to if (totalSessions > 0) totalTime / totalSessions else 0L
            )
        } else {
            val totalPlays = keys.sumOf { allData.getSafeInt("plays_day_$it", 0) }
            val totalTime = keys.sumOf { allData.getSafeLong("time_day_$it", 0L) }
            return mapOf(
                "total_plays" to totalPlays,
                "total_time_ms" to totalTime,
                "total_skips" to 0,
                "total_sessions" to 0,
                "longest_session_ms" to 0L,
                "avg_session_ms" to 0L
            )
        }
    }

    private fun getPeriodKeys(period: String): List<String> {
        val keys = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val sdf = { cal: Calendar -> 
            String.format(Locale.US, "%04d%02d%02d", 
                cal.get(Calendar.YEAR), 
                cal.get(Calendar.MONTH) + 1, 
                cal.get(Calendar.DAY_OF_MONTH)) 
        }

        when (period) {
            "Today" -> keys.add(sdf(calendar))
            "This Week" -> {
                for (i in 0 until 7) {
                    keys.add(sdf(calendar))
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
            }
            "This Month" -> {
                for (i in 0 until 30) {
                    keys.add(sdf(calendar))
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
            }
            "This Year" -> {
                for (i in 0 until 365) {
                    keys.add(sdf(calendar))
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
            }
        }
        return keys
    }

    // --- UTILS ---

    fun getSafeIntFromPrefs(context: Context, key: String, defValue: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.getSafeInt(key, defValue)
    }

    fun getFormattedTime(context: Context, songId: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val millis = prefs.all.getSafeLong("time_$songId", 0L)
        return formatMillis(millis)
    }

    fun getTopSongsWithTime(context: Context, allSongs: List<Song>, limit: Int = 10, period: String = "All Time"): List<Pair<Song, Long>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val keys = getPeriodKeys(period)
        return allSongs
            .map { song ->
                val time = if (keys.isEmpty()) {
                    allData.getSafeLong("time_${song.id}", 0L)
                } else {
                    keys.sumOf { allData.getSafeLong("time_day_${it}_song_${song.id}", 0L) }
                }
                song to time
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun formatMillis(millis: Long): String {
        val totalSeconds = millis / 1000
        val totalMinutes = totalSeconds / 60
        val totalHours = totalMinutes / 60
        
        return when {
            totalHours > 0 -> "${totalHours}h ${totalMinutes % 60}m"
            totalMinutes > 0 -> "${totalMinutes}m ${totalSeconds % 60}s"
            else -> "${totalSeconds}s"
        }
    }
}
