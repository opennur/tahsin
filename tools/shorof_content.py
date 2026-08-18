# -*- coding: utf-8 -*-
"""Konten kursus Shorof pemula, ditulis orisinal dari nol."""


def choice(prompt_id, prompt_en, prompt_ar, prompt_latin, options_id, options_en, answer):
    return {
        "type": "choice", "promptId": prompt_id, "promptEn": prompt_en,
        "promptAr": prompt_ar, "promptLatin": prompt_latin,
        "optionsId": options_id, "optionsEn": options_en, "answerIndex": answer,
    }


def lesson(id_, title_id, title_en, title_ar, intro_id, intro_en, rules, patterns, exercises, conjugations=None):
    if conjugations is not None:
        exercises, conjugations = conjugations, exercises
    return {
        "id": id_, "titleId": title_id, "titleEn": title_en, "titleAr": title_ar,
        "introId": intro_id, "introEn": intro_en, "rules": rules,
        "patterns": patterns, "conjugations": conjugations or [], "exercises": exercises,
    }


def rule(title_id, title_en, explanation_id, explanation_en, ar, latin, id_meaning, en_meaning):
    return {
        "titleId": title_id, "titleEn": title_en, "explanationId": explanation_id,
        "explanationEn": explanation_en, "exampleAr": ar, "exampleLatin": latin,
        "exampleId": id_meaning, "exampleEn": en_meaning,
    }


def pattern(root, root_latin, wazan, wazan_latin, form_id, form_en, meaning_id, meaning_en, example, latin):
    return {
        "root": root, "rootLatin": root_latin, "wazan": wazan, "wazanLatin": wazan_latin,
        "formId": form_id, "formEn": form_en, "meaningId": meaning_id, "meaningEn": meaning_en,
        "exampleAr": example, "exampleLatin": latin,
    }


LEVELS = [
    {
        "id": 1, "titleId": "Level 1 — Akar dan Tasrif Dasar",
        "titleEn": "Level 1 — Roots and Basic Conjugation", "titleAr": "المُسْتَوَى الأَوَّلُ",
        "lessons": [
            lesson(
                "1-1", "Akar Kata dan Wazan", "Roots and Wazan", "الْجِذْرُ وَالْوَزْنُ",
                "Akar kata terdiri dari huruf asli. Wazan membantu kita mengenali pola perubahan sebuah kata.",
                "A root consists of original letters. A wazan helps us recognize a word's transformation pattern.",
                [
                    rule("Akar kata tiga huruf", "A three-letter root", "Banyak kata Arab dibangun dari tiga huruf asli, seperti ع ل م.", "Many Arabic words are built from three root letters, such as ع ل م.", "عَلِمَ الطَّالِبُ", "ʿAlima ṭ-ṭālibu", "Siswa itu mengetahui", "The student knew"),
                    rule("Wazan فَعَلَ", "The فَعَلَ pattern", "فَعَلَ adalah pola dasar untuk banyak fi'il tsulatsi mujarrad.", "فَعَلَ is a basic pattern for many simple triliteral verbs.", "كَتَبَ مُحَمَّدٌ", "Kataba Muḥammadun", "Muhammad menulis", "Muhammad wrote"),
                ],
                [pattern("ع ل م", "ʿ-l-m", "فَعَلَ", "faʿala", "Fi'il tsulatsi mujarrad", "Basic triliteral verb", "mengetahui", "to know", "عَلِمَ", "ʿalima")],
                [
                    choice("Apa akar kata عَلِمَ?", "What is the root of عَلِمَ?", "عَلِمَ", "ʿAlima", ["ع ل م", "ك ت ب", "ن ص ر", "ف ت ح"], ["ع ل م", "ك ت ب", "ن ص ر", "ف ت ح"], 0),
                    choice("Wazan kata كَتَبَ adalah...", "The wazan of كَتَبَ is...", "كَتَبَ", "Kataba", ["فَعَلَ", "فَعَّلَ", "أَفْعَلَ", "فَاعَلَ"], ["فَعَلَ", "فَعَّلَ", "أَفْعَلَ", "فَاعَلَ"], 0),
                    choice("Huruf asli dalam نَصَرَ adalah...", "The root letters in نَصَرَ are...", "نَصَرَ", "Naṣara", ["ن ص ر", "ن س ر", "ص ر ن", "ن ض ر"], ["ن ص ر", "ن س ر", "ص ر ن", "ن ض ر"], 0),
                    choice("Arti عَلِمَ adalah...", "The meaning of عَلِمَ is...", "عَلِمَ", "ʿAlima", ["mengetahui", "menulis", "membuka", "menolong"], ["to know", "to write", "to open", "to help"], 0),
                ],
            ),
            lesson(
                "1-2", "Fi'il Madhi", "The Past Tense", "الْفِعْلُ الْمَاضِي",
                "Fi'il madhi menunjukkan pekerjaan yang telah terjadi. Bentuk dasar biasanya memakai dhammah pada akhir fa'il.",
                "The past tense shows an action that happened. Its basic form commonly has dammah on the ending for a masculine singular subject.",
                [
                    rule("هُوَ dan هِيَ", "هُوَ and هِيَ", "Bentuk هُوَ dan هِيَ memakai bentuk tunggal yang berbeda pada beberapa fi'il.", "هُوَ and هِيَ use different singular forms for some verbs.", "هُوَ كَتَبَ وَهِيَ كَتَبَتْ", "Huwa kataba wa-hiya katabat", "Dia laki-laki menulis dan dia perempuan menulis", "He wrote and she wrote"),
                    rule("Akhiran jamak", "Plural endings", "وَاوُ الْجَمَاعَةِ pada كَتَبُوا menunjukkan pelaku laki-laki jamak.", "The plural suffix in كَتَبُوا shows a masculine plural subject.", "هُمْ كَتَبُوا الدَّرْسَ", "Hum katabū d-darsa", "Mereka menulis pelajaran", "They wrote the lesson"),
                ],
                [pattern("ك ت ب", "k-t-b", "فَعَلَ", "faʿala", "Fi'il lampau", "Past-tense verb", "menulis", "to write", "كَتَبَ", "kataba")],
                [
                    {"pronounAr": "هُوَ", "pronounLatin": "huwa", "past": "كَتَبَ", "present": "يَكْتُبُ", "imperative": "اُكْتُبْ"},
                    {"pronounAr": "هِيَ", "pronounLatin": "hiya", "past": "كَتَبَتْ", "present": "تَكْتُبُ", "imperative": "اُكْتُبِي"},
                    {"pronounAr": "هُمْ", "pronounLatin": "hum", "past": "كَتَبُوا", "present": "يَكْتُبُونَ", "imperative": "اُكْتُبُوا"},
                ],
                [
                    choice("Bentuk madhi untuk هِيَ adalah...", "The past form for هِيَ is...", "هِيَ ____", "Hiya ____", ["كَتَبَ", "كَتَبَتْ", "كَتَبُوا", "كَتَبْتُ"], ["كَتَبَ", "كَتَبَتْ", "كَتَبُوا", "كَتَبْتُ"], 1),
                    choice("Bentuk كَتَبُوا menunjukkan...", "كَتَبُوا shows...", "هُمْ ____", "Hum ____", ["satu laki-laki", "satu perempuan", "banyak laki-laki", "saya"], ["one male", "one female", "many males", "I"], 2),
                    choice("Lengkapi: هُوَ ____ الدَّرْسَ", "Complete: He ____ the lesson", "هُوَ ____ الدَّرْسَ", "Huwa ____ d-darsa", ["كَتَبَ", "كَتَبَتْ", "كَتَبُوا", "كَتَبْنَ"], ["كَتَبَ", "كَتَبَتْ", "كَتَبُوا", "كَتَبْنَ"], 0),
                    choice("Arti كَتَبَتْ adalah...", "The meaning of كَتَبَتْ is...", "كَتَبَتْ", "Katabat", ["dia perempuan menulis", "dia laki-laki menulis", "mereka menulis", "aku menulis"], ["she wrote", "he wrote", "they wrote", "I wrote"], 0),
                ],
            ),
            lesson(
                "1-3", "Fi'il Mudhari'", "The Present Tense", "الْفِعْلُ الْمُضَارِعُ",
                "Fi'il mudhari' sering dikenali dengan salah satu huruf أ ن ي ت di awalnya.",
                "The present tense is often recognized by one of the prefix letters أ ن ي ت.",
                [
                    rule("Huruf mudhari'", "Present-tense prefixes", "أَكْتُبُ berarti saya menulis, نَكْتُبُ berarti kami menulis, dan يَكْتُبُ berarti dia laki-laki menulis.", "أَكْتُبُ means I write, نَكْتُبُ means we write, and يَكْتُبُ means he writes.", "نَكْتُبُ الدَّرْسَ", "Naktubu d-darsa", "Kami menulis pelajaran", "We write the lesson"),
                    rule("تَفْعَلُ untuk beberapa pelaku", "تَفْعَلُ for several subjects", "تَكْتُبُ dapat berarti kamu laki-laki menulis atau dia perempuan menulis.", "تَكْتُبُ can mean you (male) write or she writes.", "هِيَ تَكْتُبُ الرِّسَالَةَ", "Hiya taktubu r-risālata", "Dia perempuan menulis surat", "She writes the letter"),
                ],
                [pattern("ك ت ب", "k-t-b", "يَفْعُلُ", "yafʿulu", "Fi'il sedang/akan", "Present or future verb", "menulis", "to write", "يَكْتُبُ", "yaktubu")],
                [],
                [
                    choice("Bentuk untuk 'saya menulis' adalah...", "The form for 'I write' is...", "أَنَا ____", "Anā ____", ["أَكْتُبُ", "نَكْتُبُ", "يَكْتُبُ", "تَكْتُبُ"], ["أَكْتُبُ", "نَكْتُبُ", "يَكْتُبُ", "تَكْتُبُ"], 0),
                    choice("Huruf mudhari' pada نَكْتُبُ adalah...", "The present prefix in نَكْتُبُ is...", "نَكْتُبُ", "Naktubu", ["أ", "ن", "ي", "ت"], ["أ", "ن", "ي", "ت"], 1),
                    choice("Lengkapi: هُوَ ____ الْكِتَابَ", "Complete: He ____ the book", "هُوَ ____ الْكِتَابَ", "Huwa ____ l-kitāba", ["يَقْرَأُ", "تَقْرَأُ", "أَقْرَأُ", "نَقْرَأُ"], ["يَقْرَأُ", "تَقْرَأُ", "أَقْرَأُ", "نَقْرَأُ"], 0),
                    choice("نَكْتُبُ berarti...", "نَكْتُبُ means...", "نَكْتُبُ", "Naktubu", ["kami menulis", "saya menulis", "dia menulis", "kamu menulis"], ["we write", "I write", "he writes", "you write"], 0),
                ],
            ),
        ],
    },
    {
        "id": 2, "titleId": "Level 2 — Bentuk Turunan", "titleEn": "Level 2 — Derived Forms", "titleAr": "المُسْتَوَى الثَّانِي",
        "lessons": [
            lesson(
                "2-1", "Fi'il Amr", "The Imperative", "فِعْلُ الْأَمْرِ",
                "Fi'il amr digunakan untuk perintah. Bentuknya mengikuti fi'il mudhari' dan ditujukan kepada lawan bicara.",
                "The imperative is used for commands. Its form relates to the present tense and addresses the listener.",
                [
                    rule("Perintah kepada laki-laki", "A command to one male", "اُكْتُبْ berarti tulislah untuk satu laki-laki.", "اُكْتُبْ means write to one male.", "اُكْتُبْ دَرْسَكَ", "Uktub darsaka", "Tulislah pelajaranmu", "Write your lesson"),
                    rule("Perintah kepada perempuan dan jamak", "Commands to females and plurals", "Akhiran اُكْتُبِي dan اُكْتُبُوا menyesuaikan lawan bicara.", "The endings in اُكْتُبِي and اُكْتُبُوا match the listeners.", "اُكْتُبُوا الْوَاجِبَ", "Uktubū l-wājiba", "Tulislah tugas itu", "Write the assignment"),
                ],
                [pattern("ك ت ب", "k-t-b", "اُفْعُلْ", "ufʿul", "Perintah", "Imperative", "tulislah", "write", "اُكْتُبْ", "uktub")], [],
                [
                    choice("Perintah kepada satu laki-laki adalah...", "The command to one male is...", "أَنْتَ ____", "Anta ____", ["اُكْتُبْ", "اُكْتُبِي", "اُكْتُبُوا", "كَتَبَ"], ["اُكْتُبْ", "اُكْتُبِي", "اُكْتُبُوا", "كَتَبَ"], 0),
                    choice("Bentuk perintah untuk banyak orang adalah...", "The command to many people is...", "أَنْتُمْ ____", "Antum ____", ["اُجْلِسْ", "اُجْلِسِي", "اُجْلِسُوا", "جَلَسَ"], ["اُجْلِسْ", "اُجْلِسِي", "اُجْلِسُوا", "جَلَسَ"], 2),
                    choice("Arti اُقْرَأْ adalah...", "The meaning of اُقْرَأْ is...", "اُقْرَأْ", "Iqra'", ["bacalah", "membaca", "dia membaca", "mereka membaca"], ["read", "reading", "he reads", "they read"], 0),
                    choice("Pilih fi'il amr: درس", "Choose the imperative: درس", "اخْتَرْ فِعْلَ الْأَمْرِ", "Choose the imperative", ["دَرَسَ", "يَدْرُسُ", "اُدْرُسْ", "دِرَاسَةٌ"], ["دَرَسَ", "يَدْرُسُ", "اُدْرُسْ", "دِرَاسَةٌ"], 2),
                ],
            ),
            lesson(
                "2-2", "Wazan Form II", "Form II Wazan", "وَزْنُ فَعَّلَ",
                "Form II memakai pola فَعَّلَ. Tasydid pada huruf tengah sering memberi makna membuat atau mengintensifkan.",
                "Form II uses فَعَّلَ. A doubled middle root letter often gives a causative or intensive meaning.",
                [
                    rule("Tasydid huruf tengah", "Doubling the middle root letter", "Pada عَلَّمَ, huruf ل kedua diberi tasydid sehingga berbeda dari عَلِمَ.", "In عَلَّمَ, the second ل is doubled, making it different from عَلِمَ.", "عَلَّمَ الْمُعَلِّمُ الطَّالِبَ", "ʿAllama l-muʿallimu ṭ-ṭāliba", "Guru mengajar siswa", "The teacher taught the student"),
                    rule("Makna membuat atau mengajarkan", "Causative or intensive meaning", "Form II dapat menjadikan orang lain melakukan sesuatu, seperti عَلَّمَ.", "Form II can cause someone else to do something, as in عَلَّمَ.", "كَسَّرَ الْوَلَدُ الْقَلَمَ", "Kassara l-waladu l-qalama", "Anak itu memecahkan pena", "The child broke the pen into pieces"),
                ],
                [pattern("ع ل م", "ʿ-l-m", "فَعَّلَ", "faʿʿala", "Form II", "Form II", "mengajarkan", "to teach", "عَلَّمَ", "ʿallama")], [],
                [
                    choice("Wazan عَلَّمَ adalah...", "The wazan of عَلَّمَ is...", "عَلَّمَ", "ʿAllama", ["فَعَلَ", "فَعَّلَ", "فَاعَلَ", "أَفْعَلَ"], ["فَعَلَ", "فَعَّلَ", "فَاعَلَ", "أَفْعَلَ"], 1),
                    choice("Ciri utama Form II adalah...", "The main feature of Form II is...", "فَعَّلَ", "Faʿʿala", ["tasydid huruf tengah", "alif di awal", "hamzah di akhir", "tanwin"], ["doubling the middle letter", "an initial alif", "a final hamzah", "tanwin"], 0),
                    choice("Arti عَلَّمَ adalah...", "The meaning of عَلَّمَ is...", "عَلَّمَ", "ʿAllama", ["mengajarkan", "mengetahui", "duduk", "keluar"], ["to teach", "to know", "to sit", "to leave"], 0),
                    choice("Pilih bentuk Form II dari ك س ر", "Choose Form II of ك س ر", "ك س ر", "k-s-r", ["كَسَرَ", "كَسَّرَ", "أَكْسَرَ", "كَاسَرَ"], ["كَسَرَ", "كَسَّرَ", "أَكْسَرَ", "كَاسَرَ"], 1),
                ],
            ),
            lesson(
                "2-3", "Wazan Form III dan IV", "Form III and IV", "وَزْنُ فَاعَلَ وَأَفْعَلَ",
                "Form III memakai فَاعَلَ, sedangkan Form IV memakai أَفْعَلَ dengan hamzah tambahan di awal.",
                "Form III uses فَاعَلَ, while Form IV uses أَفْعَلَ with an added initial hamzah.",
                [
                    rule("Form III menunjukkan keterlibatan", "Form III shows involvement", "فَاعَلَ sering menunjukkan pekerjaan antara dua pihak, seperti سَاعَدَ.", "فَاعَلَ often shows an action involving two parties, such as سَاعَدَ.", "سَاعَدَ عَلِيٌّ صَدِيقَهُ", "Sāʿada ʿAliyyun ṣadīqahu", "Ali membantu temannya", "Ali helped his friend"),
                    rule("Form IV dengan أَفْعَلَ", "Form IV with أَفْعَلَ", "أَكْرَمَ memiliki hamzah tambahan dan berarti memuliakan.", "أَكْرَمَ has an added hamzah and means to honor.", "أَكْرَمَ الرَّجُلُ ضَيْفَهُ", "Akrama r-rajulu ḍayfahu", "Laki-laki itu memuliakan tamunya", "The man honored his guest"),
                ],
                [pattern("ص ع د", "ṣ-ʿ-d", "فَاعَلَ", "fāʿala", "Form III", "Form III", "saling menaikkan", "to deal with", "سَاعَدَ", "sāʿada"), pattern("ك ر م", "k-r-m", "أَفْعَلَ", "afʿala", "Form IV", "Form IV", "memuliakan", "to honor", "أَكْرَمَ", "akrama")], [],
                [
                    choice("Wazan سَاعَدَ adalah...", "The wazan of سَاعَدَ is...", "سَاعَدَ", "Sāʿada", ["فَعَلَ", "فَعَّلَ", "فَاعَلَ", "أَفْعَلَ"], ["فَعَلَ", "فَعَّلَ", "فَاعَلَ", "أَفْعَلَ"], 2),
                    choice("Wazan أَكْرَمَ adalah...", "The wazan of أَكْرَمَ is...", "أَكْرَمَ", "Akrama", ["فَعَلَ", "فَعَّلَ", "فَاعَلَ", "أَفْعَلَ"], ["فَعَلَ", "فَعَّلَ", "فَاعَلَ", "أَفْعَلَ"], 3),
                    choice("Arti سَاعَدَ adalah...", "The meaning of سَاعَدَ is...", "سَاعَدَ", "Sāʿada", ["membantu", "memuliakan", "mengetahui", "mengajar"], ["to help", "to honor", "to know", "to teach"], 0),
                    choice("Ciri Form IV adalah...", "A feature of Form IV is...", "أَفْعَلَ", "Afʿala", ["hamzah tambahan di awal", "tasydid huruf tengah", "alif setelah huruf pertama", "tanwin"], ["an added initial hamzah", "a doubled middle letter", "alif after the first letter", "tanwin"], 0),
                ],
            ),
        ],
    },
    {
        "id": 3, "titleId": "Level 3 — Isim Turunan dan Review", "titleEn": "Level 3 — Derived Nouns and Review", "titleAr": "المُسْتَوَى الثَّالِثُ",
        "lessons": [
            lesson(
                "3-1", "Isim Fa'il dan Isim Maf'ul", "Active and Passive Participles", "اسْمُ الْفَاعِلِ وَاسْمُ الْمَفْعُولِ",
                "Isim fa'il menunjukkan pelaku, sedangkan isim maf'ul menunjukkan sesuatu yang dikenai pekerjaan.",
                "The active participle shows the doer, while the passive participle shows what receives the action.",
                [
                    rule("Isim fa'il dari fi'il tiga huruf", "Active participle from a triliteral verb", "Dari كَتَبَ terbentuk كَاتِبٌ dengan pola فَاعِلٌ.", "كَاتِبٌ is formed from كَتَبَ on the فَاعِلٌ pattern.", "هٰذَا كَاتِبٌ", "Hādhā kātibun", "Ini seorang penulis", "This is a writer"),
                    rule("Isim maf'ul", "Passive participle", "Dari كَتَبَ terbentuk مَكْتُوبٌ yang berarti sesuatu yang ditulis.", "مَكْتُوبٌ comes from كَتَبَ and means something written.", "هٰذَا دَرْسٌ مَكْتُوبٌ", "Hādhā darsun maktūbun", "Ini pelajaran tertulis", "This is a written lesson"),
                ],
                [pattern("ك ت ب", "k-t-b", "فَاعِلٌ", "fāʿilun", "Isim fa'il", "Active participle", "penulis", "writer", "كَاتِبٌ", "kātibun"), pattern("ك ت ب", "k-t-b", "مَفْعُولٌ", "mafʿūlun", "Isim maf'ul", "Passive participle", "yang ditulis", "written", "مَكْتُوبٌ", "maktūbun")], [],
                [
                    choice("Isim fa'il dari كَتَبَ adalah...", "The active participle of كَتَبَ is...", "كَتَبَ → ____", "Kataba → ____", ["كَاتِبٌ", "مَكْتُوبٌ", "كِتَابَةٌ", "مَكْتَبٌ"], ["كَاتِبٌ", "مَكْتُوبٌ", "كِتَابَةٌ", "مَكْتَبٌ"], 0),
                    choice("Isim maf'ul dari كَتَبَ adalah...", "The passive participle of كَتَبَ is...", "كَتَبَ → ____", "Kataba → ____", ["كَاتِبٌ", "مَكْتُوبٌ", "كِتَابَةٌ", "كَاتَبَ"], ["كَاتِبٌ", "مَكْتُوبٌ", "كِتَابَةٌ", "كَاتَبَ"], 1),
                    choice("كَاتِبٌ berarti...", "كَاتِبٌ means...", "كَاتِبٌ", "Kātibun", ["penulis", "yang ditulis", "tulisan", "tempat menulis"], ["writer", "written", "writing", "writing place"], 0),
                    choice("Pola isim maf'ul adalah...", "The passive participle pattern is...", "اسْمُ الْمَفْعُولِ", "Ismu l-mafʿūl", ["فَاعِلٌ", "مَفْعُولٌ", "فَعَّالٌ", "أَفْعَلُ"], ["فَاعِلٌ", "مَفْعُولٌ", "فَعَّالٌ", "أَفْعَلُ"], 1),
                ],
            ),
            lesson(
                "3-2", "Masdar", "The Verbal Noun", "الْمَصْدَرُ",
                "Masdar adalah kata benda yang menunjukkan pekerjaan tanpa terikat waktu, seperti كِتَابَةٌ.",
                "A masdar is a verbal noun that shows an action without being tied to time, such as كِتَابَةٌ.",
                [
                    rule("Masdar dari كَتَبَ", "Masdar from كَتَبَ", "كِتَابَةٌ menunjukkan kegiatan menulis tanpa menyebut waktunya.", "كِتَابَةٌ shows the act of writing without specifying its time.", "كِتَابَةُ الدَّرْسِ مُفِيدَةٌ", "Kitābatu d-darsi mufīdatun", "Menulis pelajaran itu bermanfaat", "Writing the lesson is useful"),
                    rule("Masdar Form II", "Masdar of Form II", "Masdar عَلَّمَ adalah تَعْلِيمٌ, yaitu kegiatan mengajar.", "The masdar of عَلَّمَ is تَعْلِيمٌ, the act of teaching.", "التَّعْلِيمُ مُهِمٌّ", "At-taʿlīmu muhimm", "Pendidikan itu penting", "Education is important"),
                ],
                [pattern("ك ت ب", "k-t-b", "فِعَالَةٌ", "fiʿālatun", "Masdar", "Verbal noun", "penulisan", "writing", "كِتَابَةٌ", "kitābatun"), pattern("ع ل م", "ʿ-l-m", "تَفْعِيلٌ", "tafʿīlun", "Masdar Form II", "Form II verbal noun", "pengajaran", "teaching", "تَعْلِيمٌ", "taʿlīmun")], [],
                [
                    choice("Masdar dari كَتَبَ adalah...", "The masdar of كَتَبَ is...", "كَتَبَ → ____", "Kataba → ____", ["كَاتِبٌ", "مَكْتُوبٌ", "كِتَابَةٌ", "مَكْتَبٌ"], ["كَاتِبٌ", "مَكْتُوبٌ", "كِتَابَةٌ", "مَكْتَبٌ"], 2),
                    choice("تَعْلِيمٌ adalah masdar dari...", "تَعْلِيمٌ is the masdar of...", "تَعْلِيمٌ", "Taʿlīmun", ["عَلِمَ", "عَلَّمَ", "تَعَلَّمَ", "أَعْلَمَ"], ["عَلِمَ", "عَلَّمَ", "تَعَلَّمَ", "أَعْلَمَ"], 1),
                    choice("Masdar menunjukkan...", "A masdar shows...", "الْمَصْدَرُ", "Al-maṣdaru", ["pelaku", "waktu lampau", "pekerjaan tanpa waktu", "perintah"], ["the doer", "past time", "an action without time", "a command"], 2),
                    choice("Pola تَعْلِيمٌ adalah...", "The pattern of تَعْلِيمٌ is...", "تَعْلِيمٌ", "Taʿlīmun", ["فِعَالَةٌ", "تَفْعِيلٌ", "مَفْعُولٌ", "فَاعِلٌ"], ["فِعَالَةٌ", "تَفْعِيلٌ", "مَفْعُولٌ", "فَاعِلٌ"], 1),
                ],
            ),
        ],
    },
]
