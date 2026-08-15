package org.opennur.tahsin.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.opennur.tahsin.data.dreambig.DreamBigGame
import org.opennur.tahsin.data.lughoh.LughohEngine
import org.opennur.tahsin.data.quran.Ayah
import org.opennur.tahsin.data.quran.MushafPagination
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.data.quran.Surah
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.DreamBigProgressStore
import org.opennur.tahsin.util.DreamBigStats
import org.opennur.tahsin.util.GamificationStore
import org.opennur.tahsin.util.LughohProgressStore
import org.opennur.tahsin.util.LughohStats
import org.opennur.tahsin.util.LearningPlanStore
import org.opennur.tahsin.util.ReadingHistoryStore
import org.opennur.tahsin.util.ReadingStatsStore
import org.opennur.tahsin.util.SearchableAyah
import org.opennur.tahsin.util.SettingsSource
import org.opennur.tahsin.util.StatsStores
import org.opennur.tahsin.util.VocabularyStatsStore
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

/** Tes agregasi StatsViewModel dengan store file temp + fake repository. */
class StatsViewModelTest {

    private lateinit var dir: File

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dir = Files.createTempDirectory("stats-vm-test").toFile()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        dir.deleteRecursively()
    }

    private fun settings(code: String) = object : SettingsSource {
        override val languageCode: String = code
    }

    private fun fakeRepo(vararg surahs: Surah): QuranRepository = object : QuranRepository {
        override fun surahList(): List<Surah> = surahs.toList()
        override fun pagination(): MushafPagination =
            MushafPagination(1, 0, emptyList(), emptyList())
        override fun cachedSurahPlain(number: Int): Surah? =
            surahs.firstOrNull { it.number == number }
        override suspend fun cachedSurah(number: Int, lang: AppLanguage): Surah? =
            surahs.firstOrNull { it.number == number }
        override suspend fun fetchSurah(number: Int, lang: AppLanguage): Surah =
            surahs.firstOrNull { it.number == number } ?: error("no surah $number")
        override suspend fun searchIndex(): List<SearchableAyah> = emptyList()
    }

    private fun vm(code: String = "id", repo: QuranRepository = fakeRepo()): StatsViewModel =
        StatsViewModel(
            stores = StatsStores(
                readingStats = ReadingStatsStore(File(dir, "reading_stats.json")),
                vocabularyStats = VocabularyStatsStore(File(dir, "vocab_stats.json")),
                dreamBig = DreamBigProgressStore(File(dir, "dream_big.json")),
                lughoh = LughohProgressStore(File(dir, "lughoh.json")),
                gamification = GamificationStore(File(dir, "gamification.json")),
                readingHistory = ReadingHistoryStore(File(dir, "reading_history.json")),
                learningPlan = LearningPlanStore(File(dir, "learning_plan.json")),
            ),
            repository = repo,
            settings = settings(code),
        )

    private fun awaitState(state: StateFlow<StatsState>): StatsState = runBlocking {
        withTimeout(5_000) { state.first { !it.isLoading } }
    }

    @Test
    fun `agregasi angka gabungan dari store`() {
        val dream = DreamBigProgressStore(File(dir, "dream_big.json"))
        dream.write(DreamBigStats(bestScore = 8, roundsPlayed = 3))
        val lughoh = LughohProgressStore(File(dir, "lughoh.json"))
        lughoh.write(LughohStats(bestScore = 6, roundsPlayed = 2))

        val v = vm()
        v.refresh()
        val state = awaitState(v.state)

        assertEquals(3, state.dreamBigRounds)
        assertEquals(8, state.dreamBigBest)
        assertEquals(2, state.lughohRounds)
        assertEquals(6, state.lughohBest)
        // dream 8/10 ronde = 80%; lughoh 6/8 = 75% → max = 80.
        assertEquals(80, state.bestScorePct)
        assertEquals(5, state.totalRounds)
    }

    @Test
    fun `riwayat baca + nama surah diisi dari repository`() {
        val repo = fakeRepo(Surah(2, "البقرة", "Al-Baqarah", 286, emptyList()))
        val history = ReadingHistoryStore(File(dir, "reading_history.json"))
        history.record(2, 255, 100L)

        val v = vm(repo = repo)
        v.refresh()
        val state = awaitState(v.state)

        assertEquals(1, state.history.size)
        assertEquals("Al-Baqarah", state.surahNames[2])
        assertEquals(AppLanguage.ID, state.language)
    }

    @Test
    fun `bahasa mengikuti settings - EN`() {
        val v = vm(code = "en")
        v.refresh()
        val state = awaitState(v.state)
        assertEquals(AppLanguage.EN, state.language)
    }

    @Test
    fun `tanpa data - state kosong tidak crash`() {
        val v = vm()
        v.refresh()
        val state = awaitState(v.state)
        assertEquals(0, state.totalSessions)
        assertEquals(0, state.wordsMastered)
        assertTrue(state.history.isEmpty())
        assertEquals(0, state.bestScorePct)
    }

    @Test
    fun `rencana harian tampil di statistik`() {
        LearningPlanStore(File(dir, "learning_plan.json")).markComplete(
            day = LocalDate.now().toEpochDay(),
            goalKey = "recitation",
            taskKey = "recite",
        )

        val v = vm()
        v.refresh()
        val state = awaitState(v.state)

        assertEquals(1, state.dailyPlanCompleted)
        assertEquals(3, state.dailyPlanTotal)
    }
}
