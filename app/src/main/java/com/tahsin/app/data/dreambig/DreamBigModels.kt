package com.tahsin.app.data.dreambig

/**
 * Satu video dalam playlist "Dream BIG: 10-Day Live Arabic Intensive"
 * (hasil scraper `tools/scrape_dreambig.py` → `assets/dreambig/index.json`).
 */
data class DreamBigVideo(
    val videoId: String,
    val day: Int,
    val part: Int,
    val title: String,
    val watchUrl: String,
    /** Path asset transkrip, relatif ke `assets/` (mis. `dreambig/transcripts/xxx.json`). */
    val transcript: String,
) {
    /** Label "Day 2 · Part 1" (part 0 tidak ditampilkan). */
    val dayLabel: String
        get() = if (part > 0) "Day $day · Part $part" else "Day $day"
}

/**
 * Satu baris caption ASR YouTube (dinormalisasi `tools/scrape_dreambig.py`).
 * `durationMs` = jarak ke baris berikutnya (null untuk baris terakhir).
 */
data class TranscriptSegment(
    val startMs: Long,
    val durationMs: Long?,
    val text: String,
)

/** Transkrip satu video (`assets/dreambig/transcripts/<videoId>.json`). */
data class DreamBigTranscript(
    val videoId: String,
    val source: String,
    val durationMs: Long?,
    val segments: List<TranscriptSegment>,
)

/**
 * Paragraf hasil pengelompokan baris caption untuk dibaca (bukan per baris
 * mentah ASR). Baris yang berjarak ≤ [gapMs] dari akhir baris sebelumnya
 * digabung dengan spasi.
 */
data class TranscriptParagraph(
    val startMs: Long,
    val text: String,
)

/**
 * Satu level game "Dream BIG" (Day 1..10): kumpulan kunci kosakata
 * (`assets/dreambig/levels.json`, dihasilkan `tools/dreambig_levels.py`).
 * Kunci merujuk `VocabEntry.key` di `assets/quran/vocab.json`.
 */
data class DreamBigLevel(
    val day: Int,
    val title: String,
    val wordKeys: List<String>,
)
