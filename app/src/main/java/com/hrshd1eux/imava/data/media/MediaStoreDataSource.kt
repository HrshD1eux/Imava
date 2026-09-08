package com.hrshd1eux.imava.data.media

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.hrshd1eux.imava.data.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val QUERY_ARG_MATCH_NOMEDIA = "android:query-arg-match-nomedia"

enum class MediaTypeFilter {
    ALL,
    IMAGES,
    VIDEOS
}

class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver get() = context.contentResolver

    fun observeMediaStore(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }
        try {
            contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        } catch (_: Exception) {}
        try {
            contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        } catch (_: Exception) {}
        try {
            contentResolver.registerContentObserver(MediaStore.Files.getContentUri("external"), true, observer)
        } catch (_: Exception) {}

        trySend(Unit)

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {}
        }
    }

    private fun getMediaTypeCondition(mediaType: MediaTypeFilter): String {
        val imageExtensions = "(${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.jpg' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.jpeg' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.png' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.webp' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.gif' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.heic' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.heif' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.bmp' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.dng')"
        val videoExtensions = "(${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.mp4' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.mkv' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.mov' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.avi' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.webm' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.3gp' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.ts' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.flv' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.m4v')"

        return when (mediaType) {
            MediaTypeFilter.ALL -> "(${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) OR (${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%' OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%') OR $imageExtensions OR $videoExtensions)"
            MediaTypeFilter.IMAGES -> "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%' OR $imageExtensions)"
            MediaTypeFilter.VIDEOS -> "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO} OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%' OR $videoExtensions)"
        }
    }

    suspend fun getTotalMediaCount(
        bucketId: Long? = null,
        mediaType: MediaTypeFilter = MediaTypeFilter.ALL
    ): Int = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val mediaTypeCondition = getMediaTypeCondition(mediaType)
        val selection = if (bucketId != null) {
            "$mediaTypeCondition AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
        } else {
            mediaTypeCondition
        }
        val selectionArgs = if (bucketId != null) arrayOf(bucketId.toString()) else null

        try {
            val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(collection, projection, queryArgs, null)
            } else {
                contentResolver.query(collection, projection, selection, selectionArgs, null)
            }
            cursor?.use { it.count } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    suspend fun fetchMedia(
        limit: Int,
        offset: Int,
        bucketId: Long? = null,
        includeTrashed: Boolean = false,
        isAscending: Boolean = false,
        mediaType: MediaTypeFilter = MediaTypeFilter.ALL
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val collection = MediaStore.Files.getContentUri("external")

        val projectionList = mutableListOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add("is_trashed")
        }
        val projection = projectionList.toTypedArray()

        val mediaTypeCondition = getMediaTypeCondition(mediaType)
        val selection = if (bucketId != null) {
            "$mediaTypeCondition AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
        } else {
            mediaTypeCondition
        }

        val selectionArgs = if (bucketId != null) {
            arrayOf(bucketId.toString())
        } else {
            null
        }

        val cursor = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    if (limit > 0) {
                        putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    }
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Files.FileColumns.DATE_TAKEN, MediaStore.Files.FileColumns.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        if (isAscending) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    putInt(
                        MediaStore.QUERY_ARG_MATCH_TRASHED,
                        if (includeTrashed) MediaStore.MATCH_INCLUDE else MediaStore.MATCH_DEFAULT
                    )
                    putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(collection, projection, queryArgs, null)
            } else {
                val dir = if (isAscending) "ASC" else "DESC"
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} $dir, ${MediaStore.Files.FileColumns.DATE_ADDED} $dir${if (limit > 0) " LIMIT $limit OFFSET $offset" else ""}"
                contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            }
        } catch (e: Exception) {
            null
        }

        cursor?.use {
            val indices = MediaCursorIndices(it)
            while (it.moveToNext()) {
                mediaList.add(it.extractMediaItem(indices))
            }
        }
        mediaList
    }

    suspend fun fetchTrashedMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@withContext emptyList()
        }
        val mediaList = mutableListOf<MediaItem>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            "is_trashed",
            "date_expires"
        )

        val queryArgs = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                getMediaTypeCondition(MediaTypeFilter.ALL)
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
        }

        val cursor = try {
            contentResolver.query(collection, projection, queryArgs, null)
        } catch (e: Exception) {
            null
        }

        cursor?.use {
            val indices = MediaCursorIndices(it)
            while (it.moveToNext()) {
                mediaList.add(it.extractMediaItem(indices))
            }
        }
        mediaList
    }

    suspend fun fetchActiveMediaIds(): List<Long> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<Long>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val selection = getMediaTypeCondition(MediaTypeFilter.ALL)

        val cursor = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(collection, projection, queryArgs, null)
            } else {
                contentResolver.query(collection, projection, selection, null, "${MediaStore.Files.FileColumns.DATE_ADDED} DESC")
            }
        } catch (_: Exception) {
            null
        }

        cursor?.use {
            val idCol = it.getColumnIndex(MediaStore.Files.FileColumns._ID)
            if (idCol != -1) {
                while (it.moveToNext()) {
                    ids.add(it.getLong(idCol))
                }
            }
        }
        ids
    }

    suspend fun fetchMediaByIds(ids: Set<Long>): List<MediaItem> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()

        val mediaList = mutableListOf<MediaItem>()
        val collection = MediaStore.Files.getContentUri("external")

        val projectionList = mutableListOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add("is_trashed")
        }
        val projection = projectionList.toTypedArray()

        // Chunk IDs to stay well within SQLite variable limits
        ids.chunked(500).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val selection = "${MediaStore.Files.FileColumns._ID} IN ($placeholders) AND ${getMediaTypeCondition(MediaTypeFilter.ALL)}"
            val selectionArgs = chunk.map { it.toString() }.toTypedArray()

            val cursor = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val queryArgs = Bundle().apply {
                        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                        putStringArray(
                            ContentResolver.QUERY_ARG_SORT_COLUMNS,
                            arrayOf(MediaStore.Files.FileColumns.DATE_TAKEN, MediaStore.Files.FileColumns.DATE_ADDED)
                        )
                        putInt(
                            ContentResolver.QUERY_ARG_SORT_DIRECTION,
                            ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                        )
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                        putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
                    }
                    contentResolver.query(collection, projection, queryArgs, null)
                } else {
                    val sortOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC, ${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
                    contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
                }
            } catch (e: Exception) {
                null
            }

            cursor?.use {
                val indices = MediaCursorIndices(it)
                while (it.moveToNext()) {
                    mediaList.add(it.extractMediaItem(indices))
                }
            }
        }
        mediaList
    }

    suspend fun fetchBuckets(): List<BucketInfo> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA
        )
        val selection = getMediaTypeCondition(MediaTypeFilter.ALL)

        val bucketCounts = mutableMapOf<Long, Int>()
        val bucketNames = mutableMapOf<Long, String>()
        val bucketPaths = mutableMapOf<Long, String>()

        val cursor = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(collection, projection, queryArgs, null)
            } else {
                contentResolver.query(collection, projection, selection, null, null)
            }
        } catch (_: Exception) {
            null
        }

        cursor?.use {
            val idCol = it.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
            val nameCol = it.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val pathCol = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)

            if (idCol != -1) {
                while (it.moveToNext()) {
                    var bucketId = it.getLong(idCol)
                    var bucketName = if (nameCol != -1) it.getString(nameCol) else null
                    val filePath = if (pathCol != -1) it.getString(pathCol) else null
                    val parentFile = if (!filePath.isNullOrBlank()) java.io.File(filePath).parentFile else null
                    if (bucketId == 0L && parentFile != null) {
                        bucketId = parentFile.absolutePath.lowercase(java.util.Locale.ROOT).hashCode().toLong()
                    }
                    if (bucketName.isNullOrBlank() || bucketName == "Unknown") {
                        bucketName = parentFile?.name ?: "Unknown"
                    }
                    bucketCounts[bucketId] = (bucketCounts[bucketId] ?: 0) + 1
                    bucketNames[bucketId] = bucketName
                    if (parentFile != null && !bucketPaths.containsKey(bucketId)) {
                        bucketPaths[bucketId] = parentFile.absolutePath
                    }
                }
            }
        }

        // Also ensure known secondary directories (WhatsApp Sent, etc.) are included as buckets if they have files
        try {
            val thirdPartyMedia = fetchThirdPartyAppMedia()
            thirdPartyMedia.groupBy { it.bucketId }.forEach { (bId, items) ->
                val currentCount = bucketCounts[bId] ?: 0
                if (items.size > currentCount) {
                    bucketCounts[bId] = items.size
                    if (!bucketNames.containsKey(bId) || bucketNames[bId] == "Unknown") {
                        bucketNames[bId] = items.firstOrNull()?.bucketName ?: "Unknown"
                    }
                    if (!bucketPaths.containsKey(bId)) {
                        val p = items.firstOrNull()?.path
                        if (!p.isNullOrBlank()) {
                            bucketPaths[bId] = java.io.File(p).parent ?: ""
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val buckets = mutableListOf<BucketInfo>()
        bucketCounts.forEach { (id, count) ->
            buckets.add(BucketInfo(id, bucketNames[id] ?: "Unknown", count, bucketPaths[id]))
        }
        buckets.sortedByDescending { it.count }
    }

    suspend fun searchMedia(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val mediaList = mutableListOf<MediaItem>()
        val collection = MediaStore.Files.getContentUri("external")

        val projectionList = mutableListOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add("is_trashed")
        }
        val projection = projectionList.toTypedArray()

        val cleanQuery = "%${query.trim()}%"
        val mediaTypeCondition = getMediaTypeCondition(MediaTypeFilter.ALL)
        val selection = "$mediaTypeCondition AND (${MediaStore.Files.FileColumns.DATA} LIKE ? OR ${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?)"
        val selectionArgs = arrayOf(cleanQuery, cleanQuery, cleanQuery)

        val cursor = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Files.FileColumns.DATE_TAKEN, MediaStore.Files.FileColumns.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_DEFAULT)
                    putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(collection, projection, queryArgs, null)
            } else {
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC, ${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
                contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            }
        } catch (e: Exception) {
            null
        }

        cursor?.use {
            val indices = MediaCursorIndices(it)
            while (it.moveToNext()) {
                mediaList.add(it.extractMediaItem(indices))
            }
        }
        mediaList
    }

    private val validMediaExtensions = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp", "dng",
        "mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "flv", "m4v"
    )

    private fun getSecondaryTargetDirectories(): List<java.io.File> {
        val externalStorage = android.os.Environment.getExternalStorageDirectory() ?: return emptyList()
        return listOf(
            java.io.File(externalStorage, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent"),
            java.io.File(externalStorage, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Sent"),
            java.io.File(externalStorage, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Private"),
            java.io.File(externalStorage, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Private"),
            java.io.File(externalStorage, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Animated Gifs"),
            java.io.File(externalStorage, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Animated Gifs/Sent"),
            java.io.File(externalStorage, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Images/Sent"),
            java.io.File(externalStorage, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Video/Sent"),
            java.io.File(externalStorage, "Android/media/org.telegram.messenger/Telegram"),
            java.io.File(externalStorage, "Android/media/org.telegram.messenger.web/Telegram"),
            java.io.File(externalStorage, "WhatsApp/Media/WhatsApp Images/Sent"),
            java.io.File(externalStorage, "WhatsApp/Media/WhatsApp Video/Sent"),
            java.io.File(externalStorage, "WhatsApp/Media/WhatsApp Images/Private"),
            java.io.File(externalStorage, "WhatsApp/Media/WhatsApp Video/Private"),
            java.io.File(externalStorage, "WhatsApp/Media/WhatsApp Animated Gifs"),
            java.io.File(externalStorage, "WhatsApp Business/Media/WhatsApp Business Images/Sent"),
            java.io.File(externalStorage, "WhatsApp Business/Media/WhatsApp Business Video/Sent"),
            java.io.File(externalStorage, "Telegram")
        )
    }

    /**
     * Query MediaStore directly for media from third-party apps (WhatsApp, Telegram, etc.)
     * that may have media_type=0 due to .nomedia directories.
     * Also performs direct filesystem fallback discovery for files not yet indexed by MediaStore.
     */
    suspend fun fetchThirdPartyAppMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val collection = MediaStore.Files.getContentUri("external")

        val projectionList = mutableListOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add("is_trashed")
        }
        val projection = projectionList.toTypedArray()

        val pathPatterns = listOf(
            "%/WhatsApp/%Sent/%",
            "%/WhatsApp/%Private/%",
            "%/WhatsApp/%Animated Gifs/%",
            "%/WhatsApp Business/%Sent/%",
            "%/WhatsApp Business/%Private/%",
            "%/Telegram/%",
            "%.nomedia/%"
        )

        val pathConditions = pathPatterns.joinToString(" OR ") {
            "${MediaStore.Files.FileColumns.DATA} LIKE ?"
        }

        val imageExtensions = "(${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.jpg' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.jpeg' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.png' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.webp' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.gif' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.heic' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.heif' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.bmp' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.dng')"
        val videoExtensions = "(${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.mp4' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.mkv' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.mov' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.avi' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.webm' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.3gp' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.m4v')"

        val selection = "($pathConditions) AND ($imageExtensions OR $videoExtensions OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%' OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')"
        val selectionArgs = pathPatterns.toTypedArray()

        val cursor = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Files.FileColumns.DATE_TAKEN, MediaStore.Files.FileColumns.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_DEFAULT)
                    putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(collection, projection, queryArgs, null)
            } else {
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC, ${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
                contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            }
        } catch (_: Exception) {
            null
        }

        cursor?.use {
            val indices = MediaCursorIndices(it)
            while (it.moveToNext()) {
                mediaList.add(it.extractMediaItem(indices))
            }
        }

        // Fast in-memory merge: include any files discovered during secondary directory background scans
        // without performing blocking synchronous disk I/O on every query.
        if (secondaryDiskMediaCache.isNotEmpty()) {
            val existingPaths = mediaList.mapTo(HashSet()) { it.path }
            for (cachedItem in secondaryDiskMediaCache) {
                if (!java.io.File(cachedItem.path).exists()) {
                    secondaryDiskMediaCache.remove(cachedItem)
                } else if (cachedItem.path !in existingPaths) {
                    existingPaths.add(cachedItem.path)
                    mediaList.add(cachedItem)
                }
            }
        }

        mediaList
    }

    private val secondaryDiskMediaCache = java.util.concurrent.CopyOnWriteArrayList<MediaItem>()

    /**
     * Filesystem scan that walks WhatsApp Sent, Private, and Telegram media folders in the background,
     * populates secondaryDiskMediaCache, and submits unindexed files to MediaScannerConnection.
     * Returns count of files scanned.
     */
    suspend fun scanSecondaryMediaDirectories(): Int = withContext(Dispatchers.IO) {
        var scannedCount = 0
        try {
            val secondaryPaths = getSecondaryTargetDirectories()
            val unindexedFiles = mutableListOf<String>()
            val discoveredItems = mutableListOf<MediaItem>()

            secondaryPaths.filter { it.exists() && it.isDirectory }.forEach { dir ->
                try {
                    dir.walkTopDown().maxDepth(5).forEach { file ->
                        if (file.isFile && file.length() > 0L) {
                            val ext = file.extension.lowercase(java.util.Locale.ROOT)
                            if (validMediaExtensions.contains(ext)) {
                                unindexedFiles.add(file.absolutePath)
                                val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                                    ?: if (ext in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "flv", "m4v")) "video/$ext" else "image/jpeg"
                                val isVideo = mimeType.startsWith("video/") || ext in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "flv", "m4v")
                                val parent = file.parentFile
                                val bucketId = parent?.absolutePath?.lowercase(java.util.Locale.ROOT)?.hashCode()?.toLong() ?: 0L
                                val bucketName = parent?.name ?: "Sent"
                                val id = kotlin.math.abs(file.absolutePath.hashCode().toLong()).coerceAtLeast(1L)
                                val uri = android.net.Uri.fromFile(file)
                                val dateTaken = file.lastModified()
                                val size = file.length()

                                val item = if (isVideo) {
                                    MediaItem.Video(
                                        id = id,
                                        uri = uri,
                                        path = file.absolutePath,
                                        mimeType = mimeType,
                                        dateTaken = dateTaken,
                                        size = size,
                                        width = 0,
                                        height = 0,
                                        durationMs = 0L,
                                        bucketId = bucketId,
                                        bucketName = bucketName
                                    )
                                } else {
                                    MediaItem.Photo(
                                        id = id,
                                        uri = uri,
                                        path = file.absolutePath,
                                        mimeType = mimeType,
                                        dateTaken = dateTaken,
                                        size = size,
                                        width = 0,
                                        height = 0,
                                        bucketId = bucketId,
                                        bucketName = bucketName
                                    )
                                }
                                discoveredItems.add(item)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            if (discoveredItems.isNotEmpty()) {
                secondaryDiskMediaCache.clear()
                secondaryDiskMediaCache.addAll(discoveredItems)
            }

            if (unindexedFiles.isNotEmpty()) {
                scannedCount = unindexedFiles.size
                unindexedFiles.chunked(500).forEach { chunk ->
                    val mimeTypes = chunk.map { path ->
                        val ext = path.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
                        android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                            ?: when (ext) {
                                "jpg", "jpeg" -> "image/jpeg"
                                "png" -> "image/png"
                                "webp" -> "image/webp"
                                "gif" -> "image/gif"
                                "heic" -> "image/heic"
                                "heif" -> "image/heif"
                                "dng" -> "image/x-adobe-dng"
                                "bmp" -> "image/bmp"
                                "mp4" -> "video/mp4"
                                "mkv" -> "video/x-matroska"
                                "mov" -> "video/quicktime"
                                "avi" -> "video/x-msvideo"
                                "webm" -> "video/webm"
                                "3gp" -> "video/3gpp"
                                "ts" -> "video/mp2t"
                                "flv" -> "video/x-flv"
                                "m4v" -> "video/x-m4v"
                                else -> null
                            }
                    }.toTypedArray()

                    android.media.MediaScannerConnection.scanFile(
                        context,
                        chunk.toTypedArray(),
                        mimeTypes,
                        null
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        scannedCount
    }

    suspend fun getDatePositionIndex(bucketId: Long? = null, isAscending: Boolean = false): List<com.hrshd1eux.imava.data.repository.DatePositionHeader> = withContext(Dispatchers.IO) {
        val result = mutableListOf<com.hrshd1eux.imava.data.repository.DatePositionHeader>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED
        )
        val mediaTypeCondition = getMediaTypeCondition(MediaTypeFilter.ALL)
        val selection = if (bucketId != null) {
            "$mediaTypeCondition AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ?"
        } else {
            mediaTypeCondition
        }
        val selectionArgs = if (bucketId != null) arrayOf(bucketId.toString()) else null

        val cursor = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Files.FileColumns.DATE_TAKEN, MediaStore.Files.FileColumns.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        if (isAscending) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(collection, projection, queryArgs, null)
            } else {
                val dir = if (isAscending) "ASC" else "DESC"
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} $dir, ${MediaStore.Files.FileColumns.DATE_ADDED} $dir"
                contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            }
        } catch (e: Exception) {
            null
        }

        val zoneId = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zoneId)
        val yesterday = today.minusDays(1)

        cursor?.use {
            val count = it.count
            if (count == 0) return@withContext emptyList()

            val dateCol = it.getColumnIndex(MediaStore.Files.FileColumns.DATE_TAKEN)
            val addedCol = it.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)

            fun readDateAt(rowPos: Int): Long {
                if (!it.moveToPosition(rowPos)) return 0L
                val rawDateTaken = if (dateCol != -1) it.getLong(dateCol) else 0L
                val addedSecs = if (addedCol != -1) it.getLong(addedCol) else 0L
                val addedMs = if (addedSecs > 0) addedSecs * 1000L else 0L
                return if (rawDateTaken > 100000000000L) rawDateTaken else addedMs
            }

            val firstDate = readDateAt(0)
            val lastDate = readDateAt(count - 1)
            val validDates = listOf(firstDate, lastDate).filter { d -> d > 0 }
            val minDate = validDates.minOrNull() ?: System.currentTimeMillis()
            val maxDate = validDates.maxOrNull() ?: System.currentTimeMillis()

            val localMin = java.time.Instant.ofEpochMilli(minDate).atZone(zoneId).toLocalDate()
            val localMax = java.time.Instant.ofEpochMilli(maxDate).atZone(zoneId).toLocalDate()
            val spanDays = kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(localMin, localMax))
            val spanYears = kotlin.math.abs(localMax.year - localMin.year)

            val mode = when {
                spanYears >= 2 || spanDays > 730 -> "YEAR"
                spanDays > 60 -> "MONTH"
                else -> "DAY"
            }

            val yearFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy", java.util.Locale.getDefault())
            val yearShortFormatter = java.time.format.DateTimeFormatter.ofPattern("''yy", java.util.Locale.getDefault())
            val monthFullFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.getDefault())
            val monthShortWithYearFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM ''yy", java.util.Locale.getDefault())
            val monthShortFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.getDefault())
            val dayFullFormatter = java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy", java.util.Locale.getDefault())
            val dayShortFormatter = java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.getDefault())

            fun getHeaderAndLabel(dateMs: Long): Pair<String, String> {
                val ms = if (dateMs > 0) dateMs else System.currentTimeMillis()
                val localDate = java.time.Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate()
                return when (mode) {
                    "YEAR" -> {
                        val title = localDate.format(yearFormatter)
                        val label = localDate.format(yearShortFormatter)
                        Pair(title, label)
                    }
                    "MONTH" -> {
                        val title = localDate.format(monthFullFormatter)
                        val label = if (spanYears > 0) localDate.format(monthShortWithYearFormatter).uppercase()
                                    else localDate.format(monthShortFormatter).uppercase()
                        Pair(title, label)
                    }
                    else -> {
                        when (localDate) {
                            today -> Pair("Today", "TD")
                            yesterday -> Pair("Yesterday", "YS")
                            else -> Pair(localDate.format(dayFullFormatter), localDate.format(dayShortFormatter))
                        }
                    }
                }
            }

            var currentLabel = ""
            val sampleStep = (count / 80).coerceAtLeast(1)
            var pos = 0
            while (pos < count) {
                val dateTaken = readDateAt(pos)
                val (headerTitle, headerLabel) = getHeaderAndLabel(dateTaken)
                if (headerLabel != currentLabel) {
                    currentLabel = headerLabel
                    result.add(com.hrshd1eux.imava.data.repository.DatePositionHeader(headerTitle, pos, headerLabel))
                }
                pos += sampleStep
            }
        }
        result
    }
}

data class BucketInfo(
    val id: Long,
    val name: String,
    val count: Int,
    val path: String? = null
)
