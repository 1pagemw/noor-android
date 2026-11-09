package mw.noor.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizQuestionTest {
    private val question = QuizQuestion(
        prompt = "Which answer is correct?",
        options = listOf("A", "B", "C"),
        correctOptionIndex = 1,
    )

    @Test
    fun correctOptionIsAccepted() {
        assertTrue(question.isCorrect(1))
    }

    @Test
    fun differentOptionIsRejected() {
        assertFalse(question.isCorrect(0))
    }

    private val sessionQuestions = listOf(
        QuizQuestion(
            prompt = "First prompt",
            options = listOf("alpha", "beta", "gamma"),
            correctOptionIndex = 2,
        ),
        QuizQuestion(
            prompt = "Second prompt",
            options = listOf("one", "two"),
            correctOptionIndex = 0,
        ),
    )

    @Test
    fun shuffledQuestionsKeepAllQuestionsAndOptions() {
        val shuffled = shuffledQuizQuestions(sessionQuestions, seed = 7)
        assertEquals(sessionQuestions.size, shuffled.size)
        assertEquals(
            sessionQuestions.map { it.prompt }.toSet(),
            shuffled.map { it.question.prompt }.toSet(),
        )
        shuffled.forEach { item ->
            assertEquals(item.question.options.toSet(), item.options.toSet())
        }
    }

    @Test
    fun quizSessionSelectsFiveDistinctQuestionsWhenMoreAreAvailable() {
        val questionPool = (1..9).map { number ->
            QuizQuestion(
                prompt = "Question $number",
                options = listOf("A", "B", "C", "D"),
                correctOptionIndex = number % 4,
                id = "question-$number",
            )
        }

        val firstSession = shuffledQuizQuestions(questionPool, seed = 123)
        val restoredSession = shuffledQuizQuestions(questionPool, seed = 123)

        assertEquals(QUIZ_SESSION_QUESTION_COUNT, firstSession.size)
        assertEquals(QUIZ_SESSION_QUESTION_COUNT, firstSession.map { it.question.id }.toSet().size)
        assertTrue(firstSession.all { item -> questionPool.any { it.id == item.question.id } })
        assertEquals(firstSession, restoredSession)
    }

    @Test
    fun quizSessionUsesEveryAvailableQuestionWhenPoolHasFewerThanFive() {
        val shuffled = shuffledQuizQuestions(sessionQuestions, seed = 7)

        assertEquals(sessionQuestions.size, shuffled.size)
        assertEquals(sessionQuestions.map { it.prompt }.toSet(), shuffled.map { it.question.prompt }.toSet())
    }

    @Test
    fun nonPositiveQuestionLimitReturnsNoQuestions() {
        assertTrue(shuffledQuizQuestions(sessionQuestions, seed = 7, questionLimit = 0).isEmpty())
        assertTrue(shuffledQuizQuestions(sessionQuestions, seed = 7, questionLimit = -2).isEmpty())
    }

    @Test
    fun correctIndexStillPointsAtTheOriginalCorrectOption() {
        val shuffled = shuffledQuizQuestions(sessionQuestions, seed = 7)
        shuffled.forEach { item ->
            val originalCorrectOption = item.question.options[item.question.correctOptionIndex]
            assertEquals(originalCorrectOption, item.options[item.correctOptionIndex])
            assertTrue(item.isCorrect(item.correctOptionIndex))
            item.options.indices.filter { it != item.correctOptionIndex }.forEach { wrongIndex ->
                assertFalse(item.isCorrect(wrongIndex))
            }
        }
    }

    @Test
    fun sameSeedReproducesTheSameOrder() {
        assertEquals(
            shuffledQuizQuestions(sessionQuestions, seed = 7),
            shuffledQuizQuestions(sessionQuestions, seed = 7),
        )
    }

    @Test
    fun differentSeedsProduceDifferentOrdersSomewhere() {
        val orders = (0 until 20).map { seed -> shuffledQuizQuestions(sessionQuestions, seed) }.toSet()
        assertTrue("Expected at least two distinct orders across 20 seeds", orders.size > 1)
    }

    @Test
    fun emptyQuestionListShufflesToEmptyList() {
        assertTrue(shuffledQuizQuestions(emptyList(), seed = 1).isEmpty())
    }
}
