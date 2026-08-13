package org.opennur.tahsin.stt

import org.opennur.tahsin.util.ArabicNormalizer

/** Status satu kata ayat terhadap bacaan STT. */
enum class WordStatus {
    NOT_REACHED,   // belum terbaca
    READING,       // sedang dibaca (kata terakhir yang cocok sebagian)
    CORRECT,       // cocok dengan teks acuan
    MISMATCH,      // terbaca tapi tidak cocok (potensi kesalahan)
    SKIPPED,       // terlewat (tidak terbaca)
}

/** Satu kata acuan + hasil penyelarasan. */
data class AlignedWord(
    val index: Int,
    val referenceWord: String,
    val status: WordStatus,
    val spokenWord: String? = null,
)

/**
 * Menyelaraskan transkrip STT dengan kata-kata ayat acuan (level kata).
 *
 * Keterbatasan jujur: STT teks tidak bisa menilai makhraj/bunyi. Yang bisa
 * dideteksi dari teks: kata terlewat, huruf/kata salah, atau salah susun —
 * lalu mesin tajwid memberi tahu hukum yang berlaku di kata tersebut.
 */
object TranscriptAligner {

    /** Ambang kemiripan: >= ini dianggap benar. */
    private const val MATCH_THRESHOLD = 0.6
    /** Di antara threshold ini dianggap "sedang dibaca" (partial). */
    private const val READING_THRESHOLD = 0.35

    fun align(spokenRaw: String, reference: List<String>): List<AlignedWord> {
        val spoken = ArabicNormalizer.normalize(spokenRaw)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (spoken.isEmpty()) {
            return reference.mapIndexed { i, w ->
                AlignedWord(i, w, WordStatus.NOT_REACHED)
            }
        }

        val ref = reference.map { ArabicNormalizer.normalize(it) }
        val steps = greedyAlign(ref, spoken)

        // greedyAlign selalu menghasilkan SATU Step per kata acuan (indeks
        // berurutan), jadi iterasi steps langsung — tanpa find/null-arm yang
        // sebenarnya tidak pernah terjadi.
        val result = steps.map { step ->
            val i = step.refIndex
            val w = reference[i]
            when {
                step.spokenIndex == null -> AlignedWord(i, w, WordStatus.SKIPPED)
                step.similarity >= MATCH_THRESHOLD ->
                    AlignedWord(i, w, WordStatus.CORRECT, spoken[step.spokenIndex])
                step.similarity >= READING_THRESHOLD ->
                    AlignedWord(i, w, WordStatus.READING, spoken[step.spokenIndex])
                else ->
                    AlignedWord(i, w, WordStatus.MISMATCH, spoken[step.spokenIndex])
            }
        }
        return result
    }

    // ---- internal ----

    private data class Step(val refIndex: Int, val spokenIndex: Int?, val similarity: Double)

    /**
     * Greedy two-pointer: cocokkan kata acuan dengan kata ucapan secara urut.
     * Kata acuan yang dilewati → SKIPPED; kata ucapan ekstra → diabaikan.
     */
    private fun greedyAlign(ref: List<String>, spoken: List<String>): List<Step> {
        val steps = mutableListOf<Step>()
        var ri = 0
        var si = 0
        while (ri < ref.size && si < spoken.size) {
            val sim = similarity(ref[ri], spoken[si])
            when {
                sim >= READING_THRESHOLD -> {
                    steps += Step(ri, si, sim)
                    ri++
                    si++
                }
                // kata acuan ini dilewati? cek kata ucapan berikutnya
                si + 1 < spoken.size && similarity(ref[ri], spoken[si + 1]) >= READING_THRESHOLD -> {
                    steps += Step(ri, null, 0.0)   // SKIPPED
                    ri++
                }
                else -> {
                    steps += Step(ri, si, sim)     // MISMATCH
                    ri++
                    si++
                }
            }
        }
        while (ri < ref.size) {
            steps += Step(ri, null, 0.0)
            ri++
        }
        return steps
    }

    /** Kemiripan 0..1 berbasis jarak Levenshtein pada string ternormalisasi. */
    fun similarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        val dist = levenshtein(a, b)
        return 1.0 - dist.toDouble() / maxOf(a.length, b.length)
    }

    /** Jarak Levenshtein — internal supaya bisa diuji unit langsung (edge empty). */
    internal fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        // while, bukan `for (i in 0..len)` — kompilator menambah cek `i == len`
        // yang membuat cabang `i > len` tak terjangkau; while bersih 2 cabang.
        var i = 0
        while (i <= a.length) { dp[i][0] = i; i++ }
        var j = 0
        while (j <= b.length) { dp[0][j] = j; j++ }
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1,
                )
            }
        }
        return dp[a.length][b.length]
    }
}
