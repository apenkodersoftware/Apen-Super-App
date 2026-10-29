package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class LoginScreenScreenshotTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun loginScreen_default_screenshot() {
    composeTestRule.setContent {
      FasMovaPOSTheme {
        LoginScreen(isLoading = false, errorMessage = null, onSignIn = { _, _ -> })
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/login_screen.png")
  }

  @Test
  fun loginScreen_error_screenshot() {
    composeTestRule.setContent {
      FasMovaPOSTheme {
        LoginScreen(isLoading = false, errorMessage = "Invalid PIN", onSignIn = { _, _ -> })
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/login_screen_error.png")
  }
}
