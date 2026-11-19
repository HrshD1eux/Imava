package com.hrshd1eux.imava.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WebDavSyncedDao {
    @Query("SELECT mediaId FROM webdav_synced_media")
    suspend fun getAllSyncedIds(): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM webdav_synced_media WHERE mediaId = :mediaId)")
    suspend fun isMediaSynced(mediaId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markSynced(entity: WebDavSyncedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markAllSynced(entities: List<WebDavSyncedEntity>)

    @Query("DELETE FROM webdav_synced_media")
    suspend fun clearAll()
}
