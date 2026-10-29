package com.aistudio.fasmovapos.app.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.payment.PaymentIssue
import com.aistudio.fasmovapos.app.data.payment.PaymentIssueType
import com.aistudio.fasmovapos.app.data.payment.PaymentMethod
import com.aistudio.fasmovapos.app.data.payment.SPLIT_PAYMENT_CODE
import com.aistudio.fasmovapos.app.data.payment.paymentMethodOf

@Composable
fun PaymentMethod.label(): String = stringResource(labelRes)

/** Label for a stored payment code ("MPESA" -> "M-Pesa", "SPLIT" -> "Split"); unknown codes show as stored. */
@Composable
fun paymentCodeLabel(code: String): String =
  when {
    code == SPLIT_PAYMENT_CODE -> stringResource(R.string.payment_split)
    code == "BANK" -> stringResource(R.string.payment_bank)
    else -> paymentMethodOf(code)?.label() ?: code
  }

/** "Cash + Airtel Money" for a split sale, otherwise the single method's label. */
@Composable
fun paymentSummary(
  paymentCode: String,
  legMethods: List<String>,
): String =
  if (legMethods.size > 1) {
    legMethods.map { paymentCodeLabel(it) }.joinToString(" + ")
  } else {
    paymentCodeLabel(paymentCode)
  }

/** The message for [this] in the language of [context]. */
fun PaymentIssue.text(context: Context): String {
  val methodLabel = method?.let { context.getString(it.labelRes) }.orEmpty()
  return when (type) {
    PaymentIssueType.CODE_REQUIRED -> context.getString(R.string.payment_issue_code, methodLabel)
    PaymentIssueType.CREDIT_NEEDS_CUSTOMER -> context.getString(R.string.payment_issue_credit_customer)
    PaymentIssueType.SAME_METHOD -> context.getString(R.string.payment_issue_same_method)
    PaymentIssueType.AMOUNT_REQUIRED -> context.getString(R.string.payment_issue_amount_required, methodLabel)
    PaymentIssueType.AMOUNT_TOO_LARGE -> context.getString(R.string.payment_issue_amount_too_large, methodLabel)
  }
}

@Composable
fun PaymentIssue.text(): String = text(LocalContext.current)
