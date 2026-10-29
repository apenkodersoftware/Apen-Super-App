package com.aistudio.fasmovapos.app.data.payment

/** Why a mobile-money payment was not accepted. */
enum class ConfirmationRejection { CODE_ALREADY_USED, }

sealed interface ConfirmationResult {
  data object Confirmed : ConfirmationResult

  data class Rejected(
    val reason: ConfirmationRejection,
  ) : ConfirmationResult
}

/**
 * The single place that decides whether a mobile-money payment line counts as paid. Today it is
 * [ManualMobileMoneyConfirmer]: the cashier reads the code from the customer's SMS. When Safaricom (Daraja) and Airtel
 * approve a live integration, an implementation that asks the provider replaces it here and checkout does not change.
 */
interface MobileMoneyConfirmer {
  suspend fun confirm(line: PaymentLine): ConfirmationResult
}

/** Trusts the code the cashier typed, but refuses a code that already paid for another sale. */
class ManualMobileMoneyConfirmer(
  private val isCodeUsed: suspend (String) -> Boolean,
) : MobileMoneyConfirmer {
  override suspend fun confirm(line: PaymentLine): ConfirmationResult =
    if (isCodeUsed(line.reference)) ConfirmationResult.Rejected(ConfirmationRejection.CODE_ALREADY_USED) else ConfirmationResult.Confirmed
}
