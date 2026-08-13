package org.opennur.tahsin.util

import org.opennur.tahsin.stt.AlignedWord
import org.opennur.tahsin.stt.WordStatus

/**
 * Statistik kumulatif satu ayat (kunci: surahNumber + ayahNumber 1-based).
 *
 * Dibuat murni (tanpa Android) supaya logika skor & penggabungan bisa diuji JVM.
 */
data class AyahStats(
    val surahNumber: Int,
    val ayahNumber: Int,
    /** Jumlah percobaan (hasil FINAL STT yang benar-benar ada ucapan). */
    val attempts: Int = 0,
    /** Akumulasi skor semua percobaan — rata-rata = scoreSum / attempts. */
    val scoreSum: Int = 0,
    val bestScore: Int = 0,
    val lastScore: Int = 0,
    val lastPracticedAt: Long = 0L,
    /** Kata yang pernah salah/terlewat, errorCount = berapa kali (terurut menurun). */
    val wordErrors: List<WordError> = emptyList(),
) {
    /** Rata-rata skor 0..100 (0 kalau belum ada percobaan). */
    val avgScore: Int get() = if (attempts > 0) scoreSum / attempts else 0
}

/** Satu kata yang sering salah/terlewat pada satu ayat. */
data class WordError(
    val wordIndex: Int,
    /** Bentuk mushaf kata (untuk ditampilkan). */
    val word: String,
    /** Berapa kali kata ini salah/terlewat dari semua percobaan. */
    val errorCount: Int,
)

/** Logika skor & agregasi riwayat bacaan (murni — bisa diuji tanpa Android). */
object ReadingStats {

    /**
     * Skor 0..100 untuk satu hasil bacaan: proporsi kata yang benar
     * (MISMATCH/SKIPPED/NOT_REACHED dianggap belum benar).
     */
    fun scoreOf(aligned: List<AlignedWord>): Int {
        if (aligned.isEmpty()) return 0
        val correct = aligned.count { it.status == WordStatus.CORRECT }
        return (correct * 100.0 / aligned.size).toInt()
    }

    /**
     * Gabungkan satu hasil bacaan final ke statistik ayat.
     * Kata salah/terlewat (MISMATCH/SKIPPED) dihitung +1 per percobaan.
     * `now` parameter agar pengujian deterministik.
     */
    fun merge(
        surahNumber: Int,
        ayahNumber: Int,
        existing: AyahStats?,
        aligned: List<AlignedWord>,
        referenceWords: List<String>,
        now: Long = System.currentTimeMillis(),
    ): AyahStats {
        val score = scoreOf(aligned)
        val base = existing ?: AyahStats(surahNumber = surahNumber, ayahNumber = ayahNumber)

        // Kata yang salah/terlewat pada percobaan ini (satu kata dihitung sekali).
        val hitErrors = aligned
            .filter { it.status == WordStatus.MISMATCH || it.status == WordStatus.SKIPPED }
            .associate { alignedWord ->
                alignedWord.index to alignedWord.referenceWord
                    .ifBlank { referenceWords.getOrNull(alignedWord.index).orEmpty() }
                    // Pertahanan: kalau keduanya kosong, jangan tampilkan label kosong.
                    .ifBlank { "…" }
            }

        val mergedErrors = linkedMapOf<Int, WordError>()
        base.wordErrors.forEach { mergedErrors[it.wordIndex] = it }
        hitErrors.forEach { (idx, word) ->
            val cur = mergedErrors[idx]
            mergedErrors[idx] = WordError(
                wordIndex = idx,
                word = if (cur != null && cur.word.isNotBlank()) cur.word else word,
                errorCount = (cur?.errorCount ?: 0) + 1,
            )
        }

        return AyahStats(
            surahNumber = base.surahNumber,
            ayahNumber = base.ayahNumber,
            attempts = base.attempts + 1,
            scoreSum = base.scoreSum + score,
            bestScore = maxOf(base.bestScore, score),
            lastScore = score,
            lastPracticedAt = now,
            wordErrors = mergedErrors.values.sortedByDescending { it.errorCount },
        )
    }
}
