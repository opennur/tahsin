package org.opennur.tahsin.util

/** Dua kontrak tampilan mushaf yang dapat dipilih pengguna. */
enum class MushafRenderMode(val key: String) {
    /** Halaman Madani 15 baris dengan ukuran dan tinggi baris yang terkunci. */
    EXACT("exact"),
    /** Teks mengalir yang mengikuti ukuran huruf dan aksesibilitas sistem. */
    ACCESSIBLE("accessible"),
    ;

    companion object {
        fun fromKey(key: String): MushafRenderMode =
            entries.firstOrNull { it.key == key } ?: ACCESSIBLE
    }
}
