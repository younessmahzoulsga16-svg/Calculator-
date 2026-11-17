package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NovaCalc", appName)
    }

    @Test
    fun `scientific calculation flow and mode switching`() {
        composeRule.waitForIdle()

        // Tap 7 + 8 =
        composeRule.onNodeWithTag("key_7").performClick()
        composeRule.onNodeWithTag("key_add").performClick()
        composeRule.onNodeWithTag("key_8").performClick()
        composeRule.onNodeWithTag("key_equals").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("result_display").assertTextEquals("15")

        // Switch to Equations tab and solve quadratic
        composeRule.onNodeWithTag("tab_equations").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("solve_equation_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("equation_solution_card").assertExists()

        // Switch to Bases tab
        composeRule.onNodeWithTag("tab_bases").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("base_row_hex").assertExists()
    }
}
