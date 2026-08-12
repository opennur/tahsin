package com.tahsin.app.util

import android.content.Context
import com.google.gson.Gson
import com.tahsin.app.data.quran.QuranRepository
import java.time.LocalDate
import java.util.Random

/** Satu "Ayah of the Day": ayat + terjemahan untuk satu tanggal. */
data class AyahOfTheDay(
    val dateKey: String,        // yyyy-MM-dd — cache invalid saat tanggal berganti
    val surahNumber: Int,
    val ayahNumber: Int,        // 1-based
    val surahName: String,      // nama latin (mis. "Al-Fatihah")
    val arabic: String,
    val translation: String,
    val language: String,       // kode bahasa terjemahan (AppLanguage.code)
)

/**
 * "Ayah of the Day": pemilihan ayat deterministik per tanggal + cache konten.
 *
 * - Pemilihan MURNI & bisa di-unit-test: seed = hari epoch → index global
 *   [0, total ayat) → (surah, ayat) lewat daftar kumulatif. Stabil sepanjang
 *   hari, berubah otomatis besok.
 * - Konten dibaca dari bundel aset mushaf (offline), fallback unduh, lalu
 *   di-cache ke SharedPreferences supaya update widget/notifikasi instan.
 */
object AyahOfTheDayManager {

    private const val PREFS = "ayah_of_the_day"
    private const val KEY_CACHE = "cached"
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

    fun languageOf(context: Context): AppLanguage {
        val code = SettingsStore(context.applicationContext).languageCode
        return AppLanguage.entries.firstOrNull { it.code == code } ?: AppLanguage.ID
    }

    /** Konten ayah hari ini (cache kalau sudah dibuat hari ini). */
    fun cached(context: Context, date: LocalDate, lang: AppLanguage): AyahOfTheDay? {
        val json = prefs(context).getString(KEY_CACHE, null) ?: return null
        val ayah = runCatching { gson.fromJson(json, AyahOfTheDay::class.java) }.getOrNull()
            ?: return null
        return ayah.takeIf { it.dateKey == dateKey(date) && it.language == lang.code }
    }

    /** Muat konten ayah hari ini dari bundel aset (fallback: unduh), lalu cache. */
    suspend fun loadAndCache(context: Context, date: LocalDate, lang: AppLanguage): AyahOfTheDay? {
        val app = context.applicationContext
        val repository = QuranRepository(app)
        val surahs = runCatching { repository.surahList() }.getOrNull()?.sortedBy { it.number }
            ?: return null
        if (surahs.isEmpty()) return null
        val cumulative = cumulativeCounts(surahs.map { it.ayahCount })
        val (surahNumber, ayahNumber) = ayahRefForDate(date.toEpochDay(), cumulative)
        val meta = surahs.firstOrNull { it.number == surahNumber } ?: return null
        val surah = runCatching { repository.cachedSurah(surahNumber, lang) }.getOrNull()
            ?: runCatching { repository.fetchSurah(surahNumber, lang) }.getOrNull()
            ?: return null
        val ayah = surah.ayahs.getOrNull(ayahNumber - 1) ?: return null
        val result = AyahOfTheDay(
            dateKey = dateKey(date),
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            surahName = meta.nameLatin.ifBlank { meta.nameArabic },
            arabic = ayah.text,
            translation = ayah.translation,
            language = lang.code,
        )
        cache(app, result)
        return result
    }

    fun cache(context: Context, ayah: AyahOfTheDay) {
        prefs(context).edit().putString(KEY_CACHE, gson.toJson(ayah)).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
