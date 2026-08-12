package com.tahsin.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tahsin.app.theme.AyahTheme
import com.tahsin.app.ui.AudioManagerScreen
import com.tahsin.app.ui.OpenTarget
import com.tahsin.app.ui.SearchScreen
import com.tahsin.app.ui.StatsScreen
import com.tahsin.app.ui.TahsinScreen
import com.tahsin.app.ui.TajwidQuizScreen
import com.tahsin.app.widget.AyahOfTheDayAlarm

class MainActivity : ComponentActivity() {

    companion object {
        /** Extra target dari widget/notifikasi "Ayah of the Day". */
        const val EXTRA_TARGET_SURAH = "target_surah"
        const val EXTRA_TARGET_AYAH = "target_ayah"
    }

    /** Target buka (surah, ayat 1-based) — state Compose agar onNewIntent ikut recompose. */
    private var target by mutableStateOf<OpenTarget?>(null)

    /** Counter pengiriman target: naik tiap deep link valid → key LaunchedEffect unik. */
    private var targetDelivery = 0L

    /** Target buka ayat dari layar sekunder (statistik/pencarian) — dikonsumsi
     * saat deep link widget/notifikasi baru datang (onNewIntent). */
    private var pendingOpenTarget by mutableStateOf<OpenTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 35 mewajibkan edge-to-edge: konten digambar di belakang
        // system bars; inset ditangani di layar (WindowInsets.safeDrawing).
        enableEdgeToEdge()
        // Alarm harian (widget & notifikasi) tetap jalan meski user tak pernah
        // menambah widget — cukup pernah membuka aplikasi.
        AyahOfTheDayAlarm.scheduleDaily(this)
        target = readTarget(intent)
        setContent {
            AyahTheme {
                var showAudioManager by remember { mutableStateOf(false) }
                var showStats by remember { mutableStateOf(false) }
                var showSearch by remember { mutableStateOf(false) }
                var showQuiz by remember { mutableStateOf(false) }
                // Tombol back sistem kembali ke layar utama (bukan menutup aplikasi).
                BackHandler(enabled = showAudioManager || showStats || showSearch || showQuiz) {
                    showAudioManager = false
                    showStats = false
                    showSearch = false
                    showQuiz = false
                }
                when {
                    showAudioManager -> AudioManagerScreen(onBack = { showAudioManager = false })
                    showStats -> StatsScreen(
                        onBack = { showStats = false },
                        onOpenAyah = { s, a ->
                            pendingOpenTarget = OpenTarget(s, a, targetDelivery++)
                            showStats = false
                        },
                    )
                    showSearch -> SearchScreen(
                        onBack = { showSearch = false },
                        onOpenAyah = { s, a ->
                            pendingOpenTarget = OpenTarget(s, a, targetDelivery++)
                            showSearch = false
                        },
                    )
                    showQuiz -> TajwidQuizScreen(onBack = { showQuiz = false })
                    else -> TahsinScreen(
                        onOpenAudioManager = { showAudioManager = true },
                        onOpenStats = { showStats = true },
                        onOpenSearch = { showSearch = true },
                        onOpenQuiz = { showQuiz = true },
                        target = pendingOpenTarget ?: target,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Deep link baru (widget/notifikasi) menang atas target layar sekunder
        // yang belum sempat dipakai — biar ketukan widget tidak diabaikan.
        pendingOpenTarget = null
        target = readTarget(intent)
    }

    /**
     * Extra surah/ayat dari widget/notifikasi; null kalau bukan deep link.
     * Extra langsung dihapus agar rotasi layar tidak mengulang target lama.
     */
    private fun readTarget(intent: Intent?): OpenTarget? {
        val i = intent ?: return null
        val surah = i.getIntExtra(EXTRA_TARGET_SURAH, 0)
        val ayah = i.getIntExtra(EXTRA_TARGET_AYAH, 0)
        if (surah <= 0 || ayah <= 0) return null
        // Key unik per pengiriman: PendingIntent widget membekukan extra-nya,
        // jadi identitas intent tidak bisa dipakai — counter di aktivitas yang naik.
        targetDelivery++
        i.removeExtra(EXTRA_TARGET_SURAH)
        i.removeExtra(EXTRA_TARGET_AYAH)
        return OpenTarget(surah, ayah, targetDelivery)
    }
}
