package com.hrshd1eux.imava.core.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

object VideoCompressor {

    enum class CompressionPreset(
        val title: String,
        val subtitle: String,
        val maxDimension: Int,
        val defaultBitrate: Int
    ) {
        DISCORD_EMAIL("Discord / Email (<24 MB)", "Fits under 24 MB limit for fast sharing", 1280, 2_000_000),
        SOCIAL_BALANCED("Social & Web (1080p)", "70% space savings with high visual quality", 1920, 3_200_000),
        ULTRA_COMPACT("Ultra Compact (720p)", "Maximum space savings for quick uploads", 1280, 1_200_000)
    }

    data class VideoInfo(
        val width: Int,
        val height: Int,
        val durationMs: Long,
        val sizeBytes: Long,
        val rotation: Int
    )

    fun getVideoInfo(context: Context, uri: Uri): VideoInfo? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

            var size = 0L
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    size = pfd.statSize
                }
            } catch (_: Exception) {}

            VideoInfo(w, h, dur, size, rot)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun calculateTargetBitrate(preset: CompressionPreset, durationSecs: Long): Int {
        return if (preset == CompressionPreset.DISCORD_EMAIL) {
            val targetBytes = 22L * 1024L * 1024L // 22 MB target
            val calculated = ((targetBytes * 8L) / maxOf(1L, durationSecs)).toInt()
            calculated.coerceIn(500_000, 3_500_000)
        } else {
            preset.defaultBitrate
        }
    }

    fun calculateTargetDimensions(width: Int, height: Int, maxDim: Int): Pair<Int, Int> {
        val isLandscape = width >= height
        return if (isLandscape) {
            if (width > maxDim) {
                val scale = maxDim.toFloat() / width
                Pair(maxDim, ((height * scale).toInt() / 2) * 2)
            } else {
                Pair((width / 2) * 2, (height / 2) * 2)
            }
        } else {
            if (height > maxDim) {
                val scale = maxDim.toFloat() / height
                Pair(((width * scale).toInt() / 2) * 2, maxDim)
            } else {
                Pair((width / 2) * 2, (height / 2) * 2)
            }
        }
    }

    suspend fun compressVideo(
        context: Context,
        inputUri: Uri,
        preset: CompressionPreset,
        onProgress: (Float) -> Unit
    ): Uri? = withContext(Dispatchers.IO) {
        val info = getVideoInfo(context, inputUri) ?: return@withContext null
        val durationSecs = maxOf(1L, info.durationMs / 1000)

        val targetBitrate = calculateTargetBitrate(preset, durationSecs)
        val (targetWidth, targetHeight) = calculateTargetDimensions(info.width, info.height, preset.maxDimension)

        val tempOutputFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.mp4")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            val pfd = context.contentResolver.openFileDescriptor(inputUri, "r") ?: return@withContext null
            extractor.setDataSource(pfd.fileDescriptor)

            var videoTrackIdx = -1
            var audioTrackIdx = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && videoTrackIdx == -1) {
                    videoTrackIdx = i
                    videoFormat = format
                } else if (mime.startsWith("audio/") && audioTrackIdx == -1) {
                    audioTrackIdx = i
                    audioFormat = format
                }
            }

            if (videoTrackIdx == -1 || videoFormat == null) {
                pfd.close()
                return@withContext null
            }

            muxer = MediaMuxer(tempOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(info.rotation)

            val mimeType = "video/avc"
            val outputFormat = MediaFormat.createVideoFormat(mimeType, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(mimeType)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            extractor.selectTrack(videoTrackIdx)
            val decoder = MediaCodec.createDecoderByType(videoFormat.getString(MediaFormat.KEY_MIME)!!)
            decoder.configure(videoFormat, inputSurface, null, 0)
            decoder.start()

            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            var isDecoderEOS = false
            var isEncoderEOS = false
            val durationUs = info.durationMs * 1000L

            while (!isEncoderEOS) {
                // Feed decoder
                if (!isDecoderEOS) {
                    val inBufIdx = decoder.dequeueInputBuffer(10_000)
                    if (inBufIdx >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inBufIdx)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inBufIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isDecoderEOS = true
                        } else {
                            val pts = extractor.sampleTime
                            decoder.queueInputBuffer(inBufIdx, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                // Drain decoder
                var outBufIdx = decoder.dequeueOutputBuffer(bufferInfo, 10_000)
                while (outBufIdx >= 0) {
                    val render = bufferInfo.size > 0
                    decoder.releaseOutputBuffer(outBufIdx, render)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        encoder.signalEndOfInputStream()
                        break
                    }
                    outBufIdx = decoder.dequeueOutputBuffer(bufferInfo, 0)
                }

                // Drain encoder
                var encBufIdx = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                while (encBufIdx >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        encoder.releaseOutputBuffer(encBufIdx, false)
                        encBufIdx = encoder.dequeueOutputBuffer(bufferInfo, 0)
                        continue
                    }

                    if (bufferInfo.size > 0) {
                        if (!muxerStarted) {
                            muxerVideoTrack = muxer.addTrack(encoder.outputFormat)
                            if (audioFormat != null && audioTrackIdx != -1) {
                                muxerAudioTrack = muxer.addTrack(audioFormat)
                            }
                            muxer.start()
                            muxerStarted = true
                        }

                        val encodedData = encoder.getOutputBuffer(encBufIdx)!!
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)

                        muxer.writeSampleData(muxerVideoTrack, encodedData, bufferInfo)

                        if (durationUs > 0) {
                            val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f)
                            onProgress(progress * 0.9f)
                        }
                    }

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEncoderEOS = true
                        break
                    }

                    encoder.releaseOutputBuffer(encBufIdx, false)
                    encBufIdx = encoder.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            try { decoder.stop(); decoder.release() } catch (_: Exception) {}
            try { encoder.stop(); encoder.release() } catch (_: Exception) {}
            try { inputSurface.release() } catch (_: Exception) {}

            // Remux Audio Track cleanly
            if (audioTrackIdx != -1 && muxerStarted && muxerAudioTrack != -1) {
                extractor.unselectTrack(videoTrackIdx)
                extractor.selectTrack(audioTrackIdx)
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                val audioBuf = ByteBuffer.allocateDirect(1024 * 512)
                val audioBufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(audioBuf, 0)
                    if (sampleSize < 0) break

                    audioBufferInfo.offset = 0
                    audioBufferInfo.size = sampleSize
                    audioBufferInfo.presentationTimeUs = extractor.sampleTime
                    audioBufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxerAudioTrack, audioBuf, audioBufferInfo)
                    extractor.advance()
                }
            }

            onProgress(0.98f)
            pfd.close()

            if (muxerStarted) {
                try {
                    muxer.stop()
                    muxer.release()
                } catch (_: Exception) {}
            }

            // Save to Public MediaStore Movies/ImavaCompress
            val savedUri = saveToMediaStore(context, tempOutputFile, preset.name.lowercase())
            onProgress(1f)
            savedUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { extractor.release() } catch (_: Exception) {}
            try { tempOutputFile.delete() } catch (_: Exception) {}
        }
    }

    private fun saveToMediaStore(context: Context, file: File, suffix: String): Uri? {
        val fileName = "Compressed_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ImavaCompress")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, values) ?: return null

        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                FileInputStream(file).use { fis ->
                    fis.copyTo(os, 65536)
                }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) {}
            null
        }
    }
}
