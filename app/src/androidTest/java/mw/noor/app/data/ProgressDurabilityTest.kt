package mw.noor.app.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressDurabilityTest {
    @Test
    fun completionQuizAndStreakWritesAreVisibleAfterImmediateRepositoryReload() {
        val isolated = IsolatedNoorContext.create("progress-durability")
        try {
            val firstRepository = NoorRepository(isolated.context)
            val now = System.currentTimeMillis()

            firstRepository.markLessonCompleted("durability-lesson", completedOffline = true)
            firstRepository.recordQuizResult("durability-topic", correctAnswers = 2, questionCount = 5, nowMillis = now)
            firstRepository.recordStudyActivity(now)

            val reloadedRepository = NoorRepository(isolated.context)
            assertTrue("Completed lesson must be durably persisted", "durability-lesson" in reloadedRepository.completedLessonIds())
            assertTrue(reloadedRepository.wasLessonCompletedOffline("durability-lesson"))
            assertEquals(1, reloadedRepository.topicProgress("durability-topic")?.attempts)
            assertEquals(5, reloadedRepository.topicProgress("durability-topic")?.totalQuestions)
            assertEquals(2, reloadedRepository.topicProgress("durability-topic")?.correctAnswers)
            assertEquals(1, reloadedRepository.studyStreak(now).days)
        } finally {
            isolated.cleanup()
        }
    }

    @Test
    fun corruptPrimaryTopicStatsFallsBackToBackupAndBothCorruptReturnEmpty() {
        val isolated = IsolatedNoorContext.create("stats-backup")
        try {
            val repository = NoorRepository(isolated.context)
            val preferences = isolated.context.getSharedPreferences("noor_local_v1", Context.MODE_PRIVATE)
            repository.recordQuizResult("backup-topic", correctAnswers = 3, questionCount = 4)
            preferences.edit().putString("topic_stats", "not valid json").commit()

            val recovered = NoorRepository(isolated.context).topicProgress("backup-topic")
            assertEquals(1, recovered?.attempts)
            assertEquals(4, recovered?.totalQuestions)
            assertEquals(3, recovered?.correctAnswers)

            preferences.edit().putString("topic_stats_backup", "also not valid json").commit()
            assertNull("Both corrupt snapshots should safely yield empty stats", NoorRepository(isolated.context).topicProgress("backup-topic"))

            repository.recordQuizResult("clear-topic", correctAnswers = 1, questionCount = 1)
            repository.clearProgress()
            assertFalse("Clear progress must remove the backup snapshot", preferences.contains("topic_stats_backup"))
            assertNull(NoorRepository(isolated.context).topicProgress("clear-topic"))
        } finally {
            isolated.cleanup()
        }
    }
}
