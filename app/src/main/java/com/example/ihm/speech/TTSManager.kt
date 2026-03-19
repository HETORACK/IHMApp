package com.example.ihm.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class TTSManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    // Se usa un objeto anónimo para evitar que la clase principal implemente la interfaz directamente,
    // lo que puede causar problemas en el Preview de Compose si la interfaz no está disponible.
    private val onInitListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "MX"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSManager", "Lenguaje no soportado")
            } else {
                isReady = true
            }
        } else {
            Log.e("TTSManager", "Error al inicializar TTS")
        }
    }

    init {
        try {
            tts = TextToSpeech(context, onInitListener)
        } catch (e: Exception) {
            Log.e("TTSManager", "Error al crear TextToSpeech: ${e.message}")
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
