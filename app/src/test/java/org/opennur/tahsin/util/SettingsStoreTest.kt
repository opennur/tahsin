package org.opennur.tahsin.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.opennur.tahsin.data.learning.LearningGoal
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * Tes SettingsStore (Preferences DataStore) dengan Robolectric — framework
 * Android dijalankan headless di JVM, jadi DataStore + filesDir asli bisa
 * diuji tanpa emulator.
 *
 * Menggantikan SharedPreferences: baca/tulis lewat façade sinkron
 * ([PreferencesStore]), migrasi key lama lewat SharedPreferencesMigration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
// Conscrypt (TLS) tidak punya native untuk linux-arm64 (Termux) dan tes ini
// tidak memakai jaringan — matikan supaya Robolectric bisa jalan di semua platform.
@ConscryptMode(ConscryptMode.Mode.OFF)
class SettingsStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `nilai default - semua setelan bawaan`() {
        val store = SettingsStore(context)

        assertThat(store.darkMode).isFalse()
        assertThat(store.swipeHintDismissed).isFalse()
        assertThat(store.tajwidColor).isTrue()
        assertThat(store.showTranslation).isFalse()
        assertThat(store.languageCode).isEqualTo(AppLanguage.ID.code)
        assertThat(store.audioMode).isEqualTo("AYAH")
        assertThat(store.backgroundDownloadAllowed).isNull()
        assertThat(store.surahNumber).isEqualTo(1)
        assertThat(store.ayahIndex).isEqualTo(0)
        assertThat(store.ayahOfDayEnabled).isTrue()
        assertThat(store.streakReminderEnabled).isFalse()
        assertThat(store.reciter).isEqualTo(Reciter.MINSHAWY)
        assertThat(store.audioSpeed).isEqualTo(1.0f)
        assertThat(store.fontScale).isEqualTo(1.5f)
        assertThat(store.learningGoal).isEqualTo(LearningGoal.RECITATION)
        assertThat(store.dailyMinutes).isEqualTo(15)
    }

    @Test
    fun `tulis lalu baca lintas instance - tersimpan di DataStore`() {
        SettingsStore(context).apply {
            darkMode = true
            languageCode = "en"
            surahNumber = 2
            ayahIndex = 255
            reciterSlug = Reciter.HUSARY.slug
            audioSpeed = 1.25f
            fontScale = 2.0f
            backgroundDownloadAllowed = true
            onboardingComplete = true
            learningGoalKey = LearningGoal.MEMORIZATION.key
            dailyMinutes = 30
        }

        val second = SettingsStore(context)
        assertThat(second.darkMode).isTrue()
        assertThat(second.languageCode).isEqualTo("en")
        assertThat(second.surahNumber).isEqualTo(2)
        assertThat(second.ayahIndex).isEqualTo(255)
        assertThat(second.reciter).isEqualTo(Reciter.HUSARY)
        assertThat(second.audioSpeed).isEqualTo(1.25f)
        assertThat(second.fontScale).isEqualTo(2.0f)
        assertThat(second.backgroundDownloadAllowed).isTrue()
        assertThat(second.onboardingComplete).isTrue()
        assertThat(second.learningGoal).isEqualTo(LearningGoal.MEMORIZATION)
        assertThat(second.dailyMinutes).isEqualTo(30)
    }

    @Test
    fun `backgroundDownloadAllowed null = belum ditanya - bisa dihapus lagi`() {
        val store = SettingsStore(context)
        store.backgroundDownloadAllowed = false
        assertThat(store.backgroundDownloadAllowed).isFalse()

        store.backgroundDownloadAllowed = null
        assertThat(store.backgroundDownloadAllowed).isNull()
    }

    @Test
    fun `clamping - kecepatan dan ukuran huruf dibatasi`() {
        val store = SettingsStore(context)

        store.audioSpeed = 5f
        assertThat(store.audioSpeed).isEqualTo(AudioSpeeds.MAX)

        store.fontScale = 0.1f
        assertThat(store.fontScale).isEqualTo(FontScales.MIN)

        store.dailyMinutes = 1
        assertThat(store.dailyMinutes).isEqualTo(5)
        store.dailyMinutes = 100
        assertThat(store.dailyMinutes).isEqualTo(60)
    }

    @Test
    fun `slug qari tak dikenal - fallback Minshawy`() {
        val store = SettingsStore(context)
        store.reciterSlug = "TIDAK_ADA"
        assertThat(store.reciter).isEqualTo(Reciter.MINSHAWY)
    }

    @Test
    fun `migrasi dari SharedPreferences lama - nilai terbawa ke DataStore`() {
        // Simulasikan instalasi lama: key SharedPreferences persis seperti
        // sebelum migrasi (nama file "tahsin_settings").
        val legacy = context.getSharedPreferences("tahsin_settings", Context.MODE_PRIVATE)
        legacy.edit()
            .putBoolean("dark_mode", true)
            .putString("language_code", "en")
            .putInt("surah_number", 2)
            .putString("reciter_slug", Reciter.HUSARY.slug)
            .putFloat("font_scale", 2.0f)
            .commit()

        // DataStore pertama kali dibuka → SharedPreferencesMigration memindahkan
        // key lama; SettingsStore harus membaca nilai hasil migrasi.
        val store = SettingsStore(context)
        assertThat(store.darkMode).isTrue()
        assertThat(store.languageCode).isEqualTo("en")
        assertThat(store.surahNumber).isEqualTo(2)
        assertThat(store.reciter).isEqualTo(Reciter.HUSARY)
        assertThat(store.fontScale).isEqualTo(2.0f)
    }
}
