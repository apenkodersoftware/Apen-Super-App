package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.ui.components.DisplayControl
import com.aistudio.fasmovapos.app.ui.components.NavTabItem
import com.aistudio.fasmovapos.app.ui.language.AppLanguage

val SIMPLE_TABS =
  listOf(
    NavTabItem("POS", R.string.simple_sell, Icons.Default.PointOfSale),
    NavTabItem("INVENTORY", R.string.simple_stock, Icons.Default.Inventory2),
    NavTabItem("TODAY", R.string.simple_today, Icons.Default.Today),
    NavTabItem("MORE", R.string.simple_more, Icons.Default.MoreHoriz),
  )

@Composable
fun SimpleHeader(
  shopName: String,
  displayControl: DisplayControl,
  modifier: Modifier = Modifier,
) {
  Surface(modifier = modifier, tonalElevation = 2.dp) {
    Row(
      modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        text = shopName.ifBlank { stringResource(R.string.app_name) },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f).testTag("simple_shop_name"),
      )
      TextButton(onClick = { displayControl.onLanguageChange(AppLanguage.ENGLISH) }) {
        Text(stringResource(R.string.simple_en), fontSize = 16.sp)
      }
      Text(stringResource(R.string.simple_language_separator))
      TextButton(onClick = { displayControl.onLanguageChange(AppLanguage.SWAHILI) }) {
        Text(stringResource(R.string.simple_sw), fontSize = 16.sp)
      }
    }
  }
}

@Composable
fun SimpleNavigation(
  currentTab: String,
  onTabSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selected = if (SIMPLE_TABS.any { it.id == currentTab }) currentTab else "MORE"
  Surface(modifier = modifier, tonalElevation = 3.dp) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .height(IntrinsicSize.Min)
          .selectableGroup(),
    ) {
      SIMPLE_TABS.forEach { tab ->
        val active = selected == tab.id
        val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        Column(
          modifier =
            Modifier
              .weight(1f)
              .fillMaxHeight()
              .heightIn(min = 80.dp)
              .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
              .selectable(selected = active, role = Role.Tab, onClick = { onTabSelect(tab.id) })
              .padding(horizontal = 6.dp, vertical = 10.dp)
              .testTag("simple_nav_" + tab.id.lowercase()),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
          Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
          Text(
            stringResource(tab.titleRes),
            modifier = Modifier.fillMaxWidth(),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = color,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
fun SimpleMoreMenu(
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val destinations =
    listOf(
      "EXPENSES" to R.string.simple_expenses,
      "CUSTOMERS" to R.string.lite_debts,
      "REPORTS" to R.string.lite_report,
      "LOANS" to R.string.apen_loans,
      "BACKUP" to R.string.simple_backup,
    )
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    destinations.forEach { (id, label) ->
      OutlinedButton(
        onClick = { onNavigate(id) },
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(
            "simple_more_" + id.lowercase(),
          ),
      ) {
        Text(stringResource(label), modifier = Modifier.fillMaxWidth(), fontSize = 16.sp, textAlign = TextAlign.Center)
      }
    }
  }
}
