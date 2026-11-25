package com.hrshd1eux.imava.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hrshd1eux.imava.data.model.MediaItem
import java.io.File

@Entity(
    tableName = "discovered_media",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["dateTaken"]),
        Index(value = ["bucketId"])
    ]
)
data class DiscoveredMediaEntity(
    @PrimaryKey val id: Long,
    val path: String,
    val mimeType: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val bucketId: Long = 0L,
    val bucketName: String = "",
    val isVideo: Boolean = false
) {
    fun toMediaItem(customUri: android.net.Uri? = null): MediaItem {
        val file = File(path)
        val uri = customUri ?: try {
            android.net.Uri.fromFile(file)
        } catch (_: Throwable) {
            null
        } ?: try {
            android.net.Uri.parse("file://${file.absolutePath}")
        } catch (_: Throwable) {
            null
        } ?: android.net.Uri.EMPTY
        return if (isVideo) {
            MediaItem.Video(
                id = id,
                uri = uri,
                path = path,
                mimeType = mimeType,
                dateTaken = dateTaken,
                size = size,
                width = width,
                height = height,
                durationMs = durationMs,
                bucketId = bucketId,
                bucketName = bucketName
            )
        } else {
            MediaItem.Photo(
                id = id,
                uri = uri,
                path = path,
                mimeType = mimeType,
                dateTaken = dateTaken,
                size = size,
                width = width,
                height = height,
                bucketId = bucketId,
                bucketName = bucketName
            )
        }
    }

    companion object {
        fun fromMediaItem(item: MediaItem): DiscoveredMediaEntity {
            val isVid = item is MediaItem.Video
            val dur = if (item is MediaItem.Video) item.durationMs else 0L
            return DiscoveredMediaEntity(
                id = item.id,
                path = item.path,
                mimeType = item.mimeType,
                dateTaken = item.dateTaken,
                size = item.size,
                width = item.width,
                height = item.height,
                durationMs = dur,
                bucketId = item.bucketId,
                bucketName = item.bucketName,
                isVideo = isVid
            )
        }
    }
}
