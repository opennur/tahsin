package org.opennur.tahsin.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MushafRenderModeTest {

    @Test
    fun `known keys restore selected mode`() {
        assertThat(MushafRenderMode.fromKey("exact")).isEqualTo(MushafRenderMode.EXACT)
        assertThat(MushafRenderMode.fromKey("accessible")).isEqualTo(MushafRenderMode.ACCESSIBLE)
    }

    @Test
    fun `unknown key safely uses accessible mode`() {
        assertThat(MushafRenderMode.fromKey("legacy")).isEqualTo(MushafRenderMode.ACCESSIBLE)
    }
}
