package com.aistudio.fasmovapos.app.superapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import org.json.JSONObject

@Composable
fun DairyHome(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  onBuyers: () -> Unit,
  onHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var session by remember { mutableStateOf<String?>(null) }
  val today = SuperDates.today()
  val todayMilk = data.milk.filter { !it.voided && it.occurredOn == today }
  Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    LivelihoodHeading(
      type = Livelihood.DAIRY,
      title = S("milk"),
      subtitle = S("dairy_intro"),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      listOf("MORNING", "EVENING").forEach { s ->
        Card(onClick = {
          session = s
          actions.clearError()
        }, modifier = Modifier.weight(1f).heightIn(min = 120.dp).testTag("milk-$s")) {
          val recorded = todayMilk.filter { it.session == s }
          Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(S(s.lowercase()), style = MaterialTheme.typography.titleMedium)
            Text(
              if (recorded.isEmpty()) {
                S("not_recorded")
              } else {
                SuperMoney.quantity(recorded.sumOf { it.litresMilli }) +
                  " L"
              },
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
            )
            Text(S("record_milk"))
          }
        }
      }
    }
    Text(
      S("today_milk") + ": " +
        SuperMoney.quantity(
          todayMilk.sumOf {
            it.litresMilli
          },
        ) + " L · " + SuperMoney.label(todayMilk.sumOf { it.earnedCents }),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Card(Modifier.weight(1f)) { HeroNumber(S("litres_month"), SuperMoney.quantity(data.month.litresMilli) + " L") }
      Card(Modifier.weight(1f)) { HeroNumber(S("earned_month"), SuperMoney.label(data.month.earnedCents)) }
    }
    Card(Modifier.fillMaxWidth()) { HeroNumber(S("owed_buyers"), SuperMoney.label(data.month.owedCents)) }
    Text(S("seven_days"), style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      for (offset in -6..0) {
        val date = SuperDates.addDays(today, offset)
        val litres = data.milk.filter { !it.voided && it.occurredOn == date }.sumOf { it.litresMilli }
        val hasRecord = data.milk.any { !it.voided && it.occurredOn == date }
        Column(Modifier.weight(1f)) {
          Text(date.takeLast(2))
          Text(if (hasRecord)SuperMoney.quantity(litres)else "—", fontWeight = FontWeight.Bold)
        }
      }
    }
    if (data.milk.none { !it.voided })Text(S("empty_milk"))
    MilkHistoryList(
      rows =
        uiList(
          data.milk
            .filter {
              !it.voided
            }.sortedWith(compareByDescending<MilkDelivery> { it.occurredOn }.thenBy { it.session })
            .take(30),
        ),
      data = data,
      actions = actions,
      state = state,
    )
    if (data.milk.count { !it.voided } > 30) {
      OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(S("history")) }
    }
  }
  session?.let { s ->
    if (data.parties.none { !it.voided }) {
      EmptyBuyerDialog(onDismiss = { session = null }, onBuyers = {
        session = null
        onBuyers()
      })
    } else {
      MilkKeypad(data, actions, state, s, null) { session = null }
    }
  }
}

@Composable
private fun EmptyBuyerDialog(
  onDismiss: () -> Unit,
  onBuyers: () -> Unit,
) {
  AlertDialog(onDismissRequest = onDismiss, title = {
    Text(S("add_buyer"))
  }, text = {
    Text(S("empty_buyers"))
  }, confirmButton = {
    Button(onClick = onBuyers) { Text(S("add_buyer")) }
  }, dismissButton = { TextButton(onClick = onDismiss) { Text(S("cancel")) } })
}

@Composable
fun MilkKeypad(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  session: String,
  draft: MilkDelivery?,
  onDismiss: () -> Unit,
) {
  val buyers = data.parties.filter { !it.voided }
  val last =
    data.milk
      .filter { !it.voided }
      .maxByOrNull { it.recordedAt ?: 0L }
      ?.partyId
  var buyer by remember { mutableStateOf(draft?.partyId ?: last?.takeIf { id -> buyers.any { it.id == id } } ?: buyers.first().id) }
  var number by remember { mutableStateOf(draft?.let { SuperMoney.quantity(it.litresMilli) } ?: "") }
  var date by remember { mutableStateOf(draft?.occurredOn ?: SuperDates.today()) }
  var price by remember { mutableStateOf(inputMoney(draft?.priceCents ?: buyers.first { it.id == buyer }.priceCents)) }
  var reason by remember { mutableStateOf("") }
  var details by remember { mutableStateOf(false) }
  AlertDialog(
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.padding(12.dp),
    onDismissRequest = {
      if (!state.working)onDismiss()
    },
    title = {
      Text(
        S(if (draft == null)"record_milk" else "edit") + " · " + S(session.lowercase()),
      )
    },
    text = {
      Column(Modifier.heightIn(max = 570.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          number.ifBlank {
            "0"
          } + " L",
          fontSize = 44.sp,
          lineHeight = 52.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.testTag("milk-number"),
        )
        Text(
          buyers
            .firstOrNull {
              it.id == buyer
            }?.name
            .orEmpty() + " · " + SuperMoney.label(runCatching { SuperMoney.cents(price) }.getOrDefault(0)) + " / L",
        )
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫")).forEach { row ->
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { digit ->
              OutlinedButton(
                onClick = {
                  number = nextMilkNumber(number, digit)
                },
                enabled = !state.working,
                modifier =
                  Modifier
                    .weight(
                      1f,
                    ).heightIn(min = 56.dp)
                    .testTag("key-$digit"),
              ) { Text(digit, fontSize = 24.sp) }
            }
          }
        }
        if (draft == null) {
          TextButton(onClick = {
            details = !details
          }, enabled = !state.working, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(
              S("date") + " · " + S("buyer") + " · " + S("price_litre"),
            )
          }
          if (details) {
            DateField(S("date"), date, { date = it }, enabled = !state.working)
            ChoiceField(
              S("buyer"),
              buyer,
              uiList(buyers.map { it.id to it.name }),
              { id ->
                buyer = id
                price = inputMoney(buyers.first { it.id == id }.priceCents)
              },
              enabled = !state.working,
            )
            OutlinedTextField(
              price,
              {
                price = it
              },
              enabled = !state.working,
              label = { Text(S("price_litre")) },
              keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                  keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                ),
              textStyle = MaterialTheme.typography.bodyLarge,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        } else {
          OutlinedTextField(
            reason,
            {
              reason = it
            },
            label = {
              Text(
                S("reason"),
              )
            },
            enabled = !state.working,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier =
              Modifier.fillMaxWidth().testTag(
                "milk-reason",
              ),
          )
        }
        if (state.error != null)Text(errorLabel(state.error).orEmpty(), color = MaterialTheme.colorScheme.error)
      }
    },
    confirmButton = {
      Button(onClick = {
        val submittedBuyer = buyer
        val submittedDate = date
        val submittedNumber = number
        val submittedPrice = price
        val submittedReason = reason
        actions.act(onSuccess = onDismiss) {
          if (draft ==
            null
          ) {
            it.milk(data.profile.id, submittedBuyer, submittedDate, session, submittedNumber, SuperMoney.cents(submittedPrice))
          } else {
            it.editMilk(draft.id, submittedNumber, submittedReason)
          }
        }
      }, enabled = !state.working, modifier = Modifier.heightIn(min = 56.dp).testTag("milk-save")) { Text(S("save")) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !state.working, modifier = Modifier.heightIn(min = 48.dp)) { Text(S("cancel")) }
    },
  )
}

@Composable
fun MilkHistoryList(
  rows: UiList<MilkDelivery>,
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    var edit by remember { mutableStateOf<MilkDelivery?>(null) }
    var history by remember { mutableStateOf<MilkDelivery?>(null) }
    rows.forEach { row ->
      Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("delivery-${row.occurredOn}-${row.session}")) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(dateLabel(row.occurredOn) + " · " + S(row.session.lowercase()))
          Text(S("recorded_on") + ": " + entryTimeLabel(row.recordedAt))
          Text(
            SuperMoney.quantity(row.litresMilli) + " L · " + SuperMoney.label(row.earnedCents),
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            data.parties
              .firstOrNull { it.id == row.partyId }
              ?.name
              .orEmpty(),
          )
          Row {
            TextButton(onClick = {
              edit = row
              actions.clearError()
            }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S("edit")) }
            TextButton(onClick = {
              history =
                row
            }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S("history")) }
          }
        }
      }
    }
    edit?.let { row -> MilkKeypad(data, actions, state, row.session, row) { edit = null } }
    history?.let { row ->
      AlertDialog(onDismissRequest = { history = null }, title = { Text(S("history")) }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          val changes = data.history.filter { it.recordId == row.id }.sortedBy { it.recordedAt }
          changes.forEach { event ->
            Text(if (event.reason == "Recorded")S("recorded_time")else event.reason, fontWeight = FontWeight.Bold)
            Text(entryTimeLabel(event.recordedAt))
            listOf(S("before") to event.beforeValue, S("after") to event.afterValue).forEach { (label, value) ->
              if (value != null) {
                val json = runCatching { JSONObject(value) }.getOrNull()
                if (json != null) {
                  Text(
                    label + ": " + SuperMoney.quantity(json.optLong("litresMilli")) + " L · " +
                      SuperMoney.label(json.optLong("earnedCents")),
                  )
                }
              }
            }
          }
          if (changes.isEmpty())Text(S("empty_history"))
        }
      }, confirmButton = { TextButton(onClick = { history = null }) { Text(S("close")) } })
    }
  }
}

private fun nextMilkNumber(
  number: String,
  digit: String,
): String =
  when {
    digit == "⌫" -> number.dropLast(1)
    digit == "." && number.contains('.') -> number
    number.length >= 9 -> number
    else -> number + digit
  }
