package com.hrshd1eux.imava.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

object TesseractOcrEngine {

    private const val MAX_OCR_DIMENSION = 2560

    suspend fun recognizeText(
        context: Context,
        uri: Uri
    ): Result<String> = withContext(Dispatchers.Default) {
        if (!OcrModelManager.isModelReady(context)) {
            return@withContext Result.failure(IllegalStateException("Tesseract English model is not downloaded."))
        }

        val bitmap = loadOptimizedBitmap(context, uri)
            ?: return@withContext Result.failure(IllegalArgumentException("Could not load image from URI"))

        try {
            recognizeFromBitmap(context, bitmap)
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    suspend fun recognizeText(
        context: Context,
        sourceBitmap: Bitmap
    ): Result<String> = withContext(Dispatchers.Default) {
        if (!OcrModelManager.isModelReady(context)) {
            return@withContext Result.failure(IllegalStateException("Tesseract English model is not downloaded."))
        }

        val scaled = scaleBitmapIfNeeded(sourceBitmap)
        try {
            recognizeFromBitmap(context, scaled)
        } finally {
            if (scaled !== sourceBitmap && !scaled.isRecycled) {
                scaled.recycle()
            }
        }
    }

    private fun recognizeFromBitmap(context: Context, bitmap: Bitmap): Result<String> {
        val tess = TessBaseAPI()
        return try {
            val dataPath = context.filesDir.absolutePath
            val success = tess.init(dataPath, "eng")
            if (!success) {
                return Result.failure(Exception("Tesseract initialization failed for path: $dataPath"))
            }

            tess.setImage(bitmap)
            val utf8 = tess.utF8Text ?: ""
            Result.success(utf8.trim())
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                tess.recycle()
            } catch (_: Exception) {}
        }
    }

    private fun loadOptimizedBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val resolver = context.contentResolver

            // 1. Decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            // 2. Compute sample size
            val maxDim = max(options.outWidth, options.outHeight)
            var sampleSize = 1
            while (maxDim / (sampleSize * 2) >= MAX_OCR_DIMENSION) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDim = max(bitmap.width, bitmap.height)
        if (maxDim <= MAX_OCR_DIMENSION) return bitmap

        val scale = MAX_OCR_DIMENSION.toFloat() / maxDim.toFloat()
        val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
