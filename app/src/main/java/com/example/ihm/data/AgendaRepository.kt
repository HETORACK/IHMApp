package com.example.ihm.data

import kotlinx.coroutines.flow.Flow

class AgendaRepository(private val recordatorioDao: RecordatorioDao) {
    val allRecordatorios: Flow<List<RecordatorioEntity>> = recordatorioDao.getAllRecordatorios()

    fun getRecordatoriosByDate(startOfDay: Long, endOfDay: Long): Flow<List<RecordatorioEntity>> {
        return recordatorioDao.getRecordatoriosByDate(startOfDay, endOfDay)
    }

    suspend fun insert(recordatorio: RecordatorioEntity): Long {
        return recordatorioDao.insert(recordatorio)
    }

    suspend fun update(recordatorio: RecordatorioEntity) {
        recordatorioDao.update(recordatorio)
    }

    suspend fun delete(recordatorio: RecordatorioEntity) {
        recordatorioDao.delete(recordatorio)
    }

    suspend fun deleteById(id: Int) {
        recordatorioDao.deleteById(id)
    }

    suspend fun markAsCompletado(id: Int) {
        recordatorioDao.markAsCompletado(id)
    }
}
