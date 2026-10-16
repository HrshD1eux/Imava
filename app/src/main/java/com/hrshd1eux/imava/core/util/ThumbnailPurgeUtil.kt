package com.hrshd1eux.imava.core.util

import android.content.Context
import android.os.Environment
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Ghost Thumbnail & Orphan Preview Cache Purger.
 *
 * When images are deleted or shredded, operating systems, camera stacks, and image loaders
 * frequently leave unencrypted 512px thumbnail files intact inside hidden storage folders
 * (`DCIM/.thumbnails/`, `Pictures/.thumbnails/`, and internal app cache directories).
 * Recovery tools and forensic scanners (DiskDigger, Recuva) routinely resurrect "deleted"
 * photos by harvesting these ghost thumbnails.
 *
 * This utility detects all lingering thumbnail and preview cache files across internal and
 * external storage and cryptographically shreds them using [FileShredderUtil.shredFile].
 */
object ThumbnailPurgeUtil {

    data class ThumbnailScanResult(
        val fileCount: Int,
        val totalSizeBytes: Long,
        val targetDirectories: List<String>
    )

    data class PurgeResult(
        val filesPurged: Int,
        val bytesReclaimed: Long,
        val success: Boolean
    )

    /**
     * Resolves all known thumbnail cache directories across public external and private internal storage.
     */
    private fun getThumbnailDirectories(context: Context): List<File> {
        val dirs = mutableListOf<File>()

        // DCIM/.thumbnails
        try {
            val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            dirs.add(File(dcim, ".thumbnails"))
        } catch (_: Exception) {}

        // Pictures/.thumbnails
        try {
            val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            dirs.add(File(pictures, ".thumbnails"))
        } catch (_: Exception) {}

        // Root storage .thumbnails fallback
        try {
            val root = Environment.getExternalStorageDirectory()
            dirs.add(File(root, ".thumbnails"))
            dirs.add(File(root, "DCIM/.thumbnails"))
            dirs.add(File(root, "Pictures/.thumbnails"))
        } catch (_: Exception) {}

        // Coil & App Image Cache
        try {
            val cacheDir = context.cacheDir
            dirs.add(File(cacheDir, "image_cache"))
            dirs.add(File(cacheDir, "coil_disk_cache"))
            dirs.add(File(cacheDir, "shared_images"))
            dirs.add(cacheDir)
        } catch (_: Exception) {}

        // External app cache
        try {
            context.externalCacheDir?.let { extCache ->
                dirs.add(File(extCache, "image_cache"))
                dirs.add(extCache)
            }
        } catch (_: Exception) {}

        return dirs.filter { it.exists() && it.isDirectory }.distinctBy { it.absolutePath }
    }

    /**
     * Scans for thumbnail files and lingering preview caches without modifying them.
     */
    suspend fun scanGhostThumbnails(context: Context): ThumbnailScanResult = withContext(Dispatchers.IO) {
        val directories = getThumbnailDirectories(context)
        var count = 0
        var totalBytes = 0L
        val activeDirs = mutableListOf<String>()

        for (dir in directories) {
            val files = listThumbnailFiles(dir, isRootAppCache = dir == context.cacheDir || dir == context.externalCacheDir)
            if (files.isNotEmpty()) {
                activeDirs.add(dir.absolutePath)
                count += files.size
                totalBytes += files.sumOf { it.length() }
            }
        }

        ThumbnailScanResult(
            fileCount = count,
            totalSizeBytes = totalBytes,
            targetDirectories = activeDirs
        )
    }

    /**
     * Cryptographically shreds all identified thumbnail cache files and flushes in-memory and disk caches.
     */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun purgeAndShredGhostThumbnails(
        context: Context,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): PurgeResult = withContext(Dispatchers.IO) {
        val directories = getThumbnailDirectories(context)
        val allTargetFiles = mutableListOf<File>()

        for (dir in directories) {
            allTargetFiles.addAll(
                listThumbnailFiles(dir, isRootAppCache = dir == context.cacheDir || dir == context.externalCacheDir)
            )
        }

        val total = allTargetFiles.size
        var purgedCount = 0
        var bytesReclaimed = 0L

        for ((index, file) in allTargetFiles.withIndex()) {
            val size = file.length()
            val shredded = FileShredderUtil.shredFile(file)
            if (shredded || !file.exists()) {
                purgedCount++
                bytesReclaimed += size
            }
            onProgress?.invoke(index + 1, total)
        }

        // Flush Coil Memory & Disk cache to prevent resurrected thumbnail bitmaps
        try {
            context.imageLoader.memoryCache?.clear()
            context.imageLoader.diskCache?.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        PurgeResult(
            filesPurged = purgedCount,
            bytesReclaimed = bytesReclaimed,
            success = true
        )
    }

    private fun listThumbnailFiles(dir: File, isRootAppCache: Boolean): List<File> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val result = mutableListOf<File>()
        val files = dir.listFiles() ?: return emptyList()

        for (file in files) {
            if (file.isFile) {
                if (isRootAppCache) {
                    // In root app cache, only target image/preview artifacts, not config or database files
                    val name = file.name.lowercase()
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                        name.endsWith(".webp") || name.contains("thumb") || name.endsWith(".tmp")
                    ) {
                        result.add(file)
                    }
                } else {
                    // In dedicated .thumbnails or image_cache directories, all contents are preview caches
                    if (!file.name.equals(".nomedia", ignoreCase = true)) {
                        result.add(file)
                    }
                }
            } else if (file.isDirectory && !isRootAppCache) {
                // Traverse subdirectories in dedicated thumbnail caches
                result.addAll(listThumbnailFiles(file, isRootAppCache = false))
            }
        }
        return result
    }
}
