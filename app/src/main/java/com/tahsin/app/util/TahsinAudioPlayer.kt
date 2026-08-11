package com.tahsin.app.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Pemutar audio contoh bacaan, urutan prioritas:
 * 1. MP3 lokal di `assets/audio/<surah><ayah>.mp3` (offline, hasil
 *    `tools/download_minshawi.sh` — Minshawy Murattal dari everyayah.com).
 * 2. MP3 qari online (audioUrl dari mushaf.json).
 * 3. TextToSpeech Arab perangkat (kualitas dasar, bukan murattal).
 */
class TahsinAudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var openAfd: AssetFileDescriptor? = null

    private val tts: TextToSpeech = TextToSpeech(appContext) {
        // Bahasa Arab di-set ulang pada setiap speak()/isArabicTtsAvailable().
    }

    /** Mainkan ayat: asset offline → URL → fallback (mis. TTS). */
    fun playAyah(
        surahNumber: Int,
        ayahNumber: Int,
        audioUrl: String?,
        text: String,
        onFallback: () -> Unit = {},
    ) {
        val assetPath = "audio/" + surahNumber.toString().padStart(3, '0') +
            ayahNumber.toString().padStart(3, '0') + ".mp3"
        val afd = runCatching { appContext.assets.openFd(assetPath) }.getOrNull()
        if (afd != null && playFromAfd(afd, onFallback)) return

        val url = audioUrl
        if (!url.isNullOrBlank()) {
            if (playFromUrl(url, onFallback)) return
        }

        onFallback()
    }

    /** Mainkan satu kata: asset offline → URL word-by-word (qurancdn) → fallback. */
    fun playWord(
        surahNumber: Int,
        ayahNumber: Int,
        wordIndex: Int,
        word: String,
        onFallback: () -> Unit = {},
    ) {
        val key = surahNumber.toString().padStart(3, '0') + "_" +
            ayahNumber.toString().padStart(3, '0') + "_" +
            (wordIndex + 1).toString().padStart(3, '0')
        val afd = runCatching { appContext.assets.openFd("audio/wbw/$key.mp3") }.getOrNull()
        if (afd != null && playFromAfd(afd, onFallback)) return
        if (playFromUrl("https://audio.qurancdn.com/wbw/$key.mp3", onFallback)) return
        onFallback()
    }

    /** Ucapkan kata/teks Arab lewat TTS perangkat (fallback terakhir). */
    fun speak(text: String) {
        val result = tts.setLanguage(Locale("ar"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tahsin")
    }

    fun isArabicTtsAvailable(): Boolean {
        val result = tts.setLanguage(Locale("ar"))
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    // ---- internal ----

    /** true jika berhasil memulai pemutaran dari file asset. */
    private fun playFromAfd(afd: AssetFileDescriptor, onError: () -> Unit): Boolean {
        return runCatching {
            releaseMedia()
            openAfd = afd
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                setOnPreparedListener { start() }
                setOnErrorListener { _, _, _ ->
                    onError()
                    true
                }
                prepareAsync()
            }
            true
        }.getOrDefault(false)
    }

    /** true jika berhasil memulai pemutaran dari URL. */
    private fun playFromUrl(url: String, onError: () -> Unit): Boolean {
        return runCatching {
            releaseMedia()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnErrorListener { _, _, _ ->
                    onError()
                    true
                }
                prepareAsync()
            }
            true
        }.getOrDefault(false)
    }

    private fun releaseMedia() {
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        runCatching { openAfd?.close() }
        openAfd = null
    }

    fun release() {
        releaseMedia()
        runCatching { tts.shutdown() }
    }
}
