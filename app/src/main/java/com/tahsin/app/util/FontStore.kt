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
 * Font mushaf: prioritas sumber —
 * 1. `assets/fonts/<file>` (di-bundle ke APK via tools/fetch_font.sh → offline).
 * 2. `filesDir/fonts/<file>` (diunduh in-app / disediakan user).
 * 3. Font sistem perangkat.
 */
class FontStore(context: Context) {

    private val context = context.applicationContext
    private val fontsDir = File(context.filesDir, "fonts")

    private fun assetExists(name: String): Boolean =
        runCatching { context.assets.open("fonts/$name").close(); true }.getOrDefault(false)

    fun fileExists(font: ArabicFont): Boolean {
        val name = font.fileName ?: return true
        if (assetExists(name)) return true
        val f = File(fontsDir, name)
        return f.exists() && f.length() > 0L
    }

    /** FontFamily untuk pilihan font; bundle aset → filesDir → sistem. */
    fun loadFamily(font: ArabicFont): FontFamily {
        val name = font.fileName ?: return FontFamily.Default
        if (assetExists(name)) {
            return FontFamily(Font("fonts/$name", context.assets))
        }
        val f = File(fontsDir, name)
        return if (f.exists() && f.length() > 0L) FontFamily(Font(f)) else FontFamily.Default
    }

    /** Unduh font kalau file di filesDir belum ada (aset bundle sudah cukup). */
    suspend fun ensureFont(font: ArabicFont) = withContext(Dispatchers.IO) {
        val name = font.fileName ?: return@withContext
        val url = font.downloadUrl ?: return@withContext
        if (assetExists(name)) return@withContext
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
