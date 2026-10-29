package com.aistudio.fasmovapos.app.ui.components

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReceiptShareTest {
  private val context = ApplicationProvider.getApplicationContext<Application>()

  private val sale =
    SaleEntity(
      id = "s-1",
      transactionRef = "TXN-123456",
      branchId = "b-1",
      cashierId = "u-1",
      cashierName = "Njeri",
      totalAmount = 890.0,
      discountAmount = 0.0,
      taxAmount = 122.76,
      netAmount = 890.0,
      paymentMethod = "MPESA",
      paymentReference = "QKJ9821LXM",
      customerId = null,
      customerName = null,
      timestamp = 0L,
    )
  private val items =
    listOf(
      SaleItemEntity(
        saleId = "s-1",
        productId = "p-1",
        productName = "Cement 50kg",
        quantity = 1,
        unitPrice = 890.0,
        totalPrice = 890.0,
      ),
    )

  @Test
  fun contactLine_printsOnlyWhatWasEntered() {
    val none = BranchEntity("b-1", "Shop", "HQ-01", "", true)
    assertEquals("", receiptContactLine(none))
    assertEquals("TEL: 0712345678", receiptContactLine(none.copy(phone = " 0712345678 ")))
    assertEquals("KRA PIN: A001234567Z", receiptContactLine(none.copy(taxPin = "A001234567Z")))
    assertEquals("KRA PIN: A001234567Z | TEL: 0712345678", receiptContactLine(none.copy(phone = "0712345678", taxPin = "A001234567Z")))
  }

  @Test
  fun shareText_usesTheBusinessOwnDetails_andNeverAMadeUpTaxPin() {
    val branch = BranchEntity("b-1", "Mama Njeri Shop", "HQ-01", "Kilifi", true)

    val text = receiptShareText(context, sale, items, emptyList(), branch)

    assertTrue(text.startsWith("MAMA NJERI SHOP"))
    assertTrue(text.contains("TXN-123456"))
    assertTrue(text.contains("QKJ9821LXM"))
    assertTrue(text.contains("890.00"))
    assertFalse(text.contains("KRA PIN"))
    assertFalse(text.contains("P0512849302Z"))
    assertFalse(text.contains("RETAIL STORE"))
  }

  @Test
  fun shareText_includesTheKraPinAndPhoneWhenEntered() {
    val branch = BranchEntity("b-1", "Mama Njeri Shop", "HQ-01", "", true, phone = "0712345678", taxPin = "A001234567Z")

    val text = receiptShareText(context, sale, items, emptyList(), branch)

    assertTrue(text.contains("KRA PIN: A001234567Z | TEL: 0712345678"))
  }

  @Test
  fun creditReceiptShowsTotalWithoutClaimingTheCustomerPaid() {
    val branch = BranchEntity("b-1", "Shop", "HQ-01", "", true)
    val credit = sale.copy(paymentMethod = "CREDIT", paymentReference = "", customerId = "c-1", customerName = "Asha")
    val text = receiptShareText(context, credit, items, emptyList(), branch)
    assertTrue(text.contains("Asha"))
    assertTrue(text.contains("Total KES 890.00"))
    assertFalse(text.contains("total paid", ignoreCase = true))
  }
}
