package org.opennur.tahsin.data.vocab

import org.opennur.tahsin.util.AppLanguage

/**
 * Info akar kata untuk ditampilkan di tooltip mushaf.
 *
 * @param root Teks akar Arab (mis. "علم").
 * @param meaningId Arti konsep akar dalam bahasa Indonesia.
 * @param meaningEn Arti konsep akar dalam bahasa Inggris.
 * @param relatedWords Daftar kata lain dalam mushaf yang memiliki akar sama.
 */
data class RootInfo(
    val root: String,
    val meaningId: String,
    val meaningEn: String,
    val relatedWords: List<RelatedWord>,
)

/**
 * Satu kata yang berakar sama dengan kata yang diketuk.
 *
 * @param word Bentuk kata berharakat (mis. "عَلِيمٌ").
 * @param key Kunci normalisasi (tanpa harakat).
 * @param meaningId Arti kata dalam bahasa Indonesia.
 * @param meaningEn Arti kata dalam bahasa Inggris.
 * @param frequency Frekuensi kemunculan di seluruh mushaf.
 * @param example Contoh kemunculan di mushaf (surah, ayat).
 */
data class RelatedWord(
    val word: String,
    val key: String,
    val meaningId: String,
    val meaningEn: String,
    val frequency: Int,
    val exampleSurah: Int,
    val exampleAyah: Int,
)

/**
 * Mesin morfologi sederhana — lookup akar kata dan kata terkait.
 *
 * Menggunakan indeks terbalik: akar → daftar kata dengan akar yang sama.
 * Tidak memerlukan analisis morfologis penuh; mengandalkan data kurasi
 * dari `vocab.json` yang sudah memiliki field `root`.
 */
object MorphologyEngine {

    private var rootIndex: Map<String, List<VocabEntry>> = emptyMap()
    private var wordIndex: Map<String, VocabEntry> = emptyMap()

    /**
     * Inisialisasi indeks dari daftar entri kosa kata.
     * Harus dipanggil sekali saat aplikasi mulai (via [org.opennur.tahsin.di.AppModule]).
     */
    fun init(entries: List<VocabEntry>) {
        wordIndex = entries.associateBy { it.key }
        rootIndex = entries
            .filter { it.root.isNotBlank() }
            .groupBy { it.root }
    }

    /**
     * Lookup akar kata dari bentuk mushaf berharakat.
     *
     * @return [RootInfo] jika kata ditemukan di daftar kurasi dan punya akar,
     *         atau null jika kata tidak dikenal/tidak punya akar.
     */
    fun lookupRoot(word: String): RootInfo? {
        val key = VocabKey.normalize(word)
        if (key.isBlank()) return null

        val entry = wordIndex[key] ?: return null
        val root = entry.root.ifBlank { return null }

        val related = findRelatedWords(root, excludeKey = key)
        return RootInfo(
            root = root,
            meaningId = entry.rootMeaningId.ifBlank { entry.meaningId },
            meaningEn = entry.rootMeaningEn.ifBlank { entry.meaningEn },
            relatedWords = related,
        )
    }

    /**
     * Cari semua kata yang memiliki akar sama.
     *
     * @param root Teks akar Arab.
     * @param excludeKey Kunci yang dikecualikan (kata yang sedang diketuk).
     * @return Daftar kata terkait, diurutkan frekuensi menurun.
     */
    fun findRelatedWords(
        root: String,
        excludeKey: String = "",
    ): List<RelatedWord> {
        val entries = rootIndex[root] ?: return emptyList()
        return entries
            .filter { it.key != excludeKey }
            .sortedByDescending { it.freq }
            .map { entry ->
                RelatedWord(
                    word = entry.word,
                    key = entry.key,
                    meaningId = entry.meaningId,
                    meaningEn = entry.meaningEn,
                    frequency = entry.freq,
                    exampleSurah = entry.example.surah,
                    exampleAyah = entry.example.ayah,
                )
            }
    }

    /**
     * Jumlah entri yang terindeks (untuk testing/debug).
     */
    fun indexedEntryCount(): Int = wordIndex.size

    /**
     * Jumlah akar unik yang terindeks (untuk testing/debug).
     */
    fun indexedRootCount(): Int = rootIndex.size
}
