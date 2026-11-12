package com.hrshd1eux.imava.ui

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrshd1eux.imava.core.util.SharingUtils
import com.hrshd1eux.imava.core.util.findActivity
import com.hrshd1eux.imava.data.media.BucketInfo
import com.hrshd1eux.imava.data.media.DeepScanResult
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.repository.MediaRepository
import com.hrshd1eux.imava.ui.selection.SelectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.hrshd1eux.imava.data.model.TimelineItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import com.hrshd1eux.imava.data.model.isVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import javax.inject.Inject

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import androidx.paging.filter
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.data.paging.MediaPagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

private data class PagingParams(
    val bucketId: Long?,
    val category: String?,
    val sortMode: TimelineSortMode,
    val favs: List<MediaItem>,
    val trash: List<MediaItem>,
    val vault: List<MediaItem>
)

data class BatchProgress(
    val title: String,
    val current: Int = 0,
    val total: Int = 0,
    val isIndeterminate: Boolean = false
)

enum class TimelineSortMode {
    DATE_GROUPED,
    FLAT_NEWEST_FIRST
}

enum class GridStyle {
    SQUARE,
    NATURAL
}

enum class MediaFilterType(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    SCREENSHOTS("Screenshots"),
    FAVORITES("Favorites"),
    GIFS("GIFs"),
    RAW("RAW")
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: android.app.Application,
    private val repository: MediaRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _selectedFilter = MutableStateFlow(MediaFilterType.ALL)
    val selectedFilter: StateFlow<MediaFilterType> = _selectedFilter.asStateFlow()

    private val _selectedCustomTag = MutableStateFlow<String?>(null)
    val selectedCustomTag: StateFlow<String?> = _selectedCustomTag.asStateFlow()

    fun setSelectedFilter(filter: MediaFilterType) {
        _selectedFilter.value = filter
    }

    fun setSelectedCustomTag(tag: String?) {
        _selectedCustomTag.value = tag
    }

    val selectionState = SelectionState()
    
    var editingMediaItem by mutableStateOf<MediaItem.Photo?>(null)

    suspend fun saveEditedPhoto(context: Context, originalItem: MediaItem.Photo, editedBitmap: Bitmap) = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val isPng = originalItem.mimeType.equals("image/png", ignoreCase = true) ||
                originalItem.path.endsWith(".png", ignoreCase = true)
            val isWebp = originalItem.mimeType.equals("image/webp", ignoreCase = true) ||
                originalItem.path.endsWith(".webp", ignoreCase = true)

            val (ext, mimeType, compressFormat) = when {
                isPng -> Triple("png", "image/png", Bitmap.CompressFormat.PNG)
                isWebp -> {
                    val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    Triple("webp", "image/webp", format)
                }
                else -> Triple("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG)
            }

            val filename = "Edited_${System.currentTimeMillis()}.$ext"
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Edited")
            }
            val targetUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (targetUri != null) {
                resolver.openOutputStream(targetUri)?.use { output ->
                    val quality = if (compressFormat == Bitmap.CompressFormat.PNG) 100 else 95
                    editedBitmap.compress(compressFormat, quality, output)
                }

                com.hrshd1eux.imava.core.util.PhotoEditorUtils.copyExifAttributes(
                    context,
                    originalItem.uri,
                    targetUri
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val refreshTrigger = MutableStateFlow(0L)

    val scrollToTopEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    fun triggerScrollToTop() {
        scrollToTopEvents.tryEmit(Unit)
    }

    private val vaultState = com.hrshd1eux.imava.ui.vault.VaultStateHolder()

    val vaultConfigVersion: StateFlow<Int> = vaultState.vaultConfigVersion
    val isVaultUnlocked: StateFlow<Boolean> = vaultState.isVaultUnlocked
    val isDecoyVault: Boolean get() = vaultState.isDecoyVault

    fun notifyVaultConfigChanged() = vaultState.notifyVaultConfigChanged()
    fun unlockVault() = vaultState.unlockVault()
    fun unlockDecoyVault() = vaultState.unlockDecoyVault()

    fun lockVault(context: Context) {
        if (currentCategoryName == "Hidden Vault") {
            currentCategoryName = null
            currentScreen = Screen.Albums
        }
        vaultState.lockVault(context)
    }

    fun onAppBackgrounded() {
        vaultState.onAppBackgrounded()
    }

    fun onAppForegrounded(context: Context) {
        vaultState.onAppForegrounded(context)
    }

    private var _appThemeState = mutableStateOf(prefs.getString("app_theme", "system") ?: "system")
    var appTheme: String
        get() = _appThemeState.value
        set(value) {
            _appThemeState.value = value
            prefs.edit().putString("app_theme", value).apply()
            savedStateHandle["app_theme"] = value
        }

    private val galleryPrefs = application.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)

    var isAutoDeepScanEnabled: Boolean
        get() = galleryPrefs.getBoolean("auto_deep_scan", true)
        set(value) {
            galleryPrefs.edit().putBoolean("auto_deep_scan", value).apply()
            if (value) {
                triggerAutoDeepScan(force = true)
            }
        }

    private val _isDeepScanning = MutableStateFlow(false)
    val isDeepScanning: StateFlow<Boolean> = _isDeepScanning.asStateFlow()

    private val _isInitialScanning = MutableStateFlow(true)
    val isInitialScanning: StateFlow<Boolean> = _isInitialScanning.asStateFlow()

    val isLibraryScanning: StateFlow<Boolean> = combine(
        _isDeepScanning,
        _isInitialScanning
    ) { deepScanning, initialScanning ->
        deepScanning || initialScanning
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _lastDeepScanCount = MutableStateFlow(
        galleryPrefs.getInt("last_deep_scan_count", 0)
    )
    val lastDeepScanCount: StateFlow<Int> = _lastDeepScanCount.asStateFlow()

    private val _lastDeepScanNewCount = MutableStateFlow(
        galleryPrefs.getInt("last_deep_scan_new_count", 0)
    )
    val lastDeepScanNewCount: StateFlow<Int> = _lastDeepScanNewCount.asStateFlow()

    private val _deepScanCompletionEvent = MutableSharedFlow<DeepScanResult>()
    val deepScanCompletionEvent: SharedFlow<DeepScanResult> = _deepScanCompletionEvent.asSharedFlow()

    private val _batchOperationProgress = MutableStateFlow<BatchProgress?>(null)
    val batchOperationProgress: StateFlow<BatchProgress?> = _batchOperationProgress.asStateFlow()

    fun updateBatchProgress(title: String, current: Int, total: Int, isIndeterminate: Boolean = false) {
        _batchOperationProgress.value = BatchProgress(title, current, total, isIndeterminate)
    }

    fun clearBatchProgress() {
        _batchOperationProgress.value = null
    }

    private var lastAutoScanTimestamp = 0L

    fun triggerAutoDeepScan(force: Boolean = false, isManual: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastAutoScanTimestamp < 3_000L)) {
            return
        }
        if (_isDeepScanning.value) return
        if (!force && !isAutoDeepScanEnabled) return

        lastAutoScanTimestamp = now
        viewModelScope.launch(Dispatchers.IO) {
            _isDeepScanning.value = true
            try {
                val result = repository.scanSecondaryMediaDirectories()
                _lastDeepScanCount.value = result.totalCount
                _lastDeepScanNewCount.value = result.newCount
                galleryPrefs.edit()
                    .putInt("last_deep_scan_count", result.totalCount)
                    .putInt("last_deep_scan_new_count", result.newCount)
                    .putLong("last_deep_scan_timestamp", System.currentTimeMillis())
                    .apply()
                refreshAll()
                triggerAutoScrub()
                if (isManual) {
                    _deepScanCompletionEvent.emit(result)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isDeepScanning.value = false
            }
        }
    }

    suspend fun scanSecondaryMediaDirectories(): DeepScanResult {
        val result = repository.scanSecondaryMediaDirectories()
        _lastDeepScanCount.value = result.totalCount
        _lastDeepScanNewCount.value = result.newCount
        galleryPrefs.edit()
            .putInt("last_deep_scan_count", result.totalCount)
            .putInt("last_deep_scan_new_count", result.newCount)
            .putLong("last_deep_scan_timestamp", System.currentTimeMillis())
            .apply()
        return result
    }

    private val _autoScrubBucketIds = MutableStateFlow<Set<String>>(
        galleryPrefs.getStringSet("auto_scrub_bucket_ids", emptySet()) ?: emptySet()
    )
    val autoScrubBucketIds: StateFlow<Set<String>> = _autoScrubBucketIds.asStateFlow()

    fun isAutoScrubEnabled(bucketId: Long): Boolean {
        return _autoScrubBucketIds.value.contains(bucketId.toString())
    }

    fun toggleAutoScrubBucket(bucketId: Long): Boolean {
        val current = _autoScrubBucketIds.value.toMutableSet()
        val key = bucketId.toString()
        val newState = if (current.contains(key)) {
            current.remove(key)
            false
        } else {
            current.add(key)
            true
        }
        _autoScrubBucketIds.value = current
        galleryPrefs.edit().putStringSet("auto_scrub_bucket_ids", current).apply()
        if (newState) {
            triggerAutoScrub()
        }
        return newState
    }

    private val _isSecureShredEnabled = MutableStateFlow(
        com.hrshd1eux.imava.core.util.FileShredderUtil.isSecureShredEnabled(application)
    )
    val isSecureShredEnabled: StateFlow<Boolean> = _isSecureShredEnabled.asStateFlow()

    fun setSecureShredEnabled(enabled: Boolean) {
        com.hrshd1eux.imava.core.util.FileShredderUtil.setSecureShredEnabled(application, enabled)
        _isSecureShredEnabled.value = enabled
    }

    private var lastAutoScrubTimestamp = 0L

    fun triggerAutoScrub() {
        val now = System.currentTimeMillis()
        if (now - lastAutoScrubTimestamp < 10_000L) return
        val targetBuckets = _autoScrubBucketIds.value
        if (targetBuckets.isEmpty()) return

        lastAutoScrubTimestamp = now
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allItems = visibleMediaItems.value
                val count = com.hrshd1eux.imava.core.util.AutoScrubManager.executeAutoScrub(application, allItems)
                if (count > 0) {
                    refreshAll()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var _currentScreenState = mutableStateOf(
        savedStateHandle.get<Screen>("current_screen") ?: run {
            val galleryPrefs = application.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
            val defaultScreen = galleryPrefs.getString("default_start_screen", "timeline")
            when (defaultScreen) {
                "albums" -> Screen.Albums
                "last_active", "last_used" -> {
                    val last = galleryPrefs.getString("last_active_screen", "photos")
                    when (last) {
                        "albums" -> Screen.Albums
                        "settings" -> Screen.Settings
                        else -> Screen.Photos
                    }
                }
                else -> Screen.Photos
            }
        }
    )
    var currentScreen: Screen
        get() = _currentScreenState.value
        set(value) {
            _currentScreenState.value = value
            savedStateHandle["current_screen"] = value
            val galleryPrefs = application.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
            val screenName = when (value) {
                Screen.Albums -> "albums"
                Screen.Settings -> "settings"
                else -> "photos"
            }
            galleryPrefs.edit().putString("last_active_screen", screenName).apply()
        }

    private var _activeMediaIdState = mutableStateOf(savedStateHandle.get<Long>("active_media_id"))
    var activeMediaId: Long?
        get() = _activeMediaIdState.value
        set(value) {
            _activeMediaIdState.value = value
            savedStateHandle["active_media_id"] = value
        }

    private var _activeMediaItemState = mutableStateOf<MediaItem?>(null)
    var activeMediaItem: MediaItem?
        get() = _activeMediaItemState.value
        set(value) {
            _activeMediaItemState.value = value
            _activeMediaIdState.value = value?.id
            savedStateHandle["active_media_id"] = value?.id
        }

    var isExternalViewIntent by mutableStateOf(false)

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun openExternalMedia(uri: Uri, mimeType: String?) {
        isExternalViewIntent = true
        val appContext = application.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Try resolving via ContentUris if it's an existing finalized MediaStore item
                val parsedId = try {
                    android.content.ContentUris.parseId(uri)
                } catch (_: Exception) {
                    null
                }
                if (parsedId != null) {
                    val matching = repository.getMediaByIds(setOf(parsedId)).firstOrNull()
                    if (matching != null &&
                        !matching.path.contains("/.pending-") &&
                        !matching.path.substringAfterLast('/').startsWith(".pending", ignoreCase = true)
                    ) {
                        withContext(Dispatchers.Main) {
                            activeMediaItem = matching
                        }
                        return@launch
                    }
                }

                // 2. Query ContentResolver for display name, size, MIME type and _data column
                var resolvedName = uri.lastPathSegment ?: "Media_${System.currentTimeMillis()}"
                var resolvedSize = 0L
                var resolvedPath = ""
                var isPendingItem = false

                try {
                    appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameCol = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeCol = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        val dataCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                        val pendingCol = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.IS_PENDING)
                        } else -1

                        if (cursor.moveToFirst()) {
                            if (nameCol != -1) {
                                resolvedName = cursor.getString(nameCol) ?: resolvedName
                            }
                            if (sizeCol != -1) {
                                resolvedSize = cursor.getLong(sizeCol)
                            }
                            if (dataCol != -1) {
                                resolvedPath = cursor.getString(dataCol) ?: ""
                            }
                            if (pendingCol != -1) {
                                isPendingItem = (cursor.getInt(pendingCol) == 1)
                            }
                        }
                    }
                } catch (_: Exception) {}

                if (resolvedPath.isBlank() && uri.scheme == "file") {
                    resolvedPath = uri.path ?: ""
                }

                if (resolvedName.startsWith(".pending", ignoreCase = true) || resolvedPath.contains("/.pending-")) {
                    isPendingItem = true
                }

                val initialPath = resolvedPath.ifBlank {
                    if (resolvedName.isNotBlank() && !resolvedName.all { it.isDigit() }) resolvedName else uri.toString()
                }

                val resolvedMime = mimeType
                    ?: appContext.contentResolver.getType(uri)
                    ?: if (resolvedName.endsWith(".mp4", ignoreCase = true) || resolvedName.endsWith(".mkv", ignoreCase = true) || resolvedName.endsWith(".mov", ignoreCase = true)) "video/mp4" else "image/jpeg"

                val isVideo = resolvedMime.startsWith("video/", ignoreCase = true)
                val id = parsedId ?: kotlin.math.abs(uri.toString().hashCode().toLong()).coerceAtLeast(1L)
                val file = if (resolvedPath.isNotBlank()) java.io.File(resolvedPath) else null
                val dateTaken = if (file != null && file.exists()) file.lastModified() else System.currentTimeMillis()
                if (resolvedSize == 0L && file != null && file.exists()) {
                    resolvedSize = file.length()
                }

                val initialItem = if (isVideo) {
                    MediaItem.Video(
                        id = id,
                        uri = uri,
                        path = initialPath,
                        mimeType = resolvedMime,
                        dateTaken = dateTaken,
                        size = resolvedSize,
                        width = 0,
                        height = 0,
                        durationMs = 0L,
                        bucketId = file?.parentFile?.absolutePath?.lowercase(java.util.Locale.ROOT)?.hashCode()?.toLong() ?: 0L,
                        bucketName = file?.parentFile?.name ?: "",
                        isPending = isPendingItem
                    )
                } else {
                    MediaItem.Photo(
                        id = id,
                        uri = uri,
                        path = initialPath,
                        mimeType = resolvedMime,
                        dateTaken = dateTaken,
                        size = resolvedSize,
                        width = 0,
                        height = 0,
                        bucketId = file?.parentFile?.absolutePath?.lowercase(java.util.Locale.ROOT)?.hashCode()?.toLong() ?: 0L,
                        bucketName = file?.parentFile?.name ?: "",
                        isPending = isPendingItem
                    )
                }

                withContext(Dispatchers.Main) {
                    activeMediaItem = initialItem
                }

                // 3. If item is pending or 0 bytes (freshly captured photo from Camera app),
                // actively poll until the camera app finishes writing, then finalize and reload Coil cache!
                if (isPendingItem || resolvedSize == 0L) {
                    var pollAttempts = 0
                    while (pollAttempts < 35) { // Poll up to ~5.2 seconds
                        kotlinx.coroutines.delay(150)
                        pollAttempts++

                        var stillPending = false
                        var freshName = resolvedName
                        var freshPath = resolvedPath
                        var freshSize = 0L
                        var freshWidth = 0
                        var freshHeight = 0

                        try {
                            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val pCol = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                        cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.IS_PENDING)
                                    } else -1
                                    if (pCol != -1) {
                                        stillPending = (cursor.getInt(pCol) == 1)
                                    }
                                    val nCol = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                    if (nCol != -1) freshName = cursor.getString(nCol) ?: freshName
                                    val dCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                                    if (dCol != -1) freshPath = cursor.getString(dCol) ?: freshPath
                                    val sCol = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                    if (sCol != -1) freshSize = cursor.getLong(sCol)
                                    val wCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.WIDTH)
                                    if (wCol != -1) freshWidth = cursor.getInt(wCol)
                                    val hCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.HEIGHT)
                                    if (hCol != -1) freshHeight = cursor.getInt(hCol)
                                }
                            }
                        } catch (_: Exception) {}

                        val checkFile = if (freshPath.isNotBlank()) java.io.File(freshPath) else null
                        if (freshSize == 0L && checkFile != null && checkFile.exists()) {
                            freshSize = checkFile.length()
                        }

                        val isReadable = try {
                            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                                stream.available() > 0 || freshSize > 0L
                            } ?: false
                        } catch (_: Exception) { false }

                        if (!stillPending &&
                            !freshName.startsWith(".pending", ignoreCase = true) &&
                            !freshPath.contains("/.pending-") &&
                            (freshSize > 0L || isReadable)
                        ) {
                            // Camera finalized writing! Evict Coil memory and disk caches so real photo renders
                            try {
                                val imageLoader = coil.Coil.imageLoader(appContext)
                                imageLoader.memoryCache?.remove(coil.memory.MemoryCache.Key(uri.toString()))
                                imageLoader.diskCache?.remove(uri.toString())
                            } catch (_: Exception) {}

                            if (freshPath.isNotBlank()) {
                                android.media.MediaScannerConnection.scanFile(appContext, arrayOf(freshPath), null, null)
                            }

                            val finalizedItem = if (isVideo) {
                                MediaItem.Video(
                                    id = id,
                                    uri = uri,
                                    path = freshPath.ifBlank { uri.toString() },
                                    mimeType = resolvedMime,
                                    dateTaken = if (checkFile != null && checkFile.exists()) checkFile.lastModified() else System.currentTimeMillis(),
                                    size = freshSize,
                                    width = freshWidth,
                                    height = freshHeight,
                                    durationMs = 0L,
                                    bucketId = checkFile?.parentFile?.absolutePath?.lowercase(java.util.Locale.ROOT)?.hashCode()?.toLong() ?: 0L,
                                    bucketName = checkFile?.parentFile?.name ?: ""
                                )
                            } else {
                                MediaItem.Photo(
                                    id = id,
                                    uri = uri,
                                    path = freshPath.ifBlank { uri.toString() },
                                    mimeType = resolvedMime,
                                    dateTaken = if (checkFile != null && checkFile.exists()) checkFile.lastModified() else System.currentTimeMillis(),
                                    size = freshSize,
                                    width = freshWidth,
                                    height = freshHeight,
                                    bucketId = checkFile?.parentFile?.absolutePath?.lowercase(java.util.Locale.ROOT)?.hashCode()?.toLong() ?: 0L,
                                    bucketName = checkFile?.parentFile?.name ?: ""
                                )
                            }

                            withContext(Dispatchers.Main) {
                                activeMediaItem = finalizedItem
                                val current = _mediaItems.value
                                if (current.none { it.id == finalizedItem.id }) {
                                    _mediaItems.value = if (sortOrder == SortOrder.OLDEST_FIRST) current + finalizedItem else listOf(finalizedItem) + current
                                } else {
                                    _mediaItems.value = current.map { if (it.id == finalizedItem.id) finalizedItem else it }
                                }
                            }
                            refreshAll()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun openExternalMedia(context: Context, uri: Uri, mimeType: String?) {
        openExternalMedia(uri, mimeType)
    }
    
    private var _currentBucketIdState = mutableStateOf(savedStateHandle.get<Long>("current_bucket_id"))
    var currentBucketId: Long?
        get() = _currentBucketIdState.value
        set(value) {
            _currentBucketIdState.value = value
            savedStateHandle["current_bucket_id"] = value
            val targetOrder = if (value != null) {
                val albumOrder = prefs.getString("album_sort_order_$value", null)
                if (albumOrder != null) {
                    try { SortOrder.valueOf(albumOrder) } catch (_: Exception) { null }
                } else null
            } else null
            val globalOrder = try {
                SortOrder.valueOf(prefs.getString("sort_order", SortOrder.NEWEST_FIRST.name) ?: SortOrder.NEWEST_FIRST.name)
            } catch (_: Exception) { SortOrder.NEWEST_FIRST }
            _sortOrderState.value = targetOrder ?: globalOrder
        }

    var currentBucketIds: List<Long> = emptyList()

    private var _currentBucketNameState = mutableStateOf(savedStateHandle.get<String>("current_bucket_name"))
    var currentBucketName: String?
        get() = _currentBucketNameState.value
        set(value) {
            _currentBucketNameState.value = value
            savedStateHandle["current_bucket_name"] = value
        }

    private val _currentCategoryName = MutableStateFlow<String?>(savedStateHandle.get<String>("current_category_name")?.takeIf { it != "Hidden Vault" })
    val currentCategoryNameFlow: StateFlow<String?> = _currentCategoryName.asStateFlow()
    var currentCategoryName: String?
        get() = _currentCategoryName.value
        set(value) {
            _currentCategoryName.value = value
            savedStateHandle["current_category_name"] = value
            if (value == "Trash") {
                _selectedFilter.value = MediaFilterType.ALL
                _selectedCustomTag.value = null
            }
        }

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    val hasAppInitiallyLoaded = MutableStateFlow(false)

    private val _buckets = MutableStateFlow<List<BucketInfo>>(emptyList())
    val buckets: StateFlow<List<BucketInfo>> = _buckets.asStateFlow()

    private val _excludedBucketIds = MutableStateFlow<Set<String>>(
        (prefs.getStringSet("excluded_buckets", null)
            ?: application.getSharedPreferences("album_prefs", Context.MODE_PRIVATE).getStringSet("excluded_buckets", emptySet())
            ?: emptySet())
    )
    val excludedBucketIds: StateFlow<Set<String>> = _excludedBucketIds.asStateFlow()

    enum class AlbumSortOrder {
        NAME_ASC,
        COUNT_DESC,
        RECENT
    }

    enum class AlbumLayoutMode {
        LIST,
        GRID_2, // Large
        GRID_3, // Medium
        GRID_4  // Small
    }

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

    private fun localizedString(context: Context?, resId: Int, vararg args: Any): String {
        return com.hrshd1eux.imava.core.util.LocaleHelper.getString(
            context = context ?: application,
            resId = resId,
            fallback = "",
            formatArgs = args
        )
    }

    fun safeToast(context: Context?, message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
        if (context == null) return
        try {
            android.widget.Toast.makeText(context, message, duration)?.show()
        } catch (_: Exception) {}
    }

    private val _lastDeletedMediaId = MutableStateFlow<Long?>(null)
    val lastDeletedMediaId: StateFlow<Long?> = _lastDeletedMediaId.asStateFlow()

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
        refreshAll()
    }

    private val _pinnedBucketIds = MutableStateFlow<Set<String>>(
        (prefs.getStringSet("pinned_buckets", null)
            ?: application.getSharedPreferences("album_prefs", Context.MODE_PRIVATE).getStringSet("pinned_buckets", emptySet())
            ?: emptySet())
    )
    val pinnedBucketIds: StateFlow<Set<String>> = _pinnedBucketIds.asStateFlow()

    private val _mergeSameNamedAlbums = MutableStateFlow(galleryPrefs.getBoolean("merge_same_named_albums", true))
    val mergeSameNamedAlbums: StateFlow<Boolean> = _mergeSameNamedAlbums.asStateFlow()

    fun setMergeSameNamedAlbums(enabled: Boolean) {
        galleryPrefs.edit().putBoolean("merge_same_named_albums", enabled).apply()
        _mergeSameNamedAlbums.value = enabled
    }

    val visibleBuckets: StateFlow<List<BucketInfo>> = combine(
        _buckets, _excludedBucketIds, _albumSortOrder, _mergeSameNamedAlbums, mediaItems
    ) { list: List<BucketInfo>, excluded: Set<String>, sortOrder: AlbumSortOrder, mergeEnabled: Boolean, rawMedia: List<MediaItem> ->
        val rawList = if (list.isNotEmpty()) {
            list
        } else if (rawMedia.isNotEmpty()) {
            // Self-healing fallback: derive albums directly from in-memory media items if MediaStore query fails or is delayed
            rawMedia.groupBy { it.bucketId }.mapNotNull { (bId, items) ->
                if (bId == 0L || items.isEmpty()) null
                else {
                    val first = items.first()
                    val bName = first.bucketName.ifBlank {
                        val parent = first.path.substringBeforeLast('/', "")
                        if (parent.isNotBlank()) parent.substringAfterLast('/') else "Unknown"
                    }
                    val newest = items.maxByOrNull { it.dateTaken } ?: first
                    BucketInfo(
                        id = bId,
                        name = bName,
                        count = items.size,
                        path = first.path.substringBeforeLast('/', ""),
                        mergedBucketIds = listOf(bId),
                        paths = items.map { it.path.substringBeforeLast('/', "") }.filter { it.isNotBlank() }.distinct(),
                        coverUri = newest.uri,
                        latestDateTaken = newest.dateTaken
                    )
                }
            }
        } else {
            emptyList()
        }

        val reconciledList = if (rawMedia.isNotEmpty()) {
            val createdAlbumNames = application.getSharedPreferences("user_albums", Context.MODE_PRIVATE)
                .getStringSet("created_albums", emptySet()) ?: emptySet()
            
            rawList.mapNotNull { bucket ->
                val matchingMedia = rawMedia.filter { it.bucketId == bucket.id || it.bucketId in bucket.mergedBucketIds }
                if (matchingMedia.isNotEmpty()) {
                    val newest = matchingMedia.maxByOrNull { it.dateTaken } ?: matchingMedia.first()
                    bucket.copy(
                        count = matchingMedia.size,
                        coverUri = newest.uri,
                        latestDateTaken = newest.dateTaken
                    )
                } else {
                    // Check if this is an empty user-created album whose directory still exists on disk
                    val isCreatedAlbum = createdAlbumNames.any { it.equals(bucket.name, ignoreCase = true) }
                    val dirExists = !bucket.path.isNullOrBlank() && java.io.File(bucket.path).exists()
                    if (isCreatedAlbum && dirExists) {
                        bucket.copy(count = 0, coverUri = null, latestDateTaken = 0L)
                    } else {
                        // Ghost bucket whose media has been deleted or directory no longer exists -> filter out!
                        null
                    }
                }
            }
        } else {
            rawList
        }

        val filtered = reconciledList.filter { !excluded.contains(it.id.toString()) }
        val processed = if (mergeEnabled) {
            filtered.groupBy { it.name.trim().lowercase(java.util.Locale.ROOT) }.map { (_, group) ->
                val primary = group.first()
                val totalCount = group.sumOf { it.count }
                val mergedIds = group.flatMap { it.mergedBucketIds }.distinct()
                val mergedPaths = group.flatMap { it.paths }.distinct()
                val bestCoverBucket = group.filter { it.count > 0 && it.coverUri != null }.maxByOrNull { it.latestDateTaken }
                primary.copy(
                    count = totalCount,
                    mergedBucketIds = mergedIds,
                    paths = mergedPaths,
                    coverUri = if (totalCount > 0) bestCoverBucket?.coverUri else null,
                    latestDateTaken = if (totalCount > 0) (bestCoverBucket?.latestDateTaken ?: 0L) else 0L
                )
            }
        } else {
            filtered
        }
        when (sortOrder) {
            AlbumSortOrder.NAME_ASC -> processed.sortedBy { it.name.lowercase() }
            AlbumSortOrder.COUNT_DESC -> processed.sortedByDescending { it.count }
            AlbumSortOrder.RECENT -> processed.sortedByDescending { it.latestDateTaken }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun excludeBucket(bucketId: Long) {
        val newSet = _excludedBucketIds.value.toMutableSet().apply { add(bucketId.toString()) }
        _excludedBucketIds.value = newSet
        prefs.edit().putStringSet("excluded_buckets", newSet).apply()
        application.getSharedPreferences("album_prefs", Context.MODE_PRIVATE).edit().putStringSet("excluded_buckets", newSet).apply()
        refreshAll()
    }

    fun restoreExcludedBucket(bucketId: String) {
        val newSet = _excludedBucketIds.value.toMutableSet().apply { remove(bucketId) }
        _excludedBucketIds.value = newSet
        prefs.edit().putStringSet("excluded_buckets", newSet).apply()
        application.getSharedPreferences("album_prefs", Context.MODE_PRIVATE).edit().putStringSet("excluded_buckets", newSet).apply()
        refreshAll()
    }

    fun togglePinBucket(bucketId: Long) {
        val isPinned = _pinnedBucketIds.value.contains(bucketId.toString())
        val newSet = _pinnedBucketIds.value.toMutableSet().apply {
            if (isPinned) remove(bucketId.toString()) else add(bucketId.toString())
        }
        _pinnedBucketIds.value = newSet
        prefs.edit().putStringSet("pinned_buckets", newSet).apply()
        application.getSharedPreferences("album_prefs", Context.MODE_PRIVATE).edit().putStringSet("pinned_buckets", newSet).apply()
    }

    private val _favorites = MutableStateFlow<List<MediaItem>>(emptyList())
    val favorites: StateFlow<List<MediaItem>> = _favorites.asStateFlow()

    private val _trashed = MutableStateFlow<List<MediaItem>>(emptyList())
    val trashed: StateFlow<List<MediaItem>> = _trashed.asStateFlow()

    private val _hidden = MutableStateFlow<List<MediaItem>>(emptyList())
    val hidden: StateFlow<List<MediaItem>> = _hidden.asStateFlow()

    data class StorageStats(
        val photosCount: Int = 0,
        val videosCount: Int = 0,
        val vaultCount: Int = 0,
        val trashCount: Int = 0,
        val photosBytes: Long = 0L,
        val videosBytes: Long = 0L,
        val vaultBytes: Long = 0L,
        val trashBytes: Long = 0L
    ) {
        val totalBytes: Long get() = photosBytes + videosBytes + vaultBytes + trashBytes
        val formattedTotal: String get() = com.hrshd1eux.imava.core.util.FormatUtils.formatFileSize(totalBytes)
        val formattedPhotos: String get() = com.hrshd1eux.imava.core.util.FormatUtils.formatFileSize(photosBytes)
        val formattedVideos: String get() = com.hrshd1eux.imava.core.util.FormatUtils.formatFileSize(videosBytes)
        val formattedVault: String get() = com.hrshd1eux.imava.core.util.FormatUtils.formatFileSize(vaultBytes)
        val formattedTrash: String get() = com.hrshd1eux.imava.core.util.FormatUtils.formatFileSize(trashBytes)
    }

    val storageBreakdown: StateFlow<StorageStats> = combine(
        mediaItems, hidden, trashed, _excludedBucketIds
    ) { raw, vault, trash, excluded ->
        var photosBytes = 0L
        var videosBytes = 0L
        var photosCount = 0
        var videosCount = 0
        for (item in raw) {
            if (!excluded.contains(item.bucketId.toString())) {
                if (item.isVideo) {
                    videosBytes += item.size
                    videosCount++
                } else {
                    photosBytes += item.size
                    photosCount++
                }
            }
        }
        val vaultBytes = vault.sumOf { it.size }
        val trashBytes = trash.sumOf { it.size }
        StorageStats(
            photosCount = photosCount,
            videosCount = videosCount,
            vaultCount = vault.size,
            trashCount = trash.size,
            photosBytes = photosBytes,
            videosBytes = videosBytes,
            vaultBytes = vaultBytes,
            trashBytes = trashBytes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StorageStats())

    val videosCount: StateFlow<Int> = combine(
        mediaItems,
        _excludedBucketIds,
        com.hrshd1eux.imava.core.util.AppLockManager.lockStateVersion
    ) { list, excluded, _ ->
        list.count {
            it is MediaItem.Video && (
                currentBucketId != null || (
                    !excluded.contains(it.bucketId.toString()) &&
                    !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, it.bucketId, it.bucketName)
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    data class SmartAlbumCounts(
        val hugeVideos: Int = 0,
        val burstShots: Int = 0,
        val thisYear: Int = 0,
        val screenshots: Int = 0,
        val tagged: Int = 0
    )

    val smartAlbumCounts: StateFlow<SmartAlbumCounts> = combine(
        mediaItems,
        _excludedBucketIds,
        com.hrshd1eux.imava.core.util.AppLockManager.lockStateVersion
    ) { list, excluded, _ ->
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, currentYear)
            set(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfYearMs = cal.timeInMillis

        val accessibleItems = list.filter {
            !it.isTrashed && !it.isHidden &&
            !excluded.contains(it.bucketId.toString()) &&
            !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, it.bucketId, it.bucketName)
        }

        var hugeVideos = 0
        var thisYear = 0
        var screenshots = 0
        var tagged = 0
        val photos = mutableListOf<MediaItem.Photo>()

        for (item in accessibleItems) {
            if (item is MediaItem.Video && item.size >= 500L * 1024 * 1024) {
                hugeVideos++
            }
            if (item is MediaItem.Photo) {
                photos.add(item)
            }
            if (item.dateTaken >= startOfYearMs) {
                thisYear++
            }
            if (item.bucketName.contains("screenshot", ignoreCase = true) ||
                item.path.contains("screenshot", ignoreCase = true)) {
                screenshots++
            }
            if (item.tags.isNotEmpty()) {
                tagged++
            }
        }

        val burstCount = com.hrshd1eux.imava.core.util.StorageDoctorUtils.findBurstItemIds(photos).size

        SmartAlbumCounts(
            hugeVideos = hugeVideos,
            burstShots = burstCount,
            thisYear = thisYear,
            screenshots = screenshots,
            tagged = tagged
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmartAlbumCounts())

    val visibleMediaItems: StateFlow<List<MediaItem>> = combine(
        combine(mediaItems, favorites) { m, f -> Pair(m, f) },
        combine(trashed, hidden) { t, h -> Pair(t, h) },
        _currentCategoryName,
        _excludedBucketIds,
        com.hrshd1eux.imava.core.util.AppLockManager.lockStateVersion
    ) { (raw, favs), (trash, vault), category, excluded, _ ->
        val list = when (category) {
            "Favorites" -> favs.filter {
                if (currentBucketIds.isNotEmpty()) it.bucketId in currentBucketIds
                else if (currentBucketId != null) it.bucketId == currentBucketId
                else !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, it.bucketId, it.bucketName)
            }
            "Trash" -> if (com.hrshd1eux.imava.core.util.AppLockManager.isTrashLocked(application)) emptyList() else trash
            "Hidden Vault" -> if (isDecoyVault) emptyList() else vault
            "Videos" -> {
                val trashedIds = trash.map { it.id }.toSet()
                val trashedPaths = trash.filter {
                    val f = java.io.File(it.path)
                    !f.exists() || f.name.startsWith(".trashed")
                }.map { it.path }.toSet()
                val hiddenIds = vault.map { it.id }.toSet()
                val hiddenPaths = vault.map { it.path }.toSet()
                val videos = raw.filterIsInstance<MediaItem.Video>().filter { item ->
                    !item.isTrashed && !item.isHidden &&
                    item.id !in trashedIds && item.id !in hiddenIds &&
                    item.path !in trashedPaths && item.path !in hiddenPaths
                }
                if (currentBucketId != null) {
                    if (com.hrshd1eux.imava.core.util.AppLockManager.isAlbumLocked(application, currentBucketId!!, currentBucketName)) emptyList()
                    else if (currentBucketIds.isNotEmpty()) videos.filter { it.bucketId in currentBucketIds }
                    else videos
                } else videos.filter { item ->
                    !excluded.contains(item.bucketId.toString()) &&
                    !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, item.bucketId, item.bucketName)
                }
            }
            else -> {
                val trashedIds = trash.map { it.id }.toSet()
                val trashedPaths = trash.filter {
                    val f = java.io.File(it.path)
                    !f.exists() || f.name.startsWith(".trashed")
                }.map { it.path }.toSet()
                val hiddenIds = vault.map { it.id }.toSet()
                val hiddenPaths = vault.map { it.path }.toSet()
                val base = raw.filter { item ->
                    !item.isTrashed && !item.isHidden &&
                    item.id !in trashedIds && item.id !in hiddenIds &&
                    item.path !in trashedPaths && item.path !in hiddenPaths
                }
                val scopedBase = if (currentBucketId != null) {
                    if (com.hrshd1eux.imava.core.util.AppLockManager.isAlbumLocked(application, currentBucketId!!, currentBucketName)) emptyList()
                    else if (currentBucketIds.isNotEmpty()) base.filter { it.bucketId in currentBucketIds }
                    else base
                } else base.filter {
                    !excluded.contains(it.bucketId.toString()) &&
                    !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, it.bucketId, it.bucketName)
                }

                when (category) {
                    "Smart:HugeVideos" -> scopedBase.filter { it is MediaItem.Video && it.size >= 500L * 1024 * 1024 }
                    "Smart:BurstShots" -> {
                        val photos = scopedBase.filterIsInstance<MediaItem.Photo>()
                        val burstIds = com.hrshd1eux.imava.core.util.StorageDoctorUtils.findBurstItemIds(photos)
                        photos.filter { it.id in burstIds }
                    }
                    "Smart:ThisYear" -> {
                        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                        val cal = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.YEAR, currentYear)
                            set(java.util.Calendar.DAY_OF_YEAR, 1)
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        val startOfYearMs = cal.timeInMillis
                        scopedBase.filter { it.dateTaken >= startOfYearMs }
                    }
                    "Smart:Screenshots" -> scopedBase.filter {
                        it.bucketName.contains("screenshot", ignoreCase = true) ||
                        it.path.contains("screenshot", ignoreCase = true)
                    }
                    "Smart:Tagged" -> scopedBase.filter { it.tags.isNotEmpty() }
                    else -> scopedBase
                }
            }
        }
        if (category == "Trash") {
            if (sortOrder == SortOrder.OLDEST_FIRST) {
                list.sortedWith(compareBy<MediaItem> { if (it.trashTime > 0L) it.trashTime else it.dateTaken }.thenBy { it.id })
            } else {
                list.sortedWith(compareByDescending<MediaItem> { if (it.trashTime > 0L) it.trashTime else it.dateTaken }.thenByDescending { it.id })
            }
        } else {
            // Raw list from getMediaFlow() / Paging is already sorted in the requested sortOrder from MediaStore.
            // Avoid redundant O(N log N) Kotlin re-sorting on every StateFlow emission.
            list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var activeMemoryStory by mutableStateOf<com.hrshd1eux.imava.ui.timeline.MemoryStory?>(null)

    private val _memoriesDismissedTimestamp = MutableStateFlow(prefs.getLong("memories_dismissed_at", 0L))
    val memoriesDismissedTimestamp: StateFlow<Long> = _memoriesDismissedTimestamp.asStateFlow()

    fun dismissMemoriesFor24Hours() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong("memories_dismissed_at", now).apply()
        _memoriesDismissedTimestamp.value = now
    }

    val throwbackMemories: StateFlow<List<com.hrshd1eux.imava.ui.timeline.MemoryStory>> = combine(
        visibleMediaItems,
        _memoriesDismissedTimestamp,
        refreshTrigger
    ) { items, dismissedTimestamp, _ ->
        val now = System.currentTimeMillis()
        if (dismissedTimestamp > 0L && (now - dismissedTimestamp) < 24 * 60 * 60 * 1000L) {
            emptyList()
        } else {
            val localizedCtx = com.hrshd1eux.imava.core.util.LocaleHelper.localizedContext(application)
            com.hrshd1eux.imava.ui.timeline.MemoryStoryCalculator.generateStories(items, localizedCtx)
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDistinctTags: StateFlow<List<String>> = visibleMediaItems
        .map { items ->
            items.flatMap { it.tags }
                .map { it.trim().removePrefix("#").lowercase() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _customViewerMediaItems = MutableStateFlow<List<MediaItem>?>(null)
    val customViewerMediaItems: StateFlow<List<MediaItem>?> = _customViewerMediaItems.asStateFlow()

    fun setCustomViewerMediaItems(items: List<MediaItem>?) {
        _customViewerMediaItems.value = items
    }

    private fun computeViewerItems(
        visible: List<MediaItem>,
        customTag: String?,
        filter: MediaFilterType,
        customList: List<MediaItem>?
    ): List<MediaItem> {
        if (customList != null) return customList

        var result = visible
        if (!customTag.isNullOrBlank()) {
            val cleanTag = customTag.trim().removePrefix("#").removePrefix("+").trim()
            result = result.filter { item ->
                item.tags.any { t ->
                    val cleanT = t.trim().removePrefix("#").removePrefix("+").trim()
                    cleanT.equals(cleanTag, ignoreCase = true)
                }
            }
        }
        if (filter != MediaFilterType.ALL) {
            result = result.filter { matchesFilter(it, filter) }
        }
        return result
    }

    val viewerMediaItems: StateFlow<List<MediaItem>> = combine(
        visibleMediaItems,
        _selectedCustomTag,
        _selectedFilter,
        _customViewerMediaItems
    ) { visible, customTag, filter, customList ->
        computeViewerItems(visible, customTag, filter, customList)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _advancedFilterState = MutableStateFlow(com.hrshd1eux.imava.ui.search.AdvancedFilterState())
    val advancedFilterState: StateFlow<com.hrshd1eux.imava.ui.search.AdvancedFilterState> = _advancedFilterState.asStateFlow()

    fun setAdvancedFilterState(filter: com.hrshd1eux.imava.ui.search.AdvancedFilterState) {
        _advancedFilterState.value = filter
    }

    fun clearAdvancedFilter() {
        _advancedFilterState.value = com.hrshd1eux.imava.ui.search.AdvancedFilterState()
    }

    private val _ocrDownloadProgress = MutableStateFlow<Float?>(null)
    val ocrDownloadProgress: StateFlow<Float?> = _ocrDownloadProgress.asStateFlow()

    fun isOcrModelReady(context: Context): Boolean {
        return com.hrshd1eux.imava.core.ocr.OcrModelManager.isModelReady(context)
    }

    suspend fun downloadOcrModel(context: Context): Result<java.io.File> {
        _ocrDownloadProgress.value = 0f
        return try {
            val result = com.hrshd1eux.imava.core.ocr.OcrModelManager.downloadModel(context) { progress ->
                _ocrDownloadProgress.value = progress
            }
            result
        } finally {
            _ocrDownloadProgress.value = null
        }
    }

    suspend fun getCachedOcrText(mediaId: Long): String? {
        return repository.getOcrText(mediaId)
    }

    suspend fun extractOcrText(context: Context, item: MediaItem): Result<String> {
        val cached = repository.getOcrText(item.id)
        if (!cached.isNullOrBlank()) {
            return Result.success(cached)
        }
        val result = com.hrshd1eux.imava.core.ocr.TesseractOcrEngine.recognizeText(context, item.uri)
        if (result.isSuccess) {
            val text = result.getOrNull().orEmpty()
            if (text.isNotBlank()) {
                repository.saveOcrText(item.id, text)
            }
        }
        return result
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<MediaItem>> = combine(
        _searchQuery.debounce(300),
        _advancedFilterState,
        _excludedBucketIds,
        com.hrshd1eux.imava.core.util.AppLockManager.lockStateVersion
    ) { query, advFilter, excluded, _ ->
        val cleanQuery = query.trim().removePrefix("#")
        if (cleanQuery.isBlank() && !advFilter.isActive) {
            emptyList()
        } else {
            val baseList = if (cleanQuery.isNotBlank()) {
                repository.searchMedia(cleanQuery).filter { item ->
                    !excluded.contains(item.bucketId.toString()) &&
                    !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, item.bucketId, item.bucketName)
                }
            } else {
                visibleMediaItems.value.filter { item ->
                    !excluded.contains(item.bucketId.toString()) &&
                    !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, item.bucketId, item.bucketName)
                }
            }

            if (!advFilter.isActive) {
                baseList
            } else {
                val now = System.currentTimeMillis()
                val (dateStart, dateEnd) = when (advFilter.datePreset) {
                    com.hrshd1eux.imava.ui.search.DatePreset.THIS_WEEK -> Pair(now - 7L * 86400000L, now + 86400000L)
                    com.hrshd1eux.imava.ui.search.DatePreset.THIS_MONTH -> Pair(now - 30L * 86400000L, now + 86400000L)
                    com.hrshd1eux.imava.ui.search.DatePreset.LAST_3_MONTHS -> Pair(now - 90L * 86400000L, now + 86400000L)
                    com.hrshd1eux.imava.ui.search.DatePreset.THIS_YEAR -> Pair(now - 365L * 86400000L, now + 86400000L)
                    com.hrshd1eux.imava.ui.search.DatePreset.CUSTOM -> Pair(advFilter.customStartDate ?: 0L, advFilter.customEndDate ?: Long.MAX_VALUE)
                    com.hrshd1eux.imava.ui.search.DatePreset.ANY -> Pair(0L, Long.MAX_VALUE)
                }

                baseList.filter { item ->
                    when (advFilter.mediaType) {
                        com.hrshd1eux.imava.ui.search.MediaTypeFilterChoice.ALL -> {}
                        com.hrshd1eux.imava.ui.search.MediaTypeFilterChoice.PHOTOS_ONLY -> if (item.isVideo) return@filter false
                        com.hrshd1eux.imava.ui.search.MediaTypeFilterChoice.VIDEOS_ONLY -> if (!item.isVideo) return@filter false
                        com.hrshd1eux.imava.ui.search.MediaTypeFilterChoice.RAW_DNG -> {
                            val isRaw = item.mimeType.contains("dng", ignoreCase = true) || item.mimeType.contains("raw", ignoreCase = true) || item.path.endsWith(".dng", ignoreCase = true)
                            if (!isRaw) return@filter false
                        }
                        com.hrshd1eux.imava.ui.search.MediaTypeFilterChoice.GIFS -> {
                            val isGif = item.mimeType.contains("gif", ignoreCase = true) || item.path.endsWith(".gif", ignoreCase = true)
                            if (!isGif) return@filter false
                        }
                    }

                    if (advFilter.fileSize.minBytes != null && item.size < advFilter.fileSize.minBytes) return@filter false
                    if (advFilter.fileSize.maxBytes != null && item.size > advFilter.fileSize.maxBytes) return@filter false

                    when (advFilter.resolution) {
                        com.hrshd1eux.imava.ui.search.ResolutionFilter.ANY -> {}
                        com.hrshd1eux.imava.ui.search.ResolutionFilter.UHD_4K -> {
                            val is4k = (item.width >= 3840 && item.height >= 2160) || (item.height >= 3840 && item.width >= 2160)
                            if (!is4k) return@filter false
                        }
                        com.hrshd1eux.imava.ui.search.ResolutionFilter.FULL_HD_1080 -> {
                            val isFhd = (item.width >= 1920 && item.height >= 1080) || (item.height >= 1920 && item.width >= 1080)
                            if (!isFhd) return@filter false
                        }
                        com.hrshd1eux.imava.ui.search.ResolutionFilter.PANORAMA -> {
                            val w = item.width.toFloat()
                            val h = maxOf(1, item.height).toFloat()
                            val isPano = (w / h) >= 2.05f || (h / w) >= 2.05f
                            if (!isPano) return@filter false
                        }
                        com.hrshd1eux.imava.ui.search.ResolutionFilter.PORTRAIT -> {
                            if (item.height <= item.width) return@filter false
                        }
                        com.hrshd1eux.imava.ui.search.ResolutionFilter.LANDSCAPE -> {
                            if (item.width <= item.height) return@filter false
                        }
                    }

                    if (advFilter.datePreset != com.hrshd1eux.imava.ui.search.DatePreset.ANY) {
                        if (item.dateTaken < dateStart || item.dateTaken > dateEnd) return@filter false
                    }

                    if (advFilter.cameraModelQuery.isNotBlank()) {
                        if (!item.path.contains(advFilter.cameraModelQuery, ignoreCase = true)) return@filter false
                    }

                    true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var _sortModeState = mutableStateOf(
        try {
            TimelineSortMode.valueOf(prefs.getString("sort_mode", TimelineSortMode.DATE_GROUPED.name) ?: TimelineSortMode.DATE_GROUPED.name)
        } catch (_: Exception) { TimelineSortMode.DATE_GROUPED }
    )
    var sortMode: TimelineSortMode
        get() = _sortModeState.value
        set(value) {
            _sortModeState.value = value
            prefs.edit().putString("sort_mode", value.name).apply()
        }

    fun toggleSortMode() {
        sortMode = if (sortMode == TimelineSortMode.DATE_GROUPED) {
            TimelineSortMode.FLAT_NEWEST_FIRST
        } else {
            TimelineSortMode.DATE_GROUPED
        }
    }

    private var _sortOrderState = mutableStateOf(
        try {
            SortOrder.valueOf(prefs.getString("sort_order", SortOrder.NEWEST_FIRST.name) ?: SortOrder.NEWEST_FIRST.name)
        } catch (_: Exception) { SortOrder.NEWEST_FIRST }
    )
    var sortOrder: SortOrder
        get() {
            val bId = currentBucketId
            if (bId != null) {
                val albumOrderName = prefs.getString("album_sort_order_$bId", null)
                if (albumOrderName != null) {
                    try { return SortOrder.valueOf(albumOrderName) } catch (_: Exception) {}
                }
            }
            return _sortOrderState.value
        }
        set(value) {
            _sortOrderState.value = value
            val bId = currentBucketId
            if (bId != null) {
                prefs.edit().putString("album_sort_order_$bId", value.name).apply()
            } else {
                prefs.edit().putString("sort_order", value.name).apply()
            }
            refreshTrigger.value++
        }

    fun toggleSortOrder() {
        sortOrder = if (sortOrder == SortOrder.NEWEST_FIRST) SortOrder.OLDEST_FIRST else SortOrder.NEWEST_FIRST
    }

    private var _gridStyleState = mutableStateOf(
        try {
            GridStyle.valueOf(prefs.getString("grid_style", GridStyle.NATURAL.name) ?: GridStyle.NATURAL.name)
        } catch (_: Exception) { GridStyle.NATURAL }
    )
    var gridStyle: GridStyle
        get() = _gridStyleState.value
        set(value) {
            _gridStyleState.value = value
            prefs.edit().putString("grid_style", value.name).apply()
        }

    fun toggleGridStyle() {
        gridStyle = if (gridStyle == GridStyle.NATURAL) GridStyle.SQUARE else GridStyle.NATURAL
    }

    private var _gridColumnCountState = mutableStateOf(
        prefs.getInt("grid_column_count", 3).coerceIn(1, 6)
    )
    var gridColumnCount: Int
        get() = _gridColumnCountState.value
        set(value) {
            val clamped = value.coerceIn(1, 6)
            if (_gridColumnCountState.value != clamped) {
                _gridColumnCountState.value = clamped
                prefs.edit().putInt("grid_column_count", clamped).apply()
            }
        }

    fun setGridColumns(columns: Int) {
        gridColumnCount = columns
    }

    fun increaseZoom() {
        if (gridColumnCount > 1) {
            gridColumnCount--
        }
    }

    fun decreaseZoom() {
        if (gridColumnCount < 6) {
            gridColumnCount++
        }
    }

    private val headerZoneId: ZoneId = ZoneId.systemDefault()
    @Volatile private var sameYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    @Volatile private var otherYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
    private val headerTitleCache = java.util.concurrent.ConcurrentHashMap<Long, String>()
    @Volatile private var cachedTodayEpochDay: Long = -1L
    @Volatile private var cachedYesterdayEpochDay: Long = -1L
    @Volatile private var cachedCurrentYear: Int = -1
    @Volatile private var cachedLocaleTag: String = ""
    @Volatile private var cachedTodayLabel: String = "Today"
    @Volatile private var cachedYesterdayLabel: String = "Yesterday"
    @Volatile private var cachedUndatedLabel: String = "Undated"

    private fun updateCachedDaysAndLocale() {
        val activeLocale = Locale.getDefault()
        val activeTag = activeLocale.toLanguageTag()
        val today = LocalDate.now(headerZoneId)
        val todayEpoch = today.toEpochDay()
        if (cachedTodayEpochDay != todayEpoch || cachedLocaleTag != activeTag) {
            cachedTodayEpochDay = todayEpoch
            cachedYesterdayEpochDay = today.minusDays(1).toEpochDay()
            cachedCurrentYear = today.year
            cachedLocaleTag = activeTag
            sameYearFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", activeLocale)
            otherYearFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", activeLocale)
            try {
                val localizedCtx = com.hrshd1eux.imava.core.util.LocaleHelper.localizedContext(application)
                cachedTodayLabel = localizedCtx.getString(com.hrshd1eux.imava.R.string.date_today)
                cachedYesterdayLabel = localizedCtx.getString(com.hrshd1eux.imava.R.string.date_yesterday)
                cachedUndatedLabel = localizedCtx.getString(com.hrshd1eux.imava.R.string.date_undated)
            } catch (_: Exception) {}
            headerTitleCache.clear()
        }
    }

    /** Clears cached formatted headers & stories when the active locale changes across Activity recreation. */
    fun syncLocaleState() {
        val activeTag = Locale.getDefault().toLanguageTag()
        if (cachedLocaleTag.isNotEmpty() && cachedLocaleTag != activeTag) {
            cachedLocaleTag = ""
            headerTitleCache.clear()
            updateCachedDaysAndLocale()
            refreshTrigger.value++
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getHeaderTitleForMedia(media: MediaItem, category: String? = null): String {
        updateCachedDaysAndLocale()
        val isTrash = category == "Trash"
        val dateMs = if (isTrash && media.trashTime > 0L) media.trashTime else media.dateTaken
        if (dateMs <= 0L) {
            return cachedUndatedLabel
        }
        val ms = dateMs
        val localDate = Instant.ofEpochMilli(ms).atZone(headerZoneId).toLocalDate()
        val epochDay = localDate.toEpochDay()

        val cacheKey = if (isTrash) -epochDay - 1 else epochDay
        val cached = headerTitleCache[cacheKey]
        if (cached != null) return cached

        val title = when (epochDay) {
            cachedTodayEpochDay -> cachedTodayLabel
            cachedYesterdayEpochDay -> cachedYesterdayLabel
            else -> if (localDate.year == cachedCurrentYear) {
                localDate.format(sameYearFormatter)
            } else {
                localDate.format(otherYearFormatter)
            }
        }
        headerTitleCache[cacheKey] = title
        return title
    }

    val pagingDataFlow: Flow<PagingData<TimelineItem>> = combine(
        combine(snapshotFlow { currentBucketId }, _currentCategoryName) { bId, cat -> Pair(bId, cat) },
        combine(snapshotFlow { sortMode }, snapshotFlow { sortOrder }) { mode, order -> Pair(mode, order) },
        _excludedBucketIds,
        com.hrshd1eux.imava.core.util.AppLockManager.lockStateVersion,
        combine(_selectedFilter, _selectedCustomTag, refreshTrigger) { f, tag, r -> Triple(f, tag, r) }
    ) { (bucketId, category), (mode, order), excluded, _, (filter, customTag, _) ->
        val itemsFlow: Flow<PagingData<MediaItem>> = if (category == "Trash") {
            trashed.map { list ->
                if (com.hrshd1eux.imava.core.util.AppLockManager.isTrashLocked(application)) {
                    PagingData.empty()
                } else {
                    val sorted = list.sortedWith(
                        compareByDescending<MediaItem> { if (it.trashTime > 0L) it.trashTime else it.dateTaken }.thenByDescending { it.id }
                    )
                    PagingData.from(sorted)
                }
            }
        } else if (category == "Hidden Vault") {
            hidden.map { list ->
                val sorted = if (order == SortOrder.OLDEST_FIRST) list.sortedBy { it.dateTaken } else list.sortedByDescending { it.dateTaken }
                PagingData.from(sorted)
            }
        } else if (category?.startsWith("Smart:") == true) {
            visibleMediaItems.map { allItems ->
                val sorted = if (order == SortOrder.OLDEST_FIRST) allItems.sortedBy { it.dateTaken } else allItems.sortedByDescending { it.dateTaken }
                PagingData.from(sorted)
            }
        } else if (!customTag.isNullOrBlank()) {
            visibleMediaItems.map { allItems ->
                val tagFiltered = allItems.filter { item ->
                    val matchesBucket = if (bucketId != null) item.bucketId == bucketId else true
                    matchesBucket && item.tags.any { t -> t.equals(customTag, ignoreCase = true) }
                }
                val sorted = if (order == SortOrder.OLDEST_FIRST) tagFiltered.sortedBy { it.dateTaken } else tagFiltered.sortedByDescending { it.dateTaken }
                PagingData.from(sorted)
            }
        } else if (filter == MediaFilterType.SCREENSHOTS || filter == MediaFilterType.FAVORITES || filter == MediaFilterType.GIFS || filter == MediaFilterType.RAW) {
            visibleMediaItems.map { allItems ->
                val chipFiltered = allItems.filter { item ->
                    val matchesBucket = if (bucketId != null) item.bucketId == bucketId else true
                    matchesBucket && matchesFilter(item, filter)
                }
                val sorted = if (order == SortOrder.OLDEST_FIRST) chipFiltered.sortedBy { it.dateTaken } else chipFiltered.sortedByDescending { it.dateTaken }
                PagingData.from(sorted)
            }
        } else when (category) {
            "Favorites" -> favorites.map { list ->
                val filtered = if (bucketId != null) {
                    if (com.hrshd1eux.imava.core.util.AppLockManager.isAlbumLocked(application, bucketId, currentBucketName)) emptyList()
                    else list.filter { it.bucketId == bucketId }
                } else list.filter { !excluded.contains(it.bucketId.toString()) && !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, it.bucketId, it.bucketName) }
                val sorted = if (order == SortOrder.OLDEST_FIRST) filtered.sortedBy { it.dateTaken } else filtered.sortedByDescending { it.dateTaken }
                PagingData.from(sorted)
            }
            "Videos" -> Pager(
                config = PagingConfig(pageSize = 100, prefetchDistance = 120, enablePlaceholders = false),
                pagingSourceFactory = { MediaPagingSource(repository, bucketId, currentBucketIds.ifEmpty { null }, order, com.hrshd1eux.imava.data.media.MediaTypeFilter.VIDEOS) }
            ).flow.map { pagingData ->
                if (bucketId != null) {
                    if (com.hrshd1eux.imava.core.util.AppLockManager.isAlbumLocked(application, bucketId, currentBucketName)) PagingData.empty()
                    else pagingData.filter { !it.isTrashed && !it.isHidden }
                } else pagingData.filter { !it.isTrashed && !it.isHidden && !excluded.contains(it.bucketId.toString()) && !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, it.bucketId, it.bucketName) }
            }
            else -> {
                val mediaType = if (filter == MediaFilterType.PHOTOS) com.hrshd1eux.imava.data.media.MediaTypeFilter.IMAGES else com.hrshd1eux.imava.data.media.MediaTypeFilter.ALL
                Pager(
                    config = PagingConfig(pageSize = 100, prefetchDistance = 120, enablePlaceholders = false),
                    pagingSourceFactory = { MediaPagingSource(repository, bucketId, currentBucketIds.ifEmpty { null }, order, mediaType) }
                ).flow.map { pagingData ->
                    if (bucketId != null) {
                        if (com.hrshd1eux.imava.core.util.AppLockManager.isAlbumLocked(application, bucketId, currentBucketName)) PagingData.empty()
                        else pagingData.filter { !it.isTrashed && !it.isHidden }
                    } else pagingData.filter { !it.isTrashed && !it.isHidden && !excluded.contains(it.bucketId.toString()) && !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, it.bucketId, it.bucketName) }
                }
            }
        }
        itemsFlow.map { pagingData ->
            val nonTrashFiltered = if (category != "Trash") pagingData.filter { !it.isTrashed && !it.isHidden } else pagingData
            val baseFiltered = if (filter == MediaFilterType.ALL || filter == MediaFilterType.PHOTOS || filter == MediaFilterType.SCREENSHOTS || filter == MediaFilterType.FAVORITES || filter == MediaFilterType.GIFS || filter == MediaFilterType.RAW) nonTrashFiltered else nonTrashFiltered.filter { matchesFilter(it, filter) }
            val filteredData = if (customTag.isNullOrBlank()) baseFiltered else baseFiltered.filter { it.tags.any { t -> t.equals(customTag, ignoreCase = true) } }
            val mapped: PagingData<TimelineItem> = filteredData.map { TimelineItem.Media(it) }
            if (mode == TimelineSortMode.DATE_GROUPED) {
                mapped.insertSeparators { before: TimelineItem?, after: TimelineItem? ->
                    if (before == null && after is TimelineItem.Media) {
                        TimelineItem.Header(getHeaderTitleForMedia(after.item, category))
                    } else if (before is TimelineItem.Media && after is TimelineItem.Media) {
                        val beforeTitle = getHeaderTitleForMedia(before.item, category)
                        val afterTitle = getHeaderTitleForMedia(after.item, category)
                        if (beforeTitle != afterTitle) {
                            TimelineItem.Header(afterTitle)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            } else {
                mapped
            }
        }
    }.flatMapLatest { it }.cachedIn(viewModelScope)

    private val PAGE_SIZE = 200

    init {
        viewModelScope.launch {
            combine(
                snapshotFlow { currentBucketId },
                snapshotFlow { sortOrder },
                refreshTrigger
            ) { bucketId, order, _ ->
                Pair(bucketId, order)
            }.flatMapLatest { (bucketId, order) ->
                repository.getMediaFlow(bucketId, order)
            }.collect { items ->
                _mediaItems.value = items
                _isInitialScanning.value = false
            }
        }

        viewModelScope.launch {
            repository.getBucketsFlow().collect {
                _buckets.value = it
            }
        }
        viewModelScope.launch {
            repository.getFavoriteMediaFlow().collect {
                _favorites.value = it
            }
        }
        viewModelScope.launch {
            repository.getTrashedMediaFlow().collect {
                _trashed.value = it
            }
        }
        viewModelScope.launch {
            isVaultUnlocked.flatMapLatest { unlocked ->
                repository.getHiddenMediaFlow(unlocked)
            }.collect {
                _hidden.value = it
            }
        }
        

        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            repository.observeMediaChanges()
                .debounce(200)
                .collectLatest {
                    // Option E: Invalidate third-party cache on any MediaStore change
                    // so new WhatsApp/Telegram files appear on next refresh without
                    // waiting for the 60s TTL.
                    repository.invalidateThirdPartyCache()
                    refreshAll()
                }
        }

        // cleanup & auto deep scan after boot
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.purgeExpiredTrashMedia()
                com.hrshd1eux.imava.core.util.AppUpdateManager.cleanUpOldUpdates(application)
                com.hrshd1eux.imava.core.util.VaultCacheManager.clearCache(application)
                kotlinx.coroutines.delay(2000) // defer scan for cold boot performance
                if (isAutoDeepScanEnabled) {
                    triggerAutoDeepScan()
                }
                val activeIds = repository.getActiveMediaIds()
                if (activeIds.isNotEmpty()) {
                    repository.deleteOrphanedMetadata(activeIds)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Periodic background deep scan while app is running
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                kotlinx.coroutines.delay(45_000L)
                if (isAutoDeepScanEnabled && !_isDeepScanning.value) {
                    triggerAutoDeepScan()
                }
            }
        }
    }

    var pendingRenameItem: MediaItem? = null
    var pendingRenameName: String? = null
    var pendingRotateItem: MediaItem? = null
    var pendingRotateClockwise: Boolean = true

    fun renameMedia(context: Context, item: MediaItem, newName: String) {
        viewModelScope.launch {
            try {
                val success = repository.renameMedia(context, item, newName)
                if (success) {
                    applyRenamedState(item, newName)
                }
            } catch (e: Exception) {
                val recoverable = e as? android.app.RecoverableSecurityException
                    ?: e.cause as? android.app.RecoverableSecurityException
                val isSec = e is SecurityException || e.cause is SecurityException
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && (isSec || recoverable != null)) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingRenameItem = item
                        pendingRenameName = newName
                        val pendingIntent = android.provider.MediaStore.createWriteRequest(
                            context.contentResolver,
                            listOf(item.uri)
                        )
                        activity.startIntentSenderForResult(
                            pendingIntent.intentSender,
                            1006,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && recoverable != null) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingRenameItem = item
                        pendingRenameName = newName
                        activity.startIntentSenderForResult(
                            recoverable.userAction.actionIntent.intentSender,
                            1006,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                } else {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun applyRenamedState(item: MediaItem, newName: String) {
        val file = java.io.File(item.path)
        val ext = file.extension.ifEmpty { "jpg" }
        val finalName = if (newName.contains(".")) newName else "$newName.$ext"
        val newPath = if (file.parentFile != null) java.io.File(file.parentFile, finalName).absolutePath else item.path

        val currentActive = activeMediaItem
        if (currentActive?.id == item.id) {
            activeMediaItem = when (currentActive) {
                is MediaItem.Photo -> currentActive.copy(path = newPath)
                is MediaItem.Video -> currentActive.copy(path = newPath)
            }
        }
        refreshAll()
    }

    fun updateMediaDateTaken(context: Context, item: MediaItem, newDateMs: Long) {
        viewModelScope.launch {
            val success = repository.updateMediaDateTaken(context, item, newDateMs)
            if (success) {
                val updatedItem = when (item) {
                    is MediaItem.Photo -> item.copy(dateTaken = newDateMs)
                    is MediaItem.Video -> item.copy(dateTaken = newDateMs)
                }
                val currentList = _mediaItems.value.map { if (it.id == item.id) updatedItem else it }
                _mediaItems.value = if (sortOrder == SortOrder.OLDEST_FIRST) {
                    currentList.sortedBy { it.dateTaken }
                } else {
                    currentList.sortedByDescending { it.dateTaken }
                }
                if (activeMediaItem?.id == item.id) {
                    activeMediaItem = updatedItem
                }
                refreshAll()
            }
        }
    }

    fun loadNextPage() {
        viewModelScope.launch {
            val items = repository.loadMediaPaged(
                limit = 200,
                offset = 0,
                bucketId = currentBucketId,
                bucketIds = currentBucketIds.ifEmpty { null },
                sortOrder = sortOrder
            )
            _mediaItems.value = if (currentBucketId != null) items else items.filter {
                !_excludedBucketIds.value.contains(it.bucketId.toString()) &&
                !com.hrshd1eux.imava.core.util.AppLockManager.isMediaItemLocked(application, it.bucketId, it.bucketName)
            }
        }
    }

    fun selectBucket(bucketId: Long?, bucketName: String?, bucketIds: List<Long>? = null, navigateToPhotos: Boolean = true) {
        currentCategoryName = null // Reset category filter when selecting a folder
        currentBucketId = bucketId
        currentBucketIds = bucketIds ?: (bucketId?.let { listOf(it) } ?: emptyList())
        currentBucketName = bucketName
        if (navigateToPhotos) {
            currentScreen = Screen.Photos
        }
        // refreshTrigger++ is called inside loadNextPage() above, which invalidates
        // both pagingDataFlow and getMediaFlow. No MAX_VALUE scan needed.
        loadNextPage()
    }

    fun clearVaultCache(context: Context) {
        viewModelScope.launch {
            repository.clearVaultCache(context)
        }
    }

    fun loadMediaStream() {
        refreshAll()
    }

    suspend fun getAllPhotosForDuplicateScan(): List<MediaItem.Photo> = withContext(Dispatchers.IO) {
        val allMedia = repository.loadMediaPaged(limit = 5000, offset = 0, bucketId = null)
        allMedia.filterIsInstance<MediaItem.Photo>()
    }

    fun loadBuckets() {
        viewModelScope.launch(Dispatchers.IO) {
            _buckets.value = repository.getBuckets()
        }
    }

    fun refreshAll() {
        repository.invalidateThirdPartyCache()
        refreshTrigger.value++
        loadBuckets()
    }

    fun moveMediaToFolder(context: Context, items: List<MediaItem>, folderName: String) {
        val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
        val targetDir = java.io.File(picturesDir, folderName)
        moveOrCopyMedia(context, items, targetDir, isCopy = false) {}
    }

    fun createEmptyAlbum(context: Context, albumName: String) {
        viewModelScope.launch {
            try {
                val userPrefs = context.getSharedPreferences("user_albums", Context.MODE_PRIVATE)
                val currentSet = userPrefs.getStringSet("created_albums", emptySet()) ?: emptySet()
                val updatedSet = currentSet.toMutableSet().apply { add(albumName) }
                userPrefs.edit().putStringSet("created_albums", updatedSet).commit()

                val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val newAlbumDir = java.io.File(picturesDir, albumName)
                if (!newAlbumDir.exists()) {
                    newAlbumDir.mkdirs()
                }
                refreshAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAlbum(
        context: Context,
        bucketId: Long,
        bucketName: String,
        bucketPath: String? = null,
        deleteMedia: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                // 1. Immediately remove from user_albums SharedPreferences case-insensitively
                val userPrefs = context.getSharedPreferences("user_albums", Context.MODE_PRIVATE)
                val currentSet = userPrefs.getStringSet("created_albums", emptySet()) ?: emptySet()
                val updatedSet = currentSet.filter { !it.equals(bucketName, ignoreCase = true) }.toSet()
                userPrefs.edit().putStringSet("created_albums", updatedSet).commit()

                // 2. Immediately evict from ViewModel state
                if (currentBucketId == bucketId) {
                    currentBucketId = null
                    currentBucketName = null
                    currentCategoryName = null
                }
                _buckets.value = _buckets.value.filter { it.id != bucketId }

                // 3. Find items in album and candidate directories on disk
                val itemsInAlbum = repository.loadMediaPaged(limit = 5000, offset = 0, bucketId = bucketId)

                val candidateDirs = mutableListOf<java.io.File>()
                if (!bucketPath.isNullOrBlank()) {
                    candidateDirs.add(java.io.File(bucketPath))
                }
                itemsInAlbum.forEach { item ->
                    if (item.path.isNotBlank()) {
                        val p = java.io.File(item.path).parentFile
                        if (p != null && !candidateDirs.contains(p)) {
                            candidateDirs.add(p)
                        }
                    }
                }
                val defaultAlbumDir = java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                    bucketName
                )
                if (!candidateDirs.contains(defaultAlbumDir)) {
                    candidateDirs.add(defaultAlbumDir)
                }

                if (deleteMedia) {
                    val deleteTitle = context.getString(R.string.deleting_media)
                    if (itemsInAlbum.isNotEmpty()) {
                        _batchOperationProgress.value = BatchProgress(title = deleteTitle, current = 0, total = itemsInAlbum.size)
                        val hasAllFilesAccess = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !hasAllFilesAccess) {
                            pendingBatchActionItems = itemsInAlbum
                            val validUris = itemsInAlbum.mapNotNull { it.uri.takeIf { u -> u.scheme == android.content.ContentResolver.SCHEME_CONTENT } }
                            if (validUris.isNotEmpty()) {
                                try {
                                    val pendingIntent = android.provider.MediaStore.createTrashRequest(
                                        context.contentResolver,
                                        validUris,
                                        true
                                    )
                                    val activity = context.findActivity()
                                    activity?.startIntentSenderForResult(pendingIntent.intentSender, 1005, null, 0, 0, 0)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            // Android <= 10 or All Files Access:
                            val trashDir = java.io.File(context.filesDir, "imava_trash").apply { mkdirs() }
                            itemsInAlbum.forEachIndexed { index, item ->
                                _batchOperationProgress.value = BatchProgress(title = deleteTitle, current = index + 1, total = itemsInAlbum.size)
                                val srcFile = java.io.File(item.path)
                                if (srcFile.exists()) {
                                    try {
                                        val destFile = java.io.File(trashDir, "${item.id}_${srcFile.name}")
                                        if (!srcFile.renameTo(destFile)) {
                                            srcFile.copyTo(destFile, overwrite = true)
                                            srcFile.delete()
                                        }
                                    } catch (_: Exception) {
                                        try { srcFile.delete() } catch (_: Exception) {}
                                    }
                                }
                                try { context.contentResolver.delete(item.uri, null, null) } catch (_: Exception) {}
                                try {
                                    val mediaUri = getMediaProviderUri(item)
                                    context.contentResolver.delete(mediaUri, null, null)
                                } catch (_: Exception) {}
                                repository.toggleTrashed(item)
                                repository.evictSecondaryDiskItem(item.id, item.path)
                            }
                        }
                        val deletedIds = itemsInAlbum.map { it.id }.toSet()
                        _mediaItems.value = _mediaItems.value.filter { it.id !in deletedIds }
                    }

                    // Delete candidate directories recursively on disk
                    candidateDirs.forEach { dir ->
                        try {
                            if (dir.exists()) {
                                dir.deleteRecursively()
                            }
                        } catch (_: Exception) {}
                    }

                    // Clean MediaStore database entries for this bucket_id
                    val where = "${android.provider.MediaStore.Files.FileColumns.BUCKET_ID} = ?"
                    val args = arrayOf(bucketId.toString())
                    try { context.contentResolver.delete(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, where, args) } catch (_: Exception) {}
                    try { context.contentResolver.delete(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, where, args) } catch (_: Exception) {}
                    try { context.contentResolver.delete(android.provider.MediaStore.Files.getContentUri("external"), where, args) } catch (_: Exception) {}

                    // Notify media scanner of deleted directory paths
                    val pathsToScan = candidateDirs.map { it.absolutePath }.toTypedArray()
                    if (pathsToScan.isNotEmpty()) {
                        try {
                            android.media.MediaScannerConnection.scanFile(context, pathsToScan, null, null)
                        } catch (_: Exception) {}
                    }
                } else {
                    if (itemsInAlbum.isNotEmpty()) {
                        val primaryDir = candidateDirs.firstOrNull() ?: defaultAlbumDir
                        val targetDir = if (primaryDir.parentFile != null && primaryDir.parentFile?.exists() == true && primaryDir.parentFile?.canWrite() == true) {
                            primaryDir.parentFile!!
                        } else {
                            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                        }
                        val moveTitle = context.getString(R.string.moving_media)
                        _batchOperationProgress.value = BatchProgress(title = moveTitle, current = 0, total = itemsInAlbum.size)
                        repository.moveOrCopyMedia(context, itemsInAlbum, targetDir, isCopy = false) { cur, tot ->
                            _batchOperationProgress.value = BatchProgress(title = moveTitle, current = cur, total = tot)
                        }
                    }
                    candidateDirs.forEach { dir ->
                        try {
                            if (dir.exists()) {
                                dir.deleteRecursively()
                            }
                        } catch (_: Exception) {}
                    }
                    val pathsToScan = candidateDirs.map { it.absolutePath }.toTypedArray()
                    if (pathsToScan.isNotEmpty()) {
                        try {
                            android.media.MediaScannerConnection.scanFile(context, pathsToScan, null, null)
                        } catch (_: Exception) {}
                    }
                }
                try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                refreshAll()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _batchOperationProgress.value = null
            }
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
            // Sync current active view state
            if (activeMediaItem?.id == item.id) {
                activeMediaItem = when (val active = activeMediaItem) {
                    is MediaItem.Photo -> active.copy(isFavorite = !active.isFavorite)
                    is MediaItem.Video -> active.copy(isFavorite = !active.isFavorite)
                    null -> null
                }
            }
            refreshAll()
        }
    }

    fun updateMediaTags(item: MediaItem, tags: List<String>) {
        viewModelScope.launch {
            repository.updateMediaTags(item.id, tags)
            if (activeMediaItem?.id == item.id) {
                activeMediaItem = when (val active = activeMediaItem) {
                    is MediaItem.Photo -> active.copy(tags = tags)
                    is MediaItem.Video -> active.copy(tags = tags)
                    null -> null
                }
            }
            refreshAll()
        }
    }

    fun bulkUpdateMediaTags(
        itemIds: Set<Long>,
        tagsToAdd: Set<String>,
        tagsToRemove: Set<String>
    ) {
        if (itemIds.isEmpty()) return
        viewModelScope.launch {
            repository.bulkUpdateMediaTags(itemIds, tagsToAdd, tagsToRemove)
            selectionState.clear()
            refreshAll()
        }
    }

    fun toggleFavoriteSelectedMedia(selectedIds: Set<Long>) {
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            repository.toggleFavoriteBatch(selectedIds)
            selectionState.clear()
            refreshAll()
        }
    }

    private var pendingBatchRenames: List<Pair<MediaItem, String>>? = null

    fun batchRenameSelectedMedia(
        context: Context,
        renames: List<Pair<MediaItem, String>>
    ) {
        if (renames.isEmpty()) return
        viewModelScope.launch {
            val hasAllFilesAccess = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !hasAllFilesAccess) {
                val activity = context.findActivity()
                if (activity != null) {
                    try {
                        pendingBatchRenames = renames
                        val uris = renames.map { getMediaProviderUri(it.first) }
                        val pendingIntent = android.provider.MediaStore.createWriteRequest(
                            context.contentResolver,
                            uris
                        )
                        activity.startIntentSenderForResult(
                            pendingIntent.intentSender,
                            1008,
                            null,
                            0,
                            0,
                            0
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val count = repository.batchRenameMedia(context, renames)
                        selectionState.clear()
                        refreshAll()
                        if (count > 0) {
                            try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                            safeToast(context, localizedString(context, R.string.vm_renamed_items_toast_fmt, count))
                        }
                    }
                } else {
                    val count = repository.batchRenameMedia(context, renames)
                    selectionState.clear()
                    refreshAll()
                    if (count > 0) {
                        try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                        safeToast(context, localizedString(context, R.string.vm_renamed_items_toast_fmt, count))
                    }
                }
            } else {
                val count = repository.batchRenameMedia(context, renames)
                selectionState.clear()
                refreshAll()
                if (count > 0) {
                    try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                    safeToast(context, localizedString(context, R.string.vm_renamed_items_toast_fmt, count))
                }
            }
        }
    }

    private var pendingMoveSourceItems: List<MediaItem>? = null
    private var pendingMoveRollbackTargets: List<java.io.File>? = null
    private var pendingMoveOnComplete: ((Int) -> Unit)? = null

    fun moveOrCopyMedia(
        context: Context,
        items: List<MediaItem>,
        targetDirectory: java.io.File,
        isCopy: Boolean,
        onComplete: (Int) -> Unit
    ) {
        if (items.isEmpty()) return
        val title = if (isCopy) context.getString(R.string.copying_media) else context.getString(R.string.moving_media)
        if (items.size > 1) {
            _batchOperationProgress.value = BatchProgress(title = title, current = 0, total = items.size)
        }
        viewModelScope.launch {
            try {
                val result = repository.moveOrCopyMedia(context, items, targetDirectory, isCopy) { cur, tot ->
                    if (items.size > 1) {
                        _batchOperationProgress.value = BatchProgress(title = title, current = cur, total = tot)
                    }
                }
                val moveResult = result.getOrNull()
                val count = moveResult?.successCount ?: 0

                if (moveResult != null && moveResult.failedDeleteItems.isNotEmpty()) {
                    pendingMoveSourceItems = moveResult.failedDeleteItems
                    pendingMoveRollbackTargets = moveResult.createdTargetsForFailedDeletes
                    pendingMoveOnComplete = onComplete

                    val activity = context.findActivity()
                    if (activity != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                            try {
                                val uris = moveResult.failedDeleteItems.map { it.uri }
                                val pendingIntent = android.provider.MediaStore.createDeleteRequest(
                                    context.contentResolver,
                                    uris
                                )
                                activity.startIntentSenderForResult(
                                    pendingIntent.intentSender,
                                    1007,
                                    null,
                                    0,
                                    0,
                                    0
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                moveResult.createdTargetsForFailedDeletes.forEach { it.delete() }
                                pendingMoveSourceItems = null
                                pendingMoveRollbackTargets = null
                                pendingMoveOnComplete = null
                                safeToast(context, localizedString(context, R.string.vm_move_failed_fmt, e.message ?: ""))
                            }
                        } else if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.Q) {
                            try {
                                val firstUri = moveResult.failedDeleteItems.first().uri
                                context.contentResolver.delete(firstUri, null, null)
                            } catch (secEx: android.app.RecoverableSecurityException) {
                                val intentSender = secEx.userAction.actionIntent.intentSender
                                activity.startIntentSenderForResult(
                                    intentSender,
                                    1007,
                                    null,
                                    0,
                                    0,
                                    0
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                moveResult.createdTargetsForFailedDeletes.forEach { it.delete() }
                                pendingMoveSourceItems = null
                                pendingMoveRollbackTargets = null
                                pendingMoveOnComplete = null
                            }
                        } else {
                            loadBuckets()
                            refreshAll()
                            onComplete(count)
                        }
                    } else {
                        moveResult.createdTargetsForFailedDeletes.forEach { it.delete() }
                        pendingMoveSourceItems = null
                        pendingMoveRollbackTargets = null
                        pendingMoveOnComplete = null
                        safeToast(context, localizedString(context, R.string.vm_move_failed_no_activity))
                    }
                } else {
                    loadBuckets()
                    refreshAll()
                    if (count > 0) {
                        try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                        val resId = if (isCopy) R.string.vm_copied_to_album_fmt else R.string.vm_moved_to_album_fmt
                        safeToast(context, localizedString(context, resId, count, targetDirectory.name))
                    } else if (items.isNotEmpty()) {
                        safeToast(context, localizedString(context, R.string.vm_items_already_in_album))
                    }
                    onComplete(count)
                }
            } finally {
                _batchOperationProgress.value = null
            }
        }
    }

    fun moveOrCopySelectedMedia(
        context: Context,
        targetDirectory: java.io.File,
        isCopy: Boolean,
        onComplete: (Int) -> Unit
    ) {
        val selectedIds = selectionState.selectedIds.toSet()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            val items = getSelectedMediaItems(selectedIds)
            moveOrCopyMedia(context, items, targetDirectory, isCopy) { count ->
                selectionState.clear()
                onComplete(count)
            }
        }
    }

    fun shiftSelectedMediaTimestamp(
        context: Context,
        offsetMillis: Long,
        exactTimestamp: Long? = null,
        onComplete: (Int) -> Unit
    ) {
        val selectedIds = selectionState.selectedIds.toSet()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            val items = getSelectedMediaItems(selectedIds)
            val result = repository.shiftMediaTimestamps(context, items, offsetMillis, exactTimestamp)
            val count = result.getOrDefault(0)
            selectionState.clear()
            refreshAll()
            if (count > 0) {
                try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                safeToast(context, localizedString(context, R.string.vm_adjusted_datetime_fmt, count))
            }
            onComplete(count)
        }
    }

    fun disableVault(context: Context) {
        val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("vault_disabled", true)
            .remove("vault_pin_hash")
            .remove("vault_salt")
            .remove("vault_pin")
            .remove("vault_biometric_enabled")
            .remove("vault_stealth_mode")
            .apply()
        vaultState.setUnlocked(true)
        refreshAll()
    }

    fun deleteVault(context: Context, restoreMedia: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (restoreMedia) {
                repository.restoreAllVaultMedia(context)
            } else {
                repository.deleteAllVaultData(context)
            }
            val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            vaultState.setUnlocked(false)
            currentCategoryName = null
            refreshAll()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    val isVaultDisabled: Boolean
        get() {
            val prefs = application.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean("vault_disabled", false)
        }

    var pendingActionItem: MediaItem? = null
    var pendingNextItem: MediaItem? = null

    fun advanceActiveMediaItem(deletedItem: MediaItem, targetNextItem: MediaItem? = null) {
        if (activeMediaItem?.id == deletedItem.id) {
            if (targetNextItem != null && targetNextItem.id != deletedItem.id) {
                activeMediaItem = targetNextItem
            } else {
                val list = viewerMediaItems.value
                val idx = list.indexOfFirst { it.id == deletedItem.id }
                activeMediaItem = if (idx != -1) {
                    if (idx + 1 < list.size) list[idx + 1]
                    else if (idx - 1 >= 0) list[idx - 1]
                    else null
                } else {
                    null
                }
            }
        }
    }

    fun toggleHidden(context: Context, item: MediaItem, targetNextItem: MediaItem? = null) {
        viewModelScope.launch {
            try {
                repository.toggleHidden(context, item)
                refreshAll()
            } catch (e: Exception) {
                val recoverable = e as? android.app.RecoverableSecurityException
                    ?: e.cause as? android.app.RecoverableSecurityException
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingActionItem = item
                        pendingNextItem = targetNextItem
                        val pendingIntent = android.provider.MediaStore.createDeleteRequest(
                            context.contentResolver,
                            listOf(item.uri)
                        )
                        activity.startIntentSenderForResult(
                            pendingIntent.intentSender,
                            1004,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && recoverable != null) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingActionItem = item
                        pendingNextItem = targetNextItem
                        activity.startIntentSenderForResult(
                            recoverable.userAction.actionIntent.intentSender,
                            1004,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                }
            }
            advanceActiveMediaItem(item, targetNextItem)
        }
    }

    fun getMediaProviderUri(item: MediaItem): android.net.Uri {
        return if (item.uri.scheme == android.content.ContentResolver.SCHEME_CONTENT &&
            item.uri.authority == android.provider.MediaStore.AUTHORITY &&
            (item.uri.toString().contains("/images/") || item.uri.toString().contains("/video/"))
        ) {
            item.uri
        } else if (item.id > 0) {
            if (item.isVideo) {
                android.content.ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
            } else {
                android.content.ContentUris.withAppendedId(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
            }
        } else {
            item.uri
        }
    }

    fun toggleTrashed(context: Context, item: MediaItem, targetNextItem: MediaItem? = null) {
        if (!item.isTrashed) {
            _mediaItems.value = _mediaItems.value.filter { it.id != item.id }
            try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                val hasAllFilesAccess = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager()
                val isDirectDiskFile = item.uri.scheme != android.content.ContentResolver.SCHEME_CONTENT || item.id <= 0
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !isDirectDiskFile && !hasAllFilesAccess) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingActionItem = item
                        pendingNextItem = targetNextItem
                        try {
                            val mediaUri = getMediaProviderUri(item)
                            val pendingIntent = android.provider.MediaStore.createTrashRequest(
                                context.contentResolver,
                                listOf(mediaUri),
                                !item.isTrashed
                            )
                            activity.startIntentSenderForResult(
                                pendingIntent.intentSender, 1002, null, 0, 0, 0
                            )
                            return@launch
                        } catch (_: Exception) {
                            // Fall through to direct trashing if createTrashRequest failed (e.g. non-standard URI)
                        }
                    }
                }

                // Direct trashing fallback: works for Android <= 10, direct disk files, and rejected URIs
                val trashDir = java.io.File(context.filesDir, "imava_trash").apply { mkdirs() }
                if (!item.isTrashed) {
                    val srcFile = java.io.File(item.path)
                    if (srcFile.exists()) {
                        val destFile = java.io.File(trashDir, "${item.id}_${srcFile.name}")
                        try {
                            if (!srcFile.renameTo(destFile)) {
                                srcFile.copyTo(destFile, overwrite = true)
                                srcFile.delete()
                            }
                        } catch (_: Exception) {}
                    }
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                    } catch (r: android.app.RecoverableSecurityException) {
                        throw r
                    } catch (_: Exception) {}
                    try {
                        val mediaUri = getMediaProviderUri(item)
                        context.contentResolver.delete(mediaUri, null, null)
                    } catch (r: android.app.RecoverableSecurityException) {
                        throw r
                    } catch (_: Exception) {}
                    try {
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
                    } catch (_: Exception) {}
                } else {
                    val trashedFile = java.io.File(trashDir, "${item.id}_${java.io.File(item.path).name}")
                    val origFile = java.io.File(item.path)
                    if (trashedFile.exists()) {
                        try {
                            origFile.parentFile?.mkdirs()
                            if (!trashedFile.renameTo(origFile)) {
                                trashedFile.copyTo(origFile, overwrite = true)
                                trashedFile.delete()
                            }
                        } catch (_: Exception) {}
                    }
                    try {
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
                    } catch (_: Exception) {}
                }
                repository.toggleTrashed(item)
                repository.evictSecondaryDiskItem(item.id, item.path)
                repository.invalidateThirdPartyCache()
                advanceActiveMediaItem(item, targetNextItem)
                _lastDeletedMediaId.value = item.id
                pendingActionItem = null
                pendingNextItem = null
                refreshAll()
            } catch (e: Exception) {
                val recoverable = e as? android.app.RecoverableSecurityException
                    ?: e.cause as? android.app.RecoverableSecurityException
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && recoverable != null) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingActionItem = item
                        pendingNextItem = targetNextItem
                        activity.startIntentSenderForResult(
                            recoverable.userAction.actionIntent.intentSender,
                            1002,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                } else {
                    e.printStackTrace()
                }
            }
        }
    }

    fun emptyTrash(context: Context) {
        viewModelScope.launch {
            val trashedItems = trashed.value
            if (trashedItems.isNotEmpty()) {
                val trashDir = java.io.File(context.filesDir, "imava_trash")
                val secureShred = com.hrshd1eux.imava.core.util.FileShredderUtil.isSecureShredEnabled(context)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                    val mediaStoreItems = trashedItems.filter { it.id > 0 && it.uri.scheme == android.content.ContentResolver.SCHEME_CONTENT }
                    if (mediaStoreItems.isNotEmpty()) {
                        val activity = context.findActivity()
                        if (activity != null) {
                            pendingBatchActionItems = trashedItems
                            try {
                                val uris = mediaStoreItems.map { getMediaProviderUri(it) }
                                val pendingIntent = android.provider.MediaStore.createDeleteRequest(
                                    context.contentResolver,
                                    uris
                                )
                                activity.startIntentSenderForResult(pendingIntent.intentSender, 1003, null, 0, 0, 0)
                                return@launch
                            } catch (_: Exception) {
                                // Fall through to direct physical deletion
                            }
                        }
                    }
                }
                try {
                    val title = context.getString(R.string.emptying_trash)
                    _batchOperationProgress.value = BatchProgress(title = title, current = 0, total = trashedItems.size)
                    trashedItems.forEachIndexed { index, item ->
                        _batchOperationProgress.value = BatchProgress(title = title, current = index + 1, total = trashedItems.size)
                        val trashedCandidates = trashDir.listFiles { _, name -> name.startsWith("${item.id}_") }
                        trashedCandidates?.forEach { tf ->
                            try {
                                if (secureShred) com.hrshd1eux.imava.core.util.FileShredderUtil.shredFile(tf)
                                else tf.delete()
                            } catch (_: Exception) {}
                        }
                        val srcFile = java.io.File(item.path)
                        if (srcFile.exists()) {
                            try {
                                if (secureShred) com.hrshd1eux.imava.core.util.FileShredderUtil.shredFile(srcFile)
                                else srcFile.delete()
                            } catch (_: Exception) {}
                        }
                        try {
                            context.contentResolver.delete(item.uri, null, null)
                        } catch (r: android.app.RecoverableSecurityException) {
                            throw r
                        } catch (_: Exception) {}
                        try {
                            val mediaUri = getMediaProviderUri(item)
                            context.contentResolver.delete(mediaUri, null, null)
                        } catch (r: android.app.RecoverableSecurityException) {
                            throw r
                        } catch (_: Exception) {}
                        try {
                            android.media.MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
                        } catch (_: Exception) {}
                        repository.deleteMetadataPermanently(item.id)
                        repository.evictSecondaryDiskItem(item.id, item.path)
                    }
                    refreshAll()
                } catch (e: Exception) {
                    val recoverable = e as? android.app.RecoverableSecurityException
                        ?: e.cause as? android.app.RecoverableSecurityException
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && recoverable != null) {
                        val activity = context.findActivity()
                        if (activity != null) {
                            pendingBatchActionItems = trashedItems
                            activity.startIntentSenderForResult(
                                recoverable.userAction.actionIntent.intentSender,
                                1003,
                                null,
                                0,
                                0,
                                0
                            )
                        }
                    } else {
                        e.printStackTrace()
                    }
                } finally {
                    _batchOperationProgress.value = null
                }
            }
        }
    }

    suspend fun getSelectedMediaItems(selectedIds: Set<Long>): List<MediaItem> = withContext(Dispatchers.IO) {
        if (selectedIds.isEmpty()) return@withContext emptyList()
        val repoItems = repository.getMediaByIds(selectedIds)
        if (repoItems.isNotEmpty()) return@withContext repoItems
        val visible = visibleMediaItems.value.filter { selectedIds.contains(it.id) }
        if (visible.isNotEmpty()) return@withContext visible
        val all = mediaItems.value.filter { selectedIds.contains(it.id) }
        if (all.isNotEmpty()) return@withContext all
        hidden.value.filter { selectedIds.contains(it.id) }
    }

    fun shareSelectedMedia(
        context: Context,
        privacyMode: com.hrshd1eux.imava.core.util.SharePrivacyMode = com.hrshd1eux.imava.core.util.SharePrivacyMode.STRIP_ALL
    ) {
        val selectedIds = selectionState.selectedIds.toSet()
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                val selectedList = getSelectedMediaItems(selectedIds)
                if (selectedList.isNotEmpty()) {
                    SharingUtils.shareMedia(context, selectedList, privacyMode)
                    selectionState.clear()
                }
            }
        }
    }

    fun shareSelectedMedia(context: Context, stripMetadata: Boolean) {
        shareSelectedMedia(
            context,
            if (stripMetadata) com.hrshd1eux.imava.core.util.SharePrivacyMode.STRIP_ALL else com.hrshd1eux.imava.core.util.SharePrivacyMode.FULL_METADATA
        )
    }

    fun shareAlbum(context: Context, bucketId: Long, bucketName: String? = null) {
        viewModelScope.launch {
            val albumMedia = repository.loadMediaPaged(
                limit = 2000,
                offset = 0,
                bucketId = bucketId,
                sortOrder = SortOrder.NEWEST_FIRST
            )
            if (albumMedia.isEmpty()) {
                val msg = if (!bucketName.isNullOrBlank()) {
                    localizedString(context, R.string.vm_no_photos_share_named_fmt, bucketName)
                } else {
                    localizedString(context, R.string.vm_no_photos_share_album)
                }
                safeToast(context, msg)
                return@launch
            }
            SharingUtils.shareMedia(
                context = context,
                items = albumMedia,
                privacyMode = com.hrshd1eux.imava.core.util.SharePrivacyMode.STRIP_ALL
            )
        }
    }

    fun hideSelectedMedia(context: Context) {
        val selectedIds = selectionState.selectedIds.toSet()
        hideMediaByIds(context, selectedIds)
    }

    fun hideMediaByIds(context: Context, ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val selectedItems = getSelectedMediaItems(ids)
            selectedItems.forEach { item ->
                toggleHidden(context, item)
            }
            selectionState.clear()
            refreshAll()
        }
    }

    fun moveSelectedMediaToFolder(context: Context, folderName: String) {
        val selectedIds = selectionState.selectedIds.toSet()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            val selectedItems = repository.getMediaByIds(selectedIds)
            if (selectedItems.isNotEmpty()) {
                moveMediaToFolder(context, selectedItems, folderName)
                selectionState.clear()
                refreshAll()
            }
        }
    }

    fun shareSingleMedia(
        context: Context,
        item: MediaItem,
        privacyMode: com.hrshd1eux.imava.core.util.SharePrivacyMode = com.hrshd1eux.imava.core.util.SharePrivacyMode.STRIP_ALL
    ) {
        viewModelScope.launch {
            SharingUtils.shareMedia(context, listOf(item), privacyMode)
        }
    }

    fun shareSingleMedia(context: Context, item: MediaItem, stripMetadata: Boolean) {
        shareSingleMedia(
            context,
            item,
            if (stripMetadata) com.hrshd1eux.imava.core.util.SharePrivacyMode.STRIP_ALL else com.hrshd1eux.imava.core.util.SharePrivacyMode.FULL_METADATA
        )
    }

    fun deletePermanently(context: Context, item: MediaItem, targetNextItem: MediaItem? = null) {
        _mediaItems.value = _mediaItems.value.filter { it.id != item.id }
        try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
        viewModelScope.launch {
            try {
                val hasAllFilesAccess = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager()
                val isDirectDiskFile = item.uri.scheme != android.content.ContentResolver.SCHEME_CONTENT || item.id <= 0
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !isDirectDiskFile && !hasAllFilesAccess) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingActionItem = item
                        pendingNextItem = targetNextItem
                        try {
                            val mediaUri = getMediaProviderUri(item)
                            val pendingIntent = android.provider.MediaStore.createDeleteRequest(
                                context.contentResolver,
                                listOf(mediaUri)
                            )
                            activity.startIntentSenderForResult(
                                pendingIntent.intentSender,
                                1001,
                                null,
                                0,
                                0,
                                0
                            )
                            return@launch
                        } catch (_: Exception) {
                            // If createDeleteRequest rejected the URI or failed, fall through to physical deletion
                        }
                    }
                }

                // Physical deletion fallback
                val secureShred = com.hrshd1eux.imava.core.util.FileShredderUtil.isSecureShredEnabled(context)
                val file = java.io.File(item.path)
                if (file.exists()) {
                    try {
                        if (secureShred) {
                            com.hrshd1eux.imava.core.util.FileShredderUtil.shredFile(file)
                        } else {
                            file.delete()
                        }
                    } catch (_: Exception) {}
                }
                val trashDir = java.io.File(context.filesDir, "imava_trash")
                val trashedFile = java.io.File(trashDir, "${item.id}_${file.name}")
                if (trashedFile.exists()) {
                    try {
                        if (secureShred) {
                            com.hrshd1eux.imava.core.util.FileShredderUtil.shredFile(trashedFile)
                        } else {
                            trashedFile.delete()
                        }
                    } catch (_: Exception) {}
                }
                try {
                    context.contentResolver.delete(item.uri, null, null)
                } catch (r: android.app.RecoverableSecurityException) {
                    throw r
                } catch (_: Exception) {}
                try {
                    val mediaUri = getMediaProviderUri(item)
                    context.contentResolver.delete(mediaUri, null, null)
                } catch (r: android.app.RecoverableSecurityException) {
                    throw r
                } catch (_: Exception) {}
                try {
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
                } catch (_: Exception) {}
                repository.deleteMetadataPermanently(item.id)
                repository.evictSecondaryDiskItem(item.id, item.path)
                advanceActiveMediaItem(item, targetNextItem)
                _lastDeletedMediaId.value = item.id
                pendingActionItem = null
                pendingNextItem = null
                refreshAll()
            } catch (e: Exception) {
                val recoverable = e as? android.app.RecoverableSecurityException
                    ?: e.cause as? android.app.RecoverableSecurityException
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && recoverable != null) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingActionItem = item
                        pendingNextItem = targetNextItem
                        activity.startIntentSenderForResult(
                            recoverable.userAction.actionIntent.intentSender,
                            1001,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                } else {
                    e.printStackTrace()
                }
            }
        }
    }

    var pendingBatchActionItems: List<MediaItem>? = null

    fun deleteSelectedMedia(context: Context) {
        val selectedIds = selectionState.selectedIds.toSet()
        if (selectedIds.isEmpty()) return

        // Instant UI feedback: dismiss selection and remove items immediately (0ms perceived latency)
        selectionState.clear()
        _mediaItems.value = _mediaItems.value.filter { it.id !in selectedIds }
        try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}

        viewModelScope.launch {
            val visibleMap = visibleMediaItems.value.associateBy { it.id }
            val resolvedItems = selectedIds.mapNotNull { visibleMap[it] }.toMutableList()
            val missingIds = selectedIds - resolvedItems.map { it.id }.toSet()
            if (missingIds.isNotEmpty()) {
                val dbItems = repository.getMediaByIds(missingIds)
                resolvedItems.addAll(dbItems)
            }

            if (resolvedItems.isEmpty()) {
                selectedIds.forEach { id ->
                    repository.deleteMetadataPermanently(id)
                }
                refreshAll()
                return@launch
            }

            val (brokenOrphanItems, validItems) = resolvedItems.partition { item ->
                val srcFile = java.io.File(item.path)
                !srcFile.exists() || (srcFile.isFile && srcFile.length() == 0L)
            }

            if (brokenOrphanItems.isNotEmpty()) {
                brokenOrphanItems.forEach { orphan ->
                    // Mark as trashed in Room DB so it is filtered out everywhere in the UI
                    repository.toggleTrashed(orphan)
                    repository.evictSecondaryDiskItem(orphan.id, orphan.path)
                    try { context.contentResolver.delete(orphan.uri, null, null) } catch (_: Exception) {}
                    try {
                        val mediaUri = getMediaProviderUri(orphan)
                        context.contentResolver.delete(mediaUri, null, null)
                    } catch (_: Exception) {}
                    try {
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(orphan.path), null, null)
                    } catch (_: Exception) {}
                }
            }

            if (validItems.isEmpty()) {
                refreshAll()
                safeToast(context, localizedString(context, R.string.vm_moved_broken_trash_fmt, brokenOrphanItems.size))
                return@launch
            }

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingBatchActionItems = validItems
                        try {
                            val uris = validItems.map { getMediaProviderUri(it) }
                            val pendingIntent = android.provider.MediaStore.createTrashRequest(
                                context.contentResolver,
                                uris,
                                true
                            )
                            activity.startIntentSenderForResult(
                                pendingIntent.intentSender,
                                1005,
                                null,
                                0,
                                0,
                                0
                            )
                            return@launch
                        } catch (_: Exception) {
                            // If createTrashRequest fails for batch, fall back to direct trashing
                        }
                    }
                }

                val total = validItems.size
                val title = context.getString(R.string.deleting_media)
                if (total > 1) {
                    _batchOperationProgress.value = BatchProgress(title = title, current = 0, total = total)
                }
                val trashDir = java.io.File(context.filesDir, "imava_trash").apply { mkdirs() }
                validItems.forEachIndexed { index, item ->
                    if (total > 1) {
                        _batchOperationProgress.value = BatchProgress(title = title, current = index + 1, total = total)
                    }
                    val srcFile = java.io.File(item.path)
                    if (srcFile.exists()) {
                        val destFile = java.io.File(trashDir, "${item.id}_${srcFile.name}")
                        try {
                            if (!srcFile.renameTo(destFile)) {
                                srcFile.copyTo(destFile, overwrite = true)
                                srcFile.delete()
                            }
                        } catch (_: Exception) {}
                    }
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                    } catch (r: android.app.RecoverableSecurityException) {
                        throw r
                    } catch (_: Exception) {}
                    try {
                        val mediaUri = getMediaProviderUri(item)
                        context.contentResolver.delete(mediaUri, null, null)
                    } catch (r: android.app.RecoverableSecurityException) {
                        throw r
                    } catch (_: Exception) {}
                    try {
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
                    } catch (_: Exception) {}
                    repository.toggleTrashed(item)
                    repository.evictSecondaryDiskItem(item.id, item.path)
                }
                refreshAll()
            } catch (e: Exception) {
                val recoverable = e as? android.app.RecoverableSecurityException
                    ?: e.cause as? android.app.RecoverableSecurityException
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && recoverable != null) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingBatchActionItems = validItems
                        activity.startIntentSenderForResult(
                            recoverable.userAction.actionIntent.intentSender,
                            1005,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                } else {
                    e.printStackTrace()
                }
            } finally {
                _batchOperationProgress.value = null
            }
        }
    }

    fun deleteMediaItems(context: Context, items: List<MediaItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val (brokenOrphanItems, validItems) = items.partition { item ->
                val srcFile = java.io.File(item.path)
                !srcFile.exists() || (srcFile.isFile && srcFile.length() == 0L)
            }
            try {
                if (brokenOrphanItems.isNotEmpty()) {
                    brokenOrphanItems.forEach { orphan ->
                        repository.toggleTrashed(orphan)
                        repository.evictSecondaryDiskItem(orphan.id, orphan.path)
                        try { context.contentResolver.delete(orphan.uri, null, null) } catch (_: Exception) {}
                        try {
                            val mediaUri = getMediaProviderUri(orphan)
                            context.contentResolver.delete(mediaUri, null, null)
                        } catch (_: Exception) {}
                        try {
                            android.media.MediaScannerConnection.scanFile(context, arrayOf(orphan.path), null, null)
                        } catch (_: Exception) {}
                    }
                    val orphanIds = brokenOrphanItems.map { it.id }.toSet()
                    _mediaItems.value = _mediaItems.value.filter { it.id !in orphanIds }
                }

                if (validItems.isEmpty()) {
                    refreshAll()
                    return@launch
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingBatchActionItems = validItems
                        try {
                            val uris = validItems.map { getMediaProviderUri(it) }
                            val pendingIntent = android.provider.MediaStore.createTrashRequest(
                                context.contentResolver,
                                uris,
                                true
                            )
                            activity.startIntentSenderForResult(
                                pendingIntent.intentSender,
                                1005,
                                null,
                                0,
                                0,
                                0
                            )
                            return@launch
                        } catch (_: Exception) {
                            // Fall through to direct trashing
                        }
                    }
                }
                val total = validItems.size
                val title = context.getString(R.string.deleting_media)
                if (total > 1) {
                    _batchOperationProgress.value = BatchProgress(title = title, current = 0, total = total)
                }
                val trashDir = java.io.File(context.filesDir, "imava_trash").apply { mkdirs() }
                validItems.forEachIndexed { index, item ->
                    if (total > 1) {
                        _batchOperationProgress.value = BatchProgress(title = title, current = index + 1, total = total)
                    }
                    val srcFile = java.io.File(item.path)
                    if (srcFile.exists()) {
                        val destFile = java.io.File(trashDir, "${item.id}_${srcFile.name}")
                        try {
                            srcFile.copyTo(destFile, overwrite = true)
                            srcFile.delete()
                        } catch (_: Exception) {}
                    }
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                    } catch (r: android.app.RecoverableSecurityException) {
                        throw r
                    } catch (_: Exception) {}
                    try {
                        val mediaUri = getMediaProviderUri(item)
                        context.contentResolver.delete(mediaUri, null, null)
                    } catch (r: android.app.RecoverableSecurityException) {
                        throw r
                    } catch (_: Exception) {}
                    try {
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
                    } catch (_: Exception) {}
                    repository.toggleTrashed(item)
                    repository.evictSecondaryDiskItem(item.id, item.path)
                }
                refreshAll()
            } catch (e: Exception) {
                val recoverable = e as? android.app.RecoverableSecurityException
                    ?: e.cause as? android.app.RecoverableSecurityException
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && recoverable != null) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingBatchActionItems = validItems
                        activity.startIntentSenderForResult(
                            recoverable.userAction.actionIntent.intentSender,
                            1005,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                } else {
                    e.printStackTrace()
                }
            } finally {
                _batchOperationProgress.value = null
            }
        }
    }

    fun restoreSelectedMedia(context: Context) {
        val selectedIds = selectionState.selectedIds.toSet()
        if (selectedIds.isEmpty()) return
        val trashedItems = trashed.value.filter { selectedIds.contains(it.id) }
        if (trashedItems.isEmpty()) return
        viewModelScope.launch {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingBatchActionItems = trashedItems
                        try {
                            val uris = trashedItems.map { getMediaProviderUri(it) }
                            val pendingIntent = android.provider.MediaStore.createTrashRequest(
                                context.contentResolver,
                                uris,
                                false
                            )
                            activity.startIntentSenderForResult(
                                pendingIntent.intentSender,
                                1005,
                                null,
                                0,
                                0,
                                0
                            )
                            return@launch
                        } catch (_: Exception) {
                            // Fall through to direct restore
                        }
                    }
                }
                val trashDir = java.io.File(context.filesDir, "imava_trash")
                trashedItems.forEach { item ->
                    val trashedFile = java.io.File(trashDir, "${item.id}_${java.io.File(item.path).name}")
                    val origFile = java.io.File(item.path)
                    if (trashedFile.exists()) {
                        try {
                            origFile.parentFile?.mkdirs()
                            trashedFile.copyTo(origFile, overwrite = true)
                            trashedFile.delete()
                        } catch (_: Exception) {}
                    }
                    try {
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
                    } catch (_: Exception) {}
                    repository.toggleTrashed(item)
                    repository.evictSecondaryDiskItem(item.id, item.path)
                }
                selectionState.clear()
                refreshAll()
                try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                safeToast(context, localizedString(context, R.string.vm_restored_items_toast_fmt, trashedItems.size))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSelectedMediaPermanently(context: Context) {
        val selectedIds = selectionState.selectedIds.toSet()
        if (selectedIds.isEmpty()) return

        // Instant UI feedback: dismiss selection and remove items immediately (0ms perceived latency)
        selectionState.clear()
        _mediaItems.value = _mediaItems.value.filter { it.id !in selectedIds }
        try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}

        viewModelScope.launch {
            val selectedItems = trashed.value.filter { selectedIds.contains(it.id) }.ifEmpty {
                repository.getMediaByIds(selectedIds)
            }
            if (selectedItems.isEmpty()) {
                selectedIds.forEach { id ->
                    repository.deleteMetadataPermanently(id)
                    repository.evictSecondaryDiskItem(id, "")
                }
                selectionState.clear()
                refreshAll()
                return@launch
            }
            val trashDir = java.io.File(context.filesDir, "imava_trash")
            val (brokenOrphans, validItems) = selectedItems.partition { item ->
                val file = java.io.File(item.path)
                val trashedFile = java.io.File(trashDir, "${item.id}_${file.name}")
                !file.exists() && !trashedFile.exists() && trashDir.listFiles { _, name -> name.startsWith("${item.id}_") }.isNullOrEmpty()
            }

            try {
                if (brokenOrphans.isNotEmpty()) {
                    brokenOrphans.forEach { orphan ->
                        repository.deleteMetadataPermanently(orphan.id)
                        repository.evictSecondaryDiskItem(orphan.id, orphan.path)
                        try { context.contentResolver.delete(orphan.uri, null, null) } catch (_: Exception) {}
                        try { context.contentResolver.delete(getMediaProviderUri(orphan), null, null) } catch (_: Exception) {}
                        try { android.media.MediaScannerConnection.scanFile(context, arrayOf(orphan.path), null, null) } catch (_: Exception) {}
                    }
                    val orphanIds = brokenOrphans.map { it.id }.toSet()
                    _mediaItems.value = _mediaItems.value.filter { it.id !in orphanIds }
                    selectionState.deselectAll(orphanIds)
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                    val activity = context.findActivity()
                    val mediaStoreItems = validItems.filter { it.id > 0 && it.uri.scheme == android.content.ContentResolver.SCHEME_CONTENT }
                    if (activity != null && mediaStoreItems.isNotEmpty()) {
                        pendingBatchActionItems = validItems
                        try {
                            val uris = mediaStoreItems.map { getMediaProviderUri(it) }
                            val pendingIntent = android.provider.MediaStore.createDeleteRequest(
                                context.contentResolver,
                                uris
                            )
                            activity.startIntentSenderForResult(
                                pendingIntent.intentSender,
                                1001,
                                null,
                                0,
                                0,
                                0
                            )
                            return@launch
                        } catch (_: Exception) {
                            // Fall back to physical deletion
                        }
                    }
                }

                if (validItems.isEmpty()) {
                    selectionState.clear()
                    refreshAll()
                    try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                    safeToast(context, localizedString(context, R.string.vm_removed_broken_perm_fmt, brokenOrphans.size))
                    return@launch
                }

                val total = validItems.size
                val title = context.getString(R.string.deleting_media)
                if (total > 1) {
                    _batchOperationProgress.value = BatchProgress(title = title, current = 0, total = total)
                }
                val secureShred = com.hrshd1eux.imava.core.util.FileShredderUtil.isSecureShredEnabled(context)
                validItems.forEachIndexed { index, item ->
                    if (total > 1) {
                        _batchOperationProgress.value = BatchProgress(title = title, current = index + 1, total = total)
                    }
                    val file = java.io.File(item.path)
                    if (file.exists()) {
                        try {
                            if (secureShred) {
                                com.hrshd1eux.imava.core.util.FileShredderUtil.shredFile(file)
                            } else {
                                file.delete()
                            }
                        } catch (_: Exception) {}
                    }
                    val trashedCandidates = trashDir.listFiles { _, name -> name.startsWith("${item.id}_") }
                    trashedCandidates?.forEach { tf ->
                        try {
                            if (secureShred) {
                                com.hrshd1eux.imava.core.util.FileShredderUtil.shredFile(tf)
                            } else {
                                tf.delete()
                            }
                        } catch (_: Exception) {}
                    }
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                    } catch (r: android.app.RecoverableSecurityException) {
                        throw r
                    } catch (_: Exception) {}
                    try {
                        val mediaUri = getMediaProviderUri(item)
                        context.contentResolver.delete(mediaUri, null, null)
                    } catch (r: android.app.RecoverableSecurityException) {
                        throw r
                    } catch (_: Exception) {}
                    try {
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null)
                    } catch (_: Exception) {}
                    repository.deleteMetadataPermanently(item.id)
                    repository.evictSecondaryDiskItem(item.id, item.path)
                }
                selectionState.clear()
                refreshAll()
            } catch (e: Exception) {
                val recoverable = e as? android.app.RecoverableSecurityException
                    ?: e.cause as? android.app.RecoverableSecurityException
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && recoverable != null) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingBatchActionItems = validItems
                        activity.startIntentSenderForResult(
                            recoverable.userAction.actionIntent.intentSender,
                            1001,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                } else {
                    e.printStackTrace()
                }
            } finally {
                _batchOperationProgress.value = null
            }
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, context: Context? = null) {
        val item = pendingActionItem
        val batchItems = pendingBatchActionItems

        if (resultCode == android.app.Activity.RESULT_OK) {
            when (requestCode) {
                1001, 1003 -> {
                    viewModelScope.launch {
                        val trashDir = context?.let { java.io.File(it.filesDir, "imava_trash") }
                        val secureShred = context?.let { com.hrshd1eux.imava.core.util.FileShredderUtil.isSecureShredEnabled(it) } == true
                        val targets = if (batchItems != null) batchItems else if (item != null) listOf(item) else emptyList()
                        val title = localizedString(
                            context,
                            if (requestCode == 1003) R.string.emptying_trash else R.string.deleting_media
                        )
                        if (targets.size > 1) {
                            _batchOperationProgress.value = BatchProgress(title = title, current = 0, total = targets.size)
                        }

                        try {
                            targets.forEachIndexed { index, target ->
                                if (targets.size > 1) {
                                    _batchOperationProgress.value = BatchProgress(title = title, current = index + 1, total = targets.size)
                                }
                                // 1. Delete/shred from imava_trash
                                if (trashDir != null && trashDir.exists()) {
                                    trashDir.listFiles { _, name -> name.startsWith("${target.id}_") }?.forEach { tf ->
                                        try {
                                            if (secureShred) com.hrshd1eux.imava.core.util.FileShredderUtil.shredFile(tf)
                                            else tf.delete()
                                        } catch (_: Exception) {}
                                    }
                                }
                                // 2. Delete/shred original file on disk if exists
                                val src = java.io.File(target.path)
                                if (src.exists()) {
                                    try {
                                        if (secureShred) com.hrshd1eux.imava.core.util.FileShredderUtil.shredFile(src)
                                        else src.delete()
                                    } catch (_: Exception) {}
                                }
                                // 3. Delete from ContentResolver / MediaStore
                                if (context != null) {
                                    try { context.contentResolver.delete(target.uri, null, null) } catch (_: Exception) {}
                                    try { context.contentResolver.delete(getMediaProviderUri(target), null, null) } catch (_: Exception) {}
                                    try { android.media.MediaScannerConnection.scanFile(context, arrayOf(target.path), null, null) } catch (_: Exception) {}
                                }
                                // 4. Room DB and secondary disk eviction
                                repository.deleteMetadataPermanently(target.id)
                                repository.evictSecondaryDiskItem(target.id, target.path)
                            }

                            if (batchItems != null) {
                                selectionState.clear()
                                pendingBatchActionItems = null
                            }
                            if (item != null) {
                                advanceActiveMediaItem(item, pendingNextItem)
                                _lastDeletedMediaId.value = item.id
                                pendingActionItem = null
                                pendingNextItem = null
                            }
                            refreshAll()
                        } finally {
                            _batchOperationProgress.value = null
                        }
                    }
                }
                1002 -> {
                    if (item != null) {
                        viewModelScope.launch {
                            val trashDir = context?.let { java.io.File(it.filesDir, "imava_trash").apply { mkdirs() } }
                            if (!item.isTrashed) {
                                val srcFile = java.io.File(item.path)
                                if (srcFile.exists() && trashDir != null) {
                                    val destFile = java.io.File(trashDir, "${item.id}_${srcFile.name}")
                                    try {
                                        if (!srcFile.renameTo(destFile)) {
                                            srcFile.copyTo(destFile, overwrite = true)
                                            srcFile.delete()
                                        }
                                    } catch (_: Exception) {}
                                }
                                if (context != null) {
                                    try { context.contentResolver.delete(item.uri, null, null) } catch (_: Exception) {}
                                    try { context.contentResolver.delete(getMediaProviderUri(item), null, null) } catch (_: Exception) {}
                                    try { android.media.MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null) } catch (_: Exception) {}
                                }
                            } else {
                                if (trashDir != null && trashDir.exists()) {
                                    val trashedCandidates = trashDir.listFiles { _, name -> name.startsWith("${item.id}_") }
                                    val origFile = java.io.File(item.path)
                                    trashedCandidates?.forEach { tf ->
                                        if (tf.exists()) {
                                            try {
                                                origFile.parentFile?.mkdirs()
                                                if (!tf.renameTo(origFile)) {
                                                    tf.copyTo(origFile, overwrite = true)
                                                    tf.delete()
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }
                                }
                                if (context != null) {
                                    try { android.media.MediaScannerConnection.scanFile(context, arrayOf(item.path), null, null) } catch (_: Exception) {}
                                }
                            }
                            repository.toggleTrashed(item)
                            repository.evictSecondaryDiskItem(item.id, item.path)
                            advanceActiveMediaItem(item, pendingNextItem)
                            _lastDeletedMediaId.value = item.id
                            pendingActionItem = null
                            pendingNextItem = null
                            refreshAll()
                        }
                    }
                }
                1004 -> {
                    pendingActionItem = null
                    refreshAll()
                }
                1005 -> {
                    if (batchItems != null) {
                        viewModelScope.launch {
                            val count = batchItems.size
                            val isCurrentlyTrashed = batchItems.firstOrNull()?.isTrashed == true
                            val title = localizedString(context, if (isCurrentlyTrashed) R.string.vm_restoring_media else R.string.deleting_media)
                            if (count > 1) {
                                _batchOperationProgress.value = BatchProgress(title = title, current = 0, total = count)
                            }
                            val trashDir = context?.let { java.io.File(it.filesDir, "imava_trash").apply { mkdirs() } }
                            try {
                                batchItems.forEachIndexed { index, batchItem ->
                                    if (count > 1) {
                                        _batchOperationProgress.value = BatchProgress(title = title, current = index + 1, total = count)
                                    }
                                    if (!isCurrentlyTrashed) {
                                        val srcFile = java.io.File(batchItem.path)
                                        if (srcFile.exists() && trashDir != null) {
                                            val destFile = java.io.File(trashDir, "${batchItem.id}_${srcFile.name}")
                                            try {
                                                if (!srcFile.renameTo(destFile)) {
                                                    srcFile.copyTo(destFile, overwrite = true)
                                                    srcFile.delete()
                                                }
                                            } catch (_: Exception) {}
                                        }
                                        if (context != null) {
                                            try { context.contentResolver.delete(batchItem.uri, null, null) } catch (_: Exception) {}
                                            try { context.contentResolver.delete(getMediaProviderUri(batchItem), null, null) } catch (_: Exception) {}
                                            try { android.media.MediaScannerConnection.scanFile(context, arrayOf(batchItem.path), null, null) } catch (_: Exception) {}
                                        }
                                    } else {
                                        if (trashDir != null && trashDir.exists()) {
                                            val trashedCandidates = trashDir.listFiles { _, name -> name.startsWith("${batchItem.id}_") }
                                            val origFile = java.io.File(batchItem.path)
                                            trashedCandidates?.forEach { tf ->
                                                if (tf.exists()) {
                                                    try {
                                                        origFile.parentFile?.mkdirs()
                                                        if (!tf.renameTo(origFile)) {
                                                            tf.copyTo(origFile, overwrite = true)
                                                            tf.delete()
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        }
                                        if (context != null) {
                                            try { android.media.MediaScannerConnection.scanFile(context, arrayOf(batchItem.path), null, null) } catch (_: Exception) {}
                                        }
                                    }
                                    repository.toggleTrashed(batchItem)
                                    repository.evictSecondaryDiskItem(batchItem.id, batchItem.path)
                                }
                                selectionState.clear()
                                pendingBatchActionItems = null
                                refreshAll()
                                if (context != null) {
                                    try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                                    val msg = localizedString(
                                        context,
                                        if (isCurrentlyTrashed) R.string.vm_restored_items_toast_fmt else R.string.doctor_moved_trash_toast_fmt,
                                        count
                                    )
                                    safeToast(context, msg)
                                }
                            } finally {
                                _batchOperationProgress.value = null
                            }
                        }
                    } else {
                        refreshAll()
                    }
                }
                1006 -> {
                    val renameItem = pendingRenameItem
                    val renameName = pendingRenameName
                    if (renameItem != null && renameName != null && context != null) {
                        viewModelScope.launch {
                            try {
                                val success = repository.renameMedia(context, renameItem, renameName)
                                if (success) {
                                    applyRenamedState(renameItem, renameName)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                pendingRenameItem = null
                                pendingRenameName = null
                            }
                        }
                    } else {
                        pendingRenameItem = null
                        pendingRenameName = null
                    }
                }
                1007 -> {
                    val sourceItems = pendingMoveSourceItems
                    val rollbackTargets = pendingMoveRollbackTargets
                    val onCompleteCallback = pendingMoveOnComplete
                    if (sourceItems != null) {
                        viewModelScope.launch {
                            val scanned = mutableListOf<String>()
                            sourceItems.forEach { src ->
                                repository.deleteMetadataPermanently(src.id)
                                scanned.add(src.path)
                            }
                            rollbackTargets?.forEach { tgt ->
                                scanned.add(tgt.absolutePath)
                            }
                            if (context != null && scanned.isNotEmpty()) {
                                android.media.MediaScannerConnection.scanFile(
                                    context,
                                    scanned.toTypedArray(),
                                    null,
                                    null
                                )
                            }
                            if (activeMediaItem != null && sourceItems.any { it.id == activeMediaItem?.id }) {
                                val currentActive = activeMediaItem
                                if (currentActive != null) {
                                    advanceActiveMediaItem(currentActive)
                                }
                            }
                            pendingMoveSourceItems = null
                            pendingMoveRollbackTargets = null
                            pendingMoveOnComplete = null
                            loadBuckets()
                            refreshAll()
                            if (context != null) {
                                try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                                safeToast(context, localizedString(context, R.string.vm_moved_success_fmt, sourceItems.size))
                            }
                            onCompleteCallback?.invoke(sourceItems.size)
                        }
                    }
                }
                1008 -> {
                    val renames = pendingBatchRenames
                    if (renames != null && context != null) {
                        viewModelScope.launch {
                            val count = repository.batchRenameMedia(context, renames)
                            selectionState.clear()
                            pendingBatchRenames = null
                            refreshAll()
                            if (count > 0) {
                                try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                                safeToast(context, localizedString(context, R.string.vm_renamed_items_toast_fmt, count))
                            }
                        }
                    } else {
                        pendingBatchRenames = null
                        selectionState.clear()
                        refreshAll()
                    }
                }
                1009 -> {
                    val rotateTarget = pendingRotateItem
                    val clockwise = pendingRotateClockwise
                    if (rotateTarget != null && context != null) {
                        rotateMediaLosslessly(context, rotateTarget, clockwise)
                    }
                    pendingRotateItem = null
                }
            }
        } else {
            // Cancelled flow
            if (requestCode == 1004 && item != null) {
                // Rollback: delete vault file and Room entry
                viewModelScope.launch {
                    repository.deleteMetadataPermanently(item.id)
                    pendingActionItem = null
                    refreshAll()
                }
            } else if (requestCode == 1007) {
                // Rollback: delete newly copied target files so no duplicate is created
                viewModelScope.launch {
                    pendingMoveRollbackTargets?.forEach { tgt ->
                        try { tgt.delete() } catch (_: Exception) {}
                    }
                    pendingMoveSourceItems = null
                    pendingMoveRollbackTargets = null
                    pendingMoveOnComplete = null
                    loadBuckets()
                    refreshAll()
                    if (context != null) {
                        safeToast(context, localizedString(context, R.string.vm_move_cancelled_perm_denied))
                    }
                }
            } else {
                if (pendingActionItem != null && activeMediaItem?.id == pendingNextItem?.id) {
                    activeMediaItem = pendingActionItem
                }
                pendingActionItem = null
                pendingNextItem = null
                pendingBatchActionItems = null
                pendingRenameItem = null
                pendingRenameName = null
                pendingBatchRenames = null
                refreshAll()
            }
        }
    }

    internal fun matchesFilter(item: MediaItem, filter: MediaFilterType): Boolean {
        return when (filter) {
            MediaFilterType.ALL -> true
            MediaFilterType.PHOTOS -> !item.isVideo && !item.mimeType.contains("gif", ignoreCase = true)
            MediaFilterType.VIDEOS -> item.isVideo
            MediaFilterType.SCREENSHOTS -> item.path.contains("screenshot", ignoreCase = true) || item.bucketName.contains("screenshot", ignoreCase = true)
            MediaFilterType.FAVORITES -> item.isFavorite
            MediaFilterType.GIFS -> item.mimeType.contains("gif", ignoreCase = true) || item.path.endsWith(".gif", ignoreCase = true)
            MediaFilterType.RAW -> {
                val p = item.path.lowercase()
                com.hrshd1eux.imava.core.util.RawPreviewDecoder.RAW_EXTENSIONS.any { p.endsWith(".$it") } ||
                        com.hrshd1eux.imava.core.util.RawPreviewDecoder.RAW_MIME_PREFIXES.any { item.mimeType.startsWith(it, ignoreCase = true) }
            }
        }
    }

    fun rotateMediaLosslessly(context: Context, item: MediaItem, clockwise: Boolean = true) {
        viewModelScope.launch {
            try {
                val success = com.hrshd1eux.imava.core.util.LosslessRotationUtil.rotateLosslessly(context, item, clockwise)
                if (success) {
                    com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                    refreshAll()
                    // Update activeMediaItem if it matches
                    if (activeMediaItem?.id == item.id) {
                        activeMediaItem = repository.getMediaByIds(setOf(item.id)).firstOrNull() ?: item
                    }
                } else {
                    try { com.hrshd1eux.imava.core.util.HapticUtil.performError(context) } catch (_: Exception) {}
                    safeToast(context, localizedString(context, R.string.vm_could_not_rotate))
                }
            } catch (e: Exception) {
                val recoverable = e as? android.app.RecoverableSecurityException
                    ?: e.cause as? android.app.RecoverableSecurityException
                val isSec = e is SecurityException || e.cause is SecurityException
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && (isSec || recoverable != null)) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingRotateItem = item
                        pendingRotateClockwise = clockwise
                        val pendingIntent = android.provider.MediaStore.createWriteRequest(
                            context.contentResolver,
                            listOf(item.uri)
                        )
                        activity.startIntentSenderForResult(
                            pendingIntent.intentSender,
                            1009,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && recoverable != null) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingRotateItem = item
                        pendingRotateClockwise = clockwise
                        activity.startIntentSenderForResult(
                            recoverable.userAction.actionIntent.intentSender,
                            1009,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                } else {
                    try { com.hrshd1eux.imava.core.util.HapticUtil.performError(context) } catch (_: Exception) {}
                    safeToast(context, localizedString(context, R.string.vm_could_not_rotate_fmt, e.localizedMessage ?: ""))
                }
            }
        }
    }

    fun exportCleanCopy(context: Context, item: MediaItem) {
        viewModelScope.launch {
            safeToast(context, localizedString(context, R.string.vm_sanitizing_single))
            val file = com.hrshd1eux.imava.core.util.ExifSanitizerUtil.exportCleanCopy(context, item)
            if (file != null) {
                try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                safeToast(context, localizedString(context, R.string.vm_saved_clean_single), android.widget.Toast.LENGTH_LONG)
                refreshAll()
            } else {
                try { com.hrshd1eux.imava.core.util.HapticUtil.performError(context) } catch (_: Exception) {}
                safeToast(context, localizedString(context, R.string.vm_failed_clean_single))
            }
        }
    }

    fun exportCleanCopies(context: Context, items: List<MediaItem>) {
        viewModelScope.launch {
            safeToast(context, localizedString(context, R.string.vm_sanitizing_many_fmt, items.size))
            var count = 0
            for (item in items) {
                val f = com.hrshd1eux.imava.core.util.ExifSanitizerUtil.exportCleanCopy(context, item)
                if (f != null) count++
            }
            if (count > 0) {
                try { com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context) } catch (_: Exception) {}
                safeToast(context, localizedString(context, R.string.vm_saved_clean_many_fmt, count), android.widget.Toast.LENGTH_LONG)
                selectionState.clear()
                refreshAll()
            } else {
                try { com.hrshd1eux.imava.core.util.HapticUtil.performError(context) } catch (_: Exception) {}
                safeToast(context, localizedString(context, R.string.vm_failed_clean_many))
            }
        }
    }

    fun scrubAlbumMetadata(
        context: Context,
        bucketId: Long,
        options: com.hrshd1eux.imava.core.util.ExifSanitizerUtil.ScrubOptions,
        onProgress: (completed: Int, total: Int) -> Unit,
        onComplete: (scrubbedCount: Int) -> Unit
    ) {
        viewModelScope.launch {
            val itemsInAlbum = visibleMediaItems.value.filter { it.bucketId == bucketId }
            val count = com.hrshd1eux.imava.core.util.ExifSanitizerUtil.scrubItemsBatch(
                context = context,
                items = itemsInAlbum,
                options = options,
                onProgress = onProgress
            )
            refreshAll()
            onComplete(count)
        }
    }
}

enum class Screen {
    Photos, Albums, Search, Settings, DuplicateFinder, AboutLegal, PermissionsTransparency, OpenSourceLicenses, VaultSetup, VaultSettings
}

enum class SortOrder {
    NEWEST_FIRST, OLDEST_FIRST
}
