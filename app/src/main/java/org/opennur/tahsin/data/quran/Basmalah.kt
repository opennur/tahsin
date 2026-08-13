package org.opennur.tahsin.data.quran

/**
 * Aturan basmalah mushaf Madani (murni, di-unit-test):
 * - Setiap surah diawali basmalah KECUALI At-Tawbah (surah 9).
 * - Al-Fatihah: ayat 1-nya ADALAH basmalah — jadi ornamen basmalah tidak
 *   ditambahkan (komposer halaman memperlakukan ayat 1 surah 1 sebagai basmalah).
 */
object Basmalah {

    /** Teks basmalah — sama persis dengan ayat 1 Al-Fatihah di bundel. */
    const val TEXT = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ"

    /** Apakah surah memakai basmalah (semua kecuali At-Tawbah 9). */
    fun hasBasmalah(surah: Int): Boolean = surah != 9

    /** Apakah basmalah perlu dirender sebagai ornamen terpisah (bukan surah 9,
     *  dan bukan Al-Fatihah yang ayat 1-nya sudah basmalah). */
    fun needsBasmalahOrnament(surah: Int): Boolean = surah != 1 && surah != 9
}
