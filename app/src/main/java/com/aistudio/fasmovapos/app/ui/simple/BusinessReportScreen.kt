package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun BusinessReportScreen(
  data: ReportInput,
  onExpense: () -> Unit,
  onDebts: () -> Unit,
  modifier: Modifier = Modifier,
  recordContext: RecordContext = RecordContext(),
  businessName: String = "",
  branchId: String = com.aistudio.fasmovapos.app.data.repository.HQ_BRANCH_ID,
  onHistory: () -> Unit = {},
  onNoTrade: (String, Boolean) -> Unit = { _, _ -> },
) {
  val appContext = LocalContext.current
  val scope = rememberCoroutineScope()
  var preparing by remember { mutableStateOf(false) }
  var prepareProblem by remember { mutableStateOf(false) }
  var period by rememberSaveable { mutableStateOf(ReportPeriod.DAY) }
  var details by rememberSaveable { mutableStateOf(false) }
  var statement by remember { mutableStateOf<BusinessStatement?>(null) }
  val now by produceState(System.currentTimeMillis()) {
    while (true) {
      delay(30_000)
      value = System.currentTimeMillis()
    }
  }
  val comparison = comparePeriods(data, period, now)
  val coverage = recordingCoverage(data, recordContext, comparison.currentWindow)
  val today = localDate(now)
  val noTrade = recordContext.days.any { it.date == today && it.closed }
  val hasEntriesToday = recordingCoverage(data, recordContext, reportWindow(ReportPeriod.DAY, now)).entryDays > 0
  LazyColumn(
    modifier = modifier.fillMaxSize().padding(16.dp).testTag("r2_summary_list"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item { Text(stringResource(R.string.lite_report), style = MaterialTheme.typography.headlineSmall) }
    item { Text(stringResource(R.string.r2_scope)) }
    item { PeriodPicker(period) { period = it } }
    item { Text(reportDates(comparison.currentWindow)) }
    item {
      Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          ReportFigure(R.string.lite_sales, comparison.current.sales, "lite_report_sales")
          ReportFigure(R.string.lite_received, comparison.current.received, "lite_report_received")
          ReportFigure(R.string.r2_debt_now, comparison.current.outstanding, "lite_report_outstanding")
          Text(stringResource(R.string.r2_debt_notice))
        }
      }
    }
    item { Button(onClick = onExpense, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.lite_add_expense)) } }
    item { OutlinedButton(onClick = onDebts, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.lite_debts)) } }
    item { GrowthPanel(comparison) }
    item { CoveragePanel(coverage) }
    if (!hasEntriesToday) {
      item {
        OutlinedButton(onClick = { onNoTrade(today, !noTrade) }, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(if (noTrade) R.string.r2_unmark_closed else R.string.r2_mark_closed))
        }
      }
    }
    item {
      Button(
        onClick = {
          scope.launch {
            preparing = true
            val snapshot =
              runCatching {
                withContext(Dispatchers.IO) {
                  loadBusinessStatement(FasMovaPOSDatabase.getDatabase(appContext), branchId, businessName, period)
                }
              }
            statement = snapshot.getOrNull()
            prepareProblem = snapshot.isFailure
            preparing = false
          }
        },
        modifier =
          Modifier.fillMaxWidth().testTag(
            "r2_preview_statement",
          ),
        enabled = !preparing,
      ) { Text(stringResource(R.string.r2_preview)) }
    }
    if (prepareProblem) item { Text(stringResource(R.string.r2_export_problem)) }
    item { TextButton(onClick = { details = !details }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.r2_details)) } }
    if (details) {
      item { BusinessPanel(comparison.current) }
      item { ReceiptsPanel(comparison.current) }
    }
    item { OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.r2_receipts)) } }
  }
  statement?.let { StatementPreview(it) { statement = null } }
}

@Composable
private fun PeriodPicker(
  period: ReportPeriod,
  onSelect: (ReportPeriod) -> Unit,
) {
  Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf(
      ReportPeriod.DAY to R.string.lite_daily,
      ReportPeriod.WEEK to R.string.lite_weekly,
      ReportPeriod.MONTH to R.string.lite_monthly,
      ReportPeriod.YEAR to R.string.lite_yearly,
    ).forEach { (option, label) ->
      FilterChip(
        selected = period == option,
        onClick = { onSelect(option) },
        label = { Text(stringResource(label)) },
        modifier = Modifier.testTag("lite_period_${option.name}"),
      )
    }
  }
}

private fun reportDates(window: ReportWindow): String =
  DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(window.start)) + " - " +
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(window.end - 1))

@Composable
private fun GrowthPanel(comparison: PeriodComparison) {
  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(stringResource(R.string.r2_growth), style = MaterialTheme.typography.titleLarge)
      Text(stringResource(R.string.r2_previous_period, reportDates(comparison.previousWindow)))
      if (comparison.hasPreviousRecords) {
        Text(stringResource(R.string.r2_previous_sales, simpleMoney(comparison.previous.sales)))
        Text(
          stringResource(R.string.r2_sales_change, simpleMoney(comparison.current.sales - comparison.previous.sales)),
          style = MaterialTheme.typography.titleMedium,
        )
      } else {
        Text(stringResource(R.string.r2_not_enough_history))
      }
      Text(stringResource(R.string.r2_comparison_notice))
    }
  }
}

@Composable
private fun CoveragePanel(coverage: RecordCoverage) {
  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(stringResource(R.string.r2_coverage), style = MaterialTheme.typography.titleLarge)
      Text(stringResource(R.string.r2_coverage_counts, coverage.entryDays, coverage.closedDays, coverage.unknownDays))
      Text(stringResource(R.string.r2_legacy, coverage.legacyEntries))
      Text(stringResource(R.string.r2_changes, coverage.corrections))
      Text(stringResource(R.string.r2_history_retention))
      Text(stringResource(R.string.r2_coverage_notice))
    }
  }
}

@Composable
fun BusinessPanel(
  report: BusinessReport,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      ReportFigure(R.string.lite_sales, report.sales, "lite_report_sales")
      ReportFigure(R.string.lite_expenses, report.expenses, "lite_report_expenses")
      HorizontalDivider()
      ReportFigure(R.string.lite_estimate, report.estimate, "lite_report_estimate")
      Text(stringResource(R.string.lite_estimate_note))
    }
  }
}

@Composable
fun ReceiptsPanel(
  report: BusinessReport,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      ReportFigure(R.string.lite_received, report.received, "lite_report_received")
      ReportFigure(R.string.lite_credit_period, report.credit, "lite_report_credit")
      ReportFigure(R.string.lite_collections, report.collected, "lite_report_collections")
      HorizontalDivider()
      ReportFigure(R.string.lite_outstanding, report.outstanding, "lite_report_outstanding")
      ReportFigure(R.string.lite_net_receipts, report.netReceipts, "lite_report_net")
      Text(stringResource(R.string.lite_receipts_note))
    }
  }
}

@Composable
private fun ReportFigure(
  label: Int,
  amount: Double,
  tag: String,
) {
  Column {
    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
    Text(simpleMoney(amount), fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag(tag))
  }
}
