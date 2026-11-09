package com.hrshd1eux.imava.data.sync.webdav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavClientTest {

    @Test
    fun testBuildUrl_handlesTrailingAndLeadingSlashes() {
        val url = WebDavClient.buildUrl("https://nas.local:5005/remote.php/webdav/", "/Camera/2024/")
        assertEquals("https://nas.local:5005/remote.php/webdav/Camera/2024", url)
    }

    @Test
    fun testBuildUrl_encodesSpacesAndSpecialCharacters() {
        val url = WebDavClient.buildUrl("http://192.168.1.50:8080/dav", "Summer Vacation/My Photo (1).jpg")
        assertEquals("http://192.168.1.50:8080/dav/Summer%20Vacation/My%20Photo%20%281%29.jpg", url)
    }

    @Test
    fun testBuildUrl_emptyPathReturnsBaseUrl() {
        val url = WebDavClient.buildUrl("http://192.168.1.50:8080/dav/", "")
        assertEquals("http://192.168.1.50:8080/dav", url)
    }

    @Test
    fun testRemoteFileInfo_properties() {
        val info = RemoteFileInfo(exists = true, contentLength = 1048576L, etag = "abcd1234efgh")
        assertTrue(info.exists)
        assertEquals(1048576L, info.contentLength)
        assertEquals("abcd1234efgh", info.etag)

        val notFound = RemoteFileInfo(exists = false)
        assertFalse(notFound.exists)
        assertEquals(-1L, notFound.contentLength)
    }
}
