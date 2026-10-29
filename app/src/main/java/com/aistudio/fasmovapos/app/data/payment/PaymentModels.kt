package com.aistudio.fasmovapos.app.data.payment

import androidx.annotation.StringRes
import com.aistudio.fasmovapos.app.R
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/** Ways a sale (or one leg of a split sale) can be paid. [name] is what gets stored in the database. */
enum class PaymentMethod(
  @StringRes val labelRes: Int,
  @StringRes val shortLabelRes: Int,
) {
  CASH(R.string.payment_cash, R.string.payment_cash_short),
  MPESA(R.string.payment_mpesa, R.string.payment_mpesa),
  AIRTEL(R.string.payment_airtel, R.string.payment_airtel_short),
  CARD(R.string.payment_card, R.string.payment_card),
  CREDIT(R.string.payment_credit, R.string.payment_credit),
  ;

  val isMobileMoney: Boolean get() = this == MPESA || this == AIRTEL

  /** A credit leg records the unpaid balance of a part-paid sale. */
  val canSplit: Boolean get() = true
}

/** One leg of a sale's payment. A normal sale has one line; a split sale has two. */
data class PaymentLine(
  val method: PaymentMethod,
  val amount: Double,
  val phoneNumber: String? = null,
  val reference: String = "",
)

/** Stored in [com.aistudio.fasmovapos.app.data.db.entities.SaleEntity.paymentMethod] for a split sale. */
const val SPLIT_PAYMENT_CODE = "SPLIT"

/** The method for a stored payment code ("MPESA"), or null for "SPLIT" and unknown codes. */
fun paymentMethodOf(code: String): PaymentMethod? = PaymentMethod.entries.firstOrNull { it.name == code }

enum class DiscountMode { NONE, PERCENT, FLAT }

/** Why a payment can't be taken yet. The UI turns this into a message in the user's language. */
enum class PaymentIssueType { CODE_REQUIRED, CREDIT_NEEDS_CUSTOMER, SAME_METHOD, AMOUNT_REQUIRED, AMOUNT_TOO_LARGE }

data class PaymentIssue(
  val type: PaymentIssueType,
  val method: PaymentMethod? = null,
)

/** Everything the cashier has entered on the payment panel, before checkout. */
data class PaymentState(
  val method: PaymentMethod = PaymentMethod.CASH,
  val phone: String = "",
  val isSplit: Boolean = false,
  val splitMethodA: PaymentMethod = PaymentMethod.CASH,
  val splitMethodB: PaymentMethod = PaymentMethod.MPESA,
  val splitAmountA: String = "",
  val splitPhoneA: String = "",
  val splitPhoneB: String = "",
  val discountMode: DiscountMode = DiscountMode.NONE,
  val discountInput: String = "",
  // Confirmation codes the cashier copies from the customer's payment SMS (mobile money only).
  val reference: String = "",
  val splitReferenceA: String = "",
  val splitReferenceB: String = "",
) {
  /** Discount in KES for a cart worth [gross], never negative and never more than [gross]. */
  fun discountAmount(gross: Double): Double {
    val value = discountInput.toDoubleOrNull() ?: 0.0
    val raw =
      when (discountMode) {
        DiscountMode.NONE -> 0.0
        DiscountMode.PERCENT -> gross * value.coerceIn(0.0, 100.0) / 100.0
        DiscountMode.FLAT -> value
      }
    return roundMoney(raw.coerceIn(0.0, gross))
  }

  /** The second leg of a split is always whatever the first leg doesn't cover. */
  fun splitAmountB(net: Double): Double = roundMoney((net - (splitAmountA.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0))

  /** The payment legs for a sale totalling [net]. Phone numbers are kept only for mobile money. */
  fun lines(net: Double): List<PaymentLine> =
    if (isSplit) {
      val a = roundMoney(splitAmountA.toDoubleOrNull() ?: 0.0)
      listOf(
        PaymentLine(splitMethodA, a, phoneFor(splitMethodA, splitPhoneA), codeFor(splitMethodA, splitReferenceA)),
        PaymentLine(splitMethodB, splitAmountB(net), phoneFor(splitMethodB, splitPhoneB), codeFor(splitMethodB, splitReferenceB)),
      )
    } else {
      listOf(PaymentLine(method, roundMoney(net), phoneFor(method, phone), codeFor(method, reference)))
    }

  /** Why checkout is blocked, or null when the payment is ready to take. */
  fun validationError(
    net: Double,
    hasCustomer: Boolean,
  ): PaymentIssue? =
    if (isSplit) {
      splitError(net) ?: if ((splitMethodA == PaymentMethod.CREDIT || splitMethodB == PaymentMethod.CREDIT) && !hasCustomer) {
        PaymentIssue(PaymentIssueType.CREDIT_NEEDS_CUSTOMER)
      } else {
        null
      }
    } else {
      singleError(method, reference, hasCustomer)
    }

  private fun singleError(
    method: PaymentMethod,
    reference: String,
    hasCustomer: Boolean,
  ): PaymentIssue? =
    when {
      method.isMobileMoney && !isValidMobileCode(reference) -> PaymentIssue(PaymentIssueType.CODE_REQUIRED, method)
      method == PaymentMethod.CREDIT && !hasCustomer -> PaymentIssue(PaymentIssueType.CREDIT_NEEDS_CUSTOMER)
      else -> null
    }

  private fun splitError(net: Double): PaymentIssue? {
    val entered = splitAmountA.toBigDecimalOrNull()
    val a = splitAmountA.toDoubleOrNull() ?: 0.0
    return when {
      splitMethodA == splitMethodB -> PaymentIssue(PaymentIssueType.SAME_METHOD)
      entered == null || !a.isFinite() || entered.stripTrailingZeros().scale() > 2 || a <= 0.0 ->
        PaymentIssue(
          PaymentIssueType.AMOUNT_REQUIRED,
          splitMethodA,
        )
      a >= net || abs(net - a) < MONEY_EPSILON -> PaymentIssue(PaymentIssueType.AMOUNT_TOO_LARGE, splitMethodA)
      splitMethodA.isMobileMoney && !isValidMobileCode(splitReferenceA) -> PaymentIssue(PaymentIssueType.CODE_REQUIRED, splitMethodA)
      splitMethodB.isMobileMoney && !isValidMobileCode(splitReferenceB) -> PaymentIssue(PaymentIssueType.CODE_REQUIRED, splitMethodB)
      else -> null
    }
  }

  // The customer's number is optional: it is a convenience on the receipt, the reference is recorded manually and does not verify payment.
  private fun phoneFor(
    method: PaymentMethod,
    phone: String,
  ): String? = if (method.isMobileMoney) phone.trim().ifBlank { null } else null

  private fun codeFor(
    method: PaymentMethod,
    code: String,
  ): String = if (method.isMobileMoney) normalizeMobileCode(code) else ""

  private companion object {
    const val MONEY_EPSILON = 0.005
  }
}

/** The confirmation code as stored: letters and digits only, upper case ("qkj 9821-lxm" -> "QKJ9821LXM"). */
fun normalizeMobileCode(raw: String): String = raw.filter { it.isLetterOrDigit() }.uppercase()

/** M-Pesa codes are 10 characters; Airtel Money transaction IDs vary, so only a sensible minimum is enforced. */
fun isValidMobileCode(raw: String): Boolean = normalizeMobileCode(raw).length >= MIN_MOBILE_CODE_LENGTH

private const val MIN_MOBILE_CODE_LENGTH = 8

private fun roundMoney(value: Double): Double = round(value * 100.0) / 100.0

/** "1234.50" - two decimals, always a dot, whatever the device locale. */
fun formatAmount(amount: Double): String = String.format(Locale.US, "%.2f", amount)
