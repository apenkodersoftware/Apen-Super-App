package com.aistudio.fasmovapos.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.security.PinProblem

/** Plain-language explanation of why a PIN was refused, in the language the user picked. */
@Composable
fun PinProblem.message(): String =
  stringResource(
    when (this) {
      PinProblem.NOT_FOUR_DIGITS -> R.string.pin_problem_format
      PinProblem.IS_DEFAULT -> R.string.pin_problem_default
      PinProblem.SAME_AS_CURRENT -> R.string.pin_problem_same
      PinProblem.IN_USE -> R.string.pin_problem_in_use
    },
  )
