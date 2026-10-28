package mw.noor.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LessonMigrationTest {
    @Test
    fun legacySubjectPackMigratesToLessonFilesWithoutLosingProgressOrSourceCopyEarly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val index = context.assets.open("lesson_catalog.json").bufferedReader().use { reader ->
            LessonContentJsonParser.parseIndex(reader.readText())
        }
        val subject = requireNotNull(index.subject("form3_biology"))
        val repository = NoorRepository(context)
        val legacyFile = File(File(context.filesDir, "content-packs"), "${subject.id}.json")
        val lessonFiles = subject.lessons.map { entry ->
            File(File(context.filesDir, "content-lessons"), "${entry.packId}__${entry.lessonId}.json")
        }
        val backupFiles = lessonFiles.map { file -> File(file.parentFile, "${file.name}.bak") }
        val filesToRestore = (listOf(legacyFile) + lessonFiles + backupFiles).associateWith { file ->
            if (file.isFile) file.readBytes() else null
        }
        val previousProgress = repository.completedLessonIds()

        try {
            legacyFile.parentFile?.mkdirs()
            (lessonFiles + backupFiles).forEach { it.delete() }
            legacyFile.writeText(legacyPackFixture(subject).toString())

            val migratedLesson = repository.loadLesson(subject.lessons.first())

            assertNotNull(migratedLesson)
            assertEquals("Previously saved lesson text.", migratedLesson?.sections?.firstOrNull()?.body)
            assertTrue(repository.savedLessonIds().containsAll(subject.lessons.map { it.lessonId }))
            assertFalse("Legacy pack should be removed only after all its lessons are installed", legacyFile.exists())
            assertEquals(previousProgress, repository.completedLessonIds())
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

    private fun legacyPackFixture(subject: LessonSubjectIndex): JSONObject {
        val topics = JSONArray()
        subject.topics.forEach { topic ->
            val lessons = JSONArray()
            topic.lessons.forEach { entry ->
                lessons.put(
                    JSONObject()
                        .put("id", entry.lessonId)
                        .put("title", entry.title)
                        .put("readTimeMinutes", entry.readTimeMinutes)
                        .put("sections", JSONArray().put(JSONObject().put("heading", "Saved text").put("body", "Previously saved lesson text.")))
                        .put("keyTerms", JSONArray())
                        .put(
                            "questions",
                            JSONArray().put(
                                JSONObject()
                                    .put("id", "${entry.lessonId}_migration_question")
                                    .put("prompt", "Migration check?")
                                    .put("options", JSONArray().put("Yes").put("No"))
                                    .put("correctOptionIndex", 0)
                                    .put("explanation", "The fixture remains intact."),
                            ),
                        ),
                )
            }
            topics.put(
                JSONObject()
                    .put("id", topic.id)
                    .put("title", topic.title)
                    .put("description", topic.description)
                    .put("lessons", lessons),
            )
        }
        return JSONObject()
            .put("id", subject.id)
            .put("form", subject.form)
            .put("subject", subject.subject)
            .put("language", "en")
            .put("contentVersion", subject.lessons.first().contentVersion)
            .put("curriculumStatus", subject.curriculumStatus)
            .put("topics", topics)
    }
}
