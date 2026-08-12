package com.tahsin.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tes bahasa aplikasi: enum [AppLanguage] dan siklus [AppLanguage.next]. */
class AppLanguageTest {

    @Test
    fun `kode dan label benar`() {
        assertEquals("id", AppLanguage.ID.code)
        assertEquals("ID", AppLanguage.ID.label)
        assertEquals("en", AppLanguage.EN.code)
        assertEquals("EN", AppLanguage.EN.label)
    }

    @Test
    fun `urutan enum stabil - ID lalu EN`() {
        assertEquals(listOf(AppLanguage.ID, AppLanguage.EN), AppLanguage.entries)
    }

    @Test
    fun `next berputar dari ID ke EN`() {
        assertEquals(AppLanguage.EN, AppLanguage.ID.next())
    }

    @Test
    fun `next berputar kembali ke ID dari EN (siklus)`() {
        assertEquals(AppLanguage.ID, AppLanguage.EN.next())
    }

    @Test
    fun `next dua langkah kembali ke bahasa semula`() {
        for (lang in AppLanguage.entries) {
            assertEquals(lang, lang.next().next())
        }
    }
}
