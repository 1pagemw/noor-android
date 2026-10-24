package mw.noor.app

import mw.noor.app.data.ReviewScheduler
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewSchedulerTest {
    private val now = 1_800_000_000_000L
    private val oneDay = 24L * 60L * 60L * 1000L

    @Test
    fun lowScoreIsDueImmediately() {
        assertEquals(now, ReviewScheduler.nextReviewAt(score = 0.4f, successfulStreak = 0, nowMillis = now))
    }

    @Test
    fun firstSuccessfulReviewIsDueTomorrow() {
        assertEquals(now + oneDay, ReviewScheduler.nextReviewAt(score = 0.9f, successfulStreak = 1, nowMillis = now))
    }

    @Test
    fun repeatedSuccessfulReviewsBecomeLessFrequent() {
        assertEquals(now + 3 * oneDay, ReviewScheduler.nextReviewAt(score = 0.9f, successfulStreak = 2, nowMillis = now))
        assertEquals(now + 7 * oneDay, ReviewScheduler.nextReviewAt(score = 0.9f, successfulStreak = 3, nowMillis = now))
    }
}
