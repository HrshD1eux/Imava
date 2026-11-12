package com.hrshd1eux.imava.core.p2p

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImavaDropShareHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun extractShareUris_singleSendAndClipData_deduplicatesPreservingOrder() {
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        val clipItem1 = mockk<ClipData.Item> { every { uri } returns uri1 }
        val clipItem2 = mockk<ClipData.Item> { every { uri } returns uri2 }
        val clipData = mockk<ClipData> {
            every { itemCount } returns 2
            every { getItemAt(0) } returns clipItem1
            every { getItemAt(1) } returns clipItem2
        }
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_SEND
            every { getParcelableExtra<Uri>(Intent.EXTRA_STREAM) } returns uri1
            @Suppress("DEPRECATION")
            every { getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) } returns uri1
            every { this@mockk.clipData } returns clipData
        }

        val extracted = ImavaDropShareHelper.extractShareUris(intent)
        assertEquals(listOf(uri1, uri2), extracted)
    }

    @Test
    fun extractShareUris_sendMultiple_readsExtraStreamListAndNullSafeIntent() {
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_SEND_MULTIPLE
            every { getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) } returns arrayListOf(uri1, uri2)
            @Suppress("DEPRECATION")
            every { getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) } returns arrayListOf(uri1, uri2)
            every { clipData } returns null
        }

        assertEquals(listOf(uri1, uri2), ImavaDropShareHelper.extractShareUris(intent))
        assertTrue(ImavaDropShareHelper.extractShareUris(null).isEmpty())
    }

    @Test
    fun resolveConcreteMimeType_handlesMediaWildcardsExtensionsAndRejectsNonMedia() {
        assertEquals("image/png", ImavaDropShareHelper.resolveConcreteMimeType("image/PNG; charset=utf-8", "image/*", "pic.png"))
        assertEquals("image/jpeg", ImavaDropShareHelper.resolveConcreteMimeType("image/*", "image/*", "photo"))
        assertEquals("video/mp4", ImavaDropShareHelper.resolveConcreteMimeType("application/octet-stream", "*/*", "clip.mp4"))
        assertEquals("image/webp", ImavaDropShareHelper.resolveConcreteMimeType(null, "*/*", "sticker.webp"))
        assertNull(ImavaDropShareHelper.resolveConcreteMimeType("application/pdf", "application/pdf", "doc.pdf"))
        assertNull(ImavaDropShareHelper.resolveConcreteMimeType("text/plain", "text/plain", "notes.txt"))
    }

    @Test
    fun sanitizeFileName_stripsTraversalAndAppendsMissingExtension() {
        assertEquals("secret.jpg", ImavaDropShareHelper.sanitizeFileName("../../folder/secret.jpg", "image/jpeg", 0))
        assertEquals("vacation_2026_.png", ImavaDropShareHelper.sanitizeFileName("vacation:2026*.png", "image/png", 0))
        assertEquals("photo.webp", ImavaDropShareHelper.sanitizeFileName("photo", "image/webp", 0))
        assertEquals("shared_photo_1.jpg", ImavaDropShareHelper.sanitizeFileName("   ", "image/jpeg", 0))
        assertEquals("shared_video_2.mp4", ImavaDropShareHelper.sanitizeFileName(null, "video/mp4", 1))
    }

    @Test
    fun resolveUniqueTargetFile_avoidsCollisionsInStageDirectory() {
        val dir = tempFolder.newFolder("stage")
        val first = ImavaDropShareHelper.resolveUniqueTargetFile(dir, "IMG_001.jpg")
        assertEquals("IMG_001.jpg", first.name)
        first.writeText("one")

        val second = ImavaDropShareHelper.resolveUniqueTargetFile(dir, "IMG_001.jpg")
        assertEquals("IMG_001_1.jpg", second.name)
        second.writeText("two")

        val third = ImavaDropShareHelper.resolveUniqueTargetFile(dir, "IMG_001.jpg")
        assertEquals("IMG_001_2.jpg", third.name)
    }
}
