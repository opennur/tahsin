package com.tahsin.app.ui.navigation

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
    data object Quiz : AppScreen
    data object Stats : AppScreen
    data object Search : AppScreen
    data object AudioManager : AppScreen
    data object DreamBig : AppScreen
    data object Lughoh : AppScreen
    data object Settings : AppScreen

    /** Tag unik per layar (dipakai rememberSaveable untuk rotasi layar). */
    val tag: String get() = javaClass.simpleName

    companion object {
        /** Kembalikan layar dari [tag]; default [Home] untuk tag tak dikenal. */
        fun fromTag(tag: String): AppScreen = when (tag) {
            "Home" -> Home
            "Tahsin" -> Tahsin
            "Vocab" -> Vocab
            "Quiz" -> Quiz
            "Stats" -> Stats
            "Search" -> Search
            "AudioManager" -> AudioManager
            "DreamBig" -> DreamBig
            "Lughoh" -> Lughoh
            "Settings" -> Settings
            else -> Home
        }
    }
}
