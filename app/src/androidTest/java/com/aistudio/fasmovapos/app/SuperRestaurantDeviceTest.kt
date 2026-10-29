package com.aistudio.fasmovapos.app

import android.os.SystemClock
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aistudio.fasmovapos.app.superapp.SuperDates
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SuperRestaurantDeviceTest {
  @get:Rule val rule = createEmptyComposeRule()
  private val r get() = SuperDeviceRobot(rule)

  private fun item(
    name: String,
    price: String,
    category: String,
  ) {
    rule.onNodeWithTag("add-item").performClick()
    r.field("name", name)
    r.field("price", price)
    r.choice("category", category)
    r.save()
  }

  private fun quantity(
    name: String,
    value: String,
  ) {
    r.visible(rule.onNodeWithTag("tile-$name")).performTouchInput { longClick() }
    r.field("quantity", value)
    r.save()
  }

  @Test fun satR_nativeMenuDailySalesDeniShoppingCollectionAndFourTapOrder() {
    LanguageSettings.set(r.context, AppLanguage.ENGLISH)
    ActivityScenario.launch(MainActivity::class.java).use {
      val id = r.create("RESTAURANT", "Demo Kitchen Verified", "sat-r")
      r.waitFor("nav-menu")
      rule.onNodeWithTag("nav-menu").performClick()
      item("Chai", "20", "DRINKS")
      item("Chapati", "20", "FOOD")
      item("Githeri", "60", "FOOD")
      item("Ugali and sukuma", "80", "FOOD")
      item("Samosa", "15", "SNACKS")
      r.capture("SAT-R", "01-menu")
      rule.onNodeWithTag("nav-more").performClick()
      rule.onNodeWithTag("more-deni").performClick()
      rule.onNodeWithTag("add-party").performClick()
      r.field("name", "Demo regular")
      r.save()
      rule.onNodeWithTag("nav-sell").performClick()
      r.capture("SAT-R", "02-food-home")
      val started = SystemClock.elapsedRealtime()
      rule.onNodeWithTag("tile-Chai").performClick()
      rule.onNodeWithTag("tile-Chapati").performClick()
      rule.onNodeWithTag("tile-Githeri").performClick()
      rule.onNodeWithTag("tender-CASH").performClick()
      r.waitFor("sale-saved")
      val elapsed = SystemClock.elapsedRealtime() - started
      assertTrue("Four-tap order took $elapsed ms", elapsed < 10000)
      File(
        r.context.getExternalFilesDir(null),
        "sat-r-speed.txt",
      ).writeText("native_actions=4\nelapsed_ms=$elapsed\nkeyboard_actions=0\nincludes_persistence=true\n")
      val day = SuperDates.today()
      val next = SuperDates.addDays(day, 1)
      assertEquals(10000L, r.repo().snapshot(id, day, next).earnedCents)
      r.capture("SAT-R", "03-four-tap-cash-order")
      quantity("Chai", "24")
      quantity("Chapati", "14")
      quantity("Githeri", "17")
      rule.onNodeWithTag("tender-CASH").performClick()
      r.waitFor("sale-saved")
      rule.waitForIdle()
      assertEquals(188000L, r.repo().snapshot(id, day, next).receivedCents)
      quantity("Githeri", "2")
      rule.onNodeWithTag("tender-CREDIT").performClick()
      r.waitFor("super-form")
      r.save()
      var totals = r.repo().snapshot(id, day, next)
      assertEquals(200000L, totals.earnedCents)
      assertEquals(188000L, totals.receivedCents)
      assertEquals(12000L, totals.owedCents)
      rule.onNodeWithTag("nav-today").performClick()
      r.capture("SAT-R", "04-two-thousand-sales-with-deni")
      rule.onNodeWithTag("nav-costs").performClick()
      rule.onNodeWithTag("add-cost").performClick()
      r.field("amount", "900")
      r.save()
      assertEquals(110000L, r.repo().snapshot(id, day, next).salesLessExpensesCents)
      rule.onNodeWithTag("nav-today").performClick()
      rule.onNodeWithText(r.text("sales_less_expenses")).performScrollTo()
      r.capture("SAT-R", "05-shopping-nine-hundred")
      rule.onNodeWithTag("nav-more").performClick()
      rule.onNodeWithTag("more-deni").performClick()
      rule.onNodeWithTag("party-Demo regular").performClick()
      r.visible(rule.onNodeWithTag("record-payment")).performClick()
      r.field("amount", "120")
      r.dateField("date", next)
      r.save()
      totals = r.repo().snapshot(id, day, next)
      assertEquals(200000L, totals.earnedCents)
      assertEquals(200000L, totals.receivedCents)
      assertEquals(0L, totals.owedCents)
      assertTrue(
        r
          .repo()
          .dao
          .commerceItems(id)
          .all { it.stockMilli == null },
      )
      r.capture("SAT-R", "06-next-day-collection-no-new-sales")
    }
  }
}
