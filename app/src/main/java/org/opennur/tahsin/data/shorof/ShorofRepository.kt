package org.opennur.tahsin.data.shorof

import android.content.Context

/** Sumber materi Shorof offline dari aset APK. */
class ShorofRepository(context: Context) {
    private val appContext = context.applicationContext

    @Volatile
    private var cached: ShorofCatalog? = null

    fun catalog(): ShorofCatalog = cached ?: synchronized(this) {
        cached ?: run {
            val json = runCatching {
                appContext.assets.open("shorof/lessons.json")
                    .bufferedReader()
                    .use { it.readText() }
            }.getOrNull()
            val value = json?.let(ShorofParser::parse) ?: ShorofCatalog(0, emptyList())
            cached = value
            value
        }
    }
}
