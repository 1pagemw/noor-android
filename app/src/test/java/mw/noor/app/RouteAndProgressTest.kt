package mw.noor.app

import mw.noor.app.ui.RouteHistory
import mw.noor.app.ui.countLabel
import mw.noor.app.ui.normalizedProgressFraction
import mw.noor.app.ui.topicProgressFractions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RouteAndProgressTest {
    @Test
    fun backReturnsThroughTheVisitedScreens() {
        var state = RouteHistory.initial()
            .navigateTo("subject")
            .navigateTo("lesson")
            .navigateTo("quiz")

        state = state.navigateBack()
        assertEquals("lesson", state.currentRoute)
        state = state.navigateBack()
        assertEquals("subject", state.currentRoute)
        state = state.navigateBack()
        assertEquals("tabs", state.currentRoute)
    }

    @Test
    fun routeHistorySurvivesStateSerialization() {
        val original = RouteHistory.initial().navigateTo("subject").navigateTo("lesson")
        val restored = RouteHistory.decode(original.encode())

        assertEquals("lesson", restored.currentRoute)
        assertEquals("subject", restored.navigateBack().currentRoute)
    }

    @Test
    fun zeroIntermediateAndFullProgressAreExact() {
        assertEquals(0f, normalizedProgressFraction(0f), 0f)
        assertEquals(0.5f, normalizedProgressFraction(0.5f), 0f)
        assertEquals(1f, normalizedProgressFraction(1f), 0f)
    }

    @Test
    fun invalidAndOutOfRangeProgressCannotRenderAsFull() {
        assertEquals(0f, normalizedProgressFraction(Float.NaN), 0f)
        assertEquals(0f, normalizedProgressFraction(Float.POSITIVE_INFINITY), 0f)
        assertEquals(0f, normalizedProgressFraction(-0.3f), 0f)
        assertEquals(1f, normalizedProgressFraction(1.4f), 0f)
        assertFalse(normalizedProgressFraction(0f) > 0f)
        assertEquals(1f, normalizedProgressFraction(1f), 0f)
    }

    @Test
    fun completedLessonAndZeroQuizScoreStayIndependent() {
        val fractions = topicProgressFractions(completedLessons = 1, totalLessons = 1, latestScorePercent = 0)

        assertEquals(1f, fractions.lessonCompletion, 0f)
        assertEquals(0f, fractions.latestQuizAccuracy!!, 0f)
    }

    @Test
    fun topicProgressUsesRealIntermediateAndFullScores() {
        assertEquals(0.5f, topicProgressFractions(0, 1, 50).latestQuizAccuracy!!, 0f)
        assertEquals(1f, topicProgressFractions(1, 1, 100).latestQuizAccuracy!!, 0f)
        assertNull(topicProgressFractions(0, 1, null).latestQuizAccuracy)
    }

    @Test
    fun countLabelsUseCorrectSingularAndPluralForms() {
        assertEquals("1 topic", countLabel(1, "topic"))
        assertEquals("2 topics", countLabel(2, "topic"))
        assertEquals("1 practice session", countLabel(1, "practice session"))
        assertEquals("0 practice sessions", countLabel(0, "practice session"))
    }
}
