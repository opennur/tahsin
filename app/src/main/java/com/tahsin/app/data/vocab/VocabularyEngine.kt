package com.tahsin.app.data.vocab

import com.tahsin.app.util.AppLanguage
import kotlin.random.Random

/** Kotak SRS (Leitner): 0 = baru/lupa, makin tinggi makin lama intervalnya. */
data class VocabCard(
    val key: String,
    val box: Int = 0,
    /** Epoch millis kapan kartu wajib diulang; 0 = belum pernah → selalu jatuh tempo. */
    val nextDue: Long = 0L,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

/** Progres harian belajar (direset saat tanggal berganti). */
data class VocabDaily(
    val date: String = "",
    val studied: Int = 0,
    val learned: Int = 0,
)

/** Keadaan lengkap: kartu + progres harian (dipakai store & ViewModel). */
data class VocabState(
    val cards: Map<String, VocabCard> = emptyMap(),
    val daily: VocabDaily = VocabDaily(),
)

/** Satu soal kuis kosa kata (pilihan ganda, 4 opsi). */
data class VocabQuizQuestion(
    /** Teks pertanyaan: kata Arab (mode depan) atau arti (mode balik). */
    val prompt: String,
    /** Transliterasi kata (mode depan saja; kosong di mode balik). */
    val promptTranslit: String,
    /** 4 opsi jawaban (arti atau kata sesuai mode). */
    val options: List<String>,
    /** Index jawaban benar di [options]. */
    val correctIndex: Int,
    /** Kunci entri target (untuk pencatatan hasil). */
    val answerKey: String,
    /** Contoh kemunculan kata (untuk konteks / audio). */
    val example: VocabExample?,
)

/**
 * Mesin belajar kosa kata — MURNI, tanpa Android, bisa di-unit-test.
 *
 * - SRS Leitner ringan: `box` 0..5, interval 0/1/3/7/14/30 hari.
 * - Pemilihan sesi: kartu jatuh tempo dulu (tertua), lalu kata baru
 *   (urutan frekuensi menurun).
 * - Kuis pilihan ganda anti-ambigu: pengecoh dijamin artinya/kata berbeda
 *   dari target dan dari satu sama lain.
 */
object VocabularyEngine {

    private const val DAY_MS = 86_400_000L
    const val MAX_BOX = 5

    /** Interval (hari) per kotak SRS. */
    fun intervalDays(box: Int): Long = when (box) {
        0 -> 0L
        1 -> 1L
        2 -> 3L
        3 -> 7L
        4 -> 14L
        else -> 30L
    }

    /** Kunci tanggal harian (zona lokal perangkat). */
    fun dayKey(now: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        return "%04d-%02d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    fun isDue(card: VocabCard, now: Long): Boolean = card.nextDue <= now

    /** "Masih ingat" → naik kotak; "Lupa" → kembali ke kotak 0 (ulang hari ini). */
    fun remember(card: VocabCard, now: Long): VocabCard {
        val box = (card.box + 1).coerceAtMost(MAX_BOX)
        return card.copy(
            box = box,
            nextDue = now + intervalDays(box) * DAY_MS,
            correctCount = card.correctCount + 1,
        )
    }

    fun forget(card: VocabCard, now: Long): VocabCard =
        card.copy(box = 0, nextDue = now, wrongCount = card.wrongCount + 1)

    /** Arti entri sesuai bahasa aktif. */
    fun meaningOf(entry: VocabEntry, lang: AppLanguage): String =
        if (lang == AppLanguage.ID) entry.meaningId else entry.meaningEn

    /**
     * Pilih sesi belajar: kartu jatuh tempo (tertua dulu, maks [dueLimit]),
     * lalu kata baru (urutan frekuensi, maks [newLimit]).
     */
    fun selectSession(
        entries: List<VocabEntry>,
        cards: Map<String, VocabCard>,
        now: Long,
        newLimit: Int = 5,
        dueLimit: Int = 10,
    ): List<VocabEntry> {
        val due = entries.filter { entry ->
            cards[entry.key]?.let { isDue(it, now) } == true
        }.sortedWith(
            compareBy({ cards.getValue(it.key).nextDue }, { -it.freq }),
        ).take(dueLimit)

        val dueKeys = due.mapTo(mutableSetOf()) { it.key }
        val fresh = entries.filter { entry -> entry.key !in cards && entry.key !in dueKeys }
            .take(newLimit)
        return due + fresh
    }

    /**
     * Generate satu soal pilihan ganda.
     *
     * @param reverse false → "apa arti <kata>?" (opsi = arti);
     *                true  → "kata mana yang artinya <arti>?" (opsi = kata).
     * @return null kalau kolam pengecoh kurang (dataset terlalu kecil).
     */
    fun makeQuiz(
        entries: List<VocabEntry>,
        target: VocabEntry,
        lang: AppLanguage,
        reverse: Boolean = false,
        random: Random = Random.Default,
    ): VocabQuizQuestion? {
        val targetMeaning = meaningOf(target, lang)
        // Kolam pengecoh: kunci & arti berbeda dari target, arti/kata unik.
        val pool = entries.filter { entry ->
            entry.key != target.key &&
                meaningOf(entry, lang).isNotBlank() &&
                meaningOf(entry, lang) != targetMeaning
        }.distinctBy { if (reverse) it.word else meaningOf(it, lang) }
        if (pool.size < 3) return null

        val distractors = pool.shuffled(random).take(3)
        val correct: String = if (reverse) target.word else targetMeaning
        val wrong: List<String> = distractors.map { if (reverse) it.word else meaningOf(it, lang) }
        val options = (listOf(correct) + wrong).shuffled(random)

        return VocabQuizQuestion(
            prompt = if (reverse) targetMeaning else target.word,
            promptTranslit = if (reverse) "" else target.translit,
            options = options,
            correctIndex = options.indexOf(correct),
            answerKey = target.key,
            example = target.example.takeIf { it.surah > 0 },
        )
    }
}
