package com.tahsin.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tahsin.app.theme.AyahColors
import com.tahsin.app.theme.AyahTheme
import com.tahsin.app.ui.AppStrings
import com.tahsin.app.ui.AudioManagerScreen
import com.tahsin.app.ui.DreamBigScreen
import com.tahsin.app.ui.HomeScreen
import com.tahsin.app.ui.OpenTarget
import com.tahsin.app.ui.SearchScreen
import com.tahsin.app.ui.SettingsScreen
import com.tahsin.app.ui.StatsScreen
import com.tahsin.app.ui.TahsinScreen
import com.tahsin.app.ui.TahsinViewModel
import com.tahsin.app.ui.TajwidQuizScreen
import com.tahsin.app.ui.VocabularyScreen
import com.tahsin.app.ui.components.BackgroundPromptDialog
import com.tahsin.app.ui.components.DownloadNoticeDialog
import com.tahsin.app.ui.navigation.AppScreen
import com.tahsin.app.ui.tahsinViewModelFactory
import com.tahsin.app.util.SettingsStore
import com.tahsin.app.widget.AyahOfTheDayAlarm

class MainActivity : ComponentActivity() {

    companion object {
        /** Extra target dari widget/notifikasi "Ayah of the Day". */
        const val EXTRA_TARGET_SURAH = "target_surah"
        const val EXTRA_TARGET_AYAH = "target_ayah"
    }

    /**
     * Target buka (surah, ayat 1-based) — dari widget/notifikasi maupun layar
     * sekunder (statistik/pencarian/kosakata). Dikonsumsi layar Tahsin sekali
     * lewat [TahsinScreen.onTargetConsumed], jadi tidak terkirim ulang saat
     * Tahsin dibuka lagi dari portal.
     */
    private var target by mutableStateOf<OpenTarget?>(null)

    /** Counter pengiriman target: naik tiap deep link valid → key LaunchedEffect unik. */
    private var targetDelivery = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 35 mewajibkan edge-to-edge: konten digambar di belakang
        // system bars; inset ditangani di layar (WindowInsets.safeDrawing).
        enableEdgeToEdge()
        // Alarm harian (widget & notifikasi) tetap jalan meski user tak pernah
        // menambah widget — cukup pernah membuka aplikasi.
        AyahOfTheDayAlarm.scheduleDaily(this)
        // Portal & layar lain ikut mode gelap sejak awal (bukan nunggu Tahsin dimuat).
        AyahColors.isDark = SettingsStore(this).darkMode
        target = readTarget(intent)
        setContent {
            AyahTheme {
                // ViewModel bersama (scope activity): setelan dipakai portal,
                // layar Tahsin, dan layar Pengaturan — satu sumber kebenaran.
                val context = LocalContext.current
                val tahsinViewModel: TahsinViewModel = viewModel(factory = tahsinViewModelFactory(context))
                val settingsState by tahsinViewModel.settingsState.collectAsStateWithLifecycle()

                // Back stack layar: Home selalu di dasar; layar lain di-push/pop.
                // Disimpan lewat rememberSaveable agar rotasi tidak melompat ke Home.
                val stackSaver = listSaver<List<AppScreen>, String>(
                    save = { it.map { s -> s.tag } },
                    restore = { tags -> tags.map { AppScreen.fromTag(it) } },
                )
                var stack by rememberSaveable(stateSaver = stackSaver) {
                    mutableStateOf(listOf<AppScreen>(AppScreen.Home))
                }

                fun push(screen: AppScreen) {
                    // Jangan menumpuk layar yang sama di puncak (mis. double-tap kartu).
                    if (stack.last() != screen) stack = stack + screen
                }

                fun pop() {
                    if (stack.size > 1) stack = stack.dropLast(1)
                }

                // Deep link (widget/notifikasi) & target dari layar sekunder:
                // pastikan Tahsin ada di puncak tumpukan dengan target terbaru
                // (Tahsin yang lama diganti, sisanya dipertahankan).
                LaunchedEffect(target) {
                    target?.let {
                        stack = stack.filterNot { s -> s == AppScreen.Tahsin } + AppScreen.Tahsin
                    }
                }

                // Tombol back sistem = pop satu layar; di Home keluar aplikasi.
                BackHandler(enabled = stack.size > 1) { pop() }

                when (val current = stack.last()) {
                    AppScreen.Home -> HomeScreen(
                        onOpenTahsin = { push(AppScreen.Tahsin) },
                        onOpenVocab = { push(AppScreen.Vocab) },
                        onOpenQuiz = { push(AppScreen.Quiz) },
                        onOpenStats = { push(AppScreen.Stats) },
                        onOpenSearch = { push(AppScreen.Search) },
                        onOpenAudioManager = { push(AppScreen.AudioManager) },
                        onOpenDreamBig = { push(AppScreen.DreamBig) },
                        onOpenSettings = { push(AppScreen.Settings) },
                        settings = settingsState,
                    )
                    AppScreen.Tahsin -> TahsinScreen(
                        viewModel = tahsinViewModel,
                        onOpenSearch = { push(AppScreen.Search) },
                        onOpenSettings = { push(AppScreen.Settings) },
                        target = target,
                        onTargetConsumed = { target = null },
                    )
                    AppScreen.Vocab -> VocabularyScreen(
                        onBack = { pop() },
                        onOpenAyah = { s, a ->
                            target = OpenTarget(s, a, targetDelivery++)
                        },
                    )
                    AppScreen.Stats -> StatsScreen(
                        onBack = { pop() },
                        onOpenAyah = { s, a ->
                            target = OpenTarget(s, a, targetDelivery++)
                        },
                    )
                    AppScreen.Search -> SearchScreen(
                        onBack = { pop() },
                        onOpenAyah = { s, a ->
                            target = OpenTarget(s, a, targetDelivery++)
                        },
                    )
                    AppScreen.Quiz -> TajwidQuizScreen(onBack = { pop() })
                    AppScreen.AudioManager -> AudioManagerScreen(onBack = { pop() })
                    AppScreen.DreamBig -> DreamBigScreen(onBack = { pop() })
                    AppScreen.Settings -> SettingsScreen(
                        onBack = { pop() },
                        settings = settingsState,
                        onToggleTajwidColor = tahsinViewModel::toggleTajwidColor,
                        onToggleFlowMode = tahsinViewModel::toggleFlowMode,
                        onToggleDarkMode = tahsinViewModel::toggleDarkMode,
                        onSetLanguage = tahsinViewModel::setLanguage,
                        onSetReciter = tahsinViewModel::setReciter,
                        onSetSpeed = tahsinViewModel::setAudioSpeed,
                        onToggleAyahOfDay = tahsinViewModel::toggleAyahOfDay,
                        onDownloadAll = tahsinViewModel::downloadAllAudio,
                    )
                }

                // Dialog unduhan global — bisa dipicu dari Tahsin (tombol Dengar)
                // maupun Pengaturan (Unduh Semua), tampil di layar mana pun.
                val strings = AppStrings.of(settingsState.language)
                if (settingsState.showDownloadNotice) {
                    DownloadNoticeDialog(
                        strings = strings,
                        onDismiss = tahsinViewModel::dismissDownloadNotice,
                    )
                }
                if (settingsState.showBackgroundPrompt) {
                    BackgroundPromptDialog(
                        strings = strings,
                        onSetBackgroundAllowed = tahsinViewModel::setBackgroundDownloadAllowed,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Deep link baru (widget/notifikasi) menggantikan target lama yang
        // belum sempat dipakai — biar ketukan widget tidak diabaikan.
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
