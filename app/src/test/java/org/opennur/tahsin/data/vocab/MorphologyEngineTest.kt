package org.opennur.tahsin.data.vocab

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class MorphologyEngineTest {

    private val testEntries = listOf(
        VocabEntry(
            key = "قال",
            word = "قَالَ",
            translit = "qāla",
            meaningId = "berkata",
            meaningEn = "he said",
            freq = 1200,
            root = "قول",
            rootMeaningId = "kata; berkata",
            rootMeaningEn = "word; to say",
            example = VocabExample(
                2, 4, 5,
                "وَقَالَ رَبُّكُمُ",
                "wa qāla rabbukum",
                "Dan Tuhanmu berfirman,",
                "And your Lord said",
            ),
        ),
        VocabEntry(
            key = "يقولون",
            word = "يَقُوْلُوْنَ",
            translit = "yaqūlūna",
            meaningId = "mereka berkata",
            meaningEn = "they say",
            freq = 800,
            root = "قول",
            rootMeaningId = "kata; berkata",
            rootMeaningEn = "word; to say",
            example = VocabExample(2, 14, 3, "يَقُوْلُوْنَ", "yaqūlūna", "Mereka berkata", "They say"),
        ),
        VocabEntry(
            key = "علم",
            word = "عَلِمٌ",
            translit = "ʿalim",
            meaningId = "Maha Mengetahui",
            meaningEn = "All-Knowing",
            freq = 247,
            root = "علم",
            rootMeaningId = "ilmu; mengetahui",
            rootMeaningEn = "knowledge; to know",
            example = VocabExample(2, 29, 1, "عَلِيمٌ", "ʿalim", "Maha Mengetahui", "All-Knowing"),
        ),
        VocabEntry(
            key = "يعلمون",
            word = "يَعْلَمُوْنَ",
            translit = "yaʿlamūna",
            meaningId = "mereka mengetahui",
            meaningEn = "they know",
            freq = 150,
            root = "علم",
            rootMeaningId = "ilmu; mengetahui",
            rootMeaningEn = "knowledge; to know",
            example = VocabExample(3, 19, 2, "يَعْلَمُوْنَ", "yaʿlamūna", "Mereka mengetahui", "They know"),
        ),
        VocabEntry(
            key = "معلم",
            word = "مُعَلِّمٌ",
            translit = "muʿallim",
            meaningId = "guru; pengajar",
            meaningEn = "teacher",
            freq = 5,
            root = "علم",
            rootMeaningId = "ilmu; mengetahui",
            rootMeaningEn = "knowledge; to know",
            example = VocabExample(0, 0, 0, "", "", "", ""),
        ),
        VocabEntry(
            key = "من",
            word = "مِنْ",
            translit = "min",
            meaningId = "dari",
            meaningEn = "from",
            freq = 2763,
            root = "من",
            rootMeaningId = "dari",
            rootMeaningEn = "from",
            example = VocabExample(
                1, 1, 1,
                "بِسْمِ اللّٰهِ",
                "bismillāhi",
                "Dengan nama Allah",
                "In the name of Allah",
            ),
        ),
    )

    @Before
    fun setUp() {
        MorphologyEngine.init(testEntries)
    }

    @Test
    fun init_indexesAllEntries() {
        assertThat(MorphologyEngine.indexedEntryCount()).isEqualTo(6)
    }

    @Test
    fun init_indexesAllRoots() {
        // 3 unique roots: قول, علم, من
        assertThat(MorphologyEngine.indexedRootCount()).isEqualTo(3)
    }

    @Test
    fun lookupRoot_findsEntryWithRoot() {
        val result = MorphologyEngine.lookupRoot("قَالَ")
        assertThat(result).isNotNull()
        assertThat(result!!.root).isEqualTo("قول")
        assertThat(result.meaningId).isEqualTo("kata; berkata")
        assertThat(result.meaningEn).isEqualTo("word; to say")
    }

    @Test
    fun lookupRoot_returnsRelatedWords() {
        val result = MorphologyEngine.lookupRoot("قَالَ")
        assertThat(result).isNotNull()
        // Related words should include يققولون (same root, different word)
        val related = result!!.relatedWords
        assertThat(related).isNotEmpty()
        assertThat(related.any { it.key == "يقولون" }).isTrue()
    }

    @Test
    fun lookupRoot_excludesCurrentWord() {
        val result = MorphologyEngine.lookupRoot("قَالَ")
        assertThat(result).isNotNull()
        val related = result!!.relatedWords
        assertThat(related.none { it.key == "قال" }).isTrue()
    }

    @Test
    fun lookupRoot_returnsEmptyForUnknownWord() {
        val result = MorphologyEngine.lookupRoot("غير معروف")
        assertThat(result).isNull()
    }

    @Test
    fun lookupRoot_returnsEmptyForBlankWord() {
        val result = MorphologyEngine.lookupRoot("")
        assertThat(result).isNull()
    }

    @Test
    fun lookupRoot_worksWithDifferentRoot() {
        val result = MorphologyEngine.lookupRoot("عَلِمٌ")
        assertThat(result).isNotNull()
        assertThat(result!!.root).isEqualTo("علم")
        assertThat(result.meaningId).isEqualTo("ilmu; mengetahui")
        assertThat(result.relatedWords.size).isEqualTo(3) // يعلمون, معلم, and another
    }

    @Test
    fun findRelatedWords_returnsAllWordsForRoot() {
        val related = MorphologyEngine.findRelatedWords("قول")
        assertThat(related.size).isEqualTo(2) // قال, قولون
        assertThat(related.map { it.key }.toSet()).containsExactly("قال", "يقولون")
    }

    @Test
    fun findRelatedWords_excludesSpecifiedKey() {
        val related = MorphologyEngine.findRelatedWords("قول", excludeKey = "قال")
        assertThat(related.size).isEqualTo(1)
        assertThat(related[0].key).isEqualTo("يقولون")
    }

    @Test
    fun findRelatedWords_returnsEmptyForUnknownRoot() {
        val related = MorphologyEngine.findRelatedWords("xyz")
        assertThat(related).isEmpty()
    }

    @Test
    fun findRelatedWords_sortsByFrequencyDesc() {
        val related = MorphologyEngine.findRelatedWords("علم")
        assertThat(related.size).isGreaterThan(1)
        // Check that they are sorted by frequency descending
        for (i in 0 until related.size - 1) {
            assertThat(related[i].frequency).isAtLeast(related[i + 1].frequency)
        }
    }

    @Test
    fun relatedWord_containsAllFields() {
        val related = MorphologyEngine.findRelatedWords("قول")
        val word = related.first()
        assertThat(word.word).isNotEmpty()
        assertThat(word.key).isNotEmpty()
        assertThat(word.meaningId).isNotEmpty()
        assertThat(word.meaningEn).isNotEmpty()
        assertThat(word.frequency).isGreaterThan(0)
    }
}
