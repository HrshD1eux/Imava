package com.hrshd1eux.imava.core.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PureQrCodeEncoderTest {

    @Test
    fun encodeToMatrix_lanUrl_generatesValidMatrix() {
        val testUrl = "http://192.168.1.100:8765"
        val matrix = PureQrCodeEncoder.encodeToMatrix(testUrl)

        assertNotNull("Matrix should not be null for valid LAN URL", matrix)
        val m = matrix!!
        val size = m.size
        // URL length 26 chars. Mode(4) + count(8) + 26*8 = 220 bits = 28 bytes.
        // Version 2 has 28 data bytes. Size = 17 + 4*2 = 25.
        assertTrue("QR size should be between 21 and 45 for standard URLs", size in 21..45)

        // Verify top-left finder pattern (7x7)
        // Outer border black: (0,0) to (0,6), (6,0) to (6,6), (0,0) to (6,0), (0,6) to (6,6)
        assertTrue(m[0][0])
        assertTrue(m[0][6])
        assertTrue(m[6][0])
        assertTrue(m[6][6])
        // Inner 3x3 black: (2,2) to (4,4)
        assertTrue(m[2][2])
        assertTrue(m[3][3])
        assertTrue(m[4][4])

        // Verify top-right finder pattern
        assertTrue(m[0][size - 7])
        assertTrue(m[0][size - 1])
        assertTrue(m[6][size - 7])
        assertTrue(m[6][size - 1])
        assertTrue(m[3][size - 4])

        // Verify bottom-left finder pattern
        assertTrue(m[size - 7][0])
        assertTrue(m[size - 1][0])
        assertTrue(m[size - 7][6])
        assertTrue(m[size - 1][6])
        assertTrue(m[size - 4][3])
    }

    @Test
    fun encodeToMatrix_blankContent_returnsNull() {
        assertNull(PureQrCodeEncoder.encodeToMatrix(""))
        assertNull(PureQrCodeEncoder.encodeToMatrix("   "))
    }

    @Test
    fun encodeToMatrix_version1Capacity() {
        // Version 1 EC Level M holds 16 data codewords.
        // Mode (4 bits) + Count (8 bits) = 12 bits. 16 * 8 = 128 bits.
        // Max payload in byte mode for V1-M = (128 - 12) / 8 = 14 bytes.
        val v1Text = "12345678901234"
        val matrix = PureQrCodeEncoder.encodeToMatrix(v1Text)
        assertNotNull(matrix)
        assertEquals(21, matrix!!.size) // Version 1 is 21x21
    }
}
