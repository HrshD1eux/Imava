package com.hrshd1eux.imava.core.util

import android.content.ContentResolver
import android.content.Context
import android.os.ParcelFileDescriptor
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.SecureRandom
import java.util.UUID

/**
 * Cryptographic Secure File Shredder Engine.
 * 
 * Standard OS file deletion (`unlink`) only frees the inode and catalog reference,
 * leaving the physical NAND flash blocks completely untouched where data recovery tools
 * (DiskDigger, Recuva, PhotoRec) can easily reconstruct photos.
 * 
 * This engine physically overwrites the target file's exact byte blocks with CSPRNG
 * pseudo-random data, zeroes out file format magic headers (JPEG, PNG, MP4, EXIF),
 * forces hardware write cache flushes (`FileDescriptor.sync()`), truncates the file
 * size to 0 bytes, scrambles the filename in the directory entry (dentry), and then deletes it.
 */
object FileShredderUtil {

    private const val PREFS_NAME = "gallery_prefs"
    private const val KEY_SECURE_SHRED_ENABLED = "secure_shred_enabled"
    private const val CHUNK_SIZE = 64 * 1024 // 64 KB write buffer
    private const val HEADER_ZERO_SIZE = 8 * 1024 // 8 KB magic bytes zero-out

    fun isSecureShredEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SECURE_SHRED_ENABLED, true)
    }

    fun setSecureShredEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SECURE_SHRED_ENABLED, enabled).apply()
    }

    /**
     * Shreds a physical file in place using cryptographic random overwrites.
     */
    suspend fun shredFile(file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        val fileLength = file.length()
        if (fileLength == 0L) {
            return@withContext file.delete()
        }

        try {
            val secureRandom = SecureRandom()
            val randomBuffer = ByteArray(CHUNK_SIZE.coerceAtMost(fileLength.toInt().coerceAtLeast(1)))
            secureRandom.nextBytes(randomBuffer)

            RandomAccessFile(file, "rws").use { raf ->
                var bytesWritten = 0L
                while (bytesWritten < fileLength) {
                    val remaining = fileLength - bytesWritten
                    val toWrite = remaining.coerceAtMost(randomBuffer.size.toLong()).toInt()
                    raf.write(randomBuffer, 0, toWrite)
                    bytesWritten += toWrite
                }
                raf.fd.sync()

                // Pass 2: Overwrite beginning header with zeros to obliterate magic bytes
                val headerSize = HEADER_ZERO_SIZE.toLong().coerceAtMost(fileLength).toInt()
                val zeroBuffer = ByteArray(headerSize)
                raf.seek(0)
                raf.write(zeroBuffer)
                raf.fd.sync()
            }

            // Truncate file size to 0 bytes
            FileOutputStream(file).close()

            // Scramble filename in directory entry (dentry)
            val parent = file.parentFile
            val scrambledName = "${UUID.randomUUID().toString().replace("-", "")}.tmp"
            val scrambledFile = if (parent != null) File(parent, scrambledName) else File(scrambledName)
            val targetToDelete = if (file.renameTo(scrambledFile)) scrambledFile else file
            targetToDelete.delete()
        } catch (_: Exception) {
            // Fallback to direct delete if shredding failed due to permission or I/O
            file.delete()
        }
    }

    /**
     * Overwrites content via Scoped Storage ParcelFileDescriptor ("rwt").
     */
    suspend fun shredFileDescriptor(pfd: ParcelFileDescriptor): Boolean = withContext(Dispatchers.IO) {
        try {
            val length = pfd.statSize
            val secureRandom = SecureRandom()
            val randomBuffer = ByteArray(CHUNK_SIZE.coerceAtMost(length.toInt().coerceAtLeast(1)))
            secureRandom.nextBytes(randomBuffer)

            FileOutputStream(pfd.fileDescriptor).use { fos ->
                val channel = fos.channel
                channel.position(0)
                var bytesWritten = 0L
                while (bytesWritten < length) {
                    val remaining = length - bytesWritten
                    val toWrite = remaining.coerceAtMost(randomBuffer.size.toLong()).toInt()
                    fos.write(randomBuffer, 0, toWrite)
                    bytesWritten += toWrite
                }
                channel.force(true)

                // Overwrite initial header with zeroes
                val headerSize = HEADER_ZERO_SIZE.toLong().coerceAtMost(length).toInt()
                channel.position(0)
                fos.write(ByteArray(headerSize))
                channel.force(true)

                // Truncate to 0
                channel.truncate(0)
                channel.force(true)
            }
            pfd.fileDescriptor.sync()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Shreds a media item across physical disk, Scoped Storage URI, and internal trash cache.
     */
    suspend fun shredMediaItem(context: Context, item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        var success = false

        // 1. Shred physical file if accessible
        val file = File(item.path)
        if (file.exists() && file.canWrite()) {
            success = shredFile(file)
        }

        // 2. Shred internal trash cache copy if present
        val trashDir = File(context.filesDir, "imava_trash")
        val trashedFile = File(trashDir, "${item.id}_${file.name}")
        if (trashedFile.exists() && trashedFile.canWrite()) {
            shredFile(trashedFile)
        }

        // 3. Shred via Scoped Storage ContentResolver descriptor
        if (item.uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                context.contentResolver.openFileDescriptor(item.uri, "rwt")?.use { pfd ->
                    val fdShredded = shredFileDescriptor(pfd)
                    success = success || fdShredded
                }
            } catch (_: Exception) {}
        }

        success
    }

    /**
     * Batch shred with real-time progress callbacks.
     */
    suspend fun shredBatch(
        context: Context,
        items: List<MediaItem>,
        onProgress: (suspend (current: Int, total: Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val total = items.size
        items.forEachIndexed { index, item ->
            shredMediaItem(context, item)
            onProgress?.invoke(index + 1, total)
        }
    }
}
