package com.tahsin.app.util

/**
 * Bahasa aplikasi + terjemahan Al-Qur'an.
 *
 * - ID: terjemahan Kemenag dari equran.id (`teksIndonesia`).
 * - EN: Saheeh International dari quran.com API (resource 20).
 */
enum class AppLanguage(val code: String, val label: String) {
    ID("id", "ID"),
    EN("en", "EN"),
}

/** Bahasa berikutnya untuk tombol ganti bahasa (siklus). */
fun AppLanguage.next(): AppLanguage {
    val entries = AppLanguage.entries
    return entries[(ordinal + 1) % entries.size]
}
