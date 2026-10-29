package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity
import com.aistudio.fasmovapos.app.ui.components.DisplayControl
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import com.aistudio.fasmovapos.app.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class MoreScreenTest {
  @get:Rule val rule = createComposeRule()
  private val shop = BranchEntity("b-1", "Kazi Mob", "HQ", "", true, "0712345678", "A001234567Z")

  @Test
  fun changingShopNamePreservesContactDetailsThatAreHidden() {
    var saved: List<String>? = null
    rule.setContent {
      FasMovaPOSTheme {
        MoreScreen(
          shop,
          DisplayControl(ThemeMode.SYSTEM, {}, AppLanguage.ENGLISH, {}),
          true,
          {},
          { n, p, t -> saved = listOf(n, p, t) },
          {},
        )
      }
    }
    rule.onNodeWithTag("simple_more_list").performScrollToNode(hasTestTag("simple_more_shop"))
    rule.onNodeWithTag("simple_more_shop").performClick()
    rule.onNodeWithText("Phone (optional)").assertDoesNotExist()
    rule.onNodeWithTag("simple_shop_name_field").performTextClearance()
    rule.onNodeWithTag("simple_shop_name_field").performTextInput("Kazi Shop")
    rule.onNodeWithTag("simple_save_shop").performClick()
    assertEquals(listOf("Kazi Shop", shop.phone, shop.taxPin), saved)
  }

  @Test
  fun languageHasClearlyNamedChoicesThatCallTheExistingPreferenceAction() {
    var selected: AppLanguage? = null
    rule.setContent {
      FasMovaPOSTheme {
        MoreScreen(shop, DisplayControl(ThemeMode.SYSTEM, {}, AppLanguage.ENGLISH, { selected = it }), true, {}, { _, _, _ -> }, {})
      }
    }
    rule.onNodeWithTag("simple_more_list").performScrollToNode(hasTestTag("simple_more_language"))
    rule.onNodeWithTag("simple_more_language").performClick()
    rule.onNodeWithText("Kiswahili").performClick()
    assertEquals(AppLanguage.SWAHILI, selected)
  }

  @Test
  fun logoutIsAvailableUnderMoreWithoutStaffOrUserSwitching() {
    var signedOut = false
    rule.setContent {
      FasMovaPOSTheme {
        MoreScreen(shop, DisplayControl(ThemeMode.SYSTEM, {}, AppLanguage.ENGLISH, {}), true, {}, { _, _, _ -> }, { signedOut = true })
      }
    }
    rule.onNodeWithText("Staff").assertDoesNotExist()
    rule.onNodeWithTag("simple_more_list").performScrollToNode(hasTestTag("simple_more_logout"))
    rule.onNodeWithTag("simple_more_logout").performClick()
    assertEquals(true, signedOut)
  }
}
