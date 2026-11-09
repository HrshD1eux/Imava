package com.hrshd1eux.imava.data.media

import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import com.hrshd1eux.imava.data.model.MediaItem

class MediaCursorIndices(cursor: Cursor) {
    val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
    val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
    val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
    val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_TAKEN)
    val addedCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
    val modifiedCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
    val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
    val widthCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.WIDTH)
    val heightCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.HEIGHT)
    val orientCol = cursor.getColumnIndex("orientation")
    val durCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DURATION)
    val bucketIdCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
    val bucketNameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
    val mediaTypeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
    val trashedCol = cursor.getColumnIndex("is_trashed")
    val expiresCol = cursor.getColumnIndex("date_expires")
    val pendingCol = cursor.getColumnIndex("is_pending")
}

fun Cursor.extractMediaItem(indices: MediaCursorIndices): MediaItem {
    val id = if (indices.idCol != -1) getLong(indices.idCol) else 0L
    val path = if (indices.dataCol != -1) (getString(indices.dataCol) ?: "") else ""
    val rawMimeType = if (indices.mimeCol != -1) getString(indices.mimeCol) else null
    val ext = path.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
    val mimeType = if (!rawMimeType.isNullOrBlank() && rawMimeType != "application/octet-stream") {
        rawMimeType
    } else {
        android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
            "mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "flv", "m4v" -> "video/$ext"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            "dng" -> "image/x-adobe-dng"
            "bmp" -> "image/bmp"
            "svg" -> "image/svg+xml"
            "avif" -> "image/avif"
            "arw", "srf", "sr2" -> "image/x-sony-arw"
            "cr2" -> "image/x-canon-cr2"
            "cr3" -> "image/x-canon-cr3"
            "nef" -> "image/x-nikon-nef"
            "nrw" -> "image/x-nikon-nrw"
            "raf" -> "image/x-fuji-raf"
            "orf" -> "image/x-olympus-orf"
            "rw2" -> "image/x-panasonic-rw2"
            "pef" -> "image/x-pentax-pef"
            "srw" -> "image/x-samsung-srw"
            else -> "image/jpeg"
        }
    }

    val rawDateTaken = if (indices.dateCol != -1) getLong(indices.dateCol) else 0L
    val addedSecs = if (indices.addedCol != -1) getLong(indices.addedCol) else 0L
    val addedMs = if (addedSecs > 0) addedSecs * 1000L else 0L
    val modSecs = if (indices.modifiedCol != -1) getLong(indices.modifiedCol) else 0L
    val modMs = if (modSecs > 0) modSecs * 1000L else 0L

    val dateTaken = when {
        rawDateTaken > 100000000000L -> rawDateTaken
        rawDateTaken > 100000000L -> rawDateTaken * 1000L
        addedMs > 0 -> addedMs
        modMs > 0 -> modMs
        else -> 0L
    }

    val size = if (indices.sizeCol != -1) getLong(indices.sizeCol) else 0L
    val rawWidth = if (indices.widthCol != -1) getInt(indices.widthCol) else 0
    val rawHeight = if (indices.heightCol != -1) getInt(indices.heightCol) else 0
    val orientation = if (indices.orientCol != -1) getInt(indices.orientCol) else 0
    val (width, height) = if (orientation == 90 || orientation == 270) {
        rawHeight to rawWidth
    } else {
        rawWidth to rawHeight
    }

    val parentPath = if (path.isNotBlank()) path.substringBeforeLast('/', "") else ""
    val bucketIdVal = if (indices.bucketIdCol != -1 && getLong(indices.bucketIdCol) != 0L) {
        getLong(indices.bucketIdCol)
    } else if (parentPath.isNotEmpty()) {
        parentPath.lowercase(java.util.Locale.ROOT).hashCode().toLong()
    } else {
        0L
    }
    val rawBucketName = if (indices.bucketNameCol != -1) getString(indices.bucketNameCol) else null
    val bucketName = if (!rawBucketName.isNullOrBlank() && rawBucketName != "Unknown") {
        rawBucketName
    } else if (parentPath.isNotEmpty()) {
        parentPath.substringAfterLast('/')
    } else {
        "Unknown"
    }

    val mediaType = if (indices.mediaTypeCol != -1) getInt(indices.mediaTypeCol) else MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE

    val isTrashedPath = path.contains("/imava_trash/") ||
        path.contains("/.trashed") ||
        path.contains("/.trash") ||
        path.contains("/.recycle") ||
        path.substringAfterLast('/').startsWith(".trashed", ignoreCase = true) ||
        path.substringAfterLast('/').startsWith(".trash", ignoreCase = true)
    val isTrashedSystem = (if (indices.trashedCol != -1) getInt(indices.trashedCol) == 1 else false) || isTrashedPath
    val expiresSec = if (indices.expiresCol != -1) getLong(indices.expiresCol) else 0L
    val expiresMs = expiresSec * 1000L
    val computedTrashTime = if (expiresMs > 0L) {
        (expiresMs - 30L * 24L * 60L * 60L * 1000L).coerceAtLeast(0L)
    } else if (isTrashedSystem) {
        System.currentTimeMillis()
    } else {
        0L
    }

    val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ||
        mimeType.startsWith("video/") ||
        when (ext) {
            "mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "flv", "m4v" -> true
            else -> false
        }

    // Files with media_type=0 (e.g. WhatsApp Sent in .nomedia dirs) cannot be resolved
    // via images/videos content URIs — use the generic Files URI which resolves any media_type.
    val uri = when {
        id <= 0L && path.isNotBlank() ->
            android.net.Uri.fromFile(java.io.File(path))
        mediaType != MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE &&
        mediaType != MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
            ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
        isVideo ->
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
        else ->
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
    }

    val isPendingSystem = if (indices.pendingCol != -1) getInt(indices.pendingCol) == 1 else false

    return if (isVideo) {
        val duration = if (indices.durCol != -1) getLong(indices.durCol) else 0L
        MediaItem.Video(
            id = id,
            uri = uri,
            path = path,
            mimeType = mimeType,
            dateTaken = dateTaken,
            size = size,
            width = width,
            height = height,
            durationMs = duration,
            bucketId = bucketIdVal,
            bucketName = bucketName,
            isTrashed = isTrashedSystem,
            trashTime = computedTrashTime,
            isPending = isPendingSystem
        )
    } else {
        MediaItem.Photo(
            id = id,
            uri = uri,
            path = path,
            mimeType = mimeType,
            dateTaken = dateTaken,
            size = size,
            width = width,
            height = height,
            bucketId = bucketIdVal,
            bucketName = bucketName,
            isTrashed = isTrashedSystem,
            trashTime = computedTrashTime,
            isPending = isPendingSystem
        )
    }
}
