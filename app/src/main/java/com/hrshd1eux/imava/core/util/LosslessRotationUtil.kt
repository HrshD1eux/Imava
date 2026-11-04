package com.hrshd1eux.imava.core.util

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import coil.imageLoader
import coil.memory.MemoryCache
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LosslessRotationUtil {

    /**
     * Rotate an image file losslessly by updating its EXIF orientation tag in-place.
     * Takes ~15ms, zero quality loss, zero pixel re-compression.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun rotateLosslessly(
        context: Context,
        item: MediaItem,
        clockwise: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        var success = false
        val file = File(item.path)

        // 1. First attempt: Direct file EXIF write if writable or All Files Access granted
        val hasAllFilesAccess = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            file.canWrite()
        }
        if (file.exists() && (hasAllFilesAccess || file.canWrite())) {
            try {
                val exif = ExifInterface(file.absolutePath)
                val current = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val next = calculateNextOrientation(current, clockwise)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, next.toString())
                exif.saveAttributes()
                success = true
            } catch (e: SecurityException) {
                throw e
            } catch (_: Exception) {}
        }

        // 2. Second attempt: MediaStore ORIENTATION column update (instant, lossless, zero disk recompression)
        if (!success && item.uri != Uri.EMPTY) {
            try {
                val currentDeg = try {
                    context.contentResolver.query(item.uri, arrayOf(android.provider.MediaStore.Images.Media.ORIENTATION), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getInt(0) else 0
                    } ?: 0
                } catch (_: Exception) { 0 }
                val degrees = if (clockwise) 90 else 270
                val newDeg = ((currentDeg + degrees) % 360 + 360) % 360
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.ORIENTATION, newDeg)
                }
                val rows = context.contentResolver.update(item.uri, values, null, null)
                if (rows > 0) {
                    success = true
                }
            } catch (e: SecurityException) {
                throw e
            } catch (_: Exception) {}
        }

        // 3. Third attempt: Scoped storage fallback via ParcelFileDescriptor
        if (!success && item.uri != Uri.EMPTY) {
            try {
                context.contentResolver.openFileDescriptor(item.uri, "rw")?.use { pfd ->
                    val exif = ExifInterface(pfd.fileDescriptor)
                    val current = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val next = calculateNextOrientation(current, clockwise)
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, next.toString())
                    exif.saveAttributes()
                    success = true
                }
            } catch (e: SecurityException) {
                throw e
            } catch (_: Exception) {}
        }

        if (success) {
            // Invalidate Coil thumbnail and full image cache across all key variants
            try {
                val loader = context.imageLoader
                loader.memoryCache?.remove(MemoryCache.Key(item.uri.toString()))
                loader.memoryCache?.remove(MemoryCache.Key("${item.uri}_${item.dateTaken}"))
                loader.diskCache?.remove(item.uri.toString())
                loader.diskCache?.remove("${item.uri}_${item.dateTaken}")
                if (file.exists()) {
                    loader.memoryCache?.remove(MemoryCache.Key(file.absolutePath))
                    loader.memoryCache?.remove(MemoryCache.Key("${file.absolutePath}_${item.dateTaken}"))
                    loader.diskCache?.remove(file.absolutePath)
                }
            } catch (_: Exception) {}

            // Notify Android MediaScanner
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(item.path),
                    arrayOf(item.mimeType),
                    null
                )
            } catch (_: Exception) {}
        }

        success
    }

    private fun calculateNextOrientation(current: Int, clockwise: Boolean): Int {
        return if (clockwise) {
            when (current) {
                ExifInterface.ORIENTATION_NORMAL -> ExifInterface.ORIENTATION_ROTATE_90
                ExifInterface.ORIENTATION_ROTATE_90 -> ExifInterface.ORIENTATION_ROTATE_180
                ExifInterface.ORIENTATION_ROTATE_180 -> ExifInterface.ORIENTATION_ROTATE_270
                ExifInterface.ORIENTATION_ROTATE_270 -> ExifInterface.ORIENTATION_NORMAL
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> ExifInterface.ORIENTATION_TRANSPOSE
                ExifInterface.ORIENTATION_TRANSPOSE -> ExifInterface.ORIENTATION_FLIP_VERTICAL
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> ExifInterface.ORIENTATION_TRANSVERSE
                ExifInterface.ORIENTATION_TRANSVERSE -> ExifInterface.ORIENTATION_FLIP_HORIZONTAL
                else -> ExifInterface.ORIENTATION_ROTATE_90
            }
        } else {
            when (current) {
                ExifInterface.ORIENTATION_NORMAL -> ExifInterface.ORIENTATION_ROTATE_270
                ExifInterface.ORIENTATION_ROTATE_270 -> ExifInterface.ORIENTATION_ROTATE_180
                ExifInterface.ORIENTATION_ROTATE_180 -> ExifInterface.ORIENTATION_ROTATE_90
                ExifInterface.ORIENTATION_ROTATE_90 -> ExifInterface.ORIENTATION_NORMAL
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> ExifInterface.ORIENTATION_TRANSVERSE
                ExifInterface.ORIENTATION_TRANSVERSE -> ExifInterface.ORIENTATION_FLIP_VERTICAL
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> ExifInterface.ORIENTATION_TRANSPOSE
                ExifInterface.ORIENTATION_TRANSPOSE -> ExifInterface.ORIENTATION_FLIP_HORIZONTAL
                else -> ExifInterface.ORIENTATION_ROTATE_270
            }
        }
    }
}