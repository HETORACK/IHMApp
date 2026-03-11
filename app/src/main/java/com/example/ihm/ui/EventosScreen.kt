package com.example.ihm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EventosScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            CalendarView()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Proximamente",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))
            EventoCard("Evento", "Exposicion", "Fecha / Hora")
        }
    }
}

@Composable
fun CalendarView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "February", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = "2026", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Días de la semana
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Simulación de cuadrícula de calendario
        val days = (1..28).toList()
        Column {
            for (i in 0 until 4) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (j in 0 until 7) {
                        val day = days.getOrNull(i * 7 + j)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != null) {
                                Surface(
                                    color = if (day == 26) Color(0xFF80CBC4) else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 16.sp,
                                            fontWeight = if (day == 26) FontWeight.Bold else FontWeight.Normal,
                                            color = if (day == 26) Color.White else Color.Black
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
}

@Composable
fun EventoCard(titulo: String, subtitulo: String, fecha: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF2C3E50))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono placeholder
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFF2C3E50), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Icono blanco
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = subtitulo, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = fecha, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            
            IconButton(
                onClick = { },
                modifier = Modifier
                    .background(Color(0xFF80CBC4), CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventosScreenPreview() {
    EventosScreen()
}
