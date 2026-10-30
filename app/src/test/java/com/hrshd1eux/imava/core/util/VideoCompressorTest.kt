package com.hrshd1eux.imava.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCompressorTest {

    @Test
    fun testDiscordBitrateCalculationForLongVideo() {
        // 120 seconds (2 minutes)
        // 22 MB = 22 * 1024 * 1024 * 8 = 184,549,376 bits
        // bitrate = 184,549,376 / 120 = ~1,537,911 bps
        val bitrate = VideoCompressor.calculateTargetBitrate(VideoCompressor.CompressionPreset.DISCORD_EMAIL, 120)
        assertTrue("Bitrate should fit 22 MB in 120 seconds: $bitrate", bitrate in 1_500_000..1_550_000)
    }

    @Test
    fun testDiscordBitrateClampingForVeryShortVideo() {
        // 5 seconds video: raw calculation would be ~36 Mbps, but clamped to 3.5 Mbps max
        val bitrate = VideoCompressor.calculateTargetBitrate(VideoCompressor.CompressionPreset.DISCORD_EMAIL, 5)
        assertEquals(3_500_000, bitrate)
    }

    @Test
    fun testDiscordBitrateClampingForVeryLongVideo() {
        // 1000 seconds video: raw calculation would be ~184 kbps, clamped to 500 kbps min
        val bitrate = VideoCompressor.calculateTargetBitrate(VideoCompressor.CompressionPreset.DISCORD_EMAIL, 1000)
        assertEquals(500_000, bitrate)
    }

    @Test
    fun testSocialAndUltraCompactBitrates() {
        val social = VideoCompressor.calculateTargetBitrate(VideoCompressor.CompressionPreset.SOCIAL_BALANCED, 60)
        assertEquals(3_200_000, social)

        val ultra = VideoCompressor.calculateTargetBitrate(VideoCompressor.CompressionPreset.ULTRA_COMPACT, 60)
        assertEquals(1_200_000, ultra)
    }

    @Test
    fun testTargetDimensions4KLandscapeTo1080p() {
        val (w, h) = VideoCompressor.calculateTargetDimensions(3840, 2160, 1920)
        assertEquals(1920, w)
        assertEquals(1080, h)
        // Both dimensions must be even for video codecs
        assertEquals(0, w % 2)
        assertEquals(0, h % 2)
    }

    @Test
    fun testTargetDimensions4KPortraitTo1080p() {
        val (w, h) = VideoCompressor.calculateTargetDimensions(2160, 3840, 1920)
        assertEquals(1080, w)
        assertEquals(1920, h)
        assertEquals(0, w % 2)
        assertEquals(0, h % 2)
    }

    @Test
    fun testTargetDimensionsSmallerThanMaxNotUpscaled() {
        val (w, h) = VideoCompressor.calculateTargetDimensions(640, 480, 1280)
        assertEquals(640, w)
        assertEquals(480, h)
    }
}
