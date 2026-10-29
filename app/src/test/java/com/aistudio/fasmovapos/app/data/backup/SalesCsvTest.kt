package com.aistudio.fasmovapos.app.data.backup

import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class SalesCsvTest {
  private val utc = TimeZone.getTimeZone("UTC")

  private fun sale(
    ref: String,
    customer: String?,
    timestamp: Long,
  ) = SaleEntity(
    id = ref,
    transactionRef = ref,
    branchId = "b-1",
    cashierId = "u-1",
    cashierName = "Njeri",
    totalAmount = 1200.0,
    discountAmount = 100.0,
    taxAmount = 151.72,
    netAmount = 1100.0,
    paymentMethod = "MPESA",
    paymentReference = "QKJ9821LXM",
    customerId = null,
    customerName = customer,
    timestamp = timestamp,
  )

  @Test
  fun buildsHeaderAndOneRowPerSale_oldestFirst() {
    val csv = SalesCsv.build(listOf(sale("TXN-2", "Mama Njeri", 86_400_000L), sale("TXN-1", null, 0L)), utc)

    val lines = csv.trimEnd().split("\r\n")
    assertEquals("Date,Receipt,Cashier,Customer,Payment method,Payment reference,Gross,Discount,VAT,Net", lines[0])
    assertEquals("1970-01-01 00:00,TXN-1,Njeri,,MPESA,QKJ9821LXM,1200.00,100.00,151.72,1100.00", lines[1])
    assertEquals("1970-01-02 00:00,TXN-2,Njeri,Mama Njeri,MPESA,QKJ9821LXM,1200.00,100.00,151.72,1100.00", lines[2])
  }

  @Test
  fun commasAndQuotesAreEscaped_andFormulasAreDefused() {
    val csv = SalesCsv.build(listOf(sale("TXN-1", "Wanjiku, \"Mama\" & Sons", 0L), sale("TXN-2", "=HYPERLINK(\"http://evil\")", 1L)), utc)

    assertTrue(csv.contains("\"Wanjiku, \"\"Mama\"\" & Sons\""))
    assertTrue(csv.contains("\"'=HYPERLINK(\"\"http://evil\"\")\""))
  }
}
