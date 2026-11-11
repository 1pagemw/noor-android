package mw.noor.app.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mw.noor.app.QuizQuestion
import mw.noor.app.data.ContentCatalogEntry
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
    val composeRule = createComposeRule()

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

        assertEquals(0, composeRule.onAllNodesWithText("Mark lesson complete").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Report a content issue").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Practise this topic").assertIsDisplayed()
    }

    @Test
    fun todayContinueCardUsesButtonForStateAwareLessonActionWithoutTopKicker() {
        val lesson = ContentLesson(
            id = "today-card-lesson",
            title = "Cells: the basic units of life",
            readTimeMinutes = 3,
            sections = emptyList(),
            keyTerms = emptyList(),
            questions = emptyList(),
        )
        val topic = ContentTopic("today-card-topic", "Cells", "", listOf(lesson))
        val entry = ContentCatalogEntry(
            id = "today-card-pack",
            title = "Biology",
            subject = "Biology",
            form = 3,
            description = "",
            topicCount = 1,
            contentVersion = 1,
            sha256 = "0".repeat(64),
            sizeBytes = 1,
            sourceUrls = listOf("https://example.test/catalog.json"),
            curriculumStatus = "verified",
        )
        var opened = 0

        composeRule.setContent {
            MaterialTheme {
                ContinueLearningCard(
                    target = HomeLessonTarget(entry, topic, lesson),
                    completedLessons = 0,
                    totalLessons = 1,
                    buttonLabel = "Start a lesson",
                    onClick = { opened += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Biology · Cells").assertIsDisplayed()
        composeRule.onNodeWithText("Cells: the basic units of life").assertIsDisplayed()
        composeRule.onNodeWithText("Start a lesson").assertIsDisplayed().assertHasClickAction().performClick()
        assertEquals(0, composeRule.onAllNodesWithText("CONTINUE LEARNING").fetchSemanticsNodes().size)
        composeRule.runOnIdle { assertEquals(1, opened) }
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
        var backCount = 0
        var finishedResult: Pair<Int, Int>? = null

        composeRule.setContent {
            MaterialTheme {
                QuizScreen(
                    pack = pack,
                    topic = topic,
                    repository = repository,
                    onBack = { backCount += 1 },
                    onFinished = { correct, total -> finishedResult = correct to total },
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
        composeRule.onNodeWithText("Keep going! Review the lesson and try again.").assertIsDisplayed()
        composeRule.onNodeWithText("Review recommended").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Go over key concepts").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 lesson complete.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Up next").performScrollTo().assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("0 of 1 correct").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithContentDescription("Practice passed").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Review recommended").performScrollTo().performClick()
        composeRule.onNodeWithText("Next lesson title").performScrollTo().performClick()
        composeRule.onNodeWithText("View subject topics").performScrollTo().performClick()
        composeRule.onNodeWithText("Close lesson").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals("flow-test-next-lesson", openedNextLessonId)
            assertEquals(2, backCount)
            assertEquals(0 to 1, finishedResult)
            assertTrue("Completion must be separate from quiz accuracy", "flow-test-lesson" in repository.completedLessonIds())
            assertFalse("A lesson without quiz questions must stay incomplete", "flow-test-empty-lesson" in repository.completedLessonIds())
            assertEquals(0, repository.topicProgress(topic.id)?.lastScorePercent ?: -1)
            repository.clearProgress()
        }
    }

    @Test
    fun passingHighScoreQuizShowsTrophyWithoutReviewButton() {
        val repository = isolatedRepository("quiz-pass")
        repository.clearProgress()
        val question = QuizQuestion(
            prompt = "Which option is correct?",
            options = listOf("Wrong answer", "Correct answer"),
            correctOptionIndex = 1,
            id = "flow-test-pass-question",
            explanation = "The second option is correct.",
        )
        val lesson = ContentLesson(
            id = "flow-test-pass-lesson",
            title = "Test lesson",
            readTimeMinutes = 1,
            sections = emptyList(),
            keyTerms = emptyList(),
            questions = listOf(question),
        )
        val topic = ContentTopic("flow-test-pass-topic", "Test topic", "", listOf(lesson))
        val pack = ContentPack("flow-test-pass-pack", 3, "Biology", "en", 1, "provisional", listOf(topic))

        composeRule.setContent {
            MaterialTheme {
                QuizScreen(
                    pack = pack,
                    topic = topic,
                    repository = repository,
                    onBack = {},
                    onFinished = { _, _ -> },
                    onOpenNextLesson = {},
                    nextLesson = null,
                    onProgressChanged = {},
                    cachedLessonCount = 1,
                    totalLessonCount = 1,
                )
            }
        }

        composeRule.onNodeWithText("Correct answer").performClick()
        composeRule.onNodeWithText("Check answer").performClick()
        composeRule.onNodeWithText("See results").performClick()
        composeRule.onNodeWithText("100%").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Practice passed").assertIsDisplayed()
        composeRule.onNodeWithText("Strong work! You've got this topic down.").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Review recommended").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("View subject topics").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Close lesson").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sixtyPercentQuizShowsGoodStartAndReviewRecommendationWithoutTrophy() {
        val repository = isolatedRepository("quiz-good-start")
        repository.clearProgress()
        val questions = (1..5).map { questionNumber ->
            QuizQuestion(
                prompt = "Question $questionNumber",
                options = listOf("Wrong option", "Correct option"),
                correctOptionIndex = 1,
                id = "flow-test-good-start-question-$questionNumber",
                explanation = "Correct option is the right answer.",
            )
        }
        val lesson = ContentLesson(
            id = "flow-test-good-start-lesson",
            title = "Test lesson",
            readTimeMinutes = 1,
            sections = emptyList(),
            keyTerms = emptyList(),
            questions = questions,
        )
        val topic = ContentTopic("flow-test-good-start-topic", "Test topic", "", listOf(lesson))
        val pack = ContentPack("flow-test-good-start-pack", 3, "Biology", "en", 1, "provisional", listOf(topic))

        composeRule.setContent {
            MaterialTheme {
                QuizScreen(
                    pack = pack,
                    topic = topic,
                    repository = repository,
                    onBack = {},
                    onFinished = { _, _ -> },
                    onOpenNextLesson = {},
                    nextLesson = null,
                    onProgressChanged = {},
                    cachedLessonCount = 1,
                    totalLessonCount = 1,
                )
            }
        }

        repeat(5) { questionNumber ->
            val answer = if (questionNumber < 3) "Correct option" else "Wrong option"
            composeRule.onNodeWithText(answer).performClick()
            composeRule.onNodeWithText("Check answer").performClick()
            composeRule.onNodeWithText(if (questionNumber == 4) "See results" else "Next question").performClick()
        }

        composeRule.onNodeWithText("60%").assertIsDisplayed()
        composeRule.onNodeWithText("Good start! Keep practising to lock it in.").assertIsDisplayed()
        composeRule.onNodeWithText("Review recommended").performScrollTo().assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithContentDescription("Practice passed").fetchSemanticsNodes().size)
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
