package org.opennur.tahsin.util

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * Tes [PreferencesStore] — façade sinkron di atas DataStore async yang
 * menjadi fondasi migrasi SharedPreferences. Robolectric menyediakan
 * filesDir asli sehingga perilaku baca/tulis nyata bisa diverifikasi.
 *
 * Semua store lewat [DataStores] (jalur produksi) — DataStore melarang dua
 * instance aktif untuk file yang sama, dan [DataStores] menjamin satu
 * [PreferencesStore] per file.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
// Conscrypt (TLS) tidak punya native untuk linux-arm64 (Termux) — matikan.
@ConscryptMode(ConscryptMode.Mode.OFF)
class PreferencesStoreTest {

    private lateinit var context: Context
    private lateinit var store: PreferencesStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // filesDir Robolectric unik per metode test → store selalu baru.
        store = DataStores.preferences(context, "prefs-store-test")
    }

    @Test
    fun `baca sebelum ada tulis - kosong`() {
        assertThat(store.current[stringPreferencesKey("k")]).isNull()
    }

    @Test
    fun `edit - nilai langsung terlihat (sinkron)`() {
        val key = stringPreferencesKey("nama")
        store.edit { this[key] = "Minshawy" }

        assertThat(store.current[key]).isEqualTo("Minshawy")
    }

    @Test
    fun `edit beruntun - nilai terakhir menang dan cache konsisten`() {
        val key = stringPreferencesKey("k")
        store.edit { this[key] = "a" }
        store.edit { this[key] = "b" }
        store.edit { this[key] = "c" }

        assertThat(store.current[key]).isEqualTo("c")
    }

    @Test
    fun `remove - key hilang`() {
        val key = stringPreferencesKey("k")
        store.edit { this[key] = "v" }
        store.edit { remove(key) }

        assertThat(store.current[key]).isNull()
    }

    @Test
    fun `instance kedua - nilai langsung terlihat (registry bersama)`() {
        store.edit { this[stringPreferencesKey("k")] = "persisted" }

        // DataStores mengembalikan PreferencesStore yang SAMA per file →
        // cache bersama → nilai langsung terlihat (kontrak yang dipakai
        // SettingsStore saat dibuat berkali-kali dari berbagai tempat).
        val second = DataStores.preferences(context, "prefs-store-test")
        assertThat(second.current[stringPreferencesKey("k")]).isEqualTo("persisted")
    }
}
