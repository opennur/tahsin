package org.opennur.tahsin.util

/** Persistence dependencies used together by the aggregate statistics screen. */
data class StatsStores(
    val readingStats: ReadingStatsStore,
    val vocabularyStats: VocabularyStatsStore,
    val dreamBig: DreamBigProgressStore,
    val lughoh: LughohProgressStore,
    val nahwu: NahwuProgressStore? = null,
    val shorof: ShorofProgressStore? = null,
    val questionHistory: QuestionExposureStore? = null,
    val gamification: GamificationStore,
    val readingHistory: ReadingHistoryStore,
    val learningPlan: LearningPlanStore,
)
