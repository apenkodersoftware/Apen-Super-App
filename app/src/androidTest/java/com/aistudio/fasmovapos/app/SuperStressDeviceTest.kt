package com.aistudio.fasmovapos.app

import android.os.Debug
import android.os.SystemClock
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aistudio.fasmovapos.app.superapp.SuperDates
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Synthetic load uses validated domain writes only in the disposable verification installation. */
@RunWith(AndroidJUnit4::class)
class SuperStressDeviceTest {
  @get:Rule val rule = createEmptyComposeRule()
  private val r get() = SuperDeviceRobot(rule)

  @Test fun thousandDeliveryHistoryRemainsUsableAfterActivityRestart() {
    check(r.context.packageName == "com.apenkoder.apen.review.r3verification")
    LanguageSettings.set(r.context, AppLanguage.ENGLISH)
    var business = ""
    ActivityScenario.launch(MainActivity::class.java).use {
      business = r.create("DAIRY", "Demo 1000-record stress", "stress")
      val repo = r.repo()
      val buyer = repo.addParty(business, "Demo stress buyer", "COOPERATIVE", 5000).id
      repeat(1000) { index ->
        repo.milk(
          business,
          buyer,
          SuperDates.addDays("2024-01-01", index / 2),
          if (index % 2 == 0) "MORNING" else "EVENING",
          "1",
          5000,
        )
      }
      assertEquals(1000, repo.dao.milkDeliverys(business).size)
    }
    val started = SystemClock.elapsedRealtime()
    ActivityScenario.launch(MainActivity::class.java).use {
      r.waitFor("nav-milk")
      r.awaitSavedFrame()
      val launchMs = SystemClock.elapsedRealtime() - started
      rule.onNodeWithTag("nav-more").performClick()
      rule.onNodeWithTag("more-history").performClick()
      rule.onNode(hasScrollToIndexAction()).performScrollToIndex(900)
      r.awaitSavedFrame()
      r.capture("SAT-X", "thousand-record-history-scroll")
      assertEquals(
        1000,
        r
          .repo()
          .dao
          .milkDeliverys(business)
          .size,
      )
      val pss = Debug.getPss()
      assertTrue(pss > 0)
      val result =
        JSONObject()
          .put("device", "emulator-5554")
          .put("package", r.context.packageName)
          .put("fixture", "1000 milk deliveries created through validated domain APIs in disposable demo business")
          .put("records_after_activity_restart", 1000)
          .put("history_scrolled_to_index", 900)
          .put("instrumented_activity_launch_to_ready_ms", launchMs)
          .put("instrumented_process_pss_kb", pss)
          .put("physical_phone_test", false)
      File(r.context.getExternalFilesDir(null), "stress-verification.json").writeText(result.toString(2))
    }
  }
}
