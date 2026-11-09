package com.hrshd1eux.imava.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class MotionPhotoUtilTest {

    @Test
    fun testScanForMp4FromStream_detectsEmbeddedMp4FtypAtom() {
        val totalLength = 8192
        val dummyData = ByteArray(totalLength)

        // Fill with dummy JPEG bytes
        for (i in 0 until 2000) {
            dummyData[i] = (i % 256).toByte()
        }

        // At byte 2000: MP4 box header (4 bytes size + 'ftyp')
        // Atom size: 4 bytes (e.g. 0x00, 0x00, 0x00, 0x20)
        dummyData[2000] = 0x00
        dummyData[2001] = 0x00
        dummyData[2002] = 0x00
        dummyData[2003] = 0x20
        // 'ftyp'
        dummyData[2004] = 'f'.code.toByte()
        dummyData[2005] = 't'.code.toByte()
        dummyData[2006] = 'y'.code.toByte()
        dummyData[2007] = 'p'.code.toByte()

        val inputStream = ByteArrayInputStream(dummyData)
        val videoLength = MotionPhotoUtil.scanForMp4FromStream(inputStream, totalLength.toLong())

        // Video length should be totalLength (8192) - atomStart (2000) = 6192
        assertEquals(6192L, videoLength)
    }

    @Test
    fun testScanForMp4FromStream_noFtypAtom_returnsZero() {
        val totalLength = 8192
        val dummyData = ByteArray(totalLength) { 0x55.toByte() }

        val inputStream = ByteArrayInputStream(dummyData)
        val videoLength = MotionPhotoUtil.scanForMp4FromStream(inputStream, totalLength.toLong())

        assertEquals(0L, videoLength)
    }
}
