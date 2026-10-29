package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

fun simpleProduct(
  id: String,
  name: String,
  stock: Int = 12,
  price: Double = 100.0,
): ProductEntity = ProductEntity(id, "SKU-$id", "barcode-$id", name, "Kitchen", "Pcs", 0.0, price, stock, 5, "b-1")

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class SimpleCatalogTest {
  @get:Rule val rule = createComposeRule()

  @Test
  fun searchAndWholeCardTapWorkWithoutDisplayingSku() {
    var query by mutableStateOf("")
    var added = ""
    val products = listOf(simpleProduct("bucket", "Bucket"), simpleProduct("plate", "Plate"))
    rule.setContent {
      FasMovaPOSTheme {
        SimpleCatalog(CatalogState(products, query, "All"), { query = it }, {}, { added = it.id }, {})
      }
    }
    rule.onNodeWithText("SKU-bucket").assertDoesNotExist()
    rule.onNodeWithTag("simple_search").performTextInput("Bucket")
    rule.onNodeWithTag("simple_product_plate").assertDoesNotExist()
    rule.onNodeWithTag("simple_product_bucket").performClick()
    assertEquals("bucket", added)
  }

  @Test
  fun unavailableItemsCannotBeAddedAndEmptyShopHasAUsefulNextStep() {
    var products by mutableStateOf(listOf(simpleProduct("empty", "Bucket", stock = 0)))
    var openedStock = false
    rule.setContent {
      FasMovaPOSTheme {
        SimpleCatalog(CatalogState(products, "", "All"), {}, {}, {}, { openedStock = true })
      }
    }
    rule.onNodeWithTag("simple_product_empty").assertIsNotEnabled()
    rule.onNodeWithText("0 left").assertIsDisplayed()
    rule.runOnIdle { products = emptyList() }
    rule.onNodeWithText("Add your first item").performClick()
    assertEquals(true, openedStock)
  }
}
