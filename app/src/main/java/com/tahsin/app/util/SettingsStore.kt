package com.tahsin.app.util

import android.content.Context

/** Penyimpanan sederhana preferensi pengguna (SharedPreferences). */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tahsin_settings", Context.MODE_PRIVATE)

    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    /** Pewarnaan huruf tajwid di mushaf (default nyala). */
    var tajwidColor: Boolean
        get() = prefs.getBoolean("tajwid_color", true)
        set(value) = prefs.edit().putBoolean("tajwid_color", value).apply()

    /** Mode flow (muroja'ah): lanjut otomatis ke ayat berikutnya (default mati). */
    var flowMode: Boolean
        get() = prefs.getBoolean("flow_mode", false)
        set(value) = prefs.edit().putBoolean("flow_mode", value).apply()

    /** Surah & ayat terakhir yang dibuka (di-restore saat startup). */
    var surahNumber: Int
        get() = prefs.getInt("surah_number", 1)
        set(value) = prefs.edit().putInt("surah_number", value).apply()

    var ayahIndex: Int
        get() = prefs.getInt("ayah_index", 0)
        set(value) = prefs.edit().putInt("ayah_index", value).apply()
}
