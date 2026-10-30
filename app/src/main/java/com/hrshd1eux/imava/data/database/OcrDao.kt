package com.hrshd1eux.imava.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OcrDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOcr(entity: MediaOcrEntity)

    @Query("SELECT ocrText FROM media_ocr WHERE mediaId = :mediaId")
    suspend fun getOcrText(mediaId: Long): String?

    @Query("SELECT mediaId FROM media_ocr WHERE LOWER(ocrText) LIKE '%' || LOWER(:query) || '%'")
    suspend fun searchMediaIdsByOcr(query: String): List<Long>

    @Query("DELETE FROM media_ocr WHERE mediaId = :mediaId")
    suspend fun deleteOcr(mediaId: Long)

    @Query("DELETE FROM media_ocr WHERE mediaId IN (:mediaIds)")
    suspend fun deleteOcrBatch(mediaIds: List<Long>)

    @Query("SELECT COUNT(*) FROM media_ocr")
    suspend fun getScannedOcrCount(): Int
}
