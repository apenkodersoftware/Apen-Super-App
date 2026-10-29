package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.aistudio.fasmovapos.app.data.security.PinProblem
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SetupScreenTest {
  @get:Rule val composeRule = createComposeRule()

  private fun type(
    tag: String,
    text: String,
  ) = composeRule.onNodeWithTag(tag).performScrollTo().performTextInput(text)

  @Test
  fun startStaysDisabledUntilEverythingIsValid_andSubmitsWhatWasTyped() {
    var submitted: List<Any>? = null
    composeRule.setContent {
      FasMovaPOSTheme {
        SetupScreen(isLoading = false, onComplete = { b, o, u, p, s, phone, kra -> submitted = listOf(b, o, u, p, s, phone, kra) })
      }
    }
    composeRule.onNodeWithTag("setup_sample_checkbox").assertDoesNotExist()
    val start = composeRule.onNodeWithTag("setup_start_button")

    start.performScrollTo().assertIsNotEnabled()
    type("setup_business_field", "Mama Njeri Shop")
    type("setup_phone_field", "0712345678") // optional
    type("setup_kra_field", "a001234567z") // optional, stored upper-case
    type("setup_owner_name_field", "Njeri Wanjiku")
    type("setup_pin_field", "4821")
    start.performScrollTo().assertIsNotEnabled() // PIN not confirmed yet
    type("setup_confirm_pin_field", "4821")
    start.performScrollTo().assertIsEnabled().performClick()

    assertEquals(listOf<Any>("Mama Njeri Shop", "Njeri Wanjiku", "owner", "4821", false, "0712345678", "A001234567Z"), submitted)
  }

  @Test
  fun theDefaultPin_isRefusedWithAnExplanation() {
    composeRule.setContent { FasMovaPOSTheme { SetupScreen(isLoading = false, onComplete = { _, _, _, _, _, _, _ -> }) } }
    type("setup_business_field", "Shop")
    type("setup_owner_name_field", "Owner")

    type("setup_pin_field", "1234")
    type("setup_confirm_pin_field", "1234")

    composeRule.onNodeWithText("1234 is too easy to guess. Choose another PIN").assertExists()
    composeRule.onNodeWithTag("setup_start_button").performScrollTo().assertIsNotEnabled()
  }

  @Test
  fun businessNameSurvivesActivityRecreation() {
    val tester = StateRestorationTester(composeRule)
    tester.setContent { FasMovaPOSTheme { SetupScreen(isLoading = false, onComplete = { _, _, _, _, _, _, _ -> }) } }

    type("setup_business_field", "Mama Njeri Shop")
    tester.emulateSavedInstanceStateRestore()

    composeRule.onNodeWithTag("setup_business_field").assertTextContains("Mama Njeri Shop")
  }

  @Test
  fun changePinScreen_requiresMatchingValidPin() {
    composeRule.setContent {
      FasMovaPOSTheme { ChangePinScreen(isSaving = false, problem = null, onSave = {}, onSignOut = {}) }
    }
    val save = composeRule.onNodeWithTag("change_pin_save_button")

    save.performScrollTo().assertIsNotEnabled()
    type("change_pin_field", "7305")
    type("change_pin_confirm_field", "7306")
    save.performScrollTo().assertIsNotEnabled()
  }

  @Test
  fun theRefusalOfALastAttempt_disappearsAsSoonAsThePinIsEdited() {
    composeRule.setContent {
      FasMovaPOSTheme { ChangePinScreen(isSaving = false, problem = PinProblem.IN_USE, onSave = {}, onSignOut = {}) }
    }
    val inUse = "Someone else already uses this PIN. Choose a different one"
    composeRule.onNodeWithText(inUse).assertDoesNotExist() // nothing has been tried yet

    type("change_pin_field", "4821")
    type("change_pin_confirm_field", "4821")
    composeRule.onNodeWithTag("change_pin_save_button").performScrollTo().performClick()
    composeRule.onNodeWithText(inUse).assertExists()

    composeRule.onNodeWithTag("change_pin_field").performScrollTo().performTextClearance()
    composeRule.onNodeWithText(inUse).assertDoesNotExist()
  }

  @Test fun freshInstallCanReachRecoveryWithoutCreatingABusiness() {
    composeRule.setContent { FasMovaPOSTheme { SetupScreen(isLoading = false, onComplete = { _, _, _, _, _, _, _ -> }) } }
    composeRule.onNodeWithTag("setup_recover_backup").performScrollTo().performClick()
    composeRule.onNodeWithText("Choose encrypted backup…").assertExists()
  }
}
