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

    // Auto-scroll al final
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message, fontSize)
            }
        }

        ChatInputBar(
            text = inputText,
            onTextChange = { viewModel.updateDraft(it) },
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText)
                    showCustomKeyboard = false
                }
            },
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
                ChatSoundWaveVisualizer(amplitude = voiceState.amplitude)
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            if (useCustomKeyboard) {
                FloatingActionButton(
                    onClick = { onToggleCustomKeyboard(!showCustomKeyboard) },
                    containerColor = if (showCustomKeyboard) Color(0xFF6A5EEC) else Color(0xFFF0F0F0),
                    contentColor = if (showCustomKeyboard) Color.White else Color.Black,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = "Teclado Propio")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            FloatingActionButton(
                onClick = { 
                    if (voiceState.isSpeaking) {
                        voiceParser?.stopListening()
                    } else {
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
                @OptIn(ExperimentalMaterial3Api::class)
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe a Nero...", fontSize = fontSize.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    readOnly = useCustomKeyboard && showCustomKeyboard,
                    textStyle = LocalTextStyle.current.copy(fontSize = fontSize.sp)
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
