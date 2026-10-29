package com.aistudio.fasmovapos.app.ui.simple

import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.CustomerPaymentEntity
import com.aistudio.fasmovapos.app.data.db.entities.ExpenseEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import java.math.BigDecimal
import java.util.Calendar
import java.util.TimeZone

enum class ReportPeriod { DAY, WEEK, MONTH, YEAR }

data class ReportWindow(
  val start: Long,
  val end: Long,
) {
  fun contains(time: Long): Boolean = time >= start && time < end
}

data class ReportInput(
  val sales: List<SaleEntity>,
  val tenders: List<SalePaymentEntity>,
  val expenses: List<ExpenseEntity>,
  val collections: List<CustomerPaymentEntity>,
  val customers: List<CustomerEntity>,
)

data class BusinessReport(
  val sales: Double,
  val expenses: Double,
  val received: Double,
  val credit: Double,
  val collected: Double,
  val outstanding: Double,
) {
  val estimate: Double get() = (BigDecimal.valueOf(sales) - BigDecimal.valueOf(expenses)).toDouble()
  val netReceipts: Double get() = (BigDecimal.valueOf(received) - BigDecimal.valueOf(expenses)).toDouble()
}

fun reportWindow(
  period: ReportPeriod,
  now: Long,
  zone: TimeZone = TimeZone.getDefault(),
): ReportWindow {
  val start =
    Calendar.getInstance(zone).apply {
      timeInMillis = now
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      when (period) {
        ReportPeriod.DAY -> Unit
        ReportPeriod.WEEK -> {
          firstDayOfWeek = Calendar.MONDAY
          set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        ReportPeriod.MONTH -> set(Calendar.DAY_OF_MONTH, 1)
        ReportPeriod.YEAR -> set(Calendar.DAY_OF_YEAR, 1)
      }
    }
  val end =
    (start.clone() as Calendar).apply {
      add(
        when (period) {
          ReportPeriod.DAY -> Calendar.DAY_OF_YEAR
          ReportPeriod.WEEK -> Calendar.WEEK_OF_YEAR
          ReportPeriod.MONTH -> Calendar.MONTH
          ReportPeriod.YEAR -> Calendar.YEAR
        },
        1,
      )
    }
  return ReportWindow(start.timeInMillis, end.timeInMillis)
}

fun businessReport(
  data: ReportInput,
  window: ReportWindow,
): BusinessReport {
  fun Iterable<Double>.moneySum(): Double = fold(BigDecimal.ZERO) { sum, value -> sum + BigDecimal.valueOf(value) }.toDouble()
  val sales = data.sales.filter { window.contains(it.timestamp) }
  val legs = data.tenders.groupBy { it.saleId }
  var received = BigDecimal.ZERO
  var credit = BigDecimal.ZERO
  sales.forEach { sale ->
    val tenders = legs[sale.id].orEmpty()
    if (tenders.isEmpty()) {
      if (sale.paymentMethod == "CREDIT") credit += BigDecimal.valueOf(sale.netAmount) else received += BigDecimal.valueOf(sale.netAmount)
    } else {
      tenders.forEach { if (it.method == "CREDIT") credit += BigDecimal.valueOf(it.amount) else received += BigDecimal.valueOf(it.amount) }
    }
  }
  val collected =
    data.collections
      .filter { window.contains(it.timestamp) }
      .map { it.amountPaid }
      .moneySum()
  return BusinessReport(
    sales.map { it.netAmount }.moneySum(),
    data.expenses
      .filter { window.contains(it.timestamp) }
      .map { it.amount }
      .moneySum(),
    (received + BigDecimal.valueOf(collected)).toDouble(),
    credit.toDouble(),
    collected,
    data.customers.map { it.currentDebt }.moneySum(),
  )
}
