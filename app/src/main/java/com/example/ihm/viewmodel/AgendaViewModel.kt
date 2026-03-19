package com.example.ihm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ihm.data.AgendaRepository
import com.example.ihm.data.RecordatorioEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class AgendaViewModel(private val repository: AgendaRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis)
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    // Todos los recordatorios cargados una sola vez
    private val allRecordatorios = repository.allRecordatorios.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Exponer todos los eventos para el calendario
    val allEvents: StateFlow<List<RecordatorioEntity>> = allRecordatorios
        .map { list -> list.filter { it.categoria == "Evento" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Eventos del día seleccionado (incluyendo anuales)
    val dailyEvents: StateFlow<List<RecordatorioEntity>> = combine(allRecordatorios, _selectedDate) { records, selectedTs ->
        records.filter { it.categoria == "Evento" && isRelevantForDate(it, selectedTs) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tareas del día seleccionado (incluyendo rutinas)
    val recordatorios: StateFlow<List<RecordatorioEntity>> = combine(allRecordatorios, _selectedDate) { records, selectedTs ->
        records.filter { it.categoria != "Evento" && isRelevantForDate(it, selectedTs) }
            .sortedBy { it.completado }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Eventos próximos (7 días)
    val upcomingEvents: StateFlow<List<RecordatorioEntity>> = combine(allRecordatorios, _selectedDate) { records, selectedTs ->
        val today = getStartOfDay(System.currentTimeMillis())
        val nextWeek = today + 7L * 24 * 60 * 60 * 1000
        
        records.filter { record ->
            record.categoria == "Evento" && 
            record.fecha in today..nextWeek && 
            !isSameDay(record.fecha, selectedTs)
        }.sortedBy { it.fecha }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun isRelevantForDate(record: RecordatorioEntity, selectedTs: Long): Boolean {
        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedTs }
        val recordCal = Calendar.getInstance().apply { timeInMillis = record.fecha }

        // 1. Es el mismo día exacto
        if (isSameDay(record.fecha, selectedTs)) return true

        // 2. Es una rutina (se repite ciertos días de la semana)
        if (record.diasSemana.isNotEmpty()) {
            val dayOfWeek = selectedCal.get(Calendar.DAY_OF_WEEK)
            val convertedDay = when(dayOfWeek) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 0
            }
            return record.diasSemana.contains(convertedDay) && selectedTs >= getStartOfDay(record.fecha)
        }

        // 3. Es anual (cumpleaños)
        if (record.esAnual) {
            return recordCal.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH) &&
                   recordCal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
        }

        return false
    }

    private fun getStartOfDay(ts: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun selectDate(timestamp: Long) {
        _selectedDate.value = getStartOfDay(timestamp)
    }

    fun nextDay() {
        _selectedDate.value += 24 * 60 * 60 * 1000
    }

    fun previousDay() {
        _selectedDate.value -= 24 * 60 * 60 * 1000
    }

    fun actualizarRecordatorio(recordatorio: RecordatorioEntity) {
        viewModelScope.launch {
            repository.update(recordatorio)
        }
    }

    fun toggleCompletado(recordatorio: RecordatorioEntity) {
        viewModelScope.launch {
            repository.update(recordatorio.copy(completado = !recordatorio.completado))
        }
    }
    
    fun eliminarRecordatorio(recordatorio: RecordatorioEntity) {
        viewModelScope.launch {
            repository.delete(recordatorio)
        }
    }
}
