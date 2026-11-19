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
    private const val KEY_PASSWORD_ENCRYPTED = "password_encrypted"
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
        val encryptedPassword = prefs.getString(KEY_PASSWORD_ENCRYPTED, null)
        val password = if (!encryptedPassword.isNullOrBlank()) {
            try {
                com.hrshd1eux.imava.core.util.VaultCrypto.decryptString(encryptedPassword)
            } catch (_: Exception) {
                encryptedPassword
            }
        } else {
            val legacy = prefs.getString(KEY_PASSWORD, "") ?: ""
            if (legacy.isNotBlank()) {
                try {
                    val enc = com.hrshd1eux.imava.core.util.VaultCrypto.encryptString(legacy)
                    prefs.edit().putString(KEY_PASSWORD_ENCRYPTED, enc).remove(KEY_PASSWORD).apply()
                } catch (_: Exception) {}
            }
            legacy
        }

        return WebDavConfig(
            serverUrl = prefs.getString(KEY_SERVER_URL, "") ?: "",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            password = password,
            remoteDirectory = prefs.getString(KEY_REMOTE_DIR, "Imava_Backup") ?: "Imava_Backup",
            syncWifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, true),
            syncChargingOnly = prefs.getBoolean(KEY_CHARGING_ONLY, false),
            includeVideos = prefs.getBoolean(KEY_INCLUDE_VIDEOS, true),
            autoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP, false),
            lastBackupTimestamp = prefs.getLong(KEY_LAST_TIMESTAMP, 0L),
            lastBackupCount = prefs.getInt(KEY_LAST_COUNT, 0),
            lastBackupStatus = prefs.getString(KEY_LAST_STATUS, null)
                ?: com.hrshd1eux.imava.core.util.LocaleHelper.getString(
                    context,
                    com.hrshd1eux.imava.R.string.webdav_status_never_synced,
                    "Never synced"
                )
        )
    }

    fun saveConfig(context: Context, config: WebDavConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedPassword = if (config.password.isNotBlank()) {
            try {
                com.hrshd1eux.imava.core.util.VaultCrypto.encryptString(config.password)
            } catch (_: Exception) {
                config.password
            }
        } else ""

        prefs.edit()
            .putString(KEY_SERVER_URL, config.serverUrl.trim())
            .putString(KEY_USERNAME, config.username.trim())
            .putString(KEY_PASSWORD_ENCRYPTED, encryptedPassword)
            .remove(KEY_PASSWORD)
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
        return try {
            val dao = com.hrshd1eux.imava.data.database.GalleryDatabase.getInstance(context).webDavSyncedDao()
            val roomIds = kotlinx.coroutines.runBlocking { dao.getAllSyncedIds() }.map { it.toString() }.toSet()
            if (roomIds.isNotEmpty()) {
                roomIds
            } else {
                val legacy = prefs.getStringSet(KEY_SYNCED_IDS, emptySet()) ?: emptySet()
                if (legacy.isNotEmpty()) {
                    val entities = legacy.mapNotNull { it.toLongOrNull() }.map {
                        com.hrshd1eux.imava.data.database.WebDavSyncedEntity(it)
                    }
                    kotlinx.coroutines.runBlocking { dao.markAllSynced(entities) }
                    prefs.edit().remove(KEY_SYNCED_IDS).apply()
                }
                legacy
            }
        } catch (_: Exception) {
            prefs.getStringSet(KEY_SYNCED_IDS, emptySet()) ?: emptySet()
        }
    }

    fun markIdAsSynced(context: Context, mediaId: Long) {
        try {
            val dao = com.hrshd1eux.imava.data.database.GalleryDatabase.getInstance(context).webDavSyncedDao()
            kotlinx.coroutines.runBlocking {
                dao.markSynced(com.hrshd1eux.imava.data.database.WebDavSyncedEntity(mediaId))
            }
        } catch (_: Exception) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = (prefs.getStringSet(KEY_SYNCED_IDS, emptySet()) ?: emptySet()).toMutableSet()
            current.add(mediaId.toString())
            prefs.edit().putStringSet(KEY_SYNCED_IDS, current).apply()
        }
    }

    fun markIdsAsSynced(context: Context, mediaIds: Collection<Long>) {
        try {
            val dao = com.hrshd1eux.imava.data.database.GalleryDatabase.getInstance(context).webDavSyncedDao()
            val entities = mediaIds.map { com.hrshd1eux.imava.data.database.WebDavSyncedEntity(it) }
            kotlinx.coroutines.runBlocking { dao.markAllSynced(entities) }
        } catch (_: Exception) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = (prefs.getStringSet(KEY_SYNCED_IDS, emptySet()) ?: emptySet()).toMutableSet()
            mediaIds.forEach { current.add(it.toString()) }
            prefs.edit().putStringSet(KEY_SYNCED_IDS, current).apply()
        }
    }

    fun clearSyncedIds(context: Context) {
        try {
            val dao = com.hrshd1eux.imava.data.database.GalleryDatabase.getInstance(context).webDavSyncedDao()
            kotlinx.coroutines.runBlocking { dao.clearAll() }
        } catch (_: Exception) {}
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SYNCED_IDS).apply()
    }
}
