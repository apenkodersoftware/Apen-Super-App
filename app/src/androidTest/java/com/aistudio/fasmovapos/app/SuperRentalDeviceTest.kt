package com.aistudio.fasmovapos.app

import android.accessibilityservice.AccessibilityService
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SuperRentalDeviceTest {
  @get:Rule val rule = createEmptyComposeRule()
  private val r get() = SuperDeviceRobot(rule)

  private fun unit(
    label: String,
    rent: String,
  ) {
    rule.onNodeWithTag("add-unit").performClick()
    r.field("label", label)
    r.field("rent", rent)
    r.field("tenant", "$label tenant")
    r.field("phone", "+254700000001")
    r.dateField("moveIn", "2026-10-01")
    r.save()
  }

  private fun payment(
    label: String,
    amount: String,
    mpesa: Boolean = false,
    date: String? = null,
  ) {
    r.visible(rule.onNodeWithTag("unit-$label")).performClick()
    r.visible(rule.onNodeWithTag("record-rent-payment")).performClick()
    r.field("amount", amount)
    if (mpesa) {
      r.choice("method", "MPESA")
      r.field("reference", "UJR3RENT01")
    }
    if (date != null) r.dateField("date", date)
    r.save()
  }

  @Test fun satL_nativeUnitsRentVacancyOldestArrearsAndShareChooser() {
    LanguageSettings.set(r.context, AppLanguage.ENGLISH)
    ActivityScenario.launch(MainActivity::class.java).use {
      val id = r.create("RENTAL", "Demo Rentals Verified", "sat-l")
      r.waitFor("nav-units")
      rule.onNodeWithTag("nav-units").performClick()
      if (r
          .repo()
          .dao
          .propertyRows(id)
          .none { it.name == "Demo Court" }
      ) {
        rule.onNodeWithTag("add-property").performClick()
        r.field("name", "Demo Court")
        r.save()
      }
      unit("A1", "8000")
      unit("A2", "8000")
      unit("B1", "12000")
      rule.onNodeWithTag("nav-rent").performClick()
      rule.waitForIdle()
      assertEquals(2800000L, r.repo().snapshot(id, "2026-10-01", "2026-10-31").earnedCents)
      r.capture("SAT-L", "01-october-expected")
      payment("A1", "8000", true)
      r.click("close")
      payment("A2", "5000")
      r.click("close")
      var totals = r.repo().snapshot(id, "2026-10-01", "2026-11-30")
      assertEquals(1300000L, totals.receivedCents)
      assertEquals(1500000L, totals.owedCents)
      r.capture("SAT-L", "02-payments-and-arrears")
      rule.onNodeWithTag("nav-costs").performClick()
      rule.onNodeWithTag("add-cost").performClick()
      r.field("amount", "2500")
      r.field("note", "Plumbing repair")
      r.save()
      totals = r.repo().snapshot(id, "2026-10-01", "2026-11-30")
      assertEquals(1050000L, totals.receivedCents - totals.spentCents)
      r.capture("SAT-L", "03-plumbing-repair")
      rule.onNodeWithTag("nav-rent").performClick()
      r.visible(rule.onNodeWithTag("unit-B1")).performClick()
      r.click("move_out")
      r.dateField("date", "2026-11-01")
      r.field("reason", "Tenant leaving before November")
      r.save()
      r.click("close")
      rule.onNodeWithTag("month-next").performScrollTo().performClick()
      r.waitFor("charges-created")
      rule.waitForIdle()
      assertEquals(3100000L, r.repo().snapshot(id, "2026-10-01", "2026-11-30").owedCents)
      assertEquals(
        2,
        r
          .repo()
          .dao
          .rentCharges(id)
          .count { !it.voided && it.month == "2026-11" },
      )
      r.capture("SAT-L", "04-november-vacancy-and-31000-owed")
      rule.onNodeWithText("November 2026").assertIsDisplayed()
      payment("A2", "3000", date = "2026-11-02")
      val party =
        r
          .repo()
          .dao
          .partys(id)
          .first { it.name == "A2 tenant" }
      val receipt =
        r
          .repo()
          .dao
          .receiptRows(id)
          .first { it.partyId == party.id && it.amountCents == 300000L }
      assertEquals(listOf("2026-10"), r.repo().allocations(receipt.id).map { it.month })
      r.capture("SAT-L", "05-october-first-allocation")
      r.click("reminder")
      rule.onNodeWithTag("share-preview-text").assertTextContains("A2", substring = true).assertTextContains("8,000", substring = true)
      r.capture("SAT-L", "06-reminder-preview")
      rule.onNodeWithTag("open-share-sheet").performClick()
      r.captureExternal("SAT-L", "07-android-share-chooser-no-send")
      InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }
  }
}
