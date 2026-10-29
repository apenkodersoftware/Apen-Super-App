package com.aistudio.fasmovapos.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudio.fasmovapos.app.data.backup.BackupManager
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.ExpenseEntity
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.repository.FasMovaPOSRepository
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import com.aistudio.fasmovapos.app.ui.theme.ThemeMode
import com.aistudio.fasmovapos.app.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Real-device UI acceptance. All business records belong to the explicitly named temporary review shop. */
@RunWith(AndroidJUnit4::class)
class LiteShopAcceptanceTest {
  @get:Rule val rule = createEmptyComposeRule()
  private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

  private fun repository() = FasMovaPOSRepository(FasMovaPOSDatabase.getDatabase(context))

  private fun waitFor(tag: String) = rule.waitUntil(30_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

  private fun capture(name: String) {
    rule.waitForIdle()
    val directory = File(context.getExternalFilesDir(null), "review-evidence").apply { mkdirs() }
    InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?.let { bitmap ->
      File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
      bitmap.recycle()
    }
  }

  private fun seedReview() =
    runBlocking {
      val repo = repository()
      assertFalse("Acceptance needs a fresh Lite review install, never an existing business", repo.isSetupComplete.first())
      LanguageSettings.set(context, AppLanguage.ENGLISH)
      ThemePreferences(context).setThemeMode(ThemeMode.DARK)
      repo.completeSetup("FasMova Lite review", "Review owner", "review", "4821", false)

      fun product(
        id: String,
        name: String,
        unit: String,
        price: Double,
        stock: Int,
      ) = ProductEntity(
        "review-$id",
        id,
        "",
        name,
        "Food",
        unit,
        0.0,
        price,
        stock,
        1000,
        "b-1",
        quantityScale = if (unit == "Pcs") 1 else 1000,
        isFavorite = true,
      )
      listOf(
        product("beans", "Beans", "Kg", 160.0, 10_000),
        product("rice", "Rice", "Kg", 180.0, 20_000),
        product("sugar", "Sugar", "Kg", 160.0, 15_000),
        product("oil", "Liquid oil", "Ltr", 240.0, 20_000),
        product("fat", "Solid cooking fat", "Kg", 220.0, 10_000),
        product("water", "Water bottle", "Pcs", 30.0, 20),
      ).forEach { repo.saveProduct(it) }
      repo.addCustomer(CustomerEntity("review-buyer", "Review Amina", "", "", creditLimit = 2000.0))
    }

  private fun sellMeasured(
    id: String,
    value: String,
    credit: Boolean = false,
    budget: Boolean = false,
  ) {
    val before = runBlocking { repository().sales.first().size }
    rule.onNodeWithTag("simple_nav_pos").performClick()
    waitFor("simple_product_review-$id")
    rule.onNodeWithTag("simple_product_review-$id").performScrollTo().performClick()
    if (budget) rule.onNodeWithText("By amount").performClick()
    rule.onNodeWithTag("lite_quantity_input").performTextReplacement(value)
    closeSoftKeyboard()
    capture("quantity-$id-$value")
    rule.onNodeWithTag("lite_add_quantity").performClick()
    rule.onNodeWithTag("simple_charge_bar").performClick()
    waitFor("simple_complete_sale")
    capture("checkout-$id-$value")
    if (credit) {
      rule.onNodeWithTag("simple_charge_list").performScrollToNode(hasTestTag("lite_part_payment"))
      rule.onNodeWithTag("lite_part_payment").performClick()
      rule.onNodeWithTag("simple_charge_list").performScrollToNode(hasTestTag("lite_received_now"))
      rule.onNodeWithTag("lite_received_now").performTextReplacement("100")
      closeSoftKeyboard()
      rule.onNodeWithTag("simple_charge_list").performScrollToNode(hasTestTag("simple_choose_customer"))
      rule.onNodeWithTag("simple_choose_customer").performClick()
      rule.onNodeWithText("Review Amina").performClick()
      capture("part-payment-and-deni")
    }
    rule.onNodeWithTag("simple_complete_sale").performClick()
    rule.waitUntil(30_000) { runBlocking { repository().sales.first().size } == before + 1 }
    waitFor("close_receipt_button")
    capture("receipt-$id-$value")
    rule.onNodeWithTag("close_receipt_button").performClick()
    rule.waitUntil(10_000) { rule.onAllNodesWithTag("close_receipt_button").fetchSemanticsNodes().isEmpty() }
  }

  @Test
  fun looseGoodsDebtReportsRestartAndBackupAreConsistent() {
    seedReview()
    var scenario = ActivityScenario.launch(MainActivity::class.java)
    waitFor("terms_checkbox")
    capture("terms-review-operator")
    rule.onNodeWithTag("terms_checkbox").performClick()
    rule.onNodeWithTag("terms_continue").performClick()
    waitFor("login_username_field")
    rule.onNodeWithTag("login_username_field").performTextReplacement("review")
    rule.onNodeWithTag("login_pin_field").performTextReplacement("4821")
    closeSoftKeyboard()
    rule.onNodeWithTag("login_submit_button").performClick()
    waitFor("simple_nav_pos")
    capture("sell-dark")
    verifyMeasuredProductEntry()
    sellMeasured("beans", "0.25")
    sellMeasured("beans", "0.5")
    sellMeasured("beans", "1.75", credit = true)
    sellMeasured("rice", "0.5")
    sellMeasured("sugar", "0.25")
    sellMeasured("oil", "0.25")
    sellMeasured("oil", "0.5")
    sellMeasured("fat", "0.5")
    sellMeasured("rice", "30", budget = true)
    rule.onNodeWithTag("simple_search").performTextReplacement("Water bottle")
    closeSoftKeyboard()
    capture("piece-search-water")
    rule.onNodeWithTag("simple_product_review-water").performClick()
    capture("piece-after-add")
    waitFor("simple_charge_bar")
    rule.onNodeWithTag("simple_charge_bar").performClick()
    waitFor("simple_complete_sale")
    rule.onNodeWithTag("simple_plus_review-water").performClick()
    capture("piece-checkout-two-bottles")
    rule.onNodeWithTag("simple_complete_sale").performClick()
    waitFor("close_receipt_button")
    rule.onNodeWithTag("close_receipt_button").performClick()
    rule.onNodeWithTag("simple_search").performTextReplacement("")
    closeSoftKeyboard()
    rule.onNodeWithTag("simple_nav_more").performClick()
    rule.onNodeWithTag("simple_more_customers").performClick()
    waitFor("record_payment_button_review-buyer")
    capture("deni-before-repayment")
    rule.onNodeWithTag("record_payment_button_review-buyer").performScrollTo().performClick()
    rule.onNodeWithTag("lite_repayment_amount").performTextReplacement("50")
    closeSoftKeyboard()
    rule.onNodeWithTag("lite_save_repayment").performScrollTo().performClick()
    rule.waitUntil(30_000) {
      runBlocking {
        repository()
          .customers
          .first()
          .single()
          .currentDebt
      } == 130.0
    }
    rule.onNodeWithTag("lite_history_review-buyer").performScrollTo().performClick()
    capture("deni-account-history")
    rule.onNodeWithText("Close").performClick()
    runBlocking {
      repository().addExpense(
        ExpenseEntity("review-expense", "REVIEW-EXP", "b-1", "Transport", 30.0, "Temporary review expense", "Review owner"),
      )
    }
    rule.onNodeWithTag("simple_nav_today").performClick()
    waitFor("lite_report_sales")
    capture("today-dark")
    rule.onNodeWithTag("simple_nav_more").performClick()
    rule.onNodeWithTag("simple_more_reports").performClick()
    waitFor("lite_period_DAY")
    capture("report-dark")
    listOf("WEEK", "MONTH", "YEAR", "DAY").forEach { rule.onNodeWithTag("lite_period_$it").performScrollTo().performClick() }
    runBlocking { ThemePreferences(context).setThemeMode(ThemeMode.LIGHT) }
    capture("report-light")
    runBlocking { ThemePreferences(context).setThemeMode(ThemeMode.DARK) }
    rule.onNodeWithTag("simple_nav_pos").performClick()
    rule.onNodeWithText("SW").performClick()
    waitFor("simple_nav_pos")
    capture("sell-swahili")
    rule.onNodeWithTag("simple_nav_today").performClick()
    waitFor("lite_report_sales")
    capture("today-swahili")
    rule.onNodeWithText("EN").performClick()
    waitFor("simple_nav_today")
    scenario.close()
    val password = "review-backup-4821".toCharArray()
    val backup = BackupManager(context).createBackup(password)
    FasMovaPOSDatabase.closeAndReset()
    runBlocking { repository().adjustProductStock("review-beans", 1000, null, null) }
    BackupManager(context).restore(backup, password)
    runBlocking {
      val repo = repository()
      assertEquals(
        7500,
        repo.products
          .first()
          .first { it.id == "review-beans" }
          .stockQuantity,
      )
      assertEquals(
        19_334,
        repo.products
          .first()
          .first { it.id == "review-rice" }
          .stockQuantity,
      )
      assertEquals(
        19_250,
        repo.products
          .first()
          .first { it.id == "review-oil" }
          .stockQuantity,
      )
      assertEquals(
        130.0,
        repo.customers
          .first()
          .single()
          .currentDebt,
        0.0,
      )
      assertEquals(909.88, repo.sales.first().sumOf { it.netAmount }, 0.001)
      assertEquals(50.0, repo.customerPayments.first().sumOf { it.amountPaid }, 0.0)
    }
    scenario = ActivityScenario.launch(MainActivity::class.java)
    waitFor("simple_nav_today")
    rule.onNodeWithTag("simple_nav_today").performClick()
    waitFor("lite_report_sales")
    rule.onNodeWithTag("lite_report_sales").assertIsDisplayed()
    capture("final-report-after-restart-and-restore")
    // Leave the review app open; the task's final capture verifies the installed activity and empty cart.
  }

  private fun verifyMeasuredProductEntry() {
    rule.onNodeWithTag("simple_nav_inventory").performClick()
    rule.onNodeWithTag("simple_add_item").performClick()
    rule.onNodeWithTag("simple_item_name").performTextReplacement("UI-created grain")
    rule.onNodeWithTag("lite_unit_Kg").performClick()
    rule.onNodeWithTag("simple_item_price").performScrollTo().performTextReplacement("200")
    rule.onNodeWithTag("simple_item_quantity").performScrollTo().performTextReplacement("100")
    closeSoftKeyboard()
    capture("measured-product-entry")
    rule.onNodeWithTag("simple_save_item").performClick()
    rule.waitUntil(30_000) { runBlocking { repository().products.first().any { it.name == "UI-created grain" } } }
    val product = runBlocking { repository().products.first().first { it.name == "UI-created grain" } }
    assertEquals("Kg", product.unit)
    assertEquals(1000, product.quantityScale)
    assertEquals(100_000, product.stockQuantity)
    rule.onNodeWithTag("simple_nav_pos").performClick()
  }
}
