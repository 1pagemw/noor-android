package mw.noor.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        composeRule.onNodeWithText("Get started").performClick()

        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText("Learn").assertIsDisplayed()
        composeRule.onNodeWithText("Review").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Study streak: 0 days").performClick()
        composeRule.onNodeWithText("Study streak").assertIsDisplayed()
        composeRule.onNodeWithText("Got it").performClick()

        composeRule.onNodeWithContentDescription("Profile and settings").performClick()
        composeRule.onNodeWithText(
            "Progress stays on this phone; it is not synced and is deleted if the app is uninstalled or its data is cleared.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("lessons complete").assertIsDisplayed()
        composeRule.onNodeWithText("practice accuracy").assertIsDisplayed()
        composeRule.onNodeWithText("0 practice sessions").assertIsDisplayed()
        composeRule.onNodeWithText("0 topics due").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Text size control").performScrollTo().assertIsDisplayed()
    }
}
