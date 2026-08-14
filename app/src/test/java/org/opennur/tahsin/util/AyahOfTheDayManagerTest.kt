package org.opennur.tahsin.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * Tes jembatan Android "Ayah of the Day" dengan Robolectric — cache
 * (Preferences DataStore), validasi tanggal, dan bahasa — headless di JVM.
 * Logika pemilihan itu sendiri sudah diuji 100% di AyahOfTheDayPickerTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
// Conscrypt (TLS) tidak punya native untuk linux-arm64 (Termux) — matikan.
@ConscryptMode(ConscryptMode.Mode.OFF)
class AyahOfTheDayManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun ayah(date: LocalDate, language: String = AppLanguage.ID.code) = AyahOfTheDay(
        dateKey = date.toString(),
        surahNumber = 1,
        ayahNumber = 1,
        surahName = "Al-Fatihah",
        arabic = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
        translation = "Dengan nama Allah",
        language = language,
    )

    @Test
    fun `cache lalu baca - round trip utuh`() {
        val date = LocalDate.of(2024, 1, 1)
        AyahOfTheDayManager.cache(context, ayah(date))

        val loaded = AyahOfTheDayManager.cached(context, date, AppLanguage.ID)
        assertThat(loaded).isEqualTo(ayah(date))
    }

    @Test
    fun `cache basi saat tanggal berganti - null`() {
        val date = LocalDate.of(2024, 1, 1)
        AyahOfTheDayManager.cache(context, ayah(date))

        val nextDay = AyahOfTheDayManager.cached(context, LocalDate.of(2024, 1, 2), AppLanguage.ID)
        assertThat(nextDay).isNull()
    }

    @Test
    fun `cache basi saat bahasa berubah - null`() {
        val date = LocalDate.of(2024, 1, 1)
        AyahOfTheDayManager.cache(context, ayah(date, language = AppLanguage.ID.code))

        val en = AyahOfTheDayManager.cached(context, date, AppLanguage.EN)
        assertThat(en).isNull()
    }

    @Test
    fun `tanpa cache - null tidak crash`() {
        val loaded = AyahOfTheDayManager.cached(context, LocalDate.now(), AppLanguage.ID)
        assertThat(loaded).isNull()
    }

    @Test
    fun `languageOf mengikuti setelan`() {
        SettingsStore(context).languageCode = "en"
        assertThat(AyahOfTheDayManager.languageOf(context)).isEqualTo(AppLanguage.EN)

        SettingsStore(context).languageCode = "id"
        assertThat(AyahOfTheDayManager.languageOf(context)).isEqualTo(AppLanguage.ID)
    }
}
