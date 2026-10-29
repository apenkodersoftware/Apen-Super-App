package com.aistudio.fasmovapos.app.data.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentStateTest {
  @Test
  fun percentDiscountIsTakenOffTheSubtotal() {
    val state = PaymentState(discountMode = DiscountMode.PERCENT, discountInput = "10")
    assertEquals(250.0, state.discountAmount(2500.0), 0.001)
  }

  @Test
  fun flatDiscountCannotExceedSubtotal() {
    val state = PaymentState(discountMode = DiscountMode.FLAT, discountInput = "9999")
    assertEquals(500.0, state.discountAmount(500.0), 0.001)
  }

  @Test
  fun percentOver100IsCappedAndNoDiscountModeIsZero() {
    assertEquals(
      500.0,
      PaymentState(discountMode = DiscountMode.PERCENT, discountInput = "150").discountAmount(500.0),
      0.001,
    )
    assertEquals(0.0, PaymentState(discountInput = "50").discountAmount(500.0), 0.001)
  }

  @Test
  fun singleCashSaleIsOneLineForTheWholeTotal() {
    val lines = PaymentState(method = PaymentMethod.CASH, phone = "0712345678").lines(1000.0)
    assertEquals(1, lines.size)
    assertEquals(1000.0, lines.single().amount, 0.001)
    assertNull(lines.single().phoneNumber)
  }

  @Test
  fun mobileMoneyNeedsTheSmsConfirmationCode_notAPhoneNumber() {
    val noCode = PaymentState(method = PaymentMethod.AIRTEL, phone = "0733123456")
    assertEquals(PaymentIssueType.CODE_REQUIRED, noCode.validationError(1000.0, hasCustomer = false)?.type)
    assertNotNull(noCode.copy(reference = "ABC12").validationError(1000.0, hasCustomer = false)) // too short
    val withCode = PaymentState(method = PaymentMethod.AIRTEL, reference = "qkj 9821-lxm")
    assertNull(withCode.validationError(1000.0, hasCustomer = false))
    val line = withCode.lines(1000.0).single()
    assertEquals("QKJ9821LXM", line.reference) // stored normalised
    assertNull(line.phoneNumber) // the phone is optional
    assertEquals("0733123456", noCode.lines(1000.0).single().phoneNumber)
  }

  @Test
  fun cashAndCreditCarryNoCustomerCode() {
    assertEquals("", PaymentState(method = PaymentMethod.CASH, reference = "QKJ9821LXM").lines(1000.0).single().reference)
  }

  @Test
  fun creditNeedsACustomer() {
    val state = PaymentState(method = PaymentMethod.CREDIT)
    assertNotNull(state.validationError(1000.0, hasCustomer = false))
    assertNull(state.validationError(1000.0, hasCustomer = true))
  }

  @Test
  fun splitLinesAddUpToTheTotal() {
    val state =
      PaymentState(
        isSplit = true,
        splitMethodA = PaymentMethod.CASH,
        splitMethodB = PaymentMethod.MPESA,
        splitAmountA = "300",
        splitReferenceB = "QKJ9821LXM",
      )
    assertNull(state.validationError(1000.0, hasCustomer = false))
    val lines = state.lines(1000.0)
    assertEquals(listOf(300.0, 700.0), lines.map { it.amount })
    assertEquals(1000.0, lines.sumOf { it.amount }, 0.001)
  }

  @Test
  fun splitRejectsSameMethodZeroAndFullAmounts() {
    val base = PaymentState(isSplit = true, splitReferenceB = "QKJ9821LXM")
    assertNotNull(base.copy(splitMethodA = PaymentMethod.MPESA, splitAmountA = "100").validationError(1000.0, false))
    assertNotNull(base.copy(splitAmountA = "").validationError(1000.0, false))
    assertNotNull(base.copy(splitAmountA = "0").validationError(1000.0, false))
    assertNotNull(base.copy(splitAmountA = "1000").validationError(1000.0, false))
    assertNotNull(base.copy(splitAmountA = "1500").validationError(1000.0, false))
  }

  @Test
  fun splitMobileMoneyLegNeedsItsOwnCode() {
    val state = PaymentState(isSplit = true, splitMethodB = PaymentMethod.AIRTEL, splitAmountA = "200")
    assertNotNull(state.validationError(1000.0, hasCustomer = false))
    assertNull(state.copy(splitReferenceB = "PQR4567STU").validationError(1000.0, hasCustomer = false))
  }

  @Test
  fun partPaidCreditRequiresACustomerAndKeepsTheUnpaidLeg() {
    assertEquals(true, PaymentMethod.CREDIT.canSplit)
    val state = PaymentState(isSplit = true, splitMethodB = PaymentMethod.CREDIT, splitAmountA = "100")
    assertNotNull(state.validationError(280.0, hasCustomer = false))
    assertNull(state.validationError(280.0, hasCustomer = true))
    assertEquals(180.0, state.lines(280.0).last().amount, 0.0)
    assertEquals(true, PaymentMethod.AIRTEL.canSplit)
  }

  @Test
  fun storedCodesMapToMethods() {
    assertEquals(PaymentMethod.AIRTEL, paymentMethodOf("AIRTEL"))
    assertEquals(PaymentMethod.MPESA, paymentMethodOf("MPESA"))
    assertNull(paymentMethodOf(SPLIT_PAYMENT_CODE))
  }
}
