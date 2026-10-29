package com.aistudio.fasmovapos.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.payment.DiscountMode
import com.aistudio.fasmovapos.app.data.payment.PaymentMethod
import com.aistudio.fasmovapos.app.data.payment.PaymentState
import com.aistudio.fasmovapos.app.data.payment.formatAmount

private val AirtelRed = Color(0xFFED1C24)

/**
 * Discount and payment controls for the POS cart: a discount (none / % / KES), a split toggle,
 * and either one payment method or two (split). All state lives in [PaymentState]; changes go
 * through [onUpdate].
 */
@Composable
fun PaymentPanel(
  payment: PaymentState,
  netTotal: Double,
  grossTotal: Double,
  onUpdate: ((PaymentState) -> PaymentState) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    DiscountSection(payment = payment, grossTotal = grossTotal, onUpdate = onUpdate)

    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.split_between_two_methods),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
      )
      Switch(
        checked = payment.isSplit,
        onCheckedChange = { checked -> onUpdate { it.copy(isSplit = checked) } },
      )
    }

    if (payment.isSplit) {
      SplitSection(payment = payment, netTotal = netTotal, onUpdate = onUpdate)
    } else {
      SingleSection(payment = payment, onUpdate = onUpdate)
    }
  }
}

@Composable
private fun DiscountSection(
  payment: PaymentState,
  grossTotal: Double,
  onUpdate: ((PaymentState) -> PaymentState) -> Unit,
) {
  Column {
    SectionLabel(stringResource(R.string.discount))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      val noneLabel = stringResource(R.string.none)
      listOf(
        DiscountMode.NONE to noneLabel,
        DiscountMode.PERCENT to "%",
        DiscountMode.FLAT to "KES",
      ).forEach { (mode, label) ->
        FilterChip(
          selected = payment.discountMode == mode,
          onClick = {
            onUpdate { it.copy(discountMode = mode, discountInput = if (mode == DiscountMode.NONE) "" else it.discountInput) }
          },
          label = { Text(label, fontSize = 12.sp) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
      }
    }
    if (payment.discountMode != DiscountMode.NONE) {
      val applied = payment.discountAmount(grossTotal)
      OutlinedTextField(
        value = payment.discountInput,
        onValueChange = { input -> onUpdate { it.copy(discountInput = input.filter { c -> c.isDigit() || c == '.' }) } },
        label = {
          Text(
            if (payment.discountMode ==
              DiscountMode.PERCENT
            ) {
              stringResource(R.string.discount_2)
            } else {
              stringResource(R.string.discount_amount_kes)
            },
          )
        },
        supportingText = { if (applied > 0) Text(stringResource(R.string.takes_off_kes, formatAmount(applied))) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
      )
    }
  }
}

@Composable
private fun SingleSection(
  payment: PaymentState,
  onUpdate: ((PaymentState) -> PaymentState) -> Unit,
) {
  Column {
    SectionLabel(stringResource(R.string.payment_method))
    MethodRow(
      splitOnly = false,
      selected = payment.method,
      onSelect = { method -> onUpdate { it.copy(method = method) } },
    )
    if (payment.method.isMobileMoney) {
      PhoneField(
        label = stringResource(R.string.customer_phone_optional, payment.method.label()),
        value = payment.phone,
        onChange = { phone -> onUpdate { it.copy(phone = phone) } },
      )
      CodeField(
        label = stringResource(R.string.payment_code_label, payment.method.label()),
        value = payment.reference,
        onChange = { code -> onUpdate { it.copy(reference = code) } },
        tag = "payment_code_field",
      )
    }
    if (payment.method == PaymentMethod.CREDIT) {
      Text(
        text = stringResource(R.string.records_a_debt_not_a_payment),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}

@Composable
private fun SplitSection(
  payment: PaymentState,
  netTotal: Double,
  onUpdate: ((PaymentState) -> PaymentState) -> Unit,
) {
  Column {
    SectionLabel(stringResource(R.string.first_method))
    MethodRow(
      splitOnly = true,
      selected = payment.splitMethodA,
      // Choosing the method already used by the other leg swaps the two, so they never match.
      onSelect = { method ->
        onUpdate {
          if (method == it.splitMethodB) it.copy(splitMethodA = method, splitMethodB = it.splitMethodA) else it.copy(splitMethodA = method)
        }
      },
    )
    OutlinedTextField(
      value = payment.splitAmountA,
      onValueChange = { input -> onUpdate { it.copy(splitAmountA = input.filter { c -> c.isDigit() || c == '.' }) } },
      label = { Text(stringResource(R.string.amount_kes_2, payment.splitMethodA.label())) },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
    if (payment.splitMethodA.isMobileMoney) {
      PhoneField(
        label = stringResource(R.string.customer_phone_optional, payment.splitMethodA.label()),
        value = payment.splitPhoneA,
        onChange = { phone -> onUpdate { it.copy(splitPhoneA = phone) } },
      )
      CodeField(
        label = stringResource(R.string.payment_code_label, payment.splitMethodA.label()),
        value = payment.splitReferenceA,
        onChange = { code -> onUpdate { it.copy(splitReferenceA = code) } },
        tag = "payment_code_field_a",
      )
    }

    SectionLabel(stringResource(R.string.second_method_the_balance))
    MethodRow(
      splitOnly = true,
      selected = payment.splitMethodB,
      onSelect = { method ->
        onUpdate {
          if (method == it.splitMethodA) it.copy(splitMethodB = method, splitMethodA = it.splitMethodB) else it.copy(splitMethodB = method)
        }
      },
    )
    Text(
      text = stringResource(R.string.kes, payment.splitMethodB.label(), formatAmount(payment.splitAmountB(netTotal))),
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      modifier = Modifier.padding(top = 6.dp),
    )
    if (payment.splitMethodB.isMobileMoney) {
      PhoneField(
        label = stringResource(R.string.customer_phone_optional, payment.splitMethodB.label()),
        value = payment.splitPhoneB,
        onChange = { phone -> onUpdate { it.copy(splitPhoneB = phone) } },
      )
      CodeField(
        label = stringResource(R.string.payment_code_label, payment.splitMethodB.label()),
        value = payment.splitReferenceB,
        onChange = { code -> onUpdate { it.copy(splitReferenceB = code) } },
        tag = "payment_code_field_b",
      )
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
  )
}

/** The confirmation code from the customer's payment SMS - what actually proves a mobile-money sale was paid. */
@Composable
private fun CodeField(
  label: String,
  value: String,
  onChange: (String) -> Unit,
  tag: String,
) {
  OutlinedTextField(
    value = value,
    onValueChange = { onChange(it.uppercase()) },
    label = { Text(label) },
    placeholder = { Text("QKJ9821LXM") },
    singleLine = true,
    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii),
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag(tag),
  )
}

@Composable
private fun PhoneField(
  label: String,
  value: String,
  onChange: (String) -> Unit,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onChange,
    label = { Text(label) },
    placeholder = { Text(stringResource(R.string.phone_example)) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
  )
}

@Composable
private fun MethodRow(
  splitOnly: Boolean,
  selected: PaymentMethod,
  onSelect: (PaymentMethod) -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    PaymentMethod.entries.filter { !splitOnly || it.canSplit }.forEach { method ->
      MethodChip(
        method = method,
        isSelected = method == selected,
        onClick = { onSelect(method) },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun MethodChip(
  method: PaymentMethod,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    tonalElevation = if (isSelected) 4.dp else 0.dp,
    modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
      Icon(
        imageVector = method.icon(),
        contentDescription = method.label(),
        tint = if (method == PaymentMethod.AIRTEL && !isSelected) AirtelRed else contentColor,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = stringResource(method.shortLabelRes),
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = contentColor,
      )
    }
  }
}

private fun PaymentMethod.icon(): ImageVector =
  when (this) {
    PaymentMethod.CASH -> Icons.Default.LocalAtm
    PaymentMethod.MPESA, PaymentMethod.AIRTEL -> Icons.Default.PhoneAndroid
    PaymentMethod.CARD -> Icons.Default.CreditCard
    PaymentMethod.CREDIT -> Icons.Default.Receipt
  }
