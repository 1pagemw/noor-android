package mw.noor.app

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
}
