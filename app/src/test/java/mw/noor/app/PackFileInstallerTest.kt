package mw.noor.app

import java.io.File
import java.nio.file.Files
import mw.noor.app.data.PackFileInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackFileInstallerTest {
    @Test
    fun failedPromotionRestoresPreviouslyInstalledPack() {
        val directory = Files.createTempDirectory("noor-pack-update").toFile()
        try {
            val installed = File(directory, "biology.json").apply { writeText("old-valid-pack") }
            val staged = File(directory, "biology.json.tmp").apply { writeText("new-valid-pack") }
            val installer = PackFileInstaller { source, destination ->
                if (source == staged && destination == installed) false else source.renameTo(destination)
            }

            val error = runCatching { installer.install(staged, installed) }.exceptionOrNull()

            assertTrue(error is IllegalStateException)
            assertTrue(error?.message?.contains("previous pack is still available") == true)
            assertEquals("old-valid-pack", installed.readText())
            assertTrue(staged.exists())
            assertFalse(File(directory, "biology.json.bak").exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun interruptedSwapRestoresBackupWhenDestinationIsMissing() {
        val directory = Files.createTempDirectory("noor-pack-recovery").toFile()
        try {
            val destination = File(directory, "physics.json")
            val backup = File(directory, "physics.json.bak").apply { writeText("previous-pack") }

            PackFileInstaller().recover(destination)

            assertTrue(destination.exists())
            assertEquals("previous-pack", destination.readText())
            assertFalse(backup.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun completedSwapKeepsNewDestinationAndDeletesLeftoverBackup() {
        val directory = Files.createTempDirectory("noor-pack-cleanup").toFile()
        try {
            val destination = File(directory, "physics.json").apply { writeText("new-pack") }
            val backup = File(directory, "physics.json.bak").apply { writeText("old-pack") }

            PackFileInstaller().recover(destination)

            assertEquals("new-pack", destination.readText())
            assertFalse(backup.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun failedRecoveryKeepsBackupForAnotherAttempt() {
        val directory = Files.createTempDirectory("noor-pack-recovery-failure").toFile()
        try {
            val destination = File(directory, "biology.json")
            val backup = File(directory, "biology.json.bak").apply { writeText("previous-pack") }

            PackFileInstaller { _, _ -> false }.recover(destination)

            assertFalse(destination.exists())
            assertTrue(backup.exists())
            assertEquals("previous-pack", backup.readText())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun deleteAllArtifactsRemovesDestinationBackupAndBothTemporaryFiles() {
        val directory = Files.createTempDirectory("noor-pack-delete").toFile()
        try {
            val destination = File(directory, "biology.json").apply { writeText("installed") }
            File(directory, "biology.json.bak").writeText("backup")
            File(directory, "biology.json.tmp").writeText("partial")
            File(directory, "biology.json.migration.tmp").writeText("migration")

            PackFileInstaller().deleteAllArtifacts(destination)

            assertFalse(destination.exists())
            assertFalse(File(directory, "biology.json.bak").exists())
            assertFalse(File(directory, "biology.json.tmp").exists())
            assertFalse(File(directory, "biology.json.migration.tmp").exists())
            PackFileInstaller().recover(destination)
            assertFalse("Deleted pack must not be restored by recovery", destination.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun deleteAllArtifactsIsANoOpWhenNoArtifactsExist() {
        val directory = Files.createTempDirectory("noor-pack-delete-empty").toFile()
        try {
            val destination = File(directory, "biology.json")

            PackFileInstaller().deleteAllArtifacts(destination)

            assertFalse(destination.exists())
            assertEquals(emptyList<String>(), directory.list()?.toList().orEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }
}
