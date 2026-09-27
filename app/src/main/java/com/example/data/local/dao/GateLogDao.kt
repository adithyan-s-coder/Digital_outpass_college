package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.GateLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GateLogDao {
    @Query("SELECT * FROM gate_logs ORDER BY timestamp DESC")
    fun getAllGateLogsFlow(): Flow<List<GateLogEntity>>

    @Query("SELECT * FROM gate_logs ORDER BY timestamp DESC")
    suspend fun getAllGateLogs(): List<GateLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGateLog(log: GateLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGateLogs(logs: List<GateLogEntity>)

    @Query("DELETE FROM gate_logs")
    suspend fun deleteAll()
}
