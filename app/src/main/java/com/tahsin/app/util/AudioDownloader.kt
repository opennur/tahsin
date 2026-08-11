package com.tahsin.app.util

import android.content.Context
import com.tahsin.app.data.quran.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Hasil unduhan massal audio. */
data class DownloadStats(
    val ok: Int,
    val total: Int,
)

/**
 * Mengunduh audio contoh ke penyimpanan internal aplikasi (`filesDir/audio`)
 * supaya bisa diputar OFFLINE setelah diunduh sekali:
 * - per AYAT: Minshawy Murattal dari everyayah.com
 * - per KATA: word-by-word dari qurancdn.com
 *
 * Nama file sama dengan konvensi `assets/audio`, sehingga `TahsinAudioPlayer`
 * memeriksa cache (filesDir) terlebih dahulu, lalu asset bundle, lalu URL.
 */
class AudioDownloader(context: Context) {

    private val appContext = context.applicationContext

    /** Direktori cache: filesDir/audio (relatif sama dengan assets/audio). */
    val audioDir: File
        get() = File(appContext.filesDir, "audio")

    private fun key3(n: Int): String = n.toString().padStart(3, '0')

    fun ayahFile(surah: Int, ayah: Int): File =
        File(audioDir, "${key3(surah)}${key3(ayah)}.mp3")

    fun wordFile(surah: Int, ayah: Int, wordIndex: Int): File =
        File(File(audioDir, "wbw"), "${key3(surah)}_${key3(ayah)}_${key3(wordIndex + 1)}.mp3")

    private fun ayahUrl(surah: Int, ayah: Int): String =
        "https://everyayah.com/data/Minshawy_Murattal_128kbps/${key3(surah)}${key3(ayah)}.mp3"

    private fun wordUrl(surah: Int, ayah: Int, wordIndex: Int): String =
        "https://audio.qurancdn.com/wbw/${key3(surah)}_${key3(ayah)}_${key3(wordIndex + 1)}.mp3"

    /** Pastikan audio ayat ada di cache (unduh kalau belum). Melempar exception kalau gagal. */
    suspend fun ensureAyah(surah: Int, ayah: Int): File = withContext(Dispatchers.IO) {
        val file = ayahFile(surah, ayah)
        if (file.exists() && file.length() > 0L) file else download(ayahUrl(surah, ayah), file)
    }

    /** Pastikan audio kata ada di cache. Melempar exception kalau gagal. */
    suspend fun ensureWord(surah: Int, ayah: Int, wordIndex: Int): File = withContext(Dispatchers.IO) {
        val file = wordFile(surah, ayah, wordIndex)
        if (file.exists() && file.length() > 0L) file else download(wordUrl(surah, ayah, wordIndex), file)
    }

    /**
     * Unduh SEMUA audio (per ayat + per kata) untuk mushaf yang diberikan.
     * File yang sudah ada dilewati; kegagalan per-file tidak menggagalkan sisanya.
     */
    suspend fun downloadAll(
        surahs: List<Surah>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): DownloadStats = withContext(Dispatchers.IO) {
        val jobs = mutableListOf<Pair<String, File>>()
        surahs.forEach { surah ->
            surah.ayahs.forEach { ayah ->
                jobs += ayahUrl(surah.number, ayah.number) to ayahFile(surah.number, ayah.number)
                ayah.words.forEachIndexed { wi, _ ->
                    jobs += wordUrl(surah.number, ayah.number, wi) to wordFile(surah.number, ayah.number, wi)
                }
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
