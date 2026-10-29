package com.aistudio.fasmovapos.app.superapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SuperRoute(
  tab: String,
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    when (tab) {
      "rent" ->
        RentHome(
          data = data,
          actions = actions,
          state = state,
          onUnits = { onNavigate("units") },
        )
      "units" ->
        UnitsScreen(
          data = data,
          actions = actions,
          state = state,
        )
      "milk" ->
        DairyHome(
          data = data,
          actions = actions,
          state = state,
          onBuyers = { onNavigate("buyers") },
          onHistory = { onNavigate("history") },
        )
      "sell" ->
        CommerceHome(
          data = data,
          actions = actions,
          state = state,
          onNavigate = onNavigate,
        )
      "buyers", "deni" ->
        PartyScreen(
          data = data,
          actions = actions,
          state = state,
        )
      "stock", "menu" ->
        ItemsScreen(
          data = data,
          actions = actions,
          state = state,
        )
      "costs" ->
        CostsScreen(
          data = data,
          actions = actions,
          state = state,
        )
      "summary", "today" ->
        SuperSummary(
          data = data,
          actions = actions,
          state = state,
          onNavigate = onNavigate,
        )
      "history" ->
        RecordHistoryScreen(
          data = data,
          actions = actions,
          state = state,
          onBack = { onNavigate(tabsFor(data.profile.type).first()) },
        )
      "settings" ->
        BusinessSettings(
          data = data,
          actions = actions,
          state = state,
        )
      else ->
        Column(Modifier.padding(20.dp)) {
          LivelihoodArt(Livelihood.valueOf(data.profile.type), Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 140.dp))
          Text(S("soon"))
          Text(S("soon_help"))
        }
    }
  }
}

@Composable
fun BusinessSettings(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  modifier: Modifier = Modifier,
) {
  var money by remember { mutableStateOf(false) }
  var rename by remember { mutableStateOf(false) }
  Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text(S("settings"), style = MaterialTheme.typography.headlineMedium)
    Text(data.profile.name)
    OutlinedButton(onClick = {
      rename = true
      actions.clearError()
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(S("business_name")) }
    Text(S("non_sales_help"))
    Button(onClick = {
      money = true
      actions.clearError()
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(S("non_sales")) }
    Text(S("other_money") + ": " + SuperMoney.label(data.month.otherInCents))
    Text(S("held_money") + ": " + SuperMoney.label(data.month.heldCents))
  }
  if (rename) {
    SuperForm(
      title =
        S(
          "business_name",
        ),
      fields =
        uiList(
          listOf(
            SuperField("name", S("business_name"), data.profile.name),
            SuperField("reason", S("reason")),
          ),
        ),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = {
        rename =
          false
      },
      onSave = { f ->
        actions.act(
          onSuccess = { rename = false },
        ) { it.renameBusiness(data.profile.id, f["name"].orEmpty(), f["reason"].orEmpty()) }
      },
    )
  }
  if (money) {
    SuperForm(
      title =
        S(
          "non_sales",
        ),
      fields =
        uiList(
          listOf(
            SuperField(
              "kind",
              S("money_kind"),
              "OWNER_CAPITAL",
              FieldKind.CHOICE,
              optionLabels(
                uiList(listOf("OWNER_CAPITAL", "LOAN_PROCEEDS", "GRANT", "TRANSFER")),
              ),
            ),
            SuperField("amount", S("amount"), kind = FieldKind.AMOUNT),
            SuperField("date", S("date"), SuperDates.today(), FieldKind.DATE),
          ),
        ),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = {
        money =
          false
      },
      onSave = { f ->
        actions.act(onSuccess = {
          money = false
        }) { it.moneyIn(data.profile.id, SuperMoney.cents(f["amount"].orEmpty()), f["date"].orEmpty(), f["kind"].orEmpty()) }
      },
    )
  }
}
