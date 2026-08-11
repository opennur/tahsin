package com.tahsin.app.util

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.tahsin.app.theme.ArabicFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Font mushaf runtime: file TTF di `filesDir/fonts/` (diunduh in-app atau
 * disediakan user). Kalau file belum ada, memakai font sistem.
 */
class FontStore(context: Context) {

    private val fontsDir = File(context.applicationContext.filesDir, "fonts")

    fun fileExists(font: ArabicFont): Boolean {
        val name = font.fileName ?: return true
        val f = File(fontsDir, name)
        return f.exists() && f.length() > 0L
    }

    /** FontFamily untuk pilihan font; fallback ke sistem kalau file belum ada. */
    fun loadFamily(font: ArabicFont): FontFamily {
        val name = font.fileName ?: return FontFamily.Default
        val f = File(fontsDir, name)
        return if (f.exists() && f.length() > 0L) FontFamily(Font(f)) else FontFamily.Default
    }

    /** Unduh font kalau belum ada (melempar exception kalau gagal). */
    suspend fun ensureFont(font: ArabicFont) = withContext(Dispatchers.IO) {
        val name = font.fileName ?: return@withContext
        val url = font.downloadUrl ?: return@withContext
        val file = File(fontsDir, name)
        if (file.exists() && file.length() > 0L) return@withContext
        fontsDir.mkdirs()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        try {
            conn.connect()
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
            conn.inputStream.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            conn.disconnect()
        }
    }
}
