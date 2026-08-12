package com.tahsin.app.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

/** Sumber pemutaran saat ini — penentu tombol Dengar (ayat) vs tombol kata. */
enum class PlaySource { AYAH, WORD, NONE }

/**
 * Pemutar audio contoh bacaan, urutan prioritas:
 * 1. Cache internal `filesDir/audio/...` (hasil unduh dari dalam aplikasi
 *    via `AudioDownloader` — offline setelah diunduh sekali).
 * 2. MP3 di-bundle di `assets/audio/...` (hasil tools/download_minshawi.sh).
 * 3. MP3 qari online (URL dihitung dari nomor surah/ayat).
 * 4. TextToSpeech Arab perangkat (kualitas dasar, bukan murattal).
 */
class TahsinAudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val cacheRoot: File = File(context.filesDir, "audio")
    private var mediaPlayer: MediaPlayer? = null
    private var openAfd: AssetFileDescriptor? = null
    private var isPlaying = false

    /** Sumber pemutaran aktif — diset sebelum memulai media (sumber kebenaran UI). */
    var source: PlaySource = PlaySource.NONE
        private set

    /** Callback status pemutaran (true = sedang memutar). Dipakai UI untuk tombol Dengar/Stop. */
    var onPlaybackChange: ((Boolean) -> Unit)? = null

    /** Hentikan audio yang sedang diputar. */
    fun stop() {
        releaseMedia()
        runCatching { tts.stop() }
        source = PlaySource.NONE
        setPlaying(false)
    }

    private fun setPlaying(value: Boolean) {
        if (isPlaying != value) {
            isPlaying = value
            onPlaybackChange?.invoke(value)
        }
    }

    private val tts: TextToSpeech = TextToSpeech(appContext) {
        // Bahasa Arab di-set ulang pada setiap speak()/isArabicTtsAvailable().
    }

    /** Mainkan ayat: cache → asset → URL → fallback (mis. TTS). */
    fun playAyah(
        surahNumber: Int,
        ayahNumber: Int,
        text: String,
        onFallback: () -> Unit = {},
    ) {
        source = PlaySource.AYAH
        val key = AudioUrls.ayahKey(surahNumber, ayahNumber)
        val cached = File(cacheRoot, key)
        if (cached.exists() && cached.length() > 0L && playFromFile(cached, onFallback)) return

        val afd = runCatching { appContext.assets.openFd("audio/$key") }.getOrNull()
        if (afd != null && playFromAfd(afd, onFallback)) return

        if (playFromUrl(AudioUrls.ayahUrl(surahNumber, ayahNumber), onFallback)) return

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
        source = PlaySource.WORD
        val key = AudioUrls.wordKey(surahNumber, ayahNumber, wordIndex)
        val cached = File(File(cacheRoot, "wbw"), key)
        if (cached.exists() && cached.length() > 0L && playFromFile(cached, onFallback)) return

        val afd = runCatching { appContext.assets.openFd("audio/wbw/$key") }.getOrNull()
        if (afd != null && playFromAfd(afd, onFallback)) return

        if (playFromUrl(AudioUrls.wordUrl(surahNumber, ayahNumber, wordIndex), onFallback)) return
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
                setOnPreparedListener { start(); setPlaying(true) }
                setOnCompletionListener { setPlaying(false) }
                setOnErrorListener { _, _, _ ->
                    setPlaying(false)
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
                setOnPreparedListener { start(); setPlaying(true) }
                setOnCompletionListener { setPlaying(false) }
                setOnErrorListener { _, _, _ ->
                    setPlaying(false)
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
                setOnPreparedListener { start(); setPlaying(true) }
                setOnCompletionListener { setPlaying(false) }
                setOnErrorListener { _, _, _ ->
                    setPlaying(false)
                    onError()
                    true
                }
                prepareAsync()
            }
            true
        }.getOrDefault(false)
    }

    private fun releaseMedia() {
        // Pemutaran lama yang DIBAYANG oleh pemutaran baru harus dilaporkan
        // berhenti (release() saja tidak memicu onCompletion) — kalau tidak,
        // UI bisa "stuck" (tombol Stop tidak muncul).
        if (isPlaying) setPlaying(false)
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
