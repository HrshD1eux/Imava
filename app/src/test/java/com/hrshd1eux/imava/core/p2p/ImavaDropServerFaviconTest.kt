package com.hrshd1eux.imava.core.p2p

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hrshd1eux.imava.R
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
}
