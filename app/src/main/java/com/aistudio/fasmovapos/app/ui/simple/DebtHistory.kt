package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.CustomerPaymentEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import java.text.DateFormat
import java.util.Date

data class DebtRecords(
  val sales: List<SaleEntity> = emptyList(),
  val payments: List<SalePaymentEntity> = emptyList(),
)

data class DebtHistoryState(
  val entries: List<DebtEntry>,
)

data class DebtEntry(
  val id: String,
  val timestamp: Long,
  val amount: Double,
  val repayment: Boolean,
)

fun debtHistory(
  customerId: String,
  sales: List<SaleEntity>,
  tenders: List<SalePaymentEntity>,
  repayments: List<CustomerPaymentEntity>,
): List<DebtEntry> {
  val bySale = tenders.groupBy { it.saleId }
  val purchases =
    sales.filter { it.customerId == customerId }.mapNotNull { sale ->
      val legs = bySale[sale.id].orEmpty()
      val credit =
        if (legs.isEmpty() &&
          sale.paymentMethod == "CREDIT"
        ) {
          sale.netAmount
        } else {
          legs.filter { it.method == "CREDIT" }.sumOf { it.amount }
        }
      if (credit > 0) DebtEntry(sale.transactionRef, sale.timestamp, credit, false) else null
    }
  return (purchases + repayments.filter { it.customerId == customerId }.map { DebtEntry(it.id, it.timestamp, it.amountPaid, true) })
    .sortedWith(compareBy<DebtEntry> { it.timestamp }.thenBy { it.id })
}

@Composable
fun DebtHistoryDialog(
  customer: CustomerEntity,
  state: DebtHistoryState,
  onClose: () -> Unit,
) {
  AlertDialog(
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.padding(16.dp),
    onDismissRequest = onClose,
    title = { Text(customer.name + " — " + stringResource(R.string.lite_statement)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.current_debt_kes, displayAmount(customer.currentDebt)))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
          items(state.entries, key = { it.id }) { entry ->
            Column {
              Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.timestamp)))
              Text(
                stringResource(
                  if (entry.repayment) R.string.lite_repayment else R.string.lite_debt_sale,
                ) + ": " + simpleMoney(entry.amount),
              )
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.simple_close)) } },
  )
}
