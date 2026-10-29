package com.aistudio.fasmovapos.app.data.security

/** Why a PIN was refused; the UI maps each case to a plain-language message. */
enum class PinProblem {
  NOT_FOUR_DIGITS,
  IS_DEFAULT,
  SAME_AS_CURRENT,
  IN_USE,
}

object PinPolicy {
  const val LENGTH = 4

  /** Temporary PIN for sample staff and the value no one may keep: it is public knowledge. */
  const val DEFAULT_PIN = "1234"

  /** Checks the shape of [pin] only; uniqueness against other staff is checked by the repository. */
  fun formatProblem(pin: String): PinProblem? =
    when {
      pin.length != LENGTH || !pin.all { it.isDigit() } -> PinProblem.NOT_FOUR_DIGITS
      pin == DEFAULT_PIN -> PinProblem.IS_DEFAULT
      else -> null
    }
}
