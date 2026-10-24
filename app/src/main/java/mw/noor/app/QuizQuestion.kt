package mw.noor.app

data class QuizQuestion(
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int,
) {
    fun isCorrect(selectedOptionIndex: Int): Boolean = selectedOptionIndex == correctOptionIndex
}
