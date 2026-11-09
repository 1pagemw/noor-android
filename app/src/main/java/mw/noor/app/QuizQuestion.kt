package mw.noor.app

import kotlin.random.Random

const val QUIZ_SESSION_QUESTION_COUNT = 5

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
 * Deterministically selects up to [questionLimit] questions without replacement,
 * shuffles their order and shuffles each question's options for one practice
 * session. If fewer questions are available, all are used. The same [seed]
 * always reproduces the same selection and order, so a session restored after
 * process death shows the same questions and options in the same positions.
 */
fun shuffledQuizQuestions(
    questions: List<QuizQuestion>,
    seed: Int,
    questionLimit: Int = QUIZ_SESSION_QUESTION_COUNT,
): List<ShuffledQuizQuestion> {
    if (questions.isEmpty()) return emptyList()
    val selectedCount = questionLimit.coerceIn(0, questions.size)
    val questionOrder = questions.indices.shuffled(Random(seed)).take(selectedCount)
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
