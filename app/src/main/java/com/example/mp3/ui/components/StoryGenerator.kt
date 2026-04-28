package com.example.mp3.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import com.example.mp3.Song
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit

object StoryGenerator {

    fun generateAndShare(
        context: Context,
        song: Song,
        albumArt: ByteArray?,
        primaryColor: Int,
        secondaryColor: Int,
        lyricsSnippet: String? = null,
        currentPosition: Long = 0L
    ) {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Boost colors for a more vibrant background
        val boostedPrimary = boostColor(primaryColor)
        val boostedSecondary = boostColor(secondaryColor)

        // 1. Fondo degradado refinado (Diagonal)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            boostedPrimary, boostedSecondary,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // 1.1 Fondo de textura (Arte del álbum expandido)
        val artBitmap = albumArt?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
        
        if (artBitmap != null) {
            val matrix = Matrix()
            val scale = height.toFloat() / artBitmap.height.toFloat()
            matrix.postScale(scale, scale)
            matrix.postTranslate((width - artBitmap.width * scale) / 2f, 0f)
            
            val texturePaint = Paint(Paint.FILTER_BITMAP_FLAG)
            texturePaint.alpha = 45 // Muy sutil
            canvas.drawBitmap(artBitmap, matrix, texturePaint)
        }

        // Capa de oscurecimiento sutil
        paint.color = AndroidColor.BLACK
        paint.alpha = 50
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val artSize = 780f // Ligeramente más pequeña para que quepa el vinilo
        val artLeft = (width - artSize) / 2f - 70f // Desplazada más a la izquierda para el "peek"
        val artTop = 420f
        val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)

        // 2. Vinyl Peek Effect
        drawVinylPeek(canvas, artRect)

        // Sombra de la carátula
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        shadowPaint.setShadowLayer(65f, 0f, 35f, AndroidColor.argb(130, 0, 0, 0))
        canvas.drawRoundRect(artRect, 40f, 40f, shadowPaint)

        if (artBitmap != null) {
            val roundedArt = getRoundedCornerBitmap(artBitmap, 40f)
            canvas.drawBitmap(roundedArt, null, artRect, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            paint.color = AndroidColor.parseColor("#44FFFFFF")
            paint.alpha = 255
            canvas.drawRoundRect(artRect, 40f, 40f, paint)
            
            // Icono de nota musical si no hay arte
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                alpha = 100
                textSize = 200f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("♪", artRect.centerX(), artRect.centerY() + 70f, iconPaint)
        }

        // 3. Textos con sombra para legibilidad
        paint.color = AndroidColor.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.alpha = 255
        paint.setShadowLayer(10f, 0f, 5f, AndroidColor.argb(100, 0, 0, 0))

        // Título de la canción
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 72f
        val titleY = artTop + artSize + 180f
        val truncatedTitle = truncateText(song.title, 22)
        canvas.drawText(truncatedTitle, width / 2f, titleY, paint)

        // Artista
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 48f
        paint.alpha = 210
        val truncatedArtist = truncateText(song.artist, 30)
        canvas.drawText(truncatedArtist, width / 2f, titleY + 85f, paint)
        
        paint.clearShadowLayer() // Quitar sombra para el resto

        // Sticker: Duración y Barra de Progreso
        drawDurationSticker(canvas, song.duration, width, artTop)
        drawProgressBar(canvas, currentPosition, song.duration, width, titleY - 110f)

        // Sticker: Letra (si está disponible)
        if (!lyricsSnippet.isNullOrBlank()) {
            drawLyricsSticker(canvas, lyricsSnippet, width, titleY + 220f)
        }

        // 4. Branding / Footer
        drawBranding(canvas, width, height)

        // 5. Efecto de Grano/Ruido (Final Layer)
        applyGrainEffect(canvas, width, height)

        // 6. Guardar y Compartir
        shareBitmap(context, bitmap, "shark_story_${System.currentTimeMillis()}.png")
    }

    private fun drawVinylPeek(canvas: Canvas, artRect: RectF) {
        val vinylSize = artRect.height() * 0.95f
        val vinylLeft = artRect.centerX() + (artRect.width() * 0.05f)
        val vinylTop = artRect.centerY() - (vinylSize / 2f)
        val vinylRect = RectF(vinylLeft, vinylTop, vinylLeft + vinylSize, vinylTop + vinylSize)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Cuerpo del vinilo
        paint.color = AndroidColor.parseColor("#121212")
        canvas.drawCircle(vinylRect.centerX(), vinylRect.centerY(), vinylSize / 2f, paint)

        // Brillo del vinilo (concéntrico)
        paint.style = Paint.Style.STROKE
        paint.color = AndroidColor.WHITE
        for (i in 1..4) {
            paint.alpha = 15 - (i * 2)
            paint.strokeWidth = 2f
            canvas.drawCircle(vinylRect.centerX(), vinylRect.centerY(), (vinylSize / 2f) * (0.9f - (i * 0.1f)), paint)
        }

        // Etiqueta central (coloreada sutilmente)
        paint.style = Paint.Style.FILL
        paint.color = AndroidColor.WHITE
        paint.alpha = 40
        canvas.drawCircle(vinylRect.centerX(), vinylRect.centerY(), vinylSize * 0.15f, paint)
        
        // Agujero central
        paint.color = AndroidColor.BLACK
        paint.alpha = 255
        canvas.drawCircle(vinylRect.centerX(), vinylRect.centerY(), vinylSize * 0.02f, paint)
    }

    private fun applyGrainEffect(canvas: Canvas, width: Int, height: Int) {
        val noiseSize = 256
        val noiseBitmap = Bitmap.createBitmap(noiseSize, noiseSize, Bitmap.Config.ARGB_8888)
        val random = Random()
        val pixels = IntArray(noiseSize * noiseSize)
        
        for (i in pixels.indices) {
            val noise = random.nextInt(256)
            // Usamos un alfa muy bajo y el modo Overlay o Screen para textura cinematográfica
            pixels[i] = AndroidColor.argb(25, noise, noise, noise)
        }
        noiseBitmap.setPixels(pixels, 0, noiseSize, 0, 0, noiseSize, noiseSize)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val shader = BitmapShader(noiseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        paint.shader = shader
        // SRC_OVER con este alfa bajo funciona como una capa de ruido sutil
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Limpieza
        noiseBitmap.recycle()
    }

    private fun drawBranding(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.5f
        }

        // "LISTENING ON" pequeño
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textSize = 28f
        paint.alpha = 100
        canvas.drawText("LISTENING ON", width / 2f, height - 160f, paint)

        // "SHARK MUSIC" destacado
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 42f
        paint.alpha = 180
        canvas.drawText("SHARK MUSIC", width / 2f, height - 110f, paint)

        // Línea decorativa
        paint.alpha = 40
        val lineY = height - 195f
        canvas.drawRect(width / 2f - 100f, lineY, width / 2f + 100f, lineY + 2f, paint)
    }

    private fun drawLyricsSticker(canvas: Canvas, lyrics: String, width: Int, startY: Float) {
        val lines = lyrics.replace(Regex("\\[\\d{2}:\\d{2}.*?\\]"), "")
            .trim()
            .split("\n")
            .filter { it.isNotBlank() }
            .take(5) // Aumentamos a 5 líneas para la selección manual

        if (lines.isEmpty()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 46f // Un poco más grande
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
        }

        // Fondo para la letra (Sólido translúcido)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            alpha = 80 // Un poco más oscuro para que resalte la letra
        }
        
        val boxWidth = 920f
        val lineSpacing = 75f
        val boxHeight = lines.size * lineSpacing + 80f
        val boxRect = RectF(
            width / 2f - boxWidth / 2f,
            startY - 60f,
            width / 2f + boxWidth / 2f,
            startY + boxHeight - 60f
        )
        canvas.drawRoundRect(boxRect, 35f, 35f, bgPaint)

        // Comillas grandes decorativas
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            alpha = 40
            textSize = 180f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }
        canvas.drawText("“", boxRect.left + 40f, boxRect.top + 130f, quotePaint)

        var currentY = startY + 20f
        for (line in lines) {
            val truncatedLine = truncateText(line.trim(), 45)
            paint.alpha = 255
            canvas.drawText(truncatedLine, width / 2f, currentY, paint)
            currentY += lineSpacing
        }
    }

    private fun drawProgressBar(canvas: Canvas, current: Long, total: Long, width: Int, y: Float) {
        val barWidth = 700f
        val barHeight = 8f
        val left = (width - barWidth) / 2f
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Fondo de la barra
        paint.color = AndroidColor.WHITE
        paint.alpha = 60
        canvas.drawRoundRect(left, y, left + barWidth, y + barHeight, 4f, 4f, paint)
        
        // Progreso
        if (total > 0) {
            val progress = current.toFloat() / total.toFloat()
            paint.alpha = 255
            canvas.drawRoundRect(left, y, left + (barWidth * progress), y + barHeight, 4f, 4f, paint)
            
            // Puntito de progreso
            canvas.drawCircle(left + (barWidth * progress), y + barHeight / 2f, 12f, paint)
        }
    }

    private fun boostColor(color: Int): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        // Ensure saturation is at least 0.5 and lightness is around 0.4-0.6 for vibrancy
        hsl[1] = Math.max(hsl[1], 0.55f)
        hsl[2] = Math.max(0.35f, Math.min(hsl[2], 0.65f))
        return ColorUtils.HSLToColor(hsl)
    }

    private fun drawDurationSticker(canvas: Canvas, duration: Long, width: Int, artTop: Float) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        
        val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        stickerPaint.color = AndroidColor.argb(60, 255, 255, 255)
        
        val stickerWidth = 160f
        val stickerRect = RectF(
            width / 2f - stickerWidth / 2f,
            artTop - 100f,
            width / 2f + stickerWidth / 2f,
            artTop - 40f
        )
        canvas.drawRoundRect(stickerRect, 30f, 30f, stickerPaint)
        
        stickerPaint.color = AndroidColor.WHITE
        stickerPaint.textSize = 32f
        stickerPaint.textAlign = Paint.Align.CENTER
        stickerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(timeStr, width / 2f, artTop - 60f, stickerPaint)
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, pixels, pixels, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) text.substring(0, maxLength - 3) + "..." else text
    }

    private fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String) {
        try {
            val cachePath = File(context.cacheDir, "shared_stories")
            cachePath.mkdirs()
            val file = File(cachePath, fileName)
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir historia"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
