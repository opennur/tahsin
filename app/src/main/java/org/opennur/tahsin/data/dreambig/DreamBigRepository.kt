package org.opennur.tahsin.data.dreambig

import android.content.Context

/**
 * Sumber data playlist Dream BIG: `assets/dreambig/index.json` +
 * `transcripts/<videoId>.json` (offline, dihasilkan `tools/scrape_dreambig.py`,
 * di-bundle ke APK). Mengikuti pola [org.opennur.tahsin.data.vocab.VocabularyRepository].
 */
class DreamBigRepository(context: Context) {

    private val appContext = context.applicationContext

    @Volatile
    private var cachedIndex: List<DreamBigVideo>? = null

    @Volatile
    private var cachedLevels: List<DreamBigLevel>? = null

    private val transcriptCache = object : LinkedHashMap<String, DreamBigTranscript>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DreamBigTranscript>?) =
            size > 16
    }

    /** Semua video, urut (day, part) sesuai index.json. */
    fun videos(): List<DreamBigVideo> = cachedIndex ?: synchronized(this) {
        cachedIndex ?: run {
            val list = readAsset("dreambig/index.json")
                .let { if (it == null) emptyList() else DreamBigParser.parseIndex(it) }
            cachedIndex = list
            list
        }
    }

    /** Level game, urut day (1..10) sesuai levels.json. */
    fun levels(): List<DreamBigLevel> = cachedLevels ?: synchronized(this) {
        cachedLevels ?: run {
            val list = readAsset("dreambig/levels.json")
                .let { if (it == null) emptyList() else DreamBigParser.parseLevels(it) }
            cachedLevels = list
            list
        }
    }

    /** Transkrip satu video (cache LRU kecil); asset hilang → transkrip kosong. */
    fun transcript(video: DreamBigVideo): DreamBigTranscript {
        synchronized(this) {
            transcriptCache[video.videoId]?.let { return it }
        }
        val json = readAsset(video.transcript)
        val parsed = if (json == null) DreamBigTranscript(video.videoId, "", null, emptyList())
        else DreamBigParser.parseTranscript(json)
        synchronized(this) { transcriptCache[video.videoId] = parsed }
        return parsed
    }

    private fun readAsset(path: String): String? = runCatching {
        appContext.assets.open(path).bufferedReader().use { it.readText() }
    }.getOrNull()
}
