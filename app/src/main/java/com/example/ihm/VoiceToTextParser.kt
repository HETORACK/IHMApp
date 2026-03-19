package com.example.ihm

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Clase que envuelve el reconocimiento de voz de Android.
 */
class VoiceToTextParser(
    private val app: Application
) {

    private val _state = MutableStateFlow(VoiceToTextParserState())
    val state: StateFlow<VoiceToTextParserState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.update { it.copy(error = null) }
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            _state.update {
                it.copy(amplitude = rmsdB)
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _state.update {
                it.copy(isSpeaking = false)
            }
        }

        override fun onError(error: Int) {
            if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_NO_MATCH) {
                _state.update { it.copy(isSpeaking = false) }
                return
            }
            _state.update {
                it.copy(error = "Error: $error", isSpeaking = false)
            }
        }

        override fun onResults(results: Bundle?) {
            results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.getOrNull(0)
                ?.let { result ->
                    _state.update {
                        it.copy(spokenText = result, isFinished = true, isSpeaking = false)
                    }
                }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.getOrNull(0)
                ?.let { result ->
                    _state.update {
                        it.copy(spokenText = result)
                    }
                }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    fun startListening(languageCode: String = "es-ES") {
        _state.update { VoiceToTextParserState() }

        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            _state.update {
                it.copy(error = "El reconocimiento de voz no está disponible")
            }
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
        }

        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(app)
        }
        recognizer?.setRecognitionListener(recognitionListener)
        recognizer?.startListening(intent)

        _state.update {
            it.copy(isSpeaking = true)
        }
    }

    fun stopListening() {
        _state.update {
            it.copy(isSpeaking = false)
        }
        recognizer?.stopListening()
    }

    fun cancel() {
        _state.update { VoiceToTextParserState() }
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    fun clear() {
        _state.update { VoiceToTextParserState() }
    }
}

data class VoiceToTextParserState(
    val spokenText: String = "",
    val isSpeaking: Boolean = false,
    val isFinished: Boolean = false,
    val error: String? = null,
    val amplitude: Float = 0f
)
