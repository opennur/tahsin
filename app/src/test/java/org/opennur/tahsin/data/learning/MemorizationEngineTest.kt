package org.opennur.tahsin.data.learning

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MemorizationEngineTest {

    @Test
    fun `starting cards handles empty and creates ordered ayahs`() {
        assertThat(MemorizationEngine.startingCards(1, 0)).isEmpty()
        assertThat(MemorizationEngine.startingCards(1, 3)).containsExactly(
            MemorizationCard(1, 1),
            MemorizationCard(1, 2),
            MemorizationCard(1, 3),
        ).inOrder()
    }

    @Test
    fun `due and selection prefer due then earliest scheduled card`() {
        val future = MemorizationCard(1, 2, dueDay = 20)
        val dueLaterAyah = MemorizationCard(1, 3, dueDay = 5)
        val dueEarlierAyah = MemorizationCard(1, 1, dueDay = 5)

        assertThat(MemorizationEngine.isDue(dueEarlierAyah, 5)).isTrue()
        assertThat(MemorizationEngine.isDue(future, 5)).isFalse()
        assertThat(MemorizationEngine.selectNext(emptyList(), 5)).isNull()
        assertThat(MemorizationEngine.selectNext(listOf(future), 5)).isEqualTo(future)
        assertThat(
            MemorizationEngine.selectNext(listOf(future, dueLaterAyah, dueEarlierAyah), 5),
        ).isEqualTo(dueEarlierAyah)
    }

    @Test
    fun `remember grows intervals through every scheduling band`() {
        val first = MemorizationEngine.remember(MemorizationCard(1, 1), 10)
        assertThat(first.intervalDays).isEqualTo(1)
        assertThat(first.dueDay).isEqualTo(11)

        val short = MemorizationEngine.remember(first, 10)
        assertThat(short.intervalDays).isEqualTo(3)

        val medium = MemorizationEngine.remember(short, 10)
        assertThat(medium.intervalDays).isEqualTo(7)

        val long = MemorizationEngine.remember(medium.copy(intervalDays = 10), 10)
        assertThat(long.intervalDays).isEqualTo(20)
        val capped = MemorizationEngine.remember(long.copy(intervalDays = 20), 10)
        assertThat(capped.intervalDays).isEqualTo(30)
        assertThat(capped.attempts).isEqualTo(5)
        assertThat(capped.successes).isEqualTo(5)
    }

    @Test
    fun `need review keeps ayah immediately due and does not add success`() {
        val card = MemorizationCard(1, 1, dueDay = 20, intervalDays = 7, attempts = 2, successes = 1)
        val reviewed = MemorizationEngine.needReview(card, 10)

        assertThat(reviewed.dueDay).isEqualTo(10)
        assertThat(reviewed.intervalDays).isEqualTo(0)
        assertThat(reviewed.attempts).isEqualTo(3)
        assertThat(reviewed.successes).isEqualTo(1)
    }
}
