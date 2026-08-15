package org.opennur.tahsin.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.opennur.tahsin.data.quran.Surah
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap

/** HTTP 404/403 — file audio memang tidak ada di server (permanen). */
class AudioUnavailableException(message: String) : IOException(message)

/** Hasil unduhan audio. */
data class DownloadStats(
    val ok: Int,
    val total: Int,
)

/** Surah yang masih harus diselesaikan setelah proses aplikasi mati. */
data class PendingAudioDownload(
    val surahNumber: Int,
    val reciterSlug: String,
    val queuedAt: Long = System.currentTimeMillis(),
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

/** HTTP failure yang bersifat sementara dan aman untuk dicoba ulang. */
private class RetryableDownloadException(message: String) : IOException(message)

/** HTTP client error yang tidak akan membaik dengan retry otomatis. */
private class PermanentDownloadException(message: String) : IOException(message)

/** Jumlah koneksi unduhan paralel per surah. */
private const val PARALLELISM = 6
private const val MAX_DOWNLOAD_ATTEMPTS = 3
private const val RETRY_BACKOFF_MS = 500L

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
class AudioDownloader internal constructor(
    /** Akar folder audio: `filesDir/audio` (bisa di-inject untuk tes). */
    private val audioDir: File,
    /** Penyedia slug qari' aktif — dibaca LAZY tiap operasi (bisa ganti runtime). */
    private val slugProvider: () -> String,
    /** Factory koneksi agar protokol unduhan dapat diuji tanpa mengubah URL produksi. */
    private val connectionFactory: (String) -> HttpURLConnection = { url ->
        URL(url).openConnection() as HttpURLConnection
    },
    /** Jeda retry dapat diperkecil di tes; produksi memakai backoff eksponensial. */
    private val retryBackoffMs: Long = RETRY_BACKOFF_MS,
) {

    constructor(context: Context, settings: SettingsStore) : this(
        File(context.applicationContext.filesDir, "audio"),
        { settings.reciter.slug },
    )

    private val gson = Gson()

    /** Folder audio ayat qari' aktif: filesDir/audio/<slug>/. */
    private val reciterDir: File
        get() = File(audioDir, slugProvider())

    /** Registry missing: nama file (tanpa path) yang pernah 404 di server. */
    private val missingAyahsBySlug = mutableMapOf<String, MutableSet<String>>()
    private val missingWords = mutableSetOf<String>()
    private val missingLock = Any()
    /** Antrean persisten: file `.part` dipasangkan dengan surah di manifest ini. */
    private val pendingDownloads = mutableListOf<PendingAudioDownload>()
    private val pendingLock = Any()
    private val pendingType = object : TypeToken<List<PendingAudioDownload>>() {}.type
    /** Cegah dua coroutine menulis target MP3 yang sama secara bersamaan. */
    private val fileLocks = ConcurrentHashMap<String, Mutex>()

    init {
        loadMissingWords()
        // Migrasi sekali: file format lama (audio/missing.json) berisi ayat
        // Minshawy + kata — dipindah ke registry baru saat dibutuhkan.
        migrateLegacyMissing()
        loadPendingDownloads()
    }

    fun ayahFile(surah: Int, ayah: Int): File = File(reciterDir, AudioUrls.ayahKey(surah, ayah))

    fun wordFile(surah: Int, ayah: Int, wordIndex: Int): File =
        File(File(audioDir, "wbw"), AudioUrls.wordKey(surah, ayah, wordIndex))

    fun isAyahMissing(surah: Int, ayah: Int): Boolean =
        AudioUrls.ayahKey(surah, ayah) in missingAyahsFor(slugProvider())

    fun isWordMissing(surah: Int, ayah: Int, wordIndex: Int): Boolean =
        AudioUrls.wordKey(surah, ayah, wordIndex) in missingWords

    /** Snapshot antrean yang belum selesai, aman dibaca dari UI/service. */
    fun pendingDownloads(): List<PendingAudioDownload> = synchronized(pendingLock) {
        pendingDownloads.toList()
    }

    /** Bersihkan entri yang tersisa bila semua file selesai sebelum proses mati. */
    fun clearPendingDownload(surahNumber: Int, reciterSlug: String) {
        synchronized(pendingLock) {
            if (pendingDownloads.removeAll {
                    it.surahNumber == surahNumber && it.reciterSlug == reciterSlug
                }) {
                savePendingDownloadsLocked()
            }
        }
    }

    /** Pastikan audio ayat ada di cache; null kalau file memang tidak ada di server. */
    suspend fun ensureAyah(surah: Int, ayah: Int): File? = withContext(Dispatchers.IO) {
        // Snapshot slug SEKALI per operasi: URL, path, cek & catat missing harus
        // konsisten walau qari' diganti di tengah unduhan (race window).
        val slug = slugProvider()
        val key = AudioUrls.ayahKey(surah, ayah)
        val file = File(File(audioDir, slug), key)
        when {
            file.exists() && file.length() > 0L -> file
            key in missingAyahsFor(slug) -> null
            else -> try {
                downloadWithRetries(AudioUrls.ayahUrl(surah, ayah, Reciter.fromSlug(slug)), file)
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
                downloadWithRetries(AudioUrls.wordUrl(surah, ayah, wordIndex), file)
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
    ): DownloadStats {
        // Snapshot slug SEKALI per operasi (lihat catatan di ensureAyah).
        val slug = slugProvider()
        val pending = PendingAudioDownload(surah.number, slug)
        enqueuePending(pending)
        val stats = withContext(Dispatchers.IO) {
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
                        try {
                            semaphore.withPermit { processDownload(job, ok, slug) }
                            synchronized(progressLock) {
                                val d = done.incrementAndGet()
                                onProgress(d, total)
                            }
                        } catch (e: CancellationException) {
                            // Cancellation leaves the manifest and `.part` file intact.
                            throw e
                        } catch (_: Exception) {
                            // A single failed file must not cancel the rest of the surah.
                        }
                    }
                }
            }
            DownloadStats(ok.get(), total)
        }
        if (stats.ok == stats.total) completePending(pending)
        return stats
    }

    private suspend fun processDownload(job: DownloadJob, ok: AtomicInteger, slug: String) {
        if (job.file.exists() && job.file.length() > 0L) {
            ok.incrementAndGet()
            return
        }
        try {
            downloadWithRetries(job.url, job.file)
            ok.incrementAndGet()
        } catch (e: AudioUnavailableException) {
            if (job.isWord) recordMissingWord(job.file.name) else recordMissingAyah(job.file.name, slug)
        } catch (e: CancellationException) {
            throw e
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
        val missingA = missingAyahsFor(slugProvider()).count { it.startsWith(num3) }
        val missingW = missingWords.count { it.startsWith(num3 + "_") }
        return SurahAudioInfo(number, ayahFiles, ayahCount, wordFiles, totalWords, sizeBytes, missingA, missingW)
    }

    /** Hapus semua audio satu surah (file ayat qari' aktif + kata). */
    fun deleteSurahAudio(number: Int) {
        forgetPendingForSurah(number)
        val prefix = number.toString().padStart(3, '0')
        val dir = reciterDir
        if (dir.exists()) {
            dir.listFiles { f ->
                f.isFile && f.name.startsWith(prefix) &&
                    (AudioUrls.isAyahAudioFileName(f.name) || f.name.endsWith(".mp3.part"))
            }?.forEach { it.delete() }
        }
        val wbw = File(audioDir, "wbw")
        if (wbw.exists()) {
            wbw.listFiles { f ->
                f.isFile && f.name.startsWith(prefix + "_") &&
                    (f.extension == "mp3" || f.name.endsWith(".mp3.part"))
            }?.forEach { it.delete() }
        }
    }

    /** Hapus SEMUA audio terunduh (semua qari'). */
    fun deleteAllAudio() {
        synchronized(pendingLock) {
            pendingDownloads.clear()
            savePendingDownloadsLocked()
        }
        val dir = audioDir
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.deleteRecursively() }
        }
    }

    // ---- internal ----

    /**
     * Download one target with a per-file lock and exponential retry. The lock
     * also covers the final existence check, preventing a play request from
     * racing a bulk download for the same ayah.
     */
    private suspend fun downloadWithRetries(url: String, out: File): File {
        val lock = fileLocks.computeIfAbsent(out.absolutePath) { Mutex() }
        return lock.withLock {
            downloadWithRetriesLocked(url, out)
        }
    }

    @Suppress("ThrowsCount")
    private suspend fun downloadWithRetriesLocked(url: String, out: File): File {
        if (out.exists() && out.length() > 0L) return out
        var attempt = 1
        var lastError: IOException? = null
        while (attempt <= MAX_DOWNLOAD_ATTEMPTS) {
            try {
                return download(url, out)
            } catch (e: AudioUnavailableException) {
                throw e
            } catch (e: PermanentDownloadException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                lastError = e
                if (attempt == MAX_DOWNLOAD_ATTEMPTS) break
                delay(retryBackoffMs * (1L shl (attempt - 1)))
                attempt++
            }
        }
        throw lastError ?: IOException("Audio download failed")
    }

    /**
     * Download to `<target>.part`. A server that supports HTTP Range resumes a
     * partial file; a server that ignores Range safely restarts from byte zero.
     * The visible MP3 is created only by [promotePart] after validation.
     */
    @Suppress("ThrowsCount", "CyclomaticComplexMethod", "ComplexCondition")
    private fun download(url: String, out: File): File {
        out.parentFile?.mkdirs()
        val part = partFile(out)
        var offset = if (part.isFile) part.length() else 0L
        val conn = connectionFactory(url)
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        if (offset > 0L) conn.setRequestProperty("Range", "bytes=$offset-")
        try {
            conn.connect()
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND || code == 403) {
                throw AudioUnavailableException("HTTP $code")
            }
            if (code == 416) {
                // The remote object changed or the partial file is too long.
                // Delete only the temporary file; the next retry starts cleanly.
                part.delete()
                throw RetryableDownloadException("HTTP 416: invalid range")
            }
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                if (code == 408 || code == 425 || code == 429 || code in 500..599) {
                    throw RetryableDownloadException("HTTP $code")
                }
                throw PermanentDownloadException("HTTP $code")
            }

            val append = offset > 0L && code == HttpURLConnection.HTTP_PARTIAL
            if (append) validateContentRange(conn, offset)
            val expectedTotal = if (append) {
                contentRangeTotal(conn)
            } else {
                conn.contentLengthLong.takeIf { it >= 0L }
            }

            conn.inputStream.use { input ->
                FileOutputStream(part, append).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }

            val actualLength = part.length()
            if (actualLength <= 0L) throw IOException("Empty audio response")
            if (expectedTotal != null && actualLength != expectedTotal) {
                throw IOException("Incomplete audio: $actualLength/$expectedTotal bytes")
            }
            promotePart(part, out)
        } finally {
            conn.disconnect()
        }
        return out
    }

    private fun partFile(out: File): File = File(out.parentFile, "${out.name}.part")

    @Suppress("ThrowsCount")
    private fun validateContentRange(conn: HttpURLConnection, expectedStart: Long) {
        val header = conn.getHeaderField("Content-Range")
            ?: throw IOException("206 response has no Content-Range")
        val match = CONTENT_RANGE.matchEntire(header.trim())
            ?: throw IOException("Invalid Content-Range: $header")
        if (match.groupValues[1].toLong() != expectedStart) {
            throw IOException("Content-Range resumed at the wrong offset")
        }
    }

    private fun contentRangeTotal(conn: HttpURLConnection): Long? {
        val header = conn.getHeaderField("Content-Range") ?: return null
        val match = CONTENT_RANGE.matchEntire(header.trim()) ?: return null
        return match.groupValues[3].toLongOrNull()
    }

    /** Rename is the commit point: readers see either the old file or the new file. */
    private fun promotePart(part: File, out: File) {
        try {
            Files.move(part.toPath(), out.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            if (!part.renameTo(out)) {
                throw IOException("Could not atomically publish ${out.name}")
            }
        }
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
            atomicWrite(f, gson.toJson(MissingAudio(missingAyahsFor(slug).toList(), emptyList())))
        }
    }

    private fun saveMissingWords() {
        runCatching {
            val f = File(audioDir, "missing-words.json")
            f.parentFile?.mkdirs()
            atomicWrite(f, gson.toJson(MissingAudio(emptyList(), missingWords.toList())))
        }
    }

    // ---- persistent crash recovery queue ----

    private val pendingFile: File
        get() = File(audioDir, "pending-downloads.json")

    private fun loadPendingDownloads() {
        synchronized(pendingLock) {
            runCatching {
                if (pendingFile.exists()) {
                    val loaded = gson.fromJson<List<PendingAudioDownload>>(
                        pendingFile.readText(),
                        pendingType,
                    ).orEmpty()
                    pendingDownloads += loaded
                        .filter { it.surahNumber in 1..114 && it.reciterSlug.isNotBlank() }
                        .distinctBy { it.surahNumber to it.reciterSlug }
                }
            }
        }
    }

    private fun enqueuePending(item: PendingAudioDownload) {
        synchronized(pendingLock) {
            if (pendingDownloads.none { it.surahNumber == item.surahNumber && it.reciterSlug == item.reciterSlug }) {
                pendingDownloads += item
                savePendingDownloadsLocked()
            }
        }
    }

    private fun completePending(item: PendingAudioDownload) {
        synchronized(pendingLock) {
            if (pendingDownloads.removeAll {
                    it.surahNumber == item.surahNumber && it.reciterSlug == item.reciterSlug
                }) {
                savePendingDownloadsLocked()
            }
        }
    }

    private fun forgetPendingForSurah(number: Int) {
        synchronized(pendingLock) {
            if (pendingDownloads.removeAll { it.surahNumber == number }) savePendingDownloadsLocked()
        }
    }

    private fun savePendingDownloadsLocked() {
        runCatching {
            pendingFile.parentFile?.mkdirs()
            atomicWrite(pendingFile, gson.toJson(pendingDownloads))
        }
    }

    /** All cache metadata writes use the same temp-file commit protocol. */
    private fun atomicWrite(target: File, text: String) {
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.writeText(text)
        try {
            Files.move(
                temp.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: Exception) {
            if (!temp.renameTo(target)) throw IOException("Could not atomically write ${target.name}")
        }
    }

    private companion object {
        val CONTENT_RANGE = Regex("bytes (\\d+)-(\\d+)/(\\d+|\\*)")
    }
}
