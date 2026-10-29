package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.ExpenseEntity
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import com.aistudio.fasmovapos.app.data.payment.PaymentMethod
import com.aistudio.fasmovapos.app.ui.theme.FasMovaStatus

@Composable
fun AnalyticsScreen(
  sales: List<SaleEntity>,
  salePayments: List<SalePaymentEntity>,
  expenses: List<ExpenseEntity>,
  products: List<ProductEntity>,
) {
  val totalRevenue = sales.sumOf { it.netAmount }
  val totalExpenses = expenses.sumOf { it.amount }

  // Split sales count each leg under its own method; sales from before split payments use the sale itself.
  val legsBySale = salePayments.groupBy { it.saleId }
  val channelTotals = mutableMapOf<String, Double>()
  sales.forEach { sale ->
    val legs = legsBySale[sale.id]
    if (legs.isNullOrEmpty()) {
      channelTotals.merge(sale.paymentMethod, sale.netAmount, Double::plus)
    } else {
      legs.forEach { channelTotals.merge(it.method, it.amount, Double::plus) }
    }
  }
  val mpesaSales = channelTotals[PaymentMethod.MPESA.name] ?: 0.0
  val airtelSales = channelTotals[PaymentMethod.AIRTEL.name] ?: 0.0
  val cashSales = channelTotals[PaymentMethod.CASH.name] ?: 0.0
  val cardSales = channelTotals[PaymentMethod.CARD.name] ?: 0.0
  val creditSales = channelTotals[PaymentMethod.CREDIT.name] ?: 0.0

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
  ) {
    // Analytics Header
    Text(
      text = stringResource(R.string.business_reports_executive_insights),
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = stringResource(R.string.real_time_revenue_profit_loss_analysis),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Executive Metric Cards Grid
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ExecutiveMetric(
        title = stringResource(R.string.gross_revenue),
        value = "KES ${String.format("%.0f", totalRevenue)}",
        subtitle = stringResource(R.string.sales_completed, sales.size),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
      ExecutiveMetric(
        title = stringResource(R.string.est_net_profit),
        value = stringResource(R.string.profit_not_configured),
        subtitle = stringResource(R.string.profit_not_configured_hint),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
      )
      ExecutiveMetric(
        title = stringResource(R.string.operating_expenses),
        value = "KES ${String.format("%.0f", totalExpenses)}",
        subtitle = stringResource(R.string.logged_entries, expenses.size),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.weight(1f),
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Payment Method Breakdown
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          stringResource(R.string.revenue_by_payment_channel),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        Spacer(modifier = Modifier.height(12.dp))

        PaymentChannelBar(stringResource(R.string.m_pesa_mobile_money), mpesaSales, totalRevenue, FasMovaStatus.success)
        Spacer(modifier = Modifier.height(8.dp))
        PaymentChannelBar(stringResource(R.string.airtel_money), airtelSales, totalRevenue, Color(0xFFED1C24))
        Spacer(modifier = Modifier.height(8.dp))
        PaymentChannelBar(stringResource(R.string.cash_register), cashSales, totalRevenue, MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        PaymentChannelBar(stringResource(R.string.bank_card_pos), cardSales, totalRevenue, FasMovaStatus.info)
        Spacer(modifier = Modifier.height(8.dp))
        PaymentChannelBar(stringResource(R.string.store_credit_bnpl), creditSales, totalRevenue, FasMovaStatus.warning)
      }
    }
  }
}

@Composable
private fun ExecutiveMetric(
  title: String,
  value: String,
  subtitle: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
      Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
  }
}

@Composable
private fun PaymentChannelBar(
  channelName: String,
  amount: Double,
  totalRevenue: Double,
  color: Color,
) {
  val pct = if (totalRevenue > 0) (amount / totalRevenue).toFloat() else 0f

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(channelName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
      Text(
        "KES ${String.format("%.0f", amount)} (${String.format("%.0f", pct * 100)}%)",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(8.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth(pct.coerceIn(0.01f, 1f))
            .fillMaxHeight()
            .background(color, RoundedCornerShape(4.dp)),
      )
    }
  }
}
