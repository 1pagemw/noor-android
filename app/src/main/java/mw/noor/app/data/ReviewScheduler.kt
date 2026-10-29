package mw.noor.app.data

object ReviewScheduler {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun nextReviewAt(score: Float, successfulStreak: Int, nowMillis: Long): Long {
        val delayDays = when {
            score < 0.6f -> 0
            score < 0.8f -> 1
            successfulStreak <= 1 -> 1
            successfulStreak == 2 -> 3
            else -> 7
        }
        return nowMillis + delayDays * DAY_MILLIS
    }
}
