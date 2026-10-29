package com.aistudio.fasmovapos.app.data.payment

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ManualMobileMoneyConfirmerTest {
  private val line = PaymentLine(PaymentMethod.MPESA, 500.0, reference = "QKJ9821LXM")

  @Test
  fun aFreshCode_isConfirmed() =
    runTest {
      val confirmer = ManualMobileMoneyConfirmer { false }

      assertEquals(ConfirmationResult.Confirmed, confirmer.confirm(line))
    }

  @Test
  fun aCodeThatAlreadyPaidAnotherSale_isRejected() =
    runTest {
      val confirmer = ManualMobileMoneyConfirmer { it == "QKJ9821LXM" }

      assertEquals(ConfirmationResult.Rejected(ConfirmationRejection.CODE_ALREADY_USED), confirmer.confirm(line))
    }
}
