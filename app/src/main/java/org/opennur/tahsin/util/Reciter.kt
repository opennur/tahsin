package org.opennur.tahsin.util

/**
 * Pilihan qari' (perawi) audio ayat. URL everyayah.com konsisten:
 * `https://everyayah.com/data/<slug>/<surah><ayah>.mp3` — tinggal ganti folder.
 *
 * Slug diverifikasi ada di `https://everyayah.com/data/` (listing resmi).
 */
enum class Reciter(val slug: String, val label: String) {
    MINSHAWY("Minshawy_Murattal_128kbps", "Minshawy (Murattal)"),
    HUSARY("Husary_128kbps", "Husary (Murattal)"),
    /** Bacaan pelan ala pengajaran — cocok untuk latihan tahsin. */
    HUSARY_MUALLIM("Husary_Muallim_128kbps", "Husary (Muallim)"),
    ABDUL_BASIT("Abdul_Basit_Murattal_192kbps", "Abdul Basit (Murattal)"),
    ALAFASY("Alafasy_128kbps", "Alafasy"),
    SUDAIS("Abdurrahmaan_As-Sudais_192kbps", "As-Sudais"),
    HUDHAIFY("Hudhaify_128kbps", "Hudhaify"),
    ;

    companion object {
        /** Cari dari slug; null kalau tidak dikenal (fallback ke Minshawy). */
        fun fromSlug(slug: String?): Reciter =
            entries.firstOrNull { it.slug == slug } ?: MINSHAWY
    }
}

/** Pilihan kecepatan pemutaran audio (0.5×–1.25×) untuk latihan pelan-pelan. */
object AudioSpeeds {

    /** Nilai yang ditawarkan di UI, urut naik. */
    val options: List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f)

    /** Nilai aman dalam rentang yang didukung. */
    fun clamp(speed: Float): Float = speed.coerceIn(0.5f, 1.25f)

    /** Label ringkas: 0.5×, 0.75×, 1×, 1.25× (1.0 ditampilkan "1×"). */
    fun format(speed: Float): String {
        val v = if (speed == speed.toInt().toFloat()) speed.toInt().toString()
        else speed.toString().trimEnd('0')
        return "$v×"
    }
}
