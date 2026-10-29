package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.payment.PaymentMethod

@Composable
fun LiteSplitFields(
  state: ChargeState,
  actions: ChargeActions,
  total: Double,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(stringResource(R.string.lite_received_by))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(PaymentMethod.CASH, PaymentMethod.MPESA).forEach { method ->
        FilterChip(
          selected = state.payment.splitMethodA == method,
          onClick = { actions.updatePayment { it.copy(splitMethodA = method) } },
          enabled = !state.saving,
          label = { Text(stringResource(method.labelRes)) },
        )
      }
    }
    OutlinedTextField(
      value = state.payment.splitAmountA,
      onValueChange = { value ->
        actions.updatePayment { it.copy(splitAmountA = value) }
      },
      label = { Text(stringResource(R.string.lite_received_now)) },
      enabled = !state.saving,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("lite_received_now"),
    )
    if (state.payment.splitMethodA == PaymentMethod.MPESA) {
      OutlinedTextField(
        value = state.payment.splitReferenceA,
        onValueChange = { value ->
          actions.updatePayment { it.copy(splitReferenceA = value) }
        },
        label = { Text(stringResource(R.string.simple_code_required)) },
        enabled = !state.saving,
        modifier = Modifier.fillMaxWidth().testTag("lite_split_code_a"),
      )
    }
    Text(stringResource(R.string.lite_balance, simpleMoney(state.payment.splitAmountB(total))))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf(PaymentMethod.CREDIT, PaymentMethod.MPESA, PaymentMethod.CASH).forEach { method ->
        FilterChip(
          selected = state.payment.splitMethodB == method,
          onClick = { actions.updatePayment { it.copy(splitMethodB = method) } },
          enabled = !state.saving,
          label = { Text(stringResource(method.labelRes)) },
        )
      }
    }
    if (state.payment.splitMethodB == PaymentMethod.MPESA) {
      OutlinedTextField(
        value = state.payment.splitReferenceB,
        onValueChange = { value ->
          actions.updatePayment { it.copy(splitReferenceB = value) }
        },
        label = { Text(stringResource(R.string.simple_code_required)) },
        enabled = !state.saving,
        modifier = Modifier.fillMaxWidth().testTag("lite_split_code_b"),
      )
    }
  }
}
