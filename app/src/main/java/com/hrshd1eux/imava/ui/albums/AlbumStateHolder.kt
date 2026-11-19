package com.hrshd1eux.imava.ui.albums

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.hrshd1eux.imava.ui.MainViewModel.AlbumLayoutMode
import com.hrshd1eux.imava.ui.MainViewModel.AlbumSortOrder

class AlbumStateHolder(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
    private val legacyPrefs: SharedPreferences = context.getSharedPreferences("album_prefs", Context.MODE_PRIVATE)

    private val _albumSortOrder = MutableStateFlow(
        try {
            AlbumSortOrder.valueOf(prefs.getString("album_sort_order", AlbumSortOrder.NAME_ASC.name) ?: AlbumSortOrder.NAME_ASC.name)
        } catch (_: Exception) {
            AlbumSortOrder.NAME_ASC
        }
    )
    val albumSortOrder: StateFlow<AlbumSortOrder> = _albumSortOrder.asStateFlow()

    fun setAlbumSortOrder(order: AlbumSortOrder) {
        _albumSortOrder.value = order
        prefs.edit().putString("album_sort_order", order.name).apply()
    }

    private val _albumLayoutMode = MutableStateFlow(
        try {
            AlbumLayoutMode.valueOf(prefs.getString("album_layout_mode", AlbumLayoutMode.GRID_2.name) ?: AlbumLayoutMode.GRID_2.name)
        } catch (_: Exception) {
            AlbumLayoutMode.GRID_2
        }
    )
    val albumLayoutMode: StateFlow<AlbumLayoutMode> = _albumLayoutMode.asStateFlow()

    fun setAlbumLayoutMode(mode: AlbumLayoutMode) {
        _albumLayoutMode.value = mode
        prefs.edit().putString("album_layout_mode", mode.name).apply()
    }

    private val _customAlbumCovers = MutableStateFlow<Map<Long, Long>>(loadCustomAlbumCovers())
    val customAlbumCovers: StateFlow<Map<Long, Long>> = _customAlbumCovers.asStateFlow()

    private fun loadCustomAlbumCovers(): Map<Long, Long> {
        val map = mutableMapOf<Long, Long>()
        val allPrefs = prefs.all
        for ((k, v) in allPrefs) {
            if (k.startsWith("custom_cover_")) {
                val bucketId = k.removePrefix("custom_cover_").toLongOrNull()
                val mediaId = (v as? Long) ?: (v as? String)?.toLongOrNull()
                if (bucketId != null && mediaId != null) {
                    map[bucketId] = mediaId
                }
            }
        }
        return map
    }

    fun setCustomAlbumCover(bucketId: Long, mediaId: Long) {
        val newMap = _customAlbumCovers.value.toMutableMap()
        newMap[bucketId] = mediaId
        _customAlbumCovers.value = newMap
        prefs.edit().putLong("custom_cover_$bucketId", mediaId).apply()
    }

    private val _pinnedBucketIds = MutableStateFlow<Set<String>>(
        (prefs.getStringSet("pinned_buckets", null)
            ?: legacyPrefs.getStringSet("pinned_buckets", emptySet())
            ?: emptySet())
    )
    val pinnedBucketIds: StateFlow<Set<String>> = _pinnedBucketIds.asStateFlow()

    fun togglePinBucket(bucketId: Long) {
        val isPinned = _pinnedBucketIds.value.contains(bucketId.toString())
        val newSet = _pinnedBucketIds.value.toMutableSet().apply {
            if (isPinned) remove(bucketId.toString()) else add(bucketId.toString())
        }
        _pinnedBucketIds.value = newSet
        prefs.edit().putStringSet("pinned_buckets", newSet).apply()
        legacyPrefs.edit().putStringSet("pinned_buckets", newSet).apply()
    }

    private val _excludedBucketIds = MutableStateFlow<Set<String>>(
        (prefs.getStringSet("excluded_buckets", null)
            ?: legacyPrefs.getStringSet("excluded_buckets", emptySet())
            ?: emptySet())
    )
    val excludedBucketIds: StateFlow<Set<String>> = _excludedBucketIds.asStateFlow()

    fun excludeBucket(bucketId: Long) {
        val newSet = _excludedBucketIds.value.toMutableSet().apply { add(bucketId.toString()) }
        _excludedBucketIds.value = newSet
        prefs.edit().putStringSet("excluded_buckets", newSet).apply()
        legacyPrefs.edit().putStringSet("excluded_buckets", newSet).apply()
    }

    fun restoreExcludedBucket(bucketId: String) {
        val newSet = _excludedBucketIds.value.toMutableSet().apply { remove(bucketId) }
        _excludedBucketIds.value = newSet
        prefs.edit().putStringSet("excluded_buckets", newSet).apply()
        legacyPrefs.edit().putStringSet("excluded_buckets", newSet).apply()
    }

    private val _mergeSameNamedAlbums = MutableStateFlow(prefs.getBoolean("merge_same_named_albums", true))
    val mergeSameNamedAlbums: StateFlow<Boolean> = _mergeSameNamedAlbums.asStateFlow()

    fun setMergeSameNamedAlbums(enabled: Boolean) {
        prefs.edit().putBoolean("merge_same_named_albums", enabled).apply()
        _mergeSameNamedAlbums.value = enabled
    }
}
