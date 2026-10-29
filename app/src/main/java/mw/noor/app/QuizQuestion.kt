package mw.noor.app

data class QuizQuestion(
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val id: String = "",
    val explanation: String = "",
) {
    fun isCorrect(selectedOptionIndex: Int): Boolean = selectedOptionIndex == correctOptionIndex
}
