package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.repository.CartItem
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class LooseQuantityUiTest {
  @get:Rule val rule = createComposeRule()
  private val beans = ProductEntity("beans", "B", "", "Beans", "Food", "Kg", 100.0, 160.0, 10_000, 1000, "b-1", quantityScale = 1000)

  @Test
  fun customMixedQuantityShowsCanonicalTotalAndAddsMeasuredLine() {
    var selected: CartItem? = null
    rule.setContent { FasMovaPOSTheme { QuantityDialog(beans, 0, {}, { selected = it }) } }
    rule.onNodeWithTag("lite_quantity_input").performTextReplacement("1.75")
    rule.onNodeWithTag("lite_quantity_quote").assertExists()
    rule.onNodeWithText("1.75 Kg — KES 280").assertExists()
    rule.onNodeWithTag("lite_add_quantity").assertIsEnabled().performClick()
    assertNotNull(selected)
    assertEquals(1750, selected!!.quantity)
    assertEquals(280.0, selected!!.lineTotal, 0.0)
  }

  @Test
  fun heldStockAndInvalidPrecisionPreventAnotherSale() {
    rule.setContent { FasMovaPOSTheme { QuantityDialog(beans, 9900, {}, {}) } }
    rule.onNodeWithTag("lite_add_quantity").assertIsNotEnabled()
    rule.onNodeWithTag("lite_quantity_input").performTextReplacement("0.05")
    rule.onNodeWithTag("lite_add_quantity").assertIsEnabled()
    rule.onNodeWithTag("lite_quantity_input").performTextReplacement("0.0001")
    rule.onNodeWithTag("lite_add_quantity").assertIsNotEnabled()
  }

  @Test
  fun presetHonoursSavedPortionPriceAndCustomEntryReturnsToNormalRate() {
    rule.setContent { FasMovaPOSTheme { QuantityDialog(beans.copy(quarterPrice = 45.0), 0, {}, {}) } }
    rule.onNodeWithTag("lite_portion_250").performScrollTo().performClick()
    rule.onNodeWithText("250 g — KES 45").assertExists()
    rule.onNodeWithTag("lite_quantity_input").performTextClearance()
    rule.onNodeWithTag("lite_quantity_input").performTextReplacement("0.25")
    rule.onNodeWithText("250 g — KES 40").assertExists()
  }
}
