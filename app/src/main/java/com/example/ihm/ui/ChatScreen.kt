package com.example.ihm.ui

import android.Manifest
import android.app.Application
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ihm.VoiceToTextParser

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatScreen() {
    var messages by remember { mutableStateOf(listOf(
        ChatMessage("¡Hola! Soy Nero, tu asistente IA. ¿En qué puedo ayudarte hoy?", false)
    )) }
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }

        ChatInputBar(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    val userQuery = inputText
                    messages = messages + ChatMessage(userQuery, true)
                    inputText = ""
                    messages = messages + ChatMessage("Entiendo que quieres decir: '$userQuery'. Estoy procesando tu solicitud...", false)
                }
            }
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isUser) Color(0xFF5046BD) else Color(0xFFE0E0E0)
    val textColor = if (message.isUser) Color.White else Color.Black
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = backgroundColor,
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val context = LocalContext.current
    val voiceParser = remember { VoiceToTextParser(context.applicationContext as Application) }
    val voiceState by voiceParser.state.collectAsState()
    
    // Gestor de permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                voiceParser.startListening()
            } else {
                Toast.makeText(context, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(voiceState.spokenText) {
        if (voiceState.spokenText.isNotEmpty()) {
            onTextChange(voiceState.spokenText)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedVisibility(visible = voiceState.isSpeaking) {
                // Visualizador mejorado para que no se deforme
                ChatSoundWaveVisualizer(amplitude = voiceState.amplitude)
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            FloatingActionButton(
                onClick = { 
                    if (voiceState.isSpeaking) {
                        voiceParser.stopListening()
                    } else {
                        // Solicitar permiso antes de escuchar
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                containerColor = if (voiceState.isSpeaking) Color(0xFF4CAF50) else Color(0xFF6ED1B8),
                contentColor = Color.White,
                modifier = Modifier.size(64.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Dictar mensaje",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe a Nero...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSend,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFF5046BD),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

@Composable
fun ChatSoundWaveVisualizer(amplitude: Float) {
    Row(
        modifier = Modifier
            .padding(end = 8.dp)
            .background(Color(0xFF6750A4), RoundedCornerShape(32.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { index ->
            // Normalizamos la amplitud para que no crezca infinitamente (limite de 40.dp)
            val normalizedHeight = (10 + (amplitude.coerceIn(0f, 15f) * 2)).dp
            
            val animatedHeight by animateDpAsState(
                targetValue = normalizedHeight,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animatedHeight)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun ChatScreenPreview() {
    ChatScreen()
}
