package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import com.aistudio.fasmovapos.app.ui.components.paymentSummary
import com.aistudio.fasmovapos.app.ui.language.personLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesHistoryScreen(
  sales: List<SaleEntity>,
  salePayments: List<SalePaymentEntity>,
  onReprintReceipt: (SaleEntity) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }

  val filteredSales =
    sales.filter {
      searchQuery.isBlank() ||
        it.transactionRef.contains(searchQuery, ignoreCase = true) ||
        it.paymentReference.contains(searchQuery, ignoreCase = true) ||
        it.cashierName.contains(searchQuery, ignoreCase = true) ||
        (it.customerName != null && it.customerName.contains(searchQuery, ignoreCase = true))
    }

  val totalVolume = sales.sumOf { it.netAmount }
  val legsBySale = salePayments.groupBy({ it.saleId }, { it.method })

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp),
  ) {
    // Summary Header Card
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            stringResource(R.string.total_processed_transactions),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            stringResource(R.string.sales, sales.size),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            stringResource(R.string.total_net_sales_revenue),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            "KES ${String.format("%.2f", totalVolume)}",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Search Bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text(stringResource(R.string.search_by_receipt_ref_customer_or)) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
      singleLine = true,
      modifier =
        Modifier
          .fillMaxWidth()
          .testTag("sales_search_field"),
      shape = RoundedCornerShape(12.dp),
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Sales List
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(filteredSales) { sale ->
        val legs = legsBySale[sale.id].orEmpty()
        val summary = paymentSummary(sale.paymentMethod, legs)
        SaleHistoryRow(
          paymentLabel = if (legs.size > 1) summary else "$summary (${sale.paymentReference.take(10)})",
          sale = sale,
          onReprint = { onReprintReceipt(sale) },
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaleHistoryRow(
  paymentLabel: String,
  sale: SaleEntity,
  onReprint: () -> Unit,
) {
  val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
  val dateStr = dateFormat.format(Date(sale.timestamp))

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable { onReprint() },
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1.5f)) {
        FlowRow(
          verticalArrangement = Arrangement.spacedBy(4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = sale.transactionRef,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        Text(
          text = stringResource(R.string.cashier, dateStr, personLabel(sale.cashierId, sale.cashierName)),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
        )

        if (!sale.customerName.isNullOrBlank()) {
          Text(
            text = stringResource(R.string.customer_2, personLabel(sale.customerId, sale.customerName.orEmpty())),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "KES ${String.format("%.2f", sale.netAmount)}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = paymentLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      TextButton(onClick = onReprint) {
        Icon(Icons.Default.Receipt, contentDescription = stringResource(R.string.receipt), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(2.dp))
        Text(stringResource(R.string.receipt), fontSize = 11.sp)
      }
    }
  }
}
