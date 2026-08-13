package com.tahsin.app.util

import com.google.gson.Gson
import com.tahsin.app.data.quran.Ayah
import com.tahsin.app.data.quran.Surah
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Tes pemilihan "Ayah of the Day" — logika MURNI (tanpa Android). */
class AyahOfTheDayPickerTest {

    private val gson = Gson()

    private fun gsonJson(ayah: AyahOfTheDay): String = gson.toJson(ayah)

    // 3 surah fiktif: 7, 286, 200 ayat → kumulatif [7, 293, 493]
    private val counts = listOf(7, 286, 200)
    private val cumulative = AyahOfTheDayPicker.cumulativeCounts(counts)

    @Test
    fun `cumulativeCounts - prefix sum`() {
        assertEquals(listOf(7, 293, 493), cumulative)
    }

    @Test
    fun `refForIndex - ayat pertama mushaf`() {
        assertEquals(1 to 1, AyahOfTheDayPicker.refForIndex(0, cumulative))
    }

    @Test
    fun `refForIndex - ayat terakhir surah pertama`() {
        assertEquals(1 to 7, AyahOfTheDayPicker.refForIndex(6, cumulative))
    }

    @Test
    fun `refForIndex - melintasi batas surah`() {
        assertEquals(2 to 1, AyahOfTheDayPicker.refForIndex(7, cumulative))
        assertEquals(2 to 286, AyahOfTheDayPicker.refForIndex(292, cumulative))
        assertEquals(3 to 1, AyahOfTheDayPicker.refForIndex(293, cumulative))
    }

    @Test
    fun `refForIndex - ayat terakhir mushaf`() {
        assertEquals(3 to 200, AyahOfTheDayPicker.refForIndex(492, cumulative))
    }

    @Test
    fun `refForIndex - index di luar jangkauan ditolak`() {
        assertThrows(IllegalArgumentException::class.java) {
            AyahOfTheDayPicker.refForIndex(-1, cumulative)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AyahOfTheDayPicker.refForIndex(493, cumulative)
        }
    }

    @Test
    fun `refForIndex - daftar kumulatif kosong ditolak`() {
        assertThrows(IllegalArgumentException::class.java) {
            AyahOfTheDayPicker.refForIndex(0, emptyList())
        }
    }

    @Test
    fun `ayahRefForDate - deterministik untuk tanggal yang sama`() {
        val day = LocalDate.of(2026, 8, 12).toEpochDay()
        assertEquals(
            AyahOfTheDayPicker.ayahRefForDate(day, cumulative),
            AyahOfTheDayPicker.ayahRefForDate(day, cumulative),
        )
    }

    @Test
    fun `ayahRefForDate - selalu dalam jangkauan untuk rentang tanggal`() {
        for (epochDay in 19_000L..19_200L) {
            val (surah, ayah) = AyahOfTheDayPicker.ayahRefForDate(epochDay, cumulative)
            assertTrue("surah=$surah", surah in 1..counts.size)
            assertTrue("ayah=$ayah", ayah in 1..counts[surah - 1])
        }
    }

    @Test
    fun `ayahRefForDate - tanggal berbeda umumnya memberi ayat berbeda`() {
        val refs = (19_000L..19_060L).map { AyahOfTheDayPicker.ayahRefForDate(it, cumulative) }.toSet()
        assertTrue("Hanya ${refs.size} ref unik dari 61 hari", refs.size > 1)
    }

    @Test
    fun `dateKey - format yyyy-MM-dd`() {
        assertEquals("2026-08-12", AyahOfTheDayPicker.dateKey(LocalDate.of(2026, 8, 12)))
    }

// ===== cachedFrom (validasi cache) =====

@Test
fun `cachedFrom - json null - null`() {
    assertNull(AyahOfTheDayPicker.cachedFrom(null, LocalDate.of(2026, 8, 12), AppLanguage.ID))
}

@Test
fun `cachedFrom - json rusak - null`() {
    assertNull(AyahOfTheDayPicker.cachedFrom("bukan json{{{", LocalDate.of(2026, 8, 12), AppLanguage.ID))
}

@Test
fun `cachedFrom - tanggal beda - null (basi)`() {
    val json = gsonJson(AyahOfTheDay(dateKey = "2026-08-12", surahNumber = 1, ayahNumber = 1,
        surahName = "Al-Fatihah", arabic = "بِسْمِ", translation = "t", language = "id"))
    assertNull(AyahOfTheDayPicker.cachedFrom(json, LocalDate.of(2026, 8, 13), AppLanguage.ID))
}

@Test
fun `cachedFrom - bahasa beda - null`() {
    val json = gsonJson(AyahOfTheDay(dateKey = "2026-08-12", surahNumber = 1, ayahNumber = 1,
        surahName = "Al-Fatihah", arabic = "بِسْمِ", translation = "t", language = "id"))
    assertNull(AyahOfTheDayPicker.cachedFrom(json, LocalDate.of(2026, 8, 12), AppLanguage.EN))
}

@Test
fun `cachedFrom - segar - kembalikan ayah`() {
    val ayah = AyahOfTheDay(dateKey = "2026-08-12", surahNumber = 1, ayahNumber = 1,
        surahName = "Al-Fatihah", arabic = "بِسْمِ", translation = "t", language = "id")
    assertEquals(ayah, AyahOfTheDayPicker.cachedFrom(gsonJson(ayah), LocalDate.of(2026, 8, 12), AppLanguage.ID))
}

// ===== contentOf (perakitan konten) =====

private val surahs = listOf(
    Surah(number = 1, nameArabic = "الفاتحة", nameLatin = "Al-Fatihah", ayahCount = 2,
        ayahs = listOf(
            Ayah(number = 1, text = "بِسْمِ اللَّهِ", translation = "In the name"),
            Ayah(number = 2, text = "الرَّحْمَٰنِ", translation = "the Most Gracious"),
        )),
    Surah(number = 2, nameArabic = "البقرة", nameLatin = "Al-Baqarah", ayahCount = 1,
        ayahs = listOf(Ayah(number = 1, text = "الم", translation = "Alif Lam Mim"))),
)

private fun loader() = { number: Int -> surahs.firstOrNull { it.number == number } }

@Test
fun `contentOf - surah kosong - null`() {
    assertNull(runBlocking { AyahOfTheDayPicker.contentOf(emptyList(), loader(), LocalDate.of(2026, 8, 12), AppLanguage.ID) })
}

@Test
fun `contentOf - konten surah gagal dimuat - null`() {
    assertNull(
        runBlocking { AyahOfTheDayPicker.contentOf(surahs, { null }, LocalDate.of(2026, 8, 12), AppLanguage.ID) },
    )
}

@Test
fun `contentOf - konten surah lebih pendek dari ayahCount - null`() {
    // Surah 1 mengaku 2 ayat tapi kontennya cuma 1 → ayah ke-2 hilang.
    val short = listOf(
        Surah(number = 1, nameArabic = "الفاتحة", nameLatin = "Al-Fatihah", ayahCount = 2,
            ayahs = listOf(Ayah(number = 1, text = "بِسْمِ", translation = "x"))),
    )
    // Cari tanggal yang memilih ayah ke-2 surah 1 (rentang luas — bit atas
    // seed LCG berkorelasi untuk seed berurutan, jadi jangan berasumsi).
    val cumulative = AyahOfTheDayPicker.cumulativeCounts(short.map { it.ayahCount })
    var found: LocalDate? = null
    for (epoch in 0L..200_000L) {
        val ref = AyahOfTheDayPicker.ayahRefForDate(epoch, cumulative)
        if (ref == 1 to 2) { found = LocalDate.ofEpochDay(epoch); break }
    }
    assertNotNull("harusnya ada tanggal yang memilih (1,2)", found)
    assertNull(
        runBlocking { AyahOfTheDayPicker.contentOf(short, { short[0] }, found!!, AppLanguage.ID) },
    )
}

@Test
fun `contentOf - berhasil - perakitan lengkap + fallback nama arab`() {
    val cumulative = AyahOfTheDayPicker.cumulativeCounts(surahs.map { it.ayahCount })
    val ref = AyahOfTheDayPicker.ayahRefForDate(19_000L, cumulative)
    val (s, a) = ref
    val result = runBlocking { AyahOfTheDayPicker.contentOf(surahs, loader(), LocalDate.ofEpochDay(19_000L), AppLanguage.ID) }
    assertNotNull(result)
    assertEquals(s, result!!.surahNumber)
    assertEquals(a, result.ayahNumber)
    assertEquals(surahs[s - 1].ayahs[a - 1].text, result.arabic)
    assertEquals("id", result.language)
    assertEquals(LocalDate.ofEpochDay(19_000L).toString(), result.dateKey)

    // Nama latin kosong → fallback nama Arab (periksa surah hasil pemilihan).
    val noLatin = surahs.map { s -> s.copy(nameLatin = "") }
    val noLatinCumulative = AyahOfTheDayPicker.cumulativeCounts(noLatin.map { it.ayahCount })
    val (ns, _) = AyahOfTheDayPicker.ayahRefForDate(19_000L, noLatinCumulative)
    val r2 = runBlocking {
        AyahOfTheDayPicker.contentOf(
            noLatin,
            { n -> noLatin.firstOrNull { it.number == n } },
            LocalDate.ofEpochDay(19_000L),
            AppLanguage.ID,
        )
    }
    assertNotNull(r2)
    assertEquals(noLatin[ns - 1].nameArabic, r2!!.surahName)

    // Daftar surah parsial (ada gap nomor) → null, bukan crash.
    val gapped = listOf(
        Surah(number = 1, nameArabic = "الفاتحة", nameLatin = "Al-Fatihah", ayahCount = 5,
            ayahs = listOf(Ayah(1, "أ"))),
        Surah(number = 3, nameArabic = "آل عمران", nameLatin = "Ali Imran", ayahCount = 5,
            ayahs = listOf(Ayah(1, "ب"))),
    )
    val gappedCumulative = AyahOfTheDayPicker.cumulativeCounts(gapped.map { it.ayahCount })
    val gappedRef = AyahOfTheDayPicker.ayahRefForDate(19_000L, gappedCumulative)
    // Paksa index yang menunjuk ke nomor 2 (tidak ada di daftar).
    val epochForMissing = (0L..400_000L).first { epoch ->
        AyahOfTheDayPicker.ayahRefForDate(epoch, gappedCumulative).first == 2
    }
    assertNotNull(gappedRef)
    assertNull(
        runBlocking {
            AyahOfTheDayPicker.contentOf(
                gapped,
                { n -> gapped.firstOrNull { it.number == n } },
                LocalDate.ofEpochDay(epochForMissing),
                AppLanguage.ID,
            )
        },
    )
}
}
