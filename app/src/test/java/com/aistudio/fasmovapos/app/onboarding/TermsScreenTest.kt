package com.aistudio.fasmovapos.app.onboarding

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.aistudio.fasmovapos.app.ui.simple.SimpleModeTheme
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "sw-rKE-w360dp-h800dp")
class TermsScreenTest {
  @get:Rule val rule = createComposeRule()

  @Test
  fun updatedTermsRequireAnExplicitCheckboxAndContinueAction() {
    var accepted = 0
    rule.setContent { FasMovaPOSTheme { SimpleModeTheme { TermsScreen(onAccept = { accepted++ }) } } }
    rule.onNodeWithTag("terms_continue").assertIsNotEnabled()
    rule.onNodeWithTag("terms_checkbox").performClick()
    assertEquals(0, accepted)
    rule.onNodeWithTag("terms_continue").assertIsEnabled().performClick()
    assertEquals(1, accepted)
  }

  @Test
  fun consentControlsRemainVisibleAt360dpAndLargeText() {
    rule.setContent {
      FasMovaPOSTheme {
        SimpleModeTheme {
          val density = LocalDensity.current
          CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
            TermsScreen(onAccept = {})
          }
        }
      }
    }
    rule.onNodeWithTag("terms_checkbox").assertIsDisplayed()
    rule.onNodeWithTag("terms_continue").assertIsDisplayed().assertIsNotEnabled()
    rule.onNodeWithText("EN").assertIsDisplayed()
    rule.onNodeWithText("SW").assertIsDisplayed()
  }

  @Test
  fun readOnlyReviewCanCloseWithoutRecordingAnotherAcceptance() {
    var closed = false
    rule.setContent { FasMovaPOSTheme { SimpleModeTheme { TermsScreen(onAccept = { closed = true }, reviewOnly = true) } } }
    rule.onNodeWithTag("terms_checkbox").assertDoesNotExist()
    rule.onNodeWithTag("terms_continue").assertIsEnabled().performClick()
    assertEquals(true, closed)
  }
}
