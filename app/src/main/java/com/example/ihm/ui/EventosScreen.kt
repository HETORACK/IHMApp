package com.example.ihm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ihm.data.AppDatabase
import com.example.ihm.data.AgendaRepository
import com.example.ihm.data.RecordatorioEntity
import com.example.ihm.speech.TTSManager
import com.example.ihm.viewmodel.AgendaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventosScreen(fontSize: Int) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { AgendaRepository(database.recordatorioDao()) }
    val viewModel: AgendaViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AgendaViewModel(repository) as T
        }
    })

    val selectedDate by viewModel.selectedDate.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val dailyEvents by viewModel.dailyEvents.collectAsState()
    
    val ttsManager = remember { if (isPreview) null else TTSManager(context) }
    DisposableEffect(Unit) {
        onDispose { ttsManager?.shutdown() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            CalendarView(
                selectedDate = selectedDate,
                events = allEvents,
                onDateSelected = { viewModel.selectDate(it) },
                fontSize = fontSize
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Eventos para este día",
                fontSize = (fontSize + 4).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (dailyEvents.isEmpty()) {
            item {
                Text("No hay eventos programados", color = Color.Gray, modifier = Modifier.padding(16.dp), fontSize = fontSize.sp)
            }
        } else {
            items(dailyEvents) { event ->
                EventoCard(event, fontSize, ttsManager)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun CalendarView(
    selectedDate: Long,
    events: List<RecordatorioEntity>,
    onDateSelected: (Long) -> Unit,
    fontSize: Int
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedDate }) }
    
    val monthSdf = SimpleDateFormat("MMMM", Locale("es", "MX"))
    val yearSdf = SimpleDateFormat("yyyy", Locale("es", "MX"))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null)
            }
            
            Text(
                text = "${monthSdf.format(currentMonth.time).replaceFirstChar { it.uppercase() }} ${yearSdf.format(currentMonth.time)}",
                fontSize = (fontSize + 2).sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {
                currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { day ->
                Text(
                    text = day,
                    fontSize = (fontSize - 4).coerceAtLeast(10).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val calendar = (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Ajuste para que Lunes sea 0
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDate }

        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 0 until 7) {
                    val dayOfMonth = week * 7 + dayOfWeek - firstDayOfWeek + 1
                    if (dayOfMonth in 1..daysInMonth) {
                        val dayCal = (calendar.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        
                        val isSelected = selectedCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                        selectedCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
                        
                        val hasEvent = events.any { e ->
                            val eCal = Calendar.getInstance().apply { timeInMillis = e.fecha }
                            if (e.esAnual) {
                                eCal.get(Calendar.DAY_OF_MONTH) == dayCal.get(Calendar.DAY_OF_MONTH) &&
                                eCal.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH)
                            } else {
                                eCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                eCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable { onDateSelected(dayCal.timeInMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = when {
                                    isSelected -> Color(0xFF5046BD)
                                    hasEvent -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    else -> Color.Transparent
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp),
                                border = if (hasEvent && !isSelected) BorderStroke(1.dp, Color(0xFF4CAF50)) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = dayOfMonth.toString(),
                                        fontSize = (fontSize - 2).coerceAtLeast(12).sp,
                                        fontWeight = if (isSelected || hasEvent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else if (hasEvent) Color(0xFF2E7D32) else Color.Black
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (week * 7 + 7 - firstDayOfWeek >= daysInMonth) break
        }
    }
}

@Composable
fun EventoCard(item: RecordatorioEntity, fontSize: Int, ttsManager: TTSManager?) {
    val timeText = if (item.hora == null && item.horaLimite == null) {
        "Todo el día"
    } else {
        "${item.hora ?: "--:--"} ${item.horaLimite?.let { "- $it" } ?: ""}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFF0EFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForCategory(item.icono),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF5046BD)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.titulo, fontWeight = FontWeight.Bold, fontSize = fontSize.sp)
                if (item.descripcion.isNotEmpty()) {
                    Text(text = item.descripcion, fontSize = (fontSize - 2).coerceAtLeast(12).sp, color = Color.Gray)
                }
                
                Text(
                    text = timeText,
                    fontSize = (fontSize - 4).coerceAtLeast(10).sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5046BD)
                )
                
                if (item.esAnual) {
                    Text(
                        text = "Evento anual",
                        fontSize = (fontSize - 5).coerceAtLeast(9).sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            IconButton(
                onClick = { 
                    val textToSpeak = "${item.titulo}. Hora: $timeText"
                    ttsManager?.speak(textToSpeak)
                },
                modifier = Modifier
                    .background(Color(0xFF6ED1B8), CircleShape)
                    .size(32.dp)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Escuchar evento", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
