package org.opennur.tahsin.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.GamificationStats
import org.opennur.tahsin.util.GamificationStore
import org.opennur.tahsin.util.SettingsStore

/**
 * Tes GamificationViewModel dengan MockK + Turbine + Truth — pola baseline
 * MVVM testable: dependensi (GamificationStore, SettingsStore) di-mock,
 * ViewModel dikonstruksi langsung, state diverifikasi per-emisi.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GamificationViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(
        stats: GamificationStats = GamificationStats(),
        code: String = "id",
    ): GamificationViewModel = GamificationViewModel(
        gamificationStore = mockk<GamificationStore> {
            every { read() } returns stats
        },
        settings = mockk<SettingsStore> {
            every { languageCode } returns code
        },
    )

    @Test
    fun `level dan XP dihitung dari total XP`() = runTest {
        val viewModel = vm(stats = GamificationStats(xp = 950)) // level 4 (0/100/400/900)
        viewModel.refresh()

        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertThat(state.xp).isEqualTo(950)
            assertThat(state.level).isEqualTo(4)
        }
    }

    @Test
    fun `streak dan XP hari ini tampil jika aktif hari ini`() = runTest {
        val today = LocalDate.now().toEpochDay()
        val viewModel = vm(
            stats = GamificationStats(xp = 250, todayXp = 30, lastActiveDay = today, streak = 7),
        )
        viewModel.refresh()

        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertThat(state.todayXp).isEqualTo(30)
            assertThat(state.streak).isEqualTo(7)
            assertThat(state.dailyGoalXp).isEqualTo(Gamification.DAILY_GOAL_XP)
        }
    }

    @Test
    fun `XP hari kemarin tidak dihitung sebagai XP hari ini`() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toEpochDay()
        val viewModel = vm(
            stats = GamificationStats(xp = 100, todayXp = 999, lastActiveDay = yesterday, streak = 3),
        )
        viewModel.refresh()

        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertThat(state.todayXp).isEqualTo(0)
            assertThat(state.streak).isEqualTo(3)
        }
    }

    @Test
    fun `badge terakhir tampil dari peta tier`() = runTest {
        val viewModel = vm(
            stats = GamificationStats(badgeTiers = linkedMapOf("first_ayah" to 1, "streak_7" to 2)),
        )
        viewModel.refresh()

        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertThat(state.earnedBadgeCount).isEqualTo(2)
            assertThat(state.latestBadgeKey).isEqualTo("streak_7")
            assertThat(state.latestBadgeTier).isEqualTo(2)
        }
    }

    @Test
    fun `tanpa data - default aman`() = runTest {
        val viewModel = vm()
        viewModel.refresh()

        viewModel.state.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()

            assertThat(state.xp).isEqualTo(0)
            assertThat(state.level).isEqualTo(1)
            assertThat(state.streak).isEqualTo(0)
            assertThat(state.earnedBadgeCount).isEqualTo(0)
        }
    }
}
