package com.tahsin.app.data.quran

import android.content.Context
import com.google.gson.Gson

/**
 * Membaca mushaf dari `assets/quran/mushaf.json` (offline).
 * Diparse sekali lalu di-cache.
 */
class QuranRepository(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()

    @Volatile
    private var cached: Mushaf? = null

    fun loadMushaf(): Mushaf = cached ?: synchronized(this) {
        cached ?: run {
            val json = appContext.assets
                .open("quran/mushaf.json")
                .bufferedReader()
                .use { it.readText() }
            val mushaf = gson.fromJson(json, Mushaf::class.java)
            cached = mushaf
            mushaf
        }
    }

    fun surahs(): List<Surah> = loadMushaf().surahs

    fun surah(number: Int): Surah? = loadMushaf().surahs.find { it.number == number }
}
