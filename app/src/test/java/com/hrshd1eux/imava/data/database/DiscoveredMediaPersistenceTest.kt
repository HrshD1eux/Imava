package com.hrshd1eux.imava.data.database

import android.net.Uri
import com.hrshd1eux.imava.data.model.MediaItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiscoveredMediaPersistenceTest {

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>(relaxed = true)
        every { Uri.fromFile(any()) } returns mockUri
        every { Uri.parse(any()) } returns mockUri
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    private class FakeDiscoveredMediaDao : DiscoveredMediaDao {
        val items = mutableMapOf<Long, DiscoveredMediaEntity>()

        override suspend fun getAllDiscoveredMedia(): List<DiscoveredMediaEntity> {
            return items.values.sortedByDescending { it.dateTaken }
        }

        override suspend fun getDiscoveredMediaByBucket(bucketId: Long): List<DiscoveredMediaEntity> {
            return items.values.filter { it.bucketId == bucketId }.sortedByDescending { it.dateTaken }
        }

        override suspend fun insertOrUpdateBatch(items: List<DiscoveredMediaEntity>) {
            items.forEach { this.items[it.id] = it }
        }

        override suspend fun deleteById(id: Long) {
            items.remove(id)
        }

        override suspend fun deleteByPath(path: String) {
            items.entries.removeIf { it.value.path == path }
        }

        override suspend fun deleteByIds(ids: List<Long>) {
            ids.forEach { items.remove(it) }
        }

        override suspend fun purgeThumbnails(): Int {
            val before = items.size
            items.entries.removeIf {
                val p = it.value.path.lowercase()
                p.contains("/.thumbnails/") || p.contains("/thumbnails/") || p.contains("/.cache/") || p.contains("/cache/")
            }
            return before - items.size
        }

        override suspend fun getCount(): Int {
            return items.size
        }

        override suspend fun clearAll() {
            items.clear()
        }
    }

    @Test
    fun testEntityConversion_photoPreservesAllFields() {
        val mockUri = mockk<Uri>(relaxed = true)
        val photo = MediaItem.Photo(
            id = 12345L,
            uri = mockUri,
            path = "/storage/emulated/0/WhatsApp/Media/WhatsApp Images/Sent/sample.jpg",
            mimeType = "image/jpeg",
            dateTaken = 1700000000000L,
            size = 204800L,
            width = 1920,
            height = 1080,
            bucketId = 999L,
            bucketName = "WhatsApp Images Sent"
        )

        val entity = DiscoveredMediaEntity.fromMediaItem(photo)
        assertEquals(photo.id, entity.id)
        assertEquals(photo.path, entity.path)
        assertEquals(photo.mimeType, entity.mimeType)
        assertEquals(photo.dateTaken, entity.dateTaken)
        assertEquals(photo.size, entity.size)
        assertEquals(photo.width, entity.width)
        assertEquals(photo.height, entity.height)
        assertEquals(photo.bucketId, entity.bucketId)
        assertEquals(photo.bucketName, entity.bucketName)
        assertFalse(entity.isVideo)

        val reconstructed = entity.toMediaItem()
        assertTrue(reconstructed is MediaItem.Photo)
        assertEquals(photo.id, reconstructed.id)
        assertEquals(photo.path, reconstructed.path)
        assertEquals(photo.mimeType, reconstructed.mimeType)
    }

    @Test
    fun testEntityConversion_videoPreservesDurationAndType() {
        val mockUri = mockk<Uri>(relaxed = true)
        val video = MediaItem.Video(
            id = 54321L,
            uri = mockUri,
            path = "/storage/emulated/0/Telegram/Telegram Video/video.mp4",
            mimeType = "video/mp4",
            dateTaken = 1700000050000L,
            size = 10485760L,
            width = 1280,
            height = 720,
            durationMs = 45000L,
            bucketId = 888L,
            bucketName = "Telegram Video"
        )

        val entity = DiscoveredMediaEntity.fromMediaItem(video)
        assertEquals(video.id, entity.id)
        assertTrue(entity.isVideo)
        assertEquals(45000L, entity.durationMs)

        val reconstructed = entity.toMediaItem()
        assertTrue(reconstructed is MediaItem.Video)
        val vid = reconstructed as MediaItem.Video
        assertEquals(45000L, vid.durationMs)
        assertEquals("video/mp4", vid.mimeType)
    }

    @Test
    fun testDao_batchOperationsAndThumbnailPurge() = runBlocking {
        val dao = FakeDiscoveredMediaDao()

        val item1 = DiscoveredMediaEntity(
            id = 1L,
            path = "/storage/emulated/0/WhatsApp/.Shared/doc.jpg",
            mimeType = "image/jpeg",
            dateTaken = 1000L,
            size = 5000L,
            bucketId = 10L,
            bucketName = "WhatsApp Shared"
        )
        val item2 = DiscoveredMediaEntity(
            id = 2L,
            path = "/storage/emulated/0/Telegram/image.jpg",
            mimeType = "image/jpeg",
            dateTaken = 2000L,
            size = 6000L,
            bucketId = 20L,
            bucketName = "Telegram"
        )
        val thumb = DiscoveredMediaEntity(
            id = 3L,
            path = "/storage/emulated/0/DCIM/.thumbnails/thumb_123.jpg",
            mimeType = "image/jpeg",
            dateTaken = 3000L,
            size = 1000L,
            bucketId = 30L,
            bucketName = "Thumbnails"
        )

        dao.insertOrUpdateBatch(listOf(item1, item2, thumb))
        assertEquals(3, dao.getCount())

        val purged = dao.purgeThumbnails()
        assertEquals(1, purged)
        assertEquals(2, dao.getCount())

        dao.deleteByPath("/storage/emulated/0/WhatsApp/.Shared/doc.jpg")
        assertEquals(1, dao.getCount())
        assertEquals(2L, dao.getAllDiscoveredMedia()[0].id)
    }
}
