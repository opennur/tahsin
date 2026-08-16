package org.opennur.tahsin

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests untuk DownloadService — memastikan service bisa dimulai
 * dan dihentikan tanpa crash pada perangkat nyata/emulator.
 */
@RunWith(AndroidJUnit4::class)
class DownloadServiceSmokeTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun start_andStop_doesNotCrash() {
        // Pastikan service tidak running sebelum test
        DownloadService.stop(context)

        // Start service
        DownloadService.start(context)
        // Beri waktu service untuk start
        Thread.sleep(500)

        // Stop service
        DownloadService.stop(context)
        Thread.sleep(300)

        // Verifikasi tidak crash — service bisa dihentikan
        assertFalse("Service should not be running after stop", DownloadService.isRunning())
    }

    @Test
    fun updateProgress_doesNotCrash() {
        DownloadService.start(context)
        Thread.sleep(500)

        // Update progress beberapa kali
        DownloadService.updateProgress(1, 10, "Testing progress 1")
        DownloadService.updateProgress(5, 10, "Testing progress 2")
        DownloadService.updateProgress(10, 10, "Testing progress 3")

        Thread.sleep(300)
        DownloadService.stop(context)
    }

    @Test
    fun companionMethods_areAccessible() {
        // Verifikasi companion object bisa diakses tanpa crash
        assertNotNull("isRunning should return a value", DownloadService.isRunning())
    }
}
