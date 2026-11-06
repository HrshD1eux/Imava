package com.hrshd1eux.imava.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mil.nga.tiff.TIFFImage
import mil.nga.tiff.TiffReader
import mil.nga.tiff.util.TiffConstants
import java.io.ByteArrayOutputStream

/**
 * Lightweight, pure-Java/Kotlin TIFF decoder for Coil.
 * Decodes 8-bit RGB, RGBA, and Grayscale TIFF images safely with downsampling
 * to protect against OutOfMemory errors on ultra-high-resolution print files.
 */
class TiffDecoder(
    private val context: Context,
    private val sourceResult: SourceResult,
    private val options: Options
) : Decoder {

    override suspend fun decode(): DecodeResult? = withContext(Dispatchers.IO) {
        try {
            val bytes = sourceResult.source.source().use { bufferedSource ->
                val baos = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                val input = bufferedSource.inputStream()
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    baos.write(buffer, 0, read)
                }
                baos.toByteArray()
            }
            if (bytes.size < 8) return@withContext null

            // Validate TIFF byte-order signature: 'II*\0' (Little-Endian) or 'MM\0*' (Big-Endian)
            val isLeTiff = bytes[0] == 'I'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 42.toByte()
            val isBeTiff = bytes[0] == 'M'.code.toByte() && bytes[1] == 'M'.code.toByte() && bytes[3] == 42.toByte()
            if (!isLeTiff && !isBeTiff) return@withContext null

            val tiffImage: TIFFImage = TiffReader.readTiff(bytes)
            val dir = tiffImage.fileDirectory ?: return@withContext null
            val rasters = dir.readRasters() ?: return@withContext null

            val rawWidth = rasters.width
            val rawHeight = rasters.height
            if (rawWidth <= 0 || rawHeight <= 0) return@withContext null

            // Downsample if dimensions exceed 4096px (standard GPU texture limit)
            val maxDim = maxOf(rawWidth, rawHeight)
            val step = maxOf(1, maxDim / 4096)
            val width = rawWidth / step
            val height = rawHeight / step

            val pixels = IntArray(width * height)
            val photoInterpretation = dir.photometricInterpretation
            val isRgb = photoInterpretation == TiffConstants.PHOTOMETRIC_INTERPRETATION_RGB
            val isBlackIsZero = photoInterpretation == TiffConstants.PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO
            val isWhiteIsZero = photoInterpretation == TiffConstants.PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO
            val samplesPerPixel = rasters.samplesPerPixel

            var destIdx = 0
            for (y in 0 until height) {
                val srcY = y * step
                for (x in 0 until width) {
                    val srcX = x * step
                    val color = when {
                        isRgb -> {
                            val r = rasters.getPixelSample(0, srcX, srcY).toInt().coerceIn(0, 255)
                            val g = if (samplesPerPixel > 1) rasters.getPixelSample(1, srcX, srcY).toInt().coerceIn(0, 255) else r
                            val b = if (samplesPerPixel > 2) rasters.getPixelSample(2, srcX, srcY).toInt().coerceIn(0, 255) else r
                            val a = if (samplesPerPixel >= 4) rasters.getPixelSample(3, srcX, srcY).toInt().coerceIn(0, 255) else 255
                            (a shl 24) or (r shl 16) or (g shl 8) or b
                        }
                        isBlackIsZero || isWhiteIsZero -> {
                            var gray = rasters.getPixelSample(0, srcX, srcY).toInt().coerceIn(0, 255)
                            if (isWhiteIsZero) gray = 255 - gray
                            (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
                        }
                        else -> {
                            // Fallback to first available sample
                            val sample = rasters.getPixelSample(0, srcX, srcY).toInt().coerceIn(0, 255)
                            (0xFF shl 24) or (sample shl 16) or (sample shl 8) or sample
                        }
                    }
                    pixels[destIdx++] = color
                }
            }

            val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
            DecodeResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = (step > 1)
            )
        } catch (_: Exception) {
            null
        }
    }

    class Factory(private val context: Context) : Decoder.Factory {
        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            val mime = result.mimeType?.lowercase()
            val isTiffMime = mime == "image/tiff" || mime == "image/tif"
            if (isTiffMime) {
                return TiffDecoder(context, result, options)
            }

            // Sniff byte signature if MIME is generic (e.g. application/octet-stream)
            return try {
                val source = result.source.source()
                if (source.request(4)) {
                    val b0 = source.buffer[0]
                    val b1 = source.buffer[1]
                    val isLe = b0 == 'I'.code.toByte() && b1 == 'I'.code.toByte() && source.buffer[2] == 42.toByte()
                    val isBe = b0 == 'M'.code.toByte() && b1 == 'M'.code.toByte() && source.buffer[3] == 42.toByte()
                    if (isLe || isBe) TiffDecoder(context, result, options) else null
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
}
