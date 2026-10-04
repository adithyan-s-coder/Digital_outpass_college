package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.OutpassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutpassDao {
    @Query("SELECT * FROM outpasses ORDER BY appliedAt DESC")
    fun getAllOutpassesFlow(): Flow<List<OutpassEntity>>

    @Query("SELECT * FROM outpasses ORDER BY appliedAt DESC")
    suspend fun getAllOutpasses(): List<OutpassEntity>

    @Query("SELECT * FROM outpasses WHERE id = :id LIMIT 1")
    suspend fun getOutpassById(id: String): OutpassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutpass(outpass: OutpassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutpasses(outpasses: List<OutpassEntity>)

    @Update
    suspend fun updateOutpass(outpass: OutpassEntity)

    @Query("DELETE FROM outpasses WHERE id = :id")
    suspend fun deleteOutpassById(id: String)

    @Query("DELETE FROM outpasses")
    suspend fun deleteAll()
}
