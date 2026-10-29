package com.hrshd1eux.imava.data.repository

import com.hrshd1eux.imava.data.database.MediaMetadataEntity
import com.hrshd1eux.imava.data.database.MetadataDao
import com.hrshd1eux.imava.data.media.BucketInfo
import com.hrshd1eux.imava.data.media.MediaStoreDataSource
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import android.content.Context
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.hrshd1eux.imava.data.media.MediaTypeFilter
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val metadataDao: MetadataDao
) : MediaRepository {

    private suspend fun getMetadataForMediaIdsChunked(ids: List<Long>): List<MediaMetadataEntity> {
        if (ids.isEmpty()) return emptyList()
        val result = mutableListOf<MediaMetadataEntity>()
        ids.chunked(500).forEach { chunk ->
            result.addAll(metadataDao.getMetadataForMediaIds(chunk))
        }
        return result
    }

    private suspend fun deleteMetadataByIdsChunked(ids: List<Long>) {
        if (ids.isEmpty()) return
        ids.chunked(500).forEach { chunk ->
            metadataDao.deleteMetadataByIds(chunk)
        }
    }

    override fun getMediaFlow(bucketId: Long?, sortOrder: com.hrshd1eux.imava.ui.SortOrder, mediaType: MediaTypeFilter): Flow<List<MediaItem>> {
        return combine(
            mediaStoreDataSource.observeMediaStore().onStart { emit(Unit) },
            metadataDao.getAllMetadataFlow().onStart { emit(emptyList()) }
        ) { _, _ ->
            loadMediaPaged(limit = Int.MAX_VALUE, offset = 0, bucketId = bucketId, sortOrder = sortOrder, mediaType = mediaType)
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun getCleanedThirdPartyMedia(bucketId: Long?, mediaType: MediaTypeFilter): List<MediaItem> {
        val raw = fetchThirdPartyAppMedia()
        val filtered = when (mediaType) {
            MediaTypeFilter.ALL -> raw
            MediaTypeFilter.IMAGES -> raw.filterIsInstance<MediaItem.Photo>()
            MediaTypeFilter.VIDEOS -> raw.filterIsInstance<MediaItem.Video>()
        }
        val relevant = if (bucketId != null) filtered.filter { it.bucketId == bucketId } else filtered
        if (relevant.isEmpty()) return emptyList()

        val ids = relevant.map { it.id }
        val metaList = getMetadataForMediaIdsChunked(ids)
        val metaMap = metaList.associateBy { it.mediaId }

        val hiddenOrTrashedMeta = metadataDao.getHiddenOrTrashedMetadata()
        val trashedPaths = hiddenOrTrashedMeta.filter { it.isTrashed && it.originalPath.isNotBlank() }.map { it.originalPath }.toSet()
        val hiddenPaths = hiddenOrTrashedMeta.filter { it.isHidden && it.originalPath.isNotBlank() }.map { it.originalPath }.toSet()

        return relevant.map { item ->
            applyMetadata(item, metaMap[item.id])
        }.filter { item ->
            !item.isHidden && !item.isTrashed &&
            item.path !in trashedPaths &&
            item.path !in hiddenPaths &&
            !item.path.contains("/imava_trash/") &&
            !item.path.contains("/.trashed") &&
            !item.path.contains("/.Trash") &&
            !item.path.substringAfterLast('/').startsWith(".trashed", ignoreCase = true) &&
            !item.path.substringAfterLast('/').startsWith(".trash", ignoreCase = true)
        }
    }

    override suspend fun loadMediaPaged(
        limit: Int,
        offset: Int,
        bucketId: Long?,
        bucketIds: List<Long>?,
        sortOrder: com.hrshd1eux.imava.ui.SortOrder,
        mediaType: MediaTypeFilter
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val isAscending = (sortOrder == com.hrshd1eux.imava.ui.SortOrder.OLDEST_FIRST)
        val thirdPartyMedia = try {
            getCleanedThirdPartyMedia(bucketId, mediaType)
        } catch (_: Exception) {
            emptyList()
        }

        // Fast path: Bucket belongs entirely to third party media (e.g. WhatsApp Sent, .nomedia folders)
        val baseCount = mediaStoreDataSource.getTotalMediaCount(bucketId, bucketIds, mediaType)
        if (baseCount == 0 && thirdPartyMedia.isNotEmpty()) {
            val sorted = if (isAscending) thirdPartyMedia.sortedBy { it.dateTaken } else thirdPartyMedia.sortedByDescending { it.dateTaken }
            return@withContext sorted.drop(offset).take(limit)
        }

        // Fetch from MediaStore with the loop to filter out hidden/trashed items
        val sortedThirdParty = if (isAscending) thirdPartyMedia.sortedBy { it.dateTaken } else thirdPartyMedia.sortedByDescending { it.dateTaken }
        val unindexedBefore = if (thirdPartyMedia.isNotEmpty()) {
            ((offset.toLong() * sortedThirdParty.size) / (baseCount + sortedThirdParty.size).coerceAtLeast(1)).toInt().coerceIn(0, sortedThirdParty.size)
        } else 0
        val storeOffset = if (limit == Int.MAX_VALUE) 0 else (offset - unindexedBefore).coerceAtLeast(0)

        var currentOffset = storeOffset
        val resultList = mutableListOf<MediaItem>()
        var reachedEnd = false

        while (resultList.size < limit && !reachedEnd) {
            val remaining = limit - resultList.size
            val fetchBatch = if (remaining == Int.MAX_VALUE) 1000 else minOf(remaining, 1000)
            val rawMedia = mediaStoreDataSource.fetchMedia(
                limit = fetchBatch,
                offset = currentOffset,
                bucketId = bucketId,
                bucketIds = bucketIds,
                isAscending = isAscending,
                mediaType = mediaType
            )
            if (rawMedia.isEmpty()) {
                break
            }
            val ids = rawMedia.map { it.id }
            val metaMap = getMetadataForMediaIdsChunked(ids).associateBy { it.mediaId }
            val filtered = rawMedia.map { applyMetadata(it, metaMap[it.id]) }.filter { !it.isHidden && !it.isTrashed }
            resultList.addAll(filtered)
            currentOffset += rawMedia.size
            if (rawMedia.size < fetchBatch) {
                break
            }
        }

        if (thirdPartyMedia.isEmpty()) {
            return@withContext if (isAscending) {
                resultList.sortedBy { it.dateTaken }
            } else {
                resultList.sortedByDescending { it.dateTaken }
            }
        }

        if (limit == Int.MAX_VALUE) {
            val existingIds = resultList.mapTo(HashSet()) { it.id }
            val existingPaths = resultList.mapTo(HashSet()) { it.path }
            val newThirdParty = sortedThirdParty.filter { it.id !in existingIds && it.path !in existingPaths }
            resultList.addAll(newThirdParty)
            return@withContext if (isAscending) {
                resultList.sortedBy { it.dateTaken }
            } else {
                resultList.sortedByDescending { it.dateTaken }
            }
        }

        val thirdPartySlice = sortedThirdParty.drop(unindexedBefore).take(limit)
        val combined = (resultList + thirdPartySlice).distinctBy { it.path }
        val sortedCombined = if (isAscending) combined.sortedBy { it.dateTaken } else combined.sortedByDescending { it.dateTaken }
        return@withContext sortedCombined.take(limit)
    }

    override suspend fun getTotalMediaCount(
        bucketId: Long?,
        bucketIds: List<Long>?,
        mediaType: MediaTypeFilter
    ): Int = withContext(Dispatchers.IO) {
        val baseCount = mediaStoreDataSource.getTotalMediaCount(bucketId, bucketIds, mediaType)
        try {
            val relevant = getCleanedThirdPartyMedia(bucketId, mediaType)
            if (baseCount == 0) {
                relevant.size
            } else {
                baseCount + relevant.size
            }
        } catch (_: Exception) {
            baseCount
        }
    }

    override fun observeMediaChanges(): Flow<Unit> {
        return mediaStoreDataSource.observeMediaStore()
    }

    override suspend fun getBuckets(): List<BucketInfo> = withContext(Dispatchers.IO) {
        val rawBuckets = mediaStoreDataSource.fetchBuckets()
        val metadataList = metadataDao.getHiddenOrTrashedMetadata()
        
        val hiddenOrTrashedCounts = metadataList
            .filter { it.isHidden || it.isTrashed }
            .groupBy { it.bucketId }
            .mapValues { it.value.size }
            
        val userPrefs = context.getSharedPreferences("user_albums", Context.MODE_PRIVATE)
        val createdAlbums = userPrefs.getStringSet("created_albums", emptySet()) ?: emptySet()

        val processedBuckets = rawBuckets.map { bucket ->
            val subtractCount = hiddenOrTrashedCounts[bucket.id] ?: 0
            val newCount = (bucket.count - subtractCount).coerceAtLeast(0)
            BucketInfo(bucket.id, bucket.name, newCount, bucket.path)
        }.toMutableList()

        val existingNames = processedBuckets.map { it.name }.toSet()
        for (albumName in createdAlbums) {
            if (!existingNames.contains(albumName)) {
                val bucketId = albumName.hashCode().toLong()
                val albumPath = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), albumName).absolutePath
                processedBuckets.add(BucketInfo(bucketId, albumName, 0, albumPath))
            }
        }

        processedBuckets.filter { it.count > 0 || createdAlbums.contains(it.name) }
            .sortedWith(compareByDescending<BucketInfo> { it.count }.thenBy { it.name })
    }

    override fun getBucketsFlow(): Flow<List<BucketInfo>> {
        return combine(
            mediaStoreDataSource.observeMediaStore().onStart { emit(Unit) },
            metadataDao.getAllMetadataFlow().onStart { emit(emptyList()) }
        ) { _, _ ->
            getBuckets()
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun toggleFavorite(mediaItem: MediaItem) {
        val currentMeta = metadataDao.getMetadataForMedia(mediaItem.id)
            ?: MediaMetadataEntity(mediaId = mediaItem.id)
        val updated = currentMeta.copy(isFavorite = !mediaItem.isFavorite)
        metadataDao.insertOrUpdate(updated)
    }

    override suspend fun toggleFavoriteBatch(mediaIds: Set<Long>) {
        if (mediaIds.isEmpty()) return
        val idList = mediaIds.toList()
        val metas = getMetadataForMediaIdsChunked(idList).associateBy { it.mediaId }
        val allFavorited = idList.all { metas[it]?.isFavorite == true }
        val newFavoriteState = !allFavorited

        idList.chunked(500).forEach { chunk ->
            chunk.forEach { id ->
                val meta = metas[id] ?: MediaMetadataEntity(mediaId = id)
                metadataDao.insertOrUpdate(meta.copy(isFavorite = newFavoriteState))
            }
        }
    }

    override suspend fun toggleHidden(context: Context, mediaItem: MediaItem) {
        val currentMeta = metadataDao.getMetadataForMedia(mediaItem.id)
        if (mediaItem.isHidden) {
            // vault restore
            if (currentMeta != null && currentMeta.vaultPath.isNotEmpty()) {
                val vaultFile = java.io.File(currentMeta.vaultPath)
                if (vaultFile.exists()) {
                    val resolver = context.contentResolver
                    val originalName = if (currentMeta.originalPath.isNotBlank() && java.io.File(currentMeta.originalPath).name.isNotBlank()) {
                        java.io.File(currentMeta.originalPath).name
                    } else {
                        "restored_${currentMeta.mediaId}.${if (currentMeta.mimeType.contains("png")) "png" else "jpg"}"
                    }
                    val mimeType = if (currentMeta.mimeType.isNotBlank()) currentMeta.mimeType else "image/jpeg"
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, originalName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Restored")
                        if (currentMeta.dateTaken > 0L) {
                            put(android.provider.MediaStore.MediaColumns.DATE_TAKEN, currentMeta.dateTaken)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }
                    val collectionUri = if (mimeType.contains("video", ignoreCase = true)) {
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val targetUri = resolver.insert(collectionUri, contentValues)
                    if (targetUri != null) {
                        var decryptSuccess = false
                        try {
                            resolver.openOutputStream(targetUri)?.use { output ->
                                vaultFile.inputStream().use { input ->
                                    com.hrshd1eux.imava.core.util.VaultCrypto.decrypt(input, output)
                                }
                            }
                            decryptSuccess = true
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                contentValues.clear()
                                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                                resolver.update(targetUri, contentValues, null, null)
                            }


                            try {
                                android.media.MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(targetUri.toString(), currentMeta.originalPath),
                                    arrayOf(mimeType)
                                ) { _, _ -> }
                            } catch (_: Exception) {}

                            vaultFile.delete()
                            val metaFile = java.io.File(currentMeta.vaultPath + ".meta")
                            if (metaFile.exists()) metaFile.delete()
                            com.hrshd1eux.imava.core.util.VaultCacheManager.removeCachedFile(context, currentMeta.mediaId)
                            metadataDao.delete(currentMeta)
                        } catch (e: Exception) {
                            if (!decryptSuccess) {
                                resolver.delete(targetUri, null, null)
                            }
                            throw e
                        }
                    }
                }
            }
        } else {
            // vault hide
            val vaultDir = java.io.File(context.filesDir, "vault").apply { mkdirs() }
            val vaultFile = java.io.File(vaultDir, "vault_${mediaItem.id}")
            val tempVaultFile = java.io.File(vaultDir, "vault_${mediaItem.id}.tmp")
            val resolver = context.contentResolver

            val inputStream = if (mediaItem.path.isNotEmpty() && java.io.File(mediaItem.path).exists()) {
                java.io.File(mediaItem.path).inputStream()
            } else {
                resolver.openInputStream(mediaItem.uri)
            }

            if (inputStream == null) {
                throw java.io.IOException("Cannot open input stream for media: ${mediaItem.uri}")
            }

            inputStream.use { input ->
                tempVaultFile.outputStream().use { output ->
                    com.hrshd1eux.imava.core.util.VaultCrypto.encrypt(input, output)
                }
            }

            if (tempVaultFile.length() <= 0) {
                tempVaultFile.delete()
                throw java.io.IOException("Vault encryption failed: file is empty")
            }

            if (vaultFile.exists()) vaultFile.delete()
            tempVaultFile.renameTo(vaultFile)
            
            val duration = if (mediaItem is MediaItem.Video) mediaItem.durationMs else 0L
            val entity = MediaMetadataEntity(
                mediaId = mediaItem.id,
                isFavorite = mediaItem.isFavorite,
                isHidden = true,
                isTrashed = false,
                originalPath = mediaItem.path,
                vaultPath = vaultFile.absolutePath,
                mimeType = mediaItem.mimeType,
                dateTaken = mediaItem.dateTaken,
                size = mediaItem.size,
                width = mediaItem.width,
                height = mediaItem.height,
                bucketId = mediaItem.bucketId,
                bucketName = mediaItem.bucketName,
                durationMs = duration
            )
            metadataDao.insertOrUpdate(entity)


            val metaFile = java.io.File(vaultFile.absolutePath + ".meta")
            try {
                val props = java.util.Properties().apply {
                    setProperty("mediaId", entity.mediaId.toString())
                    setProperty("isFavorite", entity.isFavorite.toString())
                    setProperty("isHidden", entity.isHidden.toString())
                    setProperty("isTrashed", entity.isTrashed.toString())
                    setProperty("originalPath", entity.originalPath)
                    setProperty("vaultPath", entity.vaultPath)
                    setProperty("mimeType", entity.mimeType)
                    setProperty("dateTaken", entity.dateTaken.toString())
                    setProperty("size", entity.size.toString())
                    setProperty("width", entity.width.toString())
                    setProperty("height", entity.height.toString())
                    setProperty("bucketId", entity.bucketId.toString())
                    setProperty("bucketName", entity.bucketName)
                    setProperty("durationMs", entity.durationMs.toString())
                }
                val byteArrayOutput = java.io.ByteArrayOutputStream()
                props.store(byteArrayOutput, "Vault Metadata Sidecar")
                metaFile.outputStream().use { out ->
                    com.hrshd1eux.imava.core.util.VaultCrypto.encrypt(
                        java.io.ByteArrayInputStream(byteArrayOutput.toByteArray()),
                        out
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            

            try {
                resolver.delete(mediaItem.uri, null, null)
            } catch (e: SecurityException) {
                throw e
            }
        }
    }

    override suspend fun toggleTrashed(mediaItem: MediaItem) {
        val currentMeta = metadataDao.getMetadataForMedia(mediaItem.id)
            ?: MediaMetadataEntity(
                mediaId = mediaItem.id,
                originalPath = mediaItem.path,
                mimeType = mediaItem.mimeType,
                dateTaken = mediaItem.dateTaken,
                size = mediaItem.size,
                width = mediaItem.width,
                height = mediaItem.height,
                bucketId = mediaItem.bucketId,
                bucketName = mediaItem.bucketName,
                durationMs = (mediaItem as? MediaItem.Video)?.durationMs ?: 0L
            )
        val updated = currentMeta.copy(
            isTrashed = !mediaItem.isTrashed,
            trashTime = if (!mediaItem.isTrashed) System.currentTimeMillis() else 0L,
            originalPath = if (currentMeta.originalPath.isBlank()) mediaItem.path else currentMeta.originalPath,
            mimeType = if (currentMeta.mimeType.isBlank()) mediaItem.mimeType else currentMeta.mimeType,
            dateTaken = if (currentMeta.dateTaken <= 0L) mediaItem.dateTaken else currentMeta.dateTaken,
            size = if (currentMeta.size <= 0L) mediaItem.size else currentMeta.size,
            bucketId = if (currentMeta.bucketId == 0L) mediaItem.bucketId else currentMeta.bucketId,
            bucketName = if (currentMeta.bucketName.isBlank()) mediaItem.bucketName else currentMeta.bucketName,
            durationMs = if (currentMeta.durationMs == 0L && mediaItem is MediaItem.Video) mediaItem.durationMs else currentMeta.durationMs
        )
        metadataDao.insertOrUpdate(updated)
    }

    override fun getFavoriteMediaFlow(): Flow<List<MediaItem>> {
        return metadataDao.getFavoriteIdsFlow()
            .combine(mediaStoreDataSource.observeMediaStore()) { favoriteIds, _ -> favoriteIds }
            .map { favoriteIds ->
                if (favoriteIds.isEmpty()) return@map emptyList<MediaItem>()
                val items = mediaStoreDataSource.fetchMediaByIds(favoriteIds.toSet())
                val metadataList = getMetadataForMediaIdsChunked(favoriteIds)
                val metadataMap = metadataList.associateBy { it.mediaId }
                items.map { item ->
                    val meta = metadataMap[item.id]
                    applyMetadata(item, meta)
                }.filter { it.isFavorite && !it.isHidden && !it.isTrashed }.distinctBy { it.id }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun getTrashedMediaFlow(): Flow<List<MediaItem>> {
        return metadataDao.getTrashedIdsFlow()
            .combine(mediaStoreDataSource.observeMediaStore()) { trashedIds, _ -> trashedIds }
            .map { trashedIds ->
                val storeTrashed = mediaStoreDataSource.fetchTrashedMedia()
                val dbItems = if (trashedIds.isNotEmpty()) {
                    mediaStoreDataSource.fetchMediaByIds(trashedIds.toSet())
                } else {
                    emptyList()
                }
                val allIds = (trashedIds + storeTrashed.map { it.id }).distinct()
                val metadataList = getMetadataForMediaIdsChunked(allIds)
                val metadataMap = metadataList.associateBy { it.mediaId }
                
                val foundIds = (storeTrashed.map { it.id } + dbItems.map { it.id }).toSet()
                val localTrashedItems = mutableListOf<MediaItem>()
                val trashDir = java.io.File(context.filesDir, "imava_trash")

                // Recover any trashed items stored in local imava_trash that are missing from MediaStore query
                trashedIds.filter { it !in foundIds }.forEach { missingId ->
                    val meta = metadataMap[missingId]
                    val origName = if (meta != null && meta.originalPath.isNotBlank()) java.io.File(meta.originalPath).name else null
                    val candidateFile = if (origName != null) {
                        java.io.File(trashDir, "${missingId}_$origName")
                    } else {
                        trashDir.listFiles { _, name -> name.startsWith("${missingId}_") }?.firstOrNull()
                    }
                    if (candidateFile != null && candidateFile.exists()) {
                        val isVideo = (meta?.mimeType?.startsWith("video/") == true) ||
                                candidateFile.name.endsWith(".mp4", ignoreCase = true) ||
                                candidateFile.name.endsWith(".mkv", ignoreCase = true) ||
                                candidateFile.name.endsWith(".mov", ignoreCase = true)
                        val uri = android.net.Uri.fromFile(candidateFile)
                        val item = if (isVideo) {
                            MediaItem.Video(
                                id = missingId,
                                uri = uri,
                                path = meta?.originalPath?.ifBlank { candidateFile.absolutePath } ?: candidateFile.absolutePath,
                                mimeType = meta?.mimeType?.ifBlank { "video/mp4" } ?: "video/mp4",
                                dateTaken = if ((meta?.dateTaken ?: 0L) > 0L) meta!!.dateTaken else candidateFile.lastModified(),
                                size = candidateFile.length(),
                                width = meta?.width ?: 0,
                                height = meta?.height ?: 0,
                                durationMs = meta?.durationMs ?: 0L,
                                bucketId = meta?.bucketId ?: 0L,
                                bucketName = meta?.bucketName?.ifBlank { "Trash" } ?: "Trash",
                                isTrashed = true,
                                trashTime = if ((meta?.trashTime ?: 0L) > 0L) meta!!.trashTime else System.currentTimeMillis()
                            )
                        } else {
                            MediaItem.Photo(
                                id = missingId,
                                uri = uri,
                                path = meta?.originalPath?.ifBlank { candidateFile.absolutePath } ?: candidateFile.absolutePath,
                                mimeType = meta?.mimeType?.ifBlank { "image/jpeg" } ?: "image/jpeg",
                                dateTaken = if ((meta?.dateTaken ?: 0L) > 0L) meta!!.dateTaken else candidateFile.lastModified(),
                                size = candidateFile.length(),
                                width = meta?.width ?: 0,
                                height = meta?.height ?: 0,
                                bucketId = meta?.bucketId ?: 0L,
                                bucketName = meta?.bucketName?.ifBlank { "Trash" } ?: "Trash",
                                isTrashed = true,
                                trashTime = if ((meta?.trashTime ?: 0L) > 0L) meta!!.trashTime else System.currentTimeMillis()
                            )
                        }
                        localTrashedItems.add(item)
                    }
                }

                // Also discover any physical files in imava_trash not yet in foundIds or localTrashedItems
                if (trashDir.exists()) {
                    trashDir.listFiles()?.forEach { file ->
                        val idPrefix = file.name.substringBefore("_").toLongOrNull()
                        if (idPrefix != null && idPrefix !in foundIds && localTrashedItems.none { it.id == idPrefix }) {
                            val isVideo = file.name.endsWith(".mp4", ignoreCase = true) ||
                                    file.name.endsWith(".mkv", ignoreCase = true) ||
                                    file.name.endsWith(".mov", ignoreCase = true)
                            val meta = metadataMap[idPrefix]
                            val item = if (isVideo) {
                                MediaItem.Video(
                                    id = idPrefix,
                                    uri = android.net.Uri.fromFile(file),
                                    path = meta?.originalPath?.ifBlank { file.absolutePath } ?: file.absolutePath,
                                    mimeType = meta?.mimeType?.ifBlank { "video/mp4" } ?: "video/mp4",
                                    dateTaken = file.lastModified(),
                                    size = file.length(),
                                    width = 0,
                                    height = 0,
                                    durationMs = 0L,
                                    bucketId = 0L,
                                    bucketName = "Trash",
                                    isTrashed = true,
                                    trashTime = file.lastModified()
                                )
                            } else {
                                MediaItem.Photo(
                                    id = idPrefix,
                                    uri = android.net.Uri.fromFile(file),
                                    path = meta?.originalPath?.ifBlank { file.absolutePath } ?: file.absolutePath,
                                    mimeType = meta?.mimeType?.ifBlank { "image/jpeg" } ?: "image/jpeg",
                                    dateTaken = file.lastModified(),
                                    size = file.length(),
                                    width = 0,
                                    height = 0,
                                    bucketId = 0L,
                                    bucketName = "Trash",
                                    isTrashed = true,
                                    trashTime = file.lastModified()
                                )
                            }
                            localTrashedItems.add(item)
                        }
                    }
                }

                val combined = (storeTrashed + dbItems + localTrashedItems).distinctBy { it.id }.map { item ->
                    applyMetadata(item, metadataMap[item.id])
                }.filter { it.isTrashed }
                combined.distinctBy { it.id }.sortedWith(
                    compareByDescending<MediaItem> { if (it.trashTime > 0L) it.trashTime else it.dateTaken }
                        .thenByDescending { it.id }
                )
            }
            .flowOn(Dispatchers.IO)
    }

    private suspend fun syncVaultMetadata() = withContext(Dispatchers.IO) {
        val vaultDir = java.io.File(context.filesDir, "vault")
        if (!vaultDir.exists()) return@withContext
        val metaFiles = vaultDir.listFiles { _, name -> name.endsWith(".meta") } ?: return@withContext
        
        if (metaFiles.isNotEmpty()) {
            val dbIds = metadataDao.getAllMetadata().map { it.mediaId }.toSet()
            metaFiles.forEach { metaFile ->
                try {
                    val props = java.util.Properties()
                    try {
                        val byteArrayOutput = java.io.ByteArrayOutputStream()
                        java.io.FileInputStream(metaFile).use { input ->
                            com.hrshd1eux.imava.core.util.VaultCrypto.decrypt(input, byteArrayOutput)
                        }
                        props.load(java.io.ByteArrayInputStream(byteArrayOutput.toByteArray()))
                    } catch (e: Exception) {
                        // legacy meta fallback
                        java.io.FileInputStream(metaFile).use { input ->
                            props.load(input)
                        }
                    }
                    val mediaId = props.getProperty("mediaId").toLong()
                    if (mediaId !in dbIds) {
                        val entity = MediaMetadataEntity(
                            mediaId = mediaId,
                            isFavorite = props.getProperty("isFavorite").toBoolean(),
                            isHidden = props.getProperty("isHidden").toBoolean(),
                            isTrashed = props.getProperty("isTrashed").toBoolean(),
                            originalPath = props.getProperty("originalPath") ?: "",
                            vaultPath = props.getProperty("vaultPath") ?: "",
                            mimeType = props.getProperty("mimeType") ?: "",
                            dateTaken = props.getProperty("dateTaken").toLong(),
                            size = props.getProperty("size").toLong(),
                            width = props.getProperty("width").toInt(),
                            height = props.getProperty("height").toInt(),
                            bucketId = props.getProperty("bucketId").toLong(),
                            bucketName = props.getProperty("bucketName") ?: "",
                            durationMs = props.getProperty("durationMs").toLong()
                        )
                        metadataDao.insertOrUpdate(entity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun getHiddenMediaFlow(isVaultUnlocked: Boolean): Flow<List<MediaItem>> {
        return metadataDao.getAllMetadataFlow()
            .onStart { if (isVaultUnlocked) syncVaultMetadata() }
            .map { metadataList ->
                if (!isVaultUnlocked) return@map emptyList<MediaItem>()
                metadataList.filter { it.isHidden && !it.isTrashed }.mapNotNull { entity ->
                    val decryptedFile = com.hrshd1eux.imava.core.util.VaultCacheManager.getDecryptedFile(context, entity)
                    val uri = if (decryptedFile != null) {
                        android.net.Uri.fromFile(decryptedFile)
                    } else {
                        val vaultFile = java.io.File(entity.vaultPath)
                        if (!vaultFile.exists()) return@mapNotNull null
                        android.net.Uri.fromFile(vaultFile)
                    }
                    val path = decryptedFile?.absolutePath ?: entity.originalPath.ifEmpty { entity.vaultPath }

                    if (entity.mimeType.contains("video", ignoreCase = true)) {
                        MediaItem.Video(
                            id = entity.mediaId,
                            uri = uri,
                            path = path,
                            mimeType = entity.mimeType,
                            dateTaken = entity.dateTaken,
                            size = entity.size,
                            width = entity.width,
                            height = entity.height,
                            durationMs = entity.durationMs,
                            isFavorite = entity.isFavorite,
                            isHidden = entity.isHidden,
                            isTrashed = entity.isTrashed,
                            trashTime = entity.trashTime,
                            bucketId = entity.bucketId,
                            bucketName = entity.bucketName
                        )
                    } else {
                        MediaItem.Photo(
                            id = entity.mediaId,
                            uri = uri,
                            path = path,
                            mimeType = entity.mimeType,
                            dateTaken = entity.dateTaken,
                            size = entity.size,
                            width = entity.width,
                            height = entity.height,
                            isFavorite = entity.isFavorite,
                            isHidden = entity.isHidden,
                            isTrashed = entity.isTrashed,
                            trashTime = entity.trashTime,
                            bucketId = entity.bucketId,
                            bucketName = entity.bucketName
                        )
                    }
                }.distinctBy { it.id }
            }
            .flowOn(Dispatchers.IO)
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    override suspend fun clearVaultCache(context: Context): Unit = withContext(Dispatchers.IO) {
        com.hrshd1eux.imava.core.util.VaultCacheManager.clearCache(context)
        try {
            val imageLoader = coil.Coil.imageLoader(context)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyMetadata(item: MediaItem, meta: MediaMetadataEntity?): MediaItem {
        if (meta == null) return item
        val tagList = if (meta.tags.isNotBlank()) meta.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
        val customDate = if (meta.dateTaken > 0) meta.dateTaken else item.dateTaken
        val finalTrashTime = if (meta.trashTime > 0L) meta.trashTime else item.trashTime
        return when (item) {
            is MediaItem.Photo -> item.copy(
                dateTaken = customDate,
                isFavorite = meta.isFavorite,
                isHidden = meta.isHidden,
                isTrashed = meta.isTrashed || item.isTrashed,
                trashTime = finalTrashTime,
                tags = tagList
            )
            is MediaItem.Video -> item.copy(
                dateTaken = customDate,
                isFavorite = meta.isFavorite,
                isHidden = meta.isHidden,
                isTrashed = meta.isTrashed || item.isTrashed,
                trashTime = finalTrashTime,
                tags = tagList
            )
        }
    }

    override suspend fun deleteMetadataPermanently(mediaId: Long) {
        val meta = metadataDao.getMetadataForMedia(mediaId)
        if (meta != null) {
            if (meta.vaultPath.isNotEmpty()) {
                val vaultFile = java.io.File(meta.vaultPath)
                if (vaultFile.exists()) vaultFile.delete()
                val metaFile = java.io.File(meta.vaultPath + ".meta")
                if (metaFile.exists()) metaFile.delete()
            }
            metadataDao.delete(meta)
        }
    }

    override suspend fun deleteOrphanedMetadata(activeIds: List<Long>) = withContext(Dispatchers.IO) {
        val activeIdSet = activeIds.toSet()
        val trackedIds = metadataDao.getTrackedNonHiddenIds()
        val orphanedIds = trackedIds.filter { it !in activeIdSet }
        
        if (orphanedIds.isNotEmpty()) {
            deleteMetadataByIdsChunked(orphanedIds)
        }
    }

    override suspend fun getActiveMediaIds(): List<Long> = withContext(Dispatchers.IO) {
        val storeIds = mediaStoreDataSource.fetchActiveMediaIds()
        val secondaryIds = mediaStoreDataSource.fetchSecondaryMediaIds()
        storeIds + secondaryIds
    }

    override suspend fun getMediaByIds(ids: Set<Long>): List<MediaItem> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val items = mediaStoreDataSource.fetchMediaByIds(ids)
        val metadataList = getMetadataForMediaIdsChunked(ids.toList())
        val metadataMap = metadataList.associateBy { it.mediaId }
        val mappedStoreItems = items.map { item ->
            val meta = metadataMap[item.id]
            applyMetadata(item, meta)
        }

        val foundIds = mappedStoreItems.map { it.id }.toSet()
        val missingIds = ids - foundIds
        if (missingIds.isNotEmpty()) {
            val vaultEntities = metadataList.filter { it.mediaId in missingIds && it.isHidden }
            val vaultItems = vaultEntities.mapNotNull { entity ->
                val decryptedFile = com.hrshd1eux.imava.core.util.VaultCacheManager.getDecryptedFile(context, entity)
                val uri = if (decryptedFile != null) {
                    android.net.Uri.fromFile(decryptedFile)
                } else {
                    val vaultFile = java.io.File(entity.vaultPath)
                    if (!vaultFile.exists()) return@mapNotNull null
                    android.net.Uri.fromFile(vaultFile)
                }
                val path = decryptedFile?.absolutePath ?: entity.originalPath.ifEmpty { entity.vaultPath }
                if (entity.mimeType.contains("video", ignoreCase = true)) {
                    MediaItem.Video(
                        id = entity.mediaId,
                        uri = uri,
                        path = path,
                        mimeType = entity.mimeType,
                        dateTaken = entity.dateTaken,
                        size = entity.size,
                        width = entity.width,
                        height = entity.height,
                        durationMs = entity.durationMs,
                        isFavorite = entity.isFavorite,
                        isHidden = entity.isHidden,
                        isTrashed = entity.isTrashed,
                        trashTime = entity.trashTime,
                        bucketId = entity.bucketId,
                        bucketName = entity.bucketName
                    )
                } else {
                    MediaItem.Photo(
                        id = entity.mediaId,
                        uri = uri,
                        path = path,
                        mimeType = entity.mimeType,
                        dateTaken = entity.dateTaken,
                        size = entity.size,
                        width = entity.width,
                        height = entity.height,
                        isFavorite = entity.isFavorite,
                        isHidden = entity.isHidden,
                        isTrashed = entity.isTrashed,
                        trashTime = entity.trashTime,
                        bucketId = entity.bucketId,
                        bucketName = entity.bucketName
                    )
                }
            }
            mappedStoreItems + vaultItems
        } else {
            mappedStoreItems
        }
    }

    override suspend fun searchMedia(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val cleanQuery = query.trim().removePrefix("#")
        val rawResults = mediaStoreDataSource.searchMedia(cleanQuery)
        val tagMatchedEntities = metadataDao.getMetadataByTag(cleanQuery)
        val tagMatchedIds = tagMatchedEntities.map { it.mediaId }.toSet()
        val customLocationMatchedIds = try {
            val prefs = context.getSharedPreferences("imava_custom_locations", Context.MODE_PRIVATE)
            prefs.all.mapNotNull { (key, value) ->
                if (key.startsWith("media_") && value is String && value.contains(cleanQuery, ignoreCase = true)) {
                    key.removePrefix("media_").toLongOrNull()
                } else null
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }

        val allIds = (rawResults.map { it.id } + tagMatchedIds + customLocationMatchedIds).toList()
        if (allIds.isEmpty()) return@withContext emptyList()

        val metadataList = getMetadataForMediaIdsChunked(allIds)
        val metadataMap = metadataList.associateBy { it.mediaId }

        val extraIds = (tagMatchedIds + customLocationMatchedIds) - rawResults.map { it.id }.toSet()
        val extraItems = if (extraIds.isNotEmpty()) {
            mediaStoreDataSource.fetchMediaByIds(extraIds)
        } else emptyList<MediaItem>()

        (rawResults + extraItems).distinctBy { it.id }.map { item ->
            val meta = metadataMap[item.id]
            applyMetadata(item, meta)
        }.filter { !it.isHidden && !it.isTrashed }
    }

    override suspend fun updateMediaTags(mediaId: Long, tags: List<String>) = withContext(Dispatchers.IO) {
        val currentMeta = metadataDao.getMetadataForMedia(mediaId) ?: MediaMetadataEntity(mediaId = mediaId)
        val tagStr = tags.joinToString(",")
        metadataDao.insertOrUpdate(currentMeta.copy(tags = tagStr))
    }

    override suspend fun scanSecondaryMediaDirectories(): com.hrshd1eux.imava.data.media.DeepScanResult = mediaStoreDataSource.scanSecondaryMediaDirectories()

    override suspend fun fetchThirdPartyAppMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val rawMedia = mediaStoreDataSource.fetchThirdPartyAppMedia()
        if (rawMedia.isEmpty()) return@withContext emptyList()
        val ids = rawMedia.map { it.id }
        val metadataMap = getMetadataForMediaIdsChunked(ids).associateBy { it.mediaId }
        rawMedia.map { applyMetadata(it, metadataMap[it.id]) }
            .filter { !it.isHidden && !it.isTrashed }
    }

    override suspend fun getDatePositionIndex(bucketId: Long?, sortOrder: com.hrshd1eux.imava.ui.SortOrder, mediaType: MediaTypeFilter): List<DatePositionHeader> = mediaStoreDataSource.getDatePositionIndex(bucketId, isAscending = (sortOrder == com.hrshd1eux.imava.ui.SortOrder.OLDEST_FIRST))

    override suspend fun renameMedia(
        context: Context,
        mediaItem: MediaItem,
        newDisplayName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val file = java.io.File(mediaItem.path)
        val ext = file.extension.ifEmpty {
            when (mediaItem.mimeType) {
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                "video/mp4" -> "mp4"
                "video/x-matroska" -> "mkv"
                else -> "jpg"
            }
        }
        val finalName = if (newDisplayName.contains(".")) newDisplayName else "$newDisplayName.$ext"

        if (mediaItem.isHidden) {
            val entity = metadataDao.getMetadataForMedia(mediaItem.id)
            if (entity != null) {
                val newOriginalPath = java.io.File(java.io.File(entity.originalPath).parentFile, finalName).absolutePath
                metadataDao.insertOrUpdate(entity.copy(originalPath = newOriginalPath))
            }
            return@withContext true
        }

        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, finalName)
            put(android.provider.MediaStore.MediaColumns.TITLE, finalName.substringBeforeLast("."))
        }

        val rows = try {
            resolver.update(mediaItem.uri, contentValues, null, null)
        } catch (e: Exception) {
            val recoverable = e as? android.app.RecoverableSecurityException
                ?: e.cause as? android.app.RecoverableSecurityException
            val isSec = e is SecurityException || e.cause is SecurityException
            if (recoverable != null || isSec) {
                throw e
            }
            0
        }

        if (rows > 0) {
            if (file.exists() && file.parentFile != null) {
                val targetFile = java.io.File(file.parentFile, finalName)
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath, targetFile.absolutePath),
                    null,
                    null
                )
            }
            val entity = metadataDao.getMetadataForMedia(mediaItem.id)
            if (entity != null && file.parentFile != null) {
                val newPath = java.io.File(file.parentFile, finalName).absolutePath
                metadataDao.insertOrUpdate(entity.copy(originalPath = newPath))
            }
            return@withContext true
        }

        if (file.exists() && file.parentFile != null) {
            val targetFile = java.io.File(file.parentFile, finalName)
            if (file.renameTo(targetFile)) {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath, targetFile.absolutePath),
                    null,
                    null
                )
                val entity = metadataDao.getMetadataForMedia(mediaItem.id)
                if (entity != null) {
                    metadataDao.insertOrUpdate(entity.copy(originalPath = targetFile.absolutePath))
                }
                return@withContext true
            }
        }

        false
    }

    override suspend fun batchRenameMedia(
        context: Context,
        itemsWithNewNames: List<Pair<MediaItem, String>>
    ): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        val resolver = context.contentResolver
        val pathsToScan = mutableListOf<String>()

        itemsWithNewNames.forEach { (item, newDisplayName) ->
            try {
                val file = java.io.File(item.path)
                val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
                val finalName = if (newDisplayName.endsWith(ext, ignoreCase = true)) newDisplayName else "$newDisplayName$ext"

                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                    put(android.provider.MediaStore.MediaColumns.TITLE, finalName.substringBeforeLast("."))
                }

                val rows = try {
                    resolver.update(item.uri, contentValues, null, null)
                } catch (_: Exception) { 0 }

                if (rows > 0) {
                    successCount++
                    if (file.exists() && file.parentFile != null) {
                        val targetFile = java.io.File(file.parentFile, finalName)
                        pathsToScan.add(file.absolutePath)
                        pathsToScan.add(targetFile.absolutePath)
                        val entity = metadataDao.getMetadataForMedia(item.id)
                        if (entity != null) {
                            metadataDao.insertOrUpdate(entity.copy(originalPath = targetFile.absolutePath))
                        }
                    }
                } else if (file.exists() && file.parentFile != null) {
                    val targetFile = java.io.File(file.parentFile, finalName)
                    if (file.renameTo(targetFile)) {
                        successCount++
                        pathsToScan.add(file.absolutePath)
                        pathsToScan.add(targetFile.absolutePath)
                        val entity = metadataDao.getMetadataForMedia(item.id)
                        if (entity != null) {
                            metadataDao.insertOrUpdate(entity.copy(originalPath = targetFile.absolutePath))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (pathsToScan.isNotEmpty()) {
            android.media.MediaScannerConnection.scanFile(
                context,
                pathsToScan.toTypedArray(),
                null,
                null
            )
        }

        successCount
    }

    override suspend fun updateMediaDateTaken(context: Context, mediaItem: MediaItem, newDateMs: Long): Boolean = withContext(Dispatchers.IO) {
        try {

            val existing = metadataDao.getMetadataForMedia(mediaItem.id)
            if (existing != null) {
                metadataDao.insertOrUpdate(existing.copy(dateTaken = newDateMs))
            } else {
                metadataDao.insertOrUpdate(
                    com.hrshd1eux.imava.data.database.MediaMetadataEntity(
                        mediaId = mediaItem.id,
                        isFavorite = mediaItem.isFavorite,
                        isTrashed = mediaItem.isTrashed,
                        isHidden = mediaItem.isHidden,
                        dateTaken = newDateMs,
                        originalPath = mediaItem.path,
                        mimeType = mediaItem.mimeType,
                        size = mediaItem.size,
                        width = mediaItem.width,
                        height = mediaItem.height,
                        durationMs = if (mediaItem is MediaItem.Video) mediaItem.durationMs else 0L,
                        bucketId = mediaItem.bucketId,
                        bucketName = mediaItem.bucketName
                    )
                )
            }


            if (!mediaItem.isHidden) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DATE_TAKEN, newDateMs)
                    put(android.provider.MediaStore.MediaColumns.DATE_ADDED, newDateMs / 1000L)
                }
                try {
                    resolver.update(mediaItem.uri, contentValues, null, null)
                } catch (_: Exception) {}


                val file = java.io.File(mediaItem.path)
                if (file.exists() && file.canWrite()) {
                    try {
                        val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
                        val sdf = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.getDefault())
                        val formattedDate = sdf.format(java.util.Date(newDateMs))
                        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME, formattedDate)
                        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, formattedDate)
                        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED, formattedDate)
                        exif.saveAttributes()
                    } catch (_: Exception) {}
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun purgeExpiredTrashMedia(): Int = withContext(Dispatchers.IO) {
        var purgedCount = 0
        try {
            val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
            val cutoffTime = System.currentTimeMillis() - thirtyDaysMs
            val allMetadata = metadataDao.getAllMetadata()
            val expiredEntities = allMetadata.filter { it.isTrashed && it.trashTime > 0 && it.trashTime < cutoffTime }

            for (entity in expiredEntities) {
                deleteMetadataPermanently(entity.mediaId)
                purgedCount++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        purgedCount
    }

    override suspend fun restoreAllVaultMedia(context: Context): Int = withContext(Dispatchers.IO) {
        var restoredCount = 0
        try {
            val allMetadata = metadataDao.getAllMetadata()
            val hiddenEntities = allMetadata.filter { it.isHidden }
            for (entity in hiddenEntities) {
                val dummyItem = if (entity.mimeType.contains("video", ignoreCase = true)) {
                    MediaItem.Video(
                        id = entity.mediaId,
                        uri = android.net.Uri.fromFile(java.io.File(entity.vaultPath)),
                        path = entity.originalPath.ifEmpty { entity.vaultPath },
                        mimeType = if (entity.mimeType.isNotBlank()) entity.mimeType else "video/mp4",
                        dateTaken = entity.dateTaken,
                        size = entity.size,
                        width = entity.width,
                        height = entity.height,
                        durationMs = entity.durationMs,
                        bucketId = entity.bucketId,
                        bucketName = entity.bucketName,
                        isHidden = true
                    )
                } else {
                    MediaItem.Photo(
                        id = entity.mediaId,
                        uri = android.net.Uri.fromFile(java.io.File(entity.vaultPath)),
                        path = entity.originalPath.ifEmpty { entity.vaultPath },
                        mimeType = if (entity.mimeType.isNotBlank()) entity.mimeType else "image/jpeg",
                        dateTaken = entity.dateTaken,
                        size = entity.size,
                        width = entity.width,
                        height = entity.height,
                        bucketId = entity.bucketId,
                        bucketName = entity.bucketName,
                        isHidden = true
                    )
                }
                toggleHidden(context, dummyItem)
                restoredCount++
            }
            clearVaultCache(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        restoredCount
    }

    override suspend fun deleteAllVaultData(context: Context): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        try {
            val vaultDir = java.io.File(context.filesDir, "vault")
            if (vaultDir.exists()) {
                vaultDir.listFiles()?.forEach { file ->
                    file.delete()
                    deletedCount++
                }
            }
            val allMetadata = metadataDao.getAllMetadata()
            val hiddenEntities = allMetadata.filter { it.isHidden }
            for (entity in hiddenEntities) {
                metadataDao.delete(entity)
            }
            clearVaultCache(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        deletedCount
    }

    private suspend fun scanFileSuspend(context: Context, path: String): Uri? =
        suspendCancellableCoroutine { continuation ->
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(path),
                null
            ) { _, uri ->
                if (continuation.isActive) {
                    continuation.resume(uri)
                }
            }
        }

    override suspend fun moveOrCopyMedia(
        context: Context,
        items: List<MediaItem>,
        targetDirectory: java.io.File,
        isCopy: Boolean
    ): Result<MoveCopyResult> = withContext(Dispatchers.IO) {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }
        var count = 0
        val scannedPaths = mutableListOf<String>()
        val failedDeleteItems = mutableListOf<MediaItem>()
        val createdTargetsForFailed = mutableListOf<java.io.File>()

        for (item in items) {
            try {
                val sourceFile = java.io.File(item.path)
                if (!sourceFile.exists()) continue

                // Avoid moving to the exact same folder
                val sourceParent = sourceFile.parentFile?.canonicalPath
                val targetCanonical = targetDirectory.canonicalPath
                if (sourceParent != null && sourceParent == targetCanonical) {
                    continue
                }

                val targetFile = java.io.File(targetDirectory, sourceFile.name)
                var finalTarget = targetFile
                var counter = 1
                val baseName = sourceFile.nameWithoutExtension
                val ext = sourceFile.extension
                while (finalTarget.exists()) {
                    val newName = if (ext.isNotEmpty()) "${baseName}_$counter.$ext" else "${baseName}_$counter"
                    finalTarget = java.io.File(targetDirectory, newName)
                    counter++
                }

                val originalDate = if (item.dateTaken > 0) item.dateTaken else if (sourceFile.lastModified() > 0) sourceFile.lastModified() else System.currentTimeMillis()

                var isMoveRenamed = false
                if (!isCopy) {
                    // Try atomic OS filesystem move first (0ms latency, preserves inode & dates)
                    try {
                        isMoveRenamed = sourceFile.renameTo(finalTarget)
                    } catch (_: Exception) {
                        isMoveRenamed = false
                    }
                }

                if (!isMoveRenamed) {
                    sourceFile.inputStream().use { input ->
                        finalTarget.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                if (finalTarget.exists() && (isMoveRenamed || finalTarget.length() == sourceFile.length())) {
                    // 1. Explicitly ensure EXIF dates match originalDate
                    try {
                        val targetExif = androidx.exifinterface.media.ExifInterface(finalTarget.absolutePath)
                        val sdf = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getDefault()
                        }
                        val formattedDate = sdf.format(java.util.Date(originalDate))

                        var dt = targetExif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL)
                        if (dt.isNullOrBlank()) {
                            dt = targetExif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME)
                        }
                        if (dt.isNullOrBlank()) {
                            dt = formattedDate
                        }
                        targetExif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, dt)
                        targetExif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME, dt)
                        targetExif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED, dt)
                        targetExif.saveAttributes()
                    } catch (_: Exception) {}

                    // 2. Preserve filesystem last modified (after EXIF saveAttributes)
                    try {
                        finalTarget.setLastModified(originalDate)
                    } catch (_: Exception) {}

                    count++

                    // 3. Scan new target into MediaStore SYNCHRONOUSLY
                    val oldMeta = if (!isCopy) metadataDao.getMetadataForMedia(item.id) else null
                    val customLoc = com.hrshd1eux.imava.core.util.ExifLocationUtil.getCustomLocation(context, item.id, item.path)

                    val scannedUri = scanFileSuspend(context, finalTarget.absolutePath)
                    if (scannedUri != null) {
                        // Stamp MediaStore DATE_TAKEN isolated from read-only columns
                        try {
                            val contentValues = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DATE_TAKEN, originalDate)
                            }
                            context.contentResolver.update(scannedUri, contentValues, null, null)
                        } catch (_: Exception) {}

                        val newMediaId = try { android.content.ContentUris.parseId(scannedUri) } catch (_: Exception) { 0L }
                        if (newMediaId > 0L) {
                            if (customLoc != null) {
                                try {
                                    com.hrshd1eux.imava.core.util.ExifLocationUtil.setCustomLocation(
                                        context, newMediaId, scannedUri, finalTarget.absolutePath, customLoc
                                    )
                                } catch (_: Exception) {}
                            }

                            val targetBucketId = finalTarget.parentFile?.absolutePath?.lowercase()?.hashCode()?.toLong() ?: 0L
                            val targetBucketName = finalTarget.parentFile?.name ?: ""

                            // Always persist metadata with originalDate in Room to guarantee date never resets
                            val newEntity = oldMeta?.copy(
                                mediaId = newMediaId,
                                originalPath = finalTarget.absolutePath,
                                dateTaken = originalDate,
                                bucketId = targetBucketId,
                                bucketName = targetBucketName
                            ) ?: MediaMetadataEntity(
                                mediaId = newMediaId,
                                isFavorite = item.isFavorite,
                                isTrashed = false,
                                isHidden = false,
                                dateTaken = originalDate,
                                originalPath = finalTarget.absolutePath,
                                mimeType = item.mimeType,
                                size = finalTarget.length(),
                                width = item.width,
                                height = item.height,
                                durationMs = if (item is MediaItem.Video) item.durationMs else 0L,
                                bucketId = targetBucketId,
                                bucketName = targetBucketName
                            )

                            try {
                                metadataDao.insertOrUpdate(newEntity)
                                if (!isCopy && oldMeta != null) {
                                    metadataDao.delete(oldMeta)
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    // 4. Source deletion and MediaStore purge for Move operation
                    if (!isCopy) {
                        val oldPath = sourceFile.absolutePath

                        // Delete old MediaStore row directly via ContentResolver so it disappears from old album
                        var resolverDeleted = false
                        try {
                            val rows = context.contentResolver.delete(item.uri, null, null)
                            if (rows > 0) resolverDeleted = true
                        } catch (_: Exception) {}

                        val directDeleted = if (isMoveRenamed) {
                            true
                        } else if (sourceFile.exists()) {
                            sourceFile.delete()
                        } else {
                            true
                        }

                        if (directDeleted || resolverDeleted) {
                            scannedPaths.add(oldPath)
                            metadataDao.insertOrUpdate(
                                MediaMetadataEntity(
                                    mediaId = item.id,
                                    isHidden = true,
                                    bucketId = item.bucketId,
                                    bucketName = item.bucketName,
                                    originalPath = item.path
                                )
                            )
                        } else {
                            // File deletion denied by Scoped Storage without user consent
                            failedDeleteItems.add(item)
                            createdTargetsForFailed.add(finalTarget)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (scannedPaths.isNotEmpty()) {
            try {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    scannedPaths.toTypedArray(),
                    null,
                    null
                )
            } catch (_: Exception) {}
        }

        Result.success(MoveCopyResult(count, failedDeleteItems, createdTargetsForFailed))
    }

    override suspend fun shiftMediaTimestamps(
        context: Context,
        items: List<MediaItem>,
        offsetMillis: Long,
        exactTimestamp: Long?
    ): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        for (item in items) {
            val newDate = exactTimestamp ?: (item.dateTaken + offsetMillis).coerceAtLeast(0L)
            val success = updateMediaDateTaken(context, item, newDate)
            if (success) count++
        }
        Result.success(count)
    }
}
