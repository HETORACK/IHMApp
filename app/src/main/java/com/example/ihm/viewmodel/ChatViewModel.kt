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
import com.example.ihm.receiver.TaskReceiver
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
    val id: Int? = null,
    val ids: List<Int>? = null
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

    private val apiKey = ""

    private val systemPrompt = """
        Eres Nero, un asistente. 
        SI LA INTENCIÓN ES CREATE:
        Genera el JSON con el campo "intent": "CREATE" y llena los campos necesarios. El "id" debe ser null. Si contiene varias tareas o una lista de objetos agregalos como subtareas.
        SI LA INTENCIÓN ES DELETE, UPDATE o READ:
            1. Si no hay una lista bajo "CONTEXTO_ACTUAL", responde solo: NEED_CONTEXT.
            2. Si hay contexto:
               - DELETE: Devuelve {"intent": "DELETE", "ids": [id1, id2, ...]} siempre usa una lista en "ids" incluso para uno solo. 
               - UPDATE: Devuelve el JSON completo with "intent": "UPDATE", el "id" correspondiente y SOLO los campos que el usuario quiere cambiar actualizados (mantén los demás como los recibiste). Si el usuario menciona pasos o subtareas, actualiza el campo "subtasks".
               - READ: Devuelve {"intent": "READ", "summary": "Aquí tienes la información sobre..."}.
        FORMATO JSON:
        {
             "intent": "CREATE/UPDATE/DELETE/READ",
             "id": id_numerico_si_aplica,
             "ids": [lista_de_ids_para_delete],
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

    fun clearMessages() {
        _messages.value = listOf(
            ChatMessage("¡Hola! Soy Nero, tu asistente IA. ¿En qué puedo ayudarte hoy?", false)
        )
        _draftMessage.value = ""
    }

    private fun buildContents(msgs: List<ChatMessage>): List<Content> {
        val contents = mutableListOf<Content>()
        val sdfContext = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy, HH:mm", Locale.getDefault())
        val basePromptWithTime = "$systemPrompt\n\nFECHA Y HORA ACTUAL: ${sdfContext.format(Date())}"
        
        contents.add(Content(role = "user", parts = listOf(Part(text = basePromptWithTime))))
        contents.add(Content(role = "model", parts = listOf(Part(text = "Entendido. Soy Nero. ¿En qué puedo ayudarte?"))))
        
        msgs.takeLast(10).forEach { msg ->
            val role = if (msg.isUser) "user" else "model"
            if (contents.lastOrNull()?.role != role) {
                contents.add(Content(role = role, parts = listOf(Part(text = msg.text))))
            } else {
                val lastContent = contents.last()
                val updatedParts = lastContent.parts + Part(text = "\n" + msg.text)
                contents[contents.size - 1] = lastContent.copy(parts = updatedParts)
            }
        }
        return contents
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        val userMessage = ChatMessage(text, true)
        _messages.value += userMessage
        _isLoading.value = true
        _draftMessage.value = ""

        viewModelScope.launch {
            try {
                val firstRequest = GeminiRequest(contents = buildContents(_messages.value))
                val firstResponse = GeminiClient.instance.generateContent(apiKey, firstRequest)
                val responseText = firstResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

                if (responseText.contains("NEED_CONTEXT", ignoreCase = true)) {
                    val contextData = getContextData()
                    val contextMessage = ChatMessage("CONTEXTO_ACTUAL DE MI AGENDA:\n$contextData\n\nPor favor, ahora procesa mi solicitud anterior con esta información.", true)
                    val temporaryHistory = _messages.value + ChatMessage("NEED_CONTEXT", false) + contextMessage
                    val secondRequest = GeminiRequest(contents = buildContents(temporaryHistory))
                    val secondResponse = GeminiClient.instance.generateContent(apiKey, secondRequest)
                    val finalResponse = secondResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No pude procesar la solicitud con el contexto."
                    processGeminiResponse(finalResponse)
                } else {
                    processGeminiResponse(responseText)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error en sendMessage", e)
                _messages.value += ChatMessage("Error: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getContextData(): String {
        return try {
            val all = repository.allRecordatorios.first()
            if (all.isEmpty()) "La agenda está actualmente vacía. No hay tareas para actualizar o borrar."
            else all.joinToString("\n") {
                "ID: ${it.id} | Título: ${it.titulo} | Fecha: ${it.fecha} | Hora: ${it.hora} | Categoría: ${it.categoria} | Subtareas: ${it.subtasks.joinToString { st -> st.title }}"
            }
        } catch (e: Exception) { "Error al acceder a la base de datos." }
    }

    private suspend fun processGeminiResponse(text: String) {
        val jsonRegex = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL)
        val match = jsonRegex.find(text)

        if (match != null) {
            try {
                val action = Gson().fromJson(match.value, GeminiProductivityAction::class.java)
                handleProductivityAction(action)
                
                val feedback = when(action.intent) {
                    "CREATE" -> "¡Listo! He guardado '${action.title}'."
                    "UPDATE" -> "He actualizado '${action.title ?: "el registro"}' con éxito."
                    "DELETE" -> "He eliminado lo que me pediste."
                    "READ" -> action.summary ?: "Aquí tienes la información."
                    else -> "Hecho."
                }
                _messages.value += ChatMessage(feedback, false)
            } catch (e: Exception) {
                _messages.value += ChatMessage(text, false)
            }
        } else {
            _messages.value += ChatMessage(text, false)
        }
    }

    private suspend fun handleProductivityAction(action: GeminiProductivityAction) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        when (action.intent) {
            "CREATE" -> {
                val date = action.date?.let { try { sdf.parse(it)?.time } catch (e: Exception) { null } } ?: System.currentTimeMillis()
                val recordatorio = RecordatorioEntity(
                    titulo = action.title ?: "Sin título",
                    descripcion = action.summary ?: "",
                    fecha = date,
                    hora = action.time,
                    categoria = action.category ?: "Tarea",
                    subtasks = action.subtasks?.map { SubTask(it) } ?: emptyList()
                )
                val id = repository.insert(recordatorio).toInt()
                
                when (action.category) {
                    "Alarma" -> if (action.time != null) programarAlarma(id, action.title ?: "Alarma", date, action.time)
                    "Tarea" -> programarNotificacionTarea(id, action.title ?: "Tarea", date, action.time)
                    "Evento" -> programarNotificacionEvento(id, action.title ?: "Evento", date)
                }
            }
            "UPDATE" -> {
                action.id?.let { id ->
                    val existing = repository.allRecordatorios.first().find { it.id == id }
                    existing?.let {
                        val updated = it.copy(
                            titulo = action.title ?: it.titulo,
                            descripcion = action.summary ?: it.descripcion,
                            hora = action.time ?: it.hora,
                            subtasks = if (action.subtasks != null) action.subtasks.map { s -> SubTask(s) } else it.subtasks
                        )
                        repository.update(updated)
                        
                        when (updated.categoria) {
                            "Alarma" -> if (updated.hora != null) programarAlarma(id, updated.titulo, updated.fecha, updated.hora)
                            "Tarea" -> programarNotificacionTarea(id, updated.titulo, updated.fecha, updated.hora)
                            "Evento" -> programarNotificacionEvento(id, updated.titulo, updated.fecha)
                        }
                    }
                }
            }
            "DELETE" -> {
                action.id?.let { repository.deleteById(it) }
                action.ids?.forEach { repository.deleteById(it) }
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

        val calendar = getCalendarForTime(fecha, hora)

        if (calendar.timeInMillis <= System.currentTimeMillis()) calendar.add(Calendar.DAY_OF_YEAR, 1)

        val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(info, pendingIntent)
    }

    private fun programarNotificacionTarea(id: Int, titulo: String, fecha: Long, hora: String?) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, TaskReceiver::class.java).apply {
            putExtra(TaskReceiver.EXTRA_TASK_ID, id)
            putExtra("TITLE", titulo)
            putExtra(TaskReceiver.EXTRA_TYPE, "TASK")
        }

        // Notificación a la hora exacta
        val calendar = if (hora != null) getCalendarForTime(fecha, hora) else Calendar.getInstance().apply { timeInMillis = fecha }
        
        val pendingIntentExact = PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        if (calendar.timeInMillis > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntentExact)
        }

        // Notificación 15 minutos antes
        val calendarBefore = (calendar.clone() as Calendar).apply { add(Calendar.MINUTE, -15) }
        val pendingIntentBefore = PendingIntent.getBroadcast(
            context, id + 20000, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        if (calendarBefore.timeInMillis > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendarBefore.timeInMillis, pendingIntentBefore)
        }
    }

    private fun programarNotificacionEvento(id: Int, titulo: String, fecha: Long) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, TaskReceiver::class.java).apply {
            putExtra(TaskReceiver.EXTRA_TASK_ID, id)
            putExtra("TITLE", titulo)
            putExtra(TaskReceiver.EXTRA_TYPE, "EVENT")
        }

        // Notificar 3 días antes cada mañana (ej: 8:00 AM)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = fecha
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            add(Calendar.DAY_OF_YEAR, -3)
        }

        for (i in 0..3) {
            val notifyCalendar = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            if (notifyCalendar.timeInMillis > System.currentTimeMillis()) {
                val pendingIntent = PendingIntent.getBroadcast(
                    context, id + 30000 + i, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyCalendar.timeInMillis, pendingIntent)
            }
        }
    }

    private fun getCalendarForTime(fecha: Long, hora: String): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = fecha
            val parts = hora.split(":")
            if (parts.size >= 2) {
                set(Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
                set(Calendar.MINUTE, parts[1].trim().take(2).toInt())
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
