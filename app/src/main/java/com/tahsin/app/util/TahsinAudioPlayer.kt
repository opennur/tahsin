package com.tahsin.app.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

/**
 * Pemutar audio contoh bacaan, urutan prioritas:
 * 1. Cache internal `filesDir/audio/...` (hasil unduh dari dalam aplikasi
 *    via `AudioDownloader` — offline setelah diunduh sekali).
 * 2. MP3 di-bundle di `assets/audio/...` (hasil tools/download_minshawi.sh).
 * 3. MP3 qari online (audioUrl dari mushaf.json).
 * 4. TextToSpeech Arab perangkat (kualitas dasar, bukan murattal).
 */
class TahsinAudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val cacheRoot: File = File(context.filesDir, "audio")
    private var mediaPlayer: MediaPlayer? = null
    private var openAfd: AssetFileDescriptor? = null

    private val tts: TextToSpeech = TextToSpeech(appContext) {
        // Bahasa Arab di-set ulang pada setiap speak()/isArabicTtsAvailable().
    }

    /** Mainkan ayat: cache → asset → URL → fallback (mis. TTS). */
    fun playAyah(
        surahNumber: Int,
        ayahNumber: Int,
        audioUrl: String?,
        text: String,
        onFallback: () -> Unit = {},
    ) {
        val key = surahNumber.toString().padStart(3, '0') +
            ayahNumber.toString().padStart(3, '0') + ".mp3"
        val cached = File(cacheRoot, key)
        if (cached.exists() && cached.length() > 0L && playFromFile(cached, onFallback)) return

        val afd = runCatching { appContext.assets.openFd("audio/$key") }.getOrNull()
        if (afd != null && playFromAfd(afd, onFallback)) return

        val url = audioUrl
        if (!url.isNullOrBlank()) {
            if (playFromUrl(url, onFallback)) return
        }

        onFallback()
    }

    /** Mainkan satu kata: cache → asset → URL word-by-word → fallback. */
    fun playWord(
        surahNumber: Int,
        ayahNumber: Int,
        wordIndex: Int,
        word: String,
        onFallback: () -> Unit = {},
    ) {
        val key = surahNumber.toString().padStart(3, '0') + "_" +
            ayahNumber.toString().padStart(3, '0') + "_" +
            (wordIndex + 1).toString().padStart(3, '0') + ".mp3"
        val cached = File(File(cacheRoot, "wbw"), key)
        if (cached.exists() && cached.length() > 0L && playFromFile(cached, onFallback)) return

        val afd = runCatching { appContext.assets.openFd("audio/wbw/$key") }.getOrNull()
        if (afd != null && playFromAfd(afd, onFallback)) return

        if (playFromUrl("https://audio.qurancdn.com/wbw/$key", onFallback)) return
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

    /** true jika berhasil memulai pemutaran dari file cache. */
    private fun playFromFile(file: File, onError: () -> Unit): Boolean {
        return runCatching {
            releaseMedia()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
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
