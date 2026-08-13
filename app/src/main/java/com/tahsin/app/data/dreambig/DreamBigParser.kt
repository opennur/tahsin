package com.tahsin.app.data.dreambig

import com.google.gson.Gson

/**
 * Parsing `assets/dreambig/index.json` + `assets/dreambig/transcripts/<videoId>.json` —
 * murni JVM, tanpa Context, bisa di-unit-test. I/O assets ditangani
 * [DreamBigRepository] (path di index.json relatif ke root `assets/`).
 *
 * DTO memakai field nullable: Gson tidak memanggil konstruktor (unsafe
 * allocation), jadi field yang hilang bernilai null/0 — mapping eksplisit
 * agar model tidak pernah membawa null.
 */
object DreamBigParser {

    private val gson = Gson()

    /** Parse indeks playlist; JSON rusak/kosong → daftar kosong. */
    fun parseIndex(json: String): List<DreamBigVideo> {
        val parsed = runCatching { gson.fromJson(json, IndexJson::class.java) }.getOrNull()
            ?: return emptyList()
        return parsed.videos.orEmpty().map { it.toVideo() }
    }

    /** Parse transkrip satu video; JSON rusak → transkrip kosong (tidak crash). */
    fun parseTranscript(json: String): DreamBigTranscript {
        val parsed = runCatching { gson.fromJson(json, TranscriptJson::class.java) }.getOrNull()
            ?: return DreamBigTranscript("", "", null, emptyList())
        return DreamBigTranscript(
            videoId = parsed.videoId.orEmpty(),
            source = parsed.source.orEmpty(),
            durationMs = parsed.durationMs,
            segments = parsed.segments.orEmpty().mapNotNull { it.toSegment() },
        )
    }

    /** Parse level game; JSON rusak/kosong → daftar kosong. */
    fun parseLevels(json: String): List<DreamBigLevel> {
        val parsed = runCatching { gson.fromJson(json, LevelsJson::class.java) }.getOrNull()
            ?: return emptyList()
        return parsed.levels.orEmpty().mapNotNull { l ->
            val day = l.day
            if (day <= 0) null
            else DreamBigLevel(day = day, title = l.title.orEmpty(), wordKeys = l.wordKeys.orEmpty())
        }
    }

    /**
     * Kelompokkan baris caption jadi paragraf bacaan: baris berikutnya
     * digabung bila mulai ≤ [gapMs] dari akhir baris sebelumnya.
     * Baris kosong diabaikan.
     */
    fun paragraphs(
        segments: List<TranscriptSegment>,
        gapMs: Long = 1_500L,
    ): List<TranscriptParagraph> {
        val out = mutableListOf<TranscriptParagraph>()
        var lastEnd = Long.MIN_VALUE
        for (seg in segments) {
            val text = seg.text.trim()
            if (text.isEmpty()) continue
            val gap = if (out.isEmpty()) Long.MAX_VALUE else seg.startMs - lastEnd
            if (out.isNotEmpty() && gap <= gapMs) {
                val prev = out[out.size - 1]
                out[out.size - 1] = prev.copy(text = prev.text + " " + text)
            } else {
                out.add(TranscriptParagraph(seg.startMs, text))
            }
            lastEnd = seg.startMs + (seg.durationMs ?: gapMs)
        }
        return out
    }

    // ---- DTO (nullable, aman untuk field hilang) ----

    private data class IndexJson(val videos: List<VideoJson>? = null)

    private data class VideoJson(
        val videoId: String? = null,
        val day: Int = 0,
        val part: Int = 0,
        val title: String? = null,
        val watchUrl: String? = null,
        val transcript: String? = null,
    ) {
        fun toVideo() = DreamBigVideo(
            videoId = videoId.orEmpty(),
            day = day,
            part = part,
            title = title.orEmpty(),
            watchUrl = watchUrl.orEmpty(),
            transcript = transcript.orEmpty(),
        )
    }

    private data class TranscriptJson(
        val videoId: String? = null,
        val source: String? = null,
        val durationMs: Long? = null,
        val segments: List<SegmentJson>? = null,
    )

    private data class LevelsJson(val levels: List<LevelJson>? = null)

    private data class LevelJson(
        val day: Int = 0,
        val title: String? = null,
        val wordKeys: List<String>? = null,
    )

    private data class SegmentJson(
        val startMs: Long = 0L,
        val durationMs: Long? = null,
        val text: String? = null,
    ) {
        fun toSegment(): TranscriptSegment? {
            val t = text?.trim().orEmpty()
            if (t.isEmpty()) return null
            return TranscriptSegment(startMs = startMs, durationMs = durationMs, text = t)
        }
    }
}
