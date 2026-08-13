package org.opennur.tahsin.util

/**
 * Utilitas teks Arab: normalisasi untuk pencocokan STT + tokenisasi kata ayat.
 */
object ArabicNormalizer {

    /**
     * Harakat & tanda mushaf yang dibuang saat normalisasi/tokenisasi:
     * - \u064B-\u0652 : harakat, tanwin, shaddah, sukun
     * - \u0670       : alif khanjariah (dagger alif)
     * - \u06D6-\u06ED : tanda mushaf (waqaf, maddah \u06E4, dll.)
     * - \u08D6-\u08ED : tanda mushaf extended (mis. ࣖ akhir ayat)
     */
    private val MARKS = Regex("""[\u064B-\u0652\u0670\u06D6-\u06ED\u08D6-\u08ED]""")

    /**
     * Huruf dasar Arab (konsonan + vokal panjang), termasuk hamza (ء) — hamza
     * adalah huruf halqi dan diperlukan deteksi mad wajib muttasil & izhar halqi.
     * ە (U+06D5) sengaja TIDAK masuk.
     */
    private val LETTERS = "ابتثجحخدذرزسشصضطظعغفقكلمنهويئةءأآإى".toSet()

    /** Apakah karakter ini huruf dasar Al-Quran. */
    fun isLetter(c: Char): Boolean = c in LETTERS

    /** Buang harakat & tanda mushaf. */
    fun stripMarks(text: String): String = MARKS.replace(text, "")

    /**
     * Normalisasi penuh untuk pencocokan: buang tanda, samakan varian huruf,
     * ratakan spasi. STT bahasa Arab umumnya meleset pada hamza/ta marbuta,
     * jadi bentuk-bentuk itu diseragamkan.
     */
    fun normalize(text: String): String {
        var s = stripMarks(text)
        s = s.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
        s = s.replace('ى', 'ي')
        s = s.replace('ة', 'ه')
        s = s.replace("ء", "")    // hamza sering dihilangkan STT
        s = s.replace("ـ", "")    // tatweel
        return s.trim()
    }

    /** Pecah ayat menjadi kata-kata (buang penanda waqaf / token non-huruf). */
    fun splitWords(text: String): List<String> {
        return text.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { token -> token.isNotBlank() && token.any { isLetter(it) } }
    }
}
