package com.ayahofday.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "verses",
    indices = [Index(value = ["surahNumber", "ayahNumber"], unique = true)],
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val tafsir: String? = null,
    val asbabunNuzul: String? = null,
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["verseId"], unique = true)],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseId: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "journals")
data class JournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseId: Long? = null,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)
