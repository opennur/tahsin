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
        fun onError(message: String)
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
                l.onError(errorMessage(error))
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

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH ->
            "Tidak ada yang cocok terdeteksi — coba baca lebih pelan dan jelas."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "Tidak ada suara terdeteksi (timeout)."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Izin mikrofon belum diberikan."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "STT butuh internet (atau pasang paket bahasa Arab offline di pengaturan HP)."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "SpeechRecognizer sedang sibuk — coba lagi."
        else -> "Error speech recognition (kode $error)."
    }
}
