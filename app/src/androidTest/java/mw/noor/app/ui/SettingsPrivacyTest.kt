package mw.noor.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mw.noor.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPrivacyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsExplainLocalProgressStorageAndDeletion() {
        composeRule.onNodeWithText("Start learning").performClick()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText(
            "Progress stays on this phone; it is not synced and is deleted if the app is uninstalled or its data is cleared.",
        ).assertIsDisplayed()
    }
}
