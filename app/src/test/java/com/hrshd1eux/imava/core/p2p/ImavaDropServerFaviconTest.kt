package com.hrshd1eux.imava.core.p2p

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hrshd1eux.imava.R
import com.hrshd1eux.imava.data.model.MediaItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Base64

class ImavaDropServerFaviconTest {

    private val pngHeader = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
    private lateinit var mockBitmap: Bitmap
    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources

    @Before
    fun setUp() {
        mockkStatic(BitmapFactory::class)
        mockkStatic(Bitmap::class)

        mockBitmap = mockk(relaxed = true)
        every { mockBitmap.compress(any(), any(), any()) } answers {
            val stream = args[2] as OutputStream
            stream.write(pngHeader)
            true
        }
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } returns mockBitmap
        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any<Bitmap.Config>()) } returns mockBitmap

        mockResources = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.resources } returns mockResources
        every { BitmapFactory.decodeResource(mockResources, R.drawable.ic_imava_logo) } returns mockBitmap

        ImavaDropServer.start(mockContext, emptyList())
    }

    @After
    fun tearDown() {
        ImavaDropServer.stop(stopService = false)
        unmockkStatic(BitmapFactory::class)
        unmockkStatic(Bitmap::class)
    }

    @Test
    fun getFaviconBytes_returnsValidPngHeader() {
        val bytes = ImavaDropServer.getFaviconBytes()
        assertNotNull("Favicon bytes should not be null", bytes)
        assertTrue("Favicon bytes should be non-empty", bytes.isNotEmpty())

        assertEquals(0x89.toByte(), bytes[0])
        assertEquals('P'.code.toByte(), bytes[1])
        assertEquals('N'.code.toByte(), bytes[2])
        assertEquals('G'.code.toByte(), bytes[3])
    }

    @Test
    fun getFaviconBase64_returnsValidBase64Encoding() {
        val b64 = ImavaDropServer.getFaviconBase64()
        assertNotNull("Favicon base64 should not be null", b64)
        assertTrue("Favicon base64 should not be empty", b64.isNotEmpty())

        val decoded = Base64.getDecoder().decode(b64)
        assertEquals(0x89.toByte(), decoded[0])
        assertEquals('P'.code.toByte(), decoded[1])
        assertEquals('N'.code.toByte(), decoded[2])
        assertEquals('G'.code.toByte(), decoded[3])
    }

    @Test
    fun serveFavicon_writesHttp200AndPngHeader() {
        val output = ByteArrayOutputStream()
        ImavaDropServer.serveFavicon(output)
        val response = output.toByteArray()
        val responseString = String(response, Charsets.UTF_8)

        assertTrue("Response should contain HTTP/1.1 200 OK", responseString.startsWith("HTTP/1.1 200 OK"))
        assertTrue("Response should have Content-Type: image/png", responseString.contains("Content-Type: image/png"))
    }

    @Test
    fun startAndUpdateItems_filtersOutHiddenAndVaultItems() {
        val normalItem = MediaItem.Photo(
            id = 1L,
            uri = mockk(relaxed = true),
            path = "/storage/emulated/0/DCIM/Camera/IMG_1.jpg",
            mimeType = "image/jpeg",
            dateTaken = 1000L,
            size = 1024L,
            width = 100,
            height = 100,
            bucketId = 1L,
            bucketName = "Camera"
        )
        val hiddenItem = MediaItem.Photo(
            id = 2L,
            uri = mockk(relaxed = true),
            path = "/storage/emulated/0/DCIM/Camera/IMG_2.jpg",
            mimeType = "image/jpeg",
            dateTaken = 1000L,
            size = 2048L,
            width = 100,
            height = 100,
            bucketId = 1L,
            bucketName = "Camera",
            isHidden = true
        )
        val vaultPathItem = MediaItem.Photo(
            id = 3L,
            uri = mockk(relaxed = true),
            path = "/data/user/0/com.hrshd1eux.imava/files/vault/encrypted_3.bin",
            mimeType = "image/jpeg",
            dateTaken = 1000L,
            size = 4096L,
            width = 100,
            height = 100,
            bucketId = 1L,
            bucketName = "Vault"
        )

        ImavaDropServer.start(mockContext, listOf(normalItem, hiddenItem, vaultPathItem))
        assertEquals(1, ImavaDropServer.state.value.sharedItems.size)
        assertEquals(normalItem.id, ImavaDropServer.state.value.sharedItems[0].id)
        assertEquals(1024L, ImavaDropServer.state.value.totalBytes)

        ImavaDropServer.updateItems(listOf(normalItem, hiddenItem, vaultPathItem))
        assertEquals(1, ImavaDropServer.state.value.sharedItems.size)
        assertEquals(normalItem.id, ImavaDropServer.state.value.sharedItems[0].id)
        assertEquals(1024L, ImavaDropServer.state.value.totalBytes)
    }
}
