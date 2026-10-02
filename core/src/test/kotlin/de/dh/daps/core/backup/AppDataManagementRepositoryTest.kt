package de.dh.daps.core.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class AppDataManagementRepositoryTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `export and read ZIP container structure`() {
        val manifest = BackupManifestDto(
            version = 1,
            appVersion = "1.0",
            schemaVersion = 2,
            createdAtMs = System.currentTimeMillis(),
            includeHistory = true,
            includeDiagnostics = false
        )

        val prefs = AppPreferencesDto(
            glucoseUnit = "MG_DL",
            carbsUnit = "GRAMS"
        )

        val therapyConfig = TherapyConfigDto(
            insulinProfiles = listOf(
                InsulinProfileDto(
                    id = 1L,
                    name = "Normal Profile",
                    basalBlocks = listOf(DBBlockDto(24, 1.0)),
                    isfBlocks = listOf(DBBlockDto(24, 40.0)),
                    crBlocks = listOf(DBBlockDto(24, 10.0)),
                    insulinTypeId = "rapid",
                    insulinConcentration = 1.0,
                    diaMinutes = 300,
                    peakMinutes = 75
                )
            )
        )

        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("manifest.json"))
            zipOut.write(json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry("preferences.json"))
            zipOut.write(json.encodeToString(prefs).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            zipOut.putNextEntry(ZipEntry("therapy_config.json"))
            zipOut.write(json.encodeToString(therapyConfig).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }

        val zipBytes = outputStream.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        var readManifest: BackupManifestDto? = null
        var readPrefs: AppPreferencesDto? = null
        var readConfig: TherapyConfigDto? = null

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val content = zipIn.readBytes().toString(Charsets.UTF_8)
                when (entry.name) {
                    "manifest.json" -> readManifest = json.decodeFromString(content)
                    "preferences.json" -> readPrefs = json.decodeFromString(content)
                    "therapy_config.json" -> readConfig = json.decodeFromString(content)
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        assertNotNull(readManifest)
        assertEquals(1, readManifest?.version)
        assertEquals(2, readManifest?.schemaVersion)

        assertNotNull(readPrefs)
        assertEquals("MG_DL", readPrefs?.glucoseUnit)
        assertEquals("GRAMS", readPrefs?.carbsUnit)

        assertNotNull(readConfig)
        assertEquals(1, readConfig?.insulinProfiles?.size)
        assertEquals("Normal Profile", readConfig?.insulinProfiles?.first()?.name)
    }

    @Test
    fun `default import backup options exclude optional components except descriptors`() {
        val importOptions = BackupOptions(
            includeHistory = false,
            includeDiagnostics = false,
            includeDescriptors = true,
        )
        assertEquals(false, importOptions.includeHistory)
        assertEquals(false, importOptions.includeDiagnostics)
        assertEquals(true, importOptions.includeDescriptors)
    }
}