package com.hrshd1eux.imava.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.graphics.BitmapFactory
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val apkDownloadUrl: String?
)

sealed class AppUpdateDownloadState {
    object Idle : AppUpdateDownloadState()
    data class Downloading(
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val version: String
    ) : AppUpdateDownloadState()
    data class Paused(
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val version: String
    ) : AppUpdateDownloadState()
    data class Completed(
        val apkFile: File,
        val version: String
    ) : AppUpdateDownloadState()
    data class Failed(
        val error: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val version: String
    ) : AppUpdateDownloadState()
}

object AppUpdateManager {

    private const val CHANNEL_ID = "imava_app_updates"
    private const val CHANNEL_ID_COMPLETE = "imava_app_updates_complete"
    private const val NOTIFICATION_ID = 1010
    private const val NOTIFICATION_COMPLETE_ID = 1011

    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentDownloadJob: Job? = null

    private val _downloadState = MutableStateFlow<AppUpdateDownloadState>(AppUpdateDownloadState.Idle)
    val downloadState: StateFlow<AppUpdateDownloadState> = _downloadState.asStateFlow()

    @Volatile private var isPauseRequested = false
    private var activeUpdateInfo: UpdateInfo? = null

    suspend fun checkForUpdates(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        val currentVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.1.3"
        } catch (_: Exception) {
            "1.1.3"
        }

        val githubApiUrls = listOf(
            "https://api.github.com/repos/HrshD1eux/imava/releases/latest",
            "https://api.github.com/repos/HrshD1eux/Imava/releases/latest",
            "https://api.github.com/repos/HrshD1eux/Gallery_Android/releases/latest"
        )

        var jsonResponse: String? = null
        for (apiUrl in githubApiUrls) {
            try {
                val url = URL(apiUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "Imava-App")
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                if (connection.responseCode == 200) {
                    jsonResponse = connection.inputStream.bufferedReader().use { it.readText() }
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (jsonResponse == null) {
            return@withContext UpdateInfo(
                hasUpdate = false,
                latestVersion = currentVersion,
                currentVersion = currentVersion,
                releaseNotes = "Could not check GitHub releases. Please check your internet connection.",
                apkDownloadUrl = null
            )
        }

        try {
            val json = JSONObject(jsonResponse)
            val rawTag = json.optString("tag_name", "v1.0.0")
            val latestVersion = rawTag.removePrefix("v").trim()
            val body = json.optString("body", "No release notes available.")

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    val browserUrl = asset.optString("browser_download_url", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = browserUrl
                        break
                    }
                }
            }

            val isNewer = isVersionNewer(latestVersion, currentVersion)
            UpdateInfo(
                hasUpdate = isNewer && !apkUrl.isNullOrEmpty(),
                latestVersion = latestVersion,
                currentVersion = currentVersion,
                releaseNotes = body,
                apkDownloadUrl = apkUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateInfo(
                hasUpdate = false,
                latestVersion = currentVersion,
                currentVersion = currentVersion,
                releaseNotes = "Error parsing release info.",
                apkDownloadUrl = null
            )
        }
    }

    /**
     * Starts or resumes an update download in the background with HTTP Range support.
     * Can continue while the app is in the background with ongoing notification.
     */
    fun startOrResumeDownload(context: Context, updateInfo: UpdateInfo) {
        val downloadUrl = updateInfo.apkDownloadUrl ?: return
        activeUpdateInfo = updateInfo
        isPauseRequested = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            val activity = context.findActivity()
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1010
                )
            }
        }

        if (currentDownloadJob?.isActive == true) return

        currentDownloadJob = downloadScope.launch {
            val version = updateInfo.latestVersion
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val completedFile = File(updatesDir, "update_${version}.apk")
            val partFile = File(updatesDir, "update_${version}.apk.part")

            // Automatically clean up any stale APKs or partial downloads from older versions
            cleanUpOldUpdates(context, version)

            // Check if already completed and valid
            if (completedFile.exists() && completedFile.length() > 0) {
                val pkg = context.packageManager.getPackageArchiveInfo(completedFile.absolutePath, 0)
                if (pkg != null) {
                    _downloadState.value = AppUpdateDownloadState.Completed(completedFile, version)
                    showCompletedNotification(context, completedFile, version)
                    return@launch
                } else {
                    completedFile.delete()
                }
            }

            createNotificationChannel(context)

            var existingBytes = if (partFile.exists()) partFile.length() else 0L

            // Resolve GitHub redirect chain to final direct URL (e.g. AWS S3)
            var targetUrl = downloadUrl
            try {
                for (hop in 0 until 5) {
                    val probeConn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        connectTimeout = 15000
                        readTimeout = 15000
                        setRequestProperty("User-Agent", "Imava-App")
                    }
                    val code = probeConn.responseCode
                    if (code in 300..399) {
                        val location = probeConn.getHeaderField("Location")
                        probeConn.disconnect()
                        if (!location.isNullOrBlank()) {
                            targetUrl = location
                            continue
                        }
                    }
                    probeConn.disconnect()
                    break
                }
            } catch (_: Exception) {}

            var attempts = 0
            while (attempts < 3 && isActive) {
                attempts++
                try {
                    val connection = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20000
                        readTimeout = 45000
                        setRequestProperty("User-Agent", "Imava-App")
                        if (existingBytes > 0L) {
                            setRequestProperty("Range", "bytes=$existingBytes-")
                        }
                    }

                    val responseCode = connection.responseCode
                    val isPartial = (responseCode == 206)

                    if (responseCode == 416) {
                        // Range not satisfiable: partial file may be stale or corrupt. Reset.
                        partFile.delete()
                        existingBytes = 0L
                        startOrResumeDownload(context, updateInfo)
                        return@launch
                    }

                    if (responseCode in 500..504 && attempts < 3) {
                        connection.disconnect()
                        kotlinx.coroutines.delay(2000L * attempts)
                        continue
                    }

                    if (responseCode != 200 && responseCode != 206) {
                        val errorDetail = when (responseCode) {
                            504 -> "Gateway Timeout (504)"
                            502, 503 -> "Server Unavailable ($responseCode)"
                            else -> "HTTP Server returned response code $responseCode"
                        }
                        throw IOException(errorDetail)
                    }

                    val streamLength = connection.contentLengthLong.takeIf { it > 0 } ?: connection.contentLength.toLong()
                    val totalBytes = if (isPartial && streamLength > 0) {
                        existingBytes + streamLength
                    } else if (streamLength > 0) {
                        streamLength
                    } else {
                        0L
                    }

                    val append = isPartial && existingBytes > 0L
                    if (!append && partFile.exists()) {
                        partFile.delete()
                        existingBytes = 0L
                    }

                    var downloadedBytes = if (append) existingBytes else 0L
                    var lastProgressReported = -1
                    var lastNotificationTime = 0L

                    val initialPercent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                    _downloadState.value = AppUpdateDownloadState.Downloading(
                        progress = initialPercent,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        version = version
                    )
                    showProgressNotification(context, initialPercent, downloadedBytes, totalBytes, version)

                    connection.inputStream.use { input ->
                        FileOutputStream(partFile, append).use { output ->
                            val buffer = ByteArray(32768)
                            var read: Int

                            while (input.read(buffer).also { read = it } != -1) {
                                if (isPauseRequested) {
                                    output.flush()
                                    val currentPct = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                                    _downloadState.value = AppUpdateDownloadState.Paused(
                                        progress = currentPct,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes,
                                        version = version
                                    )
                                    showPausedNotification(context, currentPct, downloadedBytes, totalBytes, version)
                                    return@launch
                                }

                                if (!isActive) {
                                    output.flush()
                                    return@launch
                                }

                                output.write(buffer, 0, read)
                                downloadedBytes += read

                                val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                                val now = System.currentTimeMillis()

                                if (percent != lastProgressReported || (now - lastNotificationTime) >= 700L) {
                                    lastProgressReported = percent
                                    lastNotificationTime = now
                                    _downloadState.value = AppUpdateDownloadState.Downloading(
                                        progress = percent,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes,
                                        version = version
                                    )
                                    showProgressNotification(context, percent, downloadedBytes, totalBytes, version)
                                }
                            }
                            output.flush()
                        }
                    }

                    if (partFile.exists() && partFile.length() > 0) {
                        if (completedFile.exists()) completedFile.delete()
                        partFile.renameTo(completedFile)

                        _downloadState.value = AppUpdateDownloadState.Completed(completedFile, version)
                        showCompletedNotification(context, completedFile, version)
                        return@launch
                    } else {
                        throw IOException("Downloaded package is empty")
                    }
                } catch (e: Exception) {
                    if (isPauseRequested) {
                        val total = partFile.length()
                        _downloadState.value = AppUpdateDownloadState.Paused(
                            progress = 0,
                            downloadedBytes = total,
                            totalBytes = total,
                            version = version
                        )
                        return@launch
                    } else if (attempts < 3 && (e is IOException || e is SocketTimeoutException)) {
                        existingBytes = if (partFile.exists()) partFile.length() else 0L
                        kotlinx.coroutines.delay(2000L * attempts)
                        continue
                    } else {
                        if (partFile.exists() && partFile.length() == 0L) {
                            partFile.delete()
                        }
                        e.printStackTrace()
                        _downloadState.value = AppUpdateDownloadState.Failed(
                            error = e.localizedMessage ?: "Download failed",
                            downloadedBytes = if (partFile.exists()) partFile.length() else 0L,
                            totalBytes = 0L,
                            version = version
                        )
                        cancelNotification(context)
                        return@launch
                    }
                }
            }
        }
    }

    fun pauseDownload(context: Context) {
        isPauseRequested = true
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        val state = _downloadState.value
        if (state is AppUpdateDownloadState.Downloading) {
            _downloadState.value = AppUpdateDownloadState.Paused(
                progress = state.progress,
                downloadedBytes = state.downloadedBytes,
                totalBytes = state.totalBytes,
                version = state.version
            )
            showPausedNotification(context, state.progress, state.downloadedBytes, state.totalBytes, state.version)
        }
    }

    fun resumeDownload(context: Context) {
        val currentInfo = activeUpdateInfo
        if (currentInfo != null) {
            startOrResumeDownload(context, currentInfo)
        }
    }

    fun cancelDownload(context: Context) {
        isPauseRequested = false
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        cancelNotification(context)

        activeUpdateInfo?.latestVersion?.let { version ->
            val updatesDir = File(context.cacheDir, "updates")
            File(updatesDir, "update_${version}.apk.part").delete()
            File(updatesDir, "update_${version}.apk").delete()
        }

        _downloadState.value = AppUpdateDownloadState.Idle
    }

    fun resetDownloadState() {
        _downloadState.value = AppUpdateDownloadState.Idle
    }

    /**
     * Cleans up orphaned update APKs and stale .part files from cacheDir/updates.
     * Keeps only the currently targeted version if specified.
     */
    fun cleanUpOldUpdates(context: Context, currentTargetVersion: String? = null) {
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (updatesDir.exists() && updatesDir.isDirectory) {
                updatesDir.listFiles()?.forEach { file ->
                    val isCurrent = currentTargetVersion != null && (
                        file.name == "update_${currentTargetVersion}.apk" ||
                        file.name == "update_${currentTargetVersion}.apk.part"
                    )
                    if (!isCurrent) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val progressChannel = NotificationChannel(
                CHANNEL_ID,
                "Imava Gallery Update Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress when downloading Imava Gallery updates"
                setShowBadge(false)
            }
            manager.createNotificationChannel(progressChannel)

            val completeChannel = NotificationChannel(
                CHANNEL_ID_COMPLETE,
                "Imava Gallery Update Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when an Imava Gallery update is ready to install"
                setShowBadge(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(completeChannel)
        }
    }

    private fun showProgressNotification(
        context: Context,
        progress: Int,
        downloaded: Long,
        total: Long,
        version: String
    ) {
        if (!hasNotificationPermission(context)) return

        val downloadedStr = FormatUtils.formatFileSize(downloaded)
        val totalStr = if (total > 0) FormatUtils.formatFileSize(total) else "..."
        val contentText = "$progress% • $downloadedStr / $totalStr"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            1010,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val largeIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_imava_logo)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .apply {
                if (largeIconBitmap != null) setLargeIcon(largeIconBitmap)
            }
            .setContentTitle("Downloading Imava Gallery v$version")
            .setContentText(contentText)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, total <= 0)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, builder.build())
    }

    private fun showPausedNotification(
        context: Context,
        progress: Int,
        downloaded: Long,
        total: Long,
        version: String
    ) {
        if (!hasNotificationPermission(context)) return

        val downloadedStr = FormatUtils.formatFileSize(downloaded)
        val totalStr = if (total > 0) FormatUtils.formatFileSize(total) else "..."

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            1010,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val largeIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_imava_logo)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .apply {
                if (largeIconBitmap != null) setLargeIcon(largeIconBitmap)
            }
            .setContentTitle("Update Download Paused (v$version)")
            .setContentText("$progress% • $downloadedStr / $totalStr (Paused)")
            .setContentIntent(pendingOpen)
            .setOngoing(false)
            .setAutoCancel(true)
            .setProgress(100, progress, false)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, builder.build())
    }

    private fun showCompletedNotification(
        context: Context,
        apkFile: File,
        version: String
    ) {
        if (!hasNotificationPermission(context)) return

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingInstall = PendingIntent.getActivity(
            context,
            1011,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val largeIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_imava_logo)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_COMPLETE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .apply {
                if (largeIconBitmap != null) setLargeIcon(largeIconBitmap)
            }
            .setContentTitle("Imava Gallery v$version Ready to Install")
            .setContentText("Download complete. Tap to install the update now.")
            .setContentIntent(pendingInstall)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(false)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        // Clear progress notification and alert user with completion notification
        manager?.cancel(NOTIFICATION_ID)
        manager?.notify(NOTIFICATION_COMPLETE_ID, builder.build())
    }

    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
        manager?.cancel(NOTIFICATION_COMPLETE_ID)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val info = UpdateInfo(
            hasUpdate = true,
            latestVersion = "latest",
            currentVersion = "",
            releaseNotes = "",
            apkDownloadUrl = downloadUrl
        )
        startOrResumeDownload(context, info)
        true
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                android.widget.Toast.makeText(context, "Please enable 'Allow from this source' to install the update", android.widget.Toast.LENGTH_LONG).show()
                return
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Could not launch installer: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun isVersionOlderThan(version: String, target: String = "1.1.8"): Boolean {
        val vParts = version.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val tParts = target.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val length = maxOf(vParts.size, tParts.size)

        for (i in 0 until length) {
            val v = vParts.getOrElse(i) { 0 }
            val t = tParts.getOrElse(i) { 0 }
            if (v < t) return true
            if (v > t) return false
        }
        return false
    }

    fun formatCleanReleaseNotes(raw: String): String {
        return raw.lines().joinToString("\n") { line ->
            var cleaned = line.trim()
            if (cleaned.startsWith("### ")) cleaned = cleaned.removePrefix("### ").trim()
            else if (cleaned.startsWith("## ")) cleaned = cleaned.removePrefix("## ").trim()
            else if (cleaned.startsWith("# ")) cleaned = cleaned.removePrefix("# ").trim()

            if (cleaned.startsWith("* ") || cleaned.startsWith("- ")) {
                cleaned = "• " + cleaned.substring(2).trim()
            }
            cleaned.replace("**", "").replace("`", "").replace("$", "")
        }.trim()
    }

    fun shouldPromptUpdate(context: Context, latestVersion: String): Boolean {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val lastDismissedVersion = prefs.getString("last_dismissed_update_version", null)
        return lastDismissedVersion != latestVersion
    }

    fun markUpdateDismissed(context: Context, latestVersion: String) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_dismissed_update_version", latestVersion).apply()
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val length = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
