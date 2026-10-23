package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentUtteranceId = MutableStateFlow<String?>(null)
    val currentUtteranceId: StateFlow<String?> = _currentUtteranceId.asStateFlow()

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (e: Exception) {
            Log.e("TtsManager", "Failed to construct TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    _currentUtteranceId.value = utteranceId
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentUtteranceId.value = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentUtteranceId.value = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    _currentUtteranceId.value = null
                }
            })
        } else {
            Log.w("TtsManager", "TextToSpeech init status not SUCCESS: $status")
        }
    }

    /**
     * Speaks the provided text aloud.
     * Automatically selects Tamil locale (ta_IN) if Tamil Unicode block is detected,
     * otherwise defaults to English (en_IN or en_US).
     */
    fun speak(text: String, utteranceId: String = System.currentTimeMillis().toString()) {
        if (!isInitialized || tts == null) return

        // Clean markdown code blocks and symbols for speech
        val cleanedText = text
            .replace(Regex("```[a-zA-Z]*"), "")
            .replace("```", "")
            .replace(Regex("[#*_`>]"), "")
            .trim()

        if (cleanedText.isBlank()) return

        val hasTamil = containsTamil(cleanedText)
        val targetLocale = if (hasTamil) {
            Locale("ta", "IN")
        } else {
            Locale("en", "IN")
        }

        try {
            val langResult = tts?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English US or default locale
                tts?.setLanguage(Locale.US)
            }
            tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e("TtsManager", "Failed to speak text", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
            _currentUtteranceId.value = null
        } catch (e: Exception) {
            Log.e("TtsManager", "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("TtsManager", "Error shutting down TTS", e)
        }
    }

    private fun containsTamil(str: String): Boolean {
        for (char in str) {
            val code = char.code
            // Tamil unicode block is 0x0B80 - 0x0BFF
            if (code in 0x0B80..0x0BFF) {
                return true
            }
        }
        return false
    }
}
