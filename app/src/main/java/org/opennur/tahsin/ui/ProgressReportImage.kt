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
    private const val HEIGHT = 900
    private const val MARGIN = 64f
    private const val GAP = 28f
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
            drawHeader(canvas, report, strings)

            val cardTop = 220f
            val cardBottom = 760f
            val left = MARGIN
            val right = WIDTH - MARGIN
            val cardWidth = (right - left - GAP) / 2f
            val rightCardLeft = left + cardWidth + GAP

            card(canvas, left, cardTop, left + cardWidth, cardBottom)
            card(canvas, rightCardLeft, cardTop, right, cardBottom)
            drawReadingCard(canvas, left, cardTop, cardWidth, report, strings)
            drawSummaryCard(canvas, rightCardLeft, cardTop, cardWidth, report, strings)

            val footer = paint(22f, MUTED)
            canvas.drawText(
                strings.homeLevelLine.format(Gamification.levelFor(report.xp), report.xp),
                MARGIN,
                835f,
                footer,
            )
        } finally {
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            bitmap.recycle()
        }
        return file
    }

    private fun drawHeader(canvas: Canvas, report: OfflineProgressReport, strings: Strings) {
        canvas.drawText(strings.appTitle, MARGIN, 78f, paint(46f, BRAND, bold = true))
        canvas.drawText(strings.statsTitle, MARGIN, 124f, paint(30f, TEXT))
        canvas.drawText(
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                .format(Date(report.generatedAt)),
            MARGIN,
            164f,
            paint(24f, MUTED),
        )
    }

    private fun drawReadingCard(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        report: OfflineProgressReport,
        strings: Strings,
    ) {
        val x = left + 32f
        val maxWidth = width - 64f
        val body = paint(28f, TEXT)
        val muted = paint(25f, MUTED)
        val value = paint(40f, BRAND, bold = true)

        canvas.drawText(strings.statsReadingTitle, x, top + 54f, body)
        var y = drawWrappedText(
            canvas,
            strings.statsReadingSummary.format(
                report.practicedAyahs,
                report.totalAyahs,
                coveragePercent(report),
                report.goodJuz,
            ),
            x,
            top + 106f,
            maxWidth,
            muted,
        )
        y += 18f
        canvas.drawText(
            strings.homeReadingPages.format(
                report.goodPages,
                report.goodPages + report.reviewPages + report.untouchedPages,
            ),
            x,
            y,
            value,
        )
        progressBar(canvas, x, y + 34f, x + maxWidth, report.practicedAyahs, report.totalAyahs)
        canvas.drawText(strings.statsDueTitle, x, y + 102f, muted)
        canvas.drawText("${report.dueAyahs}", x, y + 148f, value)
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        report: OfflineProgressReport,
        strings: Strings,
    ) {
        val x = left + 32f
        val value = paint(46f, BRAND, bold = true)
        val muted = paint(25f, MUTED)
        val body = paint(28f, TEXT)

        canvas.drawText(strings.statsTotalSessions, x, top + 58f, muted)
        canvas.drawText("${report.totalSessions}", x, top + 112f, value)
        canvas.drawText(strings.statsBestScoreLabel, x, top + 190f, muted)
        canvas.drawText("${report.bestScorePct}%", x, top + 244f, value)
        drawWrappedText(
            canvas,
            strings.homeStreakLine.format(report.streak),
            x,
            top + 326f,
            width - 64f,
            body,
        )
    }

    private fun coveragePercent(report: OfflineProgressReport): Int =
        if (report.totalAyahs == 0) 0 else report.practicedAyahs * 100 / report.totalAyahs

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        firstBaseline: Float,
        maxWidth: Float,
        paint: Paint,
    ): Float {
        val words = text.split(" ")
        var line = StringBuilder()
        var baseline = firstBaseline
        words.forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (line.isNotEmpty() && paint.measureText(candidate) > maxWidth) {
                canvas.drawText(line.toString(), x, baseline, paint)
                baseline += paint.textSize * 1.35f
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line.toString(), x, baseline, paint)
        return baseline
    }

    private fun paint(
        size: Float,
        color: Int,
        bold: Boolean = false,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
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
        canvas.drawRoundRect(
            RectF(left, top, left + (right - left) * fraction, top + 18f),
            9f,
            9f,
            paint(1f, BRAND),
        )
    }
}
