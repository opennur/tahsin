package com.tahsin.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tahsin.app.data.quran.QuranRepository
import com.tahsin.app.util.AudioDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Satu baris pada layar manajemen audio. */
data class AudioManagerItem(
    val number: Int,
    val nameLatin: String,
    val nameArabic: String,
    val ayahFiles: Int,
    val ayahCount: Int,
    val wordFiles: Int,
    val totalWords: Int?,
    val sizeBytes: Long,
) {
    /** Semua audio surah ini sudah lengkap terunduh? */
    val isComplete: Boolean
        get() = ayahFiles >= ayahCount && (totalWords == null || wordFiles >= totalWords)
}

/** State layar manajemen audio. */
data class AudioManagerState(
    val items: List<AudioManagerItem> = emptyList(),
    val totalDownloaded: Int = 0,
    val totalSizeBytes: Long = 0L,
    /** Surah yang menunggu konfirmasi hapus. */
    val pendingDelete: Int? = null,
    /** Konfirmasi hapus semua. */
    val pendingDeleteAll: Boolean = false,
)

/**
 * Manajemen audio terunduh: daftar surah yang punya audio, info kelengkapan,
 * dan aksi hapus (per surah / semua).
 */
class AudioManagerViewModel(
    private val repository: QuranRepository,
    private val downloader: AudioDownloader,
) : ViewModel() {

    private val _state = MutableStateFlow(AudioManagerState())
    val state: StateFlow<AudioManagerState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val surahs = repository.surahList()
        val downloaded = downloader.downloadedSurahNumbers().toSet()
        val items = surahs
            .filter { it.number in downloaded }
            .map { surah ->
                val cached = repository.cachedSurah(surah.number)
                val totalWords = cached?.ayahs?.sumOf { it.words.size }
                val info = downloader.surahAudioInfo(surah.number, surah.ayahCount, totalWords)
                AudioManagerItem(
                    number = surah.number,
                    nameLatin = surah.nameLatin,
                    nameArabic = surah.nameArabic,
                    ayahFiles = info.ayahFiles,
                    ayahCount = info.ayahCount,
                    wordFiles = info.wordFiles,
                    totalWords = info.totalWords,
                    sizeBytes = info.sizeBytes,
                )
            }
            .sortedBy { it.number }
        _state.value = AudioManagerState(
            items = items,
            totalDownloaded = items.sumOf { it.ayahFiles + it.wordFiles },
            totalSizeBytes = items.sumOf { it.sizeBytes },
        )
    }

    fun requestDelete(number: Int) {
        _state.update { it.copy(pendingDelete = number, pendingDeleteAll = false) }
    }

    fun requestDeleteAll() {
        _state.update { it.copy(pendingDelete = null, pendingDeleteAll = true) }
    }

    fun cancelDelete() {
        _state.update { it.copy(pendingDelete = null, pendingDeleteAll = false) }
    }

    fun confirmDelete() {
        val number = _state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        downloader.deleteSurahAudio(number)
        refresh()
    }

    fun confirmDeleteAll() {
        _state.update { it.copy(pendingDeleteAll = false) }
        downloader.deleteAllAudio()
        refresh()
    }
}

/** Factory manual DI (tanpa Hilt). */
fun audioManagerViewModelFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = context.applicationContext
        AudioManagerViewModel(
            repository = QuranRepository(app),
            downloader = AudioDownloader(app),
        )
    }
}
