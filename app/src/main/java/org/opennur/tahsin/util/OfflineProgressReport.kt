package org.opennur.tahsin.util

import com.google.gson.GsonBuilder

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
    val surahs: List<OfflineSurahProgress>,
    val juz: List<OfflineJuzProgress>,
)

data class OfflineSurahProgress(
    val number: Int,
    val totalAyahs: Int,
    val practicedAyahs: Int,
    val goodAyahs: Int,
    val averageScore: Int,
    val dueAyahs: Int,
)

data class OfflineJuzProgress(
    val juz: Int,
    val startPage: Int,
    val totalAyahs: Int,
    val practicedAyahs: Int,
    val goodAyahs: Int,
)

object OfflineProgressReportEncoder {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun encode(report: OfflineProgressReport): String = gson.toJson(report)
}
