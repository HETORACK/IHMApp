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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ihm.VoiceToTextParser
import com.example.ihm.VoiceToTextParserState
import com.example.ihm.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    useCustomKeyboard: Boolean = false,
    fontSize: Int
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.draftMessage.collectAsState()
    val listState = rememberLazyListState()
    var showCustomKeyboard by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Usamos Box para permitir que los controles floten sobre el chat
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            // Aumentamos el padding inferior para que el contenido no quede oculto bajo el input bar
            contentPadding = PaddingValues(top = 16.dp, bottom = 180.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message, fontSize)
            }
        }

        // El InputBar ahora flota en la parte inferior
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ChatInputBar(
                text = inputText,
                onTextChange = { viewModel.updateDraft(it) },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        showCustomKeyboard = false
                    }
                },
                onClear = { viewModel.clearMessages() },
                useCustomKeyboard = useCustomKeyboard,
                showCustomKeyboard = showCustomKeyboard,
                onToggleCustomKeyboard = { showCustomKeyboard = it },
                fontSize = fontSize
            )
            
            if (useCustomKeyboard && showCustomKeyboard) {
                CustomKeyboard(
                    onKeyClick = { viewModel.updateDraft(inputText + it) },
                    onDeleteClick = { if (inputText.isNotEmpty()) viewModel.updateDraft(inputText.dropLast(1)) },
                    onSpaceClick = { viewModel.updateDraft(inputText + " ") },
                    onEnterClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            showCustomKeyboard = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, fontSize: Int) {
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
                fontSize = fontSize.sp
            )
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit,
    useCustomKeyboard: Boolean,
    showCustomKeyboard: Boolean,
    onToggleCustomKeyboard: (Boolean) -> Unit,
    fontSize: Int
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val application = context.applicationContext as? Application
    
    val voiceParser = remember {
        if (!isPreview && application != null) {
            VoiceToTextParser(application)
        } else null
    }

    val voiceState by if (voiceParser != null) {
        voiceParser.state.collectAsState()
    } else {
        remember { mutableStateOf(VoiceToTextParserState()) }
    }

    var baseTextByVoice by remember { mutableStateOf("") }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                voiceParser?.startListening()
            } else {
                Toast.makeText(context, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(voiceState.spokenText) {
        if (voiceState.isSpeaking && voiceState.spokenText.isNotEmpty()) {
            val prefix = if (baseTextByVoice.isBlank()) "" else "$baseTextByVoice "
            onTextChange(prefix + voiceState.spokenText)
        }
    }

    // El contenedor principal ahora es transparente
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de Limpiar flotante e independiente
            FloatingActionButton(
                onClick = onClear,
                containerColor = Color.White.copy(alpha = 0.9f),
                contentColor = Color(0xFFE53935),
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Limpiar historial",
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(visible = voiceState.isSpeaking) {
                ChatSoundWaveVisualizer(amplitude = voiceState.amplitude)
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            if (useCustomKeyboard) {
                FloatingActionButton(
                    onClick = { onToggleCustomKeyboard(!showCustomKeyboard) },
                    containerColor = if (showCustomKeyboard) Color(0xFF6A5EEC) else Color.White.copy(alpha = 0.9f),
                    contentColor = if (showCustomKeyboard) Color.White else Color.Black,
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = "Teclado", modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            FloatingActionButton(
                onClick = { 
                    if (voiceState.isSpeaking) {
                        voiceParser?.stopListening()
                    } else {
                        baseTextByVoice = text.trim()
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                containerColor = if (voiceState.isSpeaking) Color(0xFF4CAF50) else Color(0xFF6ED1B8),
                contentColor = Color.White,
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Dictar",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Surface para el TextField, ahora con márgenes para que parezca flotar
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = Color.White.copy(alpha = 0.95f),
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                @OptIn(ExperimentalMaterial3Api::class)
                TextField(
                    value = text,
                    onValueChange = { 
                        onTextChange(it)
                        if (!voiceState.isSpeaking) baseTextByVoice = it.trim()
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe a Nero...", fontSize = (fontSize - 2).sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    readOnly = useCustomKeyboard && showCustomKeyboard,
                    textStyle = LocalTextStyle.current.copy(fontSize = fontSize.sp)
                )
                IconButton(
                    onClick = onSend,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFF5046BD),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", modifier = Modifier.size(20.dp))
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
            .background(Color(0xFF6750A4).copy(alpha = 0.8f), RoundedCornerShape(32.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { index ->
            val normalizedHeight = (8 + (amplitude.coerceIn(0f, 15f) * 1.5f)).dp
            val animatedHeight by animateDpAsState(
                targetValue = normalizedHeight,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight)
                    .background(Color.White, CircleShape)
            )
        }
    }
}
