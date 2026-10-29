package com.aistudio.fasmovapos.app.ui.simple

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.aistudio.fasmovapos.app.BuildConfig
import com.aistudio.fasmovapos.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun StatementPreview(
  statement: BusinessStatement,
  previewText: StatementText? = null,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val lines = previewText?.lines ?: statementLines(context, statement)
  val scope = rememberCoroutineScope()
  var agreed by remember(statement.id) { mutableStateOf(false) }
  var working by remember { mutableStateOf(false) }
  var problem by remember { mutableStateOf(false) }

  fun saveStatement(
    uri: android.net.Uri?,
    pdf: Boolean,
  ) {
    if (uri == null) return
    scope.launch {
      working = true
      problem =
        runCatching {
          withContext(Dispatchers.IO) {
            val bytes = if (pdf) statement.pdf(lines) else statement.csv().toByteArray(Charsets.UTF_8)
            requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
          }
        }.isFailure
      working = false
    }
  }
  val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { saveStatement(it, true) }
  val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { saveStatement(it, false) }
  AlertDialog(
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.padding(16.dp),
    onDismissRequest = { if (!working) onDismiss() },
    title = { Text(stringResource(R.string.r2_statement)) },
    text = {
      Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        lines.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
        Text(stringResource(R.string.r2_share_notice))
        Row {
          Checkbox(checked = agreed, enabled = !working, onCheckedChange = { agreed = it }, modifier = Modifier.testTag("r2_share_consent"))
          Text(stringResource(R.string.r2_share_consent), modifier = Modifier.weight(1f))
        }
        Button(enabled = agreed && !working, modifier = Modifier.fillMaxWidth().testTag("r2_share_pdf"), onClick = {
          scope.launch {
            working = true
            problem =
              runCatching {
                val file =
                  withContext(Dispatchers.IO) {
                    val folder = File(context.cacheDir, "statements").apply { mkdirs() }
                    File(folder, "Apen-${statement.id}.pdf").apply { writeBytes(statement.pdf(lines)) }
                  }
                val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".statements", file)
                val send =
                  Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri("Apen", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                  }
                context.startActivity(Intent.createChooser(send, context.getString(R.string.r2_share_pdf)))
              }.isFailure
            working = false
          }
        }) { Text(stringResource(R.string.r2_share_pdf)) }
        listOf("pdf" to R.string.r2_save_pdf, "csv" to R.string.r2_save_csv).forEach { (type, label) ->
          OutlinedButton(enabled = agreed && !working, modifier = Modifier.fillMaxWidth().testTag("r2_save_$type"), onClick = {
            (if (type == "pdf") savePdf else saveCsv).launch("Apen-${statement.id}.$type")
          }) { Text(stringResource(label)) }
        }
        if (problem) Text(stringResource(R.string.r2_export_problem), color = MaterialTheme.colorScheme.error)
      }
    },
    confirmButton = { TextButton(enabled = !working, onClick = onDismiss) { Text(stringResource(R.string.simple_close)) } },
  )
}

fun statementLines(
  context: Context,
  statement: BusinessStatement,
): List<String> {
  val rows = statement.rows.toMap()

  fun value(key: String) = rows.getValue(key)

  fun date(key: String) =
    DateFormat
      .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
      .apply {
        timeZone =
          java.util.TimeZone.getTimeZone(value("timezone"))
      }.format(Date(value(key).toLong() - if (key == "period_end_exclusive_epoch_ms") 1 else 0))
  val labels =
    listOf(
      "sales_after_discounts" to R.string.lite_sales,
      "received_payments_including_old_debt_collections" to R.string.lite_received,
      "recorded_expenses" to R.string.lite_expenses,
      "sales_less_recorded_expenses_before_cogs_not_profit" to R.string.lite_estimate,
      "credit_issued_in_period" to R.string.lite_credit_period,
      "old_debt_collections_in_period" to R.string.lite_collections,
      "outstanding_customer_debt_as_of_generation_not_period_end" to R.string.r2_debt_now,
    )
  return listOf(
    context.getString(R.string.r2_statement),
    value("business"),
    context.getString(R.string.r2_self_recorded),
    context.getString(R.string.r2_statement_period, date("period_start_inclusive_epoch_ms"), date("period_end_exclusive_epoch_ms")),
    context.getString(R.string.r2_generated, date("generated_at_epoch_ms"), value("timezone")),
  ) + labels.map { (key, label) -> context.getString(label) + ": KES " + value(key) } +
    listOf(
      context.getString(
        R.string.r2_coverage_counts,
        value("days_with_entries").toInt(),
        value("marked_no_trade_days_without_entries").toInt(),
        value("days_without_records_or_no_trade_marker").toInt(),
      ),
      context.getString(R.string.r2_legacy, value("entries_with_unknown_original_entry_time").toInt()),
      context.getString(R.string.r2_changes, value("financial_change_events_recorded_in_period").toInt()),
      context.getString(R.string.lite_estimate_note),
      context.getString(R.string.r2_coverage_notice),
      context.getString(R.string.r2_debt_notice),
      context.getString(R.string.r2_privacy),
      "ID: " + statement.id,
      "Apen " + value("app_version") + " | " + value("schema"),
      context.getString(R.string.r2_digest_notice),
      "SHA-256 (CSV): " + statement.digest(),
    )
}
