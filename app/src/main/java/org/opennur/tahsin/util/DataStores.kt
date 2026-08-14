package org.opennur.tahsin.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Pabrik [PreferencesStore] yang AMAN terhadap banyak instance.
 *
 * Aplikasi ini membuat `SettingsStore`/`AyahOfTheDayManager` dari berbagai
 * tempat (MainActivity, receiver widget, ViewModel Hilt), jadi semua pemanggil
 * harus berbagi SATU [PreferencesStore] per file — kalau tidak, cache-nya tidak
 * sinkron dan DataStore melempar IllegalStateException untuk dua instance
 * aktif pada file yang sama. Registry global (kunci jalur file) menjamin itu.
 *
 * Migrasi dari SharedPreferences lama dilakukan otomatis (sekali jalan) lewat
 * [SharedPreferencesMigration] — setelan pengguna tidak hilang saat upgrade.
 */
object DataStores {

    private val dataStoreInstances = HashMap<String, DataStore<Preferences>>()
    private val stores = HashMap<String, PreferencesStore>()

    /**
     * [PreferencesStore] bersama untuk [name] (file
     * `filesDir/datastore/<name>.preferences_pb`). [legacySharedPrefsName] =
     * nama file SharedPreferences lama yang akan dimigrasi key-nya saat
     * DataStore pertama kali dibuka.
     */
    @Synchronized
    fun preferences(
        context: Context,
        name: String,
        legacySharedPrefsName: String? = null,
    ): PreferencesStore {
        val app = context.applicationContext
        val file = File(app.filesDir, "datastore/$name.preferences_pb")
        return stores.getOrPut(file.absolutePath) {
            val dataStore = dataStoreInstances.getOrPut(file.absolutePath) {
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                    produceFile = { file },
                    migrations = legacySharedPrefsName?.let { legacy ->
                        listOf(SharedPreferencesMigration(app, legacy))
                    } ?: emptyList(),
                )
            }
            PreferencesStore(dataStore)
        }
    }
}
