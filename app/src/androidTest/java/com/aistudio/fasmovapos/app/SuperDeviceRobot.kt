package com.aistudio.fasmovapos.app

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.widget.DatePicker
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.superapp.SuperRepository
import com.aistudio.fasmovapos.app.superapp.translated
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import org.hamcrest.Matcher
import org.junit.Assert.assertTrue
import java.io.File

/** Acts on real native UI; repository access below only verifies outcomes. */
class SuperDeviceRobot(
  val rule: ComposeTestRule,
) {
  val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

  fun repo() = SuperRepository(FasMovaPOSDatabase.getDatabase(context))

  fun text(key: String) = translated(LanguageSettings.localize(context), key)

  fun waitFor(tag: String) =
    rule.waitUntil(60_000) {
      rule.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }

  fun waitGone(tag: String) =
    rule.waitUntil(60_000) {
      rule.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty()
    }

  fun visible(node: SemanticsNodeInteraction): SemanticsNodeInteraction {
    if (!node.isDisplayed()) node.performScrollTo()
    return node
  }

  fun click(key: String) = visible(rule.onNodeWithText(text(key))).performClick()

  fun field(
    id: String,
    value: String,
  ) {
    visible(rule.onNodeWithTag("field-$id")).performTextReplacement(value)
    Espresso.closeSoftKeyboard()
  }

  fun choice(
    id: String,
    value: String,
  ) {
    rule
      .onNodeWithTag("field-$id")
      .onChildren()
      .filter(hasClickAction())[0]
      .performScrollTo()
      .performClick()
    rule.onNodeWithTag("choice-$value").performClick()
  }

  fun save() {
    rule.onNodeWithTag("action-save").performClick()
    waitGone("super-form")
  }

  fun dateField(
    id: String,
    value: String,
  ) {
    rule
      .onNodeWithTag("field-$id")
      .onChildren()
      .filter(hasClickAction())[0]
      .performScrollTo()
      .performClick()
    rule.waitForIdle()
    val parts = value.split('-').map(String::toInt)
    Espresso.onView(ViewMatchers.isAssignableFrom(DatePicker::class.java)).perform(
      object : ViewAction {
        override fun getConstraints(): Matcher<View> = ViewMatchers.isAssignableFrom(DatePicker::class.java)

        override fun getDescription() = "Set the scripted activity date"

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

  fun create(
    type: String,
    name: String,
    scenario: String,
  ): String {
    waitFor("business-switch")
    val previous = File(context.getExternalFilesDir(null), "$scenario-profile-id.txt").takeIf { it.exists() }?.readText()
    if (type == "RENTAL" &&
      previous != null &&
      repo().dao.businessProfile(previous)?.demo == true &&
      repo().dao.rentalUnitRows(previous).isEmpty() &&
      repo().dao.ledgerRows(previous).isEmpty()
    ) {
      rule.onNodeWithTag("business-switch").performClick()
      rule.onNodeWithTag("business-$previous").performScrollTo().performClick()
      waitFor("nav-units")
      awaitSavedFrame()
      return previous
    }
    rule.onNodeWithTag("business-switch").performClick()
    click("add_business")
    rule.onNodeWithText(text("demo_label")).performClick()
    visible(rule.onNodeWithTag("type-$type")).performClick()
    waitFor("super-form")
    field("name", name)
    save()
    waitFor("business-switch")
    awaitSavedFrame()
    val id = context.getSharedPreferences("apen_super", Context.MODE_PRIVATE).getString("business", null) ?: error("No selected business")
    assertTrue(repo().dao.businessProfile(id)!!.demo)
    File(context.getExternalFilesDir(null), "$scenario-profile-id.txt").writeText(id)
    return id
  }

  fun capture(
    scenario: String,
    name: String,
  ) {
    awaitSavedFrame()
    val dir = File(context.getExternalFilesDir(null), "docs/evidence/$scenario").apply { mkdirs() }
    val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: error("No native screenshot")
    try {
      File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    } finally {
      image.recycle()
    }
  }

  fun awaitSavedFrame() {
    rule.waitForIdle()
    rule.waitUntil(60_000) { rule.onAllNodesWithText(text("working")).fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty() }
    rule.waitForIdle()
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    SystemClock.sleep(350)
  }

  fun captureExternal(
    scenario: String,
    name: String,
  ) {
    // Android's share/file picker can put the app Activity in the background, with no Compose roots.
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    SystemClock.sleep(500)
    val dir = File(context.getExternalFilesDir(null), "docs/evidence/$scenario").apply { mkdirs() }
    val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: error("No native screenshot")
    try {
      File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    } finally {
      image.recycle()
    }
  }
}
