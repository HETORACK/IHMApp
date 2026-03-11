package com.example.ihm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ihm.ui.theme.IHMTheme
import kotlin.random.Random

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf("Tareas") }
    var isListening by remember { mutableStateOf(false) }
    var showKeyboardInput by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf("") }

    // Estados de configuración (En una app real esto vendría de un ViewModel/DataStore)
    var fontSize by remember { mutableIntStateOf(16) }
    var isDarkMode by remember { mutableStateOf(false) }
    var voiceResponse by remember { mutableStateOf(true) }
    var customKeyboard by remember { mutableStateOf(false) }
    var hideMicButton by remember { mutableStateOf(false) }

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
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Ocultar botones de entrada si estamos en Ajustes o en el Chat de Nero (porque el chat tiene su propia barra)
                    if (selectedTab != "Ajustes" && selectedTab != "Habla") {
                        VoiceInputBar(
                            isListening = isListening,
                            onMicClick = { isListening = !isListening },
                            onKeyboardClick = { showKeyboardInput = !showKeyboardInput },
                            hideMicButton = hideMicButton
                        )
                    }
                    TabSelector(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
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
                        "Tareas" -> RecordatoriosScreen()
                        "Eventos" -> EventosScreen()
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
                        "Habla" -> ChatScreen()
                    }
                }

                if (showKeyboardInput) {
                    KeyboardInputOverlay(
                        text = transcribedText,
                        onTextChange = { transcribedText = it },
                        onClose = { showKeyboardInput = false },
                        useCustomKeyboard = customKeyboard
                    )
                }
            }
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
    onMicClick: () -> Unit,
    onKeyboardClick: () -> Unit,
    hideMicButton: Boolean
) {
    var currentAmplitude by remember { mutableStateOf(0.1f) }

    LaunchedEffect(isListening) {
        if (isListening) {
            while (true) {
                currentAmplitude = Random.nextFloat() * (1.0f - 0.2f) + 0.2f
                kotlinx.coroutines.delay(100)
            }
        } else {
            currentAmplitude = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(horizontalAlignment = Alignment.End) {
            FloatingActionButton(
                onClick = onKeyboardClick,
                containerColor = Color(0xFF6A5EEC),
                contentColor = Color.White,
                modifier = Modifier.size(60.dp),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Keyboard,
                    contentDescription = "Teclado",
                    modifier = Modifier.size(40.dp)
                )
            }

            if (!hideMicButton) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = isListening) {
                        SoundWaveVisualizer(amplitude = currentAmplitude)
                    }

                    FloatingActionButton(
                        onClick = onMicClick,
                        containerColor = if (isListening) Color(0xFF4CAF50) else Color(0xFF6ED1B8),
                        contentColor = Color.White,
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Microfono",
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun KeyboardInputOverlay(
    text: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    useCustomKeyboard: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(16.dp)
            ) {
                if (useCustomKeyboard) {
                    Text("Usando Teclado Propio (Simulado)", modifier = Modifier.padding(bottom = 8.dp), fontWeight = FontWeight.Bold)
                    // Aquí iría tu UI de teclado propio
                }
                
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Escribe aquí...") }
                )
                Button(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Text("Listo")
                }
            }
        }
    }
}

@Composable
fun SoundWaveVisualizer(amplitude: Float) {
    Row(
        modifier = Modifier
            .padding(end = 8.dp)
            .background(Color(0xFF6750A4), RoundedCornerShape(32.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { index ->
            val animatedHeight by animateDpAsState(
                targetValue = (10 + (amplitude * 20 * (index % 3 + 1) * 0.5f)).dp,
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}
