package org.opennur.tahsin.data.dreambig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tes parsing & pengelompokan data Dream BIG (murni JVM). */
class DreamBigParserTest {

    private val indexJson = """
        {"playlistId":"PLutdSTmJ7bAIApzbo3C9vu1eWsMh2ZyUj","title":"Dream BIG",
         "source":"youtube-auto-subs (en, asr)","videos":[
          {"videoId":"XU4q9RmLj38","day":1,"part":0,
           "title":"Dream BIG: Arabic Intensive - Day 1",
           "watchUrl":"https://www.youtube.com/watch?v=XU4q9RmLj38",
           "transcript":"dreambig/transcripts/XU4q9RmLj38.json"},
          {"videoId":"iZ0O_G8xq2s","day":2,"part":1,
           "title":"Dream BIG: Arabic Intensive - Day 2 (Part 1)",
           "watchUrl":"https://www.youtube.com/watch?v=iZ0O_G8xq2s",
           "transcript":"dreambig/transcripts/iZ0O_G8xq2s.json"}
        ]}
    """.trimIndent()

    private val transcriptJson = """
        {"videoId":"XU4q9RmLj38","source":"youtube-auto-subs (en, asr)","durationMs":7887020,
         "segments":[
          {"startMs":4740,"durationMs":2760,"text":"welcome to day one of"},
          {"startMs":7500,"durationMs":1980,"text":"the dream big intensive"},
          {"startMs":9480,"durationMs":2100,"text":"  take a few minutes  "},
          {"startMs":11580,"durationMs":null,"text":""}
        ]}
    """.trimIndent()

    @Test
    fun `parseIndex - semua field terbaca + dayLabel`() {
        val videos = DreamBigParser.parseIndex(indexJson)
        assertEquals(2, videos.size)
        val day1 = videos[0]
        assertEquals("XU4q9RmLj38", day1.videoId)
        assertEquals(1, day1.day)
        assertEquals(0, day1.part)
        assertEquals("Dream BIG: Arabic Intensive - Day 1", day1.title)
        assertEquals("https://www.youtube.com/watch?v=XU4q9RmLj38", day1.watchUrl)
        assertEquals("dreambig/transcripts/XU4q9RmLj38.json", day1.transcript)
        assertEquals("Day 1", day1.dayLabel)
        assertEquals("Day 2 · Part 1", videos[1].dayLabel)
    }

    @Test
    fun `parseIndex - JSON rusak atau videos null - daftar kosong (tidak crash)`() {
        assertTrue(DreamBigParser.parseIndex("bukan json").isEmpty())
        assertTrue(DreamBigParser.parseIndex("""{"videos":null}""").isEmpty())
        assertTrue(DreamBigParser.parseIndex("").isEmpty())
    }

    @Test
    fun `parseIndex - field hilang memakai default aman`() {
        val videos = DreamBigParser.parseIndex("""{"videos":[{"videoId":"abc"}]}""")
        assertEquals(1, videos.size)
        assertEquals(0, videos[0].day)
        assertEquals(0, videos[0].part)
        assertEquals("", videos[0].watchUrl)
        assertEquals("Day 0", videos[0].dayLabel)
    }

    @Test
    fun `parseTranscript - segmen terbaca, text di-trim, baris kosong dibuang`() {
        val t = DreamBigParser.parseTranscript(transcriptJson)
        assertEquals("XU4q9RmLj38", t.videoId)
        assertEquals("youtube-auto-subs (en, asr)", t.source)
        assertEquals(7887020L, t.durationMs)
        assertEquals(3, t.segments.size)
        assertEquals(TranscriptSegment(4740L, 2760L, "welcome to day one of"), t.segments[0])
        assertEquals(9480L, t.segments[2].startMs)
        assertEquals("take a few minutes", t.segments[2].text) // trim
        assertTrue(t.segments.none { it.text.isEmpty() })
    }

    @Test
    fun `parseTranscript - JSON rusak - transkrip kosong (tidak crash)`() {
        val t = DreamBigParser.parseTranscript("rusak")
        assertEquals("", t.videoId)
        assertTrue(t.segments.isEmpty())
    }

    @Test
    fun `paragraphs - baris berjarak dekat digabung, jauh dipisah`() {
        val segments = listOf(
            TranscriptSegment(0L, 1_000L, "a"),
            TranscriptSegment(1_000L, 1_000L, "b"),   // gap 0 -> gabung
            TranscriptSegment(5_000L, 1_000L, "c"),   // gap 3000 -> paragraf baru
            TranscriptSegment(5_400L, null, "d"),     // gap 400 -> gabung
        )
        val paras = DreamBigParser.paragraphs(segments)
        assertEquals(2, paras.size)
        assertEquals(TranscriptParagraph(0L, "a b"), paras[0])
        assertEquals(TranscriptParagraph(5_000L, "c d"), paras[1])
    }

    @Test
    fun `paragraphs - baris kosong diabaikan, input kosong aman`() {
        val segments = listOf(
            TranscriptSegment(0L, 500L, "  "),
            TranscriptSegment(500L, 500L, "x"),
        )
        assertEquals(listOf(TranscriptParagraph(500L, "x")), DreamBigParser.paragraphs(segments))
        assertTrue(DreamBigParser.paragraphs(emptyList()).isEmpty())
    }

    // ---- levels.json ----

    private val levelsJson = """
        {"levels":[
          {"day":1,"title":"Day 1","wordKeys":["من","قال","الله"]},
          {"day":2,"title":"Day 2","wordKeys":["ان","في","ما"]}
        ]}
    """.trimIndent()

    @Test
    fun `parseLevels - semua field terbaca`() {
        val levels = DreamBigParser.parseLevels(levelsJson)
        assertEquals(2, levels.size)
        assertEquals(1, levels[0].day)
        assertEquals("Day 1", levels[0].title)
        assertEquals(listOf("من", "قال", "الله"), levels[0].wordKeys)
        assertEquals(2, levels[1].day)
    }

    @Test
    fun `parseLevels - JSON rusak atau levels null - daftar kosong (tidak crash)`() {
        assertTrue(DreamBigParser.parseLevels("bukan json").isEmpty())
        assertTrue(DreamBigParser.parseLevels("""{"levels":null}""").isEmpty())
        assertTrue(DreamBigParser.parseLevels("").isEmpty())
    }

    @Test
    fun `parseLevels - day hilang atau tidak valid dilewati, field lain default aman`() {
        val levels = DreamBigParser.parseLevels(
            """{"levels":[{"day":0,"title":"x","wordKeys":["a"]},{"day":3}]}""",
        )
        assertEquals(1, levels.size)
        assertEquals(3, levels[0].day)
        assertEquals("", levels[0].title)
        assertTrue(levels[0].wordKeys.isEmpty())
    }


    @Test
    fun `paragraph - getter startMs konsisten`() {
        val p = org.opennur.tahsin.data.dreambig.TranscriptParagraph(startMs = 1234L, text = "teks")
        assertEquals(1234L, p.startMs)
        assertEquals("teks", p.text)
    }


    @Test
    fun `parseTranscript - json kosong dan segmen teks kosong - aman`() {
        val t1 = DreamBigParser.parseTranscript("{}")
        assertEquals("", t1.videoId)
        assertEquals("", t1.source)
        assertTrue(t1.segments.isEmpty())

        val t2 = DreamBigParser.parseTranscript(
            """{"videoId":"v1","segments":[{"startMs":0,"durationMs":10,"text":"   "}]}""",
        )
        assertEquals("v1", t2.videoId)
        assertTrue(t2.segments.isEmpty()) // teks kosong → segmen dilewati
    }
}
