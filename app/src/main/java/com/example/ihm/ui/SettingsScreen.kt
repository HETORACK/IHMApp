package com.example.ihm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    voiceResponse: Boolean,
    onVoiceResponseChange: (Boolean) -> Unit,
    customKeyboard: Boolean,
    onCustomKeyboardChange: (Boolean) -> Unit,
    hideMicButton: Boolean,
    onHideMicButtonChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Carrusel de tamaño de letra VERTICAL
        Text(text = "Tamaño de letra mínimo", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))

        VerticalFontSizePicker(
            selectedSize = fontSize,
            onSizeSelected = onFontSizeChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Modo Oscuro
        SettingSwitch(
            label = "Modo Oscuro",
            checked = isDarkMode,
            onCheckedChange = onDarkModeChange
        )

        // Voz y Texto
        SettingSwitch(
            label = "Respuesta con Voz y Texto",
            checked = voiceResponse,
            onCheckedChange = onVoiceResponseChange,
            description = if (voiceResponse) "Voz + Texto" else "Solo Texto"
        )

        // Teclado Propio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Teclado", fontWeight = FontWeight.Medium)
            Button(
                onClick = { onCustomKeyboardChange(!customKeyboard) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A5EEC)
                )
            ) {
                Text(text = if (customKeyboard) "Propio de la App" else "Normal del Celular")
            }
        }

        // Ocultar Micrófono
        SettingSwitch(
            label = "Ocultar botón de micrófono",
            checked = hideMicButton,
            onCheckedChange = onHideMicButtonChange
        )
    }
}

@Composable
fun VerticalFontSizePicker(
    selectedSize: Int,
    onSizeSelected: (Int) -> Unit
) {
    val fontSizes = listOf(12, 14, 16, 18, 20, 22, 24)
    val listState = rememberLazyListState()

    // Sincronizar el scroll inicial con el tamaño seleccionado
    LaunchedEffect(selectedSize) {
        val index = fontSizes.indexOf(selectedSize)
        if (index != -1) {
            listState.animateScrollToItem(index)
        }
    }

    // Detectar qué item está en el centro para actualizar la selección al deslizar
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            if (centerIndex < fontSizes.size) {
                onSizeSelected(fontSizes[centerIndex])
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(100.dp)
        ) {
            IconButton(onClick = {
                val currentIndex = fontSizes.indexOf(selectedSize)
                if (currentIndex > 0) {
                    onSizeSelected(fontSizes[currentIndex - 1])
                }
            }) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
            }

            Box(
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Indicador visual de selección (el centro)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(0xFF6ED1B8).copy(alpha = 0.2f), CircleShape)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                    contentPadding = PaddingValues(vertical = 30.dp) // Para que los items puedan centrarse
                ) {
                    itemsIndexed(fontSizes) { index, size ->
                        val isSelected = size == selectedSize
                        Text(
                            text = size.toString(),
                            fontSize = if (isSelected) 24.sp else 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .clickable { onSizeSelected(size) }
                        )
                    }
                }
            }

            IconButton(onClick = {
                val currentIndex = fontSizes.indexOf(selectedSize)
                if (currentIndex < fontSizes.size - 1) {
                    onSizeSelected(fontSizes[currentIndex + 1])
                }
            }) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar")
            }
        }
    }
}

@Composable
fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = label, fontWeight = FontWeight.Medium)
            if (description != null) {
                Text(text = description, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF6ED1B8)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        fontSize = 16,
        onFontSizeChange = {},
        isDarkMode = false,
        onDarkModeChange = {},
        voiceResponse = true,
        onVoiceResponseChange = {},
        customKeyboard = false,
        onCustomKeyboardChange = {},
        hideMicButton = false,
        onHideMicButtonChange = {}
    )
}
