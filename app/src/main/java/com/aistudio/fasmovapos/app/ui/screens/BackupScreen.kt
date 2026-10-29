package com.aistudio.fasmovapos.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.ui.BackupResult
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MIN_PASSWORD_LENGTH = 6

/** What the user asked for and is still choosing a file or password for. */
private sealed interface PendingAction {
  /** Backup password chosen; waiting for the user to pick where to save. */
  class SavingBackup(
    val password: CharArray,
  ) : PendingAction

  /** File picked; waiting for the user to confirm and give its password. */
  data class Restoring(
    val uri: Uri,
  ) : PendingAction
}

/** Backup, restore and spreadsheet export. Only the Owner can use it. */
@Composable
fun BackupScreen(
  isOwner: Boolean,
  isWorking: Boolean,
  lastBackupAt: Long?,
  result: BackupResult?,
  onCreateBackup: (Uri, CharArray) -> Unit,
  onRestore: (Uri, CharArray) -> Unit,
  onExportSales: (Uri) -> Unit,
  modifier: Modifier = Modifier,
  includeLegacyCsv: Boolean = true,
) {
  var askingBackupPassword by remember { mutableStateOf(false) }
  var pending by remember { mutableStateOf<PendingAction?>(null) }
  val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

  val saveBackupLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
      (pending as? PendingAction.SavingBackup)?.let { action -> if (uri != null) onCreateBackup(uri, action.password) }
      pending = null
    }
  val pickBackupLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) pending = PendingAction.Restoring(uri)
    }
  val saveCsvLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      if (uri != null) onExportSales(uri)
    }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      stringResource(R.string.backup_title),
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
    )
    Text(
      stringResource(R.string.backup_intro),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LastBackupLine(lastBackupAt)
    if (isWorking) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      Text(stringResource(R.string.backup_working), style = MaterialTheme.typography.bodySmall)
    }
    ResultLine(result)

    if (!isOwner) {
      Text(
        stringResource(R.string.backup_owner_only),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("backup_owner_only"),
      )
    }
    val enabled = isOwner && !isWorking
    ActionCard(R.string.create_backup, R.string.create_backup_hint, "backup_create_button", enabled, primary = true) {
      askingBackupPassword = true
    }
    if (includeLegacyCsv) {
      ActionCard(R.string.export_sales, R.string.export_sales_hint, "backup_export_button", enabled) {
        saveCsvLauncher.launch("Apen-sales-$stamp.csv")
      }
    }
    ActionCard(R.string.restore_backup, R.string.restore_backup_hint, "backup_restore_button", enabled) {
      pickBackupLauncher.launch(arrayOf("*/*"))
    }
  }

  if (askingBackupPassword) {
    BackupPasswordDialog(
      onDismiss = { askingBackupPassword = false },
      onConfirm = { password ->
        askingBackupPassword = false
        pending = PendingAction.SavingBackup(password)
        saveBackupLauncher.launch("Apen-backup-$stamp.flbk")
      },
    )
  }
  (pending as? PendingAction.Restoring)?.let { action ->
    RestoreDialog(
      onDismiss = { pending = null },
      onConfirm = { password ->
        pending = null
        onRestore(action.uri, password)
      },
    )
  }
}

@Composable
private fun LastBackupLine(lastBackupAt: Long?) {
  val text =
    if (lastBackupAt == null) {
      stringResource(R.string.never_backed_up)
    } else {
      stringResource(R.string.last_backup, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastBackupAt)))
    }
  Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = if (lastBackupAt == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    modifier = Modifier.testTag("backup_last_backup"),
  )
}

@Composable
private fun ResultLine(result: BackupResult?) {
  val message =
    when (result) {
      BackupResult.BACKUP_SAVED -> R.string.backup_saved
      BackupResult.SALES_EXPORTED -> R.string.sales_exported
      BackupResult.RESTORED -> R.string.restore_done
      BackupResult.WRONG_PASSWORD_OR_INVALID -> R.string.restore_wrong_password
      BackupResult.FAILED -> R.string.backup_failed
      null -> return
    }
  val isError = result == BackupResult.WRONG_PASSWORD_OR_INVALID || result == BackupResult.FAILED
  Text(
    stringResource(message),
    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    modifier = Modifier.testTag("backup_result"),
  )
}

@Composable
private fun ActionCard(
  title: Int,
  hint: Int,
  tag: String,
  enabled: Boolean,
  primary: Boolean = false,
  onClick: () -> Unit,
) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium)
      val buttonModifier = Modifier.fillMaxWidth().height(48.dp).testTag(tag)
      if (primary) {
        Button(onClick = onClick, enabled = enabled, modifier = buttonModifier) { Text(stringResource(title)) }
      } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = buttonModifier) { Text(stringResource(title)) }
      }
    }
  }
}

@Composable
private fun BackupPasswordDialog(
  onDismiss: () -> Unit,
  onConfirm: (CharArray) -> Unit,
) {
  var password by remember { mutableStateOf("") }
  var repeat by remember { mutableStateOf("") }
  val tooShort = password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH
  val valid = password.length >= MIN_PASSWORD_LENGTH && password == repeat

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.backup_password_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.backup_password_hint), style = MaterialTheme.typography.bodySmall)
        PasswordField(password, {
          password = it
        }, R.string.backup_password, "backup_password_field", if (tooShort) R.string.backup_password_short else null)
        PasswordField(repeat, { repeat = it }, R.string.backup_password_repeat, "backup_password_repeat_field", null)
      }
    },
    confirmButton = {
      TextButton(onClick = { onConfirm(password.toCharArray()) }, enabled = valid, modifier = Modifier.testTag("backup_password_confirm")) {
        Text(stringResource(R.string.create_backup))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}

@Composable
private fun RestoreDialog(
  onDismiss: () -> Unit,
  onConfirm: (CharArray) -> Unit,
) {
  var password by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.restore_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.restore_warning), color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(4.dp))
        PasswordField(password, { password = it }, R.string.backup_password, "restore_password_field", null)
      }
    },
    confirmButton = {
      TextButton(onClick = {
        onConfirm(password.toCharArray())
      }, enabled = password.isNotEmpty(), modifier = Modifier.testTag("restore_confirm")) {
        Text(stringResource(R.string.restore_action))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}

@Composable
private fun PasswordField(
  value: String,
  onValueChange: (String) -> Unit,
  label: Int,
  tag: String,
  error: Int?,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(stringResource(label)) },
    singleLine = true,
    isError = error != null,
    supportingText = error?.let { message -> { Text(stringResource(message)) } },
    visualTransformation = PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    modifier = Modifier.fillMaxWidth().testTag(tag),
  )
}
