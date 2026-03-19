package com.example.ihm.ui

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ihm.data.AppDatabase
import com.example.ihm.data.AgendaRepository
import com.example.ihm.data.RecordatorioEntity
import com.example.ihm.data.SubTask
import com.example.ihm.receiver.AlarmReceiver
import com.example.ihm.service.AlarmService
import com.example.ihm.speech.TTSManager
import com.example.ihm.viewmodel.AgendaViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordatoriosScreen(fontSize: Int) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { AgendaRepository(database.recordatorioDao()) }
    val viewModel: AgendaViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return AgendaViewModel(repository) as T
        }
    })

    val recordatorios by viewModel.recordatorios.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailyEvents by viewModel.dailyEvents.collectAsState()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState()

    val ttsManager = remember { if (isPreview) null else TTSManager(context) }
    DisposableEffect(Unit) {
        onDispose { ttsManager?.shutdown() }
    }

    var showUpcomingDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        DateSelector(
            selectedDate = selectedDate,
            onPreviousDay = { viewModel.previousDay() },
            onNextDay = { viewModel.nextDay() },
            onDateSelected = { viewModel.selectDate(it) },
            fontSize = fontSize
        )

        EventsHeader(
            selectedDate = selectedDate,
            events = dailyEvents,
            upcomingCount = upcomingEvents.size,
            onShowUpcoming = { showUpcomingDialog = true },
            fontSize = fontSize,
            ttsManager = ttsManager
        )

        if (recordatorios.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No hay tareas para este día", color = Color.Gray, fontSize = fontSize.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(recordatorios) { item ->
                    RecordatorioCard(
                        item = item,
                        onToggleComplete = { viewModel.toggleCompletado(item) },
                        onToggleSubtask = { index -> 
                            val updatedSubtasks = item.subtasks.toMutableList()
                            val subtask = updatedSubtasks[index]
                            updatedSubtasks[index] = subtask.copy(completed = !subtask.completed)
                            viewModel.actualizarRecordatorio(item.copy(subtasks = updatedSubtasks))
                        },
                        fontSize = fontSize,
                        ttsManager = ttsManager
                    )
                }
            }
        }
    }

    if (showUpcomingDialog) {
        UpcomingEventsDialog(
            events = upcomingEvents,
            onDismiss = { showUpcomingDialog = false },
            fontSize = fontSize
        )
    }
}

@Composable
fun RecordatorioCard(
    item: RecordatorioEntity,
    onToggleComplete: () -> Unit,
    onToggleSubtask: (Int) -> Unit,
    fontSize: Int,
    ttsManager: TTSManager?
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val isAlarmDone = item.categoria == "Alarma" && item.completado
    
    var isSounding by remember { mutableStateOf(false) }
    LaunchedEffect(item) {
        if (item.categoria == "Alarma" && !item.completado && item.hora != null) {
            while (!isSounding) {
                val target = Calendar.getInstance().apply {
                    timeInMillis = item.fecha
                    val parts = item.hora!!.split(":")
                    if (parts.size >= 2) {
                        set(Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
                        set(Calendar.MINUTE, parts[1].trim().take(2).toInt())
                    }
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                isSounding = System.currentTimeMillis() >= target.timeInMillis
                if (isSounding) break
                delay(1000)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAlarmDone -> Color(0xFFF5F5F5)
                item.completado -> Color(0xFFE8F5E9)
                else -> Color.White
            }
        ),
        border = BorderStroke(1.dp, if (isAlarmDone) Color.LightGray else Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(if (isAlarmDone) Color.LightGray.copy(alpha = 0.3f) else Color(0xFFF0EFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = if (item.categoria == "Alarma") Icons.Default.Alarm else getIconForCategory(item.icono)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = if (isAlarmDone) Color.Gray else Color(0xFF5046BD)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.titulo,
                        fontSize = (fontSize + 4).sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.completado) TextDecoration.LineThrough else null,
                        color = if (item.completado) Color.Gray else Color.Black
                    )
                    
                    if (item.categoria == "Alarma" && item.hora != null) {
                        if (item.completado) {
                            Text(
                                text = "Alarma hecha",
                                fontSize = (fontSize - 2).coerceAtLeast(10).sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            CountdownTimer(fecha = item.fecha, hora = item.hora!!, fontSize = fontSize)
                        }
                    } else {
                        if (item.hora != null) {
                            Text(
                                text = item.hora!!,
                                fontSize = (fontSize - 2).coerceAtLeast(10).sp,
                                color = Color(0xFF5046BD),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (item.subtasks.isNotEmpty()) {
                            val completedCount = item.subtasks.count { it.completed }
                            Text(
                                text = "$completedCount/${item.subtasks.size} subtareas",
                                fontSize = (fontSize - 2).coerceAtLeast(10).sp,
                                color = Color(0xFF6ED1B8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { 
                            val h = if (item.hora != null) ". Hora: ${item.hora}" else ""
                            ttsManager?.speak("${item.titulo}$h")
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Escuchar", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))

                    if (item.categoria == "Alarma" && !item.completado) {
                        if (isSounding) {
                            IconButton(
                                onClick = {
                                    val stopIntent = Intent(context, AlarmService::class.java).apply {
                                        action = AlarmService.ACTION_STOP
                                        putExtra("ALARM_ID", item.id)
                                    }
                                    context.startService(stopIntent)
                                    onToggleComplete()
                                },
                                modifier = Modifier
                                    .background(Color(0xFFF44336), CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Detener Alarma", tint = Color.White)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    cancelarAlarmaProgramada(context, item)
                                    onToggleComplete()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive, 
                                    contentDescription = "Cancelar Alarma", 
                                    tint = Color(0xFF5046BD)
                                )
                            }
                        }
                    } else if (item.categoria != "Alarma") {
                        Checkbox(
                            checked = item.completado,
                            onCheckedChange = { onToggleComplete() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF4CAF50),
                                uncheckedColor = Color.Gray
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (item.descripcion.isNotEmpty()) {
                        Text(
                            text = item.descripcion,
                            fontSize = fontSize.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (item.diasSemana.isNotEmpty()) {
                        val names = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
                        val diasText = item.diasSemana.sorted().joinToString(", ") { names[it - 1] }
                        Text(
                            text = "Días: $diasText",
                            fontSize = (fontSize - 2).coerceAtLeast(10).sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (item.subtasks.isNotEmpty()) {
                        item.subtasks.forEachIndexed { index, subtask ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onToggleSubtask(index) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                border = BorderStroke(0.5.dp, Color.LightGray)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = subtask.completed,
                                        onCheckedChange = { onToggleSubtask(index) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF6ED1B8),
                                            uncheckedColor = Color.Gray
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = subtask.title,
                                        fontSize = fontSize.sp,
                                        textDecoration = if (subtask.completed) TextDecoration.LineThrough else null,
                                        color = if (subtask.completed) Color.Gray else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun cancelarAlarmaProgramada(context: Context, item: RecordatorioEntity) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, item.id, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    alarmManager.cancel(pendingIntent)
}

@Composable
fun DateSelector(
    selectedDate: Long,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateSelected: (Long) -> Unit,
    fontSize: Int
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "MX"))
    val dateText = sdf.format(Date(selectedDate))

    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCalendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(newCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Día anterior")
        }
        
        Text(
            text = dateText.replaceFirstChar { it.uppercase() },
            fontSize = (fontSize + 2).sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5046BD),
            modifier = Modifier.clickable { datePickerDialog.show() }
        )

        IconButton(onClick = onNextDay) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Día siguiente")
        }
    }
}

@Composable
fun EventsHeader(
    selectedDate: Long,
    events: List<RecordatorioEntity>,
    upcomingCount: Int,
    onShowUpcoming: () -> Unit,
    fontSize: Int,
    ttsManager: TTSManager?
) {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val tomorrow = today + 24 * 60 * 60 * 1000

    if (events.isEmpty() && upcomingCount == 0) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        events.forEach { event ->
            val isToday = isSameDay(selectedDate, today)
            val isTomorrow = isSameDay(selectedDate, tomorrow)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isToday) Color(0xFF5046BD) else Color.White
                ),
                border = if (isTomorrow) BorderStroke(2.dp, Color(0xFF5046BD)) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        tint = if (isToday) Color.White else Color(0xFF5046BD)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Evento: ${event.titulo}",
                            color = if (isToday) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize.sp
                        )
                        if (event.hora != null) {
                            Text(
                                text = "Hora: ${event.hora}",
                                color = if (isToday) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                fontSize = (fontSize - 2).coerceAtLeast(10).sp
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            val h = if (event.hora != null) ". Hora: ${event.hora}" else ""
                            ttsManager?.speak("${event.titulo}$h")
                        }
                    ) {
                        Icon(
                            Icons.Default.VolumeUp, 
                            contentDescription = "Escuchar", 
                            tint = if (isToday) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (upcomingCount > 0) {
            TextButton(
                onClick = onShowUpcoming,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Upcoming, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Ver eventos próximos ($upcomingCount)",
                    fontSize = (fontSize - 2).coerceAtLeast(10).sp,
                    color = Color(0xFF5046BD)
                )
            }
        }
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun UpcomingEventsDialog(
    events: List<RecordatorioEntity>,
    onDismiss: () -> Unit,
    fontSize: Int
) {
    val sdf = SimpleDateFormat("EEEE d", Locale("es", "MX"))
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eventos próximos (7 días)", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0EFFF), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = sdf.format(Date(event.fecha)).replaceFirstChar { it.uppercase() },
                            fontSize = (fontSize - 4).coerceAtLeast(10).sp,
                            color = Color(0xFF5046BD),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = event.titulo,
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun CountdownTimer(fecha: Long, hora: String, fontSize: Int) {
    var timeLeft by remember { mutableStateOf("") }
    
    LaunchedEffect(key1 = fecha, key2 = hora) {
        while (true) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                timeInMillis = fecha
                val parts = hora.split(":")
                if (parts.size >= 2) {
                    set(Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
                    set(Calendar.MINUTE, parts[1].trim().take(2).toInt())
                }
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val diff = target.timeInMillis - now.timeInMillis
            if (diff <= 0) {
                timeLeft = "¡Sonando!"
                break
            } else {
                val hours = diff / (1000 * 60 * 60)
                val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (diff % (1000 * 60)) / 1000
                timeLeft = String.format("Faltan: %02d:%02d:%02d", hours, minutes, seconds)
            }
            delay(1000)
        }
    }
    
    Text(
        text = timeLeft,
        fontSize = (fontSize - 2).coerceAtLeast(10).sp,
        color = if (timeLeft == "¡Sonando!") Color(0xFF5046BD) else Color(0xFFF44336),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

fun getIconForCategory(icono: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (icono) {
        "Medicina" -> Icons.Default.MedicalServices
        "CitaMedica" -> Icons.Default.LocalHospital
        "Ejercicio" -> Icons.Default.FitnessCenter
        "Rutina" -> Icons.Default.Repeat
        "Social" -> Icons.Default.People
        "Profesional" -> Icons.Default.Work
        "Compras" -> Icons.Default.ShoppingCart
        "Alimentos" -> Icons.Default.Restaurant
        "Bebida" -> Icons.Default.LocalDrink
        "Cocina" -> Icons.Default.RestaurantMenu
        "Llamada" -> Icons.Default.Call
        "Despertador" -> Icons.Default.Alarm
        "Temporizador" -> Icons.Default.Timer
        "Cumpleaños" -> Icons.Default.Cake
        "Fiesta" -> Icons.Default.Celebration
        "EventoTrabajo" -> Icons.Default.BusinessCenter
        else -> Icons.Default.Assignment
    }
}
