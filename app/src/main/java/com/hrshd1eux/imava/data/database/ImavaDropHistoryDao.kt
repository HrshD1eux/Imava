package com.hrshd1eux.imava.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImavaDropHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ImavaDropHistoryEntity): Long

    @Query("SELECT * FROM imava_drop_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<ImavaDropHistoryEntity>>

    @Query("SELECT * FROM imava_drop_history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<ImavaDropHistoryEntity>

    @Query("SELECT COUNT(*) FROM imava_drop_history")
    fun getHistoryCountFlow(): Flow<Int>

    @Query("DELETE FROM imava_drop_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM imava_drop_history")
    suspend fun clearAll()
}
