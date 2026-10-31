package com.hrshd1eux.imava.data.media

import android.content.ContentResolver
import android.content.ContentUris
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val QUERY_ARG_MATCH_NOMEDIA = "android:query-arg-match-nomedia"

private val THIRD_PARTY_PATH_PATTERNS = arrayOf(
    "%/WhatsApp/%Sent/%",
    "%/WhatsApp/%Private/%",
    "%/WhatsApp/%Animated Gifs/%",
    "%/WhatsApp/%.Shared/%",
    "%/WhatsApp/%Stickers/%",
    "%/WhatsApp Business/%Sent/%",
    "%/WhatsApp Business/%Private/%",
    "%/GBWhatsApp/%",
    "%/FMWhatsApp/%",
    "%/YoWhatsApp/%",
    "%/WhatsApp Plus/%",
    "%/Telegram/%",
    "%/Discord/%",
    "%/Snapchat/%",
    "%/Signal/%",
    "%/Viber/%",
    "%/%.hidden/%",
    "%/%.secret/%",
    "%/%.vault/%",
    "%/%.private/%",
    "%/%.calculator/%",
    "%/%.nomedia%"
)

private val THIRD_PARTY_SELECTION: String by lazy {
    val pathConditions = THIRD_PARTY_PATH_PATTERNS.joinToString(" OR ") {
        "${MediaStore.Files.FileColumns.DATA} LIKE ?"
    }
    val imageExtensions = "(${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.jpg' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.jpeg' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.png' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.webp' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.gif' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.heic' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.heif' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.bmp' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.dng')"
    val videoExtensions = "(${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.mp4' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.mkv' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.mov' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.avi' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.webm' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.3gp' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.m4v')"
    "($pathConditions) AND ($imageExtensions OR $videoExtensions OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%' OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%')"
}

data class DeepScanResult(
    val newCount: Int,
    val totalCount: Int
)

enum class MediaTypeFilter {
    ALL,
    IMAGES,
    VIDEOS
}

class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver get() = context.contentResolver
    private val _secondaryMediaChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        loadCacheFromDisk()
    }

    fun fetchSecondaryMediaIds(): List<Long> = secondaryDiskMediaCache.map { it.id }

    fun observeMediaStore(): Flow<Unit> = merge(
        callbackFlow {
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
        },
        _secondaryMediaChanges
    )

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
        bucketIds: List<Long>? = null,
        mediaType: MediaTypeFilter = MediaTypeFilter.ALL
    ): Int = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID)
        val mediaTypeCondition = getMediaTypeCondition(mediaType)
        val effectiveBucketIds = if (!bucketIds.isNullOrEmpty()) bucketIds else if (bucketId != null) listOf(bucketId) else null
        val selection = if (!effectiveBucketIds.isNullOrEmpty()) {
            val placeholders = effectiveBucketIds.joinToString(",") { "?" }
            "$mediaTypeCondition AND ${MediaStore.Files.FileColumns.BUCKET_ID} IN ($placeholders)"
        } else {
            mediaTypeCondition
        }
        val selectionArgs = effectiveBucketIds?.map { it.toString() }?.toTypedArray()

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
        bucketIds: List<Long>? = null,
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
            MediaStore.Files.FileColumns.DATE_MODIFIED,
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
        val effectiveBucketIds = if (!bucketIds.isNullOrEmpty()) bucketIds else if (bucketId != null) listOf(bucketId) else null
        val selection = if (!effectiveBucketIds.isNullOrEmpty()) {
            val placeholders = effectiveBucketIds.joinToString(",") { "?" }
            "$mediaTypeCondition AND ${MediaStore.Files.FileColumns.BUCKET_ID} IN ($placeholders)"
        } else {
            mediaTypeCondition
        }

        val selectionArgs = effectiveBucketIds?.map { it.toString() }?.toTypedArray()

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
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
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

        val trashedCondition = "${getMediaTypeCondition(MediaTypeFilter.ALL)} OR (${MediaStore.Files.FileColumns.DATA} LIKE '%.jpg' OR ${MediaStore.Files.FileColumns.DATA} LIKE '%.jpeg' OR ${MediaStore.Files.FileColumns.DATA} LIKE '%.png' OR ${MediaStore.Files.FileColumns.DATA} LIKE '%.webp' OR ${MediaStore.Files.FileColumns.DATA} LIKE '%.mp4' OR ${MediaStore.Files.FileColumns.DATA} LIKE '%.mov')"

        val queryArgs = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                trashedCondition
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
            MediaStore.Files.FileColumns.DATE_MODIFIED,
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
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val selection = getMediaTypeCondition(MediaTypeFilter.ALL)

        val bucketCounts = mutableMapOf<Long, Int>()
        val bucketNames = mutableMapOf<Long, String>()
        val bucketPaths = mutableMapOf<Long, String>()
        val bucketCoverUris = mutableMapOf<Long, Uri>()

        val cursor = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Files.FileColumns.DATE_MODIFIED, MediaStore.Files.FileColumns.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(collection, projection, queryArgs, null)
            } else {
                val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC, ${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
                contentResolver.query(collection, projection, selection, null, sortOrder)
            }
        } catch (_: Exception) {
            null
        }

        cursor?.use {
            val idCol = it.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val bucketIdCol = it.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
            val nameCol = it.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val pathCol = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val mediaTypeCol = it.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (it.moveToNext()) {
                val bIdRaw = if (bucketIdCol != -1) it.getLong(bucketIdCol) else 0L
                val filePath = if (pathCol != -1) it.getString(pathCol) else null
                val bucketId = if (bIdRaw != 0L) {
                    bIdRaw
                } else if (!filePath.isNullOrBlank()) {
                    val parent = filePath.substringBeforeLast('/', "")
                    if (parent.isNotEmpty()) parent.lowercase(java.util.Locale.ROOT).hashCode().toLong() else 0L
                } else {
                    0L
                }
                if (bucketId == 0L) continue

                val count = (bucketCounts[bucketId] ?: 0) + 1
                bucketCounts[bucketId] = count

                if (count == 1) {
                    val parent = if (!filePath.isNullOrBlank()) filePath.substringBeforeLast('/', "") else ""
                    var bucketName = if (nameCol != -1) it.getString(nameCol) else null
                    if (bucketName.isNullOrBlank() || bucketName == "Unknown") {
                        bucketName = if (parent.isNotEmpty()) parent.substringAfterLast('/') else "Unknown"
                    }
                    bucketNames[bucketId] = bucketName
                    if (parent.isNotEmpty()) {
                        bucketPaths[bucketId] = parent
                    }

                    val mediaId = if (idCol != -1) it.getLong(idCol) else 0L
                    if (mediaId > 0L) {
                        val mType = if (mediaTypeCol != -1) it.getInt(mediaTypeCol) else MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                        val baseUri = if (mType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        } else {
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }
                        bucketCoverUris[bucketId] = ContentUris.withAppendedId(baseUri, mediaId)
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
                            bucketPaths[bId] = p.substringBeforeLast('/', "")
                        }
                    }
                    if (!bucketCoverUris.containsKey(bId)) {
                        items.firstOrNull()?.uri?.let { bucketCoverUris[bId] = it }
                    }
                }
            }
        } catch (_: Exception) {}

        val buckets = mutableListOf<BucketInfo>()
        bucketCounts.forEach { (id, count) ->
            buckets.add(BucketInfo(id, bucketNames[id] ?: "Unknown", count, bucketPaths[id], coverUri = bucketCoverUris[id]))
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
            MediaStore.Files.FileColumns.DATE_MODIFIED,
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

        if (secondaryDiskMediaCache.isNotEmpty()) {
            val q = query.trim().lowercase(java.util.Locale.ROOT)
            val existingIds = mediaList.mapTo(HashSet()) { it.id }
            val existingPaths = mediaList.mapTo(HashSet()) { it.path }
            for (cached in secondaryDiskMediaCache) {
                if (cached.id !in existingIds && cached.path !in existingPaths) {
                    if (cached.path.lowercase(java.util.Locale.ROOT).contains(q) ||
                        cached.bucketName.lowercase(java.util.Locale.ROOT).contains(q) ||
                        cached.mimeType.lowercase(java.util.Locale.ROOT).contains(q)
                    ) {
                        mediaList.add(cached)
                    }
                }
            }
        }

        mediaList
    }

    private val validMediaExtensions = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp", "dng",
        "mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "flv", "m4v"
    )

    private val directoriesToSkip = setOf(
        "data", "obb", ".git", ".gradle", "node_modules", ".cache",
        ".thumbnails", ".stversions", ".trash", ".trashed", ".recycle",
        ".temp", ".tmp", "imava_trash"
    )

    private fun getStorageRoots(): List<java.io.File> {
        val roots = mutableListOf<java.io.File>()
        val primary = android.os.Environment.getExternalStorageDirectory()
        if (primary != null && primary.exists() && primary.canRead()) {
            roots.add(primary)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sm = context.getSystemService(android.os.storage.StorageManager::class.java)
                sm?.storageVolumes?.forEach { volume ->
                    val dir = volume.directory
                    if (dir != null && dir.exists() && dir.canRead() && !roots.contains(dir)) {
                        roots.add(dir)
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            androidx.core.content.ContextCompat.getExternalFilesDirs(context, null).forEach { file ->
                if (file != null) {
                    val rootPath = file.absolutePath.substringBefore("/Android/data")
                    val rootFile = java.io.File(rootPath)
                    if (rootFile.exists() && rootFile.canRead() && !roots.contains(rootFile)) {
                        roots.add(rootFile)
                    }
                }
            }
        } catch (_: Exception) {}

        return roots
    }

    private fun fetchAllMediaStorePaths(): Set<String> {
        val paths = HashSet<String>(2048)
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}) OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%' OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%'"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_DEFAULT)
                    // MUST include trashed items so MediaStore-trashed files are recognized as managed and not re-discovered as new photos
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
                    val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    if (dataCol != -1) {
                        while (cursor.moveToNext()) {
                            val p = cursor.getString(dataCol)
                            if (!p.isNullOrBlank()) {
                                paths.add(p)
                            }
                        }
                    }
                }
            } else {
                contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
                    val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    if (dataCol != -1) {
                        while (cursor.moveToNext()) {
                            val p = cursor.getString(dataCol)
                            if (!p.isNullOrBlank()) {
                                paths.add(p)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return paths
    }

    private fun fetchAllTrashedPaths(): Set<String> {
        val trashed = HashSet<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
            try {
                val queryArgs = Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                    putInt(QUERY_ARG_MATCH_NOMEDIA, MediaStore.MATCH_INCLUDE)
                }
                contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
                    val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    if (dataCol != -1) {
                        while (cursor.moveToNext()) {
                            val p = cursor.getString(dataCol)
                            if (!p.isNullOrBlank()) trashed.add(p)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return trashed
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
            MediaStore.Files.FileColumns.DATE_MODIFIED,
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

        val selection = THIRD_PARTY_SELECTION
        val selectionArgs = THIRD_PARTY_PATH_PATTERNS

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
                if (cachedItem.path !in existingPaths) {
                    existingPaths.add(cachedItem.path)
                    mediaList.add(cachedItem)
                }
            }
        }

        mediaList
    }

    private val secondaryDiskMediaCache = java.util.concurrent.CopyOnWriteArrayList<MediaItem>()

    fun evictItem(id: Long, path: String = "") {
        val removed = secondaryDiskMediaCache.removeAll { it.id == id || (path.isNotBlank() && it.path == path) }
        if (removed) {
            saveCacheToDisk()
        }
    }

    private fun saveCacheToDisk() {
        try {
            val file = java.io.File(context.filesDir, "deep_scan_cache.json")
            val jsonArray = org.json.JSONArray()
            for (item in secondaryDiskMediaCache) {
                val obj = org.json.JSONObject()
                obj.put("id", item.id)
                obj.put("path", item.path)
                obj.put("mimeType", item.mimeType)
                obj.put("dateTaken", item.dateTaken)
                obj.put("size", item.size)
                obj.put("width", item.width)
                obj.put("height", item.height)
                obj.put("bucketId", item.bucketId)
                obj.put("bucketName", item.bucketName)
                obj.put("isVideo", item is MediaItem.Video)
                if (item is MediaItem.Video) {
                    obj.put("durationMs", item.durationMs)
                }
                jsonArray.put(obj)
            }
            val tempFile = java.io.File(context.filesDir, "deep_scan_cache.json.tmp")
            tempFile.writeText(jsonArray.toString())
            tempFile.renameTo(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadCacheFromDisk() {
        try {
            val file = java.io.File(context.filesDir, "deep_scan_cache.json")
            if (!file.exists()) return
            val text = file.readText()
            if (text.isBlank()) return
            val jsonArray = org.json.JSONArray(text)
            val items = mutableListOf<MediaItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val path = obj.getString("path")
                val diskFile = java.io.File(path)
                if (!diskFile.exists() ||
                    diskFile.name.startsWith(".trashed", ignoreCase = true) ||
                    diskFile.name.startsWith(".trash", ignoreCase = true) ||
                    diskFile.name.contains(".trashed-", ignoreCase = true) ||
                    path.contains("/imava_trash/") ||
                    path.contains("/.trashed") ||
                    path.contains("/.Trash")
                ) continue
                val id = obj.optLong("id", kotlin.math.abs(path.hashCode().toLong()).coerceAtLeast(1L))
                val mimeType = obj.optString("mimeType", "image/jpeg")
                val dateTaken = obj.optLong("dateTaken", diskFile.lastModified())
                val size = obj.optLong("size", diskFile.length())
                val width = obj.optInt("width", 0)
                val height = obj.optInt("height", 0)
                val bucketId = obj.optLong("bucketId", diskFile.parentFile?.absolutePath?.lowercase(java.util.Locale.ROOT)?.hashCode()?.toLong() ?: 0L)
                val bucketName = obj.optString("bucketName", diskFile.parentFile?.name ?: "Unknown")
                val isVideo = obj.optBoolean("isVideo", mimeType.startsWith("video/"))
                val uri = android.net.Uri.fromFile(diskFile)

                val item = if (isVideo) {
                    MediaItem.Video(
                        id = id,
                        uri = uri,
                        path = path,
                        mimeType = mimeType,
                        dateTaken = dateTaken,
                        size = size,
                        width = width,
                        height = height,
                        durationMs = obj.optLong("durationMs", 0L),
                        bucketId = bucketId,
                        bucketName = bucketName
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
                        bucketId = bucketId,
                        bucketName = bucketName
                    )
                }
                items.add(item)
            }
            secondaryDiskMediaCache.clear()
            secondaryDiskMediaCache.addAll(items)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Filesystem scan that walks WhatsApp Sent, Private, and Telegram media folders in the background,
     * populates secondaryDiskMediaCache, persists to disk, and submits unindexed files to MediaScannerConnection.
     * Returns a DeepScanResult containing newDiscovered count and totalTracked count.
     */
    suspend fun scanSecondaryMediaDirectories(): DeepScanResult = withContext(Dispatchers.IO) {
        var newDiscovered = 0
        try {
            val roots = getStorageRoots()
            val mediaStorePaths = fetchAllMediaStorePaths()
            val trashedPaths = fetchAllTrashedPaths()
            val unindexedFiles = mutableListOf<String>()
            val discoveredItems = mutableListOf<MediaItem>()
            val previouslyKnownPaths = secondaryDiskMediaCache.mapTo(HashSet()) { it.path }
            val visitedDirs = HashSet<String>()

            roots.forEach { root ->
                try {
                    root.walkTopDown()
                        .maxDepth(15)
                        .onEnter { dir ->
                            val name = dir.name
                            val nameLower = name.lowercase(java.util.Locale.ROOT)
                            if ((nameLower == "data" || nameLower == "obb") &&
                                dir.parentFile?.name.equals("Android", ignoreCase = true)
                            ) {
                                return@onEnter false
                            }
                            if (directoriesToSkip.contains(nameLower) ||
                                nameLower.startsWith(".trash") ||
                                nameLower.startsWith(".recycle") ||
                                nameLower.contains("trashed") ||
                                nameLower == "imava_trash" ||
                                nameLower == ".vault" ||
                                (nameLower == "vault" && dir.parentFile?.name == "files")
                            ) {
                                return@onEnter false
                            }
                            val canonical = try { dir.canonicalPath } catch (_: Exception) { dir.absolutePath }
                            if (!visitedDirs.add(canonical)) {
                                return@onEnter false
                            }
                            true
                        }
                        .onFail { _, _ -> }
                        .forEach fileWalk@{ file ->
                            try {
                                if (file.isFile && file.length() > 0L) {
                                    val fileName = file.name
                                    // Skip trashed files and artifacts
                                    if (fileName.startsWith(".trashed", ignoreCase = true) ||
                                        fileName.startsWith(".trash", ignoreCase = true) ||
                                        fileName.contains(".trashed-", ignoreCase = true)
                                    ) {
                                        return@fileWalk
                                    }
                                    val ext = file.extension.lowercase(java.util.Locale.ROOT)
                                    if (validMediaExtensions.contains(ext)) {
                                        val absPath = file.absolutePath
                                        // Skip if known to MediaStore (active or trashed) or in trashed paths
                                        if (absPath !in mediaStorePaths && absPath !in trashedPaths) {
                                            if (absPath !in previouslyKnownPaths) {
                                                newDiscovered++
                                                unindexedFiles.add(absPath)
                                            }
                                            val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                                                ?: if (ext in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "flv", "m4v")) "video/$ext" else "image/jpeg"
                                            val isVideo = mimeType.startsWith("video/") || ext in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "ts", "flv", "m4v")
                                            val parent = file.parentFile
                                            val bucketId = parent?.absolutePath?.lowercase(java.util.Locale.ROOT)?.hashCode()?.toLong() ?: 0L
                                            val parentName = parent?.name ?: "Unknown"
                                            val grandparent = parent?.parentFile
                                            val bucketName = when {
                                                parentName.equals(".Shared", ignoreCase = true) -> "${grandparent?.name ?: "WhatsApp"} Shared"
                                                parentName.equals("WhatsApp Stickers", ignoreCase = true) -> "WhatsApp Stickers"
                                                parentName.startsWith(".") -> parentName.removePrefix(".").replaceFirstChar { it.uppercase() } + " (Hidden)"
                                                (parentName.equals("Sent", ignoreCase = true) || parentName.equals("Private", ignoreCase = true)) &&
                                                    grandparent != null && grandparent.name.isNotBlank() -> "${grandparent.name} $parentName"
                                                else -> parentName
                                            }
                                            val id = kotlin.math.abs(absPath.hashCode().toLong()).coerceAtLeast(1L)
                                            val uri = android.net.Uri.fromFile(file)
                                            val dateTaken = file.lastModified()
                                            val size = file.length()

                                            val item = if (isVideo) {
                                                MediaItem.Video(
                                                    id = id,
                                                    uri = uri,
                                                    path = absPath,
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
                                                    path = absPath,
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
                } catch (_: Exception) {}
            }

            val hasChanges = discoveredItems.size != secondaryDiskMediaCache.size || newDiscovered > 0
            if (discoveredItems.isNotEmpty() || secondaryDiskMediaCache.isNotEmpty()) {
                secondaryDiskMediaCache.clear()
                secondaryDiskMediaCache.addAll(discoveredItems)
                saveCacheToDisk()
            }

            if (unindexedFiles.isNotEmpty()) {
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

            if (hasChanges) {
                _secondaryMediaChanges.tryEmit(Unit)
            }

            return@withContext DeepScanResult(newCount = newDiscovered, totalCount = discoveredItems.size)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext DeepScanResult(newCount = 0, totalCount = secondaryDiskMediaCache.size)
        }
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
    val path: String? = null,
    val mergedBucketIds: List<Long> = listOf(id),
    val paths: List<String> = if (path != null) listOf(path) else emptyList(),
    val coverUri: android.net.Uri? = null
)
