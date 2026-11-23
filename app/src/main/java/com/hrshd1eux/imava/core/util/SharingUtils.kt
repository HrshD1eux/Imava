package com.hrshd1eux.imava.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SharingUtils {

    suspend fun shareMedia(
        context: Context,
        items: List<MediaItem>,
        privacyMode: SharePrivacyMode = SharePrivacyMode.STRIP_ALL
    ) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext

        // Cleanup old shared cache folders (> 30 mins)
        listOfNotNull(context.cacheDir, context.externalCacheDir).forEach { baseDir ->
            try {
                val parentFolder = File(baseDir, if (baseDir == context.cacheDir) "shared_images" else "shared_media")
                if (parentFolder.exists()) {
                    parentFolder.listFiles()?.forEach { file ->
                        if (file.isDirectory && (System.currentTimeMillis() - file.lastModified() > 1800000)) {
                            file.deleteRecursively()
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        var sharedFolderCache: File? = null

        val uris = items.mapNotNull { item ->
            try {
                val isImage = item.mimeType.contains("image", ignoreCase = true)
                val needsExifSanitization = isImage && privacyMode != SharePrivacyMode.FULL_METADATA

                // 1. FAST ZERO-COPY DIRECT URI:
                // For all public videos and public photos not requiring EXIF stripping.
                // Avoids copying 500MB-1.5GB 4K videos into internal cache, completely eliminating OOM & ENOSPC crashes!
                if (!item.isHidden && !needsExifSanitization) {
                    val originalFile = File(item.path)
                    if (originalFile.exists() && originalFile.canRead()) {
                        try {
                            return@mapNotNull FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                originalFile
                            )
                        } catch (_: Exception) {}
                    }
                    if (item.uri != Uri.EMPTY && item.uri.scheme == "content") {
                        return@mapNotNull item.uri
                    }
                }

                // 2. VAULT DECRYPTION OR EXIF SANITIZATION REQUIRED:
                // Use externalCacheDir for vault items (vastly larger quota for 4K video decryption)
                val targetBaseDir = if (item.isHidden) {
                    context.externalCacheDir ?: context.cacheDir
                } else {
                    context.cacheDir
                }
                val parentFolder = File(targetBaseDir, if (targetBaseDir == context.cacheDir) "shared_images" else "shared_media").apply { mkdirs() }
                val sharedFolder = sharedFolderCache ?: File(parentFolder, "share_${System.currentTimeMillis()}").apply { mkdirs() }.also {
                    sharedFolderCache = it
                }

                val fileName = "${item.id}_${item.path.substringAfterLast('/', "shared_media")}"
                val tempFile = File(sharedFolder, fileName)

                val inputStream = if (item.uri.scheme == "file" || item.uri.scheme == null) {
                    val f = File(item.path)
                    if (f.exists() && f.canRead()) java.io.FileInputStream(f) else context.contentResolver.openInputStream(item.uri)
                } else {
                    context.contentResolver.openInputStream(item.uri)
                } ?: return@mapNotNull null

                inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        if (item.isHidden) {
                            try {
                                VaultCrypto.decrypt(input, output)
                            } catch (_: Exception) {
                                input.copyTo(output, bufferSize = 64 * 1024)
                            }
                        } else {
                            input.copyTo(output, bufferSize = 64 * 1024)
                        }
                    }
                }

                if (isImage) {
                    when (privacyMode) {
                        SharePrivacyMode.STRIP_ALL -> ExifSanitizerUtil.stripMetadata(tempFile)
                        SharePrivacyMode.FUZZ_LOCATION -> ExifSanitizerUtil.fuzzLocation(tempFile)
                        SharePrivacyMode.FULL_METADATA -> { /* Keep original metadata */ }
                    }
                }

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }

        if (uris.isNotEmpty()) {
            val shareIntent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = items.first().mimeType.ifBlank { "*/*" }
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    clipData = android.content.ClipData.newUri(context.contentResolver, "media", uris.first())
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    val clip = android.content.ClipData.newUri(context.contentResolver, "media", uris.first())
                    for (i in 1 until uris.size) {
                        clip.addItem(android.content.ClipData.Item(uris[i]))
                    }
                    clipData = clip
                }
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooser = Intent.createChooser(
                shareIntent,
                LocaleHelper.getString(context, com.hrshd1eux.imava.R.string.chooser_share_media, "Share media")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(chooser)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    suspend fun shareMedia(
        context: Context,
        items: List<MediaItem>,
        stripMetadata: Boolean
    ) {
        shareMedia(
            context = context,
            items = items,
            privacyMode = if (stripMetadata) SharePrivacyMode.STRIP_ALL else SharePrivacyMode.FULL_METADATA
        )
    }
}
