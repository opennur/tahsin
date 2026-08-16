package org.opennur.tahsin

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opennur.tahsin.util.SettingsStore
import org.opennur.tahsin.util.TahsinAudioPlayer

/**
 * Smoke tests untuk TahsinAudioPlayer — memastikan player bisa dibuat
 * dan dimanipulasi tanpa crash pada perangkat nyata/emulator.
 */
@RunWith(AndroidJUnit4::class)
class TahsinAudioPlayerSmokeTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var settings: SettingsStore
    private lateinit var player: TahsinAudioPlayer

    @Before
    fun setUp() {
        settings = SettingsStore(context)
        player = TahsinAudioPlayer(context, settings)
    }

    @Test
    fun constructor_createsInstance() {
        assertNotNull("Player should be created", player)
    }

    @Test
    fun stop_doesNotCrash() {
        // Stop tanpa play sebelumnya
        player.stop()
        // Stop lagi untuk memastikan idempotent
        player.stop()
    }

    @Test
    fun sourceProperty_isAccessible() {
        // Verifikasi properti source bisa dibaca
        val source = player.source
        assertNotNull("Source should have a value", source)
    }

    @Test
    fun callbacks_canBeSet() {
        // Set callback tanpa crash
        player.onPlaybackChange = { }
        player.onCompletion = { }
        // Set null juga aman
        player.onPlaybackChange = null
        player.onCompletion = null
    }

    @Test
    fun playAyah_withInvalidData_doesNotCrash() {
        // Play dengan data invalid (surah tidak ada) — harus handle gracefully
        player.playAyah(
            surahNumber = 999,
            ayahNumber = 999,
            text = "test",
            onFallback = { },
        )
        // Tunggu sebentar untuk memastikan tidak crash async
        Thread.sleep(200)
        player.stop()
    }
}
