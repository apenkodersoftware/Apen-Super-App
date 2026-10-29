package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.fasmovapos.app.ui.components.NAV_TABS
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class SimpleNavigationTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun fourVisibleDestinationsNavigateWithoutOpeningAMenu() {
    var selected = "POS"
    composeRule.setContent { FasMovaPOSTheme { SimpleNavigation("POS", onTabSelect = { selected = it }) } }
    listOf("Sell", "Stock", "Summary", "More").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    composeRule.onNodeWithTag("simple_nav_today").performClick()
    assertEquals("TODAY", selected)
    composeRule.onNodeWithText("Staff").assertDoesNotExist()
    assertEquals(4, SIMPLE_TABS.size)
    assertTrue(NAV_TABS.any { it.id == "STAFF" })
  }

  @Test
  fun moreOpensBusinessSummaryWithoutStaff() {
    var selected = ""
    composeRule.setContent { FasMovaPOSTheme { SimpleMoreMenu(onNavigate = { selected = it }) } }
    composeRule.onNodeWithText("Staff").assertDoesNotExist()
    composeRule.onNodeWithText("Business summary").performClick()
    assertEquals("REPORTS", selected)
  }
}
