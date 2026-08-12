package com.tahsin.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Status unduhan audio global — diamati layar utama & manajer audio. */
data class DownloadProgressState(
    val isDownloading: Boolean = false,
    val currentSurahNumber: Int? = null,
    val currentSurahName: String? = null,
    /** File selesai dalam surah yang sedang diunduh. */
    val surahDone: Int = 0,
    /** Total file (ayat + kata) surah yang sedang diunduh. */
    val surahTotal: Int = 0,
)

/** Store status unduhan (application-scope, tanpa Android API). */
object DownloadProgress {

    private val _state = MutableStateFlow(DownloadProgressState())
    val state: StateFlow<DownloadProgressState> = _state.asStateFlow()

    fun update(transform: (DownloadProgressState) -> DownloadProgressState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = DownloadProgressState()
    }
}
