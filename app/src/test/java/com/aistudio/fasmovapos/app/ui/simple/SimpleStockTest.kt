package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class SimpleStockTest {
  @get:Rule val rule = createComposeRule()

  @Test
  fun addItemNeedsOnlyNamePriceAndQuantity() {
    var saved: StockItemInput? = null
    rule.setContent { FasMovaPOSTheme { SimpleStockScreen(StockState(emptyList()), { saved = it }, { _, _, _, _ -> }) } }
    rule.onNodeWithTag("simple_add_item").performClick()
    rule.onNodeWithTag("simple_item_sku").assertDoesNotExist()
    rule.onNodeWithTag("simple_item_cost").assertDoesNotExist()
    rule.onNodeWithTag("simple_item_name").performTextInput("Bucket")
    rule.onNodeWithTag("simple_item_price").performTextInput("350")
    rule.onNodeWithTag("simple_item_quantity").performTextClearance()
    rule.onNodeWithTag("simple_item_quantity").performTextInput("12")
    rule.onNodeWithTag("simple_save_item").assertIsEnabled().performClick()
    assertEquals("Bucket", saved!!.name)
    assertEquals(350.0, saved!!.price, 0.0)
    assertEquals(12, saved!!.quantity)
    assertEquals(0.0, saved!!.cost, 0.0)
  }

  @Test
  fun editingOnlyPriceDoesNotAddStockOrOverwriteHiddenCost() {
    var adjustment: List<Any?>? = null
    val item = simpleProduct("bucket", "Bucket").copy(costPrice = 60.0)
    rule.setContent {
      FasMovaPOSTheme {
        SimpleStockScreen(StockState(listOf(item)), {}, { id, delta, cost, price -> adjustment = listOf(id, delta, cost, price) })
      }
    }
    rule.onNodeWithTag("simple_stock_bucket").performClick()
    rule.onNodeWithTag("simple_edit_cost").assertDoesNotExist()
    rule.onNodeWithTag("simple_edit_price").performTextClearance()
    rule.onNodeWithTag("simple_edit_price").performTextInput("150")
    rule.onNodeWithTag("r2_stock_reason").performTextInput("Price correction")
    rule.onNodeWithText("Save").performClick()
    assertEquals(listOf("bucket", 0, null, 150.0), adjustment)
  }
}
