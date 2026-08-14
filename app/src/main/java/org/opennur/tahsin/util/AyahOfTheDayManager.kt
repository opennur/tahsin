package org.opennur.tahsin.util

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import org.opennur.tahsin.data.quran.AssetQuranRepository
import org.opennur.tahsin.data.quran.QuranRepository
import java.time.LocalDate

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
 * "Ayah of the Day" — lapisan Android (Preferences DataStore + QuranRepository).
 *
 * SELURUH logika (pemilihan deterministik, validasi cache, perakitan konten)
 * ada di [AyahOfTheDayPicker] yang murni dan diuji unit 100%; kelas ini hanya
 * jembatan ke Android. Kini bisa diuji dengan Robolectric (bukan lagi
 * butuh emulator) — lihat AyahOfTheDayManagerTest.
 */
object AyahOfTheDayManager {

    private val KEY_CACHE = stringPreferencesKey("cached")
    private val gson = Gson()

    private fun store(context: Context): PreferencesStore =
        DataStores.preferences(context, "ayah_of_the_day", legacySharedPrefsName = "ayah_of_the_day")

    fun languageOf(context: Context): AppLanguage {
        val code = SettingsStore(context.applicationContext).languageCode
        return AppLanguage.entries.firstOrNull { it.code == code } ?: AppLanguage.ID
    }

    /** Konten ayah hari ini dari cache (null kalau basi/rusak/belum ada). */
    fun cached(context: Context, date: LocalDate, lang: AppLanguage): AyahOfTheDay? {
        val json = store(context).current[KEY_CACHE]
        return AyahOfTheDayPicker.cachedFrom(json, date, lang)
    }

    /** Muat konten ayah hari ini dari bundel aset (fallback: unduh), lalu cache. */
    suspend fun loadAndCache(context: Context, date: LocalDate, lang: AppLanguage): AyahOfTheDay? {
        val app = context.applicationContext
        val repository = AssetQuranRepository(app)
        val surahs = runCatching { repository.surahList() }.getOrNull()?.sortedBy { it.number }
            ?: return null
        val result = AyahOfTheDayPicker.contentOf(surahs, { number ->
            runCatching { repository.cachedSurah(number, lang) }.getOrNull()
                ?: runCatching { repository.fetchSurah(number, lang) }.getOrNull()
        }, date, lang) ?: return null
        cache(app, result)
        return result
    }

    fun cache(context: Context, ayah: AyahOfTheDay) {
        store(context).edit { this[KEY_CACHE] = gson.toJson(ayah) }
    }
}
