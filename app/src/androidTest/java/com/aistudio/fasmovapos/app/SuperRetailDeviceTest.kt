package com.aistudio.fasmovapos.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aistudio.fasmovapos.app.superapp.SuperDates
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SuperRetailDeviceTest {
  @get:Rule val rule = createEmptyComposeRule()
  private val r get() = SuperDeviceRobot(rule)

  @Test fun satS_nativePartPaymentCollectionAndStockActions() {
    LanguageSettings.set(r.context, AppLanguage.ENGLISH)
    ActivityScenario.launch(MainActivity::class.java).use {
      val id = r.create("RETAIL", "Demo Shop", "sat-s")
      r.waitFor("nav-stock")
      rule.onNodeWithTag("nav-stock").performClick()
      rule.onNodeWithTag("add-item").performClick()
      r.field("name", "Household pack")
      r.field("price", "280")
      r.field("quantity", "10")
      r.field("cost", "100")
      r.save()
      r.capture("SAT-S", "01-stock-and-restock-actions")
      rule.onNodeWithTag("nav-deni").performClick()
      rule.onNodeWithTag("add-party").performClick()
      r.field("name", "Demo shop customer")
      r.save()
      rule.onNodeWithTag("nav-sell").performClick()
      rule.onNodeWithTag("tile-Household pack").performClick()
      rule.onNodeWithTag("tender-CREDIT").performClick()
      r.field("received", "100")
      r.save()
      val day = SuperDates.today()
      var s = r.repo().snapshot(id, day, day)
      assertEquals(28000L, s.earnedCents)
      assertEquals(10000L, s.receivedCents)
      assertEquals(18000L, s.owedCents)
      assertEquals(
        9000L,
        r
          .repo()
          .dao
          .commerceItems(id)
          .single()
          .stockMilli,
      )
      r.capture("SAT-S", "02-part-paid-sale")
      rule.onNodeWithTag("nav-deni").performClick()
      rule.onNodeWithTag("party-Demo shop customer").performClick()
      r.visible(rule.onNodeWithTag("record-payment")).performClick()
      r.field("amount", "50")
      r.save()
      s = r.repo().snapshot(id, day, day)
      assertEquals(28000L, s.earnedCents)
      assertEquals(15000L, s.receivedCents)
      assertEquals(13000L, s.owedCents)
      r.capture("SAT-S", "03-collection-no-duplicate-sale")
      rule.onNodeWithTag("nav-today").performClick()
      r.capture("SAT-S", "04-summary-definitions")
    }
  }
}
