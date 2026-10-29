package com.aistudio.fasmovapos.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleItemEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import com.aistudio.fasmovapos.app.data.payment.SPLIT_PAYMENT_CODE
import com.aistudio.fasmovapos.app.data.payment.formatAmount
import com.aistudio.fasmovapos.app.data.payment.paymentMethodOf
import com.aistudio.fasmovapos.app.data.repository.RetailQuantity
import com.aistudio.fasmovapos.app.ui.language.localizedPersonName
import com.aistudio.fasmovapos.app.ui.language.localizedProductName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "KRA PIN: A001 | TEL: 0712..." with only the parts the business actually entered; empty if neither. */
fun receiptContactLine(
  branch: BranchEntity,
  phoneLabel: String = "TEL",
  taxLabel: String = "KRA PIN",
): String =
  listOfNotNull(
    branch.taxPin
      .trim()
      .takeIf { it.isNotEmpty() }
      ?.let { "$taxLabel: $it" },
    branch.phone
      .trim()
      .takeIf { it.isNotEmpty() }
      ?.let { "$phoneLabel: $it" },
  ).joinToString(" | ")

/** The receipt as plain text, for sharing through WhatsApp, SMS or email. */
fun receiptShareText(
  context: Context,
  sale: SaleEntity,
  items: List<SaleItemEntity>,
  payments: List<SalePaymentEntity>,
  branch: BranchEntity,
): String {
  val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(sale.timestamp))
  val lines = mutableListOf<String>()
  lines += branch.name.uppercase()
  if (branch.location.isNotBlank()) lines += branch.location
  receiptContactLine(branch, context.getString(R.string.receipt_phone_label), context.getString(R.string.receipt_tax_label))
    .takeIf {
      it.isNotEmpty()
    }?.let {
      lines +=
        it
    }
  lines += ""
  lines += "${context.getString(R.string.receipt_no)} ${sale.transactionRef}"
  lines += "${context.getString(R.string.date)} $date"
  lines += "${context.getString(R.string.cashier_3)} ${localizedPersonName(context, sale.cashierId, sale.cashierName)}"
  if (payments.size > 1) {
    payments.forEach { lines += "${methodLabel(context, it.method)}: KES ${formatAmount(it.amount)} (${it.reference})" }
  } else {
    lines += "${context.getString(R.string.payment)} ${methodLabel(context, sale.paymentMethod)} (${sale.paymentReference})"
  }
  sale.customerName?.takeIf { it.isNotBlank() }?.let {
    lines +=
      "${context.getString(R.string.customer_3)} ${localizedPersonName(context, sale.customerId, it)}"
  }
  lines += "-----------------------------"
  items.forEach {
    lines += "${RetailQuantity.label(it.quantity, it.quantityScale, it.quantityUnit)} x ${localizedProductName(
      context,
      it.productId,
      it.productName,
    )}  ${formatAmount(it.totalPrice)}"
  }
  lines += "-----------------------------"
  lines += "${context.getString(R.string.subtotal_2)} KES ${formatAmount(sale.totalAmount)}"
  if (sale.discountAmount > 0) lines += "${context.getString(R.string.discount_3)} - KES ${formatAmount(sale.discountAmount)}"
  if (sale.taxAmount > 0) lines += "${context.getString(R.string.vat_16_incl_2)} KES ${formatAmount(sale.taxAmount)}"
  lines +=
    "${context.getString(
      receiptTotalLabel(sale.paymentMethod, payments.any { it.method == "CREDIT" }),
    )} KES ${formatAmount(sale.netAmount)}"
  lines += ""
  lines += context.getString(R.string.local_storage_notice)
  lines += context.getString(R.string.thank_you_for_shopping_with_us)
  return lines.joinToString("\n")
}

private fun methodLabel(
  context: Context,
  code: String,
): String =
  when {
    code == SPLIT_PAYMENT_CODE -> context.getString(R.string.payment_split)
    code == "BANK" -> context.getString(R.string.payment_bank)
    else -> paymentMethodOf(code)?.let { context.getString(it.labelRes) } ?: code
  }

/** A credit sale records an amount owed, rather than claiming it has been paid. */
@StringRes
internal fun receiptTotalLabel(
  paymentMethod: String,
  hasCredit: Boolean = false,
): Int =
  if (paymentMethod == "CREDIT" ||
    hasCredit
  ) {
    R.string.simple_total
  } else {
    R.string.total_paid
  }

/** Opens the system share sheet with [text]. */
fun shareReceiptText(
  context: Context,
  text: String,
) {
  val send =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, text)
    }
  val chooser = Intent.createChooser(send, null)
  if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  context.startActivity(chooser)
}
