package com.aistudio.fasmovapos.app

import android.accessibilityservice.AccessibilityService
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudio.fasmovapos.app.superapp.RecordPeriod
import com.aistudio.fasmovapos.app.superapp.SuperDates
import com.aistudio.fasmovapos.app.superapp.businessStatement
import com.aistudio.fasmovapos.app.superapp.tabsFor
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Actual preview/consent, Android save dialogs and chooser; never chooses a recipient or sends. */
@RunWith(AndroidJUnit4::class)
class SuperStatementDeviceTest {
  @get:Rule val rule = createEmptyComposeRule()
  private val r get() = SuperDeviceRobot(rule)
  private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

  private fun waitForApp() {
    rule.waitUntil(30_000) { automation.rootInActiveWindow?.packageName?.toString() == r.context.packageName }
    r.awaitSavedFrame()
    rule.waitUntil(60_000) {
      rule
        .onAllNodesWithTag("r2_share_pdf")
        .filter(isEnabled())
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()
    }
    rule.onNodeWithText(r.context.getString(R.string.r2_export_problem)).assertDoesNotExist()
  }

  private fun save(type: String) {
    r.visible(rule.onNodeWithTag("r2_save_$type")).performClick()
    nativeAndroidText("Show roots")
    nativeAndroidText("Downloads")
    nativeAndroidText("Save")
    waitForApp()
  }

  @Test fun allFourStatementsThroughNativePreviewSaveAndShareChooser() {
    check(r.context.packageName == "com.apenkoder.apen.review.r3verification")
    LanguageSettings.set(r.context, AppLanguage.ENGLISH)
    val receipt = JSONArray()
    ActivityScenario.launch(MainActivity::class.java).use {
      r.waitFor("business-switch")
      val profiles = r.repo().dao.businesses()
      val before = profiles.associate { p -> p.id to r.repo().dao.ledgerRows(p.id) }
      for (type in listOf("DAIRY", "RENTAL", "RESTAURANT", "RETAIL")) {
        val p = profiles.filter { it.demo && it.type == type }.maxBy { it.createdAt ?: 0L }
        rule.onNodeWithTag("business-switch").performClick()
        r.visible(rule.onNodeWithTag("business-${p.id}")).performClick()
        r.waitFor("nav-${tabsFor(type)[3]}")
        rule.onNodeWithTag("nav-${tabsFor(type)[3]}").performClick()
        r.click("month")
        r.waitFor("summary-difference")
        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("super-statement"))
        r.visible(rule.onNodeWithTag("super-statement")).performClick()
        r.waitFor("r2_share_consent")
        rule.onNodeWithTag("r2_share_pdf").assertIsNotEnabled()
        rule.onNodeWithTag("r2_save_pdf").assertIsNotEnabled()
        rule.onNodeWithTag("r2_save_csv").assertIsNotEnabled()
        val reportId =
          rule
            .onAllNodesWithText(r.text("report_id") + ": ", substring = true)
            .fetchSemanticsNodes()
            .single()
            .config[SemanticsProperties.Text]
            .single()
            .text
            .substringAfter(": ")
            .trim()
        r.capture("SAT-X", "statement-$type-preview")
        r.visible(rule.onNodeWithTag("r2_share_consent")).performClick()
        rule.onNodeWithTag("r2_share_pdf").assertIsEnabled()
        save("pdf")
        save("csv")
        r.visible(rule.onNodeWithTag("r2_share_pdf")).performClick()
        rule.waitUntil(30_000) { automation.rootInActiveWindow?.packageName?.toString() != r.context.packageName }
        r.captureExternal("SAT-X", "statement-$type-share-chooser")
        automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        waitForApp()
        val today = SuperDates.today()
        val expected = r.repo().businessStatement(p.id, RecordPeriod(today.take(7) + "-01", today))
        receipt.put(
          JSONObject()
            .put("type", type)
            .put("report_id", reportId)
            .put("expected_values", JSONObject(expected.rows.toMap() - setOf("report_id", "generated_at_epoch_ms"))),
        )
        rule.onNodeWithText(r.context.getString(R.string.simple_close)).performClick()
      }
      profiles.forEach { p -> assertEquals(before.getValue(p.id), r.repo().dao.ledgerRows(p.id)) }
    }
    File(r.context.getExternalFilesDir(null), "statement-native-manifest.json").writeText(receipt.toString(2))
  }
}
