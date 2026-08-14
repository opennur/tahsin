package org.opennur.tahsin.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/** Satu entri riwayat baca: ayat yang pernah dibuka (terbaru di depan). */
data class ReadingHistoryEntry(
    val surah: Int,
    val ayah: Int,
    val timestamp: Long,
)

/**
 * Simpanan riwayat baca di `filesDir/reading_history.json` — pola
 * `ReadingStatsStore`/`BookmarkStore`: ctor murni (File) agar bisa dicakup
 * 100%, Gson untuk serialisasi, tulis atomik (tmp → rename). Maks
 * [MAX_ENTRIES]; mengunjungi ulang (surah, ayah) yang sama tidak menambah
 * duplikat — entri lama dipindah ke paling depan.
 */
class ReadingHistoryStore internal constructor(private val file: File) {

    companion object {
        /** Batas entri yang disimpan. */
        const val MAX_ENTRIES = 20

        /** Factory Android: file di filesDir — ctor tetap murni (File). */
        fun fromContext(context: Context): ReadingHistoryStore =
            ReadingHistoryStore(File(context.applicationContext.filesDir, "reading_history.json"))
    }

    private val gson = Gson()
    private val type = object : TypeToken<List<ReadingHistoryEntry>>() {}.type

    /** Riwayat terbaru dulu, dibatasi [MAX_ENTRIES]. */
    fun load(): List<ReadingHistoryEntry> = synchronized(this) {
        readFromDisk().sortedByDescending { it.timestamp }.take(MAX_ENTRIES)
    }

    /**
     * Catat kunjungan ayat: dedup (surah, ayah) → pindah ke paling depan,
     * potong ke [MAX_ENTRIES], tulis atomik; kembalikan daftar terbaru.
     */
    fun record(surah: Int, ayah: Int, now: Long): List<ReadingHistoryEntry> = synchronized(this) {
        val current = readFromDisk()
            .filterNot { it.surah == surah && it.ayah == ayah }
            .toMutableList()
        current.add(0, ReadingHistoryEntry(surah, ayah, now))
        val capped = current.take(MAX_ENTRIES)
        writeToDisk(capped)
        capped
    }

    private fun readFromDisk(): List<ReadingHistoryEntry> = runCatching {
        if (!file.exists()) return emptyList()
        val parsed: List<ReadingHistoryEntry>? = gson.fromJson(file.readText(), type)
        // Hanya entri valid (surah & ayah > 0) yang dipakai.
        parsed?.filter { it.surah > 0 && it.ayah > 0 } ?: emptyList()
    }.getOrDefault(emptyList())

    private fun writeToDisk(entries: List<ReadingHistoryEntry>) {
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(gson.toJson(entries))
            tmp.renameTo(file)
        }
    }
}
