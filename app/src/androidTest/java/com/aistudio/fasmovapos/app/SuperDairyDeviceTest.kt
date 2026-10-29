package com.aistudio.fasmovapos.app

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.DatePicker
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.superapp.SuperDates
import com.aistudio.fasmovapos.app.superapp.SuperRepository
import com.aistudio.fasmovapos.app.superapp.translated
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import org.hamcrest.Matcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Native user entry, not database seeding. All new financial records are in an explicit Demo profile. */
@RunWith(AndroidJUnit4::class)
class SuperDairyDeviceTest {
  @get:Rule val rule = createEmptyComposeRule()
  private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

  private fun repo() = SuperRepository(FasMovaPOSDatabase.getDatabase(context))

  private fun waitFor(tag: String) = rule.waitUntil(60_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

  private fun waitGone(tag: String) = rule.waitUntil(60_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }

  private fun text(key: String) = translated(LanguageSettings.localize(context), key)

  private fun click(key: String) {
    val node = rule.onNodeWithText(text(key))
    if (!node.isDisplayed()) node.performScrollTo()
    node.performClick()
  }

  private fun field(
    id: String,
    value: String,
  ) {
    rule.onNodeWithTag("field-$id").performScrollTo().performTextReplacement(value)
    Espresso.closeSoftKeyboard()
  }

  private fun choice(
    id: String,
    value: String,
  ) {
    rule
      .onNodeWithTag("field-$id")
      .onChildren()
      .filter(hasClickAction())[0]
      .performClick()
    rule.onNodeWithTag("choice-$value").performClick()
  }

  private fun save() {
    rule.onNodeWithTag("action-save").performClick()
    waitGone("super-form")
  }

  private fun capture(name: String) {
    SuperDeviceRobot(rule).awaitSavedFrame()
    val dir = File(context.getExternalFilesDir(null), "docs/evidence/SAT-D").apply { mkdirs() }
    val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: error("No native screenshot")
    File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    image.recycle()
  }

  private fun date(value: String) {
    val parts = value.split('-').map(String::toInt)
    Espresso.onView(ViewMatchers.isAssignableFrom(DatePicker::class.java)).perform(
      object : ViewAction {
        override fun getConstraints(): Matcher<View> = ViewMatchers.isAssignableFrom(DatePicker::class.java)

        override fun getDescription() = "Choose the scripted activity date"

        override fun perform(
          controller: UiController,
          view: View,
        ) {
          (view as DatePicker).updateDate(parts[0], parts[1] - 1, parts[2])
          controller.loopMainThreadUntilIdle()
        }
      },
    )
    Espresso.onView(ViewMatchers.withId(android.R.id.button1)).perform(ViewActions.click())
  }

  private fun delivery(
    day: String,
    session: String,
    litres: String,
  ) {
    rule.onNodeWithTag("milk-$session").performClick()
    waitFor("milk-number")
    litres.forEach { rule.onNodeWithTag("key-$it").performClick() }
    rule.onNodeWithText(text("date"), substring = true).performClick()
    val today = SimpleDateFormat("EEE d MMM yyyy", Locale.UK).apply { timeZone = SuperDates.zone }.format(Date())
    rule.onNodeWithText(today).performClick()
    date(day)
    rule.onNodeWithTag("milk-save").performClick()
    waitGone("milk-number")
  }

  @Test fun satD_nativeDairyEntryDeductionCostCorrectionAndHistory() {
    LanguageSettings.set(context, AppLanguage.ENGLISH)
    ActivityScenario.launch(MainActivity::class.java).use {
      waitFor("business-switch")
      rule.onNodeWithTag("business-switch").performClick()
      click("add_business")
      rule.onNodeWithText(text("demo_label")).performClick()
      rule.onNodeWithTag("type-DAIRY").performClick()
      waitFor("super-form")
      field("name", "Demo Dairy")
      save()
      waitFor("milk-MORNING")
      val id = context.getSharedPreferences("apen_super", Context.MODE_PRIVATE).getString("business", null) ?: error("No selected business")
      assertTrue(repo().dao.businessProfile(id)!!.demo)
      File(context.getExternalFilesDir(null), "sat-d-profile-id.txt").writeText(id)
      capture("01-dairy-home")
      rule.onNodeWithTag("nav-buyers").performClick()
      rule.onNodeWithTag("add-party").performClick()
      field("name", "Demo Dairy Society")
      field("price", "50")
      save()
      capture("02-buyer")
      rule.onNodeWithTag("nav-milk").performClick()
      delivery("2026-10-27", "MORNING", "12")
      delivery("2026-10-27", "EVENING", "10")
      delivery("2026-10-28", "MORNING", "11")
      delivery("2026-10-28", "EVENING", "9")
      var totals = repo().snapshot(id, "2026-10-01", "2026-10-31")
      assertEquals(42000L, totals.litresMilli)
      assertEquals(210000L, totals.earnedCents)
      assertEquals(210000L, totals.owedCents)
      capture("03-forty-two-litres")
      rule.onNodeWithTag("nav-buyers").performClick()
      rule.onNodeWithTag("party-Demo Dairy Society").performClick()
      rule.onNodeWithTag("record-payment").performScrollTo().performClick()
      field("amount", "1800")
      choice("method", "MPESA")
      field("reference", "UJR2DEMO01")
      click("add_deduction")
      rule.onNodeWithTag("deduction-amount-0").performScrollTo().performTextReplacement("300")
      Espresso.closeSoftKeyboard()
      save()
      totals = repo().snapshot(id, "2026-10-01", "2026-10-31")
      assertEquals(180000L, totals.receivedCents)
      assertEquals(30000L, totals.spentCents)
      assertEquals(0L, totals.owedCents)
      capture("04-payment-and-feed")
      rule.onNodeWithTag("nav-costs").performClick()
      rule.onNodeWithTag("add-cost").performClick()
      choice("category", "VET_AI")
      field("amount", "500")
      save()
      totals = repo().snapshot(id, "2026-10-01", "2026-10-31")
      assertEquals(130000L, totals.salesLessExpensesCents)
      capture("05-vet-cost")
      rule.onNodeWithTag("nav-milk").performClick()
      val card = "delivery-2026-10-27-MORNING"
      rule.onNodeWithTag(card).performScrollTo()
      rule.onNode(hasText(text("edit")) and hasAnyAncestor(hasTestTag(card))).performClick()
      rule.onNodeWithTag("key-⌫").performClick()
      rule.onNodeWithTag("key-3").performClick()
      rule.onNodeWithTag("milk-reason").performScrollTo().performTextReplacement("Corrected measuring jug reading")
      Espresso.closeSoftKeyboard()
      rule.onNodeWithTag("milk-save").performClick()
      waitGone("milk-number")
      totals = repo().snapshot(id, "2026-10-01", "2026-10-31")
      assertEquals(43000L, totals.litresMilli)
      assertEquals(215000L, totals.earnedCents)
      assertEquals(5000L, totals.owedCents)
      assertEquals(135000L, totals.salesLessExpensesCents)
      rule.onNodeWithTag(card).performScrollTo()
      rule.onNode(hasText(text("history")) and hasAnyAncestor(hasTestTag(card))).performClick()
      capture("06-correction-original-and-reason")
      val record = repo().dao.milkDeliverys(id).first { it.occurredOn == "2026-10-27" && it.session == "MORNING" }
      assertTrue(
        repo().history(record.id).any {
          it.reason == "Corrected measuring jug reading" &&
            it.before!!.contains("12000") &&
            it.after!!.contains("13000")
        },
      )
    }
  }
}
