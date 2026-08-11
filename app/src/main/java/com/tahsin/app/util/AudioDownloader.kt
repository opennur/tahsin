package com.tahsin.app.util

import android.content.Context
import com.tahsin.app.data.quran.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Hasil unduhan audio. */
data class DownloadStats(
    val ok: Int,
    val total: Int,
)

/**
 * Mengunduh audio contoh ke penyimpanan internal (`filesDir/audio`) supaya
 * bisa diputar OFFLINE setelah diunduh sekali. Unduhan dilakukan PER SURAH:
 * semua MP3 ayat (Minshawy) + semua MP3 kata (quran.com) surah itu.
 */
class AudioDownloader(context: Context) {

    private val appContext = context.applicationContext

    /** filesDir/audio — relatif sama dengan konvensi assets/audio. */
    val audioDir: File
        get() = File(appContext.filesDir, "audio")

    fun ayahFile(surah: Int, ayah: Int): File = File(audioDir, AudioUrls.ayahKey(surah, ayah))

    fun wordFile(surah: Int, ayah: Int, wordIndex: Int): File =
        File(File(audioDir, "wbw"), AudioUrls.wordKey(surah, ayah, wordIndex))

    /** Pastikan audio ayat ada di cache (unduh kalau belum). */
    suspend fun ensureAyah(surah: Int, ayah: Int): File = withContext(Dispatchers.IO) {
        val file = ayahFile(surah, ayah)
        if (file.exists() && file.length() > 0L) file else download(AudioUrls.ayahUrl(surah, ayah), file)
    }

    /** Pastikan audio kata ada di cache. */
    suspend fun ensureWord(surah: Int, ayah: Int, wordIndex: Int): File = withContext(Dispatchers.IO) {
        val file = wordFile(surah, ayah, wordIndex)
        if (file.exists() && file.length() > 0L) file else download(AudioUrls.wordUrl(surah, ayah, wordIndex), file)
    }

    /** Semua audio surah (ayat + kata) sudah ada di cache? */
    fun isSurahAudioComplete(surah: Surah): Boolean {
        if (surah.ayahs.isEmpty()) return false
        surah.ayahs.forEach { ayah ->
            val af = ayahFile(surah.number, ayah.number)
            if (!(af.exists() && af.length() > 0L)) return false
            ayah.words.forEachIndexed { wi, _ ->
                val wf = wordFile(surah.number, ayah.number, wi)
                if (!(wf.exists() && wf.length() > 0L)) return false
            }
        }
        return true
    }

    /**
     * Unduh SEMUA audio satu surah (per ayat + per kata).
     * File yang sudah ada dilewati; kegagalan per-file tidak menggagalkan sisanya.
     */
    suspend fun downloadSurah(
        surah: Surah,
        onProgress: (done: Int, total: Int) -> Unit,
    ): DownloadStats = withContext(Dispatchers.IO) {
        val jobs = mutableListOf<Pair<String, File>>()
        surah.ayahs.forEach { ayah ->
            jobs += AudioUrls.ayahUrl(surah.number, ayah.number) to ayahFile(surah.number, ayah.number)
            ayah.words.forEachIndexed { wi, _ ->
                jobs += AudioUrls.wordUrl(surah.number, ayah.number, wi) to wordFile(surah.number, ayah.number, wi)
            }
        }

        var done = 0
        var ok = 0
        val total = jobs.size
        jobs.forEach { (url, file) ->
            if (file.exists() && file.length() > 0L) {
                ok++
            } else if (runCatching { download(url, file) }.isSuccess) {
                ok++
            }
            done++
            onProgress(done, total)
        }
        DownloadStats(ok, total)
    }

    private fun download(url: String, out: File): File {
        out.parentFile?.mkdirs()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        try {
            conn.connect()
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
            conn.inputStream.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            conn.disconnect()
        }
        return out
    }
}
