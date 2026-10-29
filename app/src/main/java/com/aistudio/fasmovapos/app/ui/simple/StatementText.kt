package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.runtime.Immutable
import java.util.Collections

@Immutable
class StatementText(
  lines: List<String>,
) {
  val lines: List<String> = Collections.unmodifiableList(ArrayList(lines))
}
