package com.ayahofday.app.data.model

/** Refleksi jurnal harian pengguna (akan disimpan ke Room). */
data class Journal(
    val id: Long = 0,
    val verseId: Long? = null,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)
