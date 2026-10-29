package com.aistudio.fasmovapos.app.superapp

import androidx.compose.runtime.Stable

/** Immutable event/read callbacks; screens receive snapshots and cannot retain or forward a ViewModel. */
@Stable
class SuperActions(
  private val submit: (() -> Unit, (SuperRepository) -> Unit) -> Unit,
  val clearError: () -> Unit,
  val choose: (String) -> Unit,
  val readPeriod: (String, RecordPeriod) -> PeriodReading,
  val readSource: (String, String, String) -> String?,
  val readStatement: (String, RecordPeriod) -> com.aistudio.fasmovapos.app.ui.simple.BusinessStatement,
) {
  fun act(
    onSuccess: () -> Unit = {},
    work: (SuperRepository) -> Unit,
  ) = submit(onSuccess, work)
}
