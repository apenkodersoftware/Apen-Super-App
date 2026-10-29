package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.StaffUserEntity
import com.aistudio.fasmovapos.app.ui.components.roleLabel
import com.aistudio.fasmovapos.app.ui.theme.FasMovaStatus

@Composable
fun StaffScreen(
  staffUsers: List<StaffUserEntity>,
  currentUser: StaffUserEntity,
  onAddStaff: (String, String, String, String) -> Unit,
) {
  var showAddStaffDialog by remember { mutableStateOf(false) }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp),
  ) {
    // Top Header
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
          Text(
            stringResource(R.string.staff_role_access_control_rbac),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
          Text(
            stringResource(R.string.active_login, currentUser.fullName, roleLabel(currentUser.role)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        if (currentUser.role == "OWNER") {
          Button(
            onClick = { showAddStaffDialog = true },
            modifier = Modifier.testTag("add_staff_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.add_staff))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Staff Directory
    Text(stringResource(R.string.store_staff_accounts), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(staffUsers) { user ->
        StaffUserRow(user = user)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // RBAC Permissions Matrix
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          stringResource(R.string.role_based_access_permission_matrix),
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(8.dp))

        PermissionRow(stringResource(R.string.process_pos_sales), owner = true, manager = true, cashier = true)
        PermissionRow(stringResource(R.string.print_digital_receipts), owner = true, manager = true, cashier = true)
        PermissionRow(stringResource(R.string.restock_edit_inventory), owner = true, manager = true, cashier = false)
        PermissionRow(stringResource(R.string.log_business_expenses), owner = true, manager = true, cashier = false)
        PermissionRow(stringResource(R.string.create_purchase_orders), owner = true, manager = true, cashier = false)
        PermissionRow(stringResource(R.string.view_profit_loss_analytics), owner = true, manager = false, cashier = false)
        PermissionRow(stringResource(R.string.add_manage_staff_pins), owner = true, manager = false, cashier = false)
      }
    }
  }

  if (showAddStaffDialog) {
    AddStaffDialog(
      onDismiss = { showAddStaffDialog = false },
      onConfirm = { username, fullName, role, pin ->
        onAddStaff(username, fullName, role, pin)
        showAddStaffDialog = false
      },
    )
  }
}

@Composable
private fun StaffUserRow(user: StaffUserEntity) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(user.fullName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Text(
          stringResource(R.string.username_pin, user.username),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color =
          when (user.role) {
            "OWNER" -> FasMovaStatus.dangerContainer
            "MANAGER" -> FasMovaStatus.warningContainer
            else -> FasMovaStatus.successContainer
          },
      ) {
        Text(
          text = roleLabel(user.role),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color =
            when (user.role) {
              "OWNER" -> FasMovaStatus.onDangerContainer
              "MANAGER" -> FasMovaStatus.onWarningContainer
              else -> FasMovaStatus.onSuccessContainer
            },
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }
  }
}

@Composable
private fun PermissionRow(
  permission: String,
  owner: Boolean,
  manager: Boolean,
  cashier: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(permission, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.5f))
    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
      Icon(
        if (owner) Icons.Default.Check else Icons.Default.Close,
        contentDescription = stringResource(R.string.owner),
        tint = if (owner) FasMovaStatus.success else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(16.dp),
      )
      Icon(
        if (manager) Icons.Default.Check else Icons.Default.Close,
        contentDescription = stringResource(R.string.manager),
        tint = if (manager) FasMovaStatus.success else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(16.dp),
      )
      Icon(
        if (cashier) Icons.Default.Check else Icons.Default.Close,
        contentDescription = stringResource(R.string.cashier_2),
        tint = if (cashier) FasMovaStatus.success else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

@Composable
private fun AddStaffDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String) -> Unit,
) {
  var username by remember { mutableStateOf("") }
  var fullName by remember { mutableStateOf("") }
  var role by remember { mutableStateOf("CASHIER") }
  var pin by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(stringResource(R.string.register_staff_user), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = fullName, onValueChange = {
          fullName = it
        }, label = {
          Text(
            stringResource(R.string.full_name),
          )
        }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("staff_fullname"))
        OutlinedTextField(value = username, onValueChange = {
          username = it
        }, label = {
          Text(stringResource(R.string.username_email))
        }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("staff_username"))
        OutlinedTextField(value = role, onValueChange = {
          role = it
        }, label = {
          Text(
            stringResource(R.string.role_owner_manager_cashier),
          )
        }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("staff_role"))
        OutlinedTextField(
          value = pin,
          onValueChange = { if (it.length <= 4) pin = it },
          label = { Text(stringResource(R.string.pin_4_digit_security_pin)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("staff_pin"),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (username.isNotBlank() && fullName.isNotBlank() && pin.length == 4) {
                onConfirm(username, fullName, role, pin)
              }
            },
            modifier = Modifier.testTag("confirm_staff_button"),
          ) {
            Text(stringResource(R.string.create_account))
          }
        }
      }
    }
  }
}
