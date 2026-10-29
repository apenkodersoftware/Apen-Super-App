package com.aistudio.fasmovapos.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudio.fasmovapos.app.superapp.tabsFor
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Destructive fresh-setup/recovery fixtures are restricted to our separate installation on 5554. */
@RunWith(AndroidJUnit4::class)
class SuperSharedDeviceTest {
  @get:Rule val rule = createEmptyComposeRule()
  private val r get() = SuperDeviceRobot(rule)
  private val args get() = InstrumentationRegistry.getArguments()

  private fun verificationOnly() {
    check(r.context.packageName == "com.apenkoder.apen.review.r3verification")
  }

  private fun acceptTerms() {
    rule.waitUntil(60_000) {
      rule.onAllNodesWithTag("terms_checkbox").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() ||
        rule.onAllNodesWithTag("type-DAIRY").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }
    if (rule.onAllNodesWithTag("terms_checkbox").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()) {
      rule.onNodeWithTag("terms_checkbox").performClick()
      rule.onNodeWithTag("terms_continue").performClick()
    }
    r.waitFor("type-DAIRY")
  }

  private fun replace(
    tag: String,
    value: String,
  ) {
    r.visible(rule.onNodeWithTag(tag)).performTextReplacement(value)
    Espresso.closeSoftKeyboard()
  }

  @Test fun freshSetup() {
    verificationOnly()
    val type = requireNotNull(args.getString("livelihood"))
    LanguageSettings.set(r.context, AppLanguage.ENGLISH)
    ActivityScenario.launch(MainActivity::class.java).use {
      acceptTerms()
      rule.onNodeWithText(r.text("demo_label")).performClick()
      r.visible(rule.onNodeWithTag("type-$type")).performClick()
      replace("setup_business_field", "Demo first run $type")
      replace("setup_owner_name_field", "Review owner")
      replace("setup_username_field", "reviewowner")
      replace("setup_pin_field", "5830")
      replace("setup_confirm_pin_field", "5830")
      r.visible(rule.onNodeWithTag("setup_start_button")).performClick()
      r.waitFor("business-switch")
      val home = tabsFor(type).first()
      rule.onNodeWithTag("nav-$home").assertIsDisplayed()
      val profile =
        r
          .repo()
          .dao
          .businesses()
          .single()
      assertEquals(type, profile.type)
      assertTrue(profile.demo)
      if (type == "RENTAL") {
        rule.onNodeWithTag("month-next").performClick()
        r.awaitSavedFrame()
        rule.onNodeWithTag("charges-created").assertDoesNotExist()
        rule.onNodeWithTag("month-previous").performClick()
        r.awaitSavedFrame()
        rule.onNodeWithTag("charges-created").assertDoesNotExist()
      }
      r.capture("SAT-X", "first-run-$type-en")
      rule.onNodeWithText("SW").performClick()
      rule.waitUntil(30_000) { LanguageSettings.get(r.context) == AppLanguage.SWAHILI }
      r.awaitSavedFrame()
      rule.onNodeWithTag("nav-$home").assertIsDisplayed()
      assertTrue(rule.onAllNodesWithText(r.text(home)).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty())
      r.capture("SAT-X", "first-run-$type-sw")
    }
  }

  @Test fun recoverThroughNativePicker() {
    verificationOnly()
    LanguageSettings.set(r.context, AppLanguage.ENGLISH)
    ActivityScenario.launch(MainActivity::class.java).use {
      acceptTerms()
      r.visible(rule.onNodeWithText(r.context.getString(R.string.r2_recover))).performClick()
      rule.onNodeWithText(r.context.getString(R.string.r2_pick_backup)).performClick()
      nativeAndroidText("Show roots")
      nativeAndroidText("Downloads")
      androidText("Apen-backup-2026-10-29.flbk", document = true)
      rule.waitUntil(30_000) {
        rule
          .onAllNodesWithText(
            r.context.getString(R.string.r2_backup_selected),
          ).fetchSemanticsNodes(atLeastOneRootRequired = false)
          .isNotEmpty()
      }

      fun field(
        label: Int,
        value: String,
      ) {
        r.visible(rule.onNodeWithText(r.context.getString(label))).performTextReplacement(value)
        Espresso.closeSoftKeyboard()
      }
      field(R.string.username_email, "owner")
      val privateSecret = File(r.context.getExternalFilesDir(null), "private-recovery-secret.txt")
      field(R.string.r2_backup_password, privateSecret.readText().trim())
      privateSecret.delete()
      field(R.string.choose_pin, "7395")
      field(R.string.confirm_pin, "7395")
      r.visible(rule.onNode(isToggleable() and hasAnyAncestor(isDialog()))).performClick()
      r.capture("SAT-X", "recovery-consent")
      rule.onNodeWithText(r.context.getString(R.string.r2_restore_reset)).performClick()
      rule.waitUntil(120_000) {
        rule
          .onAllNodesWithText(
            r.context.getString(R.string.r2_recovery_done),
          ).fetchSemanticsNodes(atLeastOneRootRequired = false)
          .isNotEmpty()
      }
      r.capture("SAT-X", "recovery-success")
    }
  }

  @Test fun recoveredPinLogin() {
    verificationOnly()
    LanguageSettings.set(r.context, AppLanguage.ENGLISH)
    ActivityScenario.launch(MainActivity::class.java).use {
      r.waitFor("login_username_field")
      replace("login_username_field", "owner")
      replace("login_pin_field", "7395")
      rule.onNodeWithTag("login_submit_button").performClick()
      r.waitFor("business-switch")
      r.capture("SAT-X", "recovered-pin-login")
      assertEquals(
        11,
        r
          .repo()
          .dao
          .businesses()
          .size,
      )
    }
  }

  @Test fun allExistingHomesAndLanguage() {
    LanguageSettings.set(r.context, AppLanguage.ENGLISH)
    ActivityScenario.launch(MainActivity::class.java).use {
      r.waitFor("business-switch")
      val suffix = args.getString("evidenceSuffix").orEmpty()
      check(suffix in listOf("", "-font130", "-primary"))
      val profiles = r.repo().dao.businesses()
      val before = profiles.associate { p -> p.id to r.repo().dao.ledgerRows(p.id) }
      for (type in listOf("DAIRY", "RENTAL", "RESTAURANT", "RETAIL")) {
        val profile = profiles.filter { p -> p.demo && p.type == type }.maxBy { p -> p.createdAt ?: 0L }
        rule.onNodeWithTag("business-switch").performClick()
        rule.onNodeWithTag("business-${profile.id}").performScrollTo().performClick()
        r.waitFor("nav-${tabsFor(type).first()}")
        r.awaitSavedFrame()
        r.capture("SAT-X", "existing-$type-en$suffix")
        if (suffix == "-font130") largeTextStatementPreview(type, "en")
        rule.onNodeWithText("SW").performClick()
        rule.waitUntil(30_000) { LanguageSettings.get(r.context) == AppLanguage.SWAHILI }
        r.awaitSavedFrame()
        rule.onNodeWithTag("nav-${tabsFor(type).first()}").assertIsDisplayed()
        r.capture("SAT-X", "existing-$type-sw$suffix")
        if (suffix == "-font130") largeTextStatementPreview(type, "sw")
        rule.onNodeWithText("EN").performClick()
        rule.waitUntil(30_000) { LanguageSettings.get(r.context) == AppLanguage.ENGLISH }
        r.awaitSavedFrame()
      }
      profiles.forEach { p -> assertEquals(before.getValue(p.id), r.repo().dao.ledgerRows(p.id)) }
    }
  }

  private fun largeTextStatementPreview(
    type: String,
    language: String,
  ) {
    rule.onNodeWithTag("nav-${tabsFor(type)[3]}").performClick()
    r.click("month")
    r.waitFor("summary-difference")
    rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("super-statement"))
    r.visible(rule.onNodeWithTag("super-statement")).performClick()
    r.waitFor("r2_share_consent")
    r.capture("SAT-X", "statement-$type-$language-font130")
    r.visible(rule.onNodeWithTag("r2_share_consent"))
    r.capture("SAT-X", "statement-$type-$language-consent-font130")
    rule.onNodeWithText(LanguageSettings.localize(r.context).getString(R.string.simple_close)).performClick()
    rule.onNodeWithTag("nav-${tabsFor(type).first()}").performClick()
    r.awaitSavedFrame()
  }

  @Test fun editAndVoidHistoryThroughUi() {
    verificationOnly()
    LanguageSettings.set(r.context, AppLanguage.ENGLISH)
    ActivityScenario.launch(MainActivity::class.java).use {
      val id = r.create("DAIRY", "Demo history checks", "sat-history")
      rule.onNodeWithTag("nav-costs").performClick()
      rule.onNodeWithTag("add-cost").performClick()
      r.field("amount", "100")
      r.save()
      val original =
        r
          .repo()
          .dao
          .expenseRows(id)
          .single()
      rule.onNodeWithTag("nav-more").performClick()
      rule.onNodeWithTag("more-history").performClick()
      rule.onNodeWithTag("record-${original.id}").performScrollTo().performClick()
      r.click("edit")
      r.field("amount", "150")
      r.field("reason", "Corrected feed receipt")
      r.save()
      r.awaitSavedFrame()
      assertEquals(
        15000L,
        r
          .repo()
          .dao
          .expenseRow(original.id)!!
          .amountCents,
      )
      assertEquals(
        original.recordedAt,
        r
          .repo()
          .dao
          .expenseRow(original.id)!!
          .recordedAt,
      )
      r.capture("SAT-X", "history-original-correction-reason")
      rule.onNodeWithText("Corrected feed receipt").performScrollTo()
      r.capture("SAT-X", "history-before-after-reason-visible")
      r.visible(rule.onNodeWithTag("void-record")).performClick()
      r.field("reason", "Duplicate practice expense")
      r.save()
      r.awaitSavedFrame()
      assertTrue(
        r
          .repo()
          .dao
          .expenseRow(original.id)!!
          .voided,
      )
      assertEquals(0L, r.repo().snapshot(id, "2026-10-01", "2026-10-31").spentCents)
      r.capture("SAT-X", "history-void-preserves-record")
    }
  }

  private fun androidText(
    text: String,
    document: Boolean = false,
  ) = nativeAndroidText(text, document)
}
