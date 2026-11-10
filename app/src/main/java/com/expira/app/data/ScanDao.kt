package com.expira.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert
    suspend fun insert(scan: ScanEntity): Long

    /** Newest first. */
    @Query("SELECT * FROM scans ORDER BY created_at DESC, id DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans ORDER BY created_at, id")
    suspend fun getAll(): List<ScanEntity>

    @Query("SELECT * FROM scans ORDER BY created_at DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScanEntity>>
}
