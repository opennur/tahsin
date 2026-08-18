package org.opennur.tahsin.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import org.opennur.tahsin.util.Gamification
import org.opennur.tahsin.util.OfflineProgressReport

/** Render laporan progres ringkas sebagai PNG untuk dibagikan. */
object ProgressReportImage {
    private const val WIDTH = 1080
    private const val HEIGHT = 980
    private const val BRAND = 0xFF2D7D6B.toInt()
    private const val TEXT = 0xFF1A1A1A.toInt()
    private const val MUTED = 0xFF5A5A5A.toInt()
    private const val SURFACE = 0xFFFFFFFF.toInt()
    private const val BACKGROUND = 0xFFF7F3EE.toInt()
    private const val TRACK = 0xFFE5DED5.toInt()

    fun write(file: File, report: OfflineProgressReport, strings: Strings): File {
        file.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(BACKGROUND)
            val title = paint(44f, BRAND, bold = true)
            val body = paint(30f, TEXT)
            val muted = paint(25f, MUTED)
            val value = paint(42f, BRAND, bold = true)
            val center = paint(28f, TEXT, bold = true, align = Paint.Align.CENTER)

            canvas.drawText(strings.appTitle, 54f, 78f, title)
            canvas.drawText(strings.statsTitle, 54f, 120f, body)
            canvas.drawText(
                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                    .format(Date(report.generatedAt)),
                54f,
                158f,
                muted,
            )

            card(canvas, 54f, 200f, 472f, 350f)
            card(canvas, 554f, 200f, 1026f, 350f)
            canvas.drawText(
                strings.statsReadingTitle,
                82f,
                245f,
                body,
            )
            canvas.drawText(
                strings.statsReadingSummary.format(
                    report.practicedAyahs,
                    report.totalAyahs,
                    if (report.totalAyahs == 0) 0 else report.practicedAyahs * 100 / report.totalAyahs,
                    report.goodJuz,
                ),
                82f,
                302f,
                muted,
            )
            canvas.drawText(
                strings.homeReadingPages.format(
                    report.goodPages,
                    report.goodPages + report.reviewPages + report.untouchedPages,
                ),
                82f,
                360f,
                value,
            )
            progressBar(canvas, 82f, 405f, 444f, report.practicedAyahs, report.totalAyahs)
            canvas.drawText(strings.statsDueTitle, 82f, 490f, muted)
            canvas.drawText("${report.dueAyahs}", 82f, 530f, value)

            canvas.drawText(strings.statsTotalSessions, 594f, 270f, muted)
            canvas.drawText("${report.totalSessions}", 594f, 320f, value)
            canvas.drawText(strings.statsBestScoreLabel, 594f, 390f, muted)
            canvas.drawText("${report.bestScorePct}%", 594f, 440f, value)
            canvas.drawText(strings.homeStreakLine.format(report.streak), 594f, 505f, body)

            canvas.drawText(strings.statsReadingTitle, 54f, 630f, title)
            val rows = listOf(
                strings.statsReadingSummary.format(
                    report.practicedAyahs,
                    report.totalAyahs,
                    if (report.totalAyahs == 0) 0 else report.practicedAyahs * 100 / report.totalAyahs,
                    report.goodJuz,
                ),
                strings.homeReadingPages.format(
                    report.goodPages,
                    report.goodPages + report.reviewPages + report.untouchedPages,
                ),
                strings.statsDueTitle + ": ${report.dueAyahs}",
            )
            rows.forEachIndexed { index, row ->
                canvas.drawText(row, 54f, (690 + index * 48).toFloat(), body)
            }
            canvas.drawText(
                strings.homeLevelLine.format(Gamification.levelFor(report.xp), report.xp),
                54f,
                860f,
                muted,
            )
            canvas.drawText(strings.statsShareReport, WIDTH - 54f, 920f, center)
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } finally {
            bitmap.recycle()
        }
        return file
    }

    private fun paint(
        size: Float,
        color: Int,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        textAlign = align
    }

    private fun card(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        canvas.drawRoundRect(RectF(left, top, right, bottom), 28f, 28f, paint(1f, SURFACE))
    }

    private fun progressBar(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        done: Int,
        total: Int,
    ) {
        canvas.drawRoundRect(RectF(left, top, right, top + 18f), 9f, 9f, paint(1f, TRACK))
        val fraction = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
        canvas.drawRoundRect(RectF(left, top, left + (right - left) * fraction, top + 18f), 9f, 9f, paint(1f, BRAND))
    }
}
