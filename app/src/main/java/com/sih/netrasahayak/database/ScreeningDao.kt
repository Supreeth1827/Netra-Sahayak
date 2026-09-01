package com.sih.netrasahayak.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreeningDao {

    @Insert
    suspend fun insert(screening: ScreeningEntity): Long

    @Query("SELECT * FROM screenings ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ScreeningEntity>>

    @Query("SELECT * FROM screenings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScreeningEntity?

    @Query("SELECT * FROM screenings WHERE synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsynced(): List<ScreeningEntity>

    @Query("SELECT COUNT(*) FROM screenings WHERE synced = 0")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE screenings SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM screenings WHERE id = :id")
    suspend fun deleteById(id: Long)
}
