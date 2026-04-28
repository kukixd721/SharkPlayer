package com.example.mp3
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object StatisticsManager {
    
    private var cachedRecentIds: List<String>? = null

    private fun SharedPreferences.getSafeLong(key: String, defValue: Long): Long {
        val value = all[key] ?: return defValue
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            else -> defValue
        }
    }

    // Suma tiempo (en milisegundos) a una canción y al total global
    fun addTimePlayed(context: Context, songId: String?, millis: Long) {
        if (songId == null || millis <= 0) return
        val prefs = context.getSharedPreferences("music_stats", Context.MODE_PRIVATE)
        
        // Tiempo por canción
        val currentTime = prefs.getSafeLong(songId, 0L)
        
        // Tiempo total global
        val totalGlobal = prefs.getSafeLong("total_global_time", 0L)
        
        prefs.edit {
            putLong(songId, currentTime + millis)
            putLong("total_global_time", totalGlobal + millis)
        }
    }

    // Registra una canción como "reciente"
    fun markAsRecent(context: Context, songId: String?) {
        if (songId == null) return
        val prefs = context.getSharedPreferences("music_stats", Context.MODE_PRIVATE)
        
        // Inicializar cache si es necesario
        if (cachedRecentIds == null) {
            val recentString = prefs.getString("recent_songs", "") ?: ""
            cachedRecentIds = recentString.split(",").filter { it.isNotEmpty() }
        }

        val recentList = cachedRecentIds!!.toMutableList()
        
        // Evitar duplicados y mover al principio
        recentList.remove(songId)
        recentList.add(0, songId)
        
        // Limitar a 20 recientes y actualizar cache
        val limitedList = recentList.take(20)
        cachedRecentIds = limitedList
        
        prefs.edit { putString("recent_songs", limitedList.joinToString(",")) }
    }

    // Obtiene las canciones escuchadas recientemente (optimizado con cache)
    fun getRecentSongs(context: Context, allSongs: List<Song>, limit: Int = 10): List<Song> {
        if (cachedRecentIds == null) {
            val prefs = context.getSharedPreferences("music_stats", Context.MODE_PRIVATE)
            val recentString = prefs.getString("recent_songs", "") ?: ""
            cachedRecentIds = recentString.split(",").filter { it.isNotEmpty() }
        }
        
        return cachedRecentIds!!.mapNotNull { id ->
            allSongs.find { it.id.toString() == id }
        }.take(limit)
    }

    // Obtiene el tiempo total escuchado formateado con más precisión
    fun getTotalTimeFormatted(context: Context): String {
        val prefs = context.getSharedPreferences("music_stats", Context.MODE_PRIVATE)
        val totalMillis = prefs.getSafeLong("total_global_time", 0L)
        val totalSeconds = totalMillis / 1000
        val totalMinutes = totalSeconds / 60
        
        return when {
            totalMinutes < 1 -> "${totalSeconds} seg"
            totalMinutes < 60 -> "${totalMinutes} min"
            else -> {
                val hours = totalMinutes / 60.0
                "%.1f h".format(hours)
            }
        }
    }

    // Obtiene las canciones ordenadas por MINUTOS totales
    fun getTopSongs(context: Context, allSongs: List<Song>, limit: Int = 10): List<Song> {
        val prefs = context.getSharedPreferences("music_stats", Context.MODE_PRIVATE)
        return allSongs
            .map { it to prefs.getSafeLong(it.id.toString(), 0L) }
            .filter { it.second >= 0 } // Para que muestre todo, incluso lo que tiene 0 tiempo
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
    }

    // Obtiene las canciones ordenadas por milisegundos totales (para gráficos)
    fun getTopSongsWithTime(context: Context, allSongs: List<Song>, limit: Int = 5): List<Pair<Song, Long>> {
        val prefs = context.getSharedPreferences("music_stats", Context.MODE_PRIVATE)
        return allSongs
            .map { it to prefs.getSafeLong(it.id.toString(), 0L) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
    }

    // Función auxiliar para mostrar "5 min" o "1.2 hrs"
    fun getFormattedTime(context: Context, songId: String): String {
        val prefs = context.getSharedPreferences("music_stats", Context.MODE_PRIVATE)
        val totalMillis = prefs.getSafeLong(songId, 0L)
        val totalMinutes = totalMillis / 1000 / 60
        return if (totalMinutes < 60) {
            "$totalMinutes min"
        } else {
            val hours = totalMinutes / 60.0
            "%.1f hrs".format(hours)
        }
    }
}
