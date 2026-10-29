package mw.noor.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mw.noor.app.MainActivity
import mw.noor.app.data.LessonContentJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPrivacyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun lessonCatalogIsMetadataForAllIndividualUnits() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val index = context.assets.open("lesson_catalog.json").bufferedReader().use { reader ->
            LessonContentJsonParser.parseIndex(reader.readText())
        }

        assertEquals(13, index.lessons.size)
        assertEquals(setOf("form3_biology", "form3_physics"), index.subjects.map { it.id }.toSet())
        assertEquals(index.lessons.size, index.lessons.map { it.lessonId }.toSet().size)
        assertTrue(index.lessons.all { it.sizeBytes > 0L && it.sha256.matches(Regex("[a-f0-9]{64}")) })
    }

    @Test
    fun settingsExplainLocalProgressStorageAndDeletion() {
        assertTrue(composeRule.onAllNodesWithText("Draft · teacher review").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Get started").performClick()

        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText("Learn").assertIsDisplayed()
        composeRule.onNodeWithText("Review").assertIsDisplayed()
        composeRule.onNodeWithText("Learn").performClick()
        assertTrue(composeRule.onAllNodesWithText("Draft content").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("Lessons are awaiting local teacher review.").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Biology").performClick()
        assertTrue(composeRule.onAllNodesWithText("Draft — local teacher review needed").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Today").performClick()
        composeRule.onNodeWithContentDescription("Study streak: 0 days").performClick()
        composeRule.onNodeWithText("Study streak").assertIsDisplayed()
        composeRule.onNodeWithText("Got it").performClick()

        composeRule.onNodeWithText("Review").performClick()
        composeRule.onNodeWithText("Your review list is empty").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Nothing due", substring = true).fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Today").performClick()

        composeRule.onNodeWithContentDescription("Profile and settings").performClick()
        composeRule.onNodeWithText(
            "Progress stays on this phone; it is not synced and is deleted if the app is uninstalled or its data is cleared.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("lessons complete").assertIsDisplayed()
        composeRule.onNodeWithText("No practice yet").assertIsDisplayed()
        composeRule.onNodeWithText("practice sessions").assertIsDisplayed()
        composeRule.onNodeWithText("topics due").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Text size control").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Clear learning progress").performScrollTo().performClick()
        composeRule.onNodeWithText(
            "This cannot be undone. Your lesson and practice progress, study streak, and Continue Learning position will be deleted. Saved lessons will stay.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
    }
}
