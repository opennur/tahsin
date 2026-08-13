package org.opennur.tahsin.data.vocab

import android.content.Context

/**
 * Sumber data kosa kata: `assets/quran/vocab.json` (offline, dihasilkan
 * `tools/build_vocab.py` + `tools/curate_vocab.py`, di-bundle ke APK).
 *
 * Hanya entri yang SUDAH dikurasi (arti ID/EN tidak kosong) yang tampil —
 * entri skeleton yang belum diisi arti disaring di sini.
 */
class VocabularyRepository(context: Context) {

    private val appContext = context.applicationContext

    @Volatile
    private var cached: List<VocabEntry>? = null

    /** Semua entri terkurasi, urut frekuensi menurun (urutan file). */
    fun curatedEntries(): List<VocabEntry> = cached ?: synchronized(this) {
        cached ?: run {
            val json = runCatching {
                appContext.assets.open("quran/vocab.json")
                    .bufferedReader()
                    .use { it.readText() }
            }.getOrNull()
            val list = if (json == null) emptyList()
            else VocabularyParser.parse(json).filter { it.meaningId.isNotBlank() }
            cached = list
            list
        }
    }

    /** Cari entri berdasarkan kunci ternormalisasi (lihat [VocabKey]). */
    fun lookup(key: String): VocabEntry? = curatedEntries().firstOrNull { it.key == key }
}
