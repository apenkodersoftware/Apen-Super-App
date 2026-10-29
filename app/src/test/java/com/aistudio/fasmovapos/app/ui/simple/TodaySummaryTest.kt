package com.aistudio.fasmovapos.app.ui.simple

import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TodaySummaryTest {
  private val zone = TimeZone.getTimeZone("Africa/Nairobi")

  private fun at(
    day: Int,
    hour: Int = 0,
  ): Long =
    Calendar
      .getInstance(zone)
      .apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, 0, 0)
      }.timeInMillis

  private fun sale(
    id: String,
    time: Long,
    amount: Double,
    method: String = "CASH",
  ): SaleEntity =
    SaleEntity(
      id = id,
      transactionRef = id,
      branchId = "b-1",
      cashierId = "u-1",
      cashierName = "Shop",
      totalAmount = amount,
      discountAmount = 0.0,
      taxAmount = 0.0,
      netAmount = amount,
      paymentMethod = method,
      paymentReference = "",
      customerId = null,
      customerName = null,
      timestamp = time,
    )

  @Test
  fun localMidnightIsIncludedAndTheNextMidnightIsExcluded() {
    val sales =
      listOf(
        sale("yesterday", at(24) - 1, 100.0),
        sale("start", at(24), 2.0),
        sale("last", at(25) - 1, 3.0),
        sale("tomorrow", at(25), 200.0),
      )
    val result = todaySummary(sales, emptyList(), at(24, 12), zone)
    assertEquals(listOf("last", "start"), result.sales.map { it.id })
    assertEquals(5.0, result.total, 0.0)
    assertEquals(5.0, result.cash, 0.0)
  }

  @Test
  fun splitLegsAndLegacySalesReconcileWithoutDroppingOtherMethods() {
    val sales =
      listOf(
        sale("split", at(24, 10), 100.0, "SPLIT"),
        sale("credit", at(24, 11), 40.0, "CREDIT"),
        sale("airtel", at(24, 9), 10.0, "AIRTEL"),
      )
    val payments =
      listOf(
        SalePaymentEntity(saleId = "split", method = "CASH", amount = 30.0),
        SalePaymentEntity(saleId = "split", method = "MPESA", amount = 70.0),
      )
    val result = todaySummary(sales, payments, at(24, 12), zone)
    assertEquals(3, result.sales.size)
    assertEquals(150.0, result.total, 0.0)
    assertEquals(30.0, result.cash, 0.0)
    assertEquals(70.0, result.mpesa, 0.0)
    assertEquals(40.0, result.credit, 0.0)
    assertEquals(10.0, result.other, 0.0)
  }

  @Test
  fun decimalTotalsDoNotAccumulateFloatingPointNoise() {
    val result = todaySummary(listOf(sale("a", at(24), 0.1), sale("b", at(24, 1), 0.2)), emptyList(), at(24, 12), zone)
    assertEquals(0.3, result.total, 0.0)
    assertEquals(0.3, result.cash, 0.0)
  }
}
