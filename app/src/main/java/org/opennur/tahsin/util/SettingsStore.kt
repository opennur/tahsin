package org.opennur.tahsin.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import org.opennur.tahsin.data.learning.LearningGoal

/**
 * Subset setelan yang dipakai ViewModel yang diuji — interface kecil supaya
 * unit test JVM bisa menyuntikkan fake tanpa Android (implementasi nyata:
 * [SettingsStore]).
 */
interface SettingsSource {
    /** Kode bahasa aktif ("id" / "en"). */
    val languageCode: String

    /** Primary learning goal; simple fakes can keep the default for unrelated tests. */
    val learningGoalKey: String
        get() = LearningGoal.RECITATION.key
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

    /** Whether the user has completed the first-run learning setup. */
    var onboardingComplete: Boolean
        get() = prefs[Keys.ONBOARDING_COMPLETE] ?: false
        set(value) = store.edit { this[Keys.ONBOARDING_COMPLETE] = value }

    /** Selected primary learning goal; unknown persisted values fall back safely. */
    override var learningGoalKey: String
        get() = prefs[Keys.LEARNING_GOAL] ?: LearningGoal.RECITATION.key
        set(value) = store.edit { this[Keys.LEARNING_GOAL] = value }

    val learningGoal: LearningGoal
        get() = LearningGoal.fromKey(learningGoalKey)

    /** Daily target in minutes, constrained to the supported onboarding choices. */
    var dailyMinutes: Int
        get() = (prefs[Keys.DAILY_MINUTES] ?: 15).coerceIn(5, 60)
        set(value) = store.edit { this[Keys.DAILY_MINUTES] = value.coerceIn(5, 60) }

    // ---- snapshot/restore for backup ----

    fun snapshotJson(): String {
        val parts = mutableListOf<String>()
        parts.add(jsonBool("dark_mode", darkMode))
        parts.add(jsonBool("swipe_hint_dismissed", swipeHintDismissed))
        parts.add(jsonBool("tajwid_color", tajwidColor))
        parts.add(jsonBool("show_translation", showTranslation))
        parts.add(jsonStr("language_code", languageCode))
        parts.add(jsonStr("audio_mode", audioMode))
        parts.add(jsonInt("surah_number", surahNumber))
        parts.add(jsonInt("ayah_index", ayahIndex))
        parts.add(jsonBool("ayah_of_day_enabled", ayahOfDayEnabled))
        parts.add(jsonBool("streak_reminder_enabled", streakReminderEnabled))
        parts.add(jsonStr("reciter_slug", reciterSlug))
        parts.add(jsonFloat("audio_speed", audioSpeed))
        parts.add(jsonFloat("font_scale", fontScale))
        parts.add(jsonBool("onboarding_complete", onboardingComplete))
        parts.add(jsonStr("learning_goal", learningGoalKey))
        parts.add(jsonInt("daily_minutes", dailyMinutes))
        backgroundDownloadAllowed?.let { parts.add(jsonBool("bg_download", it)) }
        return "{" + parts.joinToString(",") + "}"
    }

    fun restoreJson(json: String) {
        val map = parseSimpleJson(json)
        (map["dark_mode"] as? Boolean)?.let { darkMode = it }
        (map["swipe_hint_dismissed"] as? Boolean)?.let { swipeHintDismissed = it }
        (map["tajwid_color"] as? Boolean)?.let { tajwidColor = it }
        (map["show_translation"] as? Boolean)?.let { showTranslation = it }
        (map["language_code"] as? String)?.let { languageCode = it }
        (map["audio_mode"] as? String)?.let { audioMode = it }
        (map["surah_number"] as? Number)?.let { surahNumber = it.toInt() }
        (map["ayah_index"] as? Number)?.let { ayahIndex = it.toInt() }
        (map["ayah_of_day_enabled"] as? Boolean)?.let { ayahOfDayEnabled = it }
        (map["streak_reminder_enabled"] as? Boolean)?.let { streakReminderEnabled = it }
        (map["reciter_slug"] as? String)?.let { reciterSlug = it }
        (map["audio_speed"] as? Number)?.let { audioSpeed = it.toFloat() }
        (map["font_scale"] as? Number)?.let { fontScale = it.toFloat() }
        (map["onboarding_complete"] as? Boolean)?.let { onboardingComplete = it }
        (map["learning_goal"] as? String)?.let { learningGoalKey = it }
        (map["daily_minutes"] as? Number)?.let { dailyMinutes = it.toInt() }
        map["bg_download"]?.let {
            backgroundDownloadAllowed = it as? Boolean
        }
    }

    private fun jsonBool(key: String, value: Boolean): String = "\"$key\":$value"
    private fun jsonInt(key: String, value: Int): String = "\"$key\":$value"
    private fun jsonFloat(key: String, value: Float): String = "\"$key\":$value"
    private fun jsonStr(key: String, value: String): String =
        "\"$key\":\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

    private fun parseSimpleJson(json: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val trimmed = json.trim().removeSurrounding("{", "}")
        if (trimmed.isBlank()) return result
        // Simple parser for flat JSON object with boolean/string/number values
        val regex = Regex(""""([^"]+)"\s*:\s*("(?:[^"\\]|\\.)*"|-?\d+(?:\.\d+)?|true|false|null)""")
        for (match in regex.findAll(trimmed)) {
            val key = match.groupValues[1]
            val raw = match.groupValues[2]
            result[key] = when {
                raw == "true" -> true
                raw == "false" -> false
                raw == "null" -> continue
                raw.startsWith('"') && raw.endsWith('"') -> raw.removeSurrounding("\"")
                    .replace("\\\\", "\u0000").replace("\\\"", "\"").replace("\u0000", "\\")
                raw.contains('.') -> raw.toDouble()
                else -> raw.toLongOrNull() ?: raw.toDouble()
            }
        }
        return result
    }

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
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val LEARNING_GOAL = stringPreferencesKey("learning_goal")
        val DAILY_MINUTES = intPreferencesKey("daily_minutes")
    }
}

/** Rentang ukuran huruf mushaf (A− / A+). */
object FontScales {
    const val MIN = 1.0f
    const val MAX = 2.5f
    const val STEP = 0.25f

    fun clamp(value: Float): Float = value.coerceIn(MIN, MAX)
}
