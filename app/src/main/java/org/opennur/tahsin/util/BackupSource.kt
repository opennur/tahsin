package org.opennur.tahsin.util

/**
 * Interface untuk backup/restore setelan — dipisah dari [BackupManager]
 * supaya [SettingsStore] tidak perlu meng-implementasi interface di file
 * yang sama dengan Gson/JsonParser (menghindari masalah classpath di test).
 */
interface SettingsBackupSource {
    fun snapshotJson(): String?
    fun restoreJson(json: String)
}

/**
 * Adapter yang menghubungkan [SettingsStore] dengan [SettingsBackupSource].
 *
 * Dipisah supaya [SettingsStore] tidak perlu meng-implementasi interface
 * langsung (menghindari import Gson di [SettingsStore] yang bisa
 * menyebabkan masalah classpath di test environment).
 */
class SettingsBackupAdapter(private val store: SettingsStore) : SettingsBackupSource {
    override fun snapshotJson(): String = store.snapshotJson()
    override fun restoreJson(json: String) = store.restoreJson(json)
}

/**
 * Hasil operasi impor backup.
 *
 * @param importedStores Jumlah file store yang berhasil diimpor
 * @param errors Daftar error yang terjadi selama impor (0 = sukses penuh)
 */
data class BackupResult(
    val importedStores: Int = 0,
    val errors: List<String> = emptyList(),
) {
    val success: Boolean get() = errors.isEmpty()
}
