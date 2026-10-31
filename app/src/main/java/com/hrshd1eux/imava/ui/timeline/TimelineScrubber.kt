package com.hrshd1eux.imava.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.model.TimelineItem
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TimelineFastScroller(
    gridState: LazyStaggeredGridState,
    lazyPagingItems: LazyPagingItems<TimelineItem>,
    visibleItems: List<MediaItem>,
    inSelectionMode: Boolean,
    modifier: Modifier = Modifier
) {
    val totalCount = lazyPagingItems.itemCount
    if (totalCount <= 12) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val zoneId = remember { ZoneId.systemDefault() }

    // Derive accurate date label from currently visible top item or dragged position
    val currentDateLabel by remember(totalCount, isDragging, dragFraction) {
        derivedStateOf {
            if (isDragging && visibleItems.isNotEmpty()) {
                val targetIdx = (dragFraction * (visibleItems.size - 1)).toInt().coerceIn(0, visibleItems.lastIndex)
                val item = visibleItems[targetIdx]
                val dateMs = if (item.trashTime > 0L) item.trashTime else item.dateTaken
                val ms = if (dateMs > 0L) dateMs else System.currentTimeMillis()
                return@derivedStateOf Instant.ofEpochMilli(ms).atZone(zoneId).format(monthYearFormatter)
            }

            val firstVisible = gridState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (firstVisible != null && firstVisible.index in 0 until totalCount) {
                val item = lazyPagingItems.peek(firstVisible.index)
                when (item) {
                    is TimelineItem.Header -> item.title
                    is TimelineItem.Media -> {
                        val dateMs = if (item.item.trashTime > 0L) item.item.trashTime else item.item.dateTaken
                        val ms = if (dateMs > 0L) dateMs else System.currentTimeMillis()
                        Instant.ofEpochMilli(ms).atZone(zoneId).format(monthYearFormatter)
                    }
                    null -> null
                }
            } else null
        }
    }

    val isVisible = (gridState.isScrollInProgress || isDragging) && !currentDateLabel.isNullOrBlank()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(if (inSelectionMode) 0.dp else 44.dp)
    ) {
        val density = LocalDensity.current
        val trackHeightPx = constraints.maxHeight.toFloat()
        val thumbHeightDp = 48.dp
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        val usableTrackPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

        // Compute thumb vertical offset
        val currentFraction = if (isDragging) {
            dragFraction
        } else {
            val firstIdx = gridState.firstVisibleItemIndex.toFloat()
            (firstIdx / (totalCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
        val thumbYPx = (currentFraction * usableTrackPx).coerceIn(0f, usableTrackPx)

        // Sleek floating date bubble + thumb handle
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.85f),
            exit = fadeOut(animationSpec = tween(400, delayMillis = 600)) + scaleOut(targetScale = 0.9f),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = thumbYPx.toInt()) }
                    .padding(end = 6.dp)
            ) {
                // Floating Date Bubble
                if (!currentDateLabel.isNullOrBlank()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                        contentColor = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = currentDateLabel.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                // Drag Thumb Pill (only rendered when not in selection mode)
                if (!inSelectionMode) {
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = thumbHeightDp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isDragging) 0.95f else 0.70f))
                    )
                }
            }
        }

        // Draggable vertical touch track (strictly disabled during selection mode to prevent accidental scrubbing)
        if (!inSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
                    .align(Alignment.CenterEnd)
                    .pointerInput(totalCount) {
                        fun handlePosition(y: Float) {
                            val fraction = (y / trackHeightPx).coerceIn(0f, 1f)
                            dragFraction = fraction
                            val targetGridIndex = (fraction * (totalCount - 1)).toInt().coerceIn(0, totalCount - 1)
                            coroutineScope.launch {
                                try {
                                    gridState.scrollToItem(targetGridIndex)
                                } catch (_: Exception) {}
                            }
                        }

                        detectTapGestures(
                            onPress = { offset ->
                                isDragging = true
                                try {
                                    handlePosition(offset.y)
                                    awaitRelease()
                                } finally {
                                    isDragging = false
                                }
                            }
                        )
                    }
                    .pointerInput(totalCount) {
                        fun handlePosition(y: Float) {
                            val fraction = (y / trackHeightPx).coerceIn(0f, 1f)
                            dragFraction = fraction
                            val targetGridIndex = (fraction * (totalCount - 1)).toInt().coerceIn(0, totalCount - 1)
                            coroutineScope.launch {
                                try {
                                    gridState.scrollToItem(targetGridIndex)
                                } catch (_: Exception) {}
                            }
                        }

                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                handlePosition(offset.y)
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                handlePosition(change.position.y)
                            }
                        )
                    }
            )
        }
    }
}
