package org.opennur.tahsin.util

/**
 * URL & nama file audio contoh:
 * - per AYAT: everyayah.com (qari' pilihan — default Minshawy Murattal)
 * - per KATA: word-by-word (qurancdn.com, tidak tergantung qari')
 *
 * Nama file konsisten antara cache `filesDir/audio` dan konvensi assets.
 * Audio ayat disimpan per qari' di `filesDir/audio/<slug>/` supaya qari' yang
 * berbeda tidak saling menimpa.
 */
object AudioUrls {

    private fun key3(n: Int): String = n.toString().padStart(3, '0')

    /** Nama file audio ayat, mis. "001001.mp3". */
    fun ayahKey(surah: Int, ayah: Int): String = "${key3(surah)}${key3(ayah)}.mp3"

    /** Nama file audio kata, mis. "001_001_003.mp3". */
    fun wordKey(surah: Int, ayah: Int, wordIndex: Int): String =
        "${key3(surah)}_${key3(ayah)}_${key3(wordIndex + 1)}.mp3"

    /** Apakah nama file ini audio ayat (mis. "001001.mp3" — 6 digit + .mp3)? */
    fun isAyahAudioFileName(name: String): Boolean {
        val base = name.substringBeforeLast('.')
        return name.endsWith(".mp3") && base.length == 6 && base.all { it.isDigit() }
    }

    /** URL MP3 ayat per qari' (everyayah.com, pola konsisten: tinggal ganti folder). */
    fun ayahUrl(surah: Int, ayah: Int, reciter: Reciter = Reciter.MINSHAWY): String =
        "https://everyayah.com/data/${reciter.slug}/${key3(surah)}${key3(ayah)}.mp3"

    /** URL MP3 word-by-word (quran.com). */
    fun wordUrl(surah: Int, ayah: Int, wordIndex: Int): String =
        "https://audio.qurancdn.com/wbw/${key3(surah)}_${key3(ayah)}_${key3(wordIndex + 1)}.mp3"
}
