package mw.noor.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LessonCatalogHttpsTest {
    @Test
    fun currentCatalogsAcceptAllThirteenLessonUnitsAndBothPacks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val lessonIndexRaw = context.assets.open("lesson_catalog.json").bufferedReader().use { it.readText() }
        val catalogRaw = context.assets.open("form3_catalog.json").bufferedReader().use { it.readText() }

        assertEquals(13, LessonContentJsonParser.parseIndex(lessonIndexRaw).lessons.size)
        assertEquals(2, ContentJsonParser.parseCatalog(catalogRaw).size)
    }

    @Test
    fun lessonIndexRejectsHttpSourceUrl() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val raw = context.assets.open("lesson_catalog.json").bufferedReader().use { it.readText() }
        val insecure = raw.replaceFirst("https://", "http://")

        val error = runCatching { LessonContentJsonParser.parseIndex(insecure) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Lesson source URLs must use HTTPS.", error?.message)
    }

    @Test
    fun packCatalogRejectsHttpSourceUrl() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val raw = context.assets.open("form3_catalog.json").bufferedReader().use { it.readText() }
        val insecure = raw.replaceFirst("https://", "http://")

        val error = runCatching { ContentJsonParser.parseCatalog(insecure) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Catalogue source URLs must use HTTPS.", error?.message)
    }

    @Test
    fun repositoryRejectsHttpLessonAndPackSourcesBeforeOpeningConnection() {
        val isolated = IsolatedNoorContext.create("http-download-rejection")
        try {
            val repository = NoorRepository(isolated.context)
            val lesson = repository.lessonCatalog().lessons.first().copy(
                sourceUrls = listOf("http://noor-http-source-must-not-be-opened.invalid/lesson.json"),
            )
            val pack = repository.catalog().first().copy(
                sourceUrls = listOf("http://noor-http-source-must-not-be-opened.invalid/pack.json"),
            )

            val lessonError = runCatching {
                runBlocking { repository.downloadLesson(lesson) { } }
            }.exceptionOrNull()
            val packError = runCatching {
                runBlocking { repository.downloadPack(pack) { } }
            }.exceptionOrNull()

            assertEquals("Content sources must use HTTPS.", lessonError?.message)
            assertEquals("Content sources must use HTTPS.", packError?.message)
        } finally {
            isolated.cleanup()
        }
    }
}
