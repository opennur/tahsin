package org.opennur.tahsin.data.quran

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MushafLayoutTest {

    @Test
    fun `default manifest targets Madani Uthmani 604 pages and 15 lines`() {
        val manifest = MushafLayoutManifest.DEFAULT

        assertThat(manifest.edition).isEqualTo("Madani")
        assertThat(manifest.script).isEqualTo("Uthmani")
        assertThat(manifest.pageCount).isEqualTo(604)
        assertThat(manifest.linesPerPage).isEqualTo(15)
        assertThat(manifest.viewportFor(360f)).isEqualTo(manifest.phone)
        assertThat(manifest.viewportFor(600f)).isEqualTo(manifest.tablet)
    }

    @Test
    fun `parser reads valid manifest`() {
        val parsed = MushafLayoutManifest.parse(
            """
            {
              "schemaVersion": 2,
              "edition": "Madani",
              "script": "Uthmani",
              "pageCount": 604,
              "linesPerPage": 15,
              "referenceFont": "font.ttf",
              "phone": {"widthDp": 360, "pageAspect": 0.7, "lineHeightSp": 28},
              "tablet": {"widthDp": 640, "pageAspect": 0.7, "lineHeightSp": 34}
            }
            """.trimIndent(),
        )

        assertThat(parsed.schemaVersion).isEqualTo(2)
        assertThat(parsed.referenceFont).isEqualTo("font.ttf")
        assertThat(parsed.phone.lineHeightSp).isEqualTo(28f)
    }

    @Test
    fun `parser falls back for invalid json or wrong line count`() {
        assertThat(MushafLayoutManifest.parse("bad")).isEqualTo(MushafLayoutManifest.DEFAULT)
        assertThat(
            MushafLayoutManifest.parse(
                """{"linesPerPage": 14}""",
            ),
        ).isEqualTo(MushafLayoutManifest.DEFAULT)
        assertThat(
            MushafLayoutManifest.parse(
                """{"linesPerPage": 15}""",
            ),
        ).isEqualTo(MushafLayoutManifest.DEFAULT)
    }
}
