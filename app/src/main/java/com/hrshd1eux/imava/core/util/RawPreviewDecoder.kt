package com.hrshd1eux.imava.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import androidx.exifinterface.media.ExifInterface
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance, zero-bloat Camera RAW preview decoder.
 * Extracts embedded camera JPEG previews from RAW containers (Sony, Canon, Nikon, Fuji, etc.)
 * using low-memory seekable EXIF extraction without loading massive RAW files into RAM.
 */
class RawPreviewDecoder(
    private val context: Context,
    private val sourceResult: SourceResult,
    private val options: Options
) : Decoder {

    companion object {
        val RAW_EXTENSIONS = setOf(
            "arw", "srf", "sr2",
            "cr2", "cr3",
            "nef", "nrw",
            "raf",
            "orf",
            "rw2",
            "pef",
            "srw",
            "dng"
        )

        val RAW_MIME_PREFIXES = listOf(
            "image/x-sony",
            "image/x-canon",
            "image/x-nikon",
            "image/x-fuji",
            "image/x-olympus",
            "image/x-panasonic",
            "image/x-pentax",
            "image/x-samsung",
            "image/x-adobe-dng",
            "image/x-raw"
        )
    }

    override suspend fun decode(): DecodeResult? = withContext(Dispatchers.IO) {
        val file = sourceResult.source.fileOrNull()?.toFile() ?: try {
            sourceResult.source.file().toFile()
        } catch (_: Exception) {
            null
        }

        try {
            // 1. Create ExifInterface without buffering the entire RAW file into RAM
            val exif = if (file != null && file.exists()) {
                ExifInterface(file.absolutePath)
            } else {
                sourceResult.source.source().inputStream().use { stream ->
                    ExifInterface(stream)
                }
            }

            // 2. Extract the embedded camera-graded JPEG preview (typically 200KB - 2MB)
            if (exif.hasThumbnail()) {
                val thumbBytes = exif.thumbnailBytes
                if (thumbBytes != null && thumbBytes.isNotEmpty()) {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes.size, boundsOptions)

                    if (boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
                        val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                        var sample = 1
                        while (maxDim / sample > 4096) {
                            sample *= 2
                        }
                        val decodeOpts = BitmapFactory.Options().apply {
                            inSampleSize = sample
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val rawBmp = BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes.size, decodeOpts)
                        if (rawBmp != null) {
                            val orientation = exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                            )
                            val finalBmp = applyOrientation(rawBmp, orientation)
                            return@withContext DecodeResult(
                                drawable = BitmapDrawable(context.resources, finalBmp),
                                isSampled = (sample > 1)
                            )
                        }
                    }
                }
            }

            // 3. Fallback for DNG files: Android's BitmapFactory can decode DNG directly from file
            if (file != null && file.exists()) {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
                if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                    val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                    var sample = 1
                    while (maxDim / sample > 4096) {
                        sample *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bmp = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                    if (bmp != null) {
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        val finalBmp = applyOrientation(bmp, orientation)
                        return@withContext DecodeResult(
                            drawable = BitmapDrawable(context.resources, finalBmp),
                            isSampled = (sample > 1)
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        null
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    class Factory(private val context: Context) : Decoder.Factory {
        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            val mime = result.mimeType?.lowercase()
            val isRawMime = mime != null && RAW_MIME_PREFIXES.any { mime.startsWith(it) }
            if (isRawMime) {
                return RawPreviewDecoder(context, result, options)
            }

            // Check file extension
            val fileName = result.source.fileOrNull()?.name
            if (fileName != null) {
                val ext = fileName.substringAfterLast('.', "").lowercase()
                if (RAW_EXTENSIONS.contains(ext)) {
                    return RawPreviewDecoder(context, result, options)
                }
            }

            return null
        }
    }
}
