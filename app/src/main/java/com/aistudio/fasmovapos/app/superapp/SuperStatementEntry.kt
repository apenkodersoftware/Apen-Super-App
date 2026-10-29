package com.aistudio.fasmovapos.app.superapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun SuperStatementEntry(
  period: RecordPeriod?,
  session: StatementSession,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    OutlinedButton(
      enabled = period != null && !session.working,
      modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("super-statement"),
      onClick = { period?.let(session::prepare) },
    ) { Text(S(if (session.working) "working" else "statement")) }
    Text(S("statement_help"))
    if (session.problem) Text(S("statement_problem"), color = MaterialTheme.colorScheme.error)
  }
}
