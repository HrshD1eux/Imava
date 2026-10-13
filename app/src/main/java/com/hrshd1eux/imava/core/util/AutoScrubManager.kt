package com.hrshd1eux.imava.core.util

import android.content.Context
import com.hrshd1eux.imava.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AutoScrubManager {

    private const val PREFS_NAME = "gallery_prefs"
    private const val KEY_AUTO_SCRUB_BUCKETS = "auto_scrub_bucket_ids"
    private const val KEY_AUTO_SCRUB_PRESERVE_DATE = "auto_scrub_preserve_date"
    private const val KEY_LAST_AUTO_SCRUB_TIME = "last_auto_scrub_timestamp"

    fun getAutoScrubBucketIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_AUTO_SCRUB_BUCKETS, emptySet()) ?: emptySet()
    }

    fun isAutoScrubEnabledForBucket(context: Context, bucketId: Long): Boolean {
        return getAutoScrubBucketIds(context).contains(bucketId.toString())
    }

    fun setAutoScrubEnabledForBucket(context: Context, bucketId: Long, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_AUTO_SCRUB_BUCKETS, emptySet()) ?: emptySet()).toMutableSet()
        val key = bucketId.toString()
        if (enabled) {
            current.add(key)
        } else {
            current.remove(key)
        }
        prefs.edit().putStringSet(KEY_AUTO_SCRUB_BUCKETS, current).apply()
    }

    fun toggleAutoScrubForBucket(context: Context, bucketId: Long): Boolean {
        val isCurrentlyEnabled = isAutoScrubEnabledForBucket(context, bucketId)
        val nextState = !isCurrentlyEnabled
        setAutoScrubEnabledForBucket(context, bucketId, nextState)
        return nextState
    }

    fun isPreserveCaptureDate(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_SCRUB_PRESERVE_DATE, true)
    }

    fun setPreserveCaptureDate(context: Context, preserve: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_SCRUB_PRESERVE_DATE, preserve).apply()
    }

    /**
     * Executes auto-scrub on media items that belong to designated auto-scrub buckets.
     * Implements a 3.5s debounce on file creation/write to prevent collisions with active camera encoding.
     * Returns count of sanitized items.
     */
    suspend fun executeAutoScrub(
        context: Context,
        allItems: List<MediaItem>
    ): Int = withContext(Dispatchers.IO) {
        val targetBucketIds = getAutoScrubBucketIds(context)
        if (targetBucketIds.isEmpty()) return@withContext 0

        val preserveDate = isPreserveCaptureDate(context)
        val options = ExifSanitizerUtil.ScrubOptions(
            preserveCaptureDate = preserveDate,
            preserveOrientation = true
        )

        val now = System.currentTimeMillis()
        var scrubbedCount = 0

        // Find candidate images in designated buckets
        val candidates = allItems.filter { item ->
            item.mimeType.contains("image", ignoreCase = true) &&
                    targetBucketIds.contains(item.bucketId.toString()) &&
                    !item.isHidden
        }

        for (item in candidates) {
            val file = File(item.path)
            // Debounce check: wait at least 3.5 seconds after file write
            if (file.exists()) {
                val age = now - file.lastModified()
                if (age in 0..3500L) {
                    continue // still potentially being written by camera
                }
                if (!ExifSanitizerUtil.hasMetadataToStrip(file, options)) {
                    continue // already clean
                }
            }

            val success = ExifSanitizerUtil.scrubItemInPlace(context, item, options)
            if (success) {
                scrubbedCount++
            }
        }

        if (scrubbedCount > 0) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_AUTO_SCRUB_TIME, now).apply()
        }

        scrubbedCount
    }
}
