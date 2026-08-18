package org.opennur.tahsin.data.nahwu

import android.content.Context

/** Sumber materi Nahwu offline dari aset APK. */
class NahwuRepository(context: Context) {
    private val appContext = context.applicationContext

    @Volatile
    private var cached: NahwuCatalog? = null

    fun catalog(): NahwuCatalog = cached ?: synchronized(this) {
        cached ?: run {
            val json = runCatching {
                appContext.assets.open("nahwu/lessons.json")
                    .bufferedReader()
                    .use { it.readText() }
            }.getOrNull()
            val value = if (json == null) {
                NahwuCatalog(schemaVersion = 0, levels = emptyList())
            } else {
                NahwuParser.parse(json)
            }
            cached = value
            value
        }
    }
}
