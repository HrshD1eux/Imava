package com.hrshd1eux.imava.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DiscoveredMediaDao {
    @Query("SELECT * FROM discovered_media ORDER BY dateTaken DESC")
    suspend fun getAllDiscoveredMedia(): List<DiscoveredMediaEntity>

    @Query("SELECT * FROM discovered_media WHERE bucketId = :bucketId ORDER BY dateTaken DESC")
    suspend fun getDiscoveredMediaByBucket(bucketId: Long): List<DiscoveredMediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBatch(items: List<DiscoveredMediaEntity>)

    @Query("DELETE FROM discovered_media WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM discovered_media WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM discovered_media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM discovered_media WHERE path LIKE '%/.thumbnails/%' OR path LIKE '%/thumbnails/%' OR path LIKE '%/.cache/%' OR path LIKE '%/cache/%' OR path LIKE '%/.thumbs/%' OR path LIKE '%/thumbs/%'")
    suspend fun purgeThumbnails(): Int

    @Query("SELECT COUNT(*) FROM discovered_media")
    suspend fun getCount(): Int

    @Query("DELETE FROM discovered_media")
    suspend fun clearAll()
}
