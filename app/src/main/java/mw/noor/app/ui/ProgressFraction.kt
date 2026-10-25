package mw.noor.app.ui

/** Keeps determinate progress values finite and inside the 0..1 range. */
internal fun normalizedProgressFraction(value: Float): Float =
    if (value.isNaN() || value.isInfinite()) 0f else value.coerceIn(0f, 1f)

internal data class TopicProgressFractions(
    val lessonCompletion: Float,
    val latestQuizAccuracy: Float?,
)

internal fun topicProgressFractions(
    completedLessons: Int,
    totalLessons: Int,
    latestScorePercent: Int?,
): TopicProgressFractions = TopicProgressFractions(
    lessonCompletion = if (totalLessons <= 0) 0f else normalizedProgressFraction(completedLessons.toFloat() / totalLessons),
    latestQuizAccuracy = latestScorePercent?.let { normalizedProgressFraction(it / 100f) },
)
