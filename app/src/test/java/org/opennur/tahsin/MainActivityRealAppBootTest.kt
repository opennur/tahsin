package org.opennur.tahsin

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.opennur.tahsin.ui.TahsinViewModel
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * Boot aplikasi dengan Application DAN MainActivity yang ASLI (dari
 * manifest) — tanpa HiltAndroidRule/HiltTestApplication — sehingga persis
 * jalur perangkat:
 *
 * `TahsinApplication` (@HiltAndroidApp) diinisialisasi oleh Android,
 * komponen Hilt dicari via `Class.forName` (membuktikan komponen benar-benar
 * ada di classpath), `MainActivity` (@AndroidEntryPoint) di-boot, dan
 * `viewModel()` menyelesaikan `@HiltViewModel` lewat factory Hilt.
 *
 * Ini reproduksi paling setia dari crash "app langsung tutup saat dibuka"
 * yang bisa dijalankan di JVM (tanpa emulator).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class MainActivityRealAppBootTest {

    @Test
    fun `TahsinApplication asli - komponen Hilt ditemukan`() {
        val app = ApplicationProvider.getApplicationContext<TahsinApplication>()
        assertThat(app).isNotNull()

        // Hilt_TahsinApplication.onCreate() memanggil Class.forName(
        // "org.opennur.tahsin.TahsinApplication_HiltComponents") — kalau
        // komponen tidak ada di classpath, di sinilah crash-nya terjadi.
        val components = Class.forName("org.opennur.tahsin.TahsinApplication_HiltComponents")
        assertThat(components).isNotNull()
    }

    @Test
    fun `MainActivity asli boot - graph Hilt + ViewModel`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assertThat(activity).isNotNull()
        assertThat(activity.isFinishing).isFalse()

        // viewModel() = default factory Hilt (via @AndroidEntryPoint activity).
        val tahsin: TahsinViewModel = ViewModelProvider(activity)[TahsinViewModel::class.java]
        assertThat(tahsin).isNotNull()

        activity.finish()
    }
}
