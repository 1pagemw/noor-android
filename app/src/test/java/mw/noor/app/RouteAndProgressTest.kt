package mw.noor.app

import mw.noor.app.ui.RouteHistory
import mw.noor.app.ui.countLabel
import mw.noor.app.ui.normalizedProgressFraction
import mw.noor.app.ui.lessonCompletionFraction
import mw.noor.app.ui.topicProgressFractions
import mw.noor.app.data.currentStudyStreak
import mw.noor.app.data.nextStudyStreak
import androidx.work.WorkInfo
import mw.noor.app.ui.lessonAvailabilityLabel
import mw.noor.app.ui.shouldAutomaticallyStartDownload
import mw.noor.app.ui.shouldUseNavigationRail
import mw.noor.app.ui.responsiveGridColumnCount
import mw.noor.app.ui.visibleSearchResults
import mw.noor.app.ui.nextIncompleteLessonId
import mw.noor.app.ui.previousLessonId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteAndProgressTest {
    @Test
    fun navigationRailStartsAtCompactMediumWidthBreakpoint() {
        assertFalse(shouldUseNavigationRail(599f))
        assertTrue(shouldUseNavigationRail(600f))
        assertTrue(shouldUseNavigationRail(840f))
    }

    @Test
    fun responsiveGridAddsColumnsOnlyWhenMinimumCellsAndGapFit() {
        assertEquals(1, responsiveGridColumnCount(691f, minimumCellWidthDp = 340f, spacingDp = 12f))
        assertEquals(2, responsiveGridColumnCount(692f, minimumCellWidthDp = 340f, spacingDp = 12f))
        assertEquals(2, responsiveGridColumnCount(1200f, minimumCellWidthDp = 340f, spacingDp = 12f))
        assertEquals(1, responsiveGridColumnCount(900f, minimumCellWidthDp = 340f, spacingDp = 12f, maxColumns = 1))
    }

    @Test
    fun automaticDownloadStartsOnlyWhenWorkIsMissingOrCancelled() {
        assertTrue(shouldAutomaticallyStartDownload(null))
        assertTrue(shouldAutomaticallyStartDownload(WorkInfo.State.CANCELLED))
        assertFalse(shouldAutomaticallyStartDownload(WorkInfo.State.ENQUEUED))
        assertFalse(shouldAutomaticallyStartDownload(WorkInfo.State.RUNNING))
        assertFalse(shouldAutomaticallyStartDownload(WorkInfo.State.SUCCEEDED))
        assertFalse(shouldAutomaticallyStartDownload(WorkInfo.State.FAILED))
        assertFalse(shouldAutomaticallyStartDownload(WorkInfo.State.BLOCKED))
    }

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
    fun nextIncompleteLessonFollowsCatalogueOrderWithoutWrapping() {
        val order = listOf("a", "b", "c", "d")

        assertEquals("b", nextIncompleteLessonId(order, "a", setOf("a")))
        assertEquals("d", nextIncompleteLessonId(order, "b", setOf("a", "b", "c")))
        assertNull(nextIncompleteLessonId(order, "d", setOf("a", "b", "c", "d")))
        assertEquals("a", nextIncompleteLessonId(order, null, emptySet()))
    }

    @Test
    fun previousLessonFollowsCatalogueOrderWithoutWrapping() {
        val order = listOf("a", "b", "c", "d")

        assertEquals("b", previousLessonId(order, "c"))
        assertNull(previousLessonId(order, "a"))
        assertNull(previousLessonId(order, "unknown"))
        assertNull(previousLessonId(order, null))
    }

    @Test
    fun openingNextLessonReplacesQuizResultsAndBackReturnsToLearningContext() {
        val state = RouteHistory.initial()
            .navigateTo("subject")
            .navigateTo("lesson")
            .navigateTo("quiz")
            .navigateBack()
            .navigateTo("lesson")

        assertEquals("lesson", state.currentRoute)
        assertEquals("subject", state.navigateBack().currentRoute)
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
    fun topicCardShadingTracksLessonCompletionOnly() {
        assertEquals(0f, lessonCompletionFraction(0, 2), 0f)
        assertEquals(0.5f, lessonCompletionFraction(1, 2), 0f)
        assertEquals(1f, lessonCompletionFraction(2, 2), 0f)
        assertEquals(0f, lessonCompletionFraction(0, 0), 0f)
    }

    @Test
    fun countLabelsUseCorrectSingularAndPluralForms() {
        assertEquals("1 topic", countLabel(1, "topic"))
        assertEquals("2 topics", countLabel(2, "topic"))
        assertEquals("1 practice session", countLabel(1, "practice session"))
        assertEquals("0 practice sessions", countLabel(0, "practice session"))
    }

    @Test
    fun lessonAvailabilityUsesConsistentCachedAndUncachedLabels() {
        assertEquals("Saved offline", lessonAvailabilityLabel(true))
        assertEquals("Downloads when opened", lessonAvailabilityLabel(false))
    }

    @Test
    fun searchResultsCanExpandPastInitialLimit() {
        val matches = (1..8).toList()

        assertEquals(matches.take(6), visibleSearchResults(matches, showAll = false))
        assertEquals(matches, visibleSearchResults(matches, showAll = true))
    }

    @Test
    fun streakCountsAtMostOneActivityPerDay() {
        val streak = nextStudyStreak("2026-10-27", 4, "2026-10-27", "2026-10-26")

        assertEquals(4, streak.days)
        assertEquals(true, streak.studiedToday)
    }

    @Test
    fun streakExtendsOnConsecutiveLocalDays() {
        val streak = nextStudyStreak("2026-10-26", 4, "2026-10-27", "2026-10-26")

        assertEquals(5, streak.days)
        assertEquals(true, streak.studiedToday)
    }

    @Test
    fun streakRestartsAfterMissedDayAndExpiredStreakIsHidden() {
        val restarted = nextStudyStreak("2026-10-25", 4, "2026-10-27", "2026-10-26")
        val inactive = currentStudyStreak("2026-10-25", 4, "2026-10-27", "2026-10-26")

        assertEquals(1, restarted.days)
        assertEquals(0, inactive.days)
        assertEquals(false, inactive.studiedToday)
    }
}
