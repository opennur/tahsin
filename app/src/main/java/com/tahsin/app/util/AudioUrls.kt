package com.tahsin.app.util

/**
 * URL & nama file audio contoh:
 * - per AYAT: Minshawy Murattal (everyayah.com)
 * - per KATA: word-by-word (qurancdn.com)
 *
 * Nama file konsisten antara cache `filesDir/audio` dan konvensi assets.
 */
object AudioUrls {

    private fun key3(n: Int): String = n.toString().padStart(3, '0')

    /** Nama file audio ayat, mis. "001001.mp3". */
    fun ayahKey(surah: Int, ayah: Int): String = "${key3(surah)}${key3(ayah)}.mp3"

    /** Nama file audio kata, mis. "001_001_003.mp3". */
    fun wordKey(surah: Int, ayah: Int, wordIndex: Int): String =
        "${key3(surah)}_${key3(ayah)}_${key3(wordIndex + 1)}.mp3"

    /** URL MP3 Minshawy Murattal per ayat. */
    fun ayahUrl(surah: Int, ayah: Int): String =
        "https://everyayah.com/data/Minshawy_Murattal_128kbps/${key3(surah)}${key3(ayah)}.mp3"

    /** URL MP3 word-by-word (quran.com). */
    fun wordUrl(surah: Int, ayah: Int, wordIndex: Int): String =
        "https://audio.qurancdn.com/wbw/${key3(surah)}_${key3(ayah)}_${key3(wordIndex + 1)}.mp3"
}
