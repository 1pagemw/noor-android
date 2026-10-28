package mw.noor.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LessonMigrationTest {
    @Test
    fun validLegacyPackMigratesWithoutLosingContentOrProgress() = withIsolatedBiologyFiles { repository, subject, legacyFile, _, lessonFiles, previousProgress ->
        val canonical = canonicalPack()
        val expectedEntry = repository.catalog().first { it.id == subject.id }
        assertEquals(expectedEntry.sha256, sha256(canonical))
        legacyFile.writeBytes(canonical)

        val expectedPack = ContentJsonParser.parsePack(String(canonical, Charsets.UTF_8))
        val migratedLesson = repository.loadLesson(subject.lessons.first())

        assertEquals(expectedPack.topics.first().lessons.first(), migratedLesson)
        assertTrue(repository.savedLessonIds().containsAll(subject.lessons.map { it.lessonId }))
        assertTrue(lessonFiles.all { it.isFile })
        assertFalse("Legacy pack is removed only after every lesson is installed", legacyFile.exists())
        assertEquals(previousProgress, repository.completedLessonIds())
    }

    @Test
    fun truncatedLegacyPackIsKeptAndMigratesNoLessons() = withIsolatedBiologyFiles { repository, subject, legacyFile, _, lessonFiles, _ ->
        val truncated = canonicalPack().let { it.copyOfRange(0, it.size / 2) }
        legacyFile.writeBytes(truncated)

        assertNull(repository.loadLesson(subject.lessons.first()))
        assertArrayEquals(truncated, legacyFile.readBytes())
        assertTrue(lessonFiles.none { it.exists() })
    }

    @Test
    fun leftoverPartialDownloadTempFileIsIgnoredAndPreserved() = withIsolatedBiologyFiles { repository, subject, legacyFile, _, lessonFiles, _ ->
        val temporaryFile = File(legacyFile.parentFile, "${legacyFile.name}.tmp")
        val partial = canonicalPack().let { it.copyOfRange(0, it.size / 2) }
        temporaryFile.writeBytes(partial)

        assertNull(repository.loadLesson(subject.lessons.first()))
        assertTrue(temporaryFile.isFile)
        assertArrayEquals(partial, temporaryFile.readBytes())
        assertFalse(legacyFile.exists())
        assertTrue(lessonFiles.none { it.exists() })
    }

    @Test
    fun modifiedCurrentVersionPackFailsCatalogHashCheckBeforeMigration() = withIsolatedBiologyFiles { repository, subject, legacyFile, _, lessonFiles, _ ->
        val modified = JSONObject(String(canonicalPack(), Charsets.UTF_8))
        modified.getJSONArray("topics")
            .getJSONObject(0)
            .getJSONArray("lessons")
            .getJSONObject(0)
            .getJSONArray("sections")
            .getJSONObject(0)
            .put("body", "Modified local content that is not in the trusted catalog.")
        val changedBytes = modified.toString().toByteArray(Charsets.UTF_8)
        assertTrue(sha256(changedBytes) != repository.catalog().first { it.id == subject.id }.sha256)
        legacyFile.writeBytes(changedBytes)

        assertNull(repository.loadLesson(subject.lessons.first()))
        assertArrayEquals(changedBytes, legacyFile.readBytes())
        assertTrue(lessonFiles.none { it.exists() })
    }

    @Test
    fun incompleteCurrentVersionPackIsRetainedInsteadOfPartiallyMigrated() = withIsolatedBiologyFiles { repository, subject, legacyFile, _, lessonFiles, _ ->
        val partial = JSONObject(String(canonicalPack(), Charsets.UTF_8))
        val topics = partial.getJSONArray("topics")
        val lastTopic = topics.getJSONObject(topics.length() - 1)
        val lastTopicLessons = lastTopic.getJSONArray("lessons")
        lastTopicLessons.remove(lastTopicLessons.length() - 1)
        val partialBytes = partial.toString().toByteArray(Charsets.UTF_8)
        assertTrue(sha256(partialBytes) != repository.catalog().first { it.id == subject.id }.sha256)
        legacyFile.writeBytes(partialBytes)

        assertNull(repository.loadLesson(subject.lessons.first()))
        assertArrayEquals(partialBytes, legacyFile.readBytes())
        assertTrue(lessonFiles.none { it.exists() })
    }

    @Test
    fun backupOnlyLegacyPackIsRecoveredThenMigrated() = withIsolatedBiologyFiles { repository, subject, legacyFile, legacyBackup, lessonFiles, _ ->
        legacyBackup.writeBytes(canonicalPack())

        val recoveredLesson = repository.loadLesson(subject.lessons.first())

        assertEquals(subject.lessons.first().title, recoveredLesson?.title)
        assertTrue(repository.savedLessonIds().containsAll(subject.lessons.map { it.lessonId }))
        assertTrue(lessonFiles.all { it.isFile })
        assertFalse(legacyFile.exists())
        assertFalse(legacyBackup.exists())
    }

    @Test
    fun validBackupIsUsedWhenPrimaryLegacyPackIsCorrupt() = withIsolatedBiologyFiles { repository, subject, legacyFile, legacyBackup, lessonFiles, _ ->
        val canonical = canonicalPack()
        legacyFile.writeBytes(canonical.copyOfRange(0, canonical.size / 2))
        legacyBackup.writeBytes(canonical)

        val recoveredLesson = repository.loadLesson(subject.lessons.first())

        assertEquals(subject.lessons.first().title, recoveredLesson?.title)
        assertTrue(repository.savedLessonIds().containsAll(subject.lessons.map { it.lessonId }))
        assertTrue(lessonFiles.all { it.isFile })
        assertFalse(legacyFile.exists())
        assertFalse(legacyBackup.exists())
    }

    private fun withIsolatedBiologyFiles(
        test: (
            repository: NoorRepository,
            subject: LessonSubjectIndex,
            legacyFile: File,
            legacyBackup: File,
            lessonFiles: List<File>,
            previousProgress: Set<String>,
        ) -> Unit,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val index = context.assets.open("lesson_catalog.json").bufferedReader().use { reader ->
            LessonContentJsonParser.parseIndex(reader.readText())
        }
        val subject = requireNotNull(index.subject("form3_biology"))
        val repository = NoorRepository(context)
        val legacyFile = File(File(context.filesDir, "content-packs"), "${subject.id}.json")
        val legacyBackup = File(legacyFile.parentFile, "${legacyFile.name}.bak")
        val legacyTemporaryFile = File(legacyFile.parentFile, "${legacyFile.name}.tmp")
        val lessonFiles = subject.lessons.map { entry ->
            File(File(context.filesDir, "content-lessons"), "${entry.packId}__${entry.lessonId}.json")
        }
        val lessonBackups = lessonFiles.map { file -> File(file.parentFile, "${file.name}.bak") }
        val migrationTemps = subject.lessons.map { entry ->
            File(File(context.filesDir, "content-lessons"), "${entry.packId}__${entry.lessonId}.migration.tmp")
        }
        val filesToRestore = (listOf(legacyFile, legacyBackup, legacyTemporaryFile) + lessonFiles + lessonBackups + migrationTemps).associateWith { file ->
            if (file.isFile) file.readBytes() else null
        }
        val previousProgress = repository.completedLessonIds()

        try {
            filesToRestore.keys.forEach { it.delete() }
            test(repository, subject, legacyFile, legacyBackup, lessonFiles, previousProgress)
        } finally {
            filesToRestore.forEach { (file, bytes) ->
                if (bytes == null) file.delete()
                else {
                    file.parentFile?.mkdirs()
                    file.writeBytes(bytes)
                }
            }
        }
    }

    private fun canonicalPack(): ByteArray =
        InstrumentationRegistry.getInstrumentation().context.assets.open("form3_biology.json").use { it.readBytes() }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
