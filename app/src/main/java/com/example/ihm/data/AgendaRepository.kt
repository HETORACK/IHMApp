package com.example.ihm.data

import kotlinx.coroutines.flow.Flow

class AgendaRepository(private val recordatorioDao: RecordatorioDao) {
    val allRecordatorios: Flow<List<RecordatorioEntity>> = recordatorioDao.getAllRecordatorios()

    suspend fun insert(recordatorio: RecordatorioEntity) {
        recordatorioDao.insert(recordatorio)
    }

    suspend fun delete(recordatorio: RecordatorioEntity) {
        recordatorioDao.delete(recordatorio)
    }
}
