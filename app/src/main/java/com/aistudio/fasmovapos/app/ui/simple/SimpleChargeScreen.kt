package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.payment.DiscountMode
import com.aistudio.fasmovapos.app.data.payment.PaymentMethod
import com.aistudio.fasmovapos.app.data.payment.PaymentState
import com.aistudio.fasmovapos.app.data.repository.CartItem
import com.aistudio.fasmovapos.app.ui.components.text
import com.aistudio.fasmovapos.app.ui.language.personLabel
import com.aistudio.fasmovapos.app.ui.language.productLabel

data class ChargeState(
  val cart: List<CartItem>,
  val payment: PaymentState,
  val customers: List<CustomerEntity>,
  val customer: CustomerEntity?,
  val saving: Boolean,
)

class ChargeActions(
  val changeQuantity: (String, Int) -> Unit,
  val remove: (String) -> Unit,
  val updatePayment: ((PaymentState) -> PaymentState) -> Unit,
  val selectCustomer: (CustomerEntity?) -> Unit,
  val openCustomers: () -> Unit,
  val complete: () -> Unit,
  val back: () -> Unit,
)

@Composable
fun SimpleChargeScreen(
  state: ChargeState,
  actions: ChargeActions,
  modifier: Modifier = Modifier,
) {
  val gross = state.cart.sumOf { it.lineTotal }
  val total = (gross - state.payment.discountAmount(gross)).coerceAtLeast(0.0)
  val visibleMethod =
    if (state.payment.isSplit) {
      state.payment.splitMethodA in listOf(PaymentMethod.CASH, PaymentMethod.MPESA) &&
        state.payment.splitMethodB in listOf(PaymentMethod.CASH, PaymentMethod.MPESA, PaymentMethod.CREDIT)
    } else {
      state.payment.method in listOf(PaymentMethod.CASH, PaymentMethod.MPESA, PaymentMethod.CREDIT)
    }
  val issue = state.payment.validationError(total, state.customer != null)
  Column(modifier = modifier.fillMaxSize().imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    TextButton(onClick = actions.back, enabled = !state.saving) { Text(stringResource(R.string.simple_back_sell), fontSize = 16.sp) }
    LazyColumn(modifier = Modifier.weight(1f).testTag("simple_charge_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      items(state.cart, key = { it.lineKey }) { item ->
        SimpleCartRow(item, state.saving, actions)
      }
      item {
        HorizontalDivider()
        Text(stringResource(R.string.simple_total), fontSize = 18.sp)
        Text(
          simpleMoney(total),
          fontSize = 28.sp,
          lineHeight = 36.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.testTag("simple_charge_total"),
        )
      }
      item { SimplePaymentChoices(state, actions) }
      if (state.payment.isSplit) {
        item { LiteSplitFields(state, actions, total) }
      }
      if (!state.payment.isSplit && state.payment.method == PaymentMethod.MPESA) {
        item { SimpleMpesaFields(state, actions) }
      }
      if (state.payment.method == PaymentMethod.CREDIT || (state.payment.isSplit && state.payment.splitMethodB == PaymentMethod.CREDIT)) {
        item { SimpleCustomerPicker(state, actions) }
      }
      item { SimpleDiscount(state, actions) }
      if (!visibleMethod) {
        item { Text(stringResource(R.string.simple_choose_method), fontSize = 16.sp) }
      } else if (issue != null) {
        item { Text(issue.text(), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface) }
      }
    }
    Button(
      onClick = actions.complete,
      enabled = state.cart.isNotEmpty() && issue == null && visibleMethod && !state.saving,
      modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("simple_complete_sale"),
    ) {
      Text(stringResource(if (state.saving) R.string.simple_saving else R.string.simple_complete_sale), fontSize = 18.sp)
    }
  }
}

@Composable
private fun SimpleCartRow(
  item: CartItem,
  saving: Boolean,
  actions: ChargeActions,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(productLabel(item.product.id, item.product.name), fontSize = 18.sp, fontWeight = FontWeight.Medium)
    Text(simpleMoney(item.lineTotal), fontSize = 20.sp)
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = {
        actions.changeQuantity(item.lineKey, -if (item.product.quantityScale == 1) 1 else minOf(item.pricingQuantity, 250))
      }, enabled = !saving, modifier = Modifier.testTag("simple_minus_${item.product.id}")) {
        Icon(
          Icons.Default.Remove,
          contentDescription = stringResource(R.string.simple_decrease, productLabel(item.product.id, item.product.name)),
        )
      }
      Text(item.quantityLabel, fontSize = 20.sp)
      IconButton(onClick = {
        actions.changeQuantity(item.lineKey, if (item.product.quantityScale == 1) 1 else minOf(item.pricingQuantity, 250))
      }, enabled = !saving, modifier = Modifier.testTag("simple_plus_${item.product.id}")) {
        Icon(
          Icons.Default.Add,
          contentDescription = stringResource(R.string.simple_increase, productLabel(item.product.id, item.product.name)),
        )
      }
      TextButton(onClick = { actions.remove(item.lineKey) }, enabled = !saving) {
        Text(stringResource(R.string.simple_remove), fontSize = 16.sp)
      }
    }
  }
}

@Composable
private fun SimplePaymentChoices(
  state: ChargeState,
  actions: ChargeActions,
) {
  val methods =
    listOf(
      PaymentMethod.CASH to R.string.simple_cash,
      PaymentMethod.MPESA to R.string.simple_mpesa,
      PaymentMethod.CREDIT to R.string.simple_credit,
    )
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(stringResource(R.string.simple_pay_by), fontSize = 16.sp)
    OutlinedButton(
      onClick = {
        actions.updatePayment {
          it.copy(
            isSplit = true,
            splitMethodA = PaymentMethod.CASH,
            splitMethodB = PaymentMethod.CREDIT,
          )
        }
      },
      enabled = !state.saving,
      modifier = Modifier.fillMaxWidth().testTag("lite_part_payment"),
    ) {
      Text(stringResource(R.string.lite_part_payment))
    }
    methods.forEach { (method, label) ->
      OutlinedButton(
        onClick = {
          actions.updatePayment { it.copy(method = method, isSplit = false) }
          if (method != PaymentMethod.CREDIT) actions.selectCustomer(null)
        },
        enabled = !state.saving,
        colors =
          ButtonDefaults.outlinedButtonColors(
            containerColor =
              if (state.payment.method == method &&
                !state.payment.isSplit
              ) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.surface
              },
          ),
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics {
              selected =
                state.payment.method == method &&
                !state.payment.isSplit
            }.testTag("simple_payment_${method.name.lowercase()}"),
      ) {
        Text(stringResource(label), fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun SimpleMpesaFields(
  state: ChargeState,
  actions: ChargeActions,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(stringResource(R.string.simple_mpesa_notice), fontSize = 16.sp)
    OutlinedTextField(
      value = state.payment.phone,
      onValueChange = { value -> actions.updatePayment { it.copy(phone = value) } },
      label = { Text(stringResource(R.string.simple_phone_optional)) },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
      enabled = !state.saving,
      modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
      value = state.payment.reference,
      onValueChange = { value -> actions.updatePayment { it.copy(reference = value) } },
      label = { Text(stringResource(R.string.simple_code_required)) },
      singleLine = true,
      enabled = !state.saving,
      modifier = Modifier.fillMaxWidth().testTag("simple_mpesa_code"),
    )
  }
}

@Composable
private fun SimpleCustomerPicker(
  state: ChargeState,
  actions: ChargeActions,
) {
  var expanded by remember { mutableStateOf(false) }
  Column {
    Text(stringResource(R.string.simple_credit_notice), fontSize = 16.sp)
    Box {
      OutlinedButton(onClick = {
        expanded = true
      }, enabled = !state.saving, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("simple_choose_customer")) {
        Text(state.customer?.let { personLabel(it.id, it.name) } ?: stringResource(R.string.simple_choose_customer), fontSize = 16.sp)
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        state.customers.forEach { customer ->
          DropdownMenuItem(text = { Text(personLabel(customer.id, customer.name), fontSize = 16.sp) }, onClick = {
            actions.selectCustomer(customer)
            expanded = false
          })
        }
      }
    }
    TextButton(onClick = actions.openCustomers, enabled = !state.saving) {
      Text(stringResource(R.string.simple_manage_customers), fontSize = 16.sp)
    }
  }
}

@Composable
private fun SimpleDiscount(
  state: ChargeState,
  actions: ChargeActions,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    TextButton(onClick = { expanded = !expanded }, enabled = !state.saving) {
      Text(stringResource(if (expanded) R.string.simple_hide_discount else R.string.simple_add_discount), fontSize = 16.sp)
    }
    if (state.payment.discountMode != DiscountMode.NONE && !expanded) {
      Text(stringResource(R.string.simple_discount_applied), fontSize = 16.sp)
    }
    if (expanded) {
      val modes =
        listOf(
          DiscountMode.NONE to R.string.simple_none,
          DiscountMode.PERCENT to R.string.simple_percent,
          DiscountMode.FLAT to R.string.simple_kes,
        )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { (mode, label) ->
          OutlinedButton(onClick = {
            actions.updatePayment { it.copy(discountMode = mode) }
          }, enabled = !state.saving, modifier = Modifier.weight(1f)) {
            Text(
              stringResource(label),
              fontSize = 16.sp,
              fontWeight =
                if (state.payment.discountMode ==
                  mode
                ) {
                  FontWeight.Bold
                } else {
                  FontWeight.Normal
                },
            )
          }
        }
      }
      if (state.payment.discountMode != DiscountMode.NONE) {
        OutlinedTextField(
          value = state.payment.discountInput,
          onValueChange = { value -> actions.updatePayment { it.copy(discountInput = value) } },
          label = { Text(stringResource(R.string.simple_discount_value)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          enabled = !state.saving,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}
