package com.tahsin.app.util

import android.content.Context
import com.google.gson.Gson
import com.tahsin.app.data.quran.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

/** HTTP 404/403 — file audio memang tidak ada di server (permanen). */
class AudioUnavailableException(message: String) : IOException(message)

/** Hasil unduhan audio. */
data class DownloadStats(
    val ok: Int,
    val total: Int,
)

/** Rekam file yang ternyata tidak ada di server (persisten). */
private data class MissingAudio(
    val ayahs: List<String> = emptyList(),
    val words: List<String> = emptyList(),
)

/** Satu item unduhan: URL, file tujuan, dan apakah file kata (vs ayat). */
private data class DownloadJob(
    val url: String,
    val file: File,
    val isWord: Boolean,
)

/** Jumlah koneksi unduhan paralel per surah. */
private const val PARALLELISM = 6

/**
 * Mengunduh audio contoh ke penyimpanan internal (`filesDir/audio`) supaya
 * bisa diputar OFFLINE setelah diunduh sekali. Unduhan dilakukan PER SURAH:
 * semua MP3 ayat (Minshawy) + semua MP3 kata (quran.com) surah itu.
 *
 * File yang ternyata 404 di server dicatat permanen ("missing") supaya tidak
 * diblokir selamanya dan tidak dicoba diunduh berulang-ulang.
 */
class AudioDownloader(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val missingFile = File(appContext.filesDir, "audio/missing.json")

    /** filesDir/audio — relatif sama dengan konvensi assets/audio. */
    val audioDir: File
        get() = File(appContext.filesDir, "audio")

    /** Nama file (tanpa path) yang pernah 404 di server. */
    private val missingAyahs = mutableSetOf<String>()
    private val missingWords = mutableSetOf<String>()
    private val missingLock = Any()

    init {
        loadMissing()
    }

    fun ayahFile(surah: Int, ayah: Int): File = File(audioDir, AudioUrls.ayahKey(surah, ayah))

    fun wordFile(surah: Int, ayah: Int, wordIndex: Int): File =
        File(File(audioDir, "wbw"), AudioUrls.wordKey(surah, ayah, wordIndex))

    fun isAyahMissing(surah: Int, ayah: Int): Boolean = AudioUrls.ayahKey(surah, ayah) in missingAyahs

    fun isWordMissing(surah: Int, ayah: Int, wordIndex: Int): Boolean =
        AudioUrls.wordKey(surah, ayah, wordIndex) in missingWords

    /** Pastikan audio ayat ada di cache; null kalau file memang tidak ada di server. */
    suspend fun ensureAyah(surah: Int, ayah: Int): File? = withContext(Dispatchers.IO) {
        val file = ayahFile(surah, ayah)
        when {
            file.exists() && file.length() > 0L -> file
            isAyahMissing(surah, ayah) -> null
            else -> try {
                download(AudioUrls.ayahUrl(surah, ayah), file)
            } catch (e: AudioUnavailableException) {
                recordMissingAyah(file.name)
                null
            }
        }
    }

    /** Pastikan audio kata ada di cache; null kalau file memang tidak ada di server. */
    suspend fun ensureWord(surah: Int, ayah: Int, wordIndex: Int): File? = withContext(Dispatchers.IO) {
        val file = wordFile(surah, ayah, wordIndex)
        when {
            file.exists() && file.length() > 0L -> file
            isWordMissing(surah, ayah, wordIndex) -> null
            else -> try {
                download(AudioUrls.wordUrl(surah, ayah, wordIndex), file)
            } catch (e: AudioUnavailableException) {
                recordMissingWord(file.name)
                null
            }
        }
    }

    /** Semua audio surah (ayat + kata) sudah ada di cache / memang tidak tersedia? */
    fun isSurahAudioComplete(surah: Surah): Boolean {
        if (surah.ayahs.isEmpty()) return false
        surah.ayahs.forEach { ayah ->
            if (!isAyahMissing(surah.number, ayah.number)) {
                val af = ayahFile(surah.number, ayah.number)
                if (!(af.exists() && af.length() > 0L)) return false
            }
            ayah.words.forEachIndexed { wi, _ ->
                if (!isWordMissing(surah.number, ayah.number, wi)) {
                    val wf = wordFile(surah.number, ayah.number, wi)
                    if (!(wf.exists() && wf.length() > 0L)) return false
                }
            }
        }
        return true
    }

    /**
     * Unduh SEMUA audio satu surah (per ayat + per kata). File yang sudah ada
     * atau tercatat missing dilewati; kegagalan per-file (termasuk 404, yang
     * dicatat permanen) tidak menggagalkan sisanya.
     *
     * Diunduh PARALEL ([PARALLELISM] koneksi sekaligus) — jauh lebih cepat
     * daripada sekuensial untuk surah besar seperti Al-Baqarah (6000+ file).
     */
    suspend fun downloadSurah(
        surah: Surah,
        onProgress: (done: Int, total: Int) -> Unit,
    ): DownloadStats = withContext(Dispatchers.IO) {
        val jobs = mutableListOf<DownloadJob>()
        surah.ayahs.forEach { ayah ->
            if (!isAyahMissing(surah.number, ayah.number)) {
                jobs += DownloadJob(AudioUrls.ayahUrl(surah.number, ayah.number), ayahFile(surah.number, ayah.number), false)
            }
            ayah.words.forEachIndexed { wi, _ ->
                if (!isWordMissing(surah.number, ayah.number, wi)) {
                    jobs += DownloadJob(
                        AudioUrls.wordUrl(surah.number, ayah.number, wi),
                        wordFile(surah.number, ayah.number, wi),
                        true,
                    )
                }
            }
        }

        val total = jobs.size
        val done = AtomicInteger(0)
        val ok = AtomicInteger(0)
        val progressLock = Any()
        val semaphore = Semaphore(PARALLELISM)
        coroutineScope {
            jobs.forEach { job ->
                launch(Dispatchers.IO) {
                    // runCatching: exception apa pun dalam satu koneksi TIDAK
                    // boleh menggagalkan coroutine (exception tak tertangkap
                    // dalam coroutine = crash aplikasi).
                    runCatching {
                        semaphore.withPermit { processDownload(job, ok) }
                        synchronized(progressLock) {
                            val d = done.incrementAndGet()
                            onProgress(d, total)
                        }
                    }
                }
            }
        }
        DownloadStats(ok.get(), total)
    }

    private fun processDownload(job: DownloadJob, ok: AtomicInteger) {
        if (job.file.exists() && job.file.length() > 0L) {
            ok.incrementAndGet()
            return
        }
        try {
            download(job.url, job.file)
            ok.incrementAndGet()
        } catch (e: AudioUnavailableException) {
            if (job.isWord) recordMissingWord(job.file.name) else recordMissingAyah(job.file.name)
        } catch (e: Exception) {
            // error transien (timeout dll.) — biar dicoba lagi di kesempatan lain
        }
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
        /** Jumlah audio ayat yang memang tidak tersedia di server. */
        val missingAyahs: Int = 0,
        /** Jumlah audio kata yang memang tidak tersedia di server. */
        val missingWords: Int = 0,
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
        val num3 = number.toString().padStart(3, '0')
        val missingA = missingAyahs.count { it.length == 7 && it.startsWith(num3) }
        val missingW = missingWords.count { it.startsWith(num3 + "_") }
        return SurahAudioInfo(number, ayahFiles, ayahCount, wordFiles, totalWords, sizeBytes, missingA, missingW)
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

    // ---- internal ----

    private fun download(url: String, out: File): File {
        out.parentFile?.mkdirs()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        try {
            conn.connect()
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND || code == 403) {
                throw AudioUnavailableException("HTTP $code")
            }
            check(code == HttpURLConnection.HTTP_OK) { "HTTP $code" }
            conn.inputStream.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            conn.disconnect()
        }
        return out
    }

    private fun recordMissingAyah(fileName: String) {
        synchronized(missingLock) {
            if (missingAyahs.add(fileName)) saveMissing()
        }
    }

    private fun recordMissingWord(fileName: String) {
        synchronized(missingLock) {
            if (missingWords.add(fileName)) saveMissing()
        }
    }

    private fun loadMissing() {
        runCatching {
            if (!missingFile.exists()) return@runCatching
            val data = gson.fromJson(missingFile.readText(), MissingAudio::class.java) ?: return@runCatching
            missingAyahs += data.ayahs
            missingWords += data.words
        }
    }

    private fun saveMissing() {
        runCatching {
            missingFile.parentFile?.mkdirs()
            missingFile.writeText(gson.toJson(MissingAudio(missingAyahs.toList(), missingWords.toList())))
        }
    }
}
