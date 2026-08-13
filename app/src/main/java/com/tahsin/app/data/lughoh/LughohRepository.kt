package com.tahsin.app.data.lughoh

import android.content.Context

/**
 * Sumber data pelajaran Bahasa Arab: `assets/lughoh/lessons.json` (offline,
 * dihasilkan `tools/build_lughoh.py`, di-bundle ke APK).
 */
class LughohRepository(context: Context) {

    private val appContext = context.applicationContext

    @Volatile
    private var cached: LughohCatalog? = null

    /** Seluruh katalog (3 level, 15 pelajaran). Kosong kalau aset gagal dibaca. */
    fun catalog(): LughohCatalog = cached ?: synchronized(this) {
        cached ?: run {
            val json = runCatching {
                appContext.assets.open("lughoh/lessons.json")
                    .bufferedReader()
                    .use { it.readText() }
            }.getOrNull()
            val catalog = if (json == null) LughohCatalog(schemaVersion = 0, levels = emptyList())
            else LughohParser.parse(json)
            cached = catalog
            catalog
        }
    }
}
