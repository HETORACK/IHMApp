package com.example.ihm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RecordatorioItem(
    val titulo: String,
    val descripcion: String,
    val hora: String? = null,
    val isAlarma: Boolean = false
)

@Composable
fun RecordatoriosScreen() {
    val items = listOf(
        RecordatorioItem("Actividad1", "DESC"),
        RecordatorioItem("Actividad2", "DESC", "HORA"),
        RecordatorioItem("Alarma", "HORA", isAlarma = true)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            RecordatorioCard(item)
        }
    }
}

@Composable
fun RecordatorioCard(item: RecordatorioItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, Color(0xFF2C3E50))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de la actividad
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Color(0xFF2C3E50)),
                color = Color.Transparent
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = Color(0xFF2C3E50)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = item.descripcion,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                if (item.isAlarma) {
                    Text(
                        text = "HORA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                
                IconButton(
                    onClick = { /* Reproducir audio */ },
                    modifier = Modifier.size(32.dp).offset(y = 4.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Escuchar",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                if (item.isAlarma) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Alarma",
                        modifier = Modifier.size(40.dp),
                        tint = Color.Black
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(2.dp, Color(0xFF2C3E50), RoundedCornerShape(8.dp))
                    )
                    if (item.hora != null) {
                        Text(
                            text = item.hora,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C3E50)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecordatoriosScreenPreview() {
    RecordatoriosScreen()
}
