package org.opennur.tahsin.data.quran

import com.google.gson.Gson

/** Kontrak geometri layout Uthmani Madani yang dipakai mode exact. */
data class MushafLayoutManifest(
    val schemaVersion: Int,
    val edition: String,
    val script: String,
    val pageCount: Int,
    val linesPerPage: Int,
    val referenceFont: String,
    val phone: MushafViewport,
    val tablet: MushafViewport,
) {
    fun viewportFor(widthDp: Float): MushafViewport =
        if (widthDp < 600f) phone else tablet

    companion object {
        const val EXPECTED_LINES = 15

        val DEFAULT = MushafLayoutManifest(
            schemaVersion = 1,
            edition = "Madani",
            script = "Uthmani",
            pageCount = 604,
            linesPerPage = EXPECTED_LINES,
            referenceFont = "uthmani.ttf",
            phone = MushafViewport(widthDp = 360f, pageAspect = 0.704f, lineHeightSp = 28f),
            tablet = MushafViewport(widthDp = 640f, pageAspect = 0.704f, lineHeightSp = 34f),
        )

        private val gson = Gson()

        fun parse(json: String): MushafLayoutManifest = runCatching {
            val parsed = gson.fromJson(json, MushafLayoutManifest::class.java)
            if (isUsable(parsed)) {
                parsed
            } else {
                DEFAULT
            }
        }.getOrDefault(DEFAULT)

        private fun isUsable(manifest: MushafLayoutManifest): Boolean =
            manifest.linesPerPage == EXPECTED_LINES &&
                manifest.pageCount > 0 &&
                validViewport(manifest.phone) &&
                validViewport(manifest.tablet)

        private fun validViewport(viewport: MushafViewport): Boolean =
            viewport.widthDp > 0f && viewport.pageAspect > 0f && viewport.lineHeightSp > 0f
    }
}

data class MushafViewport(
    val widthDp: Float,
    val pageAspect: Float,
    val lineHeightSp: Float,
)
