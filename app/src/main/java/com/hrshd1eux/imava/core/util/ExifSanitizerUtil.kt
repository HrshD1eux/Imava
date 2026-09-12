package com.hrshd1eux.imava.core.util

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ExifSanitizerUtil {

    /**
     * Export a local copy of an image stripped of all GPS, camera serials, and privacy metadata.
     * Saved to Pictures/Cleaned/
     */
    suspend fun exportCleanCopy(
        context: Context,
        item: MediaItem
    ): File? = withContext(Dispatchers.IO) {
        try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val cleanedDir = File(picturesDir, "Cleaned").apply { mkdirs() }

            val origFile = File(item.path)
            val origName = origFile.nameWithoutExtension.ifEmpty { "photo" }
            val ext = origFile.extension.ifEmpty { "jpg" }
            val targetFile = File(cleanedDir, "${origName}_cleaned_${System.currentTimeMillis()}.$ext")

            // Copy file stream
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    if (item.isHidden) {
                        try {
                            VaultCrypto.decrypt(input, output)
                        } catch (_: Exception) {
                            input.copyTo(output)
                        }
                    } else {
                        input.copyTo(output)
                    }
                }
            } ?: return@withContext null

            // Strip metadata via ExifInterface
            if (item.mimeType.contains("image", ignoreCase = true)) {
                stripAllMetadata(targetFile)
            }

            // Register in MediaStore
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(item.mimeType),
                null
            )

            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val SENSITIVE_TAGS = listOf(
        // Dates & Timestamps
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED,

        // GPS / Geolocation
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_DIFFERENTIAL,

        // Hardware / Device Identifiers
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_SPECIFICATION,
        ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,

        // User / System Data & Blobs
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_IMAGE_UNIQUE_ID,
        ExifInterface.TAG_SPECTRAL_SENSITIVITY,
        ExifInterface.TAG_MAKER_NOTE,
        ExifInterface.TAG_XMP
    )

    private fun stripAllMetadata(file: File) {
        stripMetadata(file)
    }

    /**
     * In-place metadata stripping for a given image file.
     * Strips all GPS, timestamps, device serials, maker notes, and XMP payloads.
     */
    fun stripMetadata(file: File) {
        try {
            val exif = ExifInterface(file.absolutePath)
            for (tag in SENSITIVE_TAGS) {
                exif.setAttribute(tag, null)
            }
            exif.saveAttributes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}