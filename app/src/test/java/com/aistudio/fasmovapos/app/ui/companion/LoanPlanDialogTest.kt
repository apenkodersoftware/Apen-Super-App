package com.aistudio.fasmovapos.app.ui.companion

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aistudio.fasmovapos.app.data.companion.LoanPlan
import com.aistudio.fasmovapos.app.data.companion.LoanProduct
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoanPlanDialogTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun validScheduleEnablesOneSaveWithExactAmount() {
    val saved = mutableListOf<LoanPlan>()
    compose.setContent {
      MaterialTheme {
        LoanPlanDialog(LoanProduct("Inua Loan", "Biashara", "Inua Loan", "KES 8,000 – 100,000", "4 weeks"), {}, saved::add)
      }
    }
    compose.onNodeWithText("Save my note").assertIsNotEnabled()
    compose.onNodeWithTag("apen_plan_amount").performTextInput("123.45")
    compose.onNodeWithTag("apen_plan_date").performTextInput("2026-11-02")
    compose.onNodeWithText("Save my note").assertIsEnabled().performClick()
    compose.onNodeWithText("Save my note").assertIsNotEnabled()
    compose.runOnIdle {
      assertEquals(1, saved.size)
      assertEquals(12345L, saved.single().amountCents)
      assertEquals("2026-11-02", saved.single().dueDate)
    }
  }
}
