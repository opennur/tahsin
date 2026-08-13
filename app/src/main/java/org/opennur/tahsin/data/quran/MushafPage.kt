package org.opennur.tahsin.data.quran

/**
 * Komposisi HALAMAN mushaf (logika murni, tanpa Android — di-unit-test):
 * paginasi Madani + isi surah → satu halaman siap-render.
 *
 * Isi halaman mengikuti `assets/quran/pages.json` (persis mushaf Madani);
 * komposer hanya menambahkan ornamen yang TIDAK ada di teks: penanda akhir
 * ayat (۝ + nomor Arab-Indik), bendera sujud tilawah (۩), dan basmalah di
 * awal surah (kecuali At-Tawbah; Al-Fatihah ayat 1 sudah basmalah).
 */
object MushafPageComposer {

    /**
     * Susun halaman [pageNumber] dari konten surah [surahContents].
     * Null kalau halaman tidak ada atau konten surah pada halaman itu belum
     * dimuat (pemanggil — ViewModel — memuat konten dulu, lalu menyusun ulang).
     */
    fun composePage(
        pagination: MushafPagination,
        pageNumber: Int,
        surahContents: Map<Int, Surah>,
    ): ComposedPage? {
        val page = pagination.pages.firstOrNull { it.page == pageNumber } ?: return null
        // Halaman tanpa segmen = data rusak (tidak pernah ada di pages.json asli).
        if (page.segments.isEmpty()) return null
        val ayahs = mutableListOf<MushafAyah>()
        var surahStarts = 0
        for (segment in page.segments) {
            val surah = surahContents[segment.surah] ?: return null
            val firstOnPage = segment.fromAyah
            for (ayahNumber in firstOnPage..segment.toAyah) {
                val ayah = surah.ayahs.getOrNull(ayahNumber - 1) ?: return null
                val isSajdah = SajdahSigns.isSajdah(segment.surah, ayahNumber)
                ayahs += MushafAyah(
                    surah = segment.surah,
                    number = ayahNumber,
                    text = displayText(ayah.text),
                    endMarker = AyahNumbering.endOfAyahMarker(ayahNumber),
                    isSajdah = isSajdah,
                    sajdahObligatory = isSajdah && SajdahSigns.ALL.any {
                        it.surah == segment.surah && it.ayah == ayahNumber && it.obligatory
                    },
                    hasBasmalah = ayahNumber == 1 && Basmalah.needsBasmalahOrnament(segment.surah),
                )
            }
            if (firstOnPage == 1 && segment.surah != page.segments.first().surah) {
                surahStarts++
            }
        }
        val firstSurah = page.segments.first().surah
        // Sudah dijamin ada: segmen pertama pasti lolos cek surahContents di atas.
        val headerSurah = surahContents.getValue(firstSurah)
        return ComposedPage(
            page = pageNumber,
            surahNumber = firstSurah,
            surahNameArabic = headerSurah.nameArabic,
            juz = pagination.juzOfPage(pageNumber),
            juzStartsOnPage = pagination.juzStartingOnPage(pageNumber) != null,
            surahStartsMidPage = surahStarts,
            ayahs = ayahs,
        )
    }

    /**
     * Teks tampilan: buang tanda sujud ۩ (U+06E9) dan ornamen akhir surah ࣖ
     * (U+08D6 — artefak equran.id yang tidak didukung banyak font, dirender
     * sebagai kotak). Penanda akhir ayat & sujud digambar UI sebagai badge.
     */
    private fun displayText(text: String): String =
        text.replace("\u06E9", "").replace("\u08D6", "").trimEnd()
}

/** Satu halaman mushaf siap-render (hasil komposer). */
data class ComposedPage(
    val page: Int,
    /** Surah pertama di halaman — untuk judul band header. */
    val surahNumber: Int,
    val surahNameArabic: String,
    /** Juz aktif di awal halaman (header). */
    val juz: Int,
    /** Apakah halaman ini awal sebuah juz (header menampilkan penanda). */
    val juzStartsOnPage: Boolean,
    /** Jumlah surah baru yang mulai di tengah halaman (medali surah). */
    val surahStartsMidPage: Int,
    val ayahs: List<MushafAyah>,
)

/** Satu ayat siap-render di dalam halaman. */
data class MushafAyah(
    val surah: Int,
    val number: Int,
    /** Teks Arab bersih (tanpa ۩ yang dirender terpisah). */
    val text: String,
    /** Penanda akhir ayat: ۝ + nomor Arab-Indik. */
    val endMarker: String,
    /** Tempat sujud tilawah → UI menambahkan ۩ di akhir. */
    val isSajdah: Boolean,
    /** Sajdah wajib (fardhu) menurut mazhab Syafi'i. */
    val sajdahObligatory: Boolean,
    /** Ornamen basmalah sebelum ayat ini (awal surah baru). */
    val hasBasmalah: Boolean,
)
