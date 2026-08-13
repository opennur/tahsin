package org.opennur.tahsin.data.quran

/**
 * 15 tempat sujud tilawah (sajdah) mushaf Madani riwayat Hafs — disalin dari
 * alquran.cloud `/v1/sajda` (id 1..15) saat implementasi mushaf halaman.
 *
 * Catatan: pasangan sajdah (16:49/50, 17:107/109, 27:25/26, 41:37/38) memakai
 * ayat KEDUA (tempat sujud menurut mushaf Madani); tanda ۩ dirender oleh UI
 * di akhir ayat ini. Murni JVM, tanpa Android — di-unit-test.
 */
data class SajdahAyah(
    val surah: Int,
    val ayah: Int,
    /** Wajib (fardhu) menurut mazhab Syafi'i: 32:15, 41:38, 53:62, 96:19. */
    val obligatory: Boolean,
)

object SajdahSigns {

    /** Tanda sujud tilawah mushaf: ۩ (U+06E9). */
    const val SIGN = "\u06E9"

    /** 15 tempat sujud (urutan mushaf). */
    val ALL: List<SajdahAyah> = listOf(
        SajdahAyah(7, 206, obligatory = false),
        SajdahAyah(13, 15, obligatory = false),
        SajdahAyah(16, 50, obligatory = false),
        SajdahAyah(17, 109, obligatory = false),
        SajdahAyah(19, 58, obligatory = false),
        SajdahAyah(22, 18, obligatory = false),
        SajdahAyah(22, 77, obligatory = false),
        SajdahAyah(25, 60, obligatory = false),
        SajdahAyah(27, 26, obligatory = false),
        SajdahAyah(32, 15, obligatory = true),
        SajdahAyah(38, 24, obligatory = false),
        SajdahAyah(41, 38, obligatory = true),
        SajdahAyah(53, 62, obligatory = true),
        SajdahAyah(84, 21, obligatory = false),
        SajdahAyah(96, 19, obligatory = true),
    )

    /** Apakah surah:ayat adalah tempat sujud tilawah. */
    fun isSajdah(surah: Int, ayah: Int): Boolean =
        ALL.any { it.surah == surah && it.ayah == ayah }
}
