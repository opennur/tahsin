package org.opennur.tahsin.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlin.random.Random

/** Satu identitas soal yang stabil lintas sesi dan perubahan urutan opsi. */
data class QuestionKey(
    val feature: String,
    val id: String,
) {
    val storageKey: String get() = "$feature::$id"
}

/** Riwayat paparan satu soal; jawaban salah mendapat review berikutnya. */
data class QuestionExposure(
    val cycle: Int = -1,
    val shownCount: Int = 0,
    val attempts: Int = 0,
    val correctCount: Int = 0,
    val lastCorrect: Boolean? = null,
    val nextReviewDay: Long = Long.MAX_VALUE,
    val lastShownDay: Long = 0L,
)

data class QuestionLedgerState(
    val cycles: Map<String, Int> = emptyMap(),
    val exposures: Map<String, QuestionExposure> = emptyMap(),
)

/** Pure selection policy: review mistakes first, then unseen items in a cycle. */
object QuestionLedger {
    fun select(
        feature: String,
        ids: List<String>,
        state: QuestionLedgerState,
        count: Int,
        today: Long,
        random: Random = Random.Default,
    ): List<String> {
        if (count <= 0 || ids.isEmpty()) return emptyList()
        val unique = ids.distinct()
        val cycle = state.cycles[feature] ?: 0
        val due = unique.filter { id ->
            val exposure = state.exposures[QuestionKey(feature, id).storageKey]
            exposure?.lastCorrect == false && exposure.nextReviewDay <= today
        }.shuffled(random)
        val dueSelected = due.take(count)
        if (dueSelected.size == count) return dueSelected

        val remainingCount = count - dueSelected.size
        val fresh = unique.filter { id ->
            val exposure = state.exposures[QuestionKey(feature, id).storageKey]
            exposure?.cycle != cycle && id !in dueSelected
        }.shuffled(random)
        val freshSelected = fresh.take(remainingCount)
        if (dueSelected.size + freshSelected.size >= count) return dueSelected + freshSelected

        // All questions in this feature have completed the current cycle.
        return (dueSelected + freshSelected + unique.filter { it !in dueSelected && it !in freshSelected }
            .shuffled(random)).take(count)
    }

    fun reserve(
        feature: String,
        ids: List<String>,
        state: QuestionLedgerState,
        count: Int,
        today: Long,
        random: Random = Random.Default,
    ): Pair<QuestionLedgerState, List<String>> {
        val cycle = state.cycles[feature] ?: 0
        val unique = ids.distinct()
        val selected = select(feature, unique, state, count, today, random)
        val allSeenAfterReservation = unique.all { id ->
            id in selected || state.exposures[QuestionKey(feature, id).storageKey]?.cycle == cycle
        }
        val nextCycle = if (allSeenAfterReservation && selected.isNotEmpty()) cycle + 1 else cycle
        val exposures = state.exposures.toMutableMap()
        selected.forEach { id ->
            val key = QuestionKey(feature, id).storageKey
            val old = exposures[key] ?: QuestionExposure()
            val reservedCycle = if (old.cycle == cycle || old.cycle < cycle) cycle else old.cycle
            exposures[key] = old.copy(
                cycle = reservedCycle,
                shownCount = old.shownCount + 1,
                lastShownDay = today,
            )
        }
        // The cycle advances only after the current pool was exhausted; the
        // next call then sees every item as fresh again.
        val cycles = state.cycles + (feature to nextCycle)
        return QuestionLedgerState(cycles = cycles, exposures = exposures) to selected
    }

    fun record(
        feature: String,
        id: String,
        state: QuestionLedgerState,
        correct: Boolean,
        today: Long,
    ): QuestionLedgerState {
        val key = QuestionKey(feature, id).storageKey
        val old = state.exposures[key] ?: QuestionExposure()
        val nextReview = if (correct) Long.MAX_VALUE else today + 1
        return state.copy(
            exposures = state.exposures + (
                key to old.copy(
                    attempts = old.attempts + 1,
                    correctCount = old.correctCount + if (correct) 1 else 0,
                    lastCorrect = correct,
                    nextReviewDay = nextReview,
                )
            ),
        )
    }
}

/** File-backed ledger shared by every quiz and challenge. */
class QuestionExposureStore internal constructor(private val file: File) {
    companion object {
        private val LOCK = Any()
        fun fromContext(context: Context): QuestionExposureStore =
            QuestionExposureStore(File(context.applicationContext.filesDir, "question-history.json"))
    }

    private val gson = Gson()
    private val type = object : TypeToken<QuestionLedgerState>() {}.type

    fun read(): QuestionLedgerState = synchronized(LOCK) {
        runCatching {
            if (!file.exists()) QuestionLedgerState()
            else gson.fromJson<QuestionLedgerState>(file.readText(), type) ?: QuestionLedgerState()
        }.getOrDefault(QuestionLedgerState())
    }

    fun reserve(
        feature: String,
        ids: List<String>,
        count: Int,
        today: Long,
        random: Random = Random.Default,
    ): List<String> = synchronized(LOCK) {
        val (next, selected) = QuestionLedger.reserve(feature, ids, read(), count, today, random)
        writeLocked(next)
        selected
    }

    fun record(feature: String, id: String, correct: Boolean, today: Long) = synchronized(LOCK) {
        writeLocked(QuestionLedger.record(feature, id, read(), correct, today))
    }

    private fun writeLocked(state: QuestionLedgerState) {
        runCatching {
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(gson.toJson(state))
            if (!temp.renameTo(file)) {
                file.writeText(gson.toJson(state))
                temp.delete()
            }
        }
    }
}
