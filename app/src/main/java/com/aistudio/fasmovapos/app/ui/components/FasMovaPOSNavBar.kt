package com.aistudio.fasmovapos.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.R

data class NavTabItem(
  val id: String,
  @StringRes val titleRes: Int,
  val icon: ImageVector,
)

val NAV_TABS =
  listOf(
    NavTabItem("POS", R.string.nav_pos, Icons.Default.PointOfSale),
    NavTabItem("INVENTORY", R.string.stock, Icons.Default.Inventory2),
    NavTabItem("SALES", R.string.sales_2, Icons.Default.ReceiptLong),
    NavTabItem("EXPENSES", R.string.expenses, Icons.Default.MoneyOff),
    NavTabItem("SUPPLIERS", R.string.suppliers, Icons.Default.LocalShipping),
    NavTabItem("CUSTOMERS", R.string.customers, Icons.Default.Group),
    NavTabItem("ANALYTICS", R.string.reports, Icons.Default.Analytics),
    NavTabItem("STAFF", R.string.staff, Icons.Default.Security),
    NavTabItem("BACKUP", R.string.backup_tab, Icons.Default.SaveAlt),
  )

private val MIN_TAB_WIDTH = 76.dp
private const val COMPACT_WIDTH_DP = 600

/**
 * Phones are too narrow for nine bottom tabs, so on compact widths navigation moves into a
 * dropdown on the top bar's brand header (see [TabMenuState]) and the bottom bar is dropped.
 */
@Composable
fun isCompactWidth(): Boolean = LocalConfiguration.current.screenWidthDp < COMPACT_WIDTH_DP

/** Current section and selection callback for the top bar's navigation dropdown. */
class TabMenuState(
  val currentTab: String,
  val onTabSelected: (String) -> Unit,
)

@Composable
fun FasMovaPOSNavBar(
  currentTab: String,
  onTabSelected: (String) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    BoxWithConstraints(modifier = Modifier.navigationBarsPadding()) {
      // Share the width evenly when every tab fits; otherwise keep a readable width and scroll.
      val itemWidth = maxOf(MIN_TAB_WIDTH, maxWidth / NAV_TABS.size)
      Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        NAV_TABS.forEach { tab ->
          NavTab(
            tab = tab,
            isSelected = currentTab == tab.id,
            onClick = { onTabSelected(tab.id) },
            modifier = Modifier.width(itemWidth),
          )
        }
      }
    }
  }
}

@Composable
private fun NavTab(
  tab: NavTabItem,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val contentColor =
    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
  Column(
    modifier =
      modifier
        .selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
        .padding(vertical = 8.dp)
        .testTag("nav_tab_${tab.id.lowercase()}"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier =
        Modifier
          .size(width = 56.dp, height = 28.dp)
          .clip(CircleShape)
          .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
      contentAlignment = Alignment.Center,
    ) {
      Icon(imageVector = tab.icon, contentDescription = null, tint = contentColor)
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = stringResource(tab.titleRes),
      fontSize = 11.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      color = contentColor,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
    )
  }
}
