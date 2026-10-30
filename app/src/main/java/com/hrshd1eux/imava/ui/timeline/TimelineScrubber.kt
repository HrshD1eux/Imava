package com.hrshd1eux.imava.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.hrshd1eux.imava.data.repository.DatePositionHeader
import kotlinx.coroutines.launch

@Composable
fun TimelineScrubber(
    gridState: LazyStaggeredGridState,
    headers: List<DatePositionHeader>,
    totalItemCount: Int,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    val isVisible = gridState.isScrollInProgress || isDragging

    if (headers.isEmpty()) return

    val displayHeaders = remember(headers) {
        if (headers.size <= 7) {
            headers
        } else {
            val step = (headers.size - 1) / 6f
            (0..6).map { i ->
                val index = (i * step).toInt().coerceIn(0, headers.lastIndex)
                headers[index]
            }.distinctBy { it.label.ifEmpty { it.title } }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 500, delayMillis = 300)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).copy(alpha = 0.90f))
                    .padding(horizontal = 6.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .pointerInput(headers, totalItemCount) {
                            fun scrollToY(y: Float) {
                                if (totalItemCount <= 0) return
                                val totalHeight = size.height.toFloat()
                                if (totalHeight <= 0f) return
                                val fraction = (y / totalHeight).coerceIn(0f, 1f)
                                val index = (fraction * (headers.size - 1)).toInt().coerceIn(0, headers.lastIndex)
                                val rawTargetIndex = headers[index].positionIndex
                                val targetGridIndex = rawTargetIndex.coerceIn(0, (totalItemCount - 1).coerceAtLeast(0))
                                
                                coroutineScope.launch {
                                    try {
                                        gridState.scrollToItem(targetGridIndex)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            detectTapGestures(
                                onPress = { offset ->
                                    isDragging = true
                                    try {
                                        scrollToY(offset.y)
                                        awaitRelease()
                                    } finally {
                                        isDragging = false
                                    }
                                }
                            )
                        }
                        .pointerInput(headers, totalItemCount) {
                            fun scrollToY(y: Float) {
                                if (totalItemCount <= 0) return
                                val totalHeight = size.height.toFloat()
                                if (totalHeight <= 0f) return
                                val fraction = (y / totalHeight).coerceIn(0f, 1f)
                                val index = (fraction * (headers.size - 1)).toInt().coerceIn(0, headers.lastIndex)
                                val rawTargetIndex = headers[index].positionIndex
                                val targetGridIndex = rawTargetIndex.coerceIn(0, (totalItemCount - 1).coerceAtLeast(0))
                                
                                coroutineScope.launch {
                                    try {
                                        gridState.scrollToItem(targetGridIndex)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    scrollToY(offset.y)
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    scrollToY(change.position.y)
                                }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    displayHeaders.forEach { header ->
                        val displayLabel = header.label.ifEmpty {
                            val title = header.title
                            if (title == "Today") "TD" else if (title == "Yesterday") "YS" else title.take(3).uppercase()
                        }
                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
