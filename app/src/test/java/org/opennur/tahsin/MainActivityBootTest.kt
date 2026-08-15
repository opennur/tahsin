package org.opennur.tahsin

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opennur.tahsin.ui.GamificationViewModel
import org.opennur.tahsin.ui.LearningPlanViewModel
import org.opennur.tahsin.ui.TahsinViewModel
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * Test boot aplikasi dengan Hilt + Robolectric — memvalidasi di RUNTIME:
 *
 * 1. Komponen Hilt (test) terbentuk dan graph `AppModule` + semua
 *    `@HiltViewModel` ter-resolve (grafik yang sama dipakai produksi).
 * 2. `MainActivity` (@AndroidEntryPoint) benar-benar di-boot (onCreate +
 *    setContent) tanpa crash.
 * 3. ViewModel `@HiltViewModel` ter-resolve lewat default factory Hilt
 *    (`viewModel()` tanpa factory manual) — jalur yang dipakai semua layar.
 *
 * Memakai `HiltTestApplication` (aturan Hilt: `@HiltAndroidApp` asli tidak
 * bisa dipakai langsung oleh `HiltAndroidRule`). Packaging komponen Hilt ke
 * APK (regresi `enableAggregatingTask` + kapt) diverifikasi terpisah lewat
 * inspeksi dex APK di CI/verifikasi build — lihat app/build.gradle.kts.
 *
 * Konscrypt dimatikan (tidak ada native linux-arm64 di Termux); tes ini
 * tidak memakai jaringan.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = HiltTestApplication::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class MainActivityBootTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun `komponen Hilt test terbentuk - graph penuh`() {
        hiltRule.inject()
        val app = ApplicationProvider.getApplicationContext<HiltTestApplication>()
        assertThat(app).isNotNull()
    }

    @Test
    fun `MainActivity boot - compose + viewModel Hilt tersolve`() {
        hiltRule.inject()

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assertThat(activity).isNotNull()
        assertThat(activity.isFinishing).isFalse()

        // viewModel() tanpa factory manual = default factory Hilt → graph penuh.
        val tahsin: TahsinViewModel = ViewModelProvider(activity)[TahsinViewModel::class.java]
        assertThat(tahsin).isNotNull()

        // VM lain yang dipakai HomeScreen juga tersolve.
        val gamification: GamificationViewModel =
            ViewModelProvider(activity)[GamificationViewModel::class.java]
        assertThat(gamification).isNotNull()

        val learningPlan: LearningPlanViewModel =
            ViewModelProvider(activity)[LearningPlanViewModel::class.java]
        assertThat(learningPlan).isNotNull()

        activity.finish()
    }
}
