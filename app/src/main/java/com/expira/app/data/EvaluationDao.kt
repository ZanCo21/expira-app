package com.expira.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationDao {
    @Insert
    suspend fun insertAll(logs: List<EvaluationLogEntity>)

    /** Rows of the most recent test run (P0-P3 of one image). */
    @Query("SELECT * FROM evaluation_logs WHERE run_id = (SELECT MAX(run_id) FROM evaluation_logs) ORDER BY config")
    fun observeLatestRun(): Flow<List<EvaluationLogEntity>>

    @Query("SELECT * FROM evaluation_logs ORDER BY run_id, config")
    suspend fun getAll(): List<EvaluationLogEntity>
}
