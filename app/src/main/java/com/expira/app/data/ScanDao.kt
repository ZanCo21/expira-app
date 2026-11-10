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

    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun getById(id: Long): ScanEntity?

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "UPDATE scans SET reminder_days_before = :daysBefore, reminder_epoch_day = :epochDay, " +
            "reminder_status = :status WHERE id = :id",
    )
    suspend fun updateReminder(id: Long, daysBefore: Int?, epochDay: Long?, status: String?)

    @Query("SELECT * FROM scans ORDER BY created_at, id")
    suspend fun getAll(): List<ScanEntity>

    @Query("SELECT * FROM scans ORDER BY created_at DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScanEntity>>
}
