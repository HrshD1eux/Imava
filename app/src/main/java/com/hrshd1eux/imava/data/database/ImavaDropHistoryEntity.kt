package com.hrshd1eux.imava.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "imava_drop_history",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["direction"])
    ]
)
data class ImavaDropHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fileName: String,
    val fileSizeBytes: Long,
    val direction: String, // "SENT" or "RECEIVED"
    val clientIp: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS", // "SUCCESS" or "FAILED"
    val mediaUri: String? = null
) {
    companion object {
        const val DIRECTION_SENT = "SENT"
        const val DIRECTION_RECEIVED = "RECEIVED"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILED = "FAILED"
    }
}
