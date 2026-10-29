package com.hrshd1eux.imava.ui.timeline

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.hrshd1eux.imava.ui.MediaFilterType
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberUpdatedState
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import android.content.Context
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.model.formattedDuration
import com.hrshd1eux.imava.data.model.isVideo
import com.hrshd1eux.imava.data.model.TimelineItem
import com.hrshd1eux.imava.ui.MainViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val lazyPagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()
    val context = LocalContext.current
    val selectionState = viewModel.selectionState

    val visibleItems by viewModel.visibleMediaItems.collectAsState()
    val currentCategory by viewModel.currentCategoryNameFlow.collectAsState()
    val headerToMediaIdsMap = remember(visibleItems, currentCategory) {
        visibleItems.groupBy(
            keySelector = { viewModel.getHeaderTitleForMedia(it, currentCategory) },
            valueTransform = { it.id }
        )
    }

    var zoomAccumulator by remember { mutableFloatStateOf(1f) }

    val gridState = rememberLazyStaggeredGridState()

    var lastTopIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lastTopOffset by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        lastTopIndex = gridState.firstVisibleItemIndex
        lastTopOffset = gridState.firstVisibleItemScrollOffset
    }

    LaunchedEffect(viewModel.gridColumnCount) {
        if (lastTopIndex > 0) {
            gridState.scrollToItem(lastTopIndex, lastTopOffset)
        }
    }

    val dragSelectionModifier = if (selectionState.inSelectionMode) {
        Modifier.pointerInput(selectionState.inSelectionMode) {
            detectDragGestures(
                onDragStart = { offset ->
                    val hitItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                        offset.x >= info.offset.x && offset.x <= (info.offset.x + info.size.width) &&
                        offset.y >= info.offset.y && offset.y <= (info.offset.y + info.size.height)
                    }
                    if (hitItem != null && hitItem.index in 0 until lazyPagingItems.itemCount) {
                        val timelineItem = lazyPagingItems.peek(hitItem.index)
                        if (timelineItem is TimelineItem.Media) {
                            selectionState.select(timelineItem.item.id)
                            com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                        }
                    }
                },
                onDrag = { change, _ ->
                    change.consume()
                    val offset = change.position
                    val hitItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                        offset.x >= info.offset.x && offset.x <= (info.offset.x + info.size.width) &&
                        offset.y >= info.offset.y && offset.y <= (info.offset.y + info.size.height)
                    }
                    if (hitItem != null && hitItem.index in 0 until lazyPagingItems.itemCount) {
                        val timelineItem = lazyPagingItems.peek(hitItem.index)
                        if (timelineItem is TimelineItem.Media && !selectionState.selectedIds.contains(timelineItem.item.id)) {
                            selectionState.select(timelineItem.item.id)
                            com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                        }
                    }
                }
            )
        }
    } else {
        Modifier
    }

    val pinchGestureModifier = if (!selectionState.inSelectionMode) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val activePointers = event.changes.filter { it.pressed }

                    if (activePointers.size >= 2) {
                        val zoom = event.calculateZoom()
                        if (kotlin.math.abs(zoom - 1f) > 0.001f) {
                            event.changes.forEach { it.consume() }
                            zoomAccumulator *= zoom

                            // Spreading fingers (zooming in -> larger photos -> fewer columns)
                            if (zoomAccumulator > 1.25f) {
                                if (viewModel.gridColumnCount > 1) {
                                    viewModel.increaseZoom()
                                    com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                }
                                zoomAccumulator = 1f
                            }
                            // Pinching together (zooming out -> smaller photos -> more columns)
                            else if (zoomAccumulator < 0.80f) {
                                if (viewModel.gridColumnCount < 6) {
                                    viewModel.decreaseZoom()
                                    com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                }
                                zoomAccumulator = 1f
                            }
                        }
                    } else {
                        zoomAccumulator = 1f
                    }
                }
            }
        }
    } else Modifier

    val currentFilter by viewModel.selectedFilter.collectAsState()
    val showFilterChips = !selectionState.inSelectionMode && viewModel.currentCategoryName == null && viewModel.currentBucketId == null
    val hasInitiallyLoaded by viewModel.hasAppInitiallyLoaded.collectAsState()

    LaunchedEffect(lazyPagingItems.itemCount) {
        if (lazyPagingItems.itemCount > 0 && !viewModel.hasAppInitiallyLoaded.value) {
            viewModel.hasAppInitiallyLoaded.value = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .then(pinchGestureModifier)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val selectedCustomTag by viewModel.selectedCustomTag.collectAsState()
            if (showFilterChips) {
                val distinctTags by viewModel.allDistinctTags.collectAsState()
                TimelineFilterChips(
                    selectedFilter = currentFilter,
                    selectedCustomTag = selectedCustomTag,
                    availableTags = distinctTags,
                    onFilterSelected = { filter ->
                        viewModel.setSelectedFilter(filter)
                        com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                    },
                    onCustomTagSelected = { tag ->
                        viewModel.setSelectedCustomTag(tag)
                        com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                    }
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val isPagingLoading = lazyPagingItems.loadState.refresh is androidx.paging.LoadState.Loading
                if (lazyPagingItems.itemCount == 0) {
                    if (!hasInitiallyLoaded && isPagingLoading) {
                        TimelineSkeletonGrid(columns = viewModel.gridColumnCount)
                    } else if (!isPagingLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyMessage = when (viewModel.currentCategoryName) {
                                "Favorites" -> "No favorite items yet"
                                "Videos" -> "No videos found on device"
                                "Trash" -> "Trash is empty"
                                "Hidden Vault" -> "Vault is empty"
                                else -> if (currentFilter != MediaFilterType.ALL || selectedCustomTag != null) "No media matches the selected filter" else "No media found on device"
                            }
                            Text(
                                text = emptyMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(viewModel.gridColumnCount),
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = 2.dp,
                            end = 2.dp,
                            top = 8.dp,
                            bottom = 100.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalItemSpacing = 2.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(dragSelectionModifier)
                    ) {

                        items(
                            count = lazyPagingItems.itemCount,
                            key = { index ->
                                val item = lazyPagingItems.peek(index)
                                when (item) {
                                    is TimelineItem.Header -> "header_${item.title}"
                                    is TimelineItem.Media -> "media_${item.item.id}"
                                    null -> "placeholder_$index"
                                }
                            },
                            span = { index ->
                                val item = lazyPagingItems.peek(index)
                                if (item is TimelineItem.Header) {
                                    StaggeredGridItemSpan.FullLine
                                } else {
                                    StaggeredGridItemSpan.SingleLane
                                }
                            }
                        ) { index ->
                            val item = lazyPagingItems[index]
                            if (item != null) {
                                when (item) {
                                    is TimelineItem.Header -> {
                                        val dateMediaIds = headerToMediaIdsMap[item.title] ?: emptyList()
                                        val isAllDateSelected = dateMediaIds.isNotEmpty() && selectionState.selectedIds.containsAll(dateMediaIds)
                                        TimelineHeader(
                                            title = item.title,
                                            inSelectionMode = selectionState.inSelectionMode,
                                            isAllSelected = isAllDateSelected,
                                            onToggleSelection = {
                                                if (isAllDateSelected) {
                                                    selectionState.deselectAll(dateMediaIds)
                                                } else {
                                                    selectionState.selectAll(dateMediaIds)
                                                }
                                                com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                            }
                                        )
                                    }
                                    is TimelineItem.Media -> {
                                        val mediaItem = item.item
                                        val isSelected = selectionState.selectedIds.contains(mediaItem.id)
                                        val naturalRatio = if (mediaItem.width > 0 && mediaItem.height > 0) {
                                            mediaItem.width.toFloat() / mediaItem.height.toFloat()
                                        } else {
                                            1f
                                        }
                                        val cellRatio = if (viewModel.gridStyle == com.hrshd1eux.imava.ui.GridStyle.SQUARE) 1f else naturalRatio
                                        MediaGridCell(
                                            item = mediaItem,
                                            isSelected = isSelected,
                                            inSelectionMode = selectionState.inSelectionMode,
                                            aspectRatio = cellRatio,
                                            gridColumnCount = viewModel.gridColumnCount,
                                            onClick = {
                                                if (selectionState.inSelectionMode) {
                                                    selectionState.toggle(mediaItem.id)
                                                    com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                                } else {
                                                    viewModel.activeMediaItem = mediaItem
                                                }
                                            },
                                            onLongClick = {
                                                selectionState.toggle(mediaItem.id)
                                                com.hrshd1eux.imava.core.util.HapticUtil.performLongPress(context)
                                            }
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }

                    // Floating scroll scrubber on the right edge
                    val dateHeaders by viewModel.datePositionHeaders.collectAsState()
                    TimelineScrubber(
                        gridState = gridState,
                        headers = dateHeaders,
                        totalItemCount = lazyPagingItems.itemCount,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}

data class TimelineFilterChipModel(
    val id: String,
    val label: String,
    val isCustomTag: Boolean,
    val filterType: MediaFilterType? = null,
    val customTag: String? = null
)

private val DEFAULT_TIMELINE_FILTER_CHIPS = listOf(
    TimelineFilterChipModel("ALL", "All", isCustomTag = false, filterType = MediaFilterType.ALL),
    TimelineFilterChipModel("PHOTOS", "Photos", isCustomTag = false, filterType = MediaFilterType.PHOTOS),
    TimelineFilterChipModel("VIDEOS", "Videos", isCustomTag = false, filterType = MediaFilterType.VIDEOS),
    TimelineFilterChipModel("SCREENSHOTS", "Screenshots", isCustomTag = false, filterType = MediaFilterType.SCREENSHOTS),
    TimelineFilterChipModel("FAVORITES", "Favorites", isCustomTag = false, filterType = MediaFilterType.FAVORITES),
    TimelineFilterChipModel("GIFS", "GIFs", isCustomTag = false, filterType = MediaFilterType.GIFS),
    TimelineFilterChipModel("RAW", "RAW", isCustomTag = false, filterType = MediaFilterType.RAW)
)

@Composable
fun TimelineFilterChips(
    selectedFilter: MediaFilterType,
    selectedCustomTag: String?,
    availableTags: List<String>,
    onFilterSelected: (MediaFilterType) -> Unit,
    onCustomTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val galleryPrefs = remember(context) { context.getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE) }

    var chipOrderStr by remember {
        val initial = galleryPrefs.getString("timeline_chip_order", null)
        if (initial == null) {
            val legacy = galleryPrefs.getString("custom_tag_order", "") ?: ""
            if (legacy.isNotBlank()) {
                val legacyTags = legacy.split(",").map { "TAG_${it.trim().lowercase()}" }.joinToString(",")
                val migrated = "ALL,PHOTOS,VIDEOS,SCREENSHOTS,FAVORITES,GIFS,RAW,$legacyTags"
                galleryPrefs.edit().putString("timeline_chip_order", migrated).apply()
                mutableStateOf(migrated)
            } else {
                mutableStateOf("ALL,PHOTOS,VIDEOS,SCREENSHOTS,FAVORITES,GIFS,RAW")
            }
        } else {
            mutableStateOf(initial)
        }
    }
    var chipHiddenStr by remember { mutableStateOf(galleryPrefs.getString("timeline_chip_hidden", "") ?: "") }

    var showManageDialog by remember { mutableStateOf(false) }

    val allChipsMap = remember(availableTags, selectedCustomTag) {
        val map = LinkedHashMap<String, TimelineFilterChipModel>()
        DEFAULT_TIMELINE_FILTER_CHIPS.forEach { map[it.id] = it }
        availableTags.forEach { tag ->
            val clean = tag.trim().removePrefix("#").removePrefix("+").trim().removePrefix("#").removePrefix("+").trim()
            if (clean.isNotEmpty() && !clean.equals("tag", ignoreCase = true) && !clean.equals("+", ignoreCase = true)) {
                val id = "TAG_${clean.lowercase()}"
                map[id] = TimelineFilterChipModel(id, "#$clean", isCustomTag = true, customTag = clean)
            }
        }
        if (selectedCustomTag != null && selectedCustomTag.isNotBlank()) {
            val clean = selectedCustomTag.trim().removePrefix("#").removePrefix("+").trim()
            val id = "TAG_${clean.lowercase()}"
            if (!map.containsKey(id) && clean.isNotEmpty() && !clean.equals("tag", ignoreCase = true) && !clean.equals("+", ignoreCase = true)) {
                map[id] = TimelineFilterChipModel(id, "#$clean", isCustomTag = true, customTag = clean)
            }
        }
        map
    }

    val hiddenIds = remember(chipHiddenStr) {
        chipHiddenStr.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()
    }

    val (activeChips, hiddenChips) = remember(allChipsMap, chipOrderStr, hiddenIds) {
        val orderedIds = chipOrderStr.split(",").map { it.trim().uppercase() }.filter {
            it.isNotEmpty() && it != "ADD_TAG_FILTER_CHIP" && it != "ADD_TAG" && it != "TAG_TAG" && it != "TAG_+"
        }
        val active = mutableListOf<TimelineFilterChipModel>()

        for (id in orderedIds) {
            val chip = allChipsMap.entries.firstOrNull { it.key.equals(id, ignoreCase = true) }?.value
            if (chip != null && !hiddenIds.contains(chip.id.uppercase())) {
                active.add(chip)
            }
        }

        for ((_, chip) in allChipsMap) {
            if (!active.any { it.id.equals(chip.id, ignoreCase = true) } && !hiddenIds.contains(chip.id.uppercase())) {
                active.add(chip)
            }
        }

        val hidden = allChipsMap.values.filter { hiddenIds.contains(it.id.uppercase()) }
        val finalActive = if (active.isEmpty()) listOf(DEFAULT_TIMELINE_FILTER_CHIPS[0]) else active
        Pair(finalActive, hidden)
    }

    if (showManageDialog) {
        var workingActive by remember { mutableStateOf(activeChips) }
        var workingHidden by remember { mutableStateOf(hiddenChips) }

        var isAddingTag by remember { mutableStateOf(false) }
        var newTagInput by remember { mutableStateOf("") }

        var editingChip by remember { mutableStateOf<TimelineFilterChipModel?>(null) }
        var editTagInput by remember { mutableStateOf("") }

        var draggedIndex by remember { mutableStateOf<Int?>(null) }
        var dragAccumulatedY by remember { mutableStateOf(0f) }

        val density = LocalDensity.current
        val estimatedRowHeightPx = with(density) { 52.dp.toPx() }

        AlertDialog(
            onDismissRequest = { showManageDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Filters & Tags")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Long-press and drag any item to reorder it. Tap Delete to hide any filter (including RAW, GIFs, and defaults), or tap '+ Add Tag' to add custom tag filters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Horizontal Preview:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(workingActive) { idx, chip ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "${idx + 1}. ${chip.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Filters (${workingActive.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = {
                                isAddingTag = !isAddingTag
                                if (!isAddingTag) newTagInput = ""
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isAddingTag) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAddingTag) "Close" else "Add Tag",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (isAddingTag) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Add Tag Filter",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = newTagInput,
                                    onValueChange = { newTagInput = it },
                                    placeholder = { Text("e.g. Vacation, Receipts, Work") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val activeTagNames = workingActive.mapNotNull { it.customTag?.lowercase() }.toSet()
                                val availableSuggestions = availableTags.map { it.trim() }.filter { it.isNotBlank() && !activeTagNames.contains(it.lowercase()) }

                                if (availableSuggestions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Suggestions from media:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(availableSuggestions) { tag ->
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    val tagId = "TAG_${tag.lowercase()}"
                                                    val newChip = TimelineFilterChipModel(
                                                        id = tagId,
                                                        label = "#$tag",
                                                        isCustomTag = true,
                                                        customTag = tag
                                                    )
                                                    val mutableActive = workingActive.toMutableList()
                                                    if (!mutableActive.any { it.id.equals(tagId, ignoreCase = true) }) {
                                                        mutableActive.add(newChip)
                                                        workingActive = mutableActive
                                                    }
                                                    val mutableHidden = workingHidden.toMutableList()
                                                    mutableHidden.removeAll { it.id.equals(tagId, ignoreCase = true) }
                                                    workingHidden = mutableHidden
                                                    com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                                                    newTagInput = ""
                                                    isAddingTag = false
                                                },
                                                label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        isAddingTag = false
                                        newTagInput = ""
                                    }) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Button(
                                        onClick = {
                                            val clean = newTagInput.trim().removePrefix("#").removePrefix("+").trim()
                                            if (clean.isNotBlank()) {
                                                val tagId = "TAG_${clean.lowercase()}"
                                                val newChip = TimelineFilterChipModel(
                                                    id = tagId,
                                                    label = "#$clean",
                                                    isCustomTag = true,
                                                    customTag = clean
                                                )
                                                val mutableActive = workingActive.toMutableList()
                                                if (!mutableActive.any { it.id.equals(tagId, ignoreCase = true) }) {
                                                    mutableActive.add(newChip)
                                                    workingActive = mutableActive
                                                }
                                                val mutableHidden = workingHidden.toMutableList()
                                                mutableHidden.removeAll { it.id.equals(tagId, ignoreCase = true) }
                                                workingHidden = mutableHidden
                                                com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                                                newTagInput = ""
                                                isAddingTag = false
                                            }
                                        },
                                        enabled = newTagInput.trim().removePrefix("#").isNotBlank()
                                    ) {
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    workingActive.forEachIndexed { index, chip ->
                        val currentItemIndex by rememberUpdatedState(index)
                        val isDragging = draggedIndex == index
                        val elevation = if (isDragging) 8.dp else 0.dp

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .zIndex(if (isDragging) 10f else 1f)
                                .offset { IntOffset(0, if (isDragging) dragAccumulatedY.roundToInt() else 0) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDragging) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            shadowElevation = elevation
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedIndex = currentItemIndex
                                                    dragAccumulatedY = 0f
                                                    com.hrshd1eux.imava.core.util.HapticUtil.performLongPress(context)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragAccumulatedY += dragAmount.y
                                                    val currentIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                                    if (dragAccumulatedY > estimatedRowHeightPx && currentIdx < workingActive.size - 1) {
                                                        val mutable = workingActive.toMutableList()
                                                        val item = mutable.removeAt(currentIdx)
                                                        mutable.add(currentIdx + 1, item)
                                                        workingActive = mutable
                                                        draggedIndex = currentIdx + 1
                                                        dragAccumulatedY -= estimatedRowHeightPx
                                                        com.hrshd1eux.imava.core.util.HapticUtil.performTick(context)
                                                    } else if (dragAccumulatedY < -estimatedRowHeightPx && currentIdx > 0) {
                                                        val mutable = workingActive.toMutableList()
                                                        val item = mutable.removeAt(currentIdx)
                                                        mutable.add(currentIdx - 1, item)
                                                        workingActive = mutable
                                                        draggedIndex = currentIdx - 1
                                                        dragAccumulatedY += estimatedRowHeightPx
                                                        com.hrshd1eux.imava.core.util.HapticUtil.performTick(context)
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedIndex = null
                                                    dragAccumulatedY = 0f
                                                    com.hrshd1eux.imava.core.util.HapticUtil.performLongPress(context)
                                                },
                                                onDragCancel = {
                                                    draggedIndex = null
                                                    dragAccumulatedY = 0f
                                                }
                                            )
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Long press and drag to reorder",
                                        tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDragging) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (chip.isCustomTag) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Label,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = chip.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (chip.isCustomTag) {
                                        IconButton(
                                            onClick = {
                                                editingChip = chip
                                                editTagInput = chip.customTag ?: chip.label.removePrefix("#")
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit tag",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            if (workingActive.size > 1) {
                                                val mutableActive = workingActive.toMutableList()
                                                val removed = mutableActive.removeAt(index)
                                                workingActive = mutableActive
                                                val mutableHidden = workingHidden.toMutableList()
                                                if (!mutableHidden.any { it.id.equals(removed.id, ignoreCase = true) }) {
                                                    mutableHidden.add(removed)
                                                }
                                                workingHidden = mutableHidden
                                                com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                            }
                                        },
                                        enabled = workingActive.size > 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hide or Delete filter",
                                            tint = if (workingActive.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (workingHidden.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Hidden / Deleted Filters (${workingHidden.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Tap any filter or '+' to restore it to the timeline:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(workingHidden) { hiddenChip ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val mutableHidden = workingHidden.toMutableList()
                                        mutableHidden.removeAll { it.id.equals(hiddenChip.id, ignoreCase = true) }
                                        workingHidden = mutableHidden
                                        val mutableActive = workingActive.toMutableList()
                                        mutableActive.add(hiddenChip)
                                        workingActive = mutableActive
                                        com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Restore",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    label = { Text(hiddenChip.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val orderToSave = workingActive.joinToString(",") { it.id }
                        val hiddenToSave = workingHidden.joinToString(",") { it.id }
                        galleryPrefs.edit()
                            .putString("timeline_chip_order", orderToSave)
                            .putString("timeline_chip_hidden", hiddenToSave)
                            .apply()
                        chipOrderStr = orderToSave
                        chipHiddenStr = hiddenToSave

                        if (selectedCustomTag != null && workingHidden.any { it.customTag.equals(selectedCustomTag, ignoreCase = true) }) {
                            onCustomTagSelected(null)
                        }
                        if (selectedCustomTag == null && workingHidden.any { it.filterType == selectedFilter }) {
                            val firstVisibleBuiltin = workingActive.firstOrNull { !it.isCustomTag }?.filterType ?: MediaFilterType.ALL
                            onFilterSelected(firstVisibleBuiltin)
                        }

                        showManageDialog = false
                        com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                    }
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            val resetActive = DEFAULT_TIMELINE_FILTER_CHIPS.toMutableList()
                            val customChips = (workingActive + workingHidden).filter { it.isCustomTag }.distinctBy { it.id }
                            resetActive.addAll(customChips)
                            workingActive = resetActive
                            workingHidden = emptyList()
                            com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                        }
                    ) {
                        Text("Reset Defaults")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showManageDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )

        if (editingChip != null) {
            val chipToEdit = editingChip!!
            AlertDialog(
                onDismissRequest = { editingChip = null },
                shape = RoundedCornerShape(16.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Tag")
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Rename tag '${chipToEdit.label}':",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editTagInput,
                            onValueChange = { editTagInput = it },
                            placeholder = { Text("Tag name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clean = editTagInput.trim().removePrefix("#").removePrefix("+").trim()
                            if (clean.isNotBlank()) {
                                val newId = "TAG_${clean.lowercase()}"
                                val updatedChip = chipToEdit.copy(
                                    id = newId,
                                    label = "#$clean",
                                    customTag = clean
                                )
                                val mutableActive = workingActive.toMutableList()
                                val idx = mutableActive.indexOfFirst { it.id.equals(chipToEdit.id, ignoreCase = true) }
                                if (idx >= 0) {
                                    mutableActive[idx] = updatedChip
                                    workingActive = mutableActive
                                }
                                if (selectedCustomTag != null && selectedCustomTag.equals(chipToEdit.customTag, ignoreCase = true)) {
                                    onCustomTagSelected(clean)
                                }
                                com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                                editingChip = null
                            }
                        },
                        enabled = editTagInput.trim().removePrefix("#").isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingChip = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = activeChips,
            key = { it.id }
        ) { chip ->
            val isSelected = if (chip.isCustomTag) {
                chip.customTag.equals(selectedCustomTag, ignoreCase = true)
            } else {
                chip.filterType == selectedFilter && selectedCustomTag == null
            }

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "chip_scale"
            )

            val targetContainerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
            val containerColor by animateColorAsState(
                targetValue = targetContainerColor,
                animationSpec = tween(durationMillis = 180),
                label = "chip_container"
            )

            val targetLabelColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val labelColor by animateColorAsState(
                targetValue = targetLabelColor,
                animationSpec = tween(durationMillis = 180),
                label = "chip_label"
            )

            FilterChip(
                selected = isSelected,
                onClick = {
                    com.hrshd1eux.imava.core.util.HapticUtil.performSelection(context)
                    if (chip.isCustomTag) {
                        onCustomTagSelected(if (isSelected) null else chip.customTag)
                    } else {
                        onCustomTagSelected(null)
                        chip.filterType?.let { onFilterSelected(it) }
                    }
                },
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                leadingIcon = if (chip.isCustomTag) {
                    {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        )
                    }
                } else null,
                label = {
                    Text(
                        text = chip.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                trailingIcon = if (chip.isCustomTag) {
                    {
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.6f),
                            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.6f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear tag filter",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = containerColor,
                    selectedLabelColor = labelColor,
                    containerColor = containerColor,
                    labelColor = labelColor
                )
            )
        }

        // "Manage" Chip to reorder left/right and delete any filter/tag
        item(key = "manage_filters_chip") {
            FilterChip(
                selected = false,
                onClick = { showManageDialog = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = { Text("Manage", style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
fun TimelineHeader(
    title: String,
    inSelectionMode: Boolean = false,
    isAllSelected: Boolean = false,
    onToggleSelection: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (inSelectionMode) {
            TextButton(
                onClick = onToggleSelection,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (isAllSelected) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (isAllSelected) "Deselect date" else "Select date",
                    tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isAllSelected) "Deselect" else "Select",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridCell(
    item: MediaItem,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    aspectRatio: Float,
    gridColumnCount: Int = 3,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targetDimension = when {
        gridColumnCount <= 1 -> 720
        gridColumnCount == 2 -> 480
        else -> 320
    }
    val targetWidth = if (aspectRatio >= 1f) (targetDimension * aspectRatio).toInt().coerceAtMost(1080) else targetDimension
    val targetHeight = if (aspectRatio < 1f) (targetDimension / aspectRatio).toInt().coerceAtMost(1080) else targetDimension

    val imageRequest = remember(item.uri, targetWidth, targetHeight) {
        coil.request.ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(false)
            .allowHardware(true)
            .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
            .size(targetWidth, targetHeight)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .precision(coil.size.Precision.INEXACT)
            .error(android.R.drawable.ic_menu_report_image)
            .fallback(android.R.drawable.ic_menu_report_image)
            .build()
    }

    val cellGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.4f)
            ),
            startY = 100f
        )
    }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for visual aesthetics and title visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cellGradient)
        )

        // Select mode overlays
        if (inSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else Color.Transparent
                    )
            )
            
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
            )
        }

        // Trash days remaining indicator
        if (item.isTrashed) {
            val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
            val trashBaseTime = if (item.trashTime > 0L) item.trashTime else System.currentTimeMillis()
            val daysRemaining = ((thirtyDaysMs - (System.currentTimeMillis() - trashBaseTime)) / (24L * 60L * 60L * 1000L)).coerceIn(1L, 30L)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Red.copy(alpha = 0.85f), MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${daysRemaining}d left",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }

        // Video tag indicator
        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(if (item.isTrashed) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = item.formattedDuration,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TimelineSkeletonGrid(columns: Int) {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val alpha by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShimmerAlpha"
    )

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns.coerceIn(2, 6)),
        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 8.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalItemSpacing = 2.dp,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        items(20) { index ->
            val heightDp = when (index % 5) {
                0 -> 130.dp
                1 -> 165.dp
                2 -> 140.dp
                3 -> 180.dp
                else -> 120.dp
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}


