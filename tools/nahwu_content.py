# -*- coding: utf-8 -*-
"""Konten kursus Nahwu pemula yang ditulis orisinal dari nol."""


def choice(prompt_id, prompt_en, prompt_ar, prompt_latin, options_id, options_en, answer):
    return {
        "type": "choice",
        "promptId": prompt_id,
        "promptEn": prompt_en,
        "promptAr": prompt_ar,
        "promptLatin": prompt_latin,
        "optionsId": options_id,
        "optionsEn": options_en,
        "answerIndex": answer,
    }


def rearrange(prompt_id, prompt_en, words):
    return {
        "type": "rearrange",
        "promptId": prompt_id,
        "promptEn": prompt_en,
        "words": [{"ar": ar, "latin": latin} for ar, latin in words],
    }


LEVELS = [
    {
        "id": 1,
        "titleId": "Level 1 — Dasar Kalimat",
        "titleEn": "Level 1 — Sentence Basics",
        "titleAr": "المُسْتَوَى الأَوَّلُ",
        "lessons": [
            {
                "id": "1-1",
                "titleId": "Kalimah dan Tiga Jenis Kata",
                "titleEn": "Kalimah and the Three Word Types",
                "titleAr": "الْكَلِمَةُ وَأَقْسَامُهَا",
                "introId": "Dalam Nahwu, kalimah adalah kata yang memiliki makna. Kalimah terbagi menjadi isim, fi'il, dan harf.",
                "introEn": "In Nahwu, a kalimah is a word with meaning. It is divided into noun, verb, and particle.",
                "rules": [
                    {"titleId": "Isim — kata benda atau nama", "titleEn": "Isim — a noun or name", "explanationId": "Isim menunjukkan benda, orang, tempat, atau sifat dan tidak terikat waktu.", "explanationEn": "An isim points to a thing, person, place, or quality and is not tied to a time.", "exampleAr": "هٰذَا كِتَابٌ", "exampleLatin": "Hādhā kitābun", "exampleId": "Ini sebuah buku", "exampleEn": "This is a book"},
                    {"titleId": "Fi'il dan harf", "titleEn": "Fi'il and harf", "explanationId": "Fi'il menunjukkan pekerjaan yang terkait waktu. Harf menghubungkan makna kata lain.", "explanationEn": "A fi'il shows an action connected to time. A harf connects the meaning of other words.", "exampleAr": "يَكْتُبُ الطَّالِبُ فِي الدَّفْتَرِ", "exampleLatin": "Yaktubu ṭ-ṭālibu fī d-daftari", "exampleId": "Siswa menulis di buku tulis", "exampleEn": "The student writes in the notebook"},
                ],
                "exercises": [
                    choice("Kata هُوَ termasuk jenis apa?", "What type of word is هُوَ?", "هُوَ", "Huwa", ["Isim", "Fi'il", "Harf", "Jumlah"], ["Noun", "Verb", "Particle", "Sentence"], 0),
                    choice("Kata يَذْهَبُ termasuk jenis apa?", "What type of word is يَذْهَبُ?", "يَذْهَبُ", "Yadhhabu", ["Isim", "Fi'il", "Harf", "Sifat"], ["Noun", "Verb", "Particle", "Adjective"], 1),
                    choice("Kata فِي termasuk jenis apa?", "What type of word is فِي?", "فِي الْبَيْتِ", "Fī l-bayti", ["Isim", "Fi'il", "Harf", "Mubtada'"], ["Noun", "Verb", "Particle", "Subject"], 2),
                    rearrange("Susun kalimat: Siswa menulis di buku.", "Arrange the sentence: The student writes in the book.", [("يَكْتُبُ", "yaktubu"), ("الطَّالِبُ", "aṭ-ṭālibu"), ("فِي", "fī"), ("الْكِتَابِ", "al-kitābi")]),
                ],
            },
            {
                "id": "1-2",
                "titleId": "Ma'rifah dan Nakirah",
                "titleEn": "Definite and Indefinite Nouns",
                "titleAr": "الْمَعْرِفَةُ وَالنَّكِرَةُ",
                "introId": "Isim ma'rifah menunjuk sesuatu yang tertentu. Isim nakirah menunjuk sesuatu yang belum tertentu.",
                "introEn": "A definite noun points to something specific. An indefinite noun points to something not yet specific.",
                "rules": [
                    {"titleId": "Alif lam menunjukkan ma'rifah", "titleEn": "Alif lam marks definiteness", "explanationId": "Kata الْكِتَابُ berarti buku yang tertentu, sedangkan كِتَابٌ berarti sebuah buku.", "explanationEn": "الْكِتَابُ means the specific book, while كِتَابٌ means a book.", "exampleAr": "هٰذَا الْكِتَابُ مُفِيدٌ", "exampleLatin": "Hādhā l-kitābu mufīdun", "exampleId": "Buku ini bermanfaat", "exampleEn": "This book is useful"},
                    {"titleId": "Tanwin adalah tanda nakirah", "titleEn": "Tanwin marks indefiniteness", "explanationId": "Tanwin pada akhir isim, seperti كِتَابٌ, biasanya menunjukkan makna belum tertentu.", "explanationEn": "Tanwin at the end of a noun, such as كِتَابٌ, usually marks an indefinite meaning.", "exampleAr": "عِنْدِي كِتَابٌ", "exampleLatin": "ʿIndī kitābun", "exampleId": "Aku mempunyai sebuah buku", "exampleEn": "I have a book"},
                ],
                "exercises": [
                    choice("Mana yang berarti 'buku itu'?", "Which one means 'the book'?", "____ كِتَابٌ", "____ kitābun", ["كِتَابٌ", "الْكِتَابُ", "كِتَابًا", "كِتَابٍ"], ["a book", "the book", "a book (object)", "a book (after a preposition)"], 1),
                    choice("Kata كِتَابٌ termasuk...", "The word كِتَابٌ is...", "كِتَابٌ", "Kitābun", ["Ma'rifah", "Nakirah", "Fi'il", "Harf"], ["Definite", "Indefinite", "Verb", "Particle"], 1),
                    choice("Lengkapi: هٰذَا ____ مُفِيدٌ", "Complete: This ____ is useful", "هٰذَا ____ مُفِيدٌ", "Hādhā ____ mufīdun", ["كِتَابٌ", "الْكِتَابُ", "كِتَابًا", "كِتَابٍ"], ["a book", "the book", "a book (object)", "a book (after a preposition)"], 1),
                    rearrange("Susun kalimat: Aku mempunyai sebuah buku.", "Arrange the sentence: I have a book.", [("عِنْدِي", "ʿindī"), ("كِتَابٌ", "kitābun")]),
                ],
            },
            {
                "id": "1-3",
                "titleId": "Jumlah Ismiyyah",
                "titleEn": "The Nominal Sentence",
                "titleAr": "الْجُمْلَةُ الِاسْمِيَّةُ",
                "introId": "Jumlah ismiyyah dimulai dengan isim. Dua unsur dasarnya adalah mubtada' dan khabar.",
                "introEn": "A nominal sentence begins with a noun. Its two basic parts are the mubtada' and khabar.",
                "rules": [
                    {"titleId": "Mubtada' adalah pokok pembicaraan", "titleEn": "Mubtada' is the topic", "explanationId": "Mubtada' biasanya berada di awal jumlah ismiyyah dan menjadi pokok yang dibicarakan.", "explanationEn": "The mubtada' usually comes first in a nominal sentence and is its topic.", "exampleAr": "الطَّالِبُ مُجْتَهِدٌ", "exampleLatin": "Aṭ-ṭālibu mujtahidun", "exampleId": "Siswa itu rajin", "exampleEn": "The student is diligent"},
                    {"titleId": "Khabar memberi informasi", "titleEn": "Khabar gives information", "explanationId": "Khabar memberi informasi tentang mubtada' dan biasanya mengikuti mubtada' dalam keadaan marfu'.", "explanationEn": "The khabar gives information about the mubtada' and usually follows it in the nominative case.", "exampleAr": "الْبَيْتُ كَبِيرٌ", "exampleLatin": "Al-baytu kabīrun", "exampleId": "Rumah itu besar", "exampleEn": "The house is big"},
                ],
                "exercises": [
                    choice("Apa mubtada' dalam الْبَيْتُ كَبِيرٌ?", "What is the mubtada' in الْبَيْتُ كَبِيرٌ?", "الْبَيْتُ كَبِيرٌ", "Al-baytu kabīrun", ["الْبَيْتُ", "كَبِيرٌ", "كَبِيرًا", "فِي"], ["الْبَيْتُ", "كَبِيرٌ", "كَبِيرًا", "فِي"], 0),
                    choice("Apa khabar dalam الطَّالِبُ مُجْتَهِدٌ?", "What is the khabar in الطَّالِبُ مُجْتَهِدٌ?", "الطَّالِبُ مُجْتَهِدٌ", "Aṭ-ṭālibu mujtahidun", ["الطَّالِبُ", "مُجْتَهِدٌ", "الطَّالِبَ", "مِنْ"], ["الطَّالِبُ", "مُجْتَهِدٌ", "الطَّالِبَ", "مِنْ"], 1),
                    choice("Arti الْبَيْتُ كَبِيرٌ adalah...", "The meaning of الْبَيْتُ كَبِيرٌ is...", "الْبَيْتُ كَبِيرٌ", "Al-baytu kabīrun", ["Rumah itu besar", "Rumah itu kecil", "Aku melihat rumah", "Di dalam rumah"], ["The house is big", "The house is small", "I see a house", "Inside the house"], 0),
                    rearrange("Susun kalimat: Rumah itu besar.", "Arrange the sentence: The house is big.", [("الْبَيْتُ", "al-baytu"), ("كَبِيرٌ", "kabīrun")]),
                ],
            },
            {
                "id": "1-4",
                "titleId": "Jumlah Fi'liyyah dan Fa'il",
                "titleEn": "The Verbal Sentence and Subject",
                "titleAr": "الْجُمْلَةُ الْفِعْلِيَّةُ وَالْفَاعِلُ",
                "introId": "Jumlah fi'liyyah dimulai dengan fi'il. Fa'il adalah pelaku pekerjaan.",
                "introEn": "A verbal sentence begins with a verb. The fa'il is the doer of the action.",
                "rules": [
                    {"titleId": "Fi'il berada di awal", "titleEn": "The verb comes first", "explanationId": "Dalam pola dasar jumlah fi'liyyah, fi'il disebut sebelum fa'il.", "explanationEn": "In the basic verbal-sentence pattern, the verb is mentioned before the fa'il.", "exampleAr": "ذَهَبَ الطَّالِبُ", "exampleLatin": "Dhahaba ṭ-ṭālibu", "exampleId": "Siswa itu pergi", "exampleEn": "The student went"},
                    {"titleId": "Fa'il adalah pelaku", "titleEn": "Fa'il is the doer", "explanationId": "Fa'il menjawab pertanyaan siapa yang melakukan pekerjaan dan berstatus marfu'.", "explanationEn": "The fa'il answers who performed the action and is in the nominative case.", "exampleAr": "كَتَبَ أَحْمَدُ الدَّرْسَ", "exampleLatin": "Kataba Aḥmadu d-darsa", "exampleId": "Ahmad menulis pelajaran", "exampleEn": "Ahmad wrote the lesson"},
                ],
                "exercises": [
                    choice("Apa fa'il dalam جَلَسَ الْوَلَدُ?", "What is the fa'il in جَلَسَ الْوَلَدُ?", "جَلَسَ الْوَلَدُ", "Jalasa l-waladu", ["جَلَسَ", "الْوَلَدُ", "الْوَلَدَ", "فِي"], ["جَلَسَ", "الْوَلَدُ", "الْوَلَدَ", "فِي"], 1),
                    choice("Kalimat mana yang merupakan jumlah fi'liyyah?", "Which sentence is a verbal sentence?", "اخْتَرِ الْجُمْلَةَ الْفِعْلِيَّةَ", "Ikhtari l-jumlah", ["الْجَوُّ جَمِيلٌ", "ذَهَبَ عَلِيٌّ", "الْبَيْتُ كَبِيرٌ", "الطَّالِبُ نَشِيطٌ"], ["The weather is nice", "Ali went", "The house is big", "The student is active"], 1),
                    choice("Fa'il biasanya berstatus...", "The fa'il is usually in the...", "الْفَاعِلُ مَرْفُوعٌ", "Al-fāʿilu marfūʿun", ["Marfu'", "Manshub", "Majrur", "Mabni"], ["Nominative", "Accusative", "Genitive", "Fixed form"], 0),
                    rearrange("Susun kalimat: Ahmad menulis pelajaran.", "Arrange the sentence: Ahmad wrote the lesson.", [("كَتَبَ", "kataba"), ("أَحْمَدُ", "Aḥmadu"), ("الدَّرْسَ", "ad-darsa")]),
                ],
            },
        ],
    },
    {
        "id": 2,
        "titleId": "Level 2 — Unsur Kalimat",
        "titleEn": "Level 2 — Sentence Elements",
        "titleAr": "المُسْتَوَى الثَّانِي",
        "lessons": [
            {
                "id": "2-1",
                "titleId": "Maf'ul Bih",
                "titleEn": "The Direct Object",
                "titleAr": "الْمَفْعُولُ بِهِ",
                "introId": "Maf'ul bih adalah sesuatu yang dikenai pekerjaan. Ia biasanya berstatus manshub.",
                "introEn": "The maf'ul bih is what receives the action. It is usually in the accusative case.",
                "rules": [
                    {"titleId": "Maf'ul bih menerima pekerjaan", "titleEn": "Maf'ul bih receives the action", "explanationId": "Tanyakan 'apa yang dikerjakan?' untuk menemukan maf'ul bih.", "explanationEn": "Ask 'what was acted upon?' to find the maf'ul bih.", "exampleAr": "قَرَأَ الطَّالِبُ الْكِتَابَ", "exampleLatin": "Qara'a ṭ-ṭālibu l-kitāba", "exampleId": "Siswa membaca buku itu", "exampleEn": "The student read the book"},
                    {"titleId": "Tanda fathah pada maf'ul", "titleEn": "Fathah on the maf'ul", "explanationId": "Isim mufrad sebagai maf'ul bih biasanya berakhir fathah, seperti الْكِتَابَ.", "explanationEn": "A singular noun as maf'ul bih usually ends with fathah, such as الْكِتَابَ.", "exampleAr": "شَرِبَ الْوَلَدُ الْمَاءَ", "exampleLatin": "Shariba l-waladu l-mā'a", "exampleId": "Anak itu minum air", "exampleEn": "The child drank the water"},
                ],
                "exercises": [
                    choice("Apa maf'ul bih dalam قَرَأَ الطَّالِبُ الْكِتَابَ?", "What is the maf'ul bih in قَرَأَ الطَّالِبُ الْكِتَابَ?", "قَرَأَ الطَّالِبُ الْكِتَابَ", "Qara'a ṭ-ṭālibu l-kitāba", ["قَرَأَ", "الطَّالِبُ", "الْكِتَابَ", "الطَّالِبِ"], ["قَرَأَ", "الطَّالِبُ", "الْكِتَابَ", "الطَّالِبِ"], 2),
                    choice("Maf'ul bih biasanya berstatus...", "The maf'ul bih is usually in the...", "الْمَفْعُولُ بِهِ", "Al-mafʿūlu bihi", ["Marfu'", "Manshub", "Majrur", "Majzum"], ["Nominative", "Accusative", "Genitive", "Jussive"], 1),
                    choice("Lengkapi: شَرِبَ الْوَلَدُ ____", "Complete: The child drank ____", "شَرِبَ الْوَلَدُ ____", "Shariba l-waladu ____", ["الْمَاءُ", "الْمَاءَ", "الْمَاءِ", "مَاءٌ"], ["الْمَاءُ", "الْمَاءَ", "الْمَاءِ", "مَاءٌ"], 1),
                    rearrange("Susun kalimat: Siswa membaca buku itu.", "Arrange the sentence: The student read the book.", [("قَرَأَ", "qara'a"), ("الطَّالِبُ", "aṭ-ṭālibu"), ("الْكِتَابَ", "al-kitāba")]),
                ],
            },
            {
                "id": "2-2",
                "titleId": "Harf Jar dan Isim Majrur",
                "titleEn": "Prepositions and Genitive Nouns",
                "titleAr": "حُرُوفُ الْجَرِّ وَالِاسْمُ الْمَجْرُورُ",
                "introId": "Harf jar membuat isim setelahnya menjadi majrur. Tanda yang sering terlihat adalah kasrah.",
                "introEn": "A preposition makes the noun after it genitive. The common visible sign is kasrah.",
                "rules": [
                    {"titleId": "Harf jar menghubungkan kata", "titleEn": "A preposition connects words", "explanationId": "فِي berarti di dalam, مِنْ berarti dari, dan إِلَى berarti menuju.", "explanationEn": "فِي means in, مِنْ means from, and إِلَى means to.", "exampleAr": "فِي الْمَسْجِدِ", "exampleLatin": "Fī l-masjidi", "exampleId": "Di dalam masjid", "exampleEn": "In the mosque"},
                    {"titleId": "Isim setelah jar menjadi majrur", "titleEn": "The noun after a preposition is genitive", "explanationId": "Perhatikan kasrah pada الْمَسْجِدِ setelah فِي.", "explanationEn": "Notice the kasrah on الْمَسْجِدِ after فِي.", "exampleAr": "ذَهَبْتُ إِلَى الْمَدْرَسَةِ", "exampleLatin": "Dhahabtu ilā l-madrasati", "exampleId": "Aku pergi ke sekolah", "exampleEn": "I went to the school"},
                ],
                "exercises": [
                    choice("Kata setelah فِي harus berstatus...", "The word after فِي must be...", "فِي الْبَيْتِ", "Fī l-bayti", ["Marfu'", "Manshub", "Majrur", "Majzum"], ["Nominative", "Accusative", "Genitive", "Jussive"], 2),
                    choice("Mana bentuk yang benar setelah إِلَى?", "Which form is correct after إِلَى?", "إِلَى ____", "Ilā ____", ["الْمَدْرَسَةُ", "الْمَدْرَسَةَ", "الْمَدْرَسَةِ", "مَدْرَسَةٌ"], ["الْمَدْرَسَةُ", "الْمَدْرَسَةَ", "الْمَدْرَسَةِ", "مَدْرَسَةٌ"], 2),
                    choice("Arti مِنَ الْبَيْتِ adalah...", "The meaning of مِنَ الْبَيْتِ is...", "مِنَ الْبَيْتِ", "Mina l-bayti", ["Ke rumah", "Dari rumah", "Di rumah", "Rumah itu"], ["To the house", "From the house", "In the house", "The house"], 1),
                    rearrange("Susun kalimat: Aku pergi ke sekolah.", "Arrange the sentence: I went to school.", [("ذَهَبْتُ", "dhahabtu"), ("إِلَى", "ilā"), ("الْمَدْرَسَةِ", "al-madrasati")]),
                ],
            },
        ],
    },
    {
        "id": 3,
        "titleId": "Level 3 — Keserasian dan I'rab",
        "titleEn": "Level 3 — Agreement and Case Endings",
        "titleAr": "المُسْتَوَى الثَّالِثُ",
        "lessons": [
            {
                "id": "3-1",
                "titleId": "Na'at dan Man'ut",
                "titleEn": "Adjectives and Nouns",
                "titleAr": "النَّعْتُ وَالْمَنْعُوتُ",
                "introId": "Na'at adalah sifat, sedangkan man'ut adalah kata yang disifati. Keduanya mengikuti keserasian tertentu.",
                "introEn": "The na'at is an adjective and the man'ut is the noun it describes. They follow a pattern of agreement.",
                "rules": [
                    {"titleId": "Sifat mengikuti kata benda", "titleEn": "The adjective follows the noun", "explanationId": "Na'at mengikuti man'ut dalam ma'rifah dan nakirah serta jenis kelamin.", "explanationEn": "The na'at agrees with the man'ut in definiteness, indefiniteness, and gender.", "exampleAr": "بَيْتٌ كَبِيرٌ", "exampleLatin": "Baytun kabīrun", "exampleId": "Sebuah rumah besar", "exampleEn": "A big house"},
                    {"titleId": "Akhiran mengikuti posisi", "titleEn": "Endings follow the grammatical position", "explanationId": "Na'at juga mengikuti man'ut dalam keadaan marfu', manshub, atau majrur.", "explanationEn": "The na'at also follows the man'ut in nominative, accusative, or genitive case.", "exampleAr": "رَأَيْتُ بَيْتًا كَبِيرًا", "exampleLatin": "Ra'aytu baytan kabīran", "exampleId": "Aku melihat rumah besar", "exampleEn": "I saw a big house"},
                ],
                "exercises": [
                    choice("Lengkapi: بَيْتٌ ____", "Complete: بَيْتٌ ____", "بَيْتٌ ____", "Baytun ____", ["كَبِيرٌ", "كَبِيرًا", "كَبِيرٍ", "كَبِيرَةٌ"], ["كَبِيرٌ", "كَبِيرًا", "كَبِيرٍ", "كَبِيرَةٌ"], 0),
                    choice("Lengkapi: رَأَيْتُ سَيَّارَةً ____", "Complete: I saw a ____ car", "رَأَيْتُ سَيَّارَةً ____", "Ra'aytu sayyāratan ____", ["جَدِيدٌ", "جَدِيدًا", "جَدِيدٍ", "جَدِيدَةً"], ["جَدِيدٌ", "جَدِيدًا", "جَدِيدٍ", "جَدِيدَةً"], 3),
                    choice("Na'at mengikuti man'ut dalam...", "The na'at agrees with the man'ut in...", "النَّعْتُ يَتْبَعُ الْمَنْعُوتَ", "An-naʿtu yatbaʿu l-manʿūta", ["Ma'rifah saja", "Jenis kelamin saja", "Beberapa keadaan", "Tidak ada"], ["Definiteness only", "Gender only", "Several features", "Nothing"], 2),
                    rearrange("Susun kalimat: Sebuah rumah besar.", "Arrange the phrase: A big house.", [("بَيْتٌ", "baytun"), ("كَبِيرٌ", "kabīrun")]),
                ],
            },
            {
                "id": "3-2",
                "titleId": "Tanda I'rab Dasar",
                "titleEn": "Basic Case Endings",
                "titleAr": "عَلَامَاتُ الْإِعْرَابِ الْأَسَاسِيَّةُ",
                "introId": "I'rab adalah perubahan akhir kata sesuai kedudukannya. Tanda dasar yang dipelajari adalah dhammah, fathah, dan kasrah.",
                "introEn": "I'rab is the change at the end of a word according to its role. The basic signs are dammah, fathah, and kasrah.",
                "rules": [
                    {"titleId": "Dhammah untuk marfu'", "titleEn": "Dammah marks the nominative", "explanationId": "Fa'il dan mubtada' yang berupa isim tunggal biasanya memakai dhammah.", "explanationEn": "A singular fa'il and mubtada' usually take dammah.", "exampleAr": "جَاءَ الطَّالِبُ", "exampleLatin": "Jā'a ṭ-ṭālibu", "exampleId": "Siswa itu datang", "exampleEn": "The student came"},
                    {"titleId": "Fathah dan kasrah", "titleEn": "Fathah and kasrah", "explanationId": "Maf'ul bih biasanya memakai fathah, sedangkan isim setelah harf jar memakai kasrah.", "explanationEn": "The maf'ul bih usually takes fathah, while a noun after a preposition takes kasrah.", "exampleAr": "رَأَيْتُ الطَّالِبَ فِي الْفَصْلِ", "exampleLatin": "Ra'aytu ṭ-ṭāliba fī l-faṣli", "exampleId": "Aku melihat siswa itu di kelas", "exampleEn": "I saw the student in the classroom"},
                ],
                "exercises": [
                    choice("Tanda dasar marfu' adalah...", "The basic sign of the nominative is...", "الْمَرْفُوعُ", "Al-marfūʿu", ["Dhammah", "Fathah", "Kasrah", "Sukun"], ["Dammah", "Fathah", "Kasrah", "Sukun"], 0),
                    choice("Pilih bentuk fa'il yang benar: جَاءَ ____", "Choose the correct fa'il: ____ came", "جَاءَ ____", "Jā'a ____", ["الطَّالِبَ", "الطَّالِبِ", "الطَّالِبُ", "الطَّالِبْ"], ["الطَّالِبَ", "الطَّالِبِ", "الطَّالِبُ", "الطَّالِبْ"], 2),
                    choice("Pilih bentuk setelah فِي: فِي ____", "Choose the form after فِي: in the...", "فِي ____", "Fī ____", ["الْفَصْلُ", "الْفَصْلَ", "الْفَصْلِ", "الْفَصْلْ"], ["الْفَصْلُ", "الْفَصْلَ", "الْفَصْلِ", "الْفَصْلْ"], 2),
                    rearrange("Susun kalimat: Aku melihat siswa itu di kelas.", "Arrange: I saw the student in the classroom.", [("رَأَيْتُ", "ra'aytu"), ("الطَّالِبَ", "aṭ-ṭāliba"), ("فِي", "fī"), ("الْفَصْلِ", "al-faṣli")]),
                ],
            },
        ],
    },
]
