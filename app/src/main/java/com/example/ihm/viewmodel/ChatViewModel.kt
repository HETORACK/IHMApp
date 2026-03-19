package com.example.ihm.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ihm.data.AgendaRepository
import com.example.ihm.data.RecordatorioEntity
import com.example.ihm.data.SubTask
import com.example.ihm.network.*
import com.example.ihm.receiver.AlarmReceiver
import com.example.ihm.ui.ChatMessage
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class GeminiProductivityAction(
    val intent: String,
    val category: String? = null,
    val icono: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val subtasks: List<String>? = emptyList(),
    val time: String? = null,
    val date: String? = null,
    val timeLimit: String? = null,
    val dateLimit: String? = null,
    val hourInterval: Int? = null,
    val minuteInterval: Int? = null,
    val daysOfWeek: List<Int>? = emptyList(),
    val excludedDates: List<String>? = emptyList(),
    val isAnnual: Boolean? = false,
    val id: Int? = null
)

class ChatViewModel(
    application: Application,
    private val repository: AgendaRepository
) : AndroidViewModel(application) {
    private val _messages = MutableStateFlow(listOf(
        ChatMessage("¡Hola! Soy Nero, tu asistente IA. ¿En qué puedo ayudarte hoy?", false)
    ))
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _draftMessage = MutableStateFlow("")
    val draftMessage = _draftMessage.asStateFlow()

    private val apiKey = "AIzaSyApVvl8Ii4hdaauMUviU8WrO7OKc7oHj_4"

    private val systemPrompt = """
        Eres Nero, un asistente. 
        SI LA INTENCIÓN ES CREATE:
        Genera el JSON con el campo "intent": "CREATE" y llena los campos necesarios. El "id" debe ser null.
        SI LA INTENCIÓN ES DELETE, UPDATE o READ:
            1. Si no hay una lista bajo "CONTEXTO_ACTUAL", responde solo: NEED_CONTEXT.
            2. Si hay contexto:
               - DELETE: Devuelve {"intent": "DELETE", "id": [ID_DEL_CONTEXTO]}.
               - UPDATE: Devuelve el JSON completo con "intent": "UPDATE", el "id" correspondiente y SOLO los campos que el usuario quiere cambiar actualizados (mantén los demás como los recibiste).
               - READ: Devuelve {"intent": "READ", "summary": "Aquí tienes la información sobre..."}.
        FORMATO JSON:
        {
             "intent": "CREATE/UPDATE/DELETE/READ",
             "id": id_numerico_si_aplica,
             "category": "Evento/Alarma/Tarea/Rutina", 
             "icono": "Medicina/CitaMedica/Ejercicio/Rutina/Social/Profesional/Compras/Alimentos/Bebida/Cocina/Llamada/Despertador/Temporizador/Cumpleaños/FestivoMexicano/Fiesta/EventoTrabajo",
             "title": "título",
             "summary": "resumen",
             "subtasks": ["paso 1", "paso 2"], 
             "time": "HH:mm",
             "date": "YYYY-MM-DD",
             "daysOfWeek": [1, 2, 3, 4, 5, 6, 7], 
             "isAnnual": true/false
        }
        CATEGORÍAS: 
           - "Evento": Cumpleaños, citas, aniversarios.
           - "Tarea": Cosas por hacer. Usa "subtasks" si hay pasos.
           - "Alarma": Avisos sonoros. Requiere "time".
           - "Rutina": Actividades recurrentes con "daysOfWeek".
        Si la intencion es DELETE/UPDATE/READ y no te he proporcionado la lista de tareas bajo "CONTEXTO_ACTUAL", responde únicamente: NEED_CONTEXT.
    """.trimIndent()

    fun updateDraft(text: String) {
        _draftMessage.value = text
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        val userMessage = ChatMessage(text, true)
        _messages.value += userMessage
        _isLoading.value = true
        _draftMessage.value = ""

        viewModelScope.launch {
            try {
                val sdfContext = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy, HH:mm", Locale.getDefault())
                val currentTimeContext = sdfContext.format(Date())
                val basePrompt = "$systemPrompt\n\nFECHA Y HORA ACTUAL: $currentTimeContext"

                fun buildContents(msgs: List<ChatMessage>): List<Content> {
                    val contents = mutableListOf<Content>()
                    contents.add(Content(role = "user", parts = listOf(Part(text = basePrompt))))
                    contents.add(Content(role = "model", parts = listOf(Part(text = "Entendido. ¿En qué puedo ayudarte hoy?"))))
                    msgs.forEach { msg ->
                        contents.add(Content(role = if (msg.isUser) "user" else "model", parts = listOf(Part(text = msg.text))))
                    }
                    return contents
                }

                // Primera llamada a Gemini
                val firstRequest = GeminiRequest(contents = buildContents(_messages.value.drop(1)))
                val firstResponse = GeminiClient.instance.generateContent(apiKey, firstRequest)
                var responseText = firstResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Lo siento."

                // Manejo de NEED_CONTEXT
                if (responseText.trim() == "NEED_CONTEXT") {
                    val contextData = getContextData()
                    val augmentedMessages = _messages.value.drop(1) + ChatMessage("CONTEXTO_ACTUAL:\n$contextData", true)
                    val secondRequest = GeminiRequest(contents = buildContents(augmentedMessages))
                    val secondResponse = GeminiClient.instance.generateContent(apiKey, secondRequest)
                    responseText = secondResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No pude obtener el contexto necesario."
                }

                processGeminiResponse(responseText)
            } catch (e: Exception) {
                _messages.value += ChatMessage("Error: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getContextData(): String {
        val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
        val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return try {
            val all = repository.allRecordatorios.first()
            val filtered = all.filter { it.fecha in start..end || it.esAnual || it.diasSemana.isNotEmpty() }
            if (filtered.isEmpty()) "No hay tareas registradas en el periodo cercano."
            else filtered.joinToString("\n") {
                "ID: ${it.id} | Titulo: ${it.titulo} | Fecha: ${sdf.format(Date(it.fecha))} | Hora: ${it.hora ?: "N/A"} | Cat: ${it.categoria}"
            }
        } catch (e: Exception) { "Error al obtener contexto de la base de datos." }
    }

    private fun processGeminiResponse(text: String) {
        val jsonStart = text.indexOf("{")
        val jsonEnd = text.lastIndexOf("}")
        if (jsonStart != -1 && jsonEnd != -1) {
            val jsonString = text.substring(jsonStart, jsonEnd + 1)
            try {
                val action = Gson().fromJson(jsonString, GeminiProductivityAction::class.java)
                handleProductivityAction(action)
                
                val feedback = when(action.intent) {
                    "CREATE" -> "¡Hecho! He creado: ${action.title}."
                    "UPDATE" -> "He actualizado el registro correctamente."
                    "DELETE" -> "He eliminado el registro solicitado."
                    "READ" -> action.summary ?: "Aquí tienes la información que encontré."
                    else -> "Acción completada con éxito."
                }
                _messages.value += ChatMessage(feedback, false)
            } catch (e: Exception) { 
                _messages.value += ChatMessage(text, false) 
            }
        } else { 
            _messages.value += ChatMessage(text, false) 
        }
    }

    private fun handleProductivityAction(action: GeminiProductivityAction) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            when (action.intent) {
                "CREATE" -> {
                    val startDate = action.date?.let { try { sdf.parse(it)?.time } catch (e: Exception) { null } } ?: Calendar.getInstance().timeInMillis
                    val recordatorio = RecordatorioEntity(
                        titulo = action.title ?: "Sin título",
                        descripcion = action.summary ?: "",
                        fecha = startDate,
                        hora = action.time,
                        categoria = action.category ?: "Tarea",
                        esAnual = action.isAnnual ?: false,
                        subtasks = action.subtasks?.map { SubTask(it) } ?: emptyList(),
                        diasSemana = action.daysOfWeek ?: emptyList()
                    )
                    val id = repository.insert(recordatorio).toInt()
                    if (action.category == "Alarma" && action.time != null) programarAlarma(id, action.title ?: "Alarma", startDate, action.time)
                }
                "UPDATE" -> {
                    action.id?.let { id ->
                        val existingList = repository.allRecordatorios.first()
                        existingList.find { it.id == id }?.let { existing ->
                            val updatedDate = action.date?.let { try { sdf.parse(it)?.time } catch (e: Exception) { null } } ?: existing.fecha
                            val updated = existing.copy(
                                titulo = action.title ?: existing.titulo,
                                descripcion = action.summary ?: existing.descripcion,
                                fecha = updatedDate,
                                hora = action.time ?: existing.hora,
                                categoria = action.category ?: existing.categoria,
                                esAnual = action.isAnnual ?: existing.esAnual,
                                subtasks = action.subtasks?.map { SubTask(it) } ?: existing.subtasks,
                                diasSemana = action.daysOfWeek ?: existing.diasSemana
                            )
                            repository.update(updated)
                        }
                    }
                }
                "DELETE" -> {
                    action.id?.let { repository.deleteById(it) }
                }
            }
        }
    }

    private fun programarAlarma(id: Int, titulo: String, fecha: Long, hora: String) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_TITLE", titulo)
            putExtra("ALARM_ID", id)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = fecha
            val parts = hora.split(":")
            if (parts.size >= 2) {
                set(Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
                set(Calendar.MINUTE, parts[1].trim().take(2).toInt())
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) calendar.add(Calendar.DAY_OF_YEAR, 1)

        val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(info, pendingIntent)
    }
}
