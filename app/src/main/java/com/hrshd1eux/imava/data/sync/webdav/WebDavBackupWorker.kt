package com.hrshd1eux.imava.data.sync.webdav

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hrshd1eux.imava.data.media.MediaStoreDataSource
import com.hrshd1eux.imava.data.model.isVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WebDavBackupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val config = WebDavConfigManager.loadConfig(appContext)
        if (!config.isConfigured || !config.autoBackupEnabled) {
            return@withContext Result.success()
        }

        try {
            val mediaStoreDataSource = MediaStoreDataSource(appContext)
            val allMedia = mediaStoreDataSource.fetchMedia(limit = 500, offset = 0, bucketId = null)
            val alreadySyncedIds = WebDavConfigManager.getSyncedIds(appContext)

            val eligibleItems = allMedia.filter { item ->
                if (item.isHidden || item.isTrashed) return@filter false
                if (!config.includeVideos && item.isVideo) return@filter false
                !alreadySyncedIds.contains(item.id.toString())
            }

            if (eligibleItems.isEmpty()) {
                return@withContext Result.success()
            }

            WebDavClient.ensureRemoteDirectory(config, config.remoteDirectory)
            var successCount = 0

            for (item in eligibleItems) {
                if (isStopped) break

                val fileName = File(item.path).name.ifBlank { "media_${item.id}.${if (item.isVideo) "mp4" else "jpg"}" }
                val folderName = item.bucketName.ifBlank { "Camera" }
                val remotePath = "${config.remoteDirectory.trim('/')}/$folderName/$fileName"

                val result = WebDavClient.uploadMedia(
                    config = config,
                    context = appContext,
                    uri = item.uri,
                    remoteRelativePath = remotePath,
                    mimeType = item.mimeType,
                    totalBytes = item.size
                )

                if (result.isSuccess) {
                    successCount++
                    WebDavConfigManager.markIdAsSynced(appContext, item.id)
                }
            }

            val now = System.currentTimeMillis()
            WebDavConfigManager.updateLastBackup(appContext, now, successCount, "Auto-backup: $successCount items synced")
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
