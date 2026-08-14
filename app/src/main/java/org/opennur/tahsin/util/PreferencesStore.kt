package org.opennur.tahsin.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Façade SINKRON di atas DataStore yang async.
 *
 * DataStore hanya menyediakan API async (Flow/suspend), sedangkan kode
 * aplikasi ini memakai pola SharedPreferences sinkron (dibaca langsung saat
 * startup / saat state berubah). Kelas ini menjembatani keduanya tanpa
 * mengubah satu pun pemanggil:
 *
 * - **Baca**: dari cache di memori yang di-*prime* SINKRON saat konstruksi
 *   (DataStore membaca file kecil — orde beberapa ms). Cache inilah
 *   satu-satunya sumber kebenaran dalam proses: [DataStores] menjamin hanya
 *   ada SATU [PreferencesStore] per file, jadi tidak ada penulis lain yang
 *   perlu diobservasi.
 * - **Tulis**: cache di-update SINKRON (pembaca langsung melihat nilai baru,
 *   urutan read-after-write terjaga) lalu dipersist ASYNC ke DataStore —
 *   semantik seperti `SharedPreferences.apply()` (nilai langsung terlihat
 *   dalam proses; kegagalan tulis ditelan seperti apply). Registry bersama
 *   membuat instance lain membaca dari cache yang sama → visibilitas
 *   lintas instance tetap langsung.
 *
 * Migrasi dari SharedPreferences lama dilakukan lewat
 * [androidx.datastore.preferences.core.SharedPreferencesMigration] di
 * [DataStores] (lihat SettingsStore / AyahOfTheDayManager).
 */
class PreferencesStore(private val dataStore: DataStore<Preferences>) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _cache = MutableStateFlow(emptyPreferences())

    /** Cache terbaru — nilainya selalu valid (bukan default kosong). */
    val cache: StateFlow<Preferences> = _cache.asStateFlow()

    init {
        // Prime sinkron: nilai tersimpan langsung tersedia — menggantikan
        // baca sinkron SharedPreferences. Kegagalan baca → default kosong.
        _cache.value = runBlocking {
            runCatching { dataStore.data.first() }.getOrDefault(emptyPreferences())
        }
    }

    /** Nilai saat ini (selalu ter-update; tanpa nilai = key tidak ada). */
    val current: Preferences
        get() = _cache.value

    /**
     * Mutasi [block]: cache di-update SINKRON (pembaca langsung melihat hasil),
     * persist ke DataStore ASYNC di scope sendiri (seperti `apply()`). Aman
     * dipanggil dari thread mana pun; tulis berurutan dijamin DataStore.
     */
    fun edit(block: MutablePreferences.() -> Unit) {
        _cache.update { current -> current.toMutablePreferences().apply(block) }
        scope.launch {
            runCatching { dataStore.edit { prefs -> prefs.apply(block) } }
        }
    }
}
