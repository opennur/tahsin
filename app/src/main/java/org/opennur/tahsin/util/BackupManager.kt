package org.opennur.tahsin.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.time.Instant

/**
 * Manajer backup/restore data pengguna.
 *
 * Membaca/menulis file store langsung dari [filesDir] (store menggunakan pola
 * baca-dari-disk setiap akses, jadi penulisan langsung aman). Setelan di-backup
 * via [SettingsBackupSource] agar konsisten dengan cache DataStore.
 *
 * Format backup: satu file JSON berisi envelope dengan versi schema + map
 * nama-file → konten JSON (raw) + objek setelan.
 */
class BackupManager internal constructor(
    private val filesDir: File,
    private val settingsSource: SettingsBackupSource,
    private val gson: Gson = Gson(),
) {

    companion object {
        const val SCHEMA_VERSION = 1
        const val APP_ID = "org.opennur.tahsin"

        /** Nama file store yang masuk dalam backup. */
        val STORE_FILENAMES = listOf(
            "reading-stats.json",
            "reading_history.json",
            "bookmarks.json",
            "gamification.json",
            "vocab-stats.json",
            "dreambig-progress.json",
            "lughoh-progress.json",
            "nahwu-progress.json",
            "learning-plan.json",
            "memorization.json",
        )

        fun create(context: android.content.Context, settingsSource: SettingsBackupSource): BackupManager =
            BackupManager(context.applicationContext.filesDir, settingsSource)
    }

    /**
     * Ekspor semua data pengguna ke string JSON.
     *
     * Membaca setiap file store dari [filesDir] (jika ada) + snapshot setelan.
     * File yang tidak ada di-skip (pengguna baru dengan data parsial tetap valid).
     */
    fun export(): String {
        val stores = mutableMapOf<String, String>()
        for (filename in STORE_FILENAMES) {
            val file = File(filesDir, filename)
            if (file.exists()) {
                stores[filename] = file.readText(Charsets.UTF_8)
            }
        }
        val settingsJson = settingsSource.snapshotJson()
        val envelope = JsonObject().apply {
            addProperty("schemaVersion", SCHEMA_VERSION)
            addProperty("app", APP_ID)
            addProperty("exportedAt", Instant.now().toString())
            add("stores", gson.toJsonTree(stores))
            if (settingsJson != null) {
                add("settings", JsonParser.parseString(settingsJson))
            }
        }
        return gson.toJson(envelope)
    }

    /**
     * Impor data pengguna dari string JSON backup.
     *
     * - Memvalidasi schema version dan app ID
     * - Menulis setiap file store secara atomik (temp → rename)
     * - Memulihkan setelan via [SettingsBackupSource.restoreJson]
     * - File store yang tidak ada di backup di-skip (data existing tetap aman)
     *
     * @return [BackupResult] dengan jumlah file yang diimpor + daftar error
     */
    fun import(json: String): BackupResult {
        val errors = mutableListOf<String>()
        var imported = 0

        val envelope = try {
            JsonParser.parseString(json).asJsonObject
        } catch (e: Exception) {
            return BackupResult(errors = listOf("Format backup tidak valid: ${e.message}"))
        }

        // Validasi
        val version = envelope.get("schemaVersion")?.asInt ?: 0
        if (version != SCHEMA_VERSION) {
            return BackupResult(errors = listOf("Versi backup tidak didukung: $version (diharapkan $SCHEMA_VERSION)"))
        }
        val app = envelope.get("app")?.asString
        if (app != APP_ID) {
            return BackupResult(errors = listOf("File backup bukan dari Tahsin: $app"))
        }

        // Impor store files
        val stores = envelope.getAsJsonObject("stores")
        if (stores != null) {
            for ((filename, element) in stores.entrySet()) {
                if (filename !in STORE_FILENAMES) {
                    errors.add("File tidak dikenal: $filename (diabaikan)")
                    continue
                }
                val content = element.asString
                val file = File(filesDir, filename)
                try {
                    val tmp = File(filesDir, "$filename.tmp")
                    tmp.writeText(content, Charsets.UTF_8)
                    if (!tmp.renameTo(file)) {
                        // renameTo bisa gagal di beberapa filesystem; fallback ke copy
                        file.writeText(content, Charsets.UTF_8)
                        tmp.delete()
                    }
                    imported++
                } catch (e: Exception) {
                    errors.add("Gagal menulis $filename: ${e.message}")
                }
            }
        }

        // Impor setelan
        val settings = envelope.get("settings")
        if (settings != null && !settings.isJsonNull) {
            try {
                settingsSource.restoreJson(settings.toString())
            } catch (e: Exception) {
                errors.add("Gagal memulihkan setelan: ${e.message}")
            }
        }

        return BackupResult(importedStores = imported, errors = errors)
    }
}
