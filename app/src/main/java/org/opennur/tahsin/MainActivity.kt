package org.opennur.tahsin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.ui.OpenTarget
import org.opennur.tahsin.util.SettingsStore
import org.opennur.tahsin.widget.AyahOfTheDayAlarm
import org.opennur.tahsin.widget.StreakReminderAlarm

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        /** Extra target dari widget/notifikasi "Ayah of the Day". */
        const val EXTRA_TARGET_SURAH = "target_surah"
        const val EXTRA_TARGET_AYAH = "target_ayah"
    }

    /** Target ayat yang dikirim dari widget atau layar sekunder. */
    private var target by mutableStateOf<OpenTarget?>(null)

    /** Counter pengiriman target agar LaunchedEffect menerima key unik. */
    private var targetDelivery = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AyahOfTheDayAlarm.scheduleDaily(this)
        if (SettingsStore(this).streakReminderEnabled) {
            StreakReminderAlarm.scheduleDaily(this)
        }
        AyahColors.isDark = SettingsStore(this).darkMode
        target = readTarget(intent)
        setContent {
            MainActivityContent(
                target = target,
                onTargetConsumed = { target = null },
                onOpenAyah = { surah, ayah ->
                    target = OpenTarget(surah, ayah, targetDelivery++)
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        target = readTarget(intent)
    }

    /** Read and consume a widget/notification target. */
    private fun readTarget(intent: Intent?): OpenTarget? {
        val current = intent ?: return null
        val surah = current.getIntExtra(EXTRA_TARGET_SURAH, 0)
        val ayah = current.getIntExtra(EXTRA_TARGET_AYAH, 0)
        if (surah <= 0 || ayah <= 0) return null
        targetDelivery++
        current.removeExtra(EXTRA_TARGET_SURAH)
        current.removeExtra(EXTRA_TARGET_AYAH)
        return OpenTarget(surah, ayah, targetDelivery)
    }
}
