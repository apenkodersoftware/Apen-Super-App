package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.payment.PaymentState
import com.aistudio.fasmovapos.app.data.repository.CartItem
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class SimpleChargeTest {
  @get:Rule val rule = createComposeRule()
  private var payment by mutableStateOf(PaymentState())
  private var customer by mutableStateOf<CustomerEntity?>(null)
  private var saving by mutableStateOf(false)
  private var completed = 0
  private val buyer = CustomerEntity("c1", "Amina", "", "")
  private val actions =
    ChargeActions(
      changeQuantity = { _, _ -> },
      remove = {},
      updatePayment = { payment = it(payment) },
      selectCustomer = { customer = it },
      openCustomers = {},
      complete = {
        completed++
        saving = true
      },
      back = {},
    )

  private fun showCharge() {
    rule.setContent {
      FasMovaPOSTheme {
        SimpleChargeScreen(
          ChargeState(listOf(CartItem(simpleProduct("bucket", "Bucket"), 1)), payment, listOf(buyer), customer, saving),
          actions,
        )
      }
    }
  }

  @Test
  fun cashIsReadyWithoutCustomerAndSavingDisablesFurtherChanges() {
    showCharge()
    rule.onNodeWithTag("simple_payment_cash").assertIsSelected()
    rule.onNodeWithText("Choose customer").assertDoesNotExist()
    rule.onNodeWithText("Phone (optional)").assertDoesNotExist()
    rule.onNodeWithText("Airtel Money").assertDoesNotExist()
    rule.onNodeWithText("Card").assertDoesNotExist()
    rule.onNodeWithTag("simple_complete_sale").assertIsEnabled().performClick()
    assertEquals(1, completed)
    rule.onNodeWithTag("simple_complete_sale").assertIsNotEnabled()
    rule.onNodeWithTag("simple_plus_bucket").assertIsNotEnabled()
  }

  @Test
  fun mpesaPhoneIsOptionalButTheExistingCodeGuardIsPreserved() {
    showCharge()
    rule.onNodeWithTag("simple_charge_list").performScrollToNode(hasTestTag("simple_payment_mpesa"))
    rule.onNodeWithTag("simple_payment_mpesa").performClick()
    rule.onNodeWithTag("simple_complete_sale").assertIsNotEnabled()
    rule.onNodeWithTag("simple_charge_list").performScrollToNode(hasTestTag("simple_mpesa_code"))
    rule.onNodeWithTag("simple_mpesa_code").performTextInput("QKJ9821LXM")
    rule.onNodeWithTag("simple_complete_sale").assertIsEnabled()
    assertEquals("", payment.phone)
  }

  @Test
  fun creditRequiresASelectedCustomerAndCashClearsTheHiddenSelection() {
    showCharge()
    rule.onNodeWithTag("simple_charge_list").performScrollToNode(hasTestTag("simple_payment_credit"))
    rule.onNodeWithTag("simple_payment_credit").performClick()
    rule.onNodeWithTag("simple_complete_sale").assertIsNotEnabled()
    rule.onNodeWithTag("simple_charge_list").performScrollToNode(hasTestTag("simple_choose_customer"))
    rule.onNodeWithTag("simple_choose_customer").performClick()
    rule.onNodeWithText("Amina").performClick()
    rule.onNodeWithTag("simple_complete_sale").assertIsEnabled()
    rule.onNodeWithTag("simple_charge_list").performScrollToNode(hasTestTag("simple_payment_cash"))
    rule.onNodeWithTag("simple_payment_cash").performClick()
    assertNull(customer)
  }

  @Test
  fun validSplitDraftRemainsVisibleAndCanReturnToCash() {
    payment = PaymentState(isSplit = true, splitAmountA = "50", splitReferenceB = "QKJ9821LXM")
    showCharge()
    rule.onNodeWithTag("simple_charge_list").performScrollToNode(hasTestTag("lite_split_code_b"))
    rule.onNodeWithTag("lite_split_code_b").assertIsDisplayed()
    rule.onNodeWithTag("simple_complete_sale").assertIsEnabled()
    rule.onNodeWithTag("simple_charge_list").performScrollToNode(hasTestTag("simple_payment_cash"))
    rule.onNodeWithTag("simple_payment_cash").performClick()
    rule.onNodeWithTag("simple_complete_sale").assertIsEnabled()
    assertEquals(false, payment.isSplit)
  }

  @Test
  fun stickyCartShowsTheCountAndAmountAndOpensCharge() {
    var opened = false
    rule.setContent { FasMovaPOSTheme { SimpleCartBar(3, 560.0, { opened = true }) } }
    rule.onNodeWithText("3 items - KES 560").assertIsDisplayed()
    rule.onNodeWithTag("simple_charge_bar").performClick()
    assertEquals(true, opened)
  }
}
