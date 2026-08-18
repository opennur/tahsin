package org.opennur.tahsin.ui.navigation

/**
 * Identitas layar untuk navigasi back stack di [MainActivity].
 *
 * Layar utama adalah [Home] (portal menu); layar lain di-push di atasnya
 * dan di-pop dengan tombol back sistem. Setiap layar punya halaman sendiri —
 * tidak ada lagi drawer/navigasi tersembunyi.
 */
sealed interface AppScreen {
    data object Home : AppScreen
    data object Tahsin : AppScreen
    data object Vocab : AppScreen
    data object Memorization : AppScreen
    data object Quiz : AppScreen
    data object Stats : AppScreen
    data object Search : AppScreen
    data object AudioManager : AppScreen
    data object DreamBig : AppScreen
    data object Lughoh : AppScreen
    data object Nahwu : AppScreen
    data object Shorof : AppScreen
    data object AyatQuiz : AppScreen
    data object Badges : AppScreen
    data object Coherence : AppScreen
    data object Favorites : AppScreen
    data object Settings : AppScreen
    data object PetaKhatam : AppScreen
    /** Tag unik per layar (dipakai rememberSaveable untuk rotasi layar). */
    val tag: String get() = javaClass.simpleName

    companion object {
        /** Kembalikan layar dari [tag]; default [Home] untuk tag tak dikenal. */
        fun fromTag(tag: String): AppScreen = when (tag) {
            "Home" -> Home
            "Tahsin" -> Tahsin
            "Vocab" -> Vocab
            "Memorization" -> Memorization
            "Quiz" -> Quiz
            "Stats" -> Stats
            "Search" -> Search
            "AudioManager" -> AudioManager
            "DreamBig" -> DreamBig
            "Lughoh" -> Lughoh
            "Nahwu" -> Nahwu
            "Shorof" -> Shorof
            "AyatQuiz" -> AyatQuiz
            "Badges" -> Badges
            "Coherence" -> Coherence
            "Favorites" -> Favorites
            "Settings" -> Settings
            "PetaKhatam" -> PetaKhatam
            else -> Home
        }
    }
}
