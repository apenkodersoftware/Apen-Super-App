package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.CustomerPaymentEntity
import com.aistudio.fasmovapos.app.data.payment.isValidMobileCode
import com.aistudio.fasmovapos.app.ui.components.paymentCodeLabel
import com.aistudio.fasmovapos.app.ui.language.personLabel
import com.aistudio.fasmovapos.app.ui.simple.DebtHistoryDialog
import com.aistudio.fasmovapos.app.ui.simple.DebtHistoryState
import com.aistudio.fasmovapos.app.ui.simple.DebtRecords
import com.aistudio.fasmovapos.app.ui.simple.debtHistory

@Composable
fun CustomersScreen(
  customers: List<CustomerEntity>,
  payments: List<CustomerPaymentEntity>,
  onAddCustomer: (String, String, String, Double) -> Unit,
  onRecordPayment: (CustomerEntity, Double, String, String) -> Unit,
  records: DebtRecords = DebtRecords(),
) {
  var historyId by remember { mutableStateOf<String?>(null) }
  var showAddCustomerDialog by remember { mutableStateOf(false) }
  var selectedPaymentCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

  val totalDebts = customers.sumOf { it.currentDebt }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(16.dp).testTag("customer_list"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              stringResource(R.string.total_outstanding_customer_credit),
              modifier = Modifier.fillMaxWidth(),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              "KES ${String.format("%.2f", totalDebts)}",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.error,
            )
          }

          Button(
            onClick = { showAddCustomerDialog = true },
            modifier = Modifier.fillMaxWidth().testTag("add_customer_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.register_customer), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          }
        }
      }
    }
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        stringResource(R.string.customer_credit_accounts),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      )
    }
    items(customers, key = { "customer:${it.id}" }) { cust ->
      CustomerRow(customer = cust, onPayDebt = { selectedPaymentCustomer = cust }, onHistory = { historyId = cust.id })
    }
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        stringResource(R.string.recent_debt_recovery_receipts),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      )
    }
    items(payments, key = { "payment:${it.id}" }) { p ->
      PaymentRow(payment = p)
    }
  }

  customers.firstOrNull { it.id == historyId }?.let { customer ->
    DebtHistoryDialog(customer, DebtHistoryState(debtHistory(customer.id, records.sales, records.payments, payments))) { historyId = null }
  }

  if (showAddCustomerDialog) {
    AddCustomerDialog(
      onDismiss = { showAddCustomerDialog = false },
      onConfirm = { name, phone, email, limit ->
        onAddCustomer(name, phone, email, limit)
        showAddCustomerDialog = false
      },
    )
  }

  selectedPaymentCustomer?.let { cust ->
    RecordPaymentDialog(
      customer = cust,
      onDismiss = { selectedPaymentCustomer = null },
      onConfirm = { amount, method, ref ->
        onRecordPayment(cust, amount, method, ref)
        selectedPaymentCustomer = null
      },
    )
  }
}

@Composable
private fun CustomerRow(
  customer: CustomerEntity,
  onPayDebt: () -> Unit,
  onHistory: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          personLabel(customer.id, customer.name),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          if (customer.phone.isBlank()) {
            stringResource(R.string.r2_no_phone, String.format(java.util.Locale.getDefault(), "%,.0f", customer.totalSpent))
          } else {
            stringResource(
              R.string.phone_lifetime_kes,
              customer.phone,
              String.format(java.util.Locale.getDefault(), "%,.0f", customer.totalSpent),
            )
          },
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          stringResource(R.string.credit_limit_kes, String.format("%.0f", customer.creditLimit)),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
        )
      }

      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = stringResource(R.string.current_debt_kes, String.format("%.2f", customer.currentDebt)),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
          color = if (customer.currentDebt > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onHistory, modifier = Modifier.fillMaxWidth().testTag("lite_history_${customer.id}")) {
          Text(stringResource(R.string.lite_statement))
        }
        if (customer.currentDebt > 0) {
          Button(
            onClick = onPayDebt,
            modifier = Modifier.fillMaxWidth().testTag("record_payment_button_${customer.id}"),
          ) {
            Text(
              stringResource(R.string.record_payment),
              fontSize = 16.sp,
              modifier = Modifier.fillMaxWidth(),
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PaymentRow(payment: CustomerPaymentEntity) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column {
        Text(
          stringResource(R.string.received_from, personLabel(payment.customerId, payment.customerName)),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          stringResource(R.string.method, paymentCodeLabel(payment.paymentMethod), payment.reference),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      Text(
        "KES ${String.format("%.2f", payment.amountPaid)}",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun AddCustomerDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Double) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var creditLimitStr by remember { mutableStateOf("50000") }

  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
          stringResource(R.string.register_customer_account),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        OutlinedTextField(value = name, onValueChange = {
          name = it
        }, label = {
          Text(
            stringResource(R.string.customer_full_name_business),
            modifier = Modifier.fillMaxWidth(),
          )
        }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("customer_name"))
        OutlinedTextField(value = phone, onValueChange = {
          phone = it
        }, label = {
          Text(
            stringResource(R.string.simple_phone_optional),
            modifier = Modifier.fillMaxWidth(),
          )
        }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("customer_phone"))
        OutlinedTextField(value = email, onValueChange = {
          email = it
        }, label = {
          Text(stringResource(R.string.email_address), modifier = Modifier.fillMaxWidth())
        }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
          value = creditLimitStr,
          onValueChange = {
            creditLimitStr = it
          },
          label = {
            Text(stringResource(R.string.approved_credit_limit_kes), modifier = Modifier.fillMaxWidth())
          },
          keyboardOptions =
            KeyboardOptions(
              keyboardType = KeyboardType.Number,
            ),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
          TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          }
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = {
              if (name.isNotBlank()) {
                onConfirm(name.trim(), phone.trim(), email.trim(), creditLimitStr.toDouble())
              }
            },
            enabled =
              name.isNotBlank() &&
                creditLimitStr.toBigDecimalOrNull()?.let {
                  it.signum() >= 0 &&
                    it.stripTrailingZeros().scale() <= 2 &&
                    it.toDouble().isFinite()
                } ==
                true,
            modifier = Modifier.fillMaxWidth().testTag("confirm_customer_button"),
          ) {
            Text(stringResource(R.string.save_account), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          }
        }
      }
    }
  }
}

@Composable
private fun RecordPaymentDialog(
  customer: CustomerEntity,
  onDismiss: () -> Unit,
  onConfirm: (Double, String, String) -> Unit,
) {
  var amountStr by remember { mutableStateOf(customer.currentDebt.toString()) }
  var method by remember { mutableStateOf("CASH") }
  var reference by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
          stringResource(R.string.record_customer_payment, personLabel(customer.id, customer.name)),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          stringResource(R.string.current_outstanding_debt_kes, String.format("%.2f", customer.currentDebt)),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it },
          label = { Text(stringResource(R.string.amount_paid_kes), modifier = Modifier.fillMaxWidth()) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("lite_repayment_amount"),
        )

        OutlinedTextField(
          value = method,
          onValueChange = { method = it },
          label = { Text(stringResource(R.string.payment_method_mpesa_cash_bank), modifier = Modifier.fillMaxWidth()) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = reference,
          onValueChange = { reference = it },
          label = { Text(stringResource(R.string.m_pesa_bank_reference_ref), modifier = Modifier.fillMaxWidth()) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
          TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel), modifier = Modifier.fillMaxWidth())
          }
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = {
              val amt = amountStr.toDoubleOrNull() ?: 0.0
              if (amt > 0 && amt <= customer.currentDebt) {
                onConfirm(amt, method.uppercase().trim(), reference)
              }
            },
            enabled =
              amountStr.toBigDecimalOrNull()?.let {
                it.signum() > 0 &&
                  it.stripTrailingZeros().scale() <= 2 &&
                  it.toDouble() <= customer.currentDebt
              } ==
                true &&
                method.uppercase().trim() in listOf("CASH", "MPESA", "BANK") &&
                (method.uppercase().trim() != "MPESA" || isValidMobileCode(reference)),
            modifier = Modifier.testTag("lite_save_repayment"),
          ) {
            Text(stringResource(R.string.record_payment), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          }
        }
      }
    }
  }
}
