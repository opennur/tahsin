package org.opennur.tahsin.data.learning

/** One ayah in the local memorization review queue. */
data class MemorizationCard(
    val surah: Int,
    val ayah: Int,
    val dueDay: Long = 0L,
    val intervalDays: Int = 0,
    val attempts: Int = 0,
    val successes: Int = 0,
)

/** Target pilihan hafalan: surah tertentu atau rentang juz. */
sealed interface MemorizationTarget {
    data class Surah(val number: Int) : MemorizationTarget
    data class Juz(val number: Int) : MemorizationTarget
}

/** Pure, deterministic spaced-review rules for the first memorization track. */
object MemorizationEngine {

    fun startingCards(surah: Int, ayahCount: Int): List<MemorizationCard> {
        if (ayahCount <= 0) return emptyList()
        return (1..ayahCount).map { ayah -> MemorizationCard(surah = surah, ayah = ayah) }
    }

    /**
     * Seed cards for a juz range using the provided surah metadata.
     *
     * @param juz Juz number (1..30)
     * @param juzStarts List of JuzStart from MushafPagination
     * @param surahAyahCounts Map of surah number → ayah count (from Surah list)
     * @return Cards for all ayahs in the juz, in surah:ayah order
     */
    fun startingCardsForJuz(
        juz: Int,
        juzStarts: List<JuzStartRef>,
        surahAyahCounts: Map<Int, Int>,
    ): List<MemorizationCard> {
        val start = juzStarts.firstOrNull { it.juz == juz } ?: return emptyList()
        val end = juzStarts.firstOrNull { it.juz == juz + 1 }

        val cards = mutableListOf<MemorizationCard>()
        for (surah in start.surah..(end?.surah ?: 114)) {
            val ayahCount = surahAyahCounts[surah] ?: continue
            val fromAyah = if (surah == start.surah) start.ayah else 1
            val toAyah = if (end != null && surah == end.surah) end.ayah - 1 else ayahCount
            for (ayah in fromAyah..toAyah) {
                cards.add(MemorizationCard(surah = surah, ayah = ayah))
            }
        }
        return cards
    }

    /** Lightweight ref for juz start data (avoids importing MushafPages). */
    data class JuzStartRef(val juz: Int, val surah: Int, val ayah: Int)

    fun isDue(card: MemorizationCard, day: Long): Boolean = card.dueDay <= day

    /** Due cards first; otherwise show the nearest scheduled card so the screen is never empty. */
    fun selectNext(cards: List<MemorizationCard>, day: Long): MemorizationCard? {
        if (cards.isEmpty()) return null
        return cards
            .filter { isDue(it, day) }
            .minWithOrNull(compareBy<MemorizationCard> { it.dueDay }.thenBy { it.surah }.thenBy { it.ayah })
            ?: cards.minWithOrNull(compareBy<MemorizationCard> { it.dueDay }.thenBy { it.surah }.thenBy { it.ayah })
    }

    fun remember(card: MemorizationCard, day: Long): MemorizationCard {
        val nextInterval = when {
            card.intervalDays <= 0 -> 1
            card.intervalDays < 3 -> 3
            card.intervalDays < 7 -> 7
            else -> (card.intervalDays * 2).coerceAtMost(30)
        }
        return card.copy(
            dueDay = day + nextInterval,
            intervalDays = nextInterval,
            attempts = card.attempts + 1,
            successes = card.successes + 1,
        )
    }

    fun needReview(card: MemorizationCard, day: Long): MemorizationCard = card.copy(
        dueDay = day,
        intervalDays = 0,
        attempts = card.attempts + 1,
    )
}
