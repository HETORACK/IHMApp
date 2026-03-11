package com.example.ihm.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

interface VoiceManager {
    fun startListening()
    fun stopListening()
    val amplitude: StateFlow<Float> // Flujo constante de datos para la UI
}

class AndroidVoiceManager : VoiceManager {
    private var isRecording = false
    private val _amplitude = MutableStateFlow(0f)
    override val amplitude: StateFlow<Float> = _amplitude

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    override fun startListening() {
        if (isRecording) return
        isRecording = true

        job = scope.launch {
            // NOTA: Aquí simulamos la lectura.
            // Para lectura real de AudioRecord, necesitarías un buffer de Shorts.
            while (isActive && isRecording) {
                // Generamos el valor aleatorio
                val mockAmplitude = Random.nextFloat() * (0.8f) + 0.2f
                // Usamos .value para actualizaciones rápidas de UI (es ligeramente más eficiente que emit en StateFlow)
                _amplitude.value = mockAmplitude
                // 50ms = 20 actualizaciones por segundo (Ideal para animaciones suaves)
                delay(50)
            }
        }
    }

    override fun stopListening() {
        isRecording = false
        job?.cancel()
        _amplitude.value = 0f
    }
}