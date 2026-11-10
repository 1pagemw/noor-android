package mw.noor.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileSafetyTest {
    @Test
    fun twoRepositoriesSerializeInstallRecoverAndDeleteWithoutResurrection() {
        val isolated = IsolatedNoorContext.create("file-gate")
        try {
            val firstRepository = NoorRepository(isolated.context)
            val secondRepository = NoorRepository(isolated.context)
            val entry = firstRepository.lessonCatalog().lessons.first()
            val destination = lessonFile(isolated.context, entry)
            val legacyFile = File(File(isolated.context.filesDir, "content-packs"), "${entry.packId}.json")
            val canonicalPack = InstrumentationRegistry.getInstrumentation().context.assets
                .open("form3_biology.json").bufferedReader().use { it.readText() }
            legacyFile.writeText(canonicalPack)
            assertNotNull("Repository migration should install the lesson", firstRepository.loadLesson(entry))
            val validPayload = destination.readText()

            // Model a process interruption after moving the old file to .bak but before promotion.
            assertTrue(destination.delete())
            val backup = File(destination.parentFile, "${destination.name}.bak").apply { writeText(validPayload) }
            File(destination.parentFile, "${destination.name}.tmp").writeText("partial")
            File(destination.parentFile, "${destination.name}.migration.tmp").writeText("partial migration")

            val recoveryEntered = CountDownLatch(1)
            val allowRecoveryOwnerToFinish = CountDownLatch(1)
            val deleteStarted = CountDownLatch(1)
            val deleteFinished = CountDownLatch(1)
            val threadFailure = AtomicReference<Throwable?>(null)
            val recoveredLesson = AtomicReference<ContentLesson?>(null)
            val deletionResult = AtomicReference<Boolean?>(null)
            val recoveryThread = Thread {
                try {
                    synchronized(NoorFileGate.lock) {
                        val lesson = firstRepository.loadLesson(entry)
                        recoveredLesson.set(lesson)
                        recoveryEntered.countDown()
                        check(allowRecoveryOwnerToFinish.await(5, TimeUnit.SECONDS))
                    }
                } catch (error: Throwable) {
                    threadFailure.compareAndSet(null, error)
                    recoveryEntered.countDown()
                }
            }
            val deletionThread = Thread {
                try {
                    check(recoveryEntered.await(5, TimeUnit.SECONDS))
                    deleteStarted.countDown()
                    deletionResult.set(secondRepository.deleteLesson(entry))
                } catch (error: Throwable) {
                    threadFailure.compareAndSet(null, error)
                } finally {
                    deleteFinished.countDown()
                }
            }

            recoveryThread.start()
            deletionThread.start()
            assertTrue("Recovery must reach the controlled interleaving", recoveryEntered.await(5, TimeUnit.SECONDS))
            assertTrue("Deletion thread should start while recovery owns the gate", deleteStarted.await(5, TimeUnit.SECONDS))
            val deletionFinishedWhileGateHeld: Boolean
            try {
                deletionFinishedWhileGateHeld = deleteFinished.await(100, TimeUnit.MILLISECONDS)
            } finally {
                allowRecoveryOwnerToFinish.countDown()
            }
            recoveryThread.join(5_000)
            deletionThread.join(5_000)

            threadFailure.get()?.let { error ->
                throw AssertionError(
                    "Concurrent repository operation failed: ${error.javaClass.name}: ${error.message}; " +
                        "at ${error.stackTrace.firstOrNull()}",
                    error,
                )
            }
            assertFalse("Both threads should finish", recoveryThread.isAlive || deletionThread.isAlive)
            assertNotNull(
                "Recovery should restore and read the lesson before deletion",
                recoveredLesson.get(),
            )
            assertFalse("Deletion must wait until the other repository finishes recovery", deletionFinishedWhileGateHeld)
            assertTrue(deleteFinished.await(1, TimeUnit.SECONDS))
            assertTrue("The second repository should report successful deletion", deletionResult.get() == true)
            assertNull(secondRepository.loadLesson(entry))
            assertFalse(destination.exists())
            assertFalse(backup.exists())
            assertFalse(File(destination.parentFile, "${destination.name}.tmp").exists())
            assertFalse(File(destination.parentFile, "${destination.name}.migration.tmp").exists())
        } finally {
            isolated.cleanup()
        }
    }

    @Test
    fun loadingLessonRestoresBackupWhenDestinationIsMissing() {
        val isolated = IsolatedNoorContext.create("restore-lesson-backup")
        try {
            val repository = NoorRepository(isolated.context)
            val entry = repository.lessonCatalog().lessons.first()
            val destination = lessonFile(isolated.context, entry)
            val canonicalPack = InstrumentationRegistry.getInstrumentation().context.assets
                .open("form3_biology.json").bufferedReader().use { it.readText() }
            val validPayload = LessonContentJsonParser.payloadFromLegacyPack(canonicalPack, entry)
            val backup = File(destination.parentFile, "${destination.name}.bak").apply { writeText(validPayload) }

            val recovered = repository.loadLesson(entry)

            assertNotNull("The valid backup should be restored and parsed", recovered)
            assertTrue(destination.isFile)
            assertFalse(backup.exists())
        } finally {
            isolated.cleanup()
        }
    }

    @Test
    fun deletingLessonRemovesBackupAndAllTemporaryArtifactsAndDoesNotRestoreIt() {
        val isolated = IsolatedNoorContext.create("delete-lesson")
        try {
            val repository = NoorRepository(isolated.context)
            val entry = repository.lessonCatalog().lessons.first()
            val destination = lessonFile(isolated.context, entry)
            val canonicalPack = InstrumentationRegistry.getInstrumentation().context.assets
                .open("form3_biology.json").bufferedReader().use { it.readText() }
            val validPayload = LessonContentJsonParser.payloadFromLegacyPack(canonicalPack, entry)
            val backup = File(destination.parentFile, "${destination.name}.bak").apply { writeText(validPayload) }
            File(destination.parentFile, "${destination.name}.tmp").writeText("partial")
            File(destination.parentFile, "${destination.name}.migration.tmp").writeText("partial migration")

            assertTrue(repository.deleteLesson(entry))
            assertNull(repository.loadLesson(entry))
            assertFalse(destination.exists())
            assertFalse(backup.exists())
            assertFalse(File(destination.parentFile, "${destination.name}.tmp").exists())
            assertFalse(File(destination.parentFile, "${destination.name}.migration.tmp").exists())
        } finally {
            isolated.cleanup()
        }
    }

    @Test
    fun deletingPackRemovesBackupAndAllTemporaryArtifactsAndDoesNotRestoreIt() {
        val isolated = IsolatedNoorContext.create("delete-pack")
        try {
            val repository = NoorRepository(isolated.context)
            val entry = repository.catalog().first { it.id == "form3_biology" }
            val destination = File(File(isolated.context.filesDir, "content-packs"), "${entry.id}.json")
            val canonicalPack = InstrumentationRegistry.getInstrumentation().context.assets
                .open("form3_biology.json").use { it.readBytes() }
            val backup = File(destination.parentFile, "${destination.name}.bak").apply { writeBytes(canonicalPack) }
            File(destination.parentFile, "${destination.name}.tmp").writeText("partial")
            File(destination.parentFile, "${destination.name}.migration.tmp").writeText("partial migration")

            assertTrue(repository.deletePack(entry))
            assertNull(repository.loadPack(entry))
            assertFalse(destination.exists())
            assertFalse(backup.exists())
            assertFalse(File(destination.parentFile, "${destination.name}.tmp").exists())
            assertFalse(File(destination.parentFile, "${destination.name}.migration.tmp").exists())
        } finally {
            isolated.cleanup()
        }
    }

    @Test
    fun deletingMissingLessonOrPackIsANoOp() {
        val isolated = IsolatedNoorContext.create("delete-missing")
        try {
            val repository = NoorRepository(isolated.context)
            val lesson = repository.lessonCatalog().lessons.first()
            val pack = repository.catalog().first()

            assertTrue(repository.deleteLesson(lesson))
            assertTrue(repository.deletePack(pack))
            assertNull(repository.loadLesson(lesson))
            assertNull(repository.loadPack(pack))
        } finally {
            isolated.cleanup()
        }
    }

    private fun lessonFile(context: android.content.Context, entry: LessonIndexEntry): File =
        File(File(context.filesDir, "content-lessons"), "${entry.packId}__${entry.lessonId}.json")
}
