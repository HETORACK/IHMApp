package com.example.ihm.ui

import android.Manifest
import android.app.Application
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.ihm.VoiceToTextParser
import com.example.ihm.VoiceToTextParserState
import com.example.ihm.data.AgendaRepository
import com.example.ihm.data.AppDatabase
import com.example.ihm.speech.TTSManager
import com.example.ihm.ui.theme.IHMTheme
import com.example.ihm.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { AgendaRepository(database.recordatorioDao()) }
    val application = context.applicationContext as Application
    
    val chatViewModel: ChatViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(application, repository) as T
        }
    })

    val draftMessage by chatViewModel.draftMessage.collectAsState()
    val messages by chatViewModel.messages.collectAsState()

    MainScreenContent(
        messages = messages,
        draftMessage = draftMessage,
        onDraftMessageChange = { chatViewModel.updateDraft(it) },
        onSendMessage = { chatViewModel.sendMessage(it) },
        viewModel = chatViewModel
    )
}

@Composable
fun MainScreenContent(
    messages: List<ChatMessage>,
    draftMessage: String,
    onDraftMessageChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    viewModel: ChatViewModel? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val application = context.applicationContext as? Application

    var selectedTab by remember { mutableStateOf("Tareas") }
    var showKeyboardInput by remember { mutableStateOf(false) }

    // Estados de configuración
    var fontSize by remember { mutableIntStateOf(16) }
    var isDarkMode by remember { mutableStateOf(false) }
    var voiceResponse by remember { mutableStateOf(true) }
    var customKeyboard by remember { mutableStateOf(false) }
    var hideMicButton by remember { mutableStateOf(false) }

    // Lógica para el estado de animación de entrada de Nero
    var neroState by remember { mutableStateOf("nero_up") }
    var isNeroVisible by remember { mutableStateOf(false) }
    var timerStarted by remember { mutableStateOf(false) }

    // Secuencias automáticas de Nero
    LaunchedEffect(isNeroVisible, neroState) {
        if (isNeroVisible) {
            when (neroState) {
                "nero_up" -> {
                    delay(740)
                    neroState = "nero_idle"
                    timerStarted = false
                }
                "nero_down" -> {
                    delay(500)
                    neroState = "nero_hearingup"
                }
                "nero_hearingup" -> {
                    delay(800)
                    neroState = "nerohearing"
                }
                "nero_hearingdown" -> {
                    delay(800)
                    neroState = "nero_up"
                }
            }
        }
    }

    // Gestores de voz y TTS - Se manejan con precaución para el Preview
    val voiceParser = remember { 
        if (isPreview || application == null) null else VoiceToTextParser(application) 
    }
    val voiceState by if (voiceParser != null) {
        voiceParser.state.collectAsState()
    } else {
        remember { MutableStateFlow(VoiceToTextParserState()) }.collectAsState()
    }
    
    val ttsManager = remember { 
        if (isPreview) null else TTSManager(context) 
    }

    LaunchedEffect(messages.size) {
        val lastMessage = messages.lastOrNull()
        if (lastMessage != null && !lastMessage.isUser && voiceResponse) {
            ttsManager?.speak(lastMessage.text)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                neroState = "nero_down"
                voiceParser?.startListening()
            }
            else Toast.makeText(context, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    )

    LaunchedEffect(voiceState) {
        if (voiceState.spokenText.isNotEmpty()) {
            onDraftMessageChange(voiceState.spokenText)
        }
        
        if (voiceState.isFinished && voiceState.spokenText.isNotBlank()) {
            onSendMessage(voiceState.spokenText)
            voiceParser?.clear()
            neroState = "nero_hearingdown"
        }
    }

    DisposableEffect(Unit) {
        onDispose { ttsManager?.shutdown() }
    }

    IHMTheme(darkTheme = isDarkMode) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (selectedTab) {
                            "Tareas" -> "Recordatorios"
                            "Eventos" -> "Eventos"
                            "Ajustes" -> "Configuración"
                            "Habla" -> "Nero"
                            else -> ""
                        },
                        fontSize = (fontSize + 12).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab != "Ajustes" && selectedTab != "Habla") {
                    NeroAssistant(
                        neroState = neroState,
                        modifier = Modifier
                            .size(270.dp)
                            .offset(x = (-80).dp, y = (170).dp)
                            .padding(bottom = 8.dp),
                        onReady = { state ->
                            if (state == "nero_up") isNeroVisible = true
                        }
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Start,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    if (selectedTab != "Ajustes" && selectedTab != "Habla") {
                        val inputActive = (showKeyboardInput && customKeyboard) || voiceState.isSpeaking || draftMessage.isNotEmpty()
                        
                        VoiceInputBar(
                            isListening = voiceState.isSpeaking,
                            amplitude = voiceState.amplitude,
                            onMicClick = { 
                                if (voiceState.isSpeaking) {
                                    voiceParser?.stopListening()
                                    neroState = "nero_hearingdown"
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onKeyboardClick = { 
                                showKeyboardInput = !showKeyboardInput 
                                if (showKeyboardInput) {
                                    neroState = "nero_down"
                                } else {
                                    neroState = "nero_hearingdown"
                                }
                            },
                            onCancelClick = {
                                voiceParser?.cancel()
                                onDraftMessageChange("")
                                showKeyboardInput = false
                                neroState = "nero_hearingdown"
                            },
                            hideMicButton = hideMicButton,
                            isActive = inputActive
                        )
                        
                        if (inputActive && customKeyboard) {
                            Surface(
                                tonalElevation = 8.dp,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextField(
                                            value = draftMessage,
                                            onValueChange = { onDraftMessageChange(it) },
                                            modifier = Modifier.weight(1f),
                                            placeholder = { Text("Escribe a Nero...", fontSize = fontSize.sp) },
                                            readOnly = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent
                                            ),
                                            textStyle = LocalTextStyle.current.copy(fontSize = fontSize.sp)
                                        )
                                        IconButton(
                                            onClick = { 
                                                if (draftMessage.isNotBlank()) {
                                                    onSendMessage(draftMessage)
                                                    showKeyboardInput = false
                                                    neroState = "nero_hearingdown"
                                                }
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = Color(0xFF5046BD),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar a Nero")
                                        }
                                    }
                                    if (showKeyboardInput) {
                                        CustomKeyboard(
                                            onKeyClick = { onDraftMessageChange(draftMessage + it) },
                                            onDeleteClick = { if (draftMessage.isNotEmpty()) onDraftMessageChange(draftMessage.dropLast(1)) },
                                            onSpaceClick = { onDraftMessageChange("$draftMessage ") },
                                            onEnterClick = { 
                                                if (draftMessage.isNotBlank()) {
                                                    onSendMessage(draftMessage)
                                                    showKeyboardInput = false
                                                    neroState = "nero_hearingdown"
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    TabSelector(
                        selectedTab = selectedTab,
                        onTabSelected = { 
                            selectedTab = it 
                            if (it == "Ajustes") showKeyboardInput = false
                        },
                        fontSize = fontSize
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(fontSize = fontSize.sp)
                ) {
                    when (selectedTab) {
                        "Tareas" -> RecordatoriosScreen(fontSize = fontSize)
                        "Eventos" -> EventosScreen(fontSize = fontSize)
                        "Ajustes" -> SettingsScreen(
                            fontSize = fontSize,
                            onFontSizeChange = { fontSize = it },
                            isDarkMode = isDarkMode,
                            onDarkModeChange = { isDarkMode = it },
                            voiceResponse = voiceResponse,
                            onVoiceResponseChange = { voiceResponse = it },
                            customKeyboard = customKeyboard,
                            onCustomKeyboardChange = { customKeyboard = it },
                            hideMicButton = hideMicButton,
                            onHideMicButtonChange = { hideMicButton = it }
                        )
                        "Habla" -> {
                            if (viewModel != null) {
                                ChatScreen(
                                    viewModel = viewModel,
                                    useCustomKeyboard = customKeyboard,
                                    fontSize = fontSize
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Chat no disponible en Preview")
                                }
                            }
                        }
                    }
                }

                if (showKeyboardInput && !customKeyboard) {
                    KeyboardInputOverlay(
                        text = draftMessage,
                        onTextChange = { onDraftMessageChange(it) },
                        fontSize = fontSize,
                        onClose = { 
                            if (draftMessage.isNotBlank()) {
                                onSendMessage(draftMessage)
                                neroState = "nero_hearingdown"
                            } else {
                                neroState = "nero_hearingdown"
                            }
                            showKeyboardInput = false 
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NeroAssistant(
    modifier: Modifier = Modifier,
    neroState: String,
    onReady: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var displayedState by remember { mutableStateOf(neroState) }
    var targetState by remember { mutableStateOf(neroState) }

    LaunchedEffect(neroState) {
        targetState = neroState
    }

    Box(modifier = modifier) {
        Crossfade(
            targetState = displayedState,
            animationSpec = tween(durationMillis = 300),
            label = "NeroTransition"
        ) { state ->
            val drawableId = remember(state) {
                context.resources.getIdentifier(state, "drawable", context.packageName)
            }
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(if (drawableId != 0) drawableId else android.R.drawable.ic_menu_help)
                    .decoderFactory(if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory())
                    .build()
            )

            LaunchedEffect(painter.state) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    onReady(state)
                }
            }

            Image(
                painter = painter,
                contentDescription = "Nero",
                modifier = Modifier.fillMaxSize()
            )
        }

        if (targetState != displayedState) {
            val nextDrawableId = remember(targetState) {
                context.resources.getIdentifier(targetState, "drawable", context.packageName)
            }
            val nextPainter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(if (nextDrawableId != 0) nextDrawableId else android.R.drawable.ic_menu_help)
                    .decoderFactory(if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory())
                    .build()
            )

            LaunchedEffect(nextPainter.state) {
                if (nextPainter.state is AsyncImagePainter.State.Success) {
                    displayedState = targetState
                }
            }

            Image(
                painter = nextPainter,
                contentDescription = null,
                modifier = Modifier.size(1.dp).alpha(0f)
            )
        }
    }
}

@Composable
fun TabSelector(selectedTab: String, onTabSelected: (String) -> Unit, fontSize: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(Color(0xFF5046BD), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .padding(4.dp)
    ) {
        val tabs = listOf(
            Triple("Tareas", Icons.Default.AddTask, "Tareas"),
            Triple("Habla", Icons.Default.ChatBubble, "Nero"),
            Triple("Eventos", Icons.Default.CalendarMonth, "Eventos"),
            Triple("Ajustes", Icons.Default.Settings, "Ajustes")
        )

        tabs.forEach { (id, icon, label) ->
            TabButton(
                text = label,
                icon = icon,
                isSelected = selectedTab == id,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(id) },
                fontSize = fontSize
            )
        }
    }
}

@Composable
fun TabButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    fontSize: Int
) {
    val animatedOffset by animateDpAsState(
        targetValue = if (isSelected) (-30).dp else (-10).dp,
        label = "iconOffset"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(y = animatedOffset)
                .size(if (isSelected) 54.dp else 36.dp)
                .background(
                    color = if (isSelected) Color(0xFF6ED1B8) else Color.Transparent,
                    shape = CircleShape
                )
                .then(
                    if (isSelected) Modifier.border(3.dp, Color(0xFFF5F5F5), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (isSelected) 28.dp else 24.dp),
                tint = Color.White
            )
        }

        Text(
            text = text,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            fontSize = (fontSize - 2).coerceAtLeast(10).sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
            color = Color.White
        )
    }
}

@Composable
fun VoiceInputBar(
    isListening: Boolean,
    amplitude: Float,
    onMicClick: () -> Unit,
    onKeyboardClick: () -> Unit,
    onCancelClick: () -> Unit,
    hideMicButton: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Espacio reservado para Nero que ahora está en el FAB
        Spacer(modifier = Modifier.width(100.dp))

        // Spacer que empuja todo lo demás a la derecha
        Spacer(modifier = Modifier.weight(1f))

        if (isActive) {
            IconButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .background(Color.LightGray.copy(alpha = 0.4f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancelar")
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        if (isListening) {
            ChatSoundWaveVisualizer(amplitude = amplitude)
            Spacer(modifier = Modifier.width(12.dp))
        }

        if (!hideMicButton) {
            FloatingActionButton(
                onClick = onMicClick,
                containerColor = if (isListening) Color(0xFF4CAF50) else Color(0xFF6ED1B8),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Micrófono", modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        FloatingActionButton(
            onClick = onKeyboardClick,
            containerColor = Color(0xFFF0F0F0),
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = "Teclado")
        }
    }
}

@Composable
fun KeyboardInputOverlay(
    text: String,
    onTextChange: (String) -> Unit,
    fontSize: Int,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Escribe a Nero", fontSize = (fontSize + 4).sp) },
        text = {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = fontSize.sp)
            )
        },
        confirmButton = {
            Button(onClick = onClose) {
                Text("Enviar", fontSize = fontSize.sp)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    IHMTheme {
        MainScreenContent(
            messages = listOf(ChatMessage("¡Hola! Soy Nero", false)),
            draftMessage = "",
            onDraftMessageChange = {},
            onSendMessage = {}
        )
    }
}
