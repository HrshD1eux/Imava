package com.hrshd1eux.imava.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.print.PrintHelper
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PrintUtil {

    suspend fun printPhoto(context: Context, item: MediaItem) = withContext(Dispatchers.IO) {
        val printUri: Uri? = try {
            if (item.uri.scheme == "content") {
                item.uri
            } else if (item.path.isNotBlank()) {
                val file = File(item.path)
                if (file.exists()) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else null
            } else null
        } catch (_: Exception) {
            null
        }

        withContext(Dispatchers.Main) {
            try {
                val printHelper = PrintHelper(context).apply {
                    scaleMode = PrintHelper.SCALE_MODE_FIT
                    colorMode = PrintHelper.COLOR_MODE_COLOR
                }
                val fileName = item.path.substringAfterLast('/').ifEmpty { "Photo" }
                val jobName = "Imava_Print_$fileName"

                if (printUri != null) {
                    printHelper.printBitmap(jobName, printUri)
                } else {
                    val bitmap = decodeDownsampledBitmap(context, item.uri)
                    if (bitmap != null) {
                        printHelper.printBitmap(jobName, bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun decodeDownsampledBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
            var sampleSize = 1
            while (maxDim / sampleSize > 2048) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            var rotationDegrees = 0
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                rotationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }

            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }
        } catch (_: Exception) {
            null
        }
    }
}
