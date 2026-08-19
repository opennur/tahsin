package org.opennur.tahsin.util

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.nio.file.Files
import org.junit.After
import org.junit.Before
import org.junit.Test

class BackupManagerTest {

    private lateinit var dir: java.io.File
    private lateinit var manager: BackupManager
    private val fakeSettings = FakeSettingsBackupSource()

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("backup-test").toFile()
        manager = BackupManager(dir, fakeSettings)
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    // ---- export ----

    @Test
    fun export_emptyDir_noStores() {
        val json = manager.export()
        val envelope = JsonParser.parseString(json).asJsonObject
        assertThat(envelope["schemaVersion"].asInt).isEqualTo(1)
        assertThat(envelope["app"].asString).isEqualTo("org.opennur.tahsin")
        assertThat(envelope["exportedAt"]).isNotNull()
        val stores = envelope.getAsJsonObject("stores")
        assertThat(stores.size()).isEqualTo(0)
    }

    @Test
    fun export_withStoreFile_includesContent() {
        val content = """{"test":"data"}"""
        java.io.File(dir, "reading-stats.json").writeText(content)

        val json = manager.export()
        val envelope = JsonParser.parseString(json).asJsonObject
        val stores = envelope.getAsJsonObject("stores")
        assertThat(stores.has("reading-stats.json")).isTrue()
        assertThat(stores["reading-stats.json"].asString).isEqualTo(content)
    }

    @Test
    fun export_multipleStoreFiles() {
        java.io.File(dir, "reading-stats.json").writeText("[]")
        java.io.File(dir, "bookmarks.json").writeText("[]")
        java.io.File(dir, "gamification.json").writeText("{}")

        val json = manager.export()
        val stores = JsonParser.parseString(json).asJsonObject.getAsJsonObject("stores")
        assertThat(stores.size()).isEqualTo(3)
        assertThat(stores.has("reading-stats.json")).isTrue()
        assertThat(stores.has("bookmarks.json")).isTrue()
        assertThat(stores.has("gamification.json")).isTrue()
    }

    @Test
    fun export_includesSettings() {
        fakeSettings.snapshot = """{"dark_mode":true,"language_code":"en"}"""
        val json = manager.export()
        val envelope = JsonParser.parseString(json).asJsonObject
        assertThat(envelope.has("settings")).isTrue()
        assertThat(envelope["settings"].asJsonObject["dark_mode"].asBoolean).isTrue()
    }

    @Test
    fun export_ignoresUnknownFiles() {
        java.io.File(dir, "reading-stats.json").writeText("[]")
        java.io.File(dir, "unknown-file.json").writeText("should not be exported")

        val json = manager.export()
        val stores = JsonParser.parseString(json).asJsonObject.getAsJsonObject("stores")
        assertThat(stores.size()).isEqualTo(1)
        assertThat(stores.has("unknown-file.json")).isFalse()
    }

    // ---- import ----

    @Test
    fun import_invalidJson_returnsError() {
        val result = manager.import("not valid json")
        assertThat(result.success).isFalse()
        assertThat(result.errors).isNotEmpty()
    }

    @Test
    fun import_wrongSchema_returnsError() {
        val json = """{"schemaVersion":99,"app":"org.opennur.tahsin","stores":{}}"""
        val result = manager.import(json)
        assertThat(result.success).isFalse()
        assertThat(result.errors[0]).contains("Versi backup tidak didukung")
    }

    @Test
    fun import_wrongApp_returnsError() {
        val json = """{"schemaVersion":1,"app":"com.other.app","stores":{}}"""
        val result = manager.import(json)
        assertThat(result.success).isFalse()
        assertThat(result.errors[0]).contains("bukan dari Tahsin")
    }

    @Test
    fun import_roundTrip_preservesData() {
        // Write fixture data
        val statsContent = """[{"surahNumber":1,"ayahNumber":1,"attempts":5,"bestScore":90}]"""
        val bookmarksContent = """[{"surah":1,"ayah":1}]"""
        java.io.File(dir, "reading-stats.json").writeText(statsContent)
        java.io.File(dir, "bookmarks.json").writeText(bookmarksContent)
        fakeSettings.snapshot = """{"dark_mode":true}"""

        // Export
        val exported = manager.export()

        // Clear directory
        dir.listFiles()?.forEach { it.delete() }
        assertThat(dir.listFiles()).isEmpty()

        // Import
        val result = manager.import(exported)
        assertThat(result.success).isTrue()
        assertThat(result.importedStores).isEqualTo(2)

        // Verify file contents
        assertThat(java.io.File(dir, "reading-stats.json").readText()).isEqualTo(statsContent)
        assertThat(java.io.File(dir, "bookmarks.json").readText()).isEqualTo(bookmarksContent)

        // Verify settings restored
        assertThat(fakeSettings.restoredJson).isNotNull()
        assertThat(fakeSettings.restoredJson).contains("dark_mode")
    }

    @Test
    fun import_unknownFile_addsError() {
        val json = buildBackupJson(mapOf("unknown.json" to """{"x":1}"""))
        val result = manager.import(json)
        assertThat(result.success).isFalse()
        assertThat(result.errors[0]).contains("tidak dikenal")
    }

    @Test
    fun import_missingFilesInBackup_doesNotDeleteExisting() {
        // Create existing file
        java.io.File(dir, "reading-stats.json").writeText("existing")

        // Import backup with only bookmarks (no reading-stats)
        val json = buildBackupJson(mapOf("bookmarks.json" to """[]"""))
        val result = manager.import(json)
        assertThat(result.success).isTrue()

        // reading-stats should still exist (not deleted)
        assertThat(java.io.File(dir, "reading-stats.json").readText()).isEqualTo("existing")
        // bookmarks should be imported
        assertThat(java.io.File(dir, "bookmarks.json").readText()).isEqualTo("[]")
    }

    @Test
    fun import_restoresSettings() {
        val json = buildBackupJson(emptyMap(), settings = """{"language_code":"en","dark_mode":true}""")
        val result = manager.import(json)
        assertThat(result.success).isTrue()
        assertThat(fakeSettings.restoredJson).contains("language_code")
    }

    @Test
    fun import_noSettings_doesNotFail() {
        val json = buildBackupJson(mapOf("bookmarks.json" to """[]"""))
        val result = manager.import(json)
        assertThat(result.success).isTrue()
        assertThat(fakeSettings.restoredJson).isNull()
    }

    // ---- STORE_FILENAMES ----

    @Test
    fun storeFileNames_contains9Files() {
        assertThat(BackupManager.STORE_FILENAMES).hasSize(12)
    }

    @Test
    fun storeFileNames_containsExpectedFiles() {
        val expected = setOf(
            "reading-stats.json", "reading_history.json", "bookmarks.json",
            "gamification.json", "vocab-stats.json", "dreambig-progress.json",
            "lughoh-progress.json", "nahwu-progress.json", "shorof-progress.json",
            "question-history.json",
            "learning-plan.json", "memorization.json",
        )
        assertThat(BackupManager.STORE_FILENAMES.toSet()).isEqualTo(expected)
    }

    // ---- helpers ----

    private fun buildBackupJson(
        stores: Map<String, String>,
        settings: String? = null,
    ): String {
        val gson = Gson()
        val envelope = com.google.gson.JsonObject().apply {
            addProperty("schemaVersion", 1)
            addProperty("app", "org.opennur.tahsin")
            addProperty("exportedAt", "2026-01-01T00:00:00Z")
            add("stores", gson.toJsonTree(stores))
            if (settings != null) {
                add("settings", JsonParser.parseString(settings))
            }
        }
        return gson.toJson(envelope)
    }
}

/** Fake SettingsBackupSource for testing. */
private class FakeSettingsBackupSource : SettingsBackupSource {
    var snapshot: String? = null
    var restoredJson: String? = null

    override fun snapshotJson(): String? = snapshot
    override fun restoreJson(json: String) { restoredJson = json }
}
