package com.aistudio.fasmovapos.app.superapp

import android.content.Intent
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun monthLabel(month: String): String {
  val locale = LocalContext.current.resources.configuration.locales[0] ?: Locale.UK
  return java.text
    .SimpleDateFormat("MMMM yyyy", locale)
    .apply {
      timeZone = SuperDates.zone
    }.format(SuperDates.formatter("yyyy-MM-dd").parse("$month-01")!!)
}

@Composable
fun RentHome(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  onUnits: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var month by rememberSaveable { mutableStateOf(SuperDates.today().take(7)) }
  var property by rememberSaveable { mutableStateOf("ALL") }
  var created by remember { mutableStateOf(false) }
  var selected by remember { mutableStateOf<RentalUnitRow?>(null) }
  val context = LocalContext.current
  val label = monthLabel(month)
  val units = unitsAtProperty(data, property)
  val ids = units.map { it.id }.toSet()
  val charges = data.rents.filter { !it.voided && it.unitId in ids && it.month == month }
  val expected = charges.sumOf { it.amountCents }
  val unpaid = charges.sumOf { data.chargeBalance(it.id) }
  val arrears = data.rents.filter { !it.voided && it.unitId in ids && it.month <= month }.sumOf { data.chargeBalance(it.id) }
  Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    LivelihoodHeading(
      type = Livelihood.RENTAL,
      title = S("rent"),
      subtitle = S("rental_intro"),
    )
    Row(Modifier.fillMaxWidth()) {
      TextButton(onClick = {
        val target = SuperDates.addMonths(month, -1)
        created = false
        var added = 0
        actions.act(onSuccess = {
          month = target
          created = added > 0
        }) { added = it.rentCharges(data.profile.id, target) }
      }, enabled = !state.working, modifier = Modifier.heightIn(min = 48.dp).testTag("month-previous")) {
        Text(
          "‹ " + S("previous_month"),
        )
      }
      TextButton(onClick = {
        val target = SuperDates.addMonths(month, 1)
        created = false
        var added = 0
        actions.act(onSuccess = {
          month = target
          created = added > 0
        }) { added = it.rentCharges(data.profile.id, target) }
      }, enabled = !state.working, modifier = Modifier.heightIn(min = 48.dp).testTag("month-next")) {
        Text(
          S("next_month") + " ›",
        )
      }
    }
    Text(label, style = MaterialTheme.typography.headlineSmall)
    Text(S("rent_home_help"))
    val preparedToday = chargesPreparedToday(data, month)
    if (created ||
      preparedToday
    ) {
      Text(context.getString(superLabelIds.getValue("charges_created"), label), modifier = Modifier.testTag("charges-created"))
    }
    if (data.properties.size >
      1
    ) {
      ChoiceField(
        S("property"),
        property,
        uiList(listOf("ALL" to S("all_properties")) + data.properties.map { it.id to it.name }),
        { property = it },
      )
    }
    Card(Modifier.fillMaxWidth()) {
      HeroNumber(S("collected_expected"), SuperMoney.label(expected - unpaid) + " / " + SuperMoney.label(expected))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Card(Modifier.weight(1f)) { HeroNumber(S("arrears"), SuperMoney.label(arrears)) }
      Card(Modifier.weight(1f)) { HeroNumber(S("vacant_units"), units.count { data.tenancy(it.id, month) == null }.toString()) }
    }
    if (units.isEmpty()) {
      EmptyAction(
        type = Livelihood.RENTAL,
        text = S("empty_units"),
        action = S("add_unit"),
        onClick = onUnits,
      )
    }
    units.forEach { unit ->
      val active = data.tenancy(unit.id, month)
      val current = charges.filter { it.unitId == unit.id }
      val due = current.sumOf { data.chargeBalance(it.id) }
      val status =
        when {
          active == null -> "vacant"
          current.isNotEmpty() && due == 0L -> "paid"
          current.sumOf { it.amountCents } > due -> "part_paid"
          else -> "unpaid"
        }
      val unitDebt = data.rents.filter { !it.voided && it.unitId == unit.id && it.month <= month }.sumOf { data.chargeBalance(it.id) }
      Card(onClick = {
        selected = unit
        actions.clearError()
      }, modifier = Modifier.fillMaxWidth().testTag("unit-${unit.label}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row {
            Text(unit.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(S(status))
          }
          Text(
            S("unpaid_through_month") + ": " + SuperMoney.label(unitDebt),
          )
          Text(
            data.properties
              .firstOrNull { it.id == unit.propertyId }
              ?.name
              .orEmpty(),
          )
        }
      }
    }
  }
  selected?.let { u ->
    UnitAccount(
      data = data,
      unit = u,
      month = month,
      actions = actions,
      state = state,
      onDismiss = { selected = null },
    )
  }
}

@Composable
fun UnitsScreen(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  modifier: Modifier = Modifier,
) {
  var property by remember { mutableStateOf(false) }
  var unit by remember { mutableStateOf(false) }
  var selected by remember { mutableStateOf<RentalUnitRow?>(null) }
  Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(S("units"), style = MaterialTheme.typography.headlineMedium)
    Button(onClick = {
      property = true
      actions.clearError()
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add-property")) { Text(S("add_property")) }
    OutlinedButton(onClick = {
      unit = true
      actions.clearError()
    }, enabled = data.properties.isNotEmpty(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add-unit")) {
      Text(S("add_unit"))
    }
    if (data.units.isEmpty())Text(S("empty_units"))
    data.units.filter { !it.voided }.forEach { u ->
      Card(onClick = { selected = u }, modifier = Modifier.fillMaxWidth().testTag("unit-${u.label}")) {
        Column(Modifier.padding(16.dp)) {
          Text(u.label, style = MaterialTheme.typography.titleMedium)
          Text(
            data.properties
              .firstOrNull {
                it.id ==
                  u.propertyId
              }?.name
              .orEmpty(),
          )
          Text(SuperMoney.label(u.rentCents))
        }
      }
    }
  }
  if (property) {
    SuperForm(
      title = S("add_property"),
      fields = uiList(listOf(SuperField("name", S("name")))),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = { property = false },
      onSave = { f ->
        actions.act(onSuccess = {
          property =
            false
        }) { it.property(data.profile.id, f["name"].orEmpty()) }
      },
    )
  }
  if (unit) {
    SuperForm(
      title = S("add_unit"),
      fields =
        uiList(
          listOf(
            SuperField(
              "property",
              S("property"),
              data.properties.first().id,
              FieldKind.CHOICE,
              data.properties.map {
                it.id to
                  it.name
              },
            ),
            SuperField("label", S("unit_label")),
            SuperField("rent", S("monthly_rent"), kind = FieldKind.AMOUNT),
            SuperField("due", S("due_day"), "5", FieldKind.NUMBER),
            SuperField("tenant", S("tenant")),
            SuperField("phone", S("phone")),
            SuperField(
              "moveIn",
              S("move_in"),
              SuperDates.today().take(7) + "-01",
              FieldKind.DATE,
            ),
            SuperField("deposit", S("deposit"), "0", FieldKind.AMOUNT),
          ),
        ),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = {
        unit =
          false
      },
      onSave = { f ->
        actions.act(onSuccess = { unit = false }) { repo ->
          repo.store.transaction {
            repo.unit(
              data.profile.id,
              f["property"].orEmpty(),
              f["label"].orEmpty(),
              SuperMoney.cents(f["rent"].orEmpty()),
              f["due"].orEmpty().toInt(),
              f["tenant"].orEmpty(),
              f["moveIn"].orEmpty(),
              SuperMoney.cents(f["deposit"].orEmpty()),
              f["phone"].orEmpty(),
            )
            repo.rentCharges(data.profile.id, SuperDates.today().take(7))
          }
        }
      },
      extra = { Text(S("deposit_help")) },
    )
  }
  selected?.let { u ->
    UnitAccount(
      data = data,
      unit = u,
      month = SuperDates.today().take(7),
      actions = actions,
      state = state,
      onDismiss = { selected = null },
    )
  }
}

@Composable
fun UnitAccount(
  data: BusinessData,
  unit: RentalUnitRow,
  month: String,
  actions: SuperActions,
  state: SuperUiState,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val tenancies = data.tenancies.filter { !it.voided && it.unitId == unit.id }.sortedBy { it.moveIn }
  var selectedTenant by remember(unit.id) { mutableStateOf<String?>(null) }
  val tenant = tenancies.firstOrNull { it.id == selectedTenant } ?: data.tenancy(unit.id, month) ?: tenancies.lastOrNull()
  val party = data.parties.firstOrNull { it.id == tenant?.partyId }
  val property =
    data.properties
      .firstOrNull { it.id == unit.propertyId }
      ?.name
      .orEmpty()
  var payment by remember { mutableStateOf(false) }
  var moveOut by remember { mutableStateOf(false) }
  var newTenant by remember { mutableStateOf(false) }
  var share by remember { mutableStateOf<String?>(null) }
  AlertDialog(onDismissRequest = onDismiss, title = { Text(property + " · " + unit.label) }, text = {
    Column(modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      TenantChoice(data, uiList(tenancies), tenant, state.working, onSelect = { selectedTenant = it })
      Text(party?.name ?: S("vacant"))
      Text(S("oldest_first"))
      Text(S("owed") + ": " + SuperMoney.label(party?.let { data.partyBalance(it.id) } ?: 0))
      if (tenant != null)Text(S("held_money") + ": " + SuperMoney.label(tenant.depositCents))
      if (party != null) {
        Button(
          onClick = {
            payment = true
            actions.clearError()
          },
          enabled =
            data.partyBalance(
              party.id,
            ) > 0,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("record-rent-payment"),
        ) {
          Text(S("payment"))
        }
        OutlinedButton(onClick = {
          share =
            context.getString(superLabelIds.getValue("reminder_body"), property, unit.label, SuperMoney.label(data.partyBalance(party.id)))
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("rent-reminder")) { Text(S("reminder")) }
      }
      if (tenant?.moveOut == null &&
        tenant != null
      ) {
        OutlinedButton(onClick = {
          moveOut = true
          actions.clearError()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(S("move_out")) }
      }
      if (data.tenancy(unit.id, month) ==
        null
      ) {
        OutlinedButton(onClick = {
          newTenant = true
          actions.clearError()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(S("add_tenant")) }
      }
      data.rents.filter { !it.voided && it.unitId == unit.id }.sortedBy { it.month }.forEach { r ->
        Text(
          monthLabel(r.month) + ": " + SuperMoney.label(data.chargeBalance(r.id)),
        )
      }
      RentalReceiptHistory(data, party?.id, property, unit.label, onShare = { share = it })
    }
  }, confirmButton = { TextButton(onClick = onDismiss) { Text(S("close")) } })
  if (payment && party != null) {
    ReceiptDialog(
      data = data,
      party = party,
      state = state,
      actions = actions,
      onDismiss = { payment = false },
    )
  }
  if (moveOut) {
    SuperForm(
      title =
        S(
          "move_out",
        ),
      fields =
        uiList(
          listOf(
            SuperField("date", S("vacant_from"), SuperDates.addMonths(month, 1) + "-01", FieldKind.DATE),
            SuperField("reason", S("reason")),
          ),
        ),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = {
        moveOut =
          false
      },
      onSave = { f ->
        actions.act(onSuccess = {
          moveOut = false
        }) { it.moveOut(unit.id, f["date"].orEmpty(), f["reason"].orEmpty()) }
      },
      extra = { Text(S("move_out_help")) },
    )
  }
  if (newTenant) {
    SuperForm(
      title =
        S(
          "add_tenant",
        ),
      fields =
        uiList(
          listOf(
            SuperField("name", S("tenant")),
            SuperField("phone", S("phone")),
            SuperField("moveIn", S("move_in"), SuperDates.today(), FieldKind.DATE),
            SuperField("deposit", S("deposit"), "0", FieldKind.AMOUNT),
          ),
        ),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = {
        newTenant =
          false
      },
      onSave = { f ->
        actions.act(onSuccess = { newTenant = false }) {
          it.rental.tenancy(
            unit.id,
            f["name"].orEmpty(),
            f["phone"].orEmpty(),
            f["moveIn"].orEmpty(),
            SuperMoney.cents(f["deposit"].orEmpty()),
          )
          it.rentCharges(data.profile.id, month)
        }
      },
    )
  }
  share?.let { text -> ShareTextPreview(text) { share = null } }
}

@Composable
fun ShareTextPreview(
  text: String,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val chooser = S("share")
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(S("share"))
    },
    text = {
      Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
        Text(text, modifier = Modifier.testTag("share-preview-text"))
        Text(S("share_notice"))
      }
    },
    confirmButton = {
      Button(onClick = {
        try {
          context.startActivity(
            Intent.createChooser(
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
              },
              chooser,
            ),
          )
        } catch (
          _: android.content.ActivityNotFoundException,
        ) {
          android.widget.Toast
            .makeText(context, translated(context, "error"), android.widget.Toast.LENGTH_LONG)
            .show()
        }
      }, modifier = Modifier.heightIn(min = 48.dp).testTag("open-share-sheet")) { Text(S("share")) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(S("close")) } },
  )
}

private fun chargesPreparedToday(
  data: BusinessData,
  month: String,
): Boolean =
  data.rents.any {
    !it.voided &&
      it.month == month &&
      it.recordedAt?.let(SuperDates::today) == SuperDates.today()
  }

@Composable
private fun RentalReceiptHistory(
  data: BusinessData,
  partyId: String?,
  property: String,
  unitLabel: String,
  onShare: (String) -> Unit,
) {
  val context = LocalContext.current
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    data.receipts.filter { !it.voided && it.partyId == partyId }.sortedByDescending { it.recordedAt ?: 0L }.forEach { r ->
      val date = dateLabel(r.occurredOn)
      Text(SuperMoney.label(r.amountCents) + " · " + date)
      data.allocations.filter { !it.voided && it.receiptId == r.id }.forEach { a ->
        val charge = data.rents.firstOrNull { it.id == a.chargeId }
        if (charge !=
          null
        ) {
          Text(S("allocated") + ": " + monthLabel(charge.month) + " · " + SuperMoney.label(a.amountCents))
        }
      }
      TextButton(onClick = {
        onShare(
          context.getString(
            superLabelIds.getValue("receipt_body"),
            property,
            unitLabel,
            SuperMoney.label(r.amountCents),
            date,
            r.reference,
          ),
        )
      }, modifier = Modifier.heightIn(min = 48.dp)) { Text(S("rent_receipt")) }
    }
  }
}

private fun unitsAtProperty(
  data: BusinessData,
  property: String,
): List<RentalUnitRow> =
  data.units.filter {
    !it.voided &&
      (property == "ALL" || it.propertyId == property)
  }

@Composable
private fun TenantChoice(
  data: BusinessData,
  tenancies: UiList<TenancyRow>,
  tenant: TenancyRow?,
  busy: Boolean,
  onSelect: (String) -> Unit,
) {
  if (tenancies.size > 1 && tenant != null) {
    ChoiceField(
      S("tenant"),
      tenant.id,
      uiList(
        tenancies.map { t ->
          t.id to ((data.parties.firstOrNull { it.id == t.partyId }?.name ?: "—") + " · " + dateLabel(t.moveIn))
        },
      ),
      { onSelect(it) },
      enabled = !busy,
    )
  }
}
