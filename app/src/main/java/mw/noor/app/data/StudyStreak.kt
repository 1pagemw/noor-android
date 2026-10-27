package mw.noor.app.data

data class StudyStreak(
    val days: Int,
    val studiedToday: Boolean,
)

internal fun currentStudyStreak(
    lastActivityDay: String?,
    storedDays: Int,
    today: String,
    yesterday: String,
): StudyStreak {
    val isActive = lastActivityDay == today || lastActivityDay == yesterday
    return StudyStreak(
        days = if (isActive) storedDays.coerceAtLeast(0) else 0,
        studiedToday = lastActivityDay == today,
    )
}

internal fun nextStudyStreak(
    lastActivityDay: String?,
    storedDays: Int,
    today: String,
    yesterday: String,
): StudyStreak = when (lastActivityDay) {
    today -> StudyStreak(storedDays.coerceAtLeast(1), studiedToday = true)
    yesterday -> StudyStreak(storedDays.coerceAtLeast(0) + 1, studiedToday = true)
    else -> StudyStreak(days = 1, studiedToday = true)
}
