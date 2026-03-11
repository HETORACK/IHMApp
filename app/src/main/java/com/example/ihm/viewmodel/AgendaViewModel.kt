package com.example.ihm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ihm.data.AgendaRepository
import com.example.ihm.data.RecordatorioEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AgendaViewModel(private val repository: AgendaRepository) : ViewModel() {

    val recordatorios: StateFlow<List<RecordatorioEntity>> = repository.allRecordatorios
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun agregarRecordatorio(recordatorio: RecordatorioEntity) {
        viewModelScope.launch {
            repository.insert(recordatorio)
        }
    }

    fun eliminarRecordatorio(recordatorio: RecordatorioEntity) {
        viewModelScope.launch {
            repository.delete(recordatorio)
        }
    }
}
