package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import com.aistudio.fasmovapos.app.ui.components.paymentCodeLabel
import com.aistudio.fasmovapos.app.ui.language.personLabel
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class TodayData(
  val sales: List<SaleEntity>,
  val payments: List<SalePaymentEntity>,
  val reportInput: ReportInput = ReportInput(sales, payments, emptyList(), emptyList(), emptyList()),
)

data class TodaySummary(
  val sales: List<SaleEntity>,
  val total: Double,
  val cash: Double,
  val mpesa: Double,
  val credit: Double,
  val other: Double,
)

/** Read-only projection: local midnight to next local midnight, including stored legacy/split tenders. */
fun todaySummary(
  sales: List<SaleEntity>,
  payments: List<SalePaymentEntity>,
  now: Long,
  zone: TimeZone = TimeZone.getDefault(),
): TodaySummary {
  val calendar =
    Calendar.getInstance(zone).apply {
      timeInMillis = now
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
  val start = calendar.timeInMillis
  calendar.add(Calendar.DAY_OF_MONTH, 1)
  val end = calendar.timeInMillis
  val today = sales.filter { it.timestamp >= start && it.timestamp < end }.sortedByDescending { it.timestamp }
  val legs = payments.groupBy { it.saleId }
  val channels = mutableMapOf<String, BigDecimal>()
  today.forEach { sale ->
    val tender = legs[sale.id].orEmpty()
    if (tender.isEmpty()) {
      channels[sale.paymentMethod] = (channels[sale.paymentMethod] ?: BigDecimal.ZERO) + BigDecimal.valueOf(sale.netAmount)
    } else {
      tender.forEach { line -> channels[line.method] = (channels[line.method] ?: BigDecimal.ZERO) + BigDecimal.valueOf(line.amount) }
    }
  }

  fun amount(code: String): Double = channels[code]?.toDouble() ?: 0.0
  val total = today.fold(BigDecimal.ZERO) { sum, sale -> sum + BigDecimal.valueOf(sale.netAmount) }
  val other = channels.filterKeys { it !in setOf("CASH", "MPESA", "CREDIT") }.values.fold(BigDecimal.ZERO, BigDecimal::add)
  return TodaySummary(today, total.toDouble(), amount("CASH"), amount("MPESA"), amount("CREDIT"), other.toDouble())
}

@Composable
fun TodayScreen(
  data: TodayData,
  onHistory: () -> Unit,
  onReceipt: (SaleEntity) -> Unit,
  modifier: Modifier = Modifier,
  onExpense: () -> Unit = {},
  onDebts: () -> Unit = {},
) {
  val now by produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
      delay(60_000)
      value = System.currentTimeMillis()
    }
  }
  val summary = remember(data.sales, data.payments, now) { todaySummary(data.sales, data.payments, now) }
  LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item { Text(stringResource(R.string.lite_today_summary), style = MaterialTheme.typography.headlineSmall) }
    item { BusinessPanel(businessReport(data.reportInput, reportWindow(ReportPeriod.DAY, now))) }
    item { ReceiptsPanel(businessReport(data.reportInput, reportWindow(ReportPeriod.DAY, now))) }
    item { TextButton(onClick = onExpense, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.lite_add_expense)) } }
    item { TextButton(onClick = onDebts, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.lite_debts)) } }
    item { TextButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.lite_report)) } }
    item { Text(stringResource(R.string.lite_estimate_note)) }
    if (summary.sales.isEmpty()) {
      item { Text(stringResource(R.string.simple_no_sales_today), fontSize = 16.sp) }
    }
    items(summary.sales, key = { it.id }) { sale ->
      Card(onClick = { onReceipt(sale) }, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(simpleMoney(sale.netAmount), fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
          Text(
            stringResource(
              R.string.simple_sale_time,
              DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(sale.timestamp)),
              paymentCodeLabel(sale.paymentMethod),
            ),
            fontSize = 16.sp,
          )
          sale.customerName?.let { Text(personLabel(sale.customerId, it), fontSize = 16.sp) }
        }
      }
    }
    item {
      TextButton(
        onClick = onHistory,
        modifier = Modifier.fillMaxWidth(),
      ) { Text(stringResource(R.string.simple_full_reports), fontSize = 16.sp) }
    }
  }
}
