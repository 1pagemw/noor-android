package mw.noor.app.data

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoorRepositoryProgressTest {
    @Test
    fun offlineCompletionProvenanceSurvivesReopenAndClearsWithProgress() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("noor_local_v1", Context.MODE_PRIVATE)
        val original = preferences.all.mapValues { (_, value) ->
            if (value is Set<*>) value.filterIsInstance<String>().toSet() else value
        }
        val lessonId = "offline_completion_regression_test"

        try {
            val repository = NoorRepository(context)
            repository.markLessonCompleted(lessonId, completedOffline = true)

            assertTrue(lessonId in repository.completedLessonIds())
            assertTrue(repository.wasLessonCompletedOffline(lessonId))
            assertTrue(NoorRepository(context).wasLessonCompletedOffline(lessonId))

            repository.markLessonCompleted(lessonId, completedOffline = false)
            assertFalse(repository.wasLessonCompletedOffline(lessonId))

            repository.markLessonCompleted(lessonId, completedOffline = true)
            repository.clearProgress()
            assertFalse(repository.wasLessonCompletedOffline(lessonId))
        } finally {
            val editor = preferences.edit().clear()
            original.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toMutableSet())
                }
            }
            editor.commit()
        }
    }
}
