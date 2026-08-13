package com.tahsin.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tahsin.app.stt.AlignedWord
import java.io.File

/**
 * Penyimpanan persisten riwayat bacaan per ayat (hasil TranscriptAligner).
 *
 * Format: satu file JSON di `filesDir/reading-stats.json` (list [AyahStats]).
 * Sengaja TANPA Room — konsisten dengan arsitektur proyek (Gson + filesDir,
 * seperti cache AudioManagerViewModel). Data kecil (hanya ayat yang pernah
 * dilatih) jadi selalu dibaca langsung dari disk (tanpa cache instance —
 * TahsinViewModel & StatsViewModel punya instance terpisah; cache malah
 * membuat hasil baru tidak terlihat). Penulisan atomik (tulis temp → rename)
 * supaya pembaca yang berjalan paralel tidak melihat file setengah jadi.
 */
class ReadingStatsStore internal constructor(private val file: File) {

    companion object {
        /** Factory Android: file di filesDir — ctor tetap murni (File) agar bisa dicakup 100%. */
        fun fromContext(context: Context): ReadingStatsStore =
            ReadingStatsStore(File(context.applicationContext.filesDir, "reading-stats.json"))
    }

    private val gson = Gson()
    private val listType = object : TypeToken<List<AyahStats>>() {}.type

    /**
     * Catat satu hasil bacaan final untuk satu ayat: gabungkan ke statistik
     * yang ada lalu tulis ke disk. Aman dipanggil dari thread mana pun.
     */
    fun record(surahNumber: Int, ayahNumber: Int, aligned: List<AlignedWord>, referenceWords: List<String>) {
        synchronized(this) {
            val all = readFromDisk().toMutableList()
            val idx = all.indexOfFirst { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
            val merged = ReadingStats.merge(
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                existing = all.getOrNull(idx),
                aligned = aligned,
                referenceWords = referenceWords,
            )
            if (idx >= 0) all[idx] = merged else all.add(merged)
            writeToDisk(all)
        }
    }

    /** Statistik satu ayat (null kalau belum pernah dilatih). */
    fun statsFor(surahNumber: Int, ayahNumber: Int): AyahStats? = synchronized(this) {
        readFromDisk().firstOrNull { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
    }

    /** Semua statistik (urut: surah, lalu ayat). */
    fun all(): List<AyahStats> = synchronized(this) {
        readFromDisk().sortedWith(compareBy<AyahStats> { it.surahNumber }.thenBy { it.ayahNumber })
    }

    /** Hapus seluruh riwayat bacaan. */
    fun clear() {
        synchronized(this) {
            runCatching { file.delete() }
        }
    }

    // ---- internal ----

    private fun readFromDisk(): List<AyahStats> {
        // Gagal parse → daftar kosong untuk pemanggilan ini saja (tidak di-cache),
        // jadi pembacaan berikutnya mencoba lagi (mis. saat file masih ditulis).
        return runCatching {
            if (!file.exists()) emptyList()
            else gson.fromJson<List<AyahStats>>(file.readText(), listType).orEmpty()
        }.getOrDefault(emptyList())
        // Pertahanan terhadap JSON lama/rusak: field List bisa null dari Gson.
            .map { it.copy(wordErrors = it.wordErrors.orEmpty()) }
    }

    /** Tulis atomik: file temp dulu, lalu rename (pembaca tidak lihat file paruh). */
    private fun writeToDisk(all: List<AyahStats>) {
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(gson.toJson(all))
            tmp.renameTo(file)
        }
    }
}
