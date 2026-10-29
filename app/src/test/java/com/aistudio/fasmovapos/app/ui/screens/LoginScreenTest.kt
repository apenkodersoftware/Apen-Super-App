package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LoginScreenTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun signInEnabledOnlyWithUsernameAndFourDigitPin_andSubmitsEnteredValues() {
    var submitted: Pair<String, String>? = null
    composeRule.setContent {
      FasMovaPOSTheme { LoginScreen(isLoading = false, errorMessage = null, onSignIn = { u, p -> submitted = u to p }) }
    }

    composeRule.onNodeWithTag("login_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("login_username_field").performTextInput("admin")
    composeRule.onNodeWithTag("login_pin_field").performTextInput("12")
    composeRule.onNodeWithTag("login_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("login_pin_field").performTextInput("34")
    composeRule.onNodeWithTag("login_submit_button").assertIsEnabled().performClick()

    assertEquals("admin" to "1234", submitted)
  }

  @Test
  fun usernameSurvivesActivityRecreation() {
    val tester = StateRestorationTester(composeRule)
    tester.setContent { FasMovaPOSTheme { LoginScreen(isLoading = false, errorMessage = null, onSignIn = { _, _ -> }) } }

    composeRule.onNodeWithTag("login_username_field").performTextInput("admin")
    tester.emulateSavedInstanceStateRestore()

    composeRule.onNodeWithTag("login_username_field").assertTextContains("admin")
  }
}
