package com.hrshd1eux.imava.core.util

import org.junit.Assert.assertTrue
import org.junit.Test

class RawPreviewDecoderTest {

    @Test
    fun testRawExtensions_containsAllMajorCameraContainers() {
        val expected = listOf(
            "arw", "srf", "sr2", // Sony
            "cr2", "cr3",        // Canon
            "nef", "nrw",        // Nikon
            "raf",               // Fujifilm
            "orf",               // Olympus
            "rw2",               // Panasonic
            "pef",               // Pentax
            "srw",               // Samsung
            "dng"                // Adobe / Android DNG
        )

        for (ext in expected) {
            assertTrue("Expected extension $ext in RAW_EXTENSIONS", RawPreviewDecoder.RAW_EXTENSIONS.contains(ext))
        }
    }

    @Test
    fun testRawMimePrefixes_containsAllVendorMimeTypes() {
        val prefixes = RawPreviewDecoder.RAW_MIME_PREFIXES
        assertTrue(prefixes.contains("image/x-sony"))
        assertTrue(prefixes.contains("image/x-canon"))
        assertTrue(prefixes.contains("image/x-nikon"))
        assertTrue(prefixes.contains("image/x-fuji"))
        assertTrue(prefixes.contains("image/x-olympus"))
        assertTrue(prefixes.contains("image/x-panasonic"))
        assertTrue(prefixes.contains("image/x-pentax"))
        assertTrue(prefixes.contains("image/x-samsung"))
        assertTrue(prefixes.contains("image/x-adobe-dng"))
        assertTrue(prefixes.contains("image/x-raw"))
    }
}
