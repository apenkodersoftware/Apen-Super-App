package com.aistudio.fasmovapos.app.superapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Date

@Composable
fun entryTimeLabel(time: Long?): String =
  if (time == null || time <= 0) {
    S("unknown_time")
  } else {
    val locale = androidx.compose.ui.platform.LocalContext.current.resources.configuration.locales[0]
    java.text
      .SimpleDateFormat("EEE d MMM yyyy HH:mm", locale)
      .apply { timeZone = SuperDates.zone }
      .format(Date(time))
  }

@Composable
fun recordKind(
  table: String,
  type: String,
): String =
  S(
    when (table) {
      "super_milk" -> "milk"
      "super_rent_charges" -> "rent"
      "super_sales", "super_sale_lines" -> "sales"
      "super_receipts", "super_allocations", "super_deductions" -> "payment"
      "super_expenses" -> "costs"
      "super_money_in" -> "other_money"
      "super_tenancies" -> "tenant"
      "super_units", "super_properties" -> "units"
      "super_items" -> if (type == "RESTAURANT") "menu" else "stock"
      "super_parties" -> if (type == "DAIRY") "buyers" else "customer"
      "super_businesses" -> "business_name"
      else -> "history"
    },
  )

@Composable
fun historyReason(reason: String): String =
  when {
    reason == "Recorded" -> S("recorded")
    reason.startsWith("Sale ") -> S("stock_used_sale")
    reason in setOf("Initial item details", "Initial stock details", "Initial menu details") -> S("initial_details")
    reason == "Custom amount archived" -> S("custom_amount")
    else -> reason
  }

@Composable
fun RecordHistoryScreen(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selected by remember { mutableStateOf<RecordReference?>(null) }
  val records =
    remember(data.ledger, data.history) {
      val refs = linkedMapOf<Pair<String, String>, RecordReference>()
      data.ledger.forEach { refs[it.sourceTable to it.sourceId] = RecordReference(it.sourceTable, it.sourceId, it.occurredOn, it.voided) }
      data.history.forEach { h ->
        if ((h.sourceTable to h.recordId) !in refs) {
          val json = h.afterValue?.let(::JSONObject)
          refs[h.sourceTable to h.recordId] =
            RecordReference(h.sourceTable, h.recordId, json?.optString("occurredOn").orEmpty(), json?.optInt("voided") == 1)
        }
      }
      refs.values.sortedWith(compareByDescending<RecordReference> { it.date }.thenBy { it.id })
    }
  LazyColumn(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Text(S("history"), style = MaterialTheme.typography.headlineMedium)
      Text(S("history_help"))
    }
    if (records.isEmpty()) {
      item {
        EmptyAction(
          type = Livelihood.valueOf(data.profile.type),
          text = S("empty_history"),
          action = S("back"),
          onClick = onBack,
        )
      }
    }
    items(records, key = { it.table + ":" + it.id }) { ref ->
      Card(onClick = {
        selected = ref
        actions.clearError()
      }, modifier = Modifier.fillMaxWidth().testTag("record-${ref.id}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(recordKind(ref.table, data.profile.type), style = MaterialTheme.typography.titleMedium)
          RecordListCaption(data, ref)
          if (ref.date.isNotBlank()) Text(dateLabel(ref.date))
          if (ref.voided) Text(S("voided"))
          Text(S("record_id") + ": " + ref.id.takeLast(12))
        }
      }
    }
  }
  selected?.let { RecordDetails(data, it, actions, state) { selected = null } }
}

@Composable
private fun RecordDetails(
  data: BusinessData,
  ref: RecordReference,
  actions: SuperActions,
  state: SuperUiState,
  dismiss: () -> Unit,
) {
  var raw by remember(ref) { mutableStateOf<JSONObject?>(null) }
  var loaded by remember(ref) { mutableStateOf(false) }
  var voiding by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf(false) }
  LaunchedEffect(ref, data.capturedAt) {
    loaded = false
    raw = withContext(Dispatchers.IO) { actions.readSource(data.profile.id, ref.table, ref.id)?.let(::JSONObject) }
    loaded = true
  }
  val json = raw
  val changes = data.history.filter { it.recordId == ref.id && it.sourceTable == ref.table }.sortedBy { it.recordedAt ?: 0L }
  AlertDialog(onDismissRequest = { if (!state.working) dismiss() }, title = { Text(recordKind(ref.table, data.profile.type)) }, text = {
    Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(S("record_id") + ": " + ref.id)
      if (!loaded) {
        Text(S("working"))
      } else if (json == null) {
        Text(S("archived_record"))
      } else {
        RecordCurrentValues(json)
        RecordButtons(ref, json, state, onVoid = { voiding = true }, onEdit = { editing = true })
      }
      RecordAuditTrail(uiList(changes))
      if (state.error != null) Text(errorLabel(state.error).orEmpty(), color = MaterialTheme.colorScheme.error)
    }
  }, confirmButton = { TextButton(onClick = dismiss, enabled = !state.working) { Text(S("close")) } })
  if (voiding) VoidRecordDialog(ref, actions, state, onDismiss = { voiding = false })
  if (editing && json != null) EditRecordDialog(data, ref, json, actions, state, onDismiss = { editing = false })
}

@Composable
private fun ExpenseOrRentEdit(
  data: BusinessData,
  ref: RecordReference,
  json: JSONObject,
  actions: SuperActions,
  state: SuperUiState,
  dismiss: () -> Unit,
) {
  val fields = mutableListOf(SuperField("amount", S("amount"), inputMoney(json.optLong("amountCents")), FieldKind.AMOUNT))
  if (ref.table == "super_expenses") {
    fields +=
      listOf(
        SuperField("date", S("date"), json.getString("occurredOn"), FieldKind.DATE),
        SuperField(
          "category",
          S("category"),
          json.getString("category"),
          FieldKind.CHOICE,
          optionLabels(
            uiList(costCategories(data.profile.type)),
          ),
        ),
      )
  }
  fields += SuperField("reason", S("reason"))
  SuperForm(
    title = S("edit"),
    fields = uiList(fields),
    busy = state.working,
    error = errorLabel(state.error),
    onDismiss = dismiss,
    onSave = { f ->
      actions.act(onSuccess = dismiss) {
        if (ref.table ==
          "super_expenses"
        ) {
          it.edits.expenseCorrection(
            ref.id,
            SuperMoney.cents(f.getValue("amount")),
            f.getValue("date"),
            f.getValue("category"),
            f.getValue("reason"),
          )
        } else {
          it.edits.rentCharge(ref.id, SuperMoney.cents(f.getValue("amount")), f.getValue("reason"))
        }
      }
    },
  )
}

@Composable
private fun RecordValues(json: JSONObject) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (json.optString("name").isNotBlank()) Text(json.getString("name"))
    val date = json.optString("occurredOn")
    if (date.length == 10) Text(S("date") + ": " + dateLabel(date))
    if (json.has("litresMilli")) Text(S("litres") + ": " + SuperMoney.quantity(json.optLong("litresMilli")) + " L")
    listOf(
      "amountCents" to "amount",
      "earnedCents" to "earned",
      "priceCents" to "price",
      "depositCents" to "held_money",
      "discountCents" to "discount",
      "rentCents" to "rent",
      "costCents" to "cost_price",
      "quarterPriceCents" to "quarter_price",
      "halfPriceCents" to "half_price",
    ).forEach { (field, label) ->
      if (json.has(field)) Text(S(label) + ": " + SuperMoney.label(json.optLong(field)))
    }
    RecordQuantitiesAndDates(json)
    if (json.optInt("voided") == 1) Text(S("voided"))
  }
}

@Composable
private fun RecordCurrentValues(json: JSONObject) {
  val originalTime =
    when {
      !json.isNull("recordedAt") -> json.optLong("recordedAt")
      !json.isNull("createdAt") -> json.optLong("createdAt")
      else -> null
    }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(S("recorded_on") + ": " + entryTimeLabel(originalTime))
    RecordValues(json)
  }
}

@Composable
private fun RecordButtons(
  ref: RecordReference,
  json: JSONObject,
  state: SuperUiState,
  onVoid: () -> Unit,
  onEdit: () -> Unit,
) {
  val voided = json.optInt("voided") == 1
  val sourceCost = ref.table == "super_expenses" && !json.isNull("deductionId")
  val cashSaleReceipt = ref.table == "super_receipts" && !json.isNull("saleId") && json.isNull("partyId")
  val reversible = ref.table in setOf("super_milk", "super_sales", "super_receipts", "super_expenses", "super_money_in")
  val independentlyChangeable = !voided && !sourceCost
  val canVoid = independentlyChangeable && reversible && !cashSaleReceipt
  val canEdit = independentlyChangeable && ref.table in setOf("super_milk", "super_expenses", "super_rent_charges")
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (canVoid) {
      OutlinedButton(
        onClick = onVoid,
        enabled = !state.working,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("void-record"),
      ) { Text(S("void")) }
    }
    if (sourceCost) Text(S("linked_cost_help"))
    if (cashSaleReceipt) Text(S("void_sale_help"))
    if (canEdit) {
      OutlinedButton(
        onClick = onEdit,
        enabled = !state.working,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
      ) { Text(S("edit")) }
    }
  }
}

@Composable
private fun RecordAuditTrail(changes: UiList<HistoryRow>) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(S("history"), style = MaterialTheme.typography.titleLarge)
    changes.forEach { h ->
      Text(historyReason(h.reason), style = MaterialTheme.typography.titleMedium)
      Text(entryTimeLabel(h.recordedAt))
      h.beforeValue?.let {
        Text(S("before"))
        RecordValues(JSONObject(it))
      }
      h.afterValue?.let {
        Text(S("after"))
        RecordValues(JSONObject(it))
      }
    }
    if (changes.isEmpty()) Text(S("empty_history"))
  }
}

@Composable
private fun VoidRecordDialog(
  ref: RecordReference,
  actions: SuperActions,
  state: SuperUiState,
  onDismiss: () -> Unit,
) {
  SuperForm(
    title = S("void"),
    fields = uiList(listOf(SuperField("reason", S("reason")))),
    busy = state.working,
    error = errorLabel(state.error),
    onDismiss = onDismiss,
    onSave = { f ->
      actions.act(onSuccess = onDismiss) { repo ->
        val reason = f.getValue("reason")
        when (ref.table) {
          "super_milk" -> repo.dairy.void(ref.id, reason)
          "super_sales" -> repo.edits.sale(ref.id, reason)
          "super_receipts" -> repo.edits.receipt(ref.id, reason)
          "super_expenses" -> repo.edits.expense(ref.id, reason)
          "super_money_in" -> repo.edits.moneyIn(ref.id, reason)
        }
      }
    },
    extra = { Text(S("void_help")) },
  )
}

@Composable
private fun EditRecordDialog(
  data: BusinessData,
  ref: RecordReference,
  json: JSONObject,
  actions: SuperActions,
  state: SuperUiState,
  onDismiss: () -> Unit,
) {
  val milk = data.milk.firstOrNull { it.id == ref.id && ref.table == "super_milk" }
  if (milk != null) {
    MilkKeypad(data, actions, state, milk.session, milk, onDismiss)
  } else {
    ExpenseOrRentEdit(data, ref, json, actions, state, onDismiss)
  }
}

@Composable
private fun RecordListCaption(
  data: BusinessData,
  ref: RecordReference,
) {
  val row = data.ledger.firstOrNull { it.sourceTable == ref.table && it.sourceId == ref.id }
  val name = recordConfigurationName(data, ref)
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    if (name != null) Text(name)
    if (row != null) {
      val amount =
        listOf(
          row.earnedCents,
          row.receivedCents,
          row.spentCents,
          row.otherInCents,
          row.heldCents,
          row.settledCents,
          row.owedDeltaCents,
        ).firstOrNull { it != 0L } ?: 0L
      Text(SuperMoney.label(amount))
    }
  }
}

private fun recordConfigurationName(
  data: BusinessData,
  ref: RecordReference,
): String? =
  when (ref.table) {
    "super_businesses" -> data.profile.name
    "super_parties" -> data.parties.firstOrNull { it.id == ref.id }?.name
    "super_items" -> data.items.firstOrNull { it.id == ref.id }?.name
    "super_units" -> data.units.firstOrNull { it.id == ref.id }?.label
    "super_properties" -> data.properties.firstOrNull { it.id == ref.id }?.name
    else -> null
  }

@Composable
private fun RecordQuantitiesAndDates(json: JSONObject) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf("stockMilli" to "stock_left", "quantityMilli" to "quantity", "minimumMilli" to "minimum_stock").forEach { (field, label) ->
      if (json.has(field) && !json.isNull(field)) Text(S(label) + ": " + SuperMoney.quantity(json.optLong(field)))
    }
    listOf("label" to "unit", "phone" to "phone").forEach { (field, label) ->
      if (json.optString(field).isNotBlank()) Text(S(label) + ": " + json.getString(field))
    }
    listOf("moveIn" to "move_in", "moveOut" to "vacant_from", "dueDate" to "date").forEach { (field, label) ->
      val date = json.optString(field)
      if (date.length == 10) Text(S(label) + ": " + dateLabel(date))
    }
    if (json.has("dueDay")) Text(S("due_day") + ": " + json.optInt("dueDay"))
  }
}
