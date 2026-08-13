package org.opennur.tahsin.data.vocab

/**
 * Satu entri kosa kata Al-Qur'an (hasil kurasi `tools/curate_vocab.py`).
 * `key` = kata ternormalisasi ([VocabKey]); `word` = bentuk mushaf
 * berharakat dari contoh kemunculan pertama.
 */
data class VocabEntry(
    val key: String,
    val word: String,
    val translit: String,
    val meaningId: String,
    val meaningEn: String,
    val freq: Int,
    /** Akar kata (keluarga) — lihat `tools/vocab_roots.py`. */
    val root: String = "",
    /** Arti konsep akar (ID/EN) — override atau turunan anggota terfrequent. */
    val rootMeaningId: String = "",
    val rootMeaningEn: String = "",
    val example: VocabExample = VocabExample(0, 0, 0, "", "", "", ""),
)

/** Contoh kemunculan kata di mushaf (surah, ayat, indeks kata 1-based). */
data class VocabExample(
    val surah: Int,
    val ayah: Int,
    val word: Int,
    val ayahArab: String,
    val ayahLatin: String,
    val ayahId: String,
    val ayahEn: String,
)

/**
 * Normalisasi kunci kosa kata — MIRROR `tools/build_vocab.py`.
 *
 * Sengaja TERPISAH dari [org.opennur.tahsin.util.ArabicNormalizer]: skrip build
 * me-strip rentang \u064B-\u0656 (termasuk maddah 0653, hamza atas/bawah
 * 0654/0655, subscript alef 0656) supaya varian "ما" / "مآ" menjadi satu
 * kunci. Kalau normalisasi ini diubah, data harus di-regenerasi.
 */
object VocabKey {

    private val MARKS = Regex("""[\u064B-\u0656\u0670\u06D6-\u06ED\u08D6-\u08ED]""")

    fun normalize(word: String): String {
        var s = MARKS.replace(word, "")
        s = s.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
        s = s.replace('ى', 'ي')
        s = s.replace('ة', 'ه')
        s = s.replace("ء", "")
        s = s.replace("ـ", "")
        return s.trim()
    }
}
