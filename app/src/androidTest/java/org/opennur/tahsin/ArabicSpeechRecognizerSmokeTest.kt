package org.opennur.tahsin

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opennur.tahsin.stt.ArabicSpeechRecognizer

/**
 * Smoke tests untuk ArabicSpeechRecognizer — memastikan recognizer bisa
 * dibuat dan dimanipulasi tanpa crash pada perangkat nyata/emulator.
 */
@RunWith(AndroidJUnit4::class)
class ArabicSpeechRecognizerSmokeTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var recognizer: ArabicSpeechRecognizer

    @Before
    fun setUp() {
        recognizer = ArabicSpeechRecognizer(context)
    }

    @Test
    fun constructor_createsInstance() {
        assertNotNull("Recognizer should be created", recognizer)
    }

    @Test
    fun destroy_doesNotCrash() {
        // Destroy tanpa start sebelumnya
        recognizer.destroy()
    }

    @Test
    fun stop_doesNotCrash() {
        // Stop tanpa start sebelumnya
        recognizer.stop()
    }

    @Test
    fun start_andStop_cycle() {
        var listeningChanged = false
        var errorReceived = false

        val listener = object : ArabicSpeechRecognizer.Listener {
            override fun onPartial(text: String) {}
            override fun onResult(text: String) {}
            override fun onError(error: Int) { errorReceived = true }
            override fun onListeningChanged(listening: Boolean) { listeningChanged = true }
        }

        // Start — mungkin gagal tanpa izin recording, tapi tidak boleh crash
        recognizer.start(listener)
        Thread.sleep(300)

        // Stop
        recognizer.stop()
        Thread.sleep(200)

        // Destroy
        recognizer.destroy()

        // Verifikasi lifecycle selesai tanpa crash
        // (listeningChanged mungkin false jika tidak ada izin)
    }

    @Test
    fun listener_canBeSetToNull() {
        recognizer.start(object : ArabicSpeechRecognizer.Listener {
            override fun onPartial(text: String) {}
            override fun onResult(text: String) {}
            override fun onError(error: Int) {}
            override fun onListeningChanged(listening: Boolean) {}
        })
        Thread.sleep(200)
        recognizer.stop()
        recognizer.destroy()
    }
}
