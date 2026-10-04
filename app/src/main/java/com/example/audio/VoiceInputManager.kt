package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceInputManager(
  private val context: Context,
  private val onSpeechResult: (String) -> Unit,
  private val onPartialSpeech: (String) -> Unit = {},
  private val onSpeechStarted: () -> Unit = {},
  private val onErrorOccurred: (String) -> Unit = {}
) {

  private var speechRecognizer: SpeechRecognizer? = null

  private val _isListening = MutableStateFlow(false)
  val isListening = _isListening.asStateFlow()

  private val _micRmsDb = MutableStateFlow(0f)
  val micRmsDb = _micRmsDb.asStateFlow()

  fun isAvailable(): Boolean {
    return SpeechRecognizer.isRecognitionAvailable(context)
  }

  fun startListening() {
    if (_isListening.value) return

    onSpeechStarted()

    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      onErrorOccurred("Speech recognition is not available on this device.")
      return
    }

    destroyRecognizer()

    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
      setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
          _isListening.value = true
          onSpeechStarted()
        }

        override fun onBeginningOfSpeech() {
          _isListening.value = true
          onSpeechStarted()
        }

        override fun onRmsChanged(rmsdB: Float) {
          // Normalize rmsdB (-2 to 10 dB typically) to 0.0 .. 1.0 range
          val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
          _micRmsDb.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
          _isListening.value = false
          _micRmsDb.value = 0f
        }

        override fun onError(error: Int) {
          _isListening.value = false
          _micRmsDb.value = 0f
          val message = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap to try again."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input heard."
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_CLIENT -> "Client error occurred."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission needed."
            SpeechRecognizer.ERROR_NETWORK -> "Network error occurred."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy."
            else -> "Speech recognition error ($error)"
          }
          onErrorOccurred(message)
        }

        override fun onResults(results: Bundle?) {
          _isListening.value = false
          _micRmsDb.value = 0f
          val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          val bestMatch = matches?.firstOrNull()?.trim()
          if (!bestMatch.isNullOrEmpty()) {
            onSpeechResult(bestMatch)
          }
        }

        override fun onPartialResults(partialResults: Bundle?) {
          val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          val partial = matches?.firstOrNull()?.trim()
          if (!partial.isNullOrEmpty()) {
            onPartialSpeech(partial)
          }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
      })
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      // Multilingual auto-detection support: primary device locale with Hindi and Indian English fallbacks
      putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN,en-IN,en-US")
      putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "mr-IN", "bn-IN", "ta-IN", "te-IN", "gu-IN", "pa-IN", "ur-IN"))
      putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
      putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }

    try {
      speechRecognizer?.startListening(intent)
      _isListening.value = true
    } catch (e: Exception) {
      _isListening.value = false
      onErrorOccurred("Could not start listening: ${e.localizedMessage}")
    }
  }

  fun stopListening() {
    try {
      speechRecognizer?.stopListening()
    } catch (_: Exception) {}
    _isListening.value = false
    _micRmsDb.value = 0f
  }

  fun destroyRecognizer() {
    try {
      speechRecognizer?.destroy()
    } catch (_: Exception) {}
    speechRecognizer = null
    _isListening.value = false
    _micRmsDb.value = 0f
  }
}
