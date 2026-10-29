package com.aistudio.fasmovapos.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity
import com.aistudio.fasmovapos.app.data.db.entities.StaffUserEntity
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.language.labelRes
import com.aistudio.fasmovapos.app.ui.theme.ThemeMode

private val COMPACT_WIDTH = 600.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FasMovaPOSTopBar(
  currentBranch: BranchEntity,
  branches: List<BranchEntity>,
  currentUser: StaffUserEntity,
  onBranchSelected: (BranchEntity) -> Unit,
  onSaveBusinessDetails: (String, String, String) -> Unit,
  onPinAuthenticate: (String, (Boolean) -> Unit) -> Unit,
  onSignOut: () -> Unit,
  displayControl: DisplayControl,
  tabMenu: TabMenuState? = null,
) {
  var showPinDialog by remember { mutableStateOf(false) }
  var showBusinessDialog by remember { mutableStateOf(false) }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 4.dp,
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
      val compact = maxWidth < COMPACT_WIDTH

      // Business (branch) and signed-in user.
      val statusChips: @Composable () -> Unit = {
        BranchSelectorChip(
          currentBranch = currentBranch,
          branches = branches,
          onBranchSelected = onBranchSelected,
          onEditBusiness = if (currentUser.role == "OWNER") ({ showBusinessDialog = true }) else null,
        )
        StaffUserChip(currentUser = currentUser, onClick = { showPinDialog = true })
      }

      if (compact) {
        // Phones: brand and sign-out on the first line, status chips wrapping beneath.
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            BrandHeader(tabMenu = tabMenu)
            DisplayButtons(displayControl = displayControl)
            SignOutButton(onSignOut = onSignOut)
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            statusChips()
          }
        }
      } else {
        FlowRow(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          BrandHeader(tabMenu = tabMenu, modifier = Modifier.align(Alignment.CenterVertically))
          FlowRow(
            modifier = Modifier.align(Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            statusChips()
            DisplayButtons(displayControl = displayControl)
            SignOutButton(onSignOut = onSignOut)
          }
        }
      }
    }
  }

  if (showPinDialog) {
    StaffSwitchDialog(
      onDismiss = { showPinDialog = false },
      onPinAuthenticate = onPinAuthenticate,
    )
  }
  if (showBusinessDialog) {
    BusinessDetailsDialog(
      branch = currentBranch,
      onDismiss = { showBusinessDialog = false },
      onSave = { name, phone, taxPin ->
        showBusinessDialog = false
        onSaveBusinessDetails(name, phone, taxPin)
      },
    )
  }
}

@Composable
private fun SignOutButton(onSignOut: () -> Unit) {
  IconButton(
    onClick = onSignOut,
    modifier = Modifier.size(36.dp).testTag("sign_out_button"),
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Filled.Logout,
      contentDescription = stringResource(R.string.sign_out),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
  }
}

/**
 * Logo and name. With a [tabMenu] it doubles as the navigation menu button: tapping opens a
 * dropdown of every section, and the subtitle shows which section is open.
 */
@Composable
private fun BrandHeader(
  tabMenu: TabMenuState?,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val currentTitle = NAV_TABS.firstOrNull { it.id == tabMenu?.currentTab }?.titleRes?.let { stringResource(it) }

  Box(modifier = modifier) {
    Row(
      modifier =
        Modifier
          .clip(RoundedCornerShape(8.dp))
          .then(
            if (tabMenu != null) {
              Modifier.clickable { expanded = true }.testTag("brand_menu_button")
            } else {
              Modifier
            },
          ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Storefront,
          contentDescription = stringResource(R.string.malimali_logo),
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "MaliMali",
          style =
            MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
            ),
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = if (tabMenu != null) currentTitle ?: stringResource(R.string.menu) else stringResource(R.string.retail_saas_pos_platform),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (tabMenu != null) {
        Icon(
          imageVector = Icons.Default.ArrowDropDown,
          contentDescription = stringResource(R.string.open_navigation_menu),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 2.dp),
        )
      }
    }

    if (tabMenu != null) {
      NavigationDropdown(
        expanded = expanded,
        tabMenu = tabMenu,
        onDismiss = { expanded = false },
      )
    }
  }
}

@Composable
private fun NavigationDropdown(
  expanded: Boolean,
  tabMenu: TabMenuState,
  onDismiss: () -> Unit,
) {
  DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
    NAV_TABS.forEach { tab ->
      val isSelected = tab.id == tabMenu.currentTab
      DropdownMenuItem(
        text = {
          Text(
            text = stringResource(tab.titleRes),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
          )
        },
        leadingIcon = {
          Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint =
              if (isSelected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
          )
        },
        onClick = {
          tabMenu.onTabSelected(tab.id)
          onDismiss()
        },
        modifier = Modifier.testTag("nav_menu_${tab.id.lowercase()}"),
      )
    }
  }
}

@Composable
private fun BranchSelectorChip(
  currentBranch: BranchEntity,
  branches: List<BranchEntity>,
  onBranchSelected: (BranchEntity) -> Unit,
  onEditBusiness: (() -> Unit)? = null,
) {
  var showBranchDropdown by remember { mutableStateOf(false) }

  Box {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier =
        Modifier
          .clip(RoundedCornerShape(20.dp))
          .clickable { showBranchDropdown = true }
          .testTag("branch_selector_chip"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Default.Storefront,
          contentDescription = stringResource(R.string.branch),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = currentBranch.name.take(18) + if (currentBranch.name.length > 18) "..." else "",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }

    DropdownMenu(
      expanded = showBranchDropdown,
      onDismissRequest = { showBranchDropdown = false },
    ) {
      Text(
        text = stringResource(R.string.switch_store_branch),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      )
      branches.forEach { b ->
        DropdownMenuItem(
          text = {
            Text(
              text = b.name + if (b.isHeadquarters) " (HQ)" else "",
              fontWeight = if (b.id == currentBranch.id) FontWeight.Bold else FontWeight.Normal,
            )
          },
          onClick = {
            onBranchSelected(b)
            showBranchDropdown = false
          },
        )
      }
      if (onEditBusiness != null) {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.business_details)) },
          onClick = {
            showBranchDropdown = false
            onEditBusiness()
          },
          modifier = Modifier.testTag("edit_business_details"),
        )
      }
    }
  }
}

/** Staff User Chip; tapping it opens the PIN switch dialog. */
@Composable
private fun StaffUserChip(
  currentUser: StaffUserEntity,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier =
      Modifier
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag("staff_user_chip"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(
              when (currentUser.role) {
                "OWNER" -> Color(0xFFEF4444)
                "MANAGER" -> Color(0xFFF59E0B)
                else -> MaterialTheme.colorScheme.primary
              },
            ),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "${currentUser.fullName.split(" ").first()} (${roleLabel(currentUser.role)})",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
      )
      Spacer(modifier = Modifier.width(4.dp))
      Icon(
        imageVector = Icons.Default.SwapHoriz,
        contentDescription = stringResource(R.string.switch_cashier),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

/** Quick Staff Switcher PIN Dialog. */
@Composable
private fun StaffSwitchDialog(
  onDismiss: () -> Unit,
  onPinAuthenticate: (String, (Boolean) -> Unit) -> Unit,
) {
  var pinInput by remember { mutableStateOf("") }
  var showPinError by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = stringResource(R.string.lock),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
          text = stringResource(R.string.quick_staff_cashier_switch),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = stringResource(R.string.enter_your_4_digit_staff_pin),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        OutlinedTextField(
          value = pinInput,
          onValueChange = {
            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
              pinInput = it
            }
          },
          label = { Text(stringResource(R.string.pin_4_digit_pin)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("pin_input_field"),
        )

        AnimatedVisibility(visible = showPinError) {
          Text(
            text = stringResource(R.string.invalid_pin_for_this_branch),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
          )
        }

        Spacer(modifier = Modifier.size(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.cancel))
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              onPinAuthenticate(pinInput) { success ->
                if (success) {
                  onDismiss()
                } else {
                  showPinError = true
                }
              }
            },
            enabled = pinInput.length == 4,
            modifier = Modifier.testTag("submit_pin_button"),
          ) {
            Text(stringResource(R.string.authenticate))
          }
        }
      }
    }
  }
}

/** The current theme and language choices, and how to change them. */
data class DisplayControl(
  val mode: ThemeMode,
  val onModeChange: (ThemeMode) -> Unit,
  val language: AppLanguage,
  val onLanguageChange: (AppLanguage) -> Unit,
)

@Composable
fun DisplayButtons(
  displayControl: DisplayControl,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier) {
    LanguageButton(displayControl = displayControl)
    ThemeModeButton(displayControl = displayControl)
  }
}

@Composable
private fun LanguageButton(displayControl: DisplayControl) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { expanded = true }) {
      Icon(
        imageVector = Icons.Default.Language,
        contentDescription = stringResource(R.string.language_label, stringResource(displayControl.language.labelRes())),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      AppLanguage.entries.forEach { language ->
        DropdownMenuItem(
          text = {
            Text(
              text = stringResource(language.labelRes()),
              fontWeight = if (language == displayControl.language) FontWeight.Bold else FontWeight.Normal,
            )
          },
          onClick = {
            expanded = false
            displayControl.onLanguageChange(language)
          },
        )
      }
    }
  }
}

@Composable
private fun ThemeModeButton(displayControl: DisplayControl) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { expanded = true }) {
      Icon(
        imageVector =
          when (displayControl.mode) {
            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
            ThemeMode.LIGHT -> Icons.Default.LightMode
            ThemeMode.DARK -> Icons.Default.DarkMode
          },
        contentDescription = stringResource(R.string.theme, stringResource(displayControl.mode.labelRes)),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      ThemeMode.entries.forEach { mode ->
        DropdownMenuItem(
          text = {
            Text(
              text = stringResource(mode.labelRes),
              fontWeight = if (mode == displayControl.mode) FontWeight.Bold else FontWeight.Normal,
            )
          },
          onClick = {
            expanded = false
            displayControl.onModeChange(mode)
          },
        )
      }
    }
  }
}
