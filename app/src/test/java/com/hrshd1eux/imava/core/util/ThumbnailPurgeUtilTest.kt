package com.hrshd1eux.imava.core.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream

class ThumbnailPurgeUtilTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testPurgeGhostThumbnails_deletesThumbnailFilesAndPreservesNomedia() = runBlocking {
        val mockContext = mockk<Context>(relaxed = true)
        val cacheDir = tempFolder.newFolder("cache")
        val imageCacheDir = File(cacheDir, "image_cache").apply { mkdirs() }
        val extCacheDir = tempFolder.newFolder("ext_cache")

        every { mockContext.cacheDir } returns cacheDir
        every { mockContext.externalCacheDir } returns extCacheDir

        // Create ghost thumbnail files
        val ghostThumb1 = File(imageCacheDir, "thumb_101.jpg").apply {
            FileOutputStream(this).use { it.write("fake thumbnail data 1".toByteArray()) }
        }
        val ghostThumb2 = File(imageCacheDir, "thumb_102.webp").apply {
            FileOutputStream(this).use { it.write("fake thumbnail data 2".toByteArray()) }
        }
        val noMediaFile = File(imageCacheDir, ".nomedia").apply {
            createNewFile()
        }

        assertTrue("Ghost thumb 1 should exist", ghostThumb1.exists())
        assertTrue("Ghost thumb 2 should exist", ghostThumb2.exists())
        assertTrue(".nomedia should exist", noMediaFile.exists())

        // Perform scan
        val scanResult = ThumbnailPurgeUtil.scanGhostThumbnails(mockContext)
        assertTrue("Scan should find at least 2 ghost files", scanResult.fileCount >= 2)

        // Perform purge
        var progressCount = 0
        val purgeResult = ThumbnailPurgeUtil.purgeAndShredGhostThumbnails(mockContext) { current, _ ->
            progressCount = current
        }

        assertTrue("Purge should report success", purgeResult.success)
        assertTrue("Progress should have been reported", progressCount >= 2)
        assertTrue("At least 2 files purged", purgeResult.filesPurged >= 2)
        assertFalse("Ghost thumb 1 should be shredded", ghostThumb1.exists())
        assertFalse("Ghost thumb 2 should be shredded", ghostThumb2.exists())
        assertTrue(".nomedia must be preserved", noMediaFile.exists())
    }
}
