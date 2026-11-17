package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {
    private const val MAX_DIMENSION = 640
    private const val MAX_BASE64_LENGTH = 160_000

    private val bitmapCache = object : LruCache<Int, ImageBitmap>(24) {}

    fun createCameraOutputUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "bps_capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun compressAndWatermarkUri(
        context: Context,
        uri: Uri,
        activeShift: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

            var sampleSize = 1
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / sampleSize > MAX_DIMENSION * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                ?: return@withContext null

            val rotated = rotateIfRequired(bytes, rawBitmap)
            val scaled = scaleDown(rotated, MAX_DIMENSION)
            if (rotated !== rawBitmap) rawBitmap.recycle()
            if (scaled !== rotated) rotated.recycle()

            val watermarked = applySecurityWatermark(scaled, activeShift)
            if (watermarked !== scaled) scaled.recycle()

            var quality = 72
            var base64: String
            do {
                val out = ByteArrayOutputStream()
                watermarked.compress(Bitmap.CompressFormat.JPEG, quality, out)
                base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                quality -= 10
            } while (base64.length > MAX_BASE64_LENGTH && quality >= 30)

            watermarked.recycle()
            if (base64.length > MAX_BASE64_LENGTH) null else base64
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        val key = base64.hashCode()
        bitmapCache.get(key)?.let { return it }
        return try {
            val raw = Base64.decode(base64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null
            val imageBitmap = bmp.asImageBitmap()
            bitmapCache.put(key, imageBitmap)
            imageBitmap
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val raw = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(raw, 0, raw.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun rotateIfRequired(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(bytes.inputStream())
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) return bitmap
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun applySecurityWatermark(source: Bitmap, activeShift: String): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val barHeight = (result.height * 0.085f).coerceIn(24f, 38f)

        val barPaint = Paint().apply {
            color = Color.argb(185, 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            0f,
            result.height - barHeight,
            result.width.toFloat(),
            result.height.toFloat(),
            barPaint
        )

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val label = "SECURITY PLOT BPS • $activeShift • $timeStr WIB"

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(251, 191, 36)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = (barHeight * 0.48f).coerceIn(11f, 16f)
        }
        val textBounds = Rect()
        textPaint.getTextBounds(label, 0, label.length, textBounds)
        val yPos = result.height - (barHeight - textBounds.height()) / 2f
        canvas.drawText(label, 12f, yPos, textPaint)
        return result
    }
}
