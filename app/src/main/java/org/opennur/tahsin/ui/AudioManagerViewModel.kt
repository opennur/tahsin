package org.opennur.tahsin.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.opennur.tahsin.data.quran.QuranRepository
import org.opennur.tahsin.util.AppLanguage
import org.opennur.tahsin.util.AudioDownloader
import org.opennur.tahsin.util.DownloadProgress
import org.opennur.tahsin.util.SettingsStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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
    /** Audio yang memang tidak tersedia di server (dianggap lengkap). */
    val missingWords: Int = 0,
    val missingAyahs: Int = 0,
) {
    /** Semua audio surah ini sudah lengkap terunduh / memang tidak tersedia? */
    val isComplete: Boolean
        get() = (ayahFiles + missingAyahs) >= ayahCount &&
            (totalWords == null || (wordFiles + missingWords) >= totalWords)
}

/** State layar manajemen audio. */
data class AudioManagerState(
    val items: List<AudioManagerItem> = emptyList(),
    val totalDownloaded: Int = 0,
    val totalSizeBytes: Long = 0L,
    /** Bahasa aktif untuk teks UI. */
    val language: AppLanguage = AppLanguage.ID,
    /** Qari' yang sedang ditampilkan (folder audio per qari'). */
    val reciterLabel: String = "",
    /** Surah yang menunggu konfirmasi hapus. */
    val pendingDelete: Int? = null,
    /** Konfirmasi hapus semua. */
    val pendingDeleteAll: Boolean = false,
    /** Sedang memuat daftar audio (listFiles di thread IO). */
    val isLoading: Boolean = true,
)

/**
 * Manajemen audio terunduh: daftar surah yang punya audio, info kelengkapan,
 * dan aksi hapus (per surah / semua).
 *
 * Hasil pemindaian folder audio di-cache ke `filesDir/audio-manager-cache.json`.
 * Cache dipakai hanya kalau tidak ada perubahan (tidak sedang mengunduh dan
 * tidak baru dihapus) — buka layar lagi jadi instan tanpa listFiles ulang.
 */
@HiltViewModel
class AudioManagerViewModel @Inject constructor(
    private val app: Context,
    private val repository: QuranRepository,
    private val downloader: AudioDownloader,
    private val settings: SettingsStore,
) : ViewModel() {

    private val gson = Gson()
    private val itemsType = object : TypeToken<List<AudioManagerItem>>() {}.type

    /** Cache per qari' (folder audio per qari' berbeda → daftar berbeda). */
    private val cacheFile: File
        get() = File(app.applicationContext.filesDir, "audio-manager-cache-${settings.reciter.slug}.json")

    /** true = cache tidak boleh dipakai (ada perubahan file audio). */
    @Volatile
    private var cacheDirty = true

    private val _state = MutableStateFlow(AudioManagerState())
    val state: StateFlow<AudioManagerState> = _state.asStateFlow()

    init {
        refresh()
        // Selama masih ada unduhan aktif, daftar audio otomatis di-refresh
        // (live, bukan cache) agar item yang baru selesai langsung muncul.
        // Saat unduhan selesai → cache ditandai basi lalu dihitung ulang.
        viewModelScope.launch {
            var lastRefresh = 0L
            var wasDownloading = false
            DownloadProgress.state.collect { st ->
                if (st.isDownloading) {
                    wasDownloading = true
                    val now = System.currentTimeMillis()
                    if (now - lastRefresh >= 1500) {
                        lastRefresh = now
                        refresh()
                    }
                } else if (wasDownloading) {
                    // Transisi: unduhan selesai → daftar & cache perlu dihitung ulang.
                    wasDownloading = false
                    markDirty()
                    refresh()
                }
            }
        }
    }

    /** Paksa pemindaian ulang pada refresh berikutnya (cache dianggap basi). */
    fun markDirty() {
        cacheDirty = true
    }

    fun refresh() {
        // ListFiles/stat di folder audio bisa lambat (ribuan file) — jangan di thread utama.
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            // Jalur cepat: cache valid (tidak sedang mengunduh & tidak basi).
            if (!DownloadProgress.state.value.isDownloading && !cacheDirty) {
                val cached = readCache()
                if (cached != null) {
                    _state.value = buildState(cached)
                    return@launch
                }
            }
            // Jalur lambat: hitung dari disk, lalu simpan cache.
            val items = computeItems()
            writeCache(items)
            cacheDirty = false
            _state.value = buildState(items)
        }
    }

    private fun buildState(items: List<AudioManagerItem>): AudioManagerState = AudioManagerState(
        items = items,
        totalDownloaded = items.sumOf { it.ayahFiles + it.wordFiles },
        totalSizeBytes = items.sumOf { it.sizeBytes },
        language = AppLanguage.entries.firstOrNull { it.code == settings.languageCode } ?: AppLanguage.ID,
        reciterLabel = settings.reciter.label,
        isLoading = false,
    )

    /** Baca cache JSON (null kalau belum ada / rusak). */
    private fun readCache(): List<AudioManagerItem>? = runCatching {
        if (!cacheFile.exists()) return null
        gson.fromJson<List<AudioManagerItem>>(cacheFile.readText(), itemsType) ?: emptyList()
    }.getOrNull()

    private fun writeCache(items: List<AudioManagerItem>) {
        runCatching { cacheFile.writeText(gson.toJson(items)) }
    }

    /** Pemindaian folder audio → daftar surah terunduh (jalur lambat). */
    private fun computeItems(): List<AudioManagerItem> {
        val surahs = repository.surahList()
        val downloaded = downloader.downloadedSurahNumbers().toSet()
        return surahs
            .filter { it.number in downloaded }
            .map { surah ->
                val cached = repository.cachedSurahPlain(surah.number)
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
                    missingWords = info.missingWords,
                    missingAyahs = info.missingAyahs,
                )
            }
            .sortedBy { it.number }
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
        markDirty()
        refresh()
    }

    fun confirmDeleteAll() {
        _state.update { it.copy(pendingDeleteAll = false) }
        downloader.deleteAllAudio()
        markDirty()
        refresh()
    }
}
