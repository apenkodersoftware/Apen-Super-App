package com.aistudio.fasmovapos.app.ui.simple

import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.CustomerPaymentEntity
import com.aistudio.fasmovapos.app.data.db.entities.ExpenseEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BusinessReportTest {
  private val zone = TimeZone.getTimeZone("Africa/Nairobi")

  private fun at(
    day: Int,
    hour: Int = 12,
  ): Long =
    Calendar
      .getInstance(zone)
      .apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, 0)
      }.timeInMillis

  private fun sale(
    id: String,
    amount: Double,
    time: Long,
  ) = SaleEntity(id, id, "b-1", "u-1", "Owner", amount, 0.0, 0.0, amount, "SPLIT", "", "buyer", "Amina", time)

  @Test
  fun partialCreditAndOldDebtCollectionsNeverBecomeAdditionalSales() {
    val today = sale("sale", 280.0, at(26))
    val data =
      ReportInput(
        listOf(today, sale("old", 500.0, at(25))),
        listOf(
          SalePaymentEntity(saleId = "sale", method = "CASH", amount = 100.0),
          SalePaymentEntity(saleId = "sale", method = "CREDIT", amount = 180.0),
        ),
        listOf(ExpenseEntity("exp", "E1", "b-1", "Transport", 30.0, "", "Owner", at(26))),
        listOf(CustomerPaymentEntity("pay", "buyer", "Amina", 50.0, "CASH", "ref", at(26))),
        listOf(CustomerEntity("buyer", "Amina", "", "", currentDebt = 130.0)),
      )
    val result = businessReport(data, reportWindow(ReportPeriod.DAY, at(26), zone))
    assertEquals(280.0, result.sales, 0.0)
    assertEquals(180.0, result.credit, 0.0)
    assertEquals(150.0, result.received, 0.0)
    assertEquals(50.0, result.collected, 0.0)
    assertEquals(130.0, result.outstanding, 0.0)
    assertEquals(250.0, result.estimate, 0.0)
    assertEquals(120.0, result.netReceipts, 0.0)
  }

  @Test
  fun dayWeekMonthAndYearUseMatchingExclusiveBoundaries() {
    for (period in ReportPeriod.entries) {
      val window = reportWindow(period, at(26), zone)
      assertTrue(window.contains(window.start))
      assertTrue(window.contains(window.end - 1))
      assertFalse(window.contains(window.start - 1))
      assertFalse(window.contains(window.end))
    }
    assertEquals(at(26, 0), reportWindow(ReportPeriod.WEEK, at(26), zone).start)
    assertEquals(at(1, 0), reportWindow(ReportPeriod.MONTH, at(26), zone).start)
  }

  @Test
  fun emptySalesWithExpenseShowLossAndDecimalMoneyIsStable() {
    val data =
      ReportInput(
        emptyList(),
        emptyList(),
        listOf(ExpenseEntity("e", "E", "b-1", "Rent", 30.25, "", "Owner", at(26))),
        emptyList(),
        emptyList(),
      )
    val result = businessReport(data, reportWindow(ReportPeriod.DAY, at(26), zone))
    assertEquals(-30.25, result.estimate, 0.0)
    assertEquals(-30.25, result.netReceipts, 0.0)
  }
}
