package com.hrshd1eux.imava.core.util

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurDetectorTest {

    @Test
    fun testComputeLaplacianVariance_smallBitmap_returnsZero() {
        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns 2
        every { bitmap.height } returns 2

        val variance = BlurDetector.computeLaplacianVariance(bitmap)
        assertEquals(0.0, variance, 0.001)
    }

    @Test
    fun testComputeLaplacianVariance_uniformBitmap_hasZeroVariance() {
        val width = 10
        val height = 10
        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every { bitmap.getPixels(any(), any(), any(), any(), any(), any(), any()) } answers {
            val array = it.invocation.args[0] as IntArray
            // Uniform gray color 0xFF808080
            array.fill(0xFF808080.toInt())
        }

        val variance = BlurDetector.computeLaplacianVariance(bitmap)
        assertEquals(0.0, variance, 0.001)
    }

    @Test
    fun testComputeLaplacianVariance_highContrastEdges_hasHighVariance() {
        val width = 10
        val height = 10
        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        every { bitmap.getPixels(any(), any(), any(), any(), any(), any(), any()) } answers {
            val array = it.invocation.args[0] as IntArray
            // Alternating black and white pixels (checkerboard)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val isWhite = (x + y) % 2 == 0
                    array[y * width + x] = if (isWhite) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                }
            }
        }

        val variance = BlurDetector.computeLaplacianVariance(bitmap)
        assertTrue("Variance for alternating edge pattern should exceed threshold", variance > BlurDetector.BLUR_THRESHOLD)
    }
}
