package com.example.mp3

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream

/**
 * APRENDIZAJE: Singleton (object)
 * En Kotlin, 'object' crea una única instancia de esta clase (Singleton).
 * Es ideal para Managers o Utilities que no necesitan múltiples copias.
 */
object PlaylistManager {
    private const val PREFS_NAME = "playlists_data"
    private const val NAMES_KEY = "playlist_names"
    private const val IMAGE_PREFIX = "img_"

    /**
     * Obtiene los nombres de todas las playlists creadas.
     */
    fun getPlaylistNames(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(NAMES_KEY, emptySet()) ?: emptySet()
    }

    /**
     * Crea una nueva lista de reproducción vacía.
     */
    fun createPlaylist(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val names = getPlaylistNames(context).toMutableSet()
        names.add(name)
        prefs.edit { putStringSet(NAMES_KEY, names) }
    }

    /**
     * Cambia el nombre de una lista de reproducción y mueve sus datos.
     */
    fun renamePlaylist(context: Context, oldName: String, newName: String) {
        if (oldName == newName) return
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val names = getPlaylistNames(context).toMutableSet()
        
        if (names.contains(oldName)) {
            names.remove(oldName)
            names.add(newName)
            
            // Mover canciones
            val songIds = prefs.getStringSet("songs_$oldName", emptySet())
            // Mover imagen personalizada si existe
            val customImage = prefs.getString("$IMAGE_PREFIX$oldName", null)
            
            prefs.edit { 
                putStringSet(NAMES_KEY, names)
                putStringSet("songs_$newName", songIds)
                remove("songs_$oldName")
                
                if (customImage != null) {
                    putString("$IMAGE_PREFIX$newName", customImage)
                    remove("$IMAGE_PREFIX$oldName")
                }
            }
        }
    }

    /**
     * Asocia una imagen personalizada a la playlist.
     * Si se proporciona un URI, se copia a la memoria interna de la app para persistencia.
     */
    fun setPlaylistImage(context: Context, playlistName: String, imageUri: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        if (imageUri == null) {
            // Borrar imagen vieja si existe
            getPlaylistImage(context, playlistName)?.let { File(it).delete() }
            prefs.edit { remove("$IMAGE_PREFIX$playlistName") }
            return
        }

        try {
            val uri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val dir = File(context.filesDir, "playlist_covers")
                if (!dir.exists()) dir.mkdirs()
                
                val file = File(dir, "cover_${playlistName.hashCode()}_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)
                
                // Borrar imagen anterior antes de guardar la nueva
                getPlaylistImage(context, playlistName)?.let { File(it).delete() }

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                
                val finalPath = file.absolutePath
                prefs.edit { putString("$IMAGE_PREFIX$playlistName", finalPath) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Obtiene la ruta de la imagen personalizada de la playlist.
     */
    fun getPlaylistImage(context: Context, playlistName: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("$IMAGE_PREFIX$playlistName", null)
    }

    /**
     * Borra una lista de reproducción y sus canciones asociadas.
     */
    fun deletePlaylist(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val names = getPlaylistNames(context).toMutableSet()
        names.remove(name)
        prefs.edit { 
            putStringSet(NAMES_KEY, names)
            remove("songs_$name")
            remove("$IMAGE_PREFIX$name")
        }
    }

    /**
     * Obtiene los IDs de las canciones guardadas en una lista específica.
     */
    fun getSongsInPlaylist(context: Context, playlistName: String): Set<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet("songs_$playlistName", emptySet()) ?: emptySet()
        return ids.mapNotNull { it.toLongOrNull() }.toSet()
    }

    /**
     * Añade una canción a una lista.
     * APRENDIZAJE: Usamos IDs (Long) en lugar de objetos completos para ahorrar memoria
     * y evitar redundancia si los datos de la canción cambian.
     */
    fun addSongToPlaylist(context: Context, playlistName: String, songId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = getSongsInPlaylist(context, playlistName).map { it.toString() }.toMutableSet()
        ids.add(songId.toString())
        prefs.edit { putStringSet("songs_$playlistName", ids) }
    }
}
