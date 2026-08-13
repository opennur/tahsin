package org.opennur.tahsin.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.opennur.tahsin.data.vocab.VocabState
import java.io.File

/**
 * Penyimpanan persisten progres belajar kosa kata (SRS + progres harian).
 *
 * Format: satu file JSON di `filesDir/vocab-stats.json` ([VocabState]).
 * Tanpa Room — konsisten dengan arsitektur proyek (Gson + filesDir, pola
 * [ReadingStatsStore]). Data kecil, selalu dibaca dari disk (tanpa cache)
 * dan ditulis atomik (temp → rename) supaya tidak ada file setengah jadi.
 */
class VocabularyStatsStore internal constructor(private val file: File) {

    companion object {
        /** Factory Android: file di filesDir — ctor tetap murni (File) agar bisa dicakup 100%. */
        fun fromContext(context: Context): VocabularyStatsStore =
            VocabularyStatsStore(File(context.applicationContext.filesDir, "vocab-stats.json"))
    }

    private val gson = Gson()
    private val stateType = object : TypeToken<VocabState>() {}.type

    /** Keadaan saat ini (kartu + progres harian); file rusak/kosong → default. */
    fun read(): VocabState = synchronized(this) {
        runCatching {
            if (!file.exists()) VocabState()
            else gson.fromJson<VocabState>(file.readText(), stateType) ?: VocabState()
        }.getOrDefault(VocabState())
    }

    /** Simpan keadaan penuh (atomik). */
    fun write(state: VocabState) {
        synchronized(this) {
            runCatching {
                val tmp = File(file.parentFile, "${file.name}.tmp")
                tmp.writeText(gson.toJson(state))
                if (!tmp.renameTo(file)) {
                    // Rename gagal (jarang) — fallback tulis langsung.
                    writeDirect(gson.toJson(state))
                }
            }
        }
    }

    /**
     * Fallback tulis langsung — dipisah supaya jalur sukses bisa diuji unit
     * (file biasa). Kegagalan (mis. target direktori) ditelan runCatching:
     * ini pertahanan terakhir, bukan jalan utama.
     */
    internal fun writeDirect(json: String) {
        runCatching { file.writeText(json) }
    }

    /** Hapus seluruh progres kosa kata. */
    fun clear() {
        synchronized(this) {
            runCatching { file.delete() }
        }
    }
}
