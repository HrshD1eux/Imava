package com.hrshd1eux.imava.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentScannerUtilTest {

    @Test
    fun testCalculateWarpDimensionsFlatRectangle() {
        val (w, h) = DocumentScannerUtil.calculateWarpDimensions(
            tlX = 0f, tlY = 0f,
            trX = 800f, trY = 0f,
            brX = 800f, brY = 1000f,
            blX = 0f, blY = 1000f
        )
        assertEquals(800, w)
        assertEquals(1000, h)
    }

    @Test
    fun testCalculateWarpDimensionsSkewedTrapezoid() {
        val (w, h) = DocumentScannerUtil.calculateWarpDimensions(
            tlX = 100f, tlY = 50f,
            trX = 750f, trY = 120f,
            brX = 900f, brY = 950f,
            blX = 50f, blY = 900f
        )
        assertTrue("Warped width should be >= 650: $w", w >= 650)
        assertTrue("Warped height should be >= 800: $h", h >= 800)
    }

    @Test
    fun testDocumentFilterEnum() {
        assertEquals(3, DocumentScannerUtil.DocumentFilter.values().size)
        assertTrue(DocumentScannerUtil.DocumentFilter.ORIGINAL in DocumentScannerUtil.DocumentFilter.values())
        assertTrue(DocumentScannerUtil.DocumentFilter.BW_HIGH_CONTRAST in DocumentScannerUtil.DocumentFilter.values())
        assertTrue(DocumentScannerUtil.DocumentFilter.CLEAN_ENHANCED in DocumentScannerUtil.DocumentFilter.values())
    }
}
