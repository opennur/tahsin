package org.opennur.tahsin

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.opennur.tahsin.data.learning.LearningTaskType
import org.opennur.tahsin.theme.AyahColors
import org.opennur.tahsin.theme.AyahTheme
import org.opennur.tahsin.ui.AppStrings
import org.opennur.tahsin.ui.AudioManagerScreen
import org.opennur.tahsin.ui.AyatQuizScreen
import org.opennur.tahsin.ui.BadgesScreen
import org.opennur.tahsin.ui.CoherenceScreen
import org.opennur.tahsin.ui.DreamBigScreen
import org.opennur.tahsin.ui.FavoritesScreen
import org.opennur.tahsin.ui.HomeActions
import org.opennur.tahsin.ui.HomeLearningActions
import org.opennur.tahsin.ui.HomeScreen
import org.opennur.tahsin.ui.HomeUtilityActions
import org.opennur.tahsin.ui.LearningPlanViewModel
import org.opennur.tahsin.ui.LughohScreen
import org.opennur.tahsin.ui.MemorizationScreen
import org.opennur.tahsin.ui.OnboardingScreen
import org.opennur.tahsin.ui.OpenTarget
import org.opennur.tahsin.ui.PetaKhatamScreen
import org.opennur.tahsin.ui.SearchScreen
import org.opennur.tahsin.ui.SettingsActions
import org.opennur.tahsin.ui.SettingsAppearanceActions
import org.opennur.tahsin.ui.SettingsAudioActions
import org.opennur.tahsin.ui.SettingsNotificationActions
import org.opennur.tahsin.ui.SettingsScreen
import org.opennur.tahsin.ui.StatsScreen
import org.opennur.tahsin.ui.TahsinScreen
import org.opennur.tahsin.ui.TahsinViewModel
import org.opennur.tahsin.ui.TajwidQuizScreen
import org.opennur.tahsin.ui.VocabularyScreen
import org.opennur.tahsin.ui.components.BackgroundPromptDialog
import org.opennur.tahsin.ui.components.CelebrationDialog
import org.opennur.tahsin.ui.components.DownloadNoticeDialog
import org.opennur.tahsin.ui.navigation.AppScreen
import org.opennur.tahsin.util.GamificationEvents

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun MainActivityContent(
    target: OpenTarget?,
    onTargetConsumed: () -> Unit,
    onOpenAyah: (Int, Int) -> Unit,
) {
    AyahTheme {
        val context = LocalContext.current
        val tahsinViewModel: TahsinViewModel = viewModel()
        val settingsState by tahsinViewModel.settingsState.collectAsStateWithLifecycle()
        val learningPlanViewModel: LearningPlanViewModel = viewModel()
        val learningPlanState by learningPlanViewModel.state.collectAsStateWithLifecycle()

        val stackSaver = listSaver<List<AppScreen>, String>(
            save = { it.map { screen -> screen.tag } },
            restore = { tags -> tags.map { AppScreen.fromTag(it) } },
        )
        var stack by rememberSaveable(stateSaver = stackSaver) {
            mutableStateOf(listOf<AppScreen>(AppScreen.Home))
        }
        var activeTaskKey by rememberSaveable { mutableStateOf<String?>(null) }
        var showLearningSetup by rememberSaveable { mutableStateOf(false) }

        fun push(screen: AppScreen) {
            if (stack.last() != screen) stack = stack + screen
        }

        fun pop() {
            if (stack.size <= 1) return
            stack = stack.dropLast(1)
            if (stack.size == 1) {
                activeTaskKey?.let { key ->
                    LearningTaskType.fromKey(key)?.let(learningPlanViewModel::complete)
                }
                activeTaskKey = null
            }
        }

        fun openLearningTask(type: LearningTaskType) {
            activeTaskKey = type.key
            when (type) {
                LearningTaskType.RECITE -> push(AppScreen.Tahsin)
                LearningTaskType.MEMORIZATION -> push(AppScreen.Memorization)
                LearningTaskType.TAJWID -> push(AppScreen.Quiz)
                LearningTaskType.VOCABULARY -> push(AppScreen.Vocab)
                LearningTaskType.UNDERSTAND -> push(AppScreen.Coherence)
                LearningTaskType.ARABIC -> push(AppScreen.Lughoh)
            }
        }

        LaunchedEffect(target) {
            if (target != null) {
                stack = stack.filterNot { screen -> screen == AppScreen.Tahsin } + AppScreen.Tahsin
            }
        }

        BackHandler(enabled = stack.size > 1) { pop() }

        if (!learningPlanState.onboardingComplete || showLearningSetup) {
            OnboardingScreen(
                language = settingsState.language,
                initialGoal = learningPlanState.goal,
                initialMinutes = learningPlanState.dailyMinutes,
                onSetLanguage = tahsinViewModel::setLanguage,
                onComplete = { goal, minutes ->
                    learningPlanViewModel.saveOnboarding(goal, minutes)
                    showLearningSetup = false
                },
            )
        } else {
            when (val current = stack.last()) {
                AppScreen.Home -> HomeScreen(
                    actions = HomeActions(
                        learning = HomeLearningActions(
                            onOpenTahsin = { push(AppScreen.Tahsin) },
                            onOpenVocab = { push(AppScreen.Vocab) },
                            onOpenMemorization = { push(AppScreen.Memorization) },
                            onOpenQuiz = { push(AppScreen.Quiz) },
                            onOpenAyatQuiz = { push(AppScreen.AyatQuiz) },
                            onOpenLughoh = { push(AppScreen.Lughoh) },
                            onOpenDreamBig = { push(AppScreen.DreamBig) },
                        ),
                        utility = HomeUtilityActions(
                            onOpenStats = { push(AppScreen.Stats) },
                            onOpenBadges = { push(AppScreen.Badges) },
                            onOpenCoherence = { push(AppScreen.Coherence) },
                            onOpenFavorites = { push(AppScreen.Favorites) },
                            onOpenSettings = { push(AppScreen.Settings) },
                        ),
                        onOpenTask = ::openLearningTask,
                    ),
                    learningPlan = learningPlanState,
                    settings = settingsState,
                )
                AppScreen.Tahsin -> TahsinScreen(
                    viewModel = tahsinViewModel,
                    onOpenSearch = { push(AppScreen.Search) },
                    onOpenSettings = { push(AppScreen.Settings) },
                    onBack = { pop() },
                    target = target,
                    onTargetConsumed = onTargetConsumed,
                )
                AppScreen.Vocab -> VocabularyScreen(
                    onBack = { pop() },
                    onOpenAyah = onOpenAyah,
                )
                AppScreen.Memorization -> MemorizationScreen(
                    onBack = { pop() },
                    onOpenAyah = onOpenAyah,
                )
                AppScreen.Stats -> StatsScreen(
                    onBack = { pop() },
                    onOpenAyah = onOpenAyah,
                    onOpenPetaKhatam = { push(AppScreen.PetaKhatam) },
                )
                AppScreen.PetaKhatam -> PetaKhatamScreen(
                    onBack = { pop() },
                    onOpenPage = { page ->
                        // Buka halaman mushaf di Tahsin
                        val pagination = tahsinViewModel.uiState.value.let { state ->
                            (state as? org.opennur.tahsin.ui.TahsinUiState.Ready)?.pagination
                        }
                        val firstAyah = pagination?.firstAyahOf(page)
                        if (firstAyah != null) onOpenAyah(firstAyah.first, firstAyah.second)
                    },
                )
                AppScreen.Search -> SearchScreen(
                    onBack = { pop() },
                    onOpenAyah = onOpenAyah,
                )
                AppScreen.Quiz -> TajwidQuizScreen(onBack = { pop() })
                AppScreen.AudioManager -> AudioManagerScreen(
                    onBack = { pop() },
                    onDownloadAll = tahsinViewModel::downloadAllAudio,
                )
                AppScreen.DreamBig -> DreamBigScreen(onBack = { pop() })
                AppScreen.Lughoh -> LughohScreen(onBack = { pop() })
                AppScreen.AyatQuiz -> AyatQuizScreen(onBack = { pop() })
                AppScreen.Badges -> BadgesScreen(onBack = { pop() })
                AppScreen.Favorites -> FavoritesScreen(
                    onBack = { pop() },
                    onOpenAyah = onOpenAyah,
                )
                AppScreen.Coherence -> CoherenceScreen(
                    onBack = { pop() },
                    language = settingsState.language,
                )
                AppScreen.Settings -> SettingsScreen(
                    onBack = { pop() },
                    settings = settingsState,
                    actions = SettingsActions(
                        appearance = SettingsAppearanceActions(
                            onToggleTajwidColor = tahsinViewModel::toggleTajwidColor,
                            onToggleTranslation = tahsinViewModel::toggleTranslation,
                            onToggleFlowMode = tahsinViewModel::toggleFlowMode,
                            onToggleDarkMode = tahsinViewModel::toggleDarkMode,
                            onSetLanguage = tahsinViewModel::setLanguage,
                            onEditLearningPlan = { showLearningSetup = true },
                        ),
                        audio = SettingsAudioActions(
                            onSetReciter = tahsinViewModel::setReciter,
                            onSetSpeed = tahsinViewModel::setAudioSpeed,
                        ),
                        notifications = SettingsNotificationActions(
                            onToggleAyahOfDay = tahsinViewModel::toggleAyahOfDay,
                            onToggleStreakReminder = tahsinViewModel::toggleStreakReminder,
                        ),
                        onDownloadAll = tahsinViewModel::downloadAllAudio,
                        onOpenAudioManager = { push(AppScreen.AudioManager) },
                    ),
                )
            }
        }

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

        val celebration by GamificationEvents.event.collectAsStateWithLifecycle()
        LaunchedEffect(celebration) {
            if (celebration != null) {
                val vibrator = context.getSystemService(android.os.Vibrator::class.java)
                runCatching {
                    vibrator?.vibrate(
                        android.os.VibrationEffect.createOneShot(
                            250,
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE,
                        ),
                    )
                }
            }
        }
        celebration?.let { event ->
            CelebrationDialog(
                event = event,
                strings = strings,
                language = settingsState.language,
                onDismiss = { GamificationEvents.consume() },
            )
        }
    }
}
