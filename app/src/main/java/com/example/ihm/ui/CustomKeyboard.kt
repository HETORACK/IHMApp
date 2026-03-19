package com.example.ihm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomKeyboard(
    onKeyClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onEnterClick: () -> Unit,
    onSpaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val alphabet = ('A'..'Z').map { it.toString() }
    val symbols = listOf(".", ",", ":", "¿", "?")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFE0E0E0))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Fila de números
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            numbers.forEach { num ->
                KeyButton(text = num, onClick = { onKeyClick(num) }, modifier = Modifier.weight(1f))
            }
        }

        // Grid de letras
        Box(modifier = Modifier.height(230.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 2.dp),
                userScrollEnabled = false
            ) {
                items(alphabet) { letter ->
                    KeyButton(text = letter, onClick = { onKeyClick(letter) })
                }
                item {
                    KeyButton(icon = Icons.AutoMirrored.Filled.Backspace, onClick = onDeleteClick, color = Color(0xFFFFCDD2))
                }
            }
        }

        // Fila de símbolos y acciones
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            symbols.forEach { symbol ->
                KeyButton(text = symbol, onClick = { onKeyClick(symbol) }, modifier = Modifier.weight(1f))
            }
            
            KeyButton(
                icon = Icons.Default.SpaceBar,
                onClick = onSpaceClick,
                modifier = Modifier.weight(2f),
                color = Color.White
            )
            
            KeyButton(
                icon = Icons.Default.KeyboardReturn,
                onClick = onEnterClick,
                modifier = Modifier.weight(1.5f),
                color = Color(0xFF6ED1B8)
            )
        }
    }
}

@Composable
fun KeyButton(
    text: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Surface(
        modifier = modifier
            .height(42.dp) // Reducido un poco más como se solicitó
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = color,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (text != null) {
                Text(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.Black
                )
            }
        }
    }
}
