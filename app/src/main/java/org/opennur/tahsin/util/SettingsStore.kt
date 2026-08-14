package org.opennur.tahsin.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Subset setelan yang dipakai ViewModel yang diuji — interface kecil supaya
 * unit test JVM bisa menyuntikkan fake tanpa Android (implementasi nyata:
 * [SettingsStore]).
 */
interface SettingsSource {
    /** Kode bahasa aktif ("id" / "en"). */
    val languageCode: String
}

/**
 * Penyimpanan preferensi pengguna (Preferences DataStore).
 *
 * API properti SINKRON dipertahankan (pemanggil tidak berubah) via
 * [PreferencesStore] — baca dari cache yang di-prime saat konstruksi, tulis
 * update cache + persist async. Lihat [PreferencesStore] untuk detail.
 */
class SettingsStore(context: Context) : SettingsSource {

    private val store = DataStores.preferences(
        context,
        "tahsin_settings",
        legacySharedPrefsName = "tahsin_settings",
    )
    private val prefs: Preferences
        get() = store.current

    var darkMode: Boolean
        get() = prefs[Keys.DARK_MODE] ?: false
        set(value) = store.edit { this[Keys.DARK_MODE] = value }

    /** Petunjuk geser sudah ditutup user (jangan tampil lagi). */
    var swipeHintDismissed: Boolean
        get() = prefs[Keys.SWIPE_HINT_DISMISSED] ?: false
        set(value) = store.edit { this[Keys.SWIPE_HINT_DISMISSED] = value }

    /** Pewarnaan huruf tajwid di mushaf (default nyala). */
    var tajwidColor: Boolean
        get() = prefs[Keys.TAJWID_COLOR] ?: true
        set(value) = store.edit { this[Keys.TAJWID_COLOR] = value }

    /** Tampilkan terjemahan di mushaf (default MATI — mushaf asli). */
    var showTranslation: Boolean
        get() = prefs[Keys.SHOW_TRANSLATION] ?: false
        set(value) = store.edit { this[Keys.SHOW_TRANSLATION] = value }

    /** Bahasa aplikasi & terjemahan (default Indonesia). */
    override var languageCode: String
        get() = prefs[Keys.LANGUAGE_CODE] ?: AppLanguage.ID.code
        set(value) = store.edit { this[Keys.LANGUAGE_CODE] = value }

    /** Mode pemutaran audio mushaf: nama enum AudioPlaybackMode (single/continuous/repeat). */
    var audioMode: String
        get() = prefs[Keys.AUDIO_MODE] ?: "AYAH"
        set(value) = store.edit { this[Keys.AUDIO_MODE] = value }

    /**
     * Izin pengguna untuk unduhan latar belakang (foreground service).
     * null = belum pernah ditanya; true/false = keputusan tersimpan.
     */
    var backgroundDownloadAllowed: Boolean?
        get() = prefs[Keys.BG_DOWNLOAD]
        set(value) = store.edit {
            if (value == null) remove(Keys.BG_DOWNLOAD) else this[Keys.BG_DOWNLOAD] = value
        }

    /** Surah & ayat terakhir yang dibuka (di-restore saat startup). */
    var surahNumber: Int
        get() = prefs[Keys.SURAH_NUMBER] ?: 1
        set(value) = store.edit { this[Keys.SURAH_NUMBER] = value }

    var ayahIndex: Int
        get() = prefs[Keys.AYAH_INDEX] ?: 0
        set(value) = store.edit { this[Keys.AYAH_INDEX] = value }

    /** Notifikasi harian "Ayah of the Day" (default nyala). */
    var ayahOfDayEnabled: Boolean
        get() = prefs[Keys.AYAH_OF_DAY_ENABLED] ?: true
        set(value) = store.edit { this[Keys.AYAH_OF_DAY_ENABLED] = value }

    /** Pengingat harian untuk menjaga streak (default mati — opsional). */
    var streakReminderEnabled: Boolean
        get() = prefs[Keys.STREAK_REMINDER_ENABLED] ?: false
        set(value) = store.edit { this[Keys.STREAK_REMINDER_ENABLED] = value }

    /** Qari' (perawi) audio ayat aktif (default Minshawy Murattal). */
    var reciterSlug: String
        get() = prefs[Keys.RECITER_SLUG] ?: Reciter.MINSHAWY.slug
        set(value) = store.edit { this[Keys.RECITER_SLUG] = value }

    /** Qari' aktif sebagai [Reciter] (fallback Minshawy kalau slug tak dikenal). */
    val reciter: Reciter
        get() = Reciter.fromSlug(reciterSlug)

    /** Kecepatan pemutaran audio (0.5×–1.25×; default 1.0×). */
    var audioSpeed: Float
        get() = AudioSpeeds.clamp(prefs[Keys.AUDIO_SPEED] ?: 1.0f)
        set(value) = store.edit { this[Keys.AUDIO_SPEED] = AudioSpeeds.clamp(value) }

    /**
     * Ukuran huruf mushaf halaman (pengali teks). Rentang 1.0–2.5, default 1.5
     * (tampilan bawaan layar Tahsin). Dipakai tombol A− / A+ di layar Tahsin.
     */
    var fontScale: Float
        get() = FontScales.clamp(prefs[Keys.FONT_SCALE] ?: 1.5f)
        set(value) = store.edit { this[Keys.FONT_SCALE] = FontScales.clamp(value) }

    /** Nama key DataStore (sama dengan key SharedPreferences lama — migrasi 1:1). */
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SWIPE_HINT_DISMISSED = booleanPreferencesKey("swipe_hint_dismissed")
        val TAJWID_COLOR = booleanPreferencesKey("tajwid_color")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val AUDIO_MODE = stringPreferencesKey("audio_mode")
        val BG_DOWNLOAD = booleanPreferencesKey("bg_download")
        val SURAH_NUMBER = intPreferencesKey("surah_number")
        val AYAH_INDEX = intPreferencesKey("ayah_index")
        val AYAH_OF_DAY_ENABLED = booleanPreferencesKey("ayah_of_day_enabled")
        val STREAK_REMINDER_ENABLED = booleanPreferencesKey("streak_reminder_enabled")
        val RECITER_SLUG = stringPreferencesKey("reciter_slug")
        val AUDIO_SPEED = floatPreferencesKey("audio_speed")
        val FONT_SCALE = floatPreferencesKey("font_scale")
    }
}

/** Rentang ukuran huruf mushaf (A− / A+). */
object FontScales {
    const val MIN = 1.0f
    const val MAX = 2.5f
    const val STEP = 0.25f

    fun clamp(value: Float): Float = value.coerceIn(MIN, MAX)
}
