package com.aistudio.fasmovapos.app.ui.components

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
class FasMovaPOSNavBarScreenshotTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun navBar_posSelected_screenshot() {
    composeTestRule.setContent {
      FasMovaPOSTheme {
        FasMovaPOSNavBar(currentTab = "POS", onTabSelected = {})
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/nav_bar_pos.png")
  }
}
