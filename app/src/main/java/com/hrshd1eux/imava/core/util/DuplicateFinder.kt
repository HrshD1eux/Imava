package com.hrshd1eux.imava.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.math.abs

data class DuplicateGroup(
    val id: String,
    val items: List<MediaItem>,
    val bestItem: MediaItem
)

private data class PhotoSignature(
    val item: MediaItem.Photo,
    val dHash: Long,
    val aHash: Long
)

object DuplicateFinder {

    private const val PREFS_NAME = "duplicate_prefs"
    private const val KEY_IGNORED_PAIRS = "ignored_pairs"

    fun getIgnoredPairs(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_IGNORED_PAIRS, emptySet()) ?: emptySet()
    }

    fun ignorePair(context: Context, id1: Long, id2: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_IGNORED_PAIRS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val key = if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"
        current.add(key)
        prefs.edit().putStringSet(KEY_IGNORED_PAIRS, current).apply()
    }

    fun ignoreGroup(context: Context, group: DuplicateGroup) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_IGNORED_PAIRS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val items = group.items
        for (i in items.indices) {
            for (j in i + 1 until items.size) {
                val id1 = items[i].id
                val id2 = items[j].id
                val key = if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"
                current.add(key)
            }
        }
        prefs.edit().putStringSet(KEY_IGNORED_PAIRS, current).apply()
    }

    suspend fun findDuplicates(
        context: Context,
        items: List<MediaItem>,
        onProgress: ((scanned: Int, total: Int) -> Unit)? = null
    ): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        if (items.size < 2) return@withContext emptyList()

        val ignoredPairs = getIgnoredPairs(context)
        val resultGroups = mutableListOf<DuplicateGroup>()
        val groupedIds = mutableSetOf<Long>()

        // ── Phase 1: Cryptographic SHA-256 Exact Duplicate Detection ─────────
        // Step 1: Pre-filter by file size (> 0)
        val sizeBuckets = items.filter { it.size > 0L }.groupBy { it.size }
        for ((_, sizeList) in sizeBuckets) {
            if (sizeList.size < 2) continue

            // Step 2: 16KB header SHA-256 pre-check to eliminate accidental size collisions
            val partialMap = sizeList.groupBy { computePartialSha256(context, it) }
            for ((partialHash, partialCandidates) in partialMap) {
                if (partialHash == null || partialCandidates.size < 2) continue

                // Step 3: Full streaming SHA-256 cryptographic verification (zero false positives)
                val fullMap = partialCandidates.groupBy { computeFullSha256(context, it) }
                for ((fullHash, rawExactList) in fullMap) {
                    if (fullHash == null || rawExactList.size < 2) continue

                    val exactList = rawExactList.filter { candidate ->
                        rawExactList.any { other ->
                            if (other.id == candidate.id) false
                            else {
                                val pairKey = if (candidate.id < other.id) "${candidate.id}_${other.id}" else "${other.id}_${candidate.id}"
                                !ignoredPairs.contains(pairKey)
                            }
                        }
                    }
                    if (exactList.size < 2) continue

                    val best = exactList.maxWithOrNull(
                        compareBy<MediaItem> { it.width * it.height }
                            .thenByDescending { it.dateTaken }
                    ) ?: exactList.first()

                    resultGroups.add(
                        DuplicateGroup(
                            id = "exact_${fullHash.take(16)}",
                            items = exactList,
                            bestItem = best
                        )
                    )
                    exactList.forEach { groupedIds.add(it.id) }
                }
            }
        }

        // ── Phase 2: Perceptual Duplicate Detection (Similar / Burst Photos) ───
        val remainingPhotos = items.filterIsInstance<MediaItem.Photo>()
            .filter { !groupedIds.contains(it.id) }

        if (remainingPhotos.size < 2) {
            return@withContext resultGroups
        }

        val totalPhotos = remainingPhotos.size
        var scannedCount = 0

        val semaphore = Semaphore(4)
        val signatures = mutableListOf<PhotoSignature>()

        for (item in remainingPhotos) {
            semaphore.withPermit {
                try {
                    val bitmap = decodeMicroThumbnail(context, item)
                    if (bitmap != null) {
                        val dHash = computeDHash(bitmap)
                        val aHash = computeAHash(bitmap)
                        signatures.add(PhotoSignature(item, dHash, aHash))
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    scannedCount++
                    onProgress?.invoke(scannedCount, totalPhotos)
                }
            }
        }

        val visitedSignatures = mutableSetOf<Long>()

        for (i in signatures.indices) {
            val sigA = signatures[i]
            if (visitedSignatures.contains(sigA.item.id)) continue

            val cluster = mutableListOf<MediaItem.Photo>()
            cluster.add(sigA.item)

            for (j in i + 1 until signatures.size) {
                val sigB = signatures[j]
                if (visitedSignatures.contains(sigB.item.id)) continue

                val pairKey = if (sigA.item.id < sigB.item.id) "${sigA.item.id}_${sigB.item.id}" else "${sigB.item.id}_${sigA.item.id}"
                if (ignoredPairs.contains(pairKey)) continue

                if (arePerceptualDuplicates(sigA, sigB)) {
                    cluster.add(sigB.item)
                    visitedSignatures.add(sigB.item.id)
                }
            }

            if (cluster.size > 1) {
                visitedSignatures.add(sigA.item.id)
                val best = cluster.maxWithOrNull(
                    compareBy<MediaItem.Photo> { it.width * it.height }
                        .thenBy { it.size }
                        .thenByDescending { it.dateTaken }
                ) ?: sigA.item

                resultGroups.add(
                    DuplicateGroup(
                        id = "perceptual_${sigA.item.id}",
                        items = cluster,
                        bestItem = best
                    )
                )
            }
        }

        resultGroups
    }

    fun computeFullSha256(context: Context, item: MediaItem): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(32 * 1024)
            val inputStream = if (item.uri != Uri.EMPTY && item.uri.scheme == "content") {
                context.contentResolver.openInputStream(item.uri)
            } else if (item.path.isNotBlank()) {
                val f = File(item.path)
                if (f.exists() && f.canRead()) f.inputStream() else null
            } else null

            inputStream?.use { stream ->
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            } ?: return null

            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun computePartialSha256(context: Context, item: MediaItem): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(16 * 1024)
            val inputStream = if (item.uri != Uri.EMPTY && item.uri.scheme == "content") {
                context.contentResolver.openInputStream(item.uri)
            } else if (item.path.isNotBlank()) {
                val f = File(item.path)
                if (f.exists() && f.canRead()) f.inputStream() else null
            } else null

            inputStream?.use { stream ->
                val readHead = stream.read(buffer, 0, buffer.size)
                if (readHead > 0) {
                    digest.update(buffer, 0, readHead)
                }
            } ?: return null

            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeMicroThumbnail(context: Context, item: MediaItem.Photo): Bitmap? {
        val resolver = context.contentResolver
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565 // 16-bit to halve memory footprint
            inSampleSize = if (item.width > 0 && item.height > 0) {
                val maxDim = maxOf(item.width, item.height)
                if (maxDim > 64) maxDim / 32 else 1
            } else {
                8
            }
        }

        if (item.uri != Uri.EMPTY) {
            try {
                resolver.openInputStream(item.uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream, null, options)
                    if (bmp != null) return bmp
                }
            } catch (_: Exception) {}
        }

        // file fallback
        if (item.path.isNotBlank()) {
            try {
                val file = File(item.path)
                if (file.exists() && file.canRead()) {
                    return BitmapFactory.decodeFile(file.absolutePath, options)
                }
            } catch (_: Exception) {}
        }

        return null
    }

    private fun arePerceptualDuplicates(a: PhotoSignature, b: PhotoSignature): Boolean {
        // Guard against mismatched aspect ratio (e.g. landscape vs portrait)
        val aspectA = if (a.item.height > 0) a.item.width.toFloat() / a.item.height else 1f
        val aspectB = if (b.item.height > 0) b.item.width.toFloat() / b.item.height else 1f
        if (abs(aspectA - aspectB) > 0.08f) {
            return false
        }

        val timeDiffMs = abs(a.item.dateTaken - b.item.dateTaken)
        val dDist = java.lang.Long.bitCount(a.dHash xor b.dHash)
        val aDist = java.lang.Long.bitCount(a.aHash xor b.aHash)

        if (timeDiffMs <= 5000L && (dDist <= 5 && aDist <= 5)) {
            return true
        }

        return dDist <= 3 || (dDist <= 5 && aDist <= 3)
    }

    fun computeDHash(src: Bitmap): Long {
        val softwareSrc = if (src.config == Bitmap.Config.HARDWARE) {
            src.copy(Bitmap.Config.ARGB_8888, false) ?: src
        } else {
            src
        }
        val scaled = Bitmap.createScaledBitmap(softwareSrc, 9, 8, false)
        var hash = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val left = getGrayscalePixel(scaled.getPixel(x, y))
                val right = getGrayscalePixel(scaled.getPixel(x + 1, y))
                if (left > right) {
                    hash = hash or (1L shl (y * 8 + x))
                }
            }
        }
        if (scaled != softwareSrc && scaled != src) {
            scaled.recycle()
        }
        if (softwareSrc != src) {
            softwareSrc.recycle()
        }
        return hash
    }

    fun computeAHash(src: Bitmap): Long {
        val softwareSrc = if (src.config == Bitmap.Config.HARDWARE) {
            src.copy(Bitmap.Config.ARGB_8888, false) ?: src
        } else {
            src
        }
        val scaled = Bitmap.createScaledBitmap(softwareSrc, 8, 8, false)
        val grays = IntArray(64)
        var sum = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val gray = getGrayscalePixel(scaled.getPixel(x, y))
                grays[y * 8 + x] = gray
                sum += gray
            }
        }
        val avg = (sum / 64).toInt()
        var hash = 0L
        for (i in 0 until 64) {
            if (grays[i] >= avg) {
                hash = hash or (1L shl i)
            }
        }
        if (scaled != softwareSrc && scaled != src) {
            scaled.recycle()
        }
        if (softwareSrc != src) {
            softwareSrc.recycle()
        }
        return hash
    }

    private fun getGrayscalePixel(pixel: Int): Int {
        val r = (pixel ushr 16) and 0xFF
        val g = (pixel ushr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 30 + g * 59 + b * 11) / 100
    }
}
