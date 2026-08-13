package org.opennur.tahsin.util

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

    /** Notifikasi harian "Ayah of the Day" (default nyala). */
    var ayahOfDayEnabled: Boolean
        get() = prefs.getBoolean("ayah_of_day_enabled", true)
        set(value) = prefs.edit().putBoolean("ayah_of_day_enabled", value).apply()

    /** Pengingat harian untuk menjaga streak (default mati — opsional). */
    var streakReminderEnabled: Boolean
        get() = prefs.getBoolean("streak_reminder_enabled", false)
        set(value) = prefs.edit().putBoolean("streak_reminder_enabled", value).apply()

    /** Qari' (perawi) audio ayat aktif (default Minshawy Murattal). */
    var reciterSlug: String
        get() = prefs.getString("reciter_slug", Reciter.MINSHAWY.slug) ?: Reciter.MINSHAWY.slug
        set(value) = prefs.edit().putString("reciter_slug", value).apply()

    /** Qari' aktif sebagai [Reciter] (fallback Minshawy kalau slug tak dikenal). */
    val reciter: Reciter
        get() = Reciter.fromSlug(reciterSlug)

    /** Kecepatan pemutaran audio (0.5×–1.25×; default 1.0×). */
    var audioSpeed: Float
        get() = AudioSpeeds.clamp(prefs.getFloat("audio_speed", 1.0f))
        set(value) = prefs.edit().putFloat("audio_speed", AudioSpeeds.clamp(value)).apply()

    /**
     * Ukuran huruf mushaf halaman (pengali teks). Rentang 1.0–2.5, default 1.5
     * (tampilan bawaan layar Tahsin). Dipakai tombol A− / A+ di layar Tahsin.
     */
    var fontScale: Float
        get() = FontScales.clamp(prefs.getFloat("font_scale", 1.5f))
        set(value) = prefs.edit().putFloat("font_scale", FontScales.clamp(value)).apply()
}

/** Rentang ukuran huruf mushaf (A− / A+). */
object FontScales {
    const val MIN = 1.0f
    const val MAX = 2.5f
    const val STEP = 0.25f

    fun clamp(value: Float): Float = value.coerceIn(MIN, MAX)
}
