package org.opennur.tahsin.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Label waktu relatif untuk riwayat baca: "baru saja", "N mnt lalu",
 * "N jam lalu", "kemarin", atau tanggal (mis. "12 Agu"). Murni dan
 * deterministik — `now` diteruskan sebagai parameter (diisi pemanggil).
 */
object RelativeTime {

    private val ID_MONTHS = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
    )

    fun format(timestamp: Long, now: Long, lang: AppLanguage): String {
        val diff = now - timestamp
        if (diff < 60_000L) return if (lang == AppLanguage.EN) "just now" else "baru saja"
        val minutes = diff / 60_000L
        if (minutes < 60) return if (lang == AppLanguage.EN) "$minutes min ago" else "$minutes mnt lalu"
        val hours = minutes / 60
        if (hours < 24) return if (lang == AppLanguage.EN) "$hours h ago" else "$hours jam lalu"
        val t = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        val n = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
        if (t.toLocalDate() == n.toLocalDate().minusDays(1)) {
            return if (lang == AppLanguage.EN) "yesterday" else "kemarin"
        }
        return if (lang == AppLanguage.EN) {
            "${t.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${t.dayOfMonth}"
        } else {
            "${t.dayOfMonth} ${ID_MONTHS[t.monthValue - 1]}"
        }
    }
}
