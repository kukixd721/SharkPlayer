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

    fun performIntegrityCheck(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val version = prefs.getInt("version", 0)
        
        if (version < CURRENT_VERSION) {
            prefs.edit {
                putInt("version", CURRENT_VERSION)
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

    fun trackPlay(context: Context, song: Song, millis: Long, isNewPlay: Boolean = false) {
        trackPlay(context, song.id.toString(), song.title, song.artist, song.album, song.genre, millis, isNewPlay)
    }

    fun trackPlay(context: Context, songId: String, title: String, artist: String, album: String, genre: String?, millis: Long, isNewPlay: Boolean = true) {
        if (millis < 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val actualGenre = genre ?: "Unknown Genre"
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val todayKey = String.format(Locale.US, "%04d%02d%02d",
            calendar.get(Calendar.YEAR), 
            calendar.get(Calendar.MONTH) + 1, 
            calendar.get(Calendar.DAY_OF_MONTH))

        prefs.edit {
            if (millis > 0) {
                putLong("total_global_time", prefs.getLong("total_global_time", 0L) + millis)
                putLong("time_$songId", prefs.getLong("time_$songId", 0L) + millis)
                putLong("artist_time_$artist", prefs.getLong("artist_time_$artist", 0L) + millis)
                putLong("album_time_$album", prefs.getLong("album_time_$album", 0L) + millis)
                putLong("time_day_$todayKey", prefs.getLong("time_day_$todayKey", 0L) + millis)
                putLong("time_day_${todayKey}_song_$songId", prefs.getLong("time_day_${todayKey}_song_$songId", 0L) + millis)
                putLong("time_day_${todayKey}_artist_$artist", prefs.getLong("time_day_${todayKey}_artist_$artist", 0L) + millis)
                putLong("time_day_${todayKey}_album_$album", prefs.getLong("time_day_${todayKey}_album_$album", 0L) + millis)
            }
            
            if (isNewPlay) {
                putInt("plays_$songId", prefs.getInt("plays_$songId", 0) + 1)
                putInt("total_plays", prefs.getInt("total_plays", 0) + 1)
                putInt("artist_plays_$artist", prefs.getInt("artist_plays_$artist", 0) + 1)
                putInt("album_plays_$album", prefs.getInt("album_plays_$album", 0) + 1)
                putInt("genre_plays_$actualGenre", prefs.getInt("genre_plays_$actualGenre", 0) + 1)
                putInt("plays_day_$todayKey", prefs.getInt("plays_day_$todayKey", 0) + 1)
                putInt("plays_day_${todayKey}_song_$songId", prefs.getInt("plays_day_${todayKey}_song_$songId", 0) + 1)
                putInt("plays_day_${todayKey}_artist_$artist", prefs.getInt("plays_day_${todayKey}_artist_$artist", 0) + 1)
                putInt("plays_day_${todayKey}_album_$album", prefs.getInt("plays_day_${todayKey}_album_$album", 0) + 1)
                putInt("plays_day_${todayKey}_genre_$actualGenre", prefs.getInt("plays_day_${todayKey}_genre_$actualGenre", 0) + 1)
                putInt("habit_hour_$hour", prefs.getInt("habit_hour_$hour", 0) + 1)
                putInt("habit_day_$dayOfWeek", prefs.getInt("habit_day_$dayOfWeek", 0) + 1)
                putInt("habit_hour_${hour}_day_$todayKey", prefs.getInt("habit_hour_${hour}_day_$todayKey", 0) + 1)
                putLong("last_played_$songId", System.currentTimeMillis())
                markAsRecent(context, songId)
            }
        }
    }

    fun startSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastEnd = prefs.getLong("last_session_end", 0L)
        val now = System.currentTimeMillis()
        
        if (now - lastEnd > 30 * 60 * 1000) {
            prefs.edit {
                putInt("total_sessions", prefs.getInt("total_sessions", 0) + 1)
                putLong("current_session_start", now)
            }
        }
    }

    fun endSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val start = prefs.getLong("current_session_start", 0L)
        val now = System.currentTimeMillis()
        if (start > 0) {
            val sessionDuration = now - start
            val longest = prefs.getLong("longest_session", 0L)
            prefs.edit {
                if (sessionDuration > longest) putLong("longest_session", sessionDuration)
                putLong("last_session_end", now)
            }
        }
    }

    fun trackSkip(context: Context, songId: String?) {
        if (songId == null) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putInt("skips_$songId", prefs.getInt("skips_$songId", 0) + 1)
            putInt("total_skips", prefs.getInt("total_skips", 0) + 1)
        }
    }

    // --- RECENT SONGS ---

    fun markAsRecent(context: Context, songId: String?) {
        if (songId == null) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val recentString = prefs.getString("recent_songs_v2", "") ?: ""
        val recentList = recentString.split(",").filter { it.isNotEmpty() }.toMutableList()
        
        recentList.remove(songId)
        recentList.add(0, songId)
        
        val limitedList = recentList.take(50)
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

    // --- GETTERS ---

    fun getTotalTimeFormatted(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return formatMillis(prefs.getLong("total_global_time", 0L))
    }

    data class DetailedSongStat(val song: Song, val timeMs: Long, val plays: Int)
    data class DetailedStat(val name: String, val timeMs: Long, val plays: Int)

    fun getTopSongsDetailed(context: Context, allSongs: List<Song>, limit: Int = 10, period: String = "All Time"): List<DetailedSongStat> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val keys = getPeriodKeys(period)
        
        return allSongs
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

    fun getTopGenres(context: Context, allSongs: List<Song>, limit: Int = 5, period: String = "All Time"): List<Pair<String, Int>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val genres = allSongs.map { it.genre ?: "Unknown Genre" }.distinct()
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

    fun getGeneralStats(context: Context, period: String = "All Time"): Map<String, Any> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all
        val keys = getPeriodKeys(period)

        if (keys.isEmpty()) {
            val totalTime = prefs.getLong("total_global_time", 0L)
            val totalSessions = prefs.getInt("total_sessions", 0)
            return mapOf(
                "total_plays" to prefs.getInt("total_plays", 0),
                "total_skips" to prefs.getInt("total_skips", 0),
                "total_time_ms" to totalTime,
                "total_sessions" to totalSessions,
                "longest_session_ms" to prefs.getLong("longest_session", 0L),
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

    fun getFormattedTime(context: Context, songId: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return formatMillis(prefs.getLong("time_$songId", 0L))
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
