package com.hrshd1eux.imava.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.exifinterface.media.ExifInterface
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * High-performance, zero-bloat Camera RAW preview decoder.
 * Extracts pre-rendered camera-graded JPEG previews embedded within
 * Sony (.arw), Canon (.cr2, .cr3), Nikon (.nef), Fujifilm (.raf),
 * Olympus (.orf), Panasonic (.rw2), Pentax (.pef), and Samsung (.srw) RAW containers.
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
        val file = sourceResult.source.fileOrNull()?.toFile()

        // 1. First attempt: Use Android OS native hardware thumbnail provider (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && file != null) {
            try {
                val fileUri = Uri.fromFile(file)
                val targetSize = Size(4096, 4096)
                val bmp = context.contentResolver.loadThumbnail(fileUri, targetSize, null)
                if (bmp != null) {
                    return@withContext DecodeResult(
                        drawable = BitmapDrawable(context.resources, bmp),
                        isSampled = false
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. Second attempt: Extract embedded preview JPEG directly via ExifInterface
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
            if (bytes.size > 128) {
                val exif = ExifInterface(ByteArrayInputStream(bytes))
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

                // 3. Third attempt: Direct stream decode fallback (for DNG / TIFF-like streams)
                val decodeOpts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val directBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                if (directBmp != null) {
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val finalBmp = applyOrientation(directBmp, orientation)
                    return@withContext DecodeResult(
                        drawable = BitmapDrawable(context.resources, finalBmp),
                        isSampled = false
                    )
                }
            }
        } catch (_: Exception) {}

        null
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
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
