package com.hrshd1eux.imava.core.util

import android.content.Context
import android.os.Environment
import com.hrshd1eux.imava.data.database.MediaMetadataEntity
import com.hrshd1eux.imava.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MetadataAutoBackupManager {

    private const val PREFS_NAME = "gallery_prefs"
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_metadata_backup_enabled"
    private const val KEY_HAS_PROMPTED_RESTORE = "has_prompted_restore_backup"
    private const val BACKUP_DIR_NAME = "Imava"
    private const val BACKUP_FILE_NAME = "imava_metadata_backup.json"

    data class BackupInfo(
        val timestamp: Long,
        val formattedDate: String,
        val tagCount: Int,
        val favoriteCount: Int,
        val file: File
    )

    data class RestoreResult(
        val restoredTagsCount: Int,
        val restoredFavoritesCount: Int,
        val totalRestored: Int
    )

    data class BackupEntry(
        val path: String,
        val tags: String,
        val isFavorite: Boolean,
        val dateTaken: Long = 0L
    )

    fun isAutoBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun hasPromptedRestore(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_PROMPTED_RESTORE, false)
    }

    fun setPromptedRestore(context: Context, prompted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAS_PROMPTED_RESTORE, prompted).apply()
    }

    fun getBackupFile(): File {
        val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), BACKUP_DIR_NAME)
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
        return File(documentsDir, BACKUP_FILE_NAME)
    }

    private fun getExistingBackupFile(): File? {
        val primary = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "$BACKUP_DIR_NAME/$BACKUP_FILE_NAME")
        if (primary.exists() && primary.length() > 0) return primary

        val fallback = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "$BACKUP_DIR_NAME/$BACKUP_FILE_NAME")
        if (fallback.exists() && fallback.length() > 0) return fallback

        return null
    }

    fun buildBackupJson(timestamp: Long, items: List<MediaMetadataEntity>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": 1,\n")
        sb.append("  \"timestamp\": ").append(timestamp).append(",\n")
        sb.append("  \"items\": [\n")
        val validItems = items.filter { it.originalPath.isNotBlank() }
        validItems.forEachIndexed { index, item ->
            sb.append("    {\n")
            sb.append("      \"path\": \"").append(escapeJson(item.originalPath)).append("\",\n")
            sb.append("      \"tags\": \"").append(escapeJson(item.tags)).append("\",\n")
            sb.append("      \"isFavorite\": ").append(item.isFavorite).append(",\n")
            sb.append("      \"dateTaken\": ").append(item.dateTaken).append("\n")
            sb.append("    }").append(if (index < validItems.size - 1) ",\n" else "\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    fun parseBackupJson(content: String): Pair<Long, List<BackupEntry>> {
        // Try platform JSONObject first
        try {
            val json = JSONObject(content)
            val timestamp = json.optLong("timestamp", 0L)
            val itemsArray = json.optJSONArray("items")
            if (itemsArray != null) {
                val list = mutableListOf<BackupEntry>()
                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    val path = obj.optString("path", "")
                    if (path.isNotBlank()) {
                        list.add(
                            BackupEntry(
                                path = path,
                                tags = obj.optString("tags", ""),
                                isFavorite = obj.optBoolean("isFavorite", false),
                                dateTaken = obj.optLong("dateTaken", 0L)
                            )
                        )
                    }
                }
                if (list.isNotEmpty() || timestamp > 0L) {
                    return Pair(timestamp, list)
                }
            }
        } catch (_: Throwable) {}

        // Robust fallback regex parser (works universally including JVM test stubs)
        val timestamp = "\"timestamp\"\\s*:\\s*(\\d+)".toRegex().find(content)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val itemPattern = "\\{\\s*\"path\"\\s*:\\s*\"(.*?)\"\\s*,\\s*\"tags\"\\s*:\\s*\"(.*?)\"\\s*,\\s*\"isFavorite\"\\s*:\\s*(true|false)".toRegex()
        val list = mutableListOf<BackupEntry>()
        itemPattern.findAll(content).forEach { match ->
            val path = unescapeJson(match.groupValues[1])
            val tags = unescapeJson(match.groupValues[2])
            val isFavorite = match.groupValues[3].toBoolean()
            if (path.isNotBlank()) {
                list.add(BackupEntry(path, tags, isFavorite))
            }
        }
        return Pair(timestamp, list)
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }

    private fun unescapeJson(str: String): String {
        return str.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "\r")
    }

    suspend fun detectBackup(context: Context): BackupInfo? = withContext(Dispatchers.IO) {
        try {
            val file = getExistingBackupFile() ?: return@withContext null
            val content = file.readText()
            if (content.isBlank()) return@withContext null

            val (timestamp, items) = parseBackupJson(content)
            if (items.isEmpty() && timestamp <= 0L) return@withContext null

            var tagCount = 0
            var favoriteCount = 0

            for (item in items) {
                if (item.tags.isNotBlank()) tagCount++
                if (item.isFavorite) favoriteCount++
            }

            if (tagCount == 0 && favoriteCount == 0) return@withContext null

            val effectiveTimestamp = if (timestamp > 0L) timestamp else file.lastModified()
            val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(effectiveTimestamp))

            BackupInfo(
                timestamp = effectiveTimestamp,
                formattedDate = formattedDate,
                tagCount = tagCount,
                favoriteCount = favoriteCount,
                file = file
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun performBackup(context: Context, metadataList: List<MediaMetadataEntity>): Boolean = withContext(Dispatchers.IO) {
        if (!isAutoBackupEnabled(context)) return@withContext false

        val candidateItems = metadataList.filter {
            !it.isHidden && !it.isTrashed && (it.tags.isNotBlank() || it.isFavorite) && it.originalPath.isNotBlank()
        }

        if (candidateItems.isEmpty()) return@withContext false

        try {
            val targetFile = getBackupFile()
            val tempFile = File(targetFile.parentFile, "$BACKUP_FILE_NAME.tmp")

            val jsonContent = buildBackupJson(System.currentTimeMillis(), candidateItems)

            FileOutputStream(tempFile).use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
                out.flush()
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(context: Context, repository: MediaRepository): RestoreResult = withContext(Dispatchers.IO) {
        val file = getExistingBackupFile() ?: return@withContext RestoreResult(0, 0, 0)
        try {
            val content = file.readText()
            if (content.isBlank()) return@withContext RestoreResult(0, 0, 0)

            val (_, items) = parseBackupJson(content)
            if (items.isEmpty()) return@withContext RestoreResult(0, 0, 0)

            // Map backed-up paths to metadata
            val backupMap = mutableMapOf<String, Pair<String, Boolean>>() // path -> (tags, isFavorite)
            for (item in items) {
                if (item.path.isNotBlank()) {
                    backupMap[item.path] = Pair(item.tags, item.isFavorite)
                }
            }

            if (backupMap.isEmpty()) return@withContext RestoreResult(0, 0, 0)

            // Fetch live media items to match physical files to current MediaStore IDs
            val liveMedia = repository.getAllMediaItemsDirect()
            val pathToMediaMap = liveMedia.filter { it.path.isNotBlank() }.associateBy { it.path }

            val existingMeta = repository.getAllMetadata().associateBy { it.mediaId }
            val updatesToApply = mutableListOf<MediaMetadataEntity>()

            var restoredTags = 0
            var restoredFavorites = 0

            for ((backupPath, data) in backupMap) {
                val (tags, isFav) = data
                val liveItem = pathToMediaMap[backupPath]
                val mediaId = liveItem?.id ?: continue
                val existing = existingMeta[mediaId] ?: MediaMetadataEntity(
                    mediaId = mediaId,
                    originalPath = backupPath,
                    mimeType = liveItem.mimeType,
                    dateTaken = liveItem.dateTaken,
                    size = liveItem.size,
                    bucketId = liveItem.bucketId,
                    bucketName = liveItem.bucketName
                )

                var modified = false
                var newTags = existing.tags
                var newFav = existing.isFavorite

                if (tags.isNotBlank()) {
                    val existingTagsSet = if (existing.tags.isNotBlank()) existing.tags.split(",").map { it.trim() }.toSet() else emptySet()
                    val backupTagsSet = tags.split(",").map { it.trim() }.toSet()
                    val merged = (existingTagsSet + backupTagsSet).filter { it.isNotEmpty() }.sorted().joinToString(",")
                    if (merged != existing.tags) {
                        newTags = merged
                        modified = true
                        restoredTags++
                    }
                }

                if (isFav && !existing.isFavorite) {
                    newFav = true
                    modified = true
                    restoredFavorites++
                }

                if (modified) {
                    updatesToApply.add(existing.copy(tags = newTags, isFavorite = newFav))
                }
            }

            if (updatesToApply.isNotEmpty()) {
                repository.restoreMetadata(updatesToApply)
            }

            setPromptedRestore(context, true)
            RestoreResult(restoredTags, restoredFavorites, updatesToApply.size)
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult(0, 0, 0)
        }
    }
}
