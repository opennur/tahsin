package org.opennur.tahsin.util

/** Laporan progres anonim yang aman dibagikan secara offline. */
data class OfflineProgressReport(
    val schemaVersion: Int = 1,
    val generatedAt: Long,
    val totalAyahs: Int,
    val practicedAyahs: Int,
    val goodAyahs: Int,
    val dueAyahs: Int,
    val goodPages: Int,
    val reviewPages: Int,
    val untouchedPages: Int,
    val goodJuz: Int,
    val totalSessions: Int,
    val bestScorePct: Int,
    val streak: Int,
    val xp: Int,
)
