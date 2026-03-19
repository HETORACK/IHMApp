package com.example.ihm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordatorioDao {
    @Query("SELECT * FROM recordatorios ORDER BY fecha ASC")
    fun getAllRecordatorios(): Flow<List<RecordatorioEntity>>

    @Query("SELECT * FROM recordatorios WHERE fecha >= :startOfDay AND fecha <= :endOfDay ORDER BY fecha ASC")
    fun getRecordatoriosByDate(startOfDay: Long, endOfDay: Long): Flow<List<RecordatorioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recordatorio: RecordatorioEntity): Long

    @Update
    suspend fun update(recordatorio: RecordatorioEntity)

    @Delete
    suspend fun delete(recordatorio: RecordatorioEntity)

    @Query("DELETE FROM recordatorios WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    @Query("UPDATE recordatorios SET completado = 1 WHERE id = :id")
    suspend fun markAsCompletado(id: Int)
}
