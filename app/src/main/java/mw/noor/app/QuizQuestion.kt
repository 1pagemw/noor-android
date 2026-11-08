package mw.noor.app

import kotlin.random.Random

data class QuizQuestion(
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val id: String = "",
    val explanation: String = "",
) {
    fun isCorrect(selectedOptionIndex: Int): Boolean = selectedOptionIndex == correctOptionIndex
}

/**
 * A quiz question with its options presented in a shuffled order for one
 * practice session. [correctOptionIndex] is remapped so it always points at
 * the same option text as the original question.
 */
data class ShuffledQuizQuestion(
    val question: QuizQuestion,
    val options: List<String>,
    val correctOptionIndex: Int,
) {
    fun isCorrect(selectedOptionIndex: Int): Boolean = selectedOptionIndex == correctOptionIndex
}

/**
 * Deterministically shuffles the order of [questions] and of each question's
 * options for one practice session. The same [seed] always produces the same
 * order, so a session restored after process death shows the same questions
 * and options in the same positions, while a fresh session draws a new seed
 * and a new order. Repeated practice therefore cannot be memorised by
 * position, which keeps practice scores a meaningful signal for the review
 * scheduler.
 */
fun shuffledQuizQuestions(questions: List<QuizQuestion>, seed: Int): List<ShuffledQuizQuestion> {
    if (questions.isEmpty()) return emptyList()
    val questionOrder = questions.indices.shuffled(Random(seed))
    return questionOrder.map { originalIndex ->
        val question = questions[originalIndex]
        val optionOrder = question.options.indices.shuffled(Random(seed * 31 + originalIndex))
        ShuffledQuizQuestion(
            question = question,
            options = optionOrder.map { question.options[it] },
            correctOptionIndex = optionOrder.indexOf(question.correctOptionIndex),
        )
    }
}
