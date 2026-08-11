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

    // ---- manajemen audio terunduh ----

    /** Info audio yang sudah terunduh untuk satu surah. */
    data class SurahAudioInfo(
        val number: Int,
        val ayahFiles: Int,
        val ayahCount: Int,
        val wordFiles: Int,
        /** Total kata yang diharapkan; null kalau isi surah belum di-cache. */
        val totalWords: Int?,
        /** Total ukuran file terunduh (bytes). */
        val sizeBytes: Long,
    )

    /** Nomor surah-surah yang punya minimal satu file audio terunduh. */
    fun downloadedSurahNumbers(): List<Int> {
        val numbers = sortedSetOf<Int>()
        val dir = audioDir
        if (dir.exists()) {
            dir.listFiles { f -> f.isFile && f.extension == "mp3" && f.name.length == 7 }
                ?.forEach { f -> numbers += f.name.take(3).toIntOrNull() ?: return@forEach }
        }
        val wbw = File(dir, "wbw")
        if (wbw.exists()) {
            wbw.listFiles { f -> f.isFile && f.extension == "mp3" }
                ?.forEach { f ->
                    val parts = f.name.split("_")
                    if (parts.size == 3) numbers += parts[0].toIntOrNull() ?: return@forEach
                }
        }
        return numbers.toList()
    }

    /** Hitung file audio terunduh + ukurannya untuk satu surah. */
    fun surahAudioInfo(number: Int, ayahCount: Int, totalWords: Int?): SurahAudioInfo {
        var ayahFiles = 0
        var sizeBytes = 0L
        for (n in 1..ayahCount) {
            val f = ayahFile(number, n)
            if (f.exists() && f.length() > 0L) {
                ayahFiles++
                sizeBytes += f.length()
            }
        }
        val prefix = number.toString().padStart(3, '0') + "_"
        val wbwDir = File(audioDir, "wbw")
        var wordFiles = 0
        if (wbwDir.exists()) {
            wbwDir.listFiles { f -> f.isFile && f.name.startsWith(prefix) && f.extension == "mp3" }
                ?.forEach { f ->
                    wordFiles++
                    sizeBytes += f.length()
                }
        }
        return SurahAudioInfo(number, ayahFiles, ayahCount, wordFiles, totalWords, sizeBytes)
    }

    /** Hapus semua audio satu surah (file ayat + kata). */
    fun deleteSurahAudio(number: Int) {
        val prefix = number.toString().padStart(3, '0')
        val dir = audioDir
        if (dir.exists()) {
            dir.listFiles { f ->
                f.isFile && f.extension == "mp3" && f.name.length == 7 && f.name.startsWith(prefix)
            }?.forEach { it.delete() }
        }
        val wbw = File(dir, "wbw")
        if (wbw.exists()) {
            wbw.listFiles { f -> f.isFile && f.name.startsWith(prefix + "_") }?.forEach { it.delete() }
        }
    }

    /** Hapus SEMUA audio terunduh. */
    fun deleteAllAudio() {
        val dir = audioDir
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.deleteRecursively() }
        }
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
