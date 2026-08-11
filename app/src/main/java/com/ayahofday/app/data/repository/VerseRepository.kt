package com.ayahofday.app.data.repository

import com.ayahofday.app.data.model.Journal
import com.ayahofday.app.data.model.Verse
import kotlinx.coroutines.flow.Flow

/**
 * Kontrak repository ayat.
 *
 * TODO: implementasi default akan menggabungkan:
 *  - EQuran.id API (https://equran.id/api/v2/) atau API Kemenag → ayat, tafsir, asbabun nuzul
 *  - Room (AppDatabase) → cache, bookmark, jurnal
 */
interface VerseRepository {

    /** Ayat hari ini (dengan tafsir & asbabun nuzul bila tersedia). */
    suspend fun getVerseOfTheDay(): Verse

    /** Lengkapi ayat dengan tafsir Bahasa Indonesia. */
    suspend fun getTafsir(verse: Verse): Verse

    // ---- Bookmark ----

    fun observeBookmarks(): Flow<List<Verse>>
    suspend fun addBookmark(verse: Verse)
    suspend fun removeBookmark(verse: Verse)

    // ---- Journal ----

    fun observeJournals(): Flow<List<Journal>>
    suspend fun saveJournal(journal: Journal)
    suspend fun deleteJournal(journalId: Long)
}
