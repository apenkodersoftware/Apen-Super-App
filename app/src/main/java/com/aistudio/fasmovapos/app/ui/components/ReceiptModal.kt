package com.aistudio.fasmovapos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleItemEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import com.aistudio.fasmovapos.app.data.payment.formatAmount
import com.aistudio.fasmovapos.app.data.repository.RetailQuantity
import com.aistudio.fasmovapos.app.ui.language.personLabel
import com.aistudio.fasmovapos.app.ui.language.productLabel
import com.aistudio.fasmovapos.app.ui.theme.ProfessionalPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptModal(
  sale: SaleEntity?,
  items: List<SaleItemEntity>,
  payments: List<SalePaymentEntity>,
  branch: BranchEntity,
  onDismiss: () -> Unit,
) {
  if (sale == null) return
  val context = LocalContext.current

  val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
  val dateStr = dateFormat.format(Date(sale.timestamp))

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 16.dp),
    ) {
      Column(
        modifier =
          Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Success Badge
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = stringResource(R.string.success),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(48.dp),
        )
        Text(
          text = stringResource(R.string.payment_completed),
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        // Thermal Paper Receipt Card
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(Color(0xFFFFFBEB), shape = RoundedCornerShape(8.dp))
              .border(1.dp, Color(0xFFFDE68A), shape = RoundedCornerShape(8.dp))
              .padding(16.dp),
        ) {
          CompositionLocalProvider(LocalContentColor provides PaperInk) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              ReceiptHeader(branch)
              Text(
                text = "-----------------------------------------",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFFCBD5E1),
              )

              // Meta Info
              Column(modifier = Modifier.fillMaxWidth()) {
                ReceiptLine(stringResource(R.string.receipt_no), sale.transactionRef)
                ReceiptLine(stringResource(R.string.date), dateStr)
                ReceiptLine(stringResource(R.string.cashier_3), personLabel(sale.cashierId, sale.cashierName))
                if (payments.size > 1) {
                  payments.forEach { leg ->
                    ReceiptLine("${paymentCodeLabel(leg.method)}:", "KES ${formatAmount(leg.amount)}")
                    ReceiptLine("  " + stringResource(R.string.receipt_ref), leg.reference)
                  }
                } else {
                  ReceiptLine(
                    stringResource(R.string.payment),
                    "${paymentCodeLabel(sale.paymentMethod)} (${sale.paymentReference.take(14)})",
                  )
                }
                if (!sale.customerName.isNullOrBlank()) {
                  ReceiptLine(stringResource(R.string.customer_3), personLabel(sale.customerId, sale.customerName.orEmpty()))
                }
              }

              Text(
                text = "-----------------------------------------",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFFCBD5E1),
              )

              // Items Table Header
              Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                  stringResource(R.string.qty_item),
                  modifier = Modifier.weight(2f),
                  style =
                    MaterialTheme.typography.labelSmall.copy(
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                  stringResource(R.string.receipt_price),
                  modifier = Modifier.weight(1f),
                  textAlign = TextAlign.End,
                  style =
                    MaterialTheme.typography.labelSmall.copy(
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                  stringResource(R.string.receipt_total),
                  modifier = Modifier.weight(1f),
                  textAlign = TextAlign.End,
                  style =
                    MaterialTheme.typography.labelSmall.copy(
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Bold,
                    ),
                )
              }
              Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE2E8F0))

              items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                  Text(
                    "${RetailQuantity.label(
                      item.quantity,
                      item.quantityScale,
                      item.quantityUnit,
                    )} ${productLabel(item.productId, item.productName)}",
                    modifier = Modifier.weight(2f),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                  )
                  Text(
                    String.format(java.util.Locale.ROOT, "%.2f", item.unitPrice) + " / " +
                      RetailQuantity.label(item.pricingQuantity, item.quantityScale, item.quantityUnit),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                  )
                  Text(
                    String.format("%.0f", item.totalPrice),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                  )
                }
              }

              Text(
                text = "-----------------------------------------",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFFCBD5E1),
              )

              // Totals Breakdown
              ReceiptLine(stringResource(R.string.subtotal_2), "KES ${String.format("%.2f", sale.totalAmount)}")
              if (sale.discountAmount > 0) {
                ReceiptLine(stringResource(R.string.discount_3), "- KES ${String.format("%.2f", sale.discountAmount)}")
              }
              if (sale.taxAmount > 0) ReceiptLine(stringResource(R.string.vat_16_incl_2), "KES ${String.format("%.2f", sale.taxAmount)}")
              Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFF94A3B8))

              Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                  stringResource(receiptTotalLabel(sale.paymentMethod, payments.any { it.method == "CREDIT" })),
                  modifier = Modifier.weight(1f),
                  style =
                    MaterialTheme.typography.titleSmall.copy(
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Bold,
                    ),
                  color = Color(0xFF0F172A),
                )
                Text(
                  "KES ${String.format("%.2f", sale.netAmount)}",
                  modifier = Modifier.weight(1f),
                  textAlign = TextAlign.End,
                  style =
                    MaterialTheme.typography.titleSmall.copy(
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Bold,
                    ),
                  color = ProfessionalPrimary,
                )
              }

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = stringResource(R.string.local_storage_notice),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFF334155),
                textAlign = TextAlign.Center,
              )
              Text(
                text = "*${sale.transactionRef}*",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFF64748B),
              )

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = stringResource(R.string.thank_you_for_shopping_with_us),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ReceiptActions(
          onShare = { shareReceiptText(context, receiptShareText(context, sale, items, payments, branch)) },
          onDismiss = onDismiss,
        )
      }
    }
  }
}

/** The business's own details only: nothing is printed that the owner did not enter. */
@Composable
private fun ReceiptHeader(branch: BranchEntity) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = branch.name.uppercase(),
      style =
        MaterialTheme.typography.titleMedium.copy(
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
        ),
      color = Color(0xFF1E293B),
      textAlign = TextAlign.Center,
    )
    if (branch.location.isNotBlank()) {
      Text(
        text = branch.location,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = Color(0xFF475569),
      )
    }
    receiptContactLine(branch, stringResource(R.string.receipt_phone_label), stringResource(R.string.receipt_tax_label))
      .takeIf {
        it.isNotEmpty()
      }?.let { contact ->
        Text(
          text = contact,
          style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
          color = Color(0xFF64748B),
        )
      }
  }
}

/** Share the receipt, or finish and start the next sale. */
@Composable
private fun ReceiptActions(
  onShare: () -> Unit,
  onDismiss: () -> Unit,
) {
  Column {
    OutlinedButton(
      onClick = onShare,
      modifier = Modifier.fillMaxWidth().testTag("share_receipt_button"),
    ) {
      Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(stringResource(R.string.share))
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
      onClick = onDismiss,
      modifier = Modifier.fillMaxWidth().testTag("close_receipt_button"),
    ) {
      Text(stringResource(R.string.done_next_sale))
    }
  }
}

@Composable
private fun ReceiptLine(
  label: String,
  value: String,
) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
    Text(
      text = label,
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
      color = Color(0xFF475569),
    )
    Text(
      text = value,
      modifier = Modifier.weight(1f),
      textAlign = TextAlign.End,
      style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
      color = Color(0xFF0F172A),
    )
  }
}

/** Receipts are printed on light paper, so the ink stays dark in dark mode too. */
private val PaperInk = Color(0xFF0F172A)
