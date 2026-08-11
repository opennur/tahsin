package com.ayahofday.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(verse: VerseEntity): Long

    @Query("SELECT * FROM verses WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getByPosition(surahNumber: Int, ayahNumber: Int): VerseEntity?

    @Query("SELECT * FROM verses ORDER BY surahNumber ASC, ayahNumber ASC")
    fun observeAll(): Flow<List<VerseEntity>>
}

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query(
        "SELECT verses.* FROM bookmarks " +
            "INNER JOIN verses ON verses.id = bookmarks.verseId " +
            "ORDER BY bookmarks.createdAt DESC"
    )
    fun observeBookmarkedVerses(): Flow<List<VerseEntity>>

    @Query("DELETE FROM bookmarks WHERE verseId = :verseId")
    suspend fun deleteByVerseId(verseId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE verseId = :verseId)")
    suspend fun isBookmarked(verseId: Long): Boolean
}

@Dao
interface JournalDao {
    @Insert
    suspend fun insert(journal: JournalEntity): Long

    @Query("SELECT * FROM journals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<JournalEntity>>

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
