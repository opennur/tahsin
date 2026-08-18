package org.opennur.tahsin

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Smoke tests: memastikan Composable utama bisa dikomposisi tanpa crash.
 * Tidak menguji interaksi — hanya verifikasi bahwa komposisi selesai
 * tanpa exception (layout inflation, theme, dsb).
 */
@HiltAndroidTest
class ComposeSmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun homeScreen_composesWithoutCrash() {
        composeRule.setContent {
            val noOp: () -> Unit = {}
            org.opennur.tahsin.ui.HomeScreen(
                actions = org.opennur.tahsin.ui.HomeActions(
                    learning = org.opennur.tahsin.ui.HomeLearningActions(
                        onOpenTahsin = noOp,
                        onOpenVocab = noOp,
                        onOpenDreamBig = noOp,
                        onOpenLughoh = noOp,
                        onOpenAyahQuiz = noOp,
                        onOpenSurahQuiz = noOp,
                        onOpenCoherence = noOp,
                    ),
                    utility = org.opennur.tahsin.ui.HomeUtilityActions(
                        onOpenStats = noOp,
                        onOpenBadges = noOp,
                        onOpenSettings = noOp,
                    ),
                ),
                learningPlan = org.opennur.tahsin.util.LearningPlanUiState(),
                settings = org.opennur.tahsin.ui.SettingsUiState(),
            )
        }
        composeRule.onRoot().printToLog("HOME_SMOKE")
    }

    @Test
    fun searchScreen_composesWithoutCrash() {
        composeRule.setContent {
            org.opennur.tahsin.ui.SearchScreen(
                onBack = {},
                onOpenAyah = { _, _ -> },
            )
        }
        composeRule.onRoot().printToLog("SEARCH_SMOKE")
    }

    @Test
    fun settingsScreen_composesWithoutCrash() {
        composeRule.setContent {
            org.opennur.tahsin.ui.SettingsScreen(
                onBack = {},
                settings = org.opennur.tahsin.ui.SettingsUiState(),
                actions = org.opennur.tahsin.ui.SettingsActions(
                    appearance = org.opennur.tahsin.ui.SettingsAppearanceActions(
                        onToggleTajwidColor = {},
                        onToggleTranslation = {},
                        onToggleDarkMode = {},
                        onSetMushafRenderMode = {},
                        onSetLanguage = {},
                        onEditLearningPlan = {},
                    ),
                    audio = org.opennur.tahsin.ui.SettingsAudioActions(
                        onSetReciter = {},
                        onSetSpeed = {},
                    ),
                    notifications = org.opennur.tahsin.ui.SettingsNotificationActions(
                        onToggleAyahOfDay = {},
                        onToggleStreakReminder = {},
                    ),
                    onDownloadAll = {},
                    onOpenAudioManager = {},
                ),
            )
        }
        composeRule.onRoot().printToLog("SETTINGS_SMOKE")
    }
}
