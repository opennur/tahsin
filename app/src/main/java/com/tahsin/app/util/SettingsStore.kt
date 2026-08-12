package com.tahsin.app.util

import android.content.Context

/** Penyimpanan sederhana preferensi pengguna (SharedPreferences). */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tahsin_settings", Context.MODE_PRIVATE)

    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    /** Petunjuk geser sudah ditutup user (jangan tampil lagi). */
    var swipeHintDismissed: Boolean
        get() = prefs.getBoolean("swipe_hint_dismissed", false)
        set(value) = prefs.edit().putBoolean("swipe_hint_dismissed", value).apply()

    /** Pewarnaan huruf tajwid di mushaf (default nyala). */
    var tajwidColor: Boolean
        get() = prefs.getBoolean("tajwid_color", true)
        set(value) = prefs.edit().putBoolean("tajwid_color", value).apply()

    /** Bahasa aplikasi & terjemahan (default Indonesia). */
    var languageCode: String
        get() = prefs.getString("language_code", AppLanguage.ID.code) ?: AppLanguage.ID.code
        set(value) = prefs.edit().putString("language_code", value).apply()

    /** Mode flow (muroja'ah): lanjut otomatis ke ayat berikutnya (default mati). */
    var flowMode: Boolean
        get() = prefs.getBoolean("flow_mode", false)
        set(value) = prefs.edit().putBoolean("flow_mode", value).apply()

    /**
     * Izin pengguna untuk unduhan latar belakang (foreground service).
     * null = belum pernah ditanya; true/false = keputusan tersimpan.
     */
    var backgroundDownloadAllowed: Boolean?
        get() = if (prefs.contains("bg_download")) prefs.getBoolean("bg_download", false) else null
        set(value) {
            val e = prefs.edit()
            if (value == null) e.remove("bg_download") else e.putBoolean("bg_download", value)
            e.apply()
        }

    /** Surah & ayat terakhir yang dibuka (di-restore saat startup). */
    var surahNumber: Int
        get() = prefs.getInt("surah_number", 1)
        set(value) = prefs.edit().putInt("surah_number", value).apply()

    var ayahIndex: Int
        get() = prefs.getInt("ayah_index", 0)
        set(value) = prefs.edit().putInt("ayah_index", value).apply()
}
