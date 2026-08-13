package org.opennur.tahsin.data.quran

/**
 * Konversi nomor ayat ke angka Arab-Indik (٠١٢٣٤٥٦٧٨٩) + penanda akhir ayat
 * mushaf: U+06DD (ARABIC END OF AYAH) diikuti nomor — font Amiri merendernya
 * sebagai lingkaran hias berisi nomor, persis mushaf Madani.
 *
 * Murni JVM, tanpa Android — di-unit-test.
 */
object AyahNumbering {

    private const val ARABIC_INDIC = "\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669" // ٠١٢٣٤٥٦٧٨٩

    /** Angka Latin → angka Arab-Indik ("286" → "٢٨٦"). */
    fun toArabicIndic(number: Int): String {
        require(number >= 1) { "Nomor ayat harus ≥ 1, dapat $number" }
        return number.toString().map { ARABIC_INDIC[it - '0'] }.joinToString("")
    }

    /** Penanda akhir ayat mushaf: ۝ + nomor Arab-Indik. */
    fun endOfAyahMarker(number: Int): String = "\u06DD" + toArabicIndic(number)
}
