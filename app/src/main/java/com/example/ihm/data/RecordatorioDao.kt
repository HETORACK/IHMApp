package com.example.ihm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordatorioDao {
    @Query("SELECT * FROM recordatorios ORDER BY fecha ASC")
    fun getAllRecordatorios(): Flow<List<RecordatorioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recordatorio: RecordatorioEntity)

    @Delete
    suspend fun delete(recordatorio: RecordatorioEntity)
}
