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
    suspend fun rotateLosslessly(
        context: Context,
        item: MediaItem,
        clockwise: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            var success = false
            val file = File(item.path)

            if (file.exists() && file.canWrite()) {
                val exif = ExifInterface(file.absolutePath)
                val current = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val next = calculateNextOrientation(current, clockwise)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, next.toString())
                exif.saveAttributes()
                success = true
            } else if (item.uri != Uri.EMPTY) {
                // Scoped storage fallback via ParcelFileDescriptor
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
            }

            if (success) {
                // Invalidate Coil thumbnail and full image cache
                try {
                    val loader = context.imageLoader
                    loader.memoryCache?.remove(MemoryCache.Key(item.uri.toString()))
                    loader.diskCache?.remove(item.uri.toString())
                    if (file.exists()) {
                        loader.memoryCache?.remove(MemoryCache.Key(file.absolutePath))
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
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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