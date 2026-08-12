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
 * semua MP3 ayat (qari' aktif, everyayah.com) + semua MP3 kata (quran.com).
 *
 * Audio ayat disimpan per qari' di `filesDir/audio/<slug>/` — ganti qari' =
 * ganti folder, tidak saling menimpa. Kata (wbw) tidak tergantung qari'.
 *
 * File yang ternyata 404 di server dicatat permanen ("missing") supaya tidak
 * diblokir selamanya dan tidak dicoba diunduh berulang-ulang. Registry missing
 * per qari' (`missing-<slug>.json`), kata global (`missing-words.json`);
 * `missing.json` lama (format awal, Minshawy) dimigrasi otomatis.
 */
class AudioDownloader(context: Context, private val settings: SettingsStore) {

    private val appContext = context.applicationContext
    private val gson = Gson()

    /** filesDir/audio — relatif sama dengan konvensi assets/audio. */
    val audioDir: File
        get() = File(appContext.filesDir, "audio")

    /** Folder audio ayat qari' aktif: filesDir/audio/<slug>/. */
    private val reciterDir: File
        get() = File(audioDir, settings.reciter.slug)

    /** Registry missing: nama file (tanpa path) yang pernah 404 di server. */
    private val missingAyahsBySlug = mutableMapOf<String, MutableSet<String>>()
    private val missingWords = mutableSetOf<String>()
    private val missingLock = Any()

    init {
        loadMissingWords()
        // Migrasi sekali: file format lama (audio/missing.json) berisi ayat
        // Minshawy + kata — dipindah ke registry baru saat dibutuhkan.
        migrateLegacyMissing()
    }

    fun ayahFile(surah: Int, ayah: Int): File = File(reciterDir, AudioUrls.ayahKey(surah, ayah))

    fun wordFile(surah: Int, ayah: Int, wordIndex: Int): File =
        File(File(audioDir, "wbw"), AudioUrls.wordKey(surah, ayah, wordIndex))

    fun isAyahMissing(surah: Int, ayah: Int): Boolean =
        AudioUrls.ayahKey(surah, ayah) in missingAyahsFor(settings.reciter.slug)

    fun isWordMissing(surah: Int, ayah: Int, wordIndex: Int): Boolean =
        AudioUrls.wordKey(surah, ayah, wordIndex) in missingWords

    /** Pastikan audio ayat ada di cache; null kalau file memang tidak ada di server. */
    suspend fun ensureAyah(surah: Int, ayah: Int): File? = withContext(Dispatchers.IO) {
        // Snapshot slug SEKALI per operasi: URL, path, cek & catat missing harus
        // konsisten walau qari' diganti di tengah unduhan (race window).
        val slug = settings.reciter.slug
        val key = AudioUrls.ayahKey(surah, ayah)
        val file = File(File(audioDir, slug), key)
        when {
            file.exists() && file.length() > 0L -> file
            key in missingAyahsFor(slug) -> null
            else -> try {
                download(AudioUrls.ayahUrl(surah, ayah, Reciter.fromSlug(slug)), file)
            } catch (e: AudioUnavailableException) {
                recordMissingAyah(file.name, slug)
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
        // Snapshot slug SEKALI per operasi (lihat catatan di ensureAyah).
        val slug = settings.reciter.slug
        val reciter = Reciter.fromSlug(slug)
        val slugDir = File(audioDir, slug)
        val jobs = mutableListOf<DownloadJob>()
        surah.ayahs.forEach { ayah ->
            if (AudioUrls.ayahKey(surah.number, ayah.number) !in missingAyahsFor(slug)) {
                jobs += DownloadJob(
                    AudioUrls.ayahUrl(surah.number, ayah.number, reciter),
                    File(slugDir, AudioUrls.ayahKey(surah.number, ayah.number)),
                    false,
                )
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
                        semaphore.withPermit { processDownload(job, ok, slug) }
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

    private fun processDownload(job: DownloadJob, ok: AtomicInteger, slug: String) {
        if (job.file.exists() && job.file.length() > 0L) {
            ok.incrementAndGet()
            return
        }
        try {
            download(job.url, job.file)
            ok.incrementAndGet()
        } catch (e: AudioUnavailableException) {
            if (job.isWord) recordMissingWord(job.file.name) else recordMissingAyah(job.file.name, slug)
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
        val dir = reciterDir
        if (dir.exists()) {
            dir.listFiles { f -> f.isFile && AudioUrls.isAyahAudioFileName(f.name) }
                ?.forEach { f -> numbers += f.name.take(3).toIntOrNull() ?: return@forEach }
        }
        val wbw = File(audioDir, "wbw")
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
        val dir = reciterDir
        var ayahFiles = 0
        var sizeBytes = 0L
        for (n in 1..ayahCount) {
            val f = File(dir, AudioUrls.ayahKey(number, n))
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
        val missingA = missingAyahsFor(settings.reciter.slug).count { it.startsWith(num3) }
        val missingW = missingWords.count { it.startsWith(num3 + "_") }
        return SurahAudioInfo(number, ayahFiles, ayahCount, wordFiles, totalWords, sizeBytes, missingA, missingW)
    }

    /** Hapus semua audio satu surah (file ayat qari' aktif + kata). */
    fun deleteSurahAudio(number: Int) {
        val prefix = number.toString().padStart(3, '0')
        val dir = reciterDir
        if (dir.exists()) {
            dir.listFiles { f ->
                f.isFile && AudioUrls.isAyahAudioFileName(f.name) && f.name.startsWith(prefix)
            }?.forEach { it.delete() }
        }
        val wbw = File(audioDir, "wbw")
        if (wbw.exists()) {
            wbw.listFiles { f -> f.isFile && f.name.startsWith(prefix + "_") }?.forEach { it.delete() }
        }
    }

    /** Hapus SEMUA audio terunduh (semua qari'). */
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

    /** Set missing ayat untuk satu slug — dimuat lazy (slug bisa berganti runtime). */
    private fun missingAyahsFor(slug: String): MutableSet<String> = synchronized(missingLock) {
        missingAyahsBySlug.getOrPut(slug) {
            val set = mutableSetOf<String>()
            runCatching {
                val f = File(audioDir, "missing-$slug.json")
                if (f.exists()) {
                    val data = gson.fromJson(f.readText(), MissingAudio::class.java) ?: return@runCatching
                    set += data.ayahs
                }
            }
            // Migrasi format lama: audio/missing.json berisi ayat Minshawy.
            if (slug == Reciter.MINSHAWY.slug) {
                runCatching {
                    val legacy = File(audioDir, "missing.json")
                    if (legacy.exists()) {
                        val data = gson.fromJson(legacy.readText(), MissingAudio::class.java) ?: return@runCatching
                        set += data.ayahs
                    }
                }
            }
            set
        }
    }

    private fun loadMissingWords() {
        runCatching {
            val f = File(audioDir, "missing-words.json")
            if (f.exists()) {
                val data = gson.fromJson(f.readText(), MissingAudio::class.java) ?: return@runCatching
                missingWords += data.words
            }
        }
    }

    /** Migrasi sekali: kata dari file format lama (audio/missing.json). */
    private fun migrateLegacyMissing() {
        synchronized(missingLock) {
            runCatching {
                val legacy = File(audioDir, "missing.json")
                if (!legacy.exists()) return@runCatching
                val data = gson.fromJson(legacy.readText(), MissingAudio::class.java) ?: return@runCatching
                if (data.words.isNotEmpty() && missingWords.isEmpty()) {
                    missingWords += data.words
                    saveMissingWords()
                }
            }
        }
    }

    private fun recordMissingAyah(fileName: String, slug: String) {
        synchronized(missingLock) {
            if (missingAyahsFor(slug).add(fileName)) saveMissingAyahs(slug)
        }
    }

    private fun recordMissingWord(fileName: String) {
        synchronized(missingLock) {
            if (missingWords.add(fileName)) saveMissingWords()
        }
    }

    private fun saveMissingAyahs(slug: String) {
        runCatching {
            val f = File(audioDir, "missing-$slug.json")
            f.parentFile?.mkdirs()
            f.writeText(gson.toJson(MissingAudio(missingAyahsFor(slug).toList(), emptyList())))
        }
    }

    private fun saveMissingWords() {
        runCatching {
            val f = File(audioDir, "missing-words.json")
            f.parentFile?.mkdirs()
            f.writeText(gson.toJson(MissingAudio(emptyList(), missingWords.toList())))
        }
    }
}
