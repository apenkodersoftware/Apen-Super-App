package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class StatementPreviewTest {
  @get:Rule val rule = createComposeRule()

  @Test fun sharingRequiresAnExplicitChoiceForThisPreview() {
    val data = ReportInput(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    val now = System.currentTimeMillis()
    val comparison = comparePeriods(data, ReportPeriod.DAY, now)
    val statement =
      BusinessStatement.create(
        "Test shop",
        comparison,
        recordingCoverage(data, RecordContext(), comparison.currentWindow),
        now,
      )
    rule.setContent { FasMovaPOSTheme { StatementPreview(statement) {} } }
    rule.onNodeWithTag("r2_share_pdf").assertIsNotEnabled()
    rule.onNodeWithTag("r2_share_consent").performScrollTo().performClick()
    rule.onNodeWithTag("r2_share_pdf").assertIsEnabled()
  }

  @Test fun summaryDisplaysNumbersAndDatesWithoutReplacementCharacters() {
    val data = ReportInput(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    rule.setContent { FasMovaPOSTheme { BusinessReportScreen(data, {}, {}) } }
    rule.onNodeWithTag("lite_report_sales").assertIsDisplayed()
    rule.onAllNodesWithText("\uFFFD", substring = true, useUnmergedTree = true).assertCountEquals(0)
  }
}
