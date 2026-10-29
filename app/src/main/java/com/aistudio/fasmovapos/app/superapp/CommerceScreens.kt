package com.aistudio.fasmovapos.app.superapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@Composable
fun stockLabel(item: CommerceItem): String {
  val amount = SuperMoney.quantity(item.stockMilli ?: 0)
  val context = LocalContext.current
  val unit =
    when (item.unit.lowercase()) {
      "pcs" -> "pcs"
      "kg" -> "kg"
      "ltr" -> "ltr"
      "box" -> "box"
      "bag" -> "bag"
      else -> null
    }
  return if (unit == "pcs") {
    context.getString(superLabelIds.getValue("stock_left"), amount)
  } else {
    context.getString(superLabelIds.getValue("quantity_left"), amount, if (unit == null) item.unit else S("option_$unit"))
  }
}

@Composable
fun ItemsScreen(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  modifier: Modifier = Modifier,
) {
  if (data.profile.type == "RESTAURANT") {
    RestaurantMenu(
      data = data,
      actions = actions,
      state = state,
    )
    return
  }
  var adding by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf<CommerceItem?>(null) }
  var quick by remember { mutableStateOf<CommerceItem?>(null) }
  var search by remember { mutableStateOf("") }
  val rows = data.items.filter { !it.voided && !it.archived && it.name.contains(search, true) }.sortedBy { it.name }
  LazyColumn(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Text(S("stock"), style = MaterialTheme.typography.headlineMedium)
      Button(
        onClick = {
          adding = true
          actions.clearError()
        },
        enabled = !state.working,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add-item"),
      ) { Text(S("add_item")) }
      OutlinedTextField(search, { search = it }, singleLine = true, label = { Text(S("search")) }, modifier = Modifier.fillMaxWidth())
    }
    if (rows.isEmpty()) {
      item {
        EmptyAction(
          type = Livelihood.RETAIL,
          text = S("empty_stock"),
          action = S("add_item"),
          onClick = { adding = true },
        )
      }
    }
    items(rows, key = { it.id }) { item ->
      Card(Modifier.fillMaxWidth().testTag("stock-${item.name}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(item.name, style = MaterialTheme.typography.titleMedium)
          Text(SuperMoney.label(item.priceCents))
          Text(stockLabel(item))
          if ((item.stockMilli ?: 0L) <= item.minimumMilli) Text(S("low_stock"))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                quick = item
                actions.clearError()
              },
              enabled = !state.working,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(S("add_stock")) }
            OutlinedButton(
              onClick = {
                editing = item
                actions.clearError()
              },
              enabled = !state.working,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(S("restock")) }
          }
        }
      }
    }
  }
  if (adding) RetailItemEditor(data, null, actions, state) { adding = false }
  editing?.let { RetailItemEditor(data, it, actions, state) { editing = null } }
  quick?.let { item ->
    SuperForm(
      title = S("add_stock") + ": " + item.name,
      fields =
        uiList(
          listOf(
            SuperField("quantity", S("add_stock"), kind = FieldKind.AMOUNT),
            SuperField("reason", S("reason")),
          ),
        ),
      busy = state.working,
      error = errorLabel(state.error),
      onDismiss = { quick = null },
      onSave = { f ->
        actions.act(onSuccess = { quick = null }) {
          it.commerce.adjustItem(item.id, item.priceCents, SuperMoney.milli(f.getValue("quantity")), f.getValue("reason"))
        }
      },
    )
  }
}

@Composable
private fun RetailItemEditor(
  data: BusinessData,
  item: CommerceItem?,
  actions: SuperActions,
  state: SuperUiState,
  dismiss: () -> Unit,
) {
  val fields =
    mutableListOf(
      SuperField("name", S("name"), item?.name.orEmpty()),
      SuperField("price", S("price"), item?.let { inputMoney(it.priceCents) }.orEmpty(), FieldKind.AMOUNT),
      SuperField("category", S("category"), item?.category ?: "GENERAL"),
      SuperField("quantity", S(if (item == null) "quantity" else "add_stock"), "0", FieldKind.AMOUNT),
      SuperField("cost", S("cost_price"), inputMoney(item?.costCents ?: 0), FieldKind.AMOUNT),
      SuperField("minimum", S("minimum_stock"), SuperMoney.quantity(item?.minimumMilli ?: 0), FieldKind.AMOUNT),
      SuperField("quarter", S("quarter_price"), inputMoney(item?.quarterPriceCents ?: 0), FieldKind.AMOUNT),
      SuperField("half", S("half_price"), inputMoney(item?.halfPriceCents ?: 0), FieldKind.AMOUNT),
    )
  if (item == null) {
    fields +=
      SuperField(
        "unit",
        S("unit"),
        "Pcs",
        FieldKind.CHOICE,
        optionLabels(
          uiList(listOf("Pcs", "Kg", "Ltr", "Box", "Bag")),
        ),
      )
  } else {
    fields += SuperField("reason", S("reason"))
  }
  SuperForm(
    title = S(if (item == null) "add_item" else "restock"),
    fields = uiList(fields),
    busy = state.working,
    error = errorLabel(state.error),
    onDismiss = dismiss,
    onSave = { f ->
      actions.act(onSuccess = dismiss) { repo ->
        repo.store.transaction {
          val q = BigDecimal(f.getValue("quantity")).movePointRight(3).longValueExact()
          val price = SuperMoney.cents(f.getValue("price"))
          val reason = if (item == null) "Initial stock details" else f.getValue("reason")
          val id = item?.id ?: repo.commerce.item(data.profile.id, f.getValue("name"), price, f.getValue("category"), q, f.getValue("unit"))
          if (item != null) repo.commerce.adjustItem(id, price, q, reason)
          repo.retailDetails(
            id,
            f.getValue("name"),
            f.getValue("category"),
            SuperMoney.cents(f.getValue("cost")),
            BigDecimal(f.getValue("minimum")).movePointRight(3).longValueExact(),
            SuperMoney.cents(f.getValue("quarter")),
            SuperMoney.cents(f.getValue("half")),
            reason,
          )
        }
      }
    },
    extra = { Text(S("tier_price_help")) },
  )
}
