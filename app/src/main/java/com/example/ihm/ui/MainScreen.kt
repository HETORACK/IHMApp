package com.example.ihm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import kotlin.random.Random


@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf("Tareas") }
    var isListening by remember { mutableStateOf(false) }
    var showKeyboardInput by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (selectedTab == "Tareas") "Recordatorios" else "Eventos",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Respeta la barra de navegación del sistema
            ) {
                VoiceInputBar(
                    isListening = isListening,
                    onMicClick = { isListening = !isListening },
                    onKeyboardClick = { showKeyboardInput = !showKeyboardInput },
                    transcribedText = transcribedText
                )
                TabSelector(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                "Tareas" -> RecordatoriosScreen()
                "Habla" -> ChatScreen()
                "Eventos" -> EventosScreen()
            }

            if (showKeyboardInput) {
                KeyboardInputOverlay(
                    text = transcribedText,
                    onTextChange = { transcribedText = it },
                    onClose = { showKeyboardInput = false }
                )
            }
        }
    }
}

@Composable
fun TabSelector(selectedTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF5046BD), RoundedCornerShape(30.dp, 30.dp))
            .padding(4.dp)
     ) {
        TabButton(
            text = "Tareas",
            icon = Icons.Default.AddTask ,
            isSelected = selectedTab == "Tareas",
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected("Tareas") }
        )
        TabButton(
            text = "Nero",
            icon = Icons.Default.ChatBubble,
            isSelected = selectedTab == "Habla",
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected("Habla") }
        )
        TabButton(
            text = "Eventos",
            icon = Icons.Default.CalendarMonth,
            isSelected = selectedTab == "Eventos",
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected("Eventos") }
        )
        TabButton(
            text = "Ajustes",
            icon = Icons.Default.Settings,
            isSelected = selectedTab == "Ajustes",
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected("Ajustes") }
        )
    }
}

@Composable
fun TabButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val animatedOffset by animateDpAsState(
        targetValue = if (isSelected) (-30).dp else (-10).dp, // Subimos ambos para centrar
        label = "iconOffset"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Icono flotante
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

        // Texto con posición fija cerca del fondo
        Text(
            text = text,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp), // Ajusta este padding para subir/bajar el texto
            fontSize = 15.sp,
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
    transcribedText: String
) {
    // 1. Estado local para la amplitud (reactividad)
    var currentAmplitude by remember { mutableStateOf(0.1f) }

    // 2. Efecto lateral: cuando isListening sea true, generamos valores aleatorios
    LaunchedEffect(isListening) {
        if (isListening) {
            while (true) {
                // Genera un Float aleatorio entre 0.2 y 1.0
                currentAmplitude = Random.nextFloat() * (1.0f - 0.2f) + 0.2f
                kotlinx.coroutines.delay(100)
            }
        } else {
            currentAmplitude = 0f // Reset al dejar de escuchar
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(horizontalAlignment = Alignment.End) {
            // Botón de Teclado
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 3. Reemplazamos el Row de texto por el Visualizer animado
                AnimatedVisibility(visible = isListening) {
                    SoundWaveVisualizer(amplitude = currentAmplitude)
                }

                // Botón de Micrófono
                FloatingActionButton(
                    onClick = onMicClick,
                    // Cambiamos el color según si escucha o no para dar feedback visual
                    containerColor = if (isListening) Color(0xFF4CAF50) else Color(0xFF6ED1B8),
                    contentColor = Color.White,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microfono",
                        modifier = Modifier.size(60.dp)
                    )
                }

                // Espaciador para que no quede pegado al borde derecho si así lo deseas
                Spacer(modifier = Modifier.width(16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun KeyboardInputOverlay(
    text: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit
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
        // Dibujamos 5 barras que reaccionan a la amplitud
        repeat(5) { index ->
            val heightFactor = remember { mutableStateOf(1f) }
            // Añadimos un poco de aleatoriedad para que se vea orgánico
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
