package com.aistudio.fasmovapos.app.superapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.fasmovapos.app.ui.simple.StatementPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SuperSummary(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val statementScope = rememberCoroutineScope()
  val statementSession = remember(data.profile.id, actions) { StatementSession(data.profile.id, statementScope, actions.readStatement) }
  val today = SuperDates.today()
  val rental = data.profile.type == "RENTAL"
  var from by remember { mutableStateOf(if (rental) today.take(7) + "-01" else today) }
  var through by remember { mutableStateOf(today) }
  var reading by remember { mutableStateOf<PeriodReading?>(null) }
  var error by remember { mutableStateOf(false) }
  var noTrade by remember { mutableStateOf(false) }
  var custom by remember { mutableStateOf(false) }
  LaunchedEffect(data.profile.id, from, through, data.capturedAt) {
    reading = null
    error = false
    try {
      reading = withContext(Dispatchers.IO) { actions.readPeriod(data.profile.id, RecordPeriod(from, through)) }
    } catch (
      _: IllegalArgumentException,
    ) {
      error = true
    }
  }
  Box(modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
        Text(S("summary"), style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("day", "week", "month").forEach { period ->
            OutlinedButton(onClick = {
              through = today
              from =
                when (period) {
                  "week" -> SuperDates.addDays(today, -6)
                  "month" -> today.take(7) + "-01"
                  else -> today
                }
            }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(S(period)) }
          }
        }
        Text(dateLabel(from) + " – " + dateLabel(through))
        OutlinedButton(onClick = { custom = !custom }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S("custom_dates")) }
        if (custom) {
          DateField(S("period_from"), from, { from = it })
          DateField(S("period_through"), through, { through = it })
        }
        if (through > today) Text(S("period_future"))
        if (error) Text(S("error_date"), color = MaterialTheme.colorScheme.error)
      }
      val report = reading
      if (report == null && !error) item { Text(S("working")) }
      if (report != null) {
        item { SummaryTotals(data, report) }
        item { SummaryCoverage(report) }
        item {
          SuperStatementEntry(report.period.takeIf { it.from == from && it.through == through }, statementSession)
        }
        if (data.profile.type == "RESTAURANT") item { TopSellers(data, report.period) }
        if (rental) {
          item { Text(S("per_property"), style = MaterialTheme.typography.titleLarge) }
          items(data.properties.filter { !it.voided }, key = { it.id }) { property -> PropertySummary(data, property, report.period) }
          item {
            Text(S("arrears_age"), style = MaterialTheme.typography.titleLarge)
            Text(S("owed_as_now_help"))
          }
          items(
            data.rents
              .filter {
                !it.voided && data.chargeBalance(it.id) > 0
              }.sortedBy { it.month },
            key = { it.id },
          ) { rent -> RentAgeRow(data, rent) }
        }
      }
      item {
        OutlinedButton(
          onClick = {
            noTrade = true
            actions.clearError()
          },
          enabled = !state.working,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(S("mark_no_trade")) }
        if (data.profile.type == "RESTAURANT") {
          OutlinedButton(
            onClick = { onNavigate("deni") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
          ) { Text(S("deni")) }
        }
      }
    }
    if (noTrade) {
      SuperForm(
        title = S("mark_no_trade"),
        fields = uiList(listOf(SuperField("date", S("date"), today, FieldKind.DATE))),
        busy = state.working,
        error = errorLabel(state.error),
        onDismiss = { noTrade = false },
        onSave = { fields ->
          actions.act(onSuccess = { noTrade = false }) {
            it.markNoTrade(data.profile.id, fields.getValue("date"))
          }
        },
        extra = { Text(S("no_trade_help")) },
      )
    }
    SummaryStatementDialog(statementSession)
  }
}

@Composable
private fun SummaryStatementDialog(session: StatementSession) {
  val context = LocalContext.current
  session.statement?.let { frozen ->
    StatementPreview(statement = frozen, previewText = superStatementText(context, frozen), onDismiss = session::dismiss)
  }
}

@Composable
private fun SummaryTotals(
  data: BusinessData,
  reading: PeriodReading,
) {
  val s = reading.snapshot
  val rental = data.profile.type == "RENTAL"
  val known = reading.coverageDays > 0

  fun amount(value: Long): String = if (known) SuperMoney.label(value) else "—"
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    HeroNumber(
      S(
        when (data.profile.type) {
          "DAIRY" -> "earned"
          "RENTAL" -> "expected"
          else -> "sales"
        },
      ),
      amount(s.earnedCents),
    )
    HeroNumber(S(if (rental) "collected" else "received"), amount(s.receivedCents))
    HeroNumber(S("spent"), amount(s.spentCents))
    HeroNumber(S("owed_as_now"), SuperMoney.label(s.owedCents))
    Text(S("owed_as_now_help"))
    Text(S("received_limit"))
    if (data.profile.type == "DAIRY") {
      HeroNumber(S("litres"), if (known) SuperMoney.quantity(s.litresMilli) + " L" else "—")
      val average =
        if (reading.milkDays == 0) {
          "—"
        } else {
          BigDecimal(s.litresMilli)
            .movePointLeft(3)
            .divide(BigDecimal(reading.milkDays), 3, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString() + " L"
        }
      HeroNumber(S("average_recorded_day"), average)
      Text(S("average_day_help"))
    }
    Card(Modifier.fillMaxWidth().testTag("summary-difference")) {
      Column(Modifier.padding(16.dp)) {
        HeroNumber(
          S(if (rental) "collected_less_expenses" else "sales_less_expenses"),
          amount(if (rental) s.receivedCents - s.spentCents else s.salesLessExpensesCents),
        )
        Text(S(if (rental) "rental_difference_help" else "before_cogs"))
      }
    }
    if (s.otherInCents != 0L) {
      HeroNumber(S("other_money"), SuperMoney.label(s.otherInCents))
      Text(S("other_money_help"))
    }
    if (rental) {
      HeroNumber(S("held_money"), SuperMoney.label(s.heldCents))
      Text(S("deposit_help"))
    }
  }
}

@Composable
private fun SummaryCoverage(reading: PeriodReading) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(S("coverage") + ": ${reading.coverageDays} / ${reading.calendarDays}")
    Text(S("coverage_help"))
    Text(S("unknown_entry_times") + ": " + reading.snapshot.unknownEntryTimes)
    val previous = reading.previous
    if (previous == null) {
      Text(S("not_enough_history"))
    } else {
      val prior = reading.period.previous()
      Text(S("equal_period") + ": " + dateLabel(prior.from) + " – " + dateLabel(prior.through))
      Text(S("sales_change") + ": " + SuperMoney.label(reading.snapshot.earnedCents - previous.earnedCents))
    }
    Text(S("self_recorded"))
  }
}

@Composable
private fun TopSellers(
  data: BusinessData,
  period: RecordPeriod,
) {
  val rows =
    data.lines
      .filter {
        !it.voided &&
          it.occurredOn in period.from..period.through &&
          it.itemId in data.items.filter { row -> row.category != "CUSTOM" }.map { row -> row.id }
      }.groupBy { it.itemId }
      .values
      .sortedByDescending { group -> group.sumOf { it.quantityMilli } }
      .take(5)
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(S("top_sellers"), style = MaterialTheme.typography.titleLarge)
    rows.forEach { group -> Text(group.first().name + ": " + SuperMoney.quantity(group.sumOf { it.quantityMilli })) }
    if (rows.isEmpty()) Text(S("not_recorded"))
  }
}

@Composable
private fun PropertySummary(
  data: BusinessData,
  property: PropertyRow,
  period: RecordPeriod,
) {
  val units = data.units.filter { !it.voided && it.propertyId == property.id }
  val unitIds = units.map { it.id }.toSet()
  val expected =
    data.rents
      .filter {
        !it.voided && it.unitId in unitIds && it.occurredOn in period.from..period.through
      }.sumOf { it.amountCents }
  val ledger = data.ledger.filter { !it.voided && it.propertyId == property.id && it.occurredOn in period.from..period.through }
  val owed = data.rents.filter { !it.voided && it.unitId in unitIds }.sumOf { data.chargeBalance(it.id) }
  Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(property.name, style = MaterialTheme.typography.titleMedium)
      Text(S("expected") + ": " + SuperMoney.label(expected))
      Text(
        S("collected") + ": " + SuperMoney.label(ledger.sumOf { it.receivedCents }),
      )
      Text(S("spent") + ": " + SuperMoney.label(ledger.sumOf { it.spentCents }))
      Text(S("owed_as_now") + ": " + SuperMoney.label(owed))
      Text(S("vacant_units") + ": " + units.count { data.tenancy(it.id, SuperDates.today().take(7)) == null })
    }
  }
}

@Composable
private fun RentAgeRow(
  data: BusinessData,
  rent: RentCharge,
) {
  val month = SuperDates.today().take(7)
  val age =
    (
      (
        month
          .take(
            4,
          ).toInt() - rent.month.take(4).toInt()
      ) * 12 + month.takeLast(2).toInt() - rent.month.takeLast(2).toInt()
    ).coerceAtLeast(0)
  val unit = data.units.firstOrNull { it.id == rent.unitId }
  Text(
    (unit?.label ?: "—") + " · " + monthLabel(rent.month) + " · " + SuperMoney.label(data.chargeBalance(rent.id)) +
      " · " + S(if (rent.dueDate < SuperDates.today()) "late" else "due") + " · " + age + " " + S("months"),
  )
}
