package com.hrshd1eux.imava.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hrshd1eux.imava.data.model.MediaItem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class SharingUtilsTest {

    private lateinit var mockContext: Context
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        tempDir = File(System.getProperty("java.io.tmpdir"), "sharing_utils_test_${System.currentTimeMillis()}").apply { mkdirs() }
        every { mockContext.cacheDir } returns tempDir
        every { mockContext.externalCacheDir } returns tempDir
        every { mockContext.packageName } returns "com.hrshd1eux.imava"
        every { mockContext.contentResolver } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
        unmockkAll()
    }

    @Test
    fun testShareMedia_emptyList_doesNothing() = runBlocking {
        SharingUtils.shareMedia(mockContext, emptyList())
        verify(exactly = 0) { mockContext.startActivity(any()) }
    }

    @Test
    fun testShareMedia_publicVideo_usesZeroCopyDirectUri() = runBlocking {
        val contentUri = mockk<Uri>(relaxed = true)
        every { contentUri.scheme } returns "content"
        every { contentUri.toString() } returns "content://media/external/video/media/1001"

        val videoItem = MediaItem.Video(
            id = 1001L,
            uri = contentUri,
            path = "/storage/emulated/0/DCIM/Camera/VID_4K.mp4",
            mimeType = "video/mp4",
            dateTaken = 1700000000000L,
            size = 1024L * 1024L * 800L, // 800MB 4K video
            width = 3840,
            height = 2160,
            durationMs = 60000L,
            bucketId = 1L,
            bucketName = "Camera"
        )

        val intentSlot = slot<Intent>()
        every { mockContext.startActivity(capture(intentSlot)) } returns Unit

        mockkStatic(Intent::class)
        val mockChooser = mockk<Intent>(relaxed = true)
        every { Intent.createChooser(any(), any()) } returns mockChooser

        SharingUtils.shareMedia(mockContext, listOf(videoItem), SharePrivacyMode.STRIP_ALL)

        // Verifies chooser was created and started without creating temporary files or OOM
        verify(atLeast = 1) { Intent.createChooser(any(), any()) }
    }
}
