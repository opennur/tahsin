package com.tahsin.app.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Pembungkus `SpeechRecognizer` bawaan Android dengan bahasa Arab (ar-SA).
 *
 * - Partial results real-time (`onPartialResults`) → highlight kata saat membaca.
 * - Butuh izin RECORD_AUDIO (minta di UI).
 * - Online default; bisa offline bila paket bahasa Arab terpasang di HP.
 * - Untuk kualitas makhraj tingkat lanjut, ganti dengan backend lain
 *   (Gemini Live / Whisper) lewat interface yang sama — TODO.
 */
class ArabicSpeechRecognizer(context: Context) {

    interface Listener {
        fun onPartial(text: String)
        fun onResult(text: String)
        fun onError(error: Int)
        fun onListeningChanged(listening: Boolean)
    }

    private val speech: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(
        context.applicationContext,
    )
    private var listener: Listener? = null

    fun start(l: Listener) {
        listener = l
        l.onListeningChanged(true)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                l.onListeningChanged(false)
            }

            override fun onError(error: Int) {
                l.onListeningChanged(false)
                // Kode mentah SpeechRecognizer — terjemahan diserahkan ke UI
                // (AppStrings.sttErrorMessage) agar pembungkus tetap netral bahasa.
                l.onError(error)
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                l.onResult(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) l.onPartial(text)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speech.startListening(intent)
    }

    fun stop() {
        runCatching { speech.stopListening() }
        listener?.onListeningChanged(false)
    }

    fun destroy() {
        runCatching { speech.destroy() }
    }
}
