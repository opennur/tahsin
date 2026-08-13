package com.tahsin.app.util

import com.google.gson.Gson
import com.tahsin.app.data.quran.Surah
import java.time.LocalDate
import java.util.Random

/**
 * Pemilihan & perakitan konten "Ayah of the Day" — MURNI, tanpa Android,
 * bisa diuji unit 100% (ini bagian yang menentukan ayah mana tampil, jadi
 * harus bebas dari kesalahan).
 *
 * [AyahOfTheDayManager] (glue Android: SharedPreferences + QuranRepository)
 * mendelegasikan ke sini.
 */
object AyahOfTheDayPicker {

    private val gson = Gson()

    fun dateKey(date: LocalDate): String = date.toString() // yyyy-MM-dd

    /** Prefix sum jumlah ayat per surah, mis. [7, 293, ...] — elemen terakhir = total ayat. */
    fun cumulativeCounts(ayahCounts: List<Int>): List<Int> {
        val out = ArrayList<Int>(ayahCounts.size)
        var acc = 0
        for (c in ayahCounts) {
            acc += c
            out += acc
        }
        return out
    }

    /** Index ayat global (0-based) → (nomor surah 1-based, nomor ayat 1-based). */
    fun refForIndex(index: Int, cumulativeAyahCounts: List<Int>): Pair<Int, Int> {
        require(cumulativeAyahCounts.isNotEmpty()) { "Daftar kumulatif kosong" }
        require(index in 0 until cumulativeAyahCounts.last()) { "Index di luar jangkauan: $index" }
        val surahIdx = cumulativeAyahCounts.indexOfFirst { it > index }
        val before = if (surahIdx == 0) 0 else cumulativeAyahCounts[surahIdx - 1]
        return (surahIdx + 1) to (index - before + 1)
    }

    /**
     * Ayah untuk tanggal: deterministik (seed = hari epoch), jadi semua
     * perangkat melihat ayat yang sama sepanjang hari itu.
     */
    fun ayahRefForDate(epochDay: Long, cumulativeAyahCounts: List<Int>): Pair<Int, Int> {
        val index = Random(epochDay).nextInt(cumulativeAyahCounts.last())
        return refForIndex(index, cumulativeAyahCounts)
    }

    /**
     * Validasi cache: JSON null/rusak, tanggal beda, atau bahasa beda → null
     * (cache dianggap basi). Logika murni — pemanggil menyuplai raw JSON.
     */
    fun cachedFrom(json: String?, date: LocalDate, lang: AppLanguage): AyahOfTheDay? {
        if (json == null) return null
        val ayah = runCatching { gson.fromJson(json, AyahOfTheDay::class.java) }.getOrNull()
            ?: return null
        return ayah.takeIf { it.dateKey == dateKey(date) && it.language == lang.code }
    }

    /**
     * Rakit konten hari ini dari daftar surah + pemuat konten surah.
     * Murni: pemilihan deterministik (seed tanggal) → lookup meta → lookup
     * ayat → perakitan [AyahOfTheDay]. `surahContent` adalah fungsi injeksi
     * (glue jaringan/aset di pemanggil) supaya seluruh logika bisa diuji.
     */
    suspend fun contentOf(
        surahs: List<Surah>,
        surahContent: suspend (Int) -> Surah?,
        date: LocalDate,
        lang: AppLanguage,
    ): AyahOfTheDay? {
        if (surahs.isEmpty()) return null
        val cumulative = cumulativeCounts(surahs.map { it.ayahCount })
        val (surahNumber, ayahNumber) = ayahRefForDate(date.toEpochDay(), cumulative)
        // Pertahanan: daftar surah rusak/parsial (mis. ada gap nomor) → null,
        // bukan crash — "Ayah of the Day" harus tetap aman.
        val meta = surahs.firstOrNull { it.number == surahNumber } ?: return null
        val surah = surahContent(surahNumber) ?: return null
        val ayah = surah.ayahs.getOrNull(ayahNumber - 1) ?: return null
        return AyahOfTheDay(
            dateKey = dateKey(date),
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            surahName = meta.nameLatin.ifBlank { meta.nameArabic },
            arabic = ayah.text,
            translation = ayah.translation,
            language = lang.code,
        )
    }
}
