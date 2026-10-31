package com.hrshd1eux.imava.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.model.TimelineItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Minimalist top-center floating date badge.
 * Displays accurate date context during scroll without any intrusive sidebars,
 * side scrollbars, or touch-gesture conflicts with Android OS back swipe.
 */
@Composable
fun TimelineFloatingDateBadge(
    gridState: LazyStaggeredGridState,
    lazyPagingItems: LazyPagingItems<TimelineItem>,
    visibleItems: List<MediaItem>,
    modifier: Modifier = Modifier
) {
    val totalCount = lazyPagingItems.itemCount
    if (totalCount <= 12) return

    val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val zoneId = remember { ZoneId.systemDefault() }

    // Derive accurate date label from currently visible top item or proportional index
    val currentDateLabel by remember(totalCount) {
        derivedStateOf {
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
            } else if (visibleItems.isNotEmpty()) {
                val fraction = (gridState.firstVisibleItemIndex.toFloat() / (totalCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                val targetIdx = (fraction * (visibleItems.size - 1)).toInt().coerceIn(0, visibleItems.lastIndex)
                val item = visibleItems[targetIdx]
                val dateMs = if (item.trashTime > 0L) item.trashTime else item.dateTaken
                val ms = if (dateMs > 0L) dateMs else System.currentTimeMillis()
                Instant.ofEpochMilli(ms).atZone(zoneId).format(monthYearFormatter)
            } else null
        }
    }

    val isVisible = gridState.isScrollInProgress && !currentDateLabel.isNullOrBlank()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.85f),
        exit = fadeOut(animationSpec = tween(350, delayMillis = 400)) + scaleOut(targetScale = 0.9f),
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
