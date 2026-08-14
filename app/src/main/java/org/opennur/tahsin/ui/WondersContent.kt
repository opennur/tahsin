package org.opennur.tahsin.ui

/**
 * Konten statis layar "Keajaiban & Keindahan Al-Qur'an".
 *
 * Semua item ditulis dengan frasa yang hati-hati (klaim ilmiah disajikan
 * sebagai *keselarasan*, bukan "pembuktian"), dan setiap item membawa sumber
 * yang bisa diverifikasi: quran.com (terjemahan Sahih International, teks
 * diperiksa langsung lewat API resminya), NASA, Britannica, museum, jurnal
 * akademik (BSOAS), dan University of Birmingham/BBC News untuk manuskrip
 * Birmingham. Item yang lemah sebagai "mukjizat sains" (gunung sebagai
 * pasak, sidik jari) sengaja TIDAK diframing ilmiah.
 */

/** Satu item konten: judul, rujukan ayat, teks ID/EN, dan sumber ID/EN + URL. */
data class WonderItem(
    val titleId: String,
    val titleEn: String,
    val reference: String,
    val textId: String,
    val textEn: String,
    val sourceId: String,
    val sourceEn: String,
    val sourceUrl: String,
)

/** Satu kategori di layar Keajaiban: emoji + judul (ID/EN) + item. */
data class WonderCategory(
    val emoji: String,
    val titleId: String,
    val titleEn: String,
    val noteId: String = "",
    val noteEn: String = "",
    val items: List<WonderItem>,
)

/** Daftar kategori & item konten (statis, dua bahasa). */
object WondersContent {

    val categories: List<WonderCategory> = listOf(
        WonderCategory(
            emoji = "🔬",
            titleId = "Keajaiban Ilmiah",
            titleEn = "Scientific Miracles",
            noteId = "Disajikan sebagai keselarasan yang dicatat para ilmuwan (bukan " +
                "\"pembuktian\" lewat sains), dengan frasa hati-hati dan atribusi.",
            noteEn = "Presented as alignments noted by scientists (not \"proof\" through " +
                "science), with careful phrasing and attribution.",
            items = listOf(
                WonderItem(
                    titleId = "Tahapan Penciptaan Manusia",
                    titleEn = "Stages of Human Creation",
                    reference = "QS. Al-Mu'minun 23:12–14",
                    textId = "Al-Qur'an menyebut penciptaan manusia bertahap: dari sari pati " +
                        "tanah, lalu setetes mani, segumpal darah ('alaqah), segumpal daging, " +
                        "tulang yang dibungkus daging, kemudian menjadi makhluk baru. Ahli " +
                        "embriologi Keith Moore (Universitas Toronto, penulis \"The Developing " +
                        "Human\") memaparkan bahwa deskripsi bertahap ini selaras dengan " +
                        "pengetahuan embriologi modern.",
                    textEn = "The Qur'an describes human creation in stages: from an extract " +
                        "of clay, then a sperm-drop, a clinging clot ('alaqah), a lump of " +
                        "flesh, bones covered with flesh, and finally a new creation. " +
                        "Embryologist Keith Moore (University of Toronto, author of \"The " +
                        "Developing Human\") presented that this staged description aligns " +
                        "with modern embryology.",
                    sourceId = "Sumber: K.L. Moore, JIMA 18 (1986) — jurnal kedokteran " +
                        "terindeks; teks & terjemahan: quran.com (Sahih International).",
                    sourceEn = "Source: K.L. Moore, JIMA 18 (1986) — peer-reviewed medical " +
                        "journal; text & translation: quran.com (Sahih International).",
                    sourceUrl = "https://quran.com/23/12-14",
                ),
                WonderItem(
                    titleId = "Alam Semesta yang Mengembang",
                    titleEn = "An Expanding Universe",
                    reference = "QS. Adz-Dzariyat 51:47",
                    textId = "\"Dan langit itu Kami bangun dengan kekuatan, dan sesungguhnya " +
                        "Kami benar-benar meluaskannya.\" (Terjemahan Sahih International). " +
                        "Sering diselaraskan dengan ekspansi alam semesta yang ditemukan " +
                        "astronomi modern. Catatan jujur: kata mūsi'ūn juga dapat dibaca " +
                        "\"Kami membuatnya lapang\" — disajikan sebagai keselarasan, bukan " +
                        "ramalan ilmiah.",
                    textEn = "\"And the heaven We constructed with strength, and indeed, We " +
                        "are [its] expander.\" (Sahih International). Often aligned with the " +
                        "expansion of the universe found by modern astronomy. Honest note: " +
                        "the word mūsi'ūn can also be read \"We make it spacious\" — " +
                        "presented as an alignment, not a scientific prediction.",
                    sourceId = "Sumber: terjemahan Sahih International (quran.com 51:47); " +
                        "NASA — ekspansi alam semesta.",
                    sourceEn = "Source: Sahih International translation (quran.com 51:47); " +
                        "NASA — expansion of the universe.",
                    sourceUrl = "https://quran.com/51/47",
                ),
                WonderItem(
                    titleId = "Langit & Bumi Semula Satu",
                    titleEn = "Heavens and Earth Were One",
                    reference = "QS. Al-Anbiya 21:30",
                    textId = "\"…sesungguhnya langit dan bumi itu dahulunya adalah satu yang " +
                        "padu, kemudian Kami pisahkan antara keduanya…\" Ayat ini kerap " +
                        "diselaraskan dengan gambaran kosmologi modern tentang alam semesta " +
                        "yang bermula dari satu kesatuan. Disajikan sebagai keselarasan " +
                        "interpretatif dengan frasa yang hati-hati.",
                    textEn = "\"…the heavens and the earth were a joined entity, and then We " +
                        "separated them…\" This verse is often aligned with the modern " +
                        "cosmological picture of a universe that began as one entity. " +
                        "Presented as an interpretive alignment with careful phrasing.",
                    sourceId = "Sumber: terjemahan Sahih International (quran.com 21:30); " +
                        "NASA — kosmologi / awal alam semesta.",
                    sourceEn = "Source: Sahih International translation (quran.com 21:30); " +
                        "NASA — cosmology / early universe.",
                    sourceUrl = "https://quran.com/21/30",
                ),
                WonderItem(
                    titleId = "Air: Asal Kehidupan",
                    titleEn = "Water: Origin of Life",
                    reference = "QS. Al-Anbiya 21:30",
                    textId = "Pada ayat yang sama: \"…dan dari air Kami jadikan segala sesuatu " +
                        "yang hidup.\" Biologi modern menegaskan bahwa air merupakan syarat " +
                        "mutlak bagi kehidupan sebagaimana kita kenal. Keselarasan ini dicatat " +
                        "secara luas; disajikan di sini sebagai pengamatan, bukan klaim ilmiah.",
                    textEn = "In the same verse: \"…and We made from water every living " +
                        "thing.\" Modern biology confirms that water is essential to life as " +
                        "we know it. This alignment is widely noted; presented here as an " +
                        "observation, not a scientific claim.",
                    sourceId = "Sumber: terjemahan Sahih International (quran.com 21:30).",
                    sourceEn = "Source: Sahih International translation (quran.com 21:30).",
                    sourceUrl = "https://quran.com/21/30",
                ),
                WonderItem(
                    titleId = "Besi yang \"Diturunkan\"",
                    titleEn = "Iron \"Sent Down\"",
                    reference = "QS. Al-Hadid 57:25",
                    textId = "\"…dan Kami menurunkan besi yang padanya terdapat kekuatan yang " +
                        "hebat dan berbagai manfaat bagi manusia…\" Ilmu material modern: " +
                        "besi di Bumi terbentuk lewat nukleosintesis di inti bintang dan " +
                        "ledakan supernova, lalu tiba ke Bumi dari luar angkasa (mis. lewat " +
                        "ejecta/metorit). Frasa \"menurunkan\" disajikan sebagai keselarasan " +
                        "interpretatif.",
                    textEn = "\"…And We sent down iron, wherein is great military might and " +
                        "benefits for the people…\" Modern material science: Earth's iron " +
                        "formed through nucleosynthesis in stellar cores and supernovae, then " +
                        "arrived on Earth from space (e.g. via ejecta/meteorites). The word " +
                        "\"sent down\" is presented as an interpretive alignment.",
                    sourceId = "Sumber: terjemahan Sahih International (quran.com 57:25); " +
                        "NASA — asal-usul unsur-unsur (origin of the elements).",
                    sourceEn = "Source: Sahih International translation (quran.com 57:25); " +
                        "NASA — origin of the elements.",
                    sourceUrl = "https://quran.com/57/25",
                ),
                WonderItem(
                    titleId = "Benda Langit Beredar pada Orbit",
                    titleEn = "Celestial Bodies in Orbits",
                    reference = "QS. Al-Anbiya 21:33",
                    textId = "\"…dan masing-masing beredar pada garis edarnya.\" (tentang " +
                        "matahari dan bulan). Astronomi modern menegaskan bahwa benda langit " +
                        "bergerak pada orbit yang teratur. Frasa klasik \"berenang\" (yasbahūn) " +
                        "menggambarkan gerakan halus tanpa henti — disajikan sebagai " +
                        "pengamatan tekstual.",
                    textEn = "\"…and all [heavenly bodies] in an orbit are swimming.\" " +
                        "(about the sun and the moon). Modern astronomy confirms celestial " +
                        "bodies move in regular orbits. The classical verb \"swimming\" " +
                        "(yasbahūn) pictures smooth, ceaseless motion — presented as a " +
                        "textual observation.",
                    sourceId = "Sumber: terjemahan Sahih International (quran.com 21:33).",
                    sourceEn = "Source: Sahih International translation (quran.com 21:33).",
                    sourceUrl = "https://quran.com/21/33",
                ),
            ),
        ),
        WonderCategory(
            emoji = "📜",
            titleId = "Kabar Masa Depan",
            titleEn = "News of the Future",
            items = listOf(
                WonderItem(
                    titleId = "Kemenangan Romawi",
                    titleEn = "The Romans' Victory",
                    reference = "QS. Ar-Rum 30:2–4",
                    textId = "\"Bangsa Romawi telah dikalahkan di negeri yang terdekat, dan " +
                        "mereka setelah kekalahannya itu akan menang dalam beberapa tahun " +
                        "(tiga sampai sembilan).\" Ayat ini turun sekitar 615 M, tepat ketika " +
                        "Persia baru merebut Yerusalem (614). Secara historis Romawi memang " +
                        "menang dalam rentang itu lewat kampanye Kaisar Heraclius (622–627). " +
                        "Disajikan sebagai pembacaan tradisional yang peristiwanya tercatat " +
                        "sejarah.",
                    textEn = "\"The Byzantines have been defeated in the nearest land. But " +
                        "they, after their defeat, will overcome within three to nine " +
                        "years.\" The verse was revealed around 615 CE, just as Persia had " +
                        "taken Jerusalem (614). Historically the Byzantines did win within " +
                        "that window through Emperor Heraclius' campaigns (622–627). " +
                        "Presented as the traditional reading, with the events recorded in " +
                        "history.",
                    sourceId = "Sumber: terjemahan Sahih International (quran.com 30:2–4); " +
                        "Britannica — Heraclius (sejarah Kekaisaran Romawi Timur).",
                    sourceEn = "Source: Sahih International translation (quran.com 30:2–4); " +
                        "Britannica — Heraclius (Eastern Roman Empire history).",
                    sourceUrl = "https://quran.com/30/2-4",
                ),
                WonderItem(
                    titleId = "Jasad Fir'aun Tersimpan",
                    titleEn = "Pharaoh's Body Preserved",
                    reference = "QS. Yunus 10:92",
                    textId = "\"Maka pada hari ini Kami selamatkan badanmu supaya engkau " +
                        "menjadi tanda bagi orang-orang yang datang sesudahmu.\" Mumi Ramses II " +
                        "— firaun yang umum dikaitkan dengan kisah ini, meski identifikasi " +
                        "pasti menurut para ahli Mesir Kuno tidak dipastikan — ditemukan di " +
                        "Deir el-Bahari (1881) dan kini disimpan di National Museum of " +
                        "Egyptian Civilization, Kairo. Frasa ayat \"menyelamatkan badanmu\" " +
                        "dibaca sebagai terpeliharanya jasad tersebut.",
                    textEn = "\"But today We will save you in your body that you may be to " +
                        "those after you a sign.\" The mummy of Ramesses II — commonly " +
                        "associated with this story, though Egyptologists do not conclusively " +
                        "identify him as that pharaoh — was found at Deir el-Bahari (1881) and " +
                        "is now kept at the National Museum of Egyptian Civilization, Cairo. " +
                        "The verse's \"saving your body\" is read as the body's preservation.",
                    sourceId = "Sumber: terjemahan Sahih International (quran.com 10:92); " +
                        "National Museum of Egyptian Civilization (ruang mumi).",
                    sourceEn = "Source: Sahih International translation (quran.com 10:92); " +
                        "National Museum of Egyptian Civilization (Royal Mummies Hall).",
                    sourceUrl = "https://quran.com/10/92",
                ),
            ),
        ),
        WonderCategory(
            emoji = "💬",
            titleId = "Keindahan Bahasa",
            titleEn = "Linguistic Beauty",
            noteId = "I'jaz (kemukjizatan) dari sisi bahasa dibahas para ulama dan " +
                "akademisi; klaim estetis disajikan sebagai doktrin, dan observasi " +
                "tekstual bisa diverifikasi sendiri.",
            noteEn = "Linguistic i'jaz (inimitability) is discussed by scholars and " +
                "academics; aesthetic claims are presented as doctrine, and textual " +
                "observations can be verified by yourself.",
            items = listOf(
                WonderItem(
                    titleId = "Tantangan untuk Menandingi (Tahaddi)",
                    titleEn = "The Challenge to Match It (Tahaddi)",
                    reference = "QS. Al-Baqarah 2:23 · QS. Al-Isra 17:88",
                    textId = "\"…maka datangkanlah satu surah semisalnya…\" dan \"Katakanlah: " +
                        "\"Sesungguhnya jika manusia dan jin berkumpul untuk membuat yang " +
                        "serupa dengan Al-Qur'an ini, niscaya mereka tidak akan mampu \"…\" " +
                        "Para ulama menjadikan tantangan ini fondasi doktrin i'jaz Al-Qur'an. " +
                        "Disajikan sebagai klaim doktrinal/estetis — bukan klaim empiris " +
                        "yang bisa diuji.",
                    textEn = "\"…then bring a surah like it…\" and \"Say: 'If mankind and " +
                        "the jinn gathered to produce the like of this Qur'an, they could " +
                        "not…'\" Scholars make this challenge the foundation of the doctrine " +
                        "of the Qur'an's inimitability (i'jaz). Presented as a doctrinal / " +
                        "aesthetic claim — not an empirically testable one.",
                    sourceId = "Sumber: terjemahan Sahih International (quran.com 2:23, " +
                        "17:88); literatur 'ulumul Qur'an tentang i'jaz.",
                    sourceEn = "Source: Sahih International translation (quran.com 2:23, " +
                        "17:88); 'ulum al-Qur'an literature on i'jaz.",
                    sourceUrl = "https://quran.com/2/23",
                ),
                WonderItem(
                    titleId = "Bukan Syair, Bukan Prosa",
                    titleEn = "Neither Poetry nor Prose",
                    reference = "QS. Yasin 36:69",
                    textId = "\"Dan Kami tidak mengajarkan syair kepadanya (Muhammad) dan " +
                        "tidaklah pantas baginya.\" Pada masa turunnya, syair adalah standar " +
                        "keunggulan sastra Arab; Al-Qur'an hadir dengan gaya yang tidak " +
                        "tergolong syair maupun prosa biasa — salah satu ciri yang dibahas " +
                        "dalam kajian i'jaz.",
                    textEn = "\"And We did not teach him (Muhammad) poetry, nor is it " +
                        "befitting for him.\" In its era, poetry was the standard of Arabic " +
                        "literary excellence; the Qur'an came in a style that is neither " +
                        "poetry nor ordinary prose — one of the features discussed in i'jaz " +
                        "studies.",
                    sourceId = "Sumber: terjemahan Sahih International (quran.com 36:69).",
                    sourceEn = "Source: Sahih International translation (quran.com 36:69).",
                    sourceUrl = "https://quran.com/36/69",
                ),
                WonderItem(
                    titleId = "Iltifat: Pergeseran Gramatikal",
                    titleEn = "Iltifat: Grammatical Shift",
                    reference = "Contoh: QS. Al-Fatihah 1:5",
                    textId = "Pergeseran persona gramatikal (mis. dari orang ketiga ke orang " +
                        "kedua) yang lazim dalam Al-Qur'an diakui sebagai perangkat retoris " +
                        "yang disengaja — bukan ketidakonsistenan. Kajian akademik rujukan: " +
                        "M.A.S. Abdel Haleem, \"Grammatical Shift for Rhetorical Purposes: " +
                        "Iltifāt and Related Features in the Qur'ān\", Bulletin of SOAS 55.3 " +
                        "(1992).",
                    textEn = "Grammatical person shifts (e.g. from third to second person) " +
                        "common in the Qur'an are recognized as a deliberate rhetorical " +
                        "device — not inconsistency. Reference academic study: M.A.S. Abdel " +
                        "Haleem, \"Grammatical Shift for Rhetorical Purposes: Iltifāt and " +
                        "Related Features in the Qur'ān\", Bulletin of SOAS 55.3 (1992).",
                    sourceId = "Sumber: jurnal akademik BSOAS 55.3 (1992), Cambridge " +
                        "University Press.",
                    sourceEn = "Source: academic journal BSOAS 55.3 (1992), Cambridge " +
                        "University Press.",
                    sourceUrl = "https://www.cambridge.org/core/journals/bulletin-of-the-school-of-oriental-and-african-studies",
                ),
                WonderItem(
                    titleId = "Konsistensi Urutan Kata",
                    titleEn = "Consistent Word Order",
                    reference = "Mis. QS. Al-Baqarah 2:257 · QS. Ibrahim 14:1",
                    textId = "Bila \"kegelapan\" (ẓulumāt) dan \"cahaya\" (nūr) muncul dalam " +
                        "satu ayat, kegelapan selalu disebut lebih dulu; dalam ayat-ayat " +
                        "penciptaan, \"langit\" selalu disebut sebelum \"bumi\". Ini observasi " +
                        "tekstual yang bisa diverifikasi sendiri lewat pencarian quran.com — " +
                        "contoh keteraturan yang disengaja dalam gaya Al-Qur'an.",
                    textEn = "Whenever \"darknesses\" (ẓulumāt) and \"light\" (nūr) appear " +
                        "in the same verse, darkness is always named first; in creation " +
                        "verses, \"heavens\" is always named before \"earth\". This is a " +
                        "textual observation you can verify yourself via quran.com search — " +
                        "an example of deliberate orderliness in the Qur'an's style.",
                    sourceId = "Sumber: observasi teks — verifikasi lewat pencarian " +
                        "quran.com (mis. 2:257, 14:1, 57:9; 7:54, 10:3, 11:7).",
                    sourceEn = "Source: textual observation — verify via quran.com search " +
                        "(e.g. 2:257, 14:1, 57:9; 7:54, 10:3, 11:7).",
                    sourceUrl = "https://quran.com/2/257",
                ),
            ),
        ),
        WonderCategory(
            emoji = "🛡️",
            titleId = "Terjaga Sepanjang Masa",
            titleEn = "Preserved Through Time",
            items = listOf(
                WonderItem(
                    titleId = "Janji Penjagaan",
                    titleEn = "The Promise of Preservation",
                    reference = "QS. Al-Hijr 15:9",
                    textId = "\"Sesungguhnya Kamilah yang menurunkan Al-Qur'an, dan pasti " +
                        "Kami (pula) yang memeliharanya.\" Teks Al-Qur'an yang kita miliki " +
                        "hari ini dipelihara lewat tradisi hafalan dan penulisan yang " +
                        "berkesinambungan sejak masa Nabi Muhammad ﷺ.",
                    textEn = "\"Indeed, it is We who sent down the Qur'an, and indeed, We " +
                        "will be its guardian.\" The Qur'anic text we have today has been " +
                        "preserved through an unbroken tradition of memorization and writing " +
                        "since the time of Prophet Muhammad ﷺ.",
                    sourceId = "Sumber: terjemahan Sahih International (quran.com 15:9).",
                    sourceEn = "Source: Sahih International translation (quran.com 15:9).",
                    sourceUrl = "https://quran.com/15/9",
                ),
                WonderItem(
                    titleId = "Manuskrip Birmingham",
                    titleEn = "The Birmingham Manuscript",
                    reference = "Mingana 1572a · QS. Al-Kahf 18–QS. Thaha 20",
                    textId = "Folio Al-Qur'an di University of Birmingham (surah 18–20) " +
                        "diradiokarbon bertanggal 568–645 M (tingkat kepercayaan 95,4%) — " +
                        "tumpang tindih dengan masa hidup Nabi Muhammad ﷺ. Ini salah satu " +
                        "saksi tertua teks Al-Qur'an yang diketahui.",
                    textEn = "Qur'an folios at the University of Birmingham (surahs 18–20) " +
                        "were radiocarbon-dated to 568–645 CE (95.4% confidence) — " +
                        "overlapping with the lifetime of Prophet Muhammad ﷺ. They are among " +
                        "the oldest known witnesses of the Qur'anic text.",
                    sourceId = "Sumber: University of Birmingham (Cadbury Research Library); " +
                        "BBC News (2015).",
                    sourceEn = "Source: University of Birmingham (Cadbury Research Library); " +
                        "BBC News (2015).",
                    sourceUrl = "https://www.bbc.com/news/business-33436021",
                ),
            ),
        ),
    )
}
