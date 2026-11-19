package com.hrshd1eux.imava.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "webdav_synced_media",
    indices = [Index(value = ["mediaId"])]
)
data class WebDavSyncedEntity(
    @PrimaryKey val mediaId: Long,
    val syncedAt: Long = System.currentTimeMillis()
)
