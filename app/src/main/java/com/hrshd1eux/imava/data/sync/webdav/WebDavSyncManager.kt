package com.hrshd1eux.imava.data.sync.webdav

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.model.isVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

object WebDavSyncManager {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _syncProgressFraction = MutableStateFlow(0f)
    val syncProgressFraction: StateFlow<Float> = _syncProgressFraction.asStateFlow()

    private const val WORK_NAME = "imava_webdav_periodic_backup"

    fun scheduleAutoBackup(context: Context, config: WebDavConfig) {
        val workManager = WorkManager.getInstance(context)
        if (!config.autoBackupEnabled || !config.isConfigured) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val constraintsBuilder = Constraints.Builder()
        if (config.syncWifiOnly) {
            constraintsBuilder.setRequiredNetworkType(NetworkType.UNMETERED)
        } else {
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)
        }
        if (config.syncChargingOnly) {
            constraintsBuilder.setRequiresCharging(true)
        }

        val periodicWork = PeriodicWorkRequestBuilder<WebDavBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraintsBuilder.build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork
        )
    }

    fun cancelAutoBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun startSync(
        context: Context,
        scope: CoroutineScope,
        allItems: List<MediaItem>,
        onFinished: (success: Int, failed: Int) -> Unit = { _, _ -> }
    ) {
        if (_isSyncing.value) return

        val config = WebDavConfigManager.loadConfig(context)
        if (!config.isConfigured) {
            _syncStatus.value = "WebDAV server is not configured"
            return
        }

        _isSyncing.value = true
        _syncProgressFraction.value = 0f
        _syncStatus.value = "Preparing backup list..."

        scope.launch(Dispatchers.IO) {
            var successCount = 0
            var failCount = 0

            try {
                val alreadySyncedIds = WebDavConfigManager.getSyncedIds(context)
                val eligibleItems = allItems.filter { item ->
                    if (item.isHidden || item.isTrashed) return@filter false
                    if (!config.includeVideos && item.isVideo) return@filter false
                    !alreadySyncedIds.contains(item.id.toString())
                }

                val total = eligibleItems.size
                if (total == 0) {
                    _syncStatus.value = "All media already backed up to NAS"
                    _isSyncing.value = false
                    _syncProgressFraction.value = 1f
                    onFinished(0, 0)
                    return@launch
                }

                _syncStatus.value = "Found $total new items to backup..."

                // Pre-ensure root remote directory exists
                WebDavClient.ensureRemoteDirectory(config, config.remoteDirectory)

                eligibleItems.forEachIndexed { index, item ->
                    val fileName = File(item.path).name.ifBlank { "media_${item.id}.${if (item.isVideo) "mp4" else "jpg"}" }
                    val folderName = item.bucketName.ifBlank { "Camera" }
                    val remotePath = "${config.remoteDirectory.trim('/')}/$folderName/$fileName"

                    _syncStatus.value = "Backing up ${index + 1}/$total: $fileName"
                    _syncProgressFraction.value = (index.toFloat() / total)

                    val result = WebDavClient.uploadMedia(
                        config = config,
                        context = context,
                        uri = item.uri,
                        remoteRelativePath = remotePath,
                        mimeType = item.mimeType,
                        totalBytes = item.size
                    )

                    if (result.isSuccess) {
                        successCount++
                        WebDavConfigManager.markIdAsSynced(context, item.id)
                    } else {
                        failCount++
                    }
                }

                val now = System.currentTimeMillis()
                val statusText = if (failCount == 0) "Completed: $successCount items backed up" else "Completed: $successCount backed up, $failCount failed"
                WebDavConfigManager.updateLastBackup(context, now, successCount, statusText)
                _syncStatus.value = statusText
                _syncProgressFraction.value = 1f
            } catch (e: Exception) {
                _syncStatus.value = "Backup failed: ${e.message}"
            } finally {
                _isSyncing.value = false
                onFinished(successCount, failCount)
            }
        }
    }
}
