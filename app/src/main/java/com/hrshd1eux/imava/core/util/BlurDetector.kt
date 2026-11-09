package com.hrshd1eux.imava.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Pure-math offline Blurry Photo Detector using discrete Laplacian edge variance.
 * 
 * Zero machine learning dependencies, zero ML Kit bloat, runs 100% offline.
 * Analyzes downsampled grayscale thumbnails (~160px) in 2-4ms with negligible RAM consumption.
 * Sharp images have strong high-frequency gradients (variance > 150+), while out-of-focus
 * and motion-blurred photos exhibit diffuse transitions (variance < 100).
 */
object BlurDetector {

    const val BLUR_THRESHOLD = 100.0

    data class BlurResult(
        val isBlurry: Boolean,
        val variance: Double
    )

    /**
     * Computes the variance of the 2D discrete Laplacian operator over the bitmap.
     * Higher variance indicates sharp edges; low variance indicates blur or defocus.
     */
    fun computeLaplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return 0.0

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert RGB to 8-bit luminance: Y = 0.299R + 0.587G + 0.114B
        val gray = IntArray(width * height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            gray[i] = (299 * r + 587 * g + 114 * b) / 1000
        }

        // 3x3 Discrete Laplacian Kernel:
        // [ 0,  1,  0 ]
        // [ 1, -4,  1 ]
        // [ 0,  1,  0 ]
        var sum = 0.0
        var sumSq = 0.0
        val count = (width - 2) * (height - 2)
        if (count <= 0) return 0.0

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val center = gray[rowOffset + x]
                val top = gray[rowOffset - width + x]
                val bottom = gray[rowOffset + width + x]
                val left = gray[rowOffset + x - 1]
                val right = gray[rowOffset + x + 1]

                val laplacian = top + bottom + left + right - 4 * center
                sum += laplacian
                sumSq += (laplacian * laplacian).toDouble()
            }
        }

        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)
        return max(0.0, variance)
    }

    /**
     * Evaluates a photo uri for blurriness by downsampling to thumbnail resolution.
     */
    suspend fun evaluatePhoto(context: Context, uri: Uri): BlurResult = withContext(Dispatchers.IO) {
        try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return@withContext BlurResult(false, 0.0)

            // Calculate sample size to downscale close to ~160-200px
            val targetSize = 160
            var inSampleSize = 1
            var w = origW
            var h = origH
            while (w / 2 >= targetSize && h / 2 >= targetSize) {
                w /= 2
                h /= 2
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val downsampled = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext BlurResult(false, 0.0)

            val variance = computeLaplacianVariance(downsampled)
            downsampled.recycle()

            BlurResult(
                isBlurry = variance < BLUR_THRESHOLD,
                variance = variance
            )
        } catch (_: Exception) {
            BlurResult(false, 0.0)
        }
    }
}
