package com.tahsin.app.data.vocab

import com.google.gson.Gson

/**
 * Parsing `vocab.json` — murni JVM, tanpa Context, bisa di-unit-test.
 * I/O (assets) ditangani [VocabularyRepository]; parser ini hanya JSON → model.
 *
 * DTO memakai field nullable: Gson tidak memanggil konstruktor (unsafe
 * allocation), jadi field yang hilang bernilai null/0 — mapping eksplisit
 * agar [VocabEntry] tidak pernah membawa null.
 */
object VocabularyParser {

    private val gson = Gson()

    /** Parse seluruh file kosa kata; JSON rusak/kosong → daftar kosong. */
    fun parse(json: String): List<VocabEntry> {
        val parsed = runCatching { gson.fromJson(json, VocabJson::class.java) }.getOrNull()
            ?: return emptyList()
        return parsed.entries.orEmpty().map { it.toEntry() }
    }

    private data class VocabJson(val entries: List<VocabEntryJson>? = null)

    private data class VocabEntryJson(
        val key: String? = null,
        val word: String? = null,
        val translit: String? = null,
        val meaningId: String? = null,
        val meaningEn: String? = null,
        val freq: Int = 0,
        val root: String? = null,
        val rootMeaningId: String? = null,
        val rootMeaningEn: String? = null,
        val example: VocabExampleJson? = null,
    ) {
        fun toEntry() = VocabEntry(
            key = key.orEmpty(),
            word = word.orEmpty(),
            translit = translit.orEmpty(),
            meaningId = meaningId.orEmpty(),
            meaningEn = meaningEn.orEmpty(),
            freq = freq,
            root = root.orEmpty(),
            rootMeaningId = rootMeaningId.orEmpty(),
            rootMeaningEn = rootMeaningEn.orEmpty(),
            example = example?.toExample() ?: VocabExample(0, 0, 0, "", "", "", ""),
        )
    }

    private data class VocabExampleJson(
        val surah: Int = 0,
        val ayah: Int = 0,
        val word: Int = 0,
        val ayahArab: String? = null,
        val ayahLatin: String? = null,
        val ayahId: String? = null,
        val ayahEn: String? = null,
    ) {
        fun toExample() = VocabExample(
            surah, ayah, word,
            ayahArab.orEmpty(), ayahLatin.orEmpty(), ayahId.orEmpty(), ayahEn.orEmpty(),
        )
    }
}
