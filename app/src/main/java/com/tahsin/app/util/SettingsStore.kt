package com.tahsin.app.util

import android.content.Context
import com.tahsin.app.theme.ArabicFont

/** Penyimpanan sederhana preferensi pengguna (SharedPreferences). */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tahsin_settings", Context.MODE_PRIVATE)

    var fontScale: Float
        get() = prefs.getFloat("font_scale", 1f).coerceIn(1f, 1.5f)
        set(value) = prefs.edit().putFloat("font_scale", value).apply()

    var fontName: String
        get() = prefs.getString("font_name", ArabicFont.UTSMANI.name) ?: ArabicFont.UTSMANI.name
        set(value) = prefs.edit().putString("font_name", value).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    /** Surah & ayat terakhir yang dibuka (di-restore saat startup). */
    var surahNumber: Int
        get() = prefs.getInt("surah_number", 1)
        set(value) = prefs.edit().putInt("surah_number", value).apply()

    var ayahIndex: Int
        get() = prefs.getInt("ayah_index", 0)
        set(value) = prefs.edit().putInt("ayah_index", value).apply()
}
