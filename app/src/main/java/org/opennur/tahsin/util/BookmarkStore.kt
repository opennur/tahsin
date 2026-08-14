package org.opennur.tahsin.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/** Bookmark satu ayat mushaf. */
data class Bookmark(val surah: Int, val ayah: Int)

/**
 * Simpanan bookmark ayat (surah, ayah) di `filesDir/bookmarks.json` —
 * pola `ReadingStatsStore`: ctor murni (File) agar bisa dicakup 100%,
 * Gson untuk serialisasi, tulis atomik (tmp → rename).
 */
class BookmarkStore internal constructor(private val file: File) {

    companion object {
        /** Factory Android: file di filesDir — ctor tetap murni (File). */
        fun fromContext(context: Context): BookmarkStore =
            BookmarkStore(File(context.applicationContext.filesDir, "bookmarks.json"))
    }

    private val gson = Gson()
    private val type = object : TypeToken<List<Bookmark>>() {}.type

    /** Semua bookmark yang tersimpan (set: surah, ayah). */
    fun load(): Set<Bookmark> = synchronized(this) {
        readFromDisk().toSet()
    }

    /** Tambah kalau belum ada, hapus kalau sudah ada; kembalikan set terbaru. */
    fun toggle(bookmark: Bookmark): Set<Bookmark> = synchronized(this) {
        val current = readFromDisk().toMutableList()
        if (current.remove(bookmark)) {
            writeToDisk(current)
        } else {
            current.add(bookmark)
            writeToDisk(current)
        }
        current.toSet()
    }

    private fun readFromDisk(): List<Bookmark> = runCatching {
        if (!file.exists()) return emptyList()
        val parsed: List<Bookmark>? = gson.fromJson(file.readText(), type)
        // Hanya bookmark valid (surah & ayah > 0) yang dipakai.
        parsed?.filter { it.surah > 0 && it.ayah > 0 } ?: emptyList()
    }.getOrDefault(emptyList())

    private fun writeToDisk(bookmarks: List<Bookmark>) {
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(gson.toJson(bookmarks))
            tmp.renameTo(file)
        }
    }
}
