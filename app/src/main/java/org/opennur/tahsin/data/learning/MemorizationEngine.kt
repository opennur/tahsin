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

/** Pure, deterministic spaced-review rules for the first memorization track. */
object MemorizationEngine {

    fun startingCards(surah: Int, ayahCount: Int): List<MemorizationCard> {
        if (ayahCount <= 0) return emptyList()
        return (1..ayahCount).map { ayah -> MemorizationCard(surah = surah, ayah = ayah) }
    }

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
