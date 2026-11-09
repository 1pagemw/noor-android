package mw.noor.app.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mw.noor.app.MainActivity
import mw.noor.app.QuizQuestion
import mw.noor.app.data.ContentLesson
import mw.noor.app.data.ContentPack
import mw.noor.app.data.ContentTopic
import mw.noor.app.data.LessonIndexEntry
import mw.noor.app.data.NoorRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LearningFlowStateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun lessonScreenHasNoManualCompletionOrReportActions() {
        val repository = isolatedRepository("footer")
        repository.clearProgress()
        val lesson = ContentLesson(
            id = "footer-test-lesson",
            title = "Test lesson",
            readTimeMinutes = 1,
            sections = emptyList(),
            keyTerms = emptyList(),
            questions = emptyList(),
        )
        val topic = ContentTopic("footer-test-topic", "Test topic", "", listOf(lesson))
        val pack = ContentPack("footer-test-pack", 3, "Biology", "en", 1, "provisional", listOf(topic))

        composeRule.setContent {
            MaterialTheme {
                LessonScreen(
                    pack = pack,
                    topic = topic,
                    lesson = lesson,
                    repository = repository,
                    onBack = {},
                    onStartQuiz = {},
                )
            }
        }

        composeRule.onNodeWithText("Mark lesson complete").assertDoesNotExist()
        composeRule.onNodeWithText("Report a content issue").assertDoesNotExist()
        composeRule.onNodeWithText("Practise this topic").assertIsDisplayed()
    }

    @Test
    fun finishingZeroScoreQuizCompletesOnlyQuestionLessonsAndOffersNextLesson() {
        val repository = isolatedRepository("quiz")
        repository.clearProgress()
        val question = QuizQuestion(
            prompt = "Which option is correct?",
            options = listOf("Wrong answer", "Correct answer"),
            correctOptionIndex = 1,
            id = "flow-test-question",
            explanation = "The second option is correct.",
        )
        val lesson = ContentLesson(
            id = "flow-test-lesson",
            title = "Test lesson",
            readTimeMinutes = 1,
            sections = emptyList(),
            keyTerms = emptyList(),
            questions = listOf(question),
        )
        val noQuestionLesson = ContentLesson(
            id = "flow-test-empty-lesson",
            title = "Lesson without questions",
            readTimeMinutes = 1,
            sections = emptyList(),
            keyTerms = emptyList(),
            questions = emptyList(),
        )
        val topic = ContentTopic("flow-test-topic", "Test topic", "", listOf(lesson, noQuestionLesson))
        val pack = ContentPack("flow-test-pack", 3, "Biology", "en", 1, "provisional", listOf(topic))
        val nextLesson = LessonIndexEntry(
            packId = pack.id,
            form = pack.form,
            subject = pack.subject,
            language = pack.language,
            curriculumStatus = pack.curriculumStatus,
            topicId = "next-topic",
            topicTitle = "Next topic",
            topicDescription = "",
            lessonId = "flow-test-next-lesson",
            title = "Next lesson title",
            readTimeMinutes = 2,
            contentVersion = 1,
            sizeBytes = 100,
            sha256 = "0".repeat(64),
            sourceUrls = listOf("https://example.test/next.json"),
            order = 1,
        )
        var openedNextLessonId: String? = null

        composeRule.setContent {
            MaterialTheme {
                QuizScreen(
                    pack = pack,
                    topic = topic,
                    repository = repository,
                    onBack = {},
                    onFinished = { _, _ -> },
                    onOpenNextLesson = { openedNextLessonId = it.lessonId },
                    nextLesson = nextLesson,
                    onProgressChanged = {},
                    cachedLessonCount = 2,
                    totalLessonCount = 2,
                )
            }
        }

        composeRule.onNodeWithText("Wrong answer").performClick()
        composeRule.onNodeWithText("Check answer").performClick()
        composeRule.onNodeWithText("See results").performClick()
        composeRule.onNodeWithText("0%").assertIsDisplayed()
        composeRule.onNodeWithText("1 lesson complete.").assertIsDisplayed()
        composeRule.onNodeWithText("Up next").assertIsDisplayed()
        composeRule.onNodeWithText("Next lesson").performClick()
        composeRule.runOnIdle {
            assertEquals("flow-test-next-lesson", openedNextLessonId)
            assertTrue("Completion must be separate from quiz accuracy", "flow-test-lesson" in repository.completedLessonIds())
            assertFalse("A lesson without quiz questions must stay incomplete", "flow-test-empty-lesson" in repository.completedLessonIds())
            assertEquals(0, repository.topicProgress(topic.id)?.lastScorePercent ?: -1)
            repository.clearProgress()
        }
    }

    private fun isolatedRepository(testName: String): NoorRepository {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val isolatedContext = object : ContextWrapper(baseContext) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(preferenceName: String, mode: Int): SharedPreferences =
                baseContext.getSharedPreferences("learning-flow-$testName-$preferenceName", mode)
            override fun getFilesDir(): File = File(baseContext.filesDir, "learning-flow-$testName").apply { mkdirs() }
        }
        return NoorRepository(isolatedContext)
    }
}
