package com.hrshd1eux.imava.core.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max

object DocumentScannerUtil {

    enum class DocumentFilter {
        ORIGINAL,
        BW_HIGH_CONTRAST,
        CLEAN_ENHANCED
    }

    fun calculateWarpDimensions(
        tlX: Float, tlY: Float,
        trX: Float, trY: Float,
        brX: Float, brY: Float,
        blX: Float, blY: Float
    ): Pair<Int, Int> {
        val widthTop = hypot((trX - tlX).toDouble(), (trY - tlY).toDouble())
        val widthBottom = hypot((brX - blX).toDouble(), (brY - blY).toDouble())
        val targetWidth = max(widthTop, widthBottom).toInt().coerceAtLeast(100)

        val heightLeft = hypot((blX - tlX).toDouble(), (blY - tlY).toDouble())
        val heightRight = hypot((brX - trX).toDouble(), (brY - trY).toDouble())
        val targetHeight = max(heightLeft, heightRight).toInt().coerceAtLeast(100)

        return Pair(targetWidth, targetHeight)
    }

    fun calculateWarpDimensions(
        tl: PointF,
        tr: PointF,
        br: PointF,
        bl: PointF
    ): Pair<Int, Int> = calculateWarpDimensions(tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y)

    /**
     * Corrects perspective distortion using Android's native Matrix.setPolyToPoly (API 1+).
     * Zero external libraries, zero OpenCV, fully native hardware acceleration.
     */
    fun warpPerspective(
        source: Bitmap,
        tl: PointF,
        tr: PointF,
        br: PointF,
        bl: PointF
    ): Bitmap {
        val (targetWidth, targetHeight) = calculateWarpDimensions(tl, tr, br, bl)

        val srcPoints = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(source, matrix, paint)

        return output
    }

    /**
     * Applies document enhancement filters (Crisp Black & White or High-Contrast Color).
     */
    fun applyDocumentFilter(bitmap: Bitmap, filter: DocumentFilter): Bitmap {
        return when (filter) {
            DocumentFilter.ORIGINAL -> bitmap
            DocumentFilter.BW_HIGH_CONTRAST -> {
                val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                // High contrast binarization matrix: grayscale + boost contrast + brightness shift
                val cm = ColorMatrix(floatArrayOf(
                    2.0f, 2.0f, 2.0f, 0f, -240f,
                    2.0f, 2.0f, 2.0f, 0f, -240f,
                    2.0f, 2.0f, 2.0f, 0f, -240f,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                output
            }
            DocumentFilter.CLEAN_ENHANCED -> {
                val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                // Gentle document clarity: increase contrast by 1.3x and slightly brighten paper background
                val contrast = 1.35f
                val brightness = 15f
                val cm = ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                ))
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                output
            }
        }
    }

    suspend fun saveDocument(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val fileName = "Doc_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Documents")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, values) ?: return@withContext null

        return@withContext try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, os)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
            null
        }
    }
}
