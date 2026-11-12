package com.hrshd1eux.imava.core.p2p

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.IntentCompat
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

/**
 * Extracts, validates, and temporarily stages media URIs received from the Android System Share Sheet
 * (`ACTION_SEND` / `ACTION_SEND_MULTIPLE`) so [ImavaDropServer] can reliably serve external
 * FileProvider URIs over local Wi-Fi even after transient Activity grants expire.
 */
object ImavaDropShareHelper {

    internal const val STAGE_ROOT_DIR = "imava_drop_share_stage"
    private val UNSAFE_FILENAME_CHARS = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    /** Collects deduplicated stream URIs from `ACTION_SEND` or `ACTION_SEND_MULTIPLE` intents. */
    fun extractShareUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val uris = LinkedHashSet<Uri>()

        when (intent.action) {
            Intent.ACTION_SEND -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
                    uris.add(it)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.forEach {
                    if (it != null) uris.add(it)
                }
            }
        }

        val clipData = intent.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i)?.uri?.let { uris.add(it) }
            }
        }

        return uris.toList()
    }

    private fun resolveMimeFromExtension(ext: String): String? {
        val fromSystem = try {
            MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(ext)?.lowercase()
        } catch (_: Exception) {
            null
        }
        if (!fromSystem.isNullOrBlank()) return fromSystem
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            "avif" -> "image/avif"
            "dng" -> "image/x-adobe-dng"
            "mp4", "m4v" -> "video/mp4"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            else -> null
        }
    }

    /**
     * Resolves a concrete `image/...` or `video/...` MIME type from resolver metadata, filename extension,
     * and intent fallback, returning `null` for unsupported non-media types.
     */
    internal fun resolveConcreteMimeType(
        rawMime: String?,
        intentType: String?,
        fileName: String?
    ): String? {
        val cleanRaw = rawMime?.substringBefore(';')?.trim()?.lowercase()
        val cleanIntent = intentType?.substringBefore(';')?.trim()?.lowercase()

        if (cleanRaw != null && isConcreteMediaMime(cleanRaw)) {
            return cleanRaw
        }

        val ext = fileName?.substringAfterLast('.', "")?.trim()?.lowercase().orEmpty()
        if (ext.isNotEmpty() && ext != fileName?.lowercase()) {
            val fromExt = resolveMimeFromExtension(ext)
            if (fromExt != null && isConcreteMediaMime(fromExt)) {
                return fromExt
            }
        }

        val fallback = when {
            cleanRaw != null && isMediaWildcardOrConcrete(cleanRaw) -> cleanRaw
            cleanIntent != null && isMediaWildcardOrConcrete(cleanIntent) -> cleanIntent
            else -> null
        } ?: return null

        return when (fallback) {
            "image/*" -> "image/jpeg"
            "video/*" -> "video/mp4"
            else -> fallback
        }
    }

    private fun isConcreteMediaMime(mime: String): Boolean {
        return (mime.startsWith("image/") || mime.startsWith("video/")) && !mime.endsWith("/*")
    }

    private fun isMediaWildcardOrConcrete(mime: String): Boolean {
        return mime.startsWith("image/") || mime.startsWith("video/")
    }

    /** Sanitizes an incoming display name into a safe leaf filename with an appropriate extension. */
    internal fun sanitizeFileName(rawName: String?, mimeType: String, index: Int): String {
        val leaf = rawName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.replace("..", "_")
            ?.replace(UNSAFE_FILENAME_CHARS, "_")
            ?.trim(' ', '.')
            .orEmpty()

        val defaultExt = resolveDefaultExtension(mimeType)
        val fallbackBase = if (mimeType.startsWith("video/")) "shared_video_${index + 1}" else "shared_photo_${index + 1}"
        val baseName = leaf.ifBlank { "$fallbackBase.$defaultExt" }

        val dotIdx = baseName.lastIndexOf('.')
        val hasValidExt = dotIdx > 0 && dotIdx < baseName.length - 1
        return if (hasValidExt) baseName else "$baseName.$defaultExt"
    }

    private fun resolveDefaultExtension(mimeType: String): String {
        val fromMap = try {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType.lowercase())
        } catch (_: Exception) {
            null
        }
        if (!fromMap.isNullOrBlank()) return fromMap
        return when {
            mimeType.equals("image/png", ignoreCase = true) -> "png"
            mimeType.equals("image/webp", ignoreCase = true) -> "webp"
            mimeType.equals("image/gif", ignoreCase = true) -> "gif"
            mimeType.startsWith("video/", ignoreCase = true) -> "mp4"
            else -> "jpg"
        }
    }

    /** Allocates a non-colliding file inside [stageDir] for [fileName]. */
    internal fun resolveUniqueTargetFile(stageDir: File, fileName: String): File {
        var candidate = File(stageDir, fileName)
        if (!candidate.exists()) return candidate

        val dotIndex = fileName.lastIndexOf('.')
        val stem = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        val ext = if (dotIndex > 0) fileName.substring(dotIndex) else ""

        var suffix = 1
        while (candidate.exists()) {
            candidate = File(stageDir, "${stem}_$suffix$ext")
            suffix++
        }
        return candidate
    }

    /**
     * Stages all supported shared media URIs into `cacheDir/imava_drop_share_stage/` so local HTTP
     * transfers never break if external ContentProvider permissions expire.
     */
    suspend fun stageSharedMedia(
        context: Context,
        intent: Intent?,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val uris = extractShareUris(intent)
        if (uris.isEmpty()) return@withContext emptyList()

        val rootStageDir = File(context.cacheDir, STAGE_ROOT_DIR)
        if (!ImavaDropServer.isRunning()) {
            try { rootStageDir.deleteRecursively() } catch (_: Exception) {}
        }

        val sessionDir = File(rootStageDir, "session_${System.currentTimeMillis()}").apply {
            mkdirs()
        }

        val stagedItems = mutableListOf<MediaItem>()
        val total = uris.size
        onProgress(0, total)

        uris.forEachIndexed { index, uri ->
            coroutineContext.ensureActive()

            val queriedName = queryDisplayName(context, uri)
            val resolvedMime = resolveConcreteMimeType(
                rawMime = try { context.contentResolver.getType(uri) } catch (_: Exception) { null },
                intentType = intent?.type,
                fileName = queriedName ?: uri.lastPathSegment
            )

            if (resolvedMime == null) {
                onProgress(index + 1, total)
                return@forEachIndexed
            }

            val safeName = sanitizeFileName(queriedName ?: uri.lastPathSegment, resolvedMime, index)
            val targetFile = resolveUniqueTargetFile(sessionDir, safeName)

            val copied = try {
                val input = context.contentResolver.openInputStream(uri)
                if (input == null) {
                    false
                } else {
                    input.use { src ->
                        FileOutputStream(targetFile).use { dst ->
                            val buffer = ByteArray(65536)
                            var bytesRead: Int
                            var totalWritten = 0L
                            while (src.read(buffer).also { bytesRead = it } != -1) {
                                coroutineContext.ensureActive()
                                dst.write(buffer, 0, bytesRead)
                                totalWritten += bytesRead
                            }
                            dst.flush()
                            totalWritten > 0L
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                try { targetFile.delete() } catch (_: Exception) {}
                throw e
            } catch (_: Exception) {
                try { targetFile.delete() } catch (_: Exception) {}
                false
            }

            if (copied && targetFile.exists() && targetFile.length() > 0L) {
                val itemId = -(index + 1L)
                val now = System.currentTimeMillis()
                val fileSize = targetFile.length()
                val fileUri = Uri.fromFile(targetFile)

                val mediaItem = if (resolvedMime.startsWith("video/")) {
                    val (width, height, durationMs) = probeVideoMetadata(targetFile)
                    MediaItem.Video(
                        id = itemId,
                        uri = fileUri,
                        path = targetFile.absolutePath,
                        mimeType = resolvedMime,
                        dateTaken = now,
                        size = fileSize,
                        width = width,
                        height = height,
                        durationMs = durationMs
                    )
                } else {
                    val (width, height) = probeImageBounds(targetFile)
                    MediaItem.Photo(
                        id = itemId,
                        uri = fileUri,
                        path = targetFile.absolutePath,
                        mimeType = resolvedMime,
                        dateTaken = now,
                        size = fileSize,
                        width = width,
                        height = height
                    )
                }
                stagedItems.add(mediaItem)
            }

            onProgress(index + 1, total)
        }

        if (stagedItems.isEmpty()) {
            try { sessionDir.deleteRecursively() } catch (_: Exception) {}
        }
        stagedItems
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (col >= 0) cursor.getString(col) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun probeImageBounds(file: File): Pair<Int, Int> {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            opts.outWidth.coerceAtLeast(0) to opts.outHeight.coerceAtLeast(0)
        } catch (_: Exception) {
            0 to 0
        }
    }

    private fun probeVideoMetadata(file: File): Triple<Int, Int, Long> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            Triple(width, height, duration)
        } catch (_: Exception) {
            Triple(0, 0, 0L)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /** Removes any staged temporary shared media files from `cacheDir`. */
    fun clearStagedFiles(context: Context?) {
        if (context == null) return
        try {
            File(context.cacheDir, STAGE_ROOT_DIR).deleteRecursively()
        } catch (_: Exception) {}
    }
}
