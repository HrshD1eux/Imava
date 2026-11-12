package com.hrshd1eux.imava.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.os.ConfigurationCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.LazyPagingItems
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.core.util.HapticUtil
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.model.TimelineItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Derives an accurate Month-Year date string for a given item index.
 * Uses a 3-tier fallback strategy:
 * 1. Direct peek of the visible item (Header or MediaItem).
 * 2. Windowed scanning (+/- 15 items) if the current index is unloaded during fast fling.
 * 3. Proportional lookup in the pre-sorted visibleMediaItems list.
 */
internal fun resolveDateLabel(
    index: Int,
    lazyPagingItems: LazyPagingItems<TimelineItem>,
    visibleItems: List<MediaItem>,
    zoneId: ZoneId,
    formatter: DateTimeFormatter,
    monthCache: HashMap<Long, String>,
    undatedLabel: String = "Undated"
): String? = resolveDateLabelInternal(
    index = index,
    totalCount = lazyPagingItems.itemCount,
    itemProvider = { if (it in 0 until lazyPagingItems.itemCount) lazyPagingItems.peek(it) else null },
    visibleItems = visibleItems,
    zoneId = zoneId,
    formatter = formatter,
    monthCache = monthCache,
    undatedLabel = undatedLabel
)

internal fun resolveDateLabelInternal(
    index: Int,
    totalCount: Int,
    itemProvider: (Int) -> TimelineItem?,
    visibleItems: List<MediaItem>,
    zoneId: ZoneId,
    formatter: DateTimeFormatter,
    monthCache: HashMap<Long, String>,
    undatedLabel: String = "Undated"
): String? {
    if (totalCount == 0 && visibleItems.isEmpty()) return null

    // 1. Direct item peek
    if (index in 0 until totalCount) {
        val directItem = itemProvider(index)
        if (directItem is TimelineItem.Media) {
            val dateMs = if (directItem.item.trashTime > 0L) directItem.item.trashTime else directItem.item.dateTaken
            if (dateMs > 0L) {
                val localDate = Instant.ofEpochMilli(dateMs).atZone(zoneId).toLocalDate()
                val monthKey = localDate.year.toLong() * 100 + localDate.monthValue
                return monthCache.getOrPut(monthKey) { localDate.format(formatter) }
            }
        } else if (directItem is TimelineItem.Header) {
            val nextItem = if (index + 1 < totalCount) itemProvider(index + 1) else null
            if (nextItem is TimelineItem.Media) {
                val dateMs = if (nextItem.item.trashTime > 0L) nextItem.item.trashTime else nextItem.item.dateTaken
                if (dateMs > 0L) {
                    val localDate = Instant.ofEpochMilli(dateMs).atZone(zoneId).toLocalDate()
                    val monthKey = localDate.year.toLong() * 100 + localDate.monthValue
                    return monthCache.getOrPut(monthKey) { localDate.format(formatter) }
                }
            }
        }

        // 2. Local window search for loaded neighbors (+/- 15 items)
        for (offset in 1..15) {
            val nextIdx = index + offset
            if (nextIdx in 0 until totalCount) {
                val nextItem = itemProvider(nextIdx)
                if (nextItem is TimelineItem.Media) {
                    val dateMs = if (nextItem.item.trashTime > 0L) nextItem.item.trashTime else nextItem.item.dateTaken
                    if (dateMs > 0L) {
                        val localDate = Instant.ofEpochMilli(dateMs).atZone(zoneId).toLocalDate()
                        val monthKey = localDate.year.toLong() * 100 + localDate.monthValue
                        return monthCache.getOrPut(monthKey) { localDate.format(formatter) }
                    }
                }
            }
            val prevIdx = index - offset
            if (prevIdx in 0 until totalCount) {
                val prevItem = itemProvider(prevIdx)
                if (prevItem is TimelineItem.Media) {
                    val dateMs = if (prevItem.item.trashTime > 0L) prevItem.item.trashTime else prevItem.item.dateTaken
                    if (dateMs > 0L) {
                        val localDate = Instant.ofEpochMilli(dateMs).atZone(zoneId).toLocalDate()
                        val monthKey = localDate.year.toLong() * 100 + localDate.monthValue
                        return monthCache.getOrPut(monthKey) { localDate.format(formatter) }
                    }
                }
            }
        }
    }

    // 3. Fallback to calibrated position in visibleItems
    if (visibleItems.isNotEmpty()) {
        // Headers add roughly 12-15% overhead in date-grouped timeline mode
        val estimatedTotalGridItems = (visibleItems.size * 1.13f).toInt().coerceAtLeast(visibleItems.size)
        val ratio = (index.toFloat() / estimatedTotalGridItems.toFloat()).coerceIn(0f, 1f)
        val targetIdx = (ratio * (visibleItems.size - 1)).toInt().coerceIn(0, visibleItems.lastIndex)
        val item = visibleItems[targetIdx]
        val dateMs = if (item.trashTime > 0L) item.trashTime else item.dateTaken
        if (dateMs <= 0L) return undatedLabel

        val localDate = Instant.ofEpochMilli(dateMs).atZone(zoneId).toLocalDate()
        val monthKey = localDate.year.toLong() * 100 + localDate.monthValue
        return monthCache.getOrPut(monthKey) { localDate.format(formatter) }
    }

    return null
}

/**
 * Minimalist top-center floating date badge.
 * Displays accurate date context during scroll without any flickering or inaccurate time-leaps.
 */
@Composable
fun TimelineFloatingDateBadge(
    gridState: LazyGridState? = null,
    staggeredGridState: LazyStaggeredGridState? = null,
    lazyPagingItems: LazyPagingItems<TimelineItem>,
    visibleItems: List<MediaItem>,
    modifier: Modifier = Modifier
) {
    val totalCount = lazyPagingItems.itemCount
    if (totalCount <= 12 && visibleItems.size <= 12) return

    val currentLocale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val monthYearFormatter = remember(currentLocale) { DateTimeFormatter.ofPattern("MMMM yyyy", currentLocale) }
    val zoneId = remember { ZoneId.systemDefault() }
    val monthCache = remember(currentLocale) { HashMap<Long, String>() }
    val undatedLabel = stringResource(R.string.date_undated)

    val isScrollInProgress = when {
        staggeredGridState != null -> staggeredGridState.isScrollInProgress
        gridState != null -> gridState.isScrollInProgress
        else -> false
    }

    val currentDateLabel by remember(staggeredGridState, gridState) {
        derivedStateOf {
            val firstIndex = when {
                staggeredGridState != null -> staggeredGridState.firstVisibleItemIndex
                gridState != null -> gridState.firstVisibleItemIndex
                else -> 0
            }
            resolveDateLabel(firstIndex, lazyPagingItems, visibleItems, zoneId, monthYearFormatter, monthCache, undatedLabel)
        }
    }

    val isVisible = isScrollInProgress && !currentDateLabel.isNullOrBlank()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.85f),
        exit = fadeOut(animationSpec = tween(300, delayMillis = 350)) + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp).copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp,
            tonalElevation = 6.dp
        ) {
            Text(
                text = currentDateLabel.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Interactive, vertical edge-anchored FastScroller.
 * Displays a tactile draggable thumb on the right margin with an attached Month-Year tooltip.
 * Provides haptic feedback as the user's thumb crosses month boundaries.
 */
@Composable
fun TimelineFastScroller(
    gridState: LazyGridState? = null,
    staggeredGridState: LazyStaggeredGridState? = null,
    lazyPagingItems: LazyPagingItems<TimelineItem>,
    visibleItems: List<MediaItem>,
    modifier: Modifier = Modifier
) {
    val totalCount = lazyPagingItems.itemCount.coerceAtLeast(visibleItems.size)
    if (totalCount <= 24) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentLocale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val monthYearFormatter = remember(currentLocale) { DateTimeFormatter.ofPattern("MMM yyyy", currentLocale) }
    val zoneId = remember { ZoneId.systemDefault() }
    val monthCache = remember(currentLocale) { HashMap<Long, String>() }
    val undatedLabel = stringResource(R.string.date_undated)

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var lastHapticLabel by remember { mutableStateOf<String?>(null) }
    var userInteractedTimestamp by remember { mutableStateOf(0L) }

    val isScrolling = when {
        staggeredGridState != null -> staggeredGridState.isScrollInProgress
        gridState != null -> gridState.isScrollInProgress
        else -> false
    }

    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            userInteractedTimestamp = System.currentTimeMillis()
        }
    }

    // Auto-hide when idle
    var isThumbVisible by remember { mutableStateOf(false) }
    LaunchedEffect(userInteractedTimestamp, isDragging) {
        if (isDragging) {
            isThumbVisible = true
        } else {
            isThumbVisible = true
            delay(2000)
            if (!isDragging && !isScrolling) {
                isThumbVisible = false
            }
        }
    }

    val currentScrollProgress by remember(isDragging, dragProgress, staggeredGridState, gridState) {
        derivedStateOf {
            if (isDragging) {
                dragProgress
            } else {
                val firstIdx = when {
                    staggeredGridState != null -> staggeredGridState.firstVisibleItemIndex
                    gridState != null -> gridState.firstVisibleItemIndex
                    else -> 0
                }
                (firstIdx.toFloat() / (totalCount - 1).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    val activeDateLabel by remember(currentScrollProgress, isDragging) {
        derivedStateOf {
            val targetIndex = (currentScrollProgress * (totalCount - 1)).toInt().coerceIn(0, (totalCount - 1).coerceAtLeast(0))
            resolveDateLabel(targetIndex, lazyPagingItems, visibleItems, zoneId, monthYearFormatter, monthCache, undatedLabel)
        }
    }

    // Haptic feedback when scrubbing across months
    LaunchedEffect(activeDateLabel) {
        if (isDragging && activeDateLabel != null && activeDateLabel != lastHapticLabel) {
            lastHapticLabel = activeDateLabel
            HapticUtil.performSelection(context)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(56.dp)
            .padding(end = 4.dp, top = 64.dp, bottom = 80.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val thumbHeightDp = 48.dp
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        val maxTravelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

        val thumbOffsetY = (currentScrollProgress * maxTravelPx).roundToInt()

        // Thumb visibility animated
        AnimatedVisibility(
            visible = isThumbVisible || isDragging,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(tween(400)) + scaleOut(targetScale = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalCount, trackHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val clampedY = (offset.y - (thumbHeightPx / 2f)).coerceIn(0f, maxTravelPx)
                                dragProgress = clampedY / maxTravelPx
                                val targetIdx = (dragProgress * (totalCount - 1)).toInt().coerceIn(0, totalCount - 1)
                                scope.launch {
                                    staggeredGridState?.scrollToItem(targetIdx)
                                    gridState?.scrollToItem(targetIdx)
                                }
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val clampedY = (change.position.y - (thumbHeightPx / 2f)).coerceIn(0f, maxTravelPx)
                                dragProgress = clampedY / maxTravelPx
                                val targetIdx = (dragProgress * (totalCount - 1)).toInt().coerceIn(0, totalCount - 1)
                                scope.launch {
                                    staggeredGridState?.scrollToItem(targetIdx)
                                    gridState?.scrollToItem(targetIdx)
                                }
                            }
                        )
                    }
            ) {
                // Draggable Thumb & Attached Popup Pill
                Row(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = thumbOffsetY) }
                        .align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Tooltip Badge (visible while dragging)
                    AnimatedVisibility(
                        visible = isDragging && !activeDateLabel.isNullOrBlank(),
                        enter = fadeIn(tween(100)) + scaleIn(initialScale = 0.85f),
                        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.85f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 8.dp,
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(
                                text = activeDateLabel.orEmpty(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                maxLines = 1
                            )
                        }
                    }

                    // Scroller Thumb Pill
                    Surface(
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                        color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        shadowElevation = if (isDragging) 6.dp else 2.dp,
                        modifier = Modifier
                            .height(thumbHeightDp)
                            .width(if (isDragging) 10.dp else 5.dp)
                    ) {}
                }
            }
        }
    }
}
