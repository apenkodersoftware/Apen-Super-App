package com.aistudio.fasmovapos.app.superapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun RestaurantMenu(
  data: BusinessData,
  actions: SuperActions,
  state: SuperUiState,
  modifier: Modifier = Modifier,
) {
  var adding by remember { mutableStateOf(false) }
  var editing by remember { mutableStateOf<CommerceItem?>(null) }
  var archived by remember { mutableStateOf(false) }
  val rows = data.items.filter { !it.voided && it.category != "CUSTOM" && it.archived == archived }.sortedBy { it.name }
  LazyColumn(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item {
      Text(S("menu"), style = MaterialTheme.typography.headlineMedium)
      Button(
        onClick = {
          adding = true
          actions.clearError()
        },
        enabled = !state.working,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add-item"),
      ) { Text(S("add_menu")) }
      OutlinedButton(
        onClick = { archived = !archived },
        enabled = !state.working,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
      ) { Text(S(if (archived) "active_menu" else "archived_menu")) }
    }
    if (rows.isEmpty()) {
      item {
        EmptyAction(
          type = Livelihood.RESTAURANT,
          text = S("empty_menu"),
          action = S("add_menu"),
          onClick = { adding = true },
        )
      }
    }
    items(rows, key = { it.id }) { item ->
      Card(Modifier.fillMaxWidth().testTag("menu-${item.name}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(item.name, style = MaterialTheme.typography.titleMedium)
          Text(SuperMoney.label(item.priceCents))
          Text(S("option_" + item.category.lowercase()))
          if (item.archived) Text(S("archive_menu"))
          OutlinedButton(
            onClick = {
              editing = item
              actions.clearError()
            },
            enabled = !state.working,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
          ) { Text(S("edit")) }
        }
      }
    }
  }
  if (adding) {
    MenuEditor(
      data = data,
      item = null,
      actions = actions,
      state = state,
      onDismiss = { adding = false },
    )
  }
  editing?.let {
    MenuEditor(
      data = data,
      item = it,
      actions = actions,
      state = state,
      onDismiss = { editing = null },
    )
  }
}
