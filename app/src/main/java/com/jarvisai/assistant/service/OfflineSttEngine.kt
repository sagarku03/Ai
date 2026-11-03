package com.jarvisai.assistant.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * On-device Offline Speech-to-Text (STT) engine using Android's on-device SpeechRecognizer
 * (SpeechRecognizer.createOnDeviceSpeechRecognizer on Android 12+ with offline language packs).
 */
class OfflineSttEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var recognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _finalTranscript = MutableStateFlow("")
    val finalTranscript: StateFlow<String> = _finalTranscript.asStateFlow()

    fun startOfflineDictation(
        languageTag: String = "en-IN",
        onResult: (String) -> Unit
    ) {
        scope.launch(Dispatchers.Main) {
            stopOfflineDictation()
            try {
                val sr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                ) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                sr.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        Log.w(TAG, "Offline STT error code: $error")
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull()?.trim().orEmpty()
                        if (best.isNotEmpty()) {
                            _finalTranscript.value = best
                            onResult(best)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (partial.isNotEmpty()) {
                            _partialTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag.ifBlank { Locale.US.toLanguageTag() })
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                recognizer = sr
                sr.startListening(intent)
            } catch (e: Exception) {
                _isListening.value = false
                Log.e(TAG, "Failed to start offline dictation: ${e.message}")
            }
        }
    }

    fun stopOfflineDictation() {
        scope.launch(Dispatchers.Main) {
            try {
                recognizer?.stopListening()
                recognizer?.cancel()
                recognizer?.destroy()
            } catch (_: Exception) {
            } finally {
                recognizer = null
                _isListening.value = false
            }
        }
    }

    companion object {
        private const val TAG = "OfflineSttEngine"
    }
}
