package com.hrshd1eux.imava.data.sync.webdav

import android.content.Context

data class WebDavConfig(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val remoteDirectory: String = "Imava_Backup",
    val syncWifiOnly: Boolean = true,
    val syncChargingOnly: Boolean = false,
    val includeVideos: Boolean = true,
    val autoBackupEnabled: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val lastBackupCount: Int = 0,
    val lastBackupStatus: String = "Never synced"
) {
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && (serverUrl.startsWith("http://") || serverUrl.startsWith("https://"))
}

object WebDavConfigManager {
    private const val PREFS_NAME = "webdav_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_REMOTE_DIR = "remote_dir"
    private const val KEY_WIFI_ONLY = "wifi_only"
    private const val KEY_CHARGING_ONLY = "charging_only"
    private const val KEY_INCLUDE_VIDEOS = "include_videos"
    private const val KEY_AUTO_BACKUP = "auto_backup"
    private const val KEY_LAST_TIMESTAMP = "last_timestamp"
    private const val KEY_LAST_COUNT = "last_count"
    private const val KEY_LAST_STATUS = "last_status"
    private const val KEY_SYNCED_IDS = "synced_media_ids"

    fun loadConfig(context: Context): WebDavConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return WebDavConfig(
            serverUrl = prefs.getString(KEY_SERVER_URL, "") ?: "",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            password = prefs.getString(KEY_PASSWORD, "") ?: "",
            remoteDirectory = prefs.getString(KEY_REMOTE_DIR, "Imava_Backup") ?: "Imava_Backup",
            syncWifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, true),
            syncChargingOnly = prefs.getBoolean(KEY_CHARGING_ONLY, false),
            includeVideos = prefs.getBoolean(KEY_INCLUDE_VIDEOS, true),
            autoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP, false),
            lastBackupTimestamp = prefs.getLong(KEY_LAST_TIMESTAMP, 0L),
            lastBackupCount = prefs.getInt(KEY_LAST_COUNT, 0),
            lastBackupStatus = prefs.getString(KEY_LAST_STATUS, "Never synced") ?: "Never synced"
        )
    }

    fun saveConfig(context: Context, config: WebDavConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SERVER_URL, config.serverUrl.trim())
            .putString(KEY_USERNAME, config.username.trim())
            .putString(KEY_PASSWORD, config.password)
            .putString(KEY_REMOTE_DIR, config.remoteDirectory.trim())
            .putBoolean(KEY_WIFI_ONLY, config.syncWifiOnly)
            .putBoolean(KEY_CHARGING_ONLY, config.syncChargingOnly)
            .putBoolean(KEY_INCLUDE_VIDEOS, config.includeVideos)
            .putBoolean(KEY_AUTO_BACKUP, config.autoBackupEnabled)
            .putLong(KEY_LAST_TIMESTAMP, config.lastBackupTimestamp)
            .putInt(KEY_LAST_COUNT, config.lastBackupCount)
            .putString(KEY_LAST_STATUS, config.lastBackupStatus)
            .apply()
    }

    fun updateLastBackup(context: Context, timestamp: Long, count: Int, status: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_TIMESTAMP, timestamp)
            .putInt(KEY_LAST_COUNT, count)
            .putString(KEY_LAST_STATUS, status)
            .apply()
    }

    fun getSyncedIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SYNCED_IDS, emptySet()) ?: emptySet()
    }

    fun markIdAsSynced(context: Context, mediaId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_SYNCED_IDS, emptySet()) ?: emptySet()).toMutableSet()
        current.add(mediaId.toString())
        prefs.edit().putStringSet(KEY_SYNCED_IDS, current).apply()
    }

    fun markIdsAsSynced(context: Context, mediaIds: Collection<Long>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_SYNCED_IDS, emptySet()) ?: emptySet()).toMutableSet()
        mediaIds.forEach { current.add(it.toString()) }
        prefs.edit().putStringSet(KEY_SYNCED_IDS, current).apply()
    }

    fun clearSyncedIds(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SYNCED_IDS).apply()
    }
}
