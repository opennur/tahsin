package com.ayahofday.app.data.sample

import com.ayahofday.app.data.model.Verse

/**
 * Data contoh untuk skeleton UI.
 * TODO: ganti dengan data asli dari VerseRepository (EQuran.id / Kemenag).
 */
object SampleData {
    val verseOfTheDay = Verse(
        surahNumber = 1,
        ayahNumber = 1,
        surahName = "Al-Fatihah",
        arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        transliteration = "Bismillāhir-raḥmānir-raḥīm",
        translation = "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.",
        tafsir = "Ayat ini mengajarkan agar setiap amal baik dimulai dengan menyebut nama Allah, " +
            "serta mengingatkan dua sifat-Nya: Ar-Rahman (Maha Pengasih di dunia) dan Ar-Rahim " +
            "(Maha Penyayang di akhirat). (Tafsir lengkap akan dimuat dari EQuran.id / Kemenag.)",
        asbabunNuzul = "Asbabun nuzul ayat ini akan dimuat dari sumber tafsir jika tersedia.",
    )

    val bookmarkedVerses = listOf(
        Verse(
            surahNumber = 2,
            ayahNumber = 152,
            surahName = "Al-Baqarah",
            arabic = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
            transliteration = "Fażkurūnī ażkurkum wasykurū lī wa lā takfurūn",
            translation = "Maka ingatlah kepada-Ku, Aku pun akan ingat kepadamu. " +
                "Bersyukurlah kepada-Ku dan janganlah kamu ingkar kepada-Ku.",
        ),
        Verse(
            surahNumber = 94,
            ayahNumber = 6,
            surahName = "Al-Insyirah",
            arabic = "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            transliteration = "Inna ma'al-'usri yusrā",
            translation = "Sesungguhnya bersama kesulitan ada kemudahan.",
        ),
    )

    val lessonsLearned = listOf(
        "Setiap kebaikan hendaknya dimulai dengan menyebut nama Allah.",
        "Allah Maha Pengasih (Ar-Rahman) dan Maha Penyayang (Ar-Rahim).",
        "Menyebut nama Allah menenangkan hati dan mengingatkan tujuan hidup.",
    )

    val practicalDeeds = listOf(
        "Ucapkan basmalah sebelum memulai aktivitas hari ini.",
        "Renungkan satu nama Allah (Asmaul Husna) hari ini.",
        "Bagikan ayat hari ini kepada satu orang terdekat.",
    )
}
