package com.aistudio.fasmovapos.app.superapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommerceHome(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val restaurant = data.profile.type == "RESTAURANT"
  var cart by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
  var search by remember { mutableStateOf("") }
  var quantityItem by remember { mutableStateOf<CommerceItem?>(null) }
  var payment by remember { mutableStateOf(false) }
  var custom by remember { mutableStateOf(false) }
  var checkout by remember { mutableStateOf(false) }
  var saved by remember { mutableStateOf(false) }
  val active = remember(data.items) { data.items.filter { !it.voided && !it.archived } }
  val ordered =
    remember(active, data.lines, restaurant) {
      if (restaurant) menuBySales(active, data.lines) else active.sortedBy { it.name }
    }
  val visible = ordered.filter { it.name.contains(search, true) }
  val total = orderTotal(cart, active)

  fun finish() {
    checkout = false
    cart = emptyMap()
    payment = false
    custom = false
    saved = true
  }
  Column(modifier.fillMaxSize()) {
    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      item {
        CommerceHeader(data, restaurant, search, onSearch = { search = it })
      }
      if (saved) item { Text(S("sale_saved"), modifier = Modifier.testTag("sale-saved")) }
      if (visible.isEmpty()) {
        item {
          EmptyAction(
            type = Livelihood.valueOf(data.profile.type),
            text = S(if (restaurant) "empty_menu" else "empty_stock"),
            action = S(if (restaurant) "add_menu" else "add_item"),
            onClick = {
              onNavigate(if (restaurant) "menu" else "stock")
            },
          )
        }
      }
      items(visible.chunked(2), key = { it.first().id }) { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          row.forEach { item ->
            CommerceTile(
              item = item,
              restaurant = restaurant,
              count = cart[item.id],
              enabled = !state.working,
              modifier = Modifier.weight(1f),
              add = {
                if (item.unit in
                  setOf("Pcs", "Box", "Bag")
                ) {
                  cart = cart + (item.id to ((cart[item.id] ?: 0) + 1000))
                } else {
                  quantityItem = item
                }
                saved = false
              },
              quantity = { quantityItem = item },
            )
          }
          if (row.size == 1) Column(Modifier.weight(1f)) {}
        }
      }
      if (restaurant) {
        item {
          OutlinedButton(
            onClick = {
              custom = true
              actions.clearError()
            },
            enabled = !state.working,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("custom-amount"),
          ) { Text(S("custom_amount")) }
        }
      }
      if (state.error != null) item { Text(errorLabel(state.error).orEmpty(), color = MaterialTheme.colorScheme.error) }
    }
    if (cart.isNotEmpty()) {
      OrderBar(total, state.working, { cart = emptyMap() }) { method ->
        if (method == "CREDIT") {
          payment = true
          actions.clearError()
        } else {
          val submitted = cart.toMap()
          actions.act(onSuccess = { finish() }) { it.commerce.sale(data.profile.id, submitted, total, SuperDates.today(), method) }
        }
      }
    }
    if (cart.isNotEmpty() && !restaurant) {
      OutlinedButton(
        onClick = {
          checkout = true
          actions.clearError()
        },
        enabled = !state.working,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("retail-checkout"),
      ) { Text(S("checkout")) }
    }
  }

  CommerceDialogs(
    data,
    actions,
    state,
    CartDialogs(quantityItem, payment, custom, checkout, uiMap(cart)),
    onDismiss = {
      quantityItem = null
      payment = false
      custom = false
      checkout = false
    },
    onFinish = { finish() },
    onQuantity = { id, q ->
      cart = cart + (id to q)
      quantityItem = null
      saved = false
    },
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommerceTile(
  item: CommerceItem,
  restaurant: Boolean,
  count: Long?,
  enabled: Boolean,
  add: () -> Unit,
  quantity: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier
      .heightIn(
        min = if (restaurant) 116.dp else 130.dp,
      ).combinedClickable(enabled = enabled, onClick = add, onLongClick = quantity)
      .testTag("tile-${item.name}"),
  ) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      if (restaurant) {
        val bitmap =
          remember(item.photo) {
            item.photo?.let {
              android.graphics.BitmapFactory
                .decodeByteArray(it, 0, it.size)
                ?.asImageBitmap()
            }
          }
        if (bitmap != null) {
          Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxWidth().height(36.dp))
        } else {
          Icon(
            when (item.category) {
              "DRINKS" -> Icons.Default.LocalCafe
              "SNACKS" -> Icons.Default.BakeryDining
              else -> Icons.Default.Restaurant
            },
            null,
            Modifier.size(28.dp),
          )
        }
      }
      Text(item.name, style = MaterialTheme.typography.titleMedium)
      Text(SuperMoney.label(item.priceCents), fontSize = 22.sp)
      if (!restaurant && item.stockMilli != null) Text(stockLabel(item))
      if (count != null) Text(S("quantity") + ": " + SuperMoney.quantity(count))
    }
  }
}

@Composable
private fun OrderBar(
  total: Long,
  busy: Boolean,
  clear: () -> Unit,
  tender: (String) -> Unit,
) {
  Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(S("order") + ": " + SuperMoney.label(total), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
      TextButton(onClick = clear, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(S("clear_order")) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      listOf("CASH", "MPESA", "CREDIT").forEach { method ->
        Button(
          onClick = { tender(method) },
          enabled = !busy,
          modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("tender-$method"),
        ) {
          Text(S("option_" + method.lowercase()))
        }
      }
    }
  }
}

private fun orderTotal(
  cart: Map<String, Long>,
  active: List<CommerceItem>,
): Long = cart.entries.sumOf { (id, q) -> active.firstOrNull { it.id == id }?.let { SuperPricing.lineTotal(it, q) } ?: 0L }

@Composable
private fun CommerceHeader(
  data: BusinessData,
  restaurant: Boolean,
  search: String,
  onSearch: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (restaurant) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LivelihoodArt(Livelihood.RESTAURANT, Modifier.size(80.dp, 64.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
          Text(S("sell"), style = MaterialTheme.typography.headlineMedium)
          Text(S("takings") + ": " + SuperMoney.label(data.today.receivedCents))
        }
      }
    } else {
      LivelihoodHeading(
        type = Livelihood.RETAIL,
        title = S("sell"),
        subtitle = S("retail_intro"),
      )
      Text(S("takings") + ": " + SuperMoney.label(data.today.receivedCents))
      OutlinedTextField(search, { onSearch(it) }, label = { Text(S("search")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
  }
}
