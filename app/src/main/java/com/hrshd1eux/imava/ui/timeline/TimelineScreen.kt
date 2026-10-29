package com.hrshd1eux.imava.ui.timeline

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.hrshd1eux.imava.ui.MediaFilterType
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapVert
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
    var customTagOrderStr by remember { mutableStateOf(galleryPrefs.getString("custom_tag_order", "") ?: "") }

    var showAddTagFilterDialog by remember { mutableStateOf(false) }
    var showReorderDialog by remember { mutableStateOf(false) }
    var tagFilterInput by remember { mutableStateOf("") }

    val sortedTags = remember(availableTags, customTagOrderStr) {
        if (customTagOrderStr.isBlank()) {
            availableTags
        } else {
            val preferredOrder = customTagOrderStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val inPreferred = preferredOrder.filter { preferred -> availableTags.any { it.equals(preferred, ignoreCase = true) } }
            val remaining = availableTags.filter { tag -> !preferredOrder.any { it.equals(tag, ignoreCase = true) } }
            inPreferred + remaining
        }
    }

    if (showReorderDialog) {
        var reorderList by remember { mutableStateOf(sortedTags) }

        AlertDialog(
            onDismissRequest = { showReorderDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Rearrange Filter Tags") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Move your most frequent or daily necessity tags to the top so they appear first in the filter row:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    reorderList.forEachIndexed { index, tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val mutable = reorderList.toMutableList()
                                            val temp = mutable[index]
                                            mutable[index] = mutable[index - 1]
                                            mutable[index - 1] = temp
                                            reorderList = mutable
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                                }

                                IconButton(
                                    onClick = {
                                        if (index < reorderList.size - 1) {
                                            val mutable = reorderList.toMutableList()
                                            val temp = mutable[index]
                                            mutable[index] = mutable[index + 1]
                                            mutable[index + 1] = temp
                                            reorderList = mutable
                                        }
                                    },
                                    enabled = index < reorderList.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                                }
                            }
                        }
                        if (index < reorderList.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val orderToSave = reorderList.joinToString(",")
                        galleryPrefs.edit().putString("custom_tag_order", orderToSave).apply()
                        customTagOrderStr = orderToSave
                        showReorderDialog = false
                        com.hrshd1eux.imava.core.util.HapticUtil.performSuccess(context)
                    }
                ) {
                    Text("Save Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReorderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddTagFilterDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagFilterDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Filter by Tag") },
            text = {
                Column {
                    Text(
                        text = "Enter a tag to search photos with that tag:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tagFilterInput,
                        onValueChange = { tagFilterInput = it },
                        placeholder = { Text("e.g. vacation, family") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (sortedTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Existing Tags:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(sortedTags) { tag ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        tagFilterInput = tag
                                        showAddTagFilterDialog = false
                                        onCustomTagSelected(tag)
                                    },
                                    label = { Text("#$tag") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = tagFilterInput.trim().removePrefix("#")
                        if (clean.isNotBlank()) {
                            showAddTagFilterDialog = false
                            onCustomTagSelected(clean)
                        }
                    },
                    enabled = tagFilterInput.isNotBlank()
                ) {
                    Text("Filter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagFilterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = MediaFilterType.entries.toTypedArray(),
            key = { it.name }
        ) { filter ->
            val isSelected = filter == selectedFilter && selectedCustomTag == null
            FilterChip(
                selected = isSelected,
                onClick = {
                    onCustomTagSelected(null)
                    onFilterSelected(filter)
                },
                label = {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        item {
            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        // Active custom tag (if not in sortedTags)
        if (selectedCustomTag != null && !sortedTags.contains(selectedCustomTag)) {
            item(key = "active_custom_tag") {
                FilterChip(
                    selected = true,
                    onClick = { onCustomTagSelected(null) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    label = { Text("#$selectedCustomTag", style = MaterialTheme.typography.labelMedium) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear tag filter",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Available custom tags in sorted order
        items(
            items = sortedTags,
            key = { "tag_$it" }
        ) { tag ->
            val isTagSelected = tag.equals(selectedCustomTag, ignoreCase = true)
            FilterChip(
                selected = isTagSelected,
                onClick = {
                    onCustomTagSelected(if (isTagSelected) null else tag)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isTagSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )
                },
                label = { Text("#$tag", style = MaterialTheme.typography.labelMedium) },
                trailingIcon = if (isTagSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear tag filter",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        // "+ Tag" Chip to add custom tag filter
        item(key = "add_tag_filter_chip") {
            FilterChip(
                selected = false,
                onClick = { showAddTagFilterDialog = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = { Text("Tag", style = MaterialTheme.typography.labelMedium) }
            )
        }

        // Reorder tags chip if 2 or more tags exist
        if (sortedTags.size >= 2) {
            item(key = "reorder_tags_chip") {
                FilterChip(
                    selected = false,
                    onClick = { showReorderDialog = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text("Reorder", style = MaterialTheme.typography.labelMedium) }
                )
            }
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


