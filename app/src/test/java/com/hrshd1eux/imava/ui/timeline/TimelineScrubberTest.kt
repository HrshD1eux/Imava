package com.hrshd1eux.imava.ui.timeline

import android.net.Uri
import com.hrshd1eux.imava.data.model.MediaItem
import com.hrshd1eux.imava.data.model.TimelineItem
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TimelineScrubberTest {

    private val zoneId = ZoneId.of("UTC")
    private val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    private val monthCache = HashMap<Long, String>()
    private val mockUri = mockk<Uri>(relaxed = true)

    private fun createMediaItem(id: Long, epochMillis: Long): MediaItem {
        return MediaItem.Photo(
            id = id,
            uri = mockUri,
            path = "/storage/emulated/0/DCIM/Camera/IMG_$id.jpg",
            mimeType = "image/jpeg",
            dateTaken = epochMillis,
            size = 1024L,
            width = 1920,
            height = 1080,
            bucketId = 1L,
            bucketName = "Camera"
        )
    }

    @Test
    fun testResolveDateLabel_directMediaItem_returnsFormattedMonthYear() {
        val epochMs = LocalDate.of(2024, 8, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val media = createMediaItem(1L, epochMs)
        val items = listOf(TimelineItem.Media(media))

        val result = resolveDateLabelInternal(
            index = 0,
            totalCount = items.size,
            itemProvider = { items[it] },
            visibleItems = emptyList(),
            zoneId = zoneId,
            formatter = formatter,
            monthCache = monthCache
        )

        assertEquals("August 2024", result)
    }

    @Test
    fun testResolveDateLabel_headerItem_peeksNextMediaItem_neverReturnsYesterday() {
        val epochMs = LocalDate.of(2024, 9, 10).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val media = createMediaItem(2L, epochMs)
        val items = listOf(
            TimelineItem.Header("Yesterday"),
            TimelineItem.Media(media)
        )

        val result = resolveDateLabelInternal(
            index = 0, // Header position
            totalCount = items.size,
            itemProvider = { items[it] },
            visibleItems = emptyList(),
            zoneId = zoneId,
            formatter = formatter,
            monthCache = monthCache
        )

        // Must return the month of the media under the header, NOT "Yesterday"
        assertEquals("September 2024", result)
    }

    @Test
    fun testResolveDateLabel_windowSearch_findsLoadedNeighbor() {
        val epochMs = LocalDate.of(2024, 8, 20).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val media = createMediaItem(3L, epochMs)
        val itemsMap = mapOf(
            5 to TimelineItem.Media(media)
        )

        val result = resolveDateLabelInternal(
            index = 2,
            totalCount = 10,
            itemProvider = { itemsMap[it] },
            visibleItems = emptyList(),
            zoneId = zoneId,
            formatter = formatter,
            monthCache = monthCache
        )

        assertEquals("August 2024", result)
    }

    @Test
    fun testResolveDateLabel_fallbackToVisibleItems_whenUnpaged() {
        val epochMs = LocalDate.of(2024, 8, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val media = createMediaItem(4L, epochMs)
        val visibleList = listOf(media)

        val result = resolveDateLabelInternal(
            index = 50,
            totalCount = 100,
            itemProvider = { null },
            visibleItems = visibleList,
            zoneId = zoneId,
            formatter = formatter,
            monthCache = monthCache
        )

        assertEquals("August 2024", result)
    }

    @Test
    fun testResolveDateLabel_emptyData_returnsNull() {
        val result = resolveDateLabelInternal(
            index = 0,
            totalCount = 0,
            itemProvider = { null },
            visibleItems = emptyList(),
            zoneId = zoneId,
            formatter = formatter,
            monthCache = monthCache
        )

        assertNull(result)
    }
}
