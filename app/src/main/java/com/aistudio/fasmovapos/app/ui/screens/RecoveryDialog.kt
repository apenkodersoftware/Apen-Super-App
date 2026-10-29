package com.aistudio.fasmovapos.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.backup.BackupManager
import com.aistudio.fasmovapos.app.data.backup.OwnerRecovery
import com.aistudio.fasmovapos.app.data.security.PinPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.system.exitProcess

@Composable
fun RecoveryDialog(onDismiss: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var uri by remember { mutableStateOf<Uri?>(null) }
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var pin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var agreed by remember { mutableStateOf(false) }
  var working by remember { mutableStateOf(false) }
  var problem by remember { mutableStateOf(false) }
  var done by remember { mutableStateOf(false) }
  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
      uri = it
      agreed = false
    }
  AlertDialog(
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.padding(16.dp),
    onDismissRequest = { if (!working && !done) onDismiss() },
    title = { Text(stringResource(R.string.r2_recover)) },
    text = {
      Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(if (done) R.string.r2_recovery_done else R.string.r2_recovery_notice))
        if (working) {
          androidx.compose.material3.LinearProgressIndicator()
          Text(stringResource(R.string.r3_recovering))
        }
        if (!done) {
          OutlinedButton(enabled = !working, onClick = { picker.launch(arrayOf("*/*")) }) {
            Text(stringResource(if (uri == null) R.string.r2_pick_backup else R.string.r2_backup_selected))
          }
          OutlinedTextField(username, {
            username = it
          }, label = { Text(stringResource(R.string.username_email)) }, enabled = !working, singleLine = true)
          OutlinedTextField(
            password,
            { password = it },
            label = { Text(stringResource(R.string.r2_backup_password)) },
            enabled = !working,
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
          )
          OutlinedTextField(
            pin,
            {
              if (it.length <= 4 &&
                it.all(Char::isDigit)
              ) {
                pin = it
              }
            },
            label = { Text(stringResource(R.string.choose_pin)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            enabled = !working,
          )
          OutlinedTextField(
            confirmPin,
            {
              if (it.length <= 4 &&
                it.all(Char::isDigit)
              ) {
                confirmPin = it
              }
            },
            label = { Text(stringResource(R.string.confirm_pin)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            enabled = !working,
          )
          Row {
            Checkbox(checked = agreed, onCheckedChange = { if (!working) agreed = it }, enabled = !working)
            Text(stringResource(R.string.r2_replace_confirm))
          }
          if (problem) Text(stringResource(R.string.r2_recovery_problem))
        }
      }
    },
    confirmButton = {
      if (done) {
        Button(onClick = {
          val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
          context.startActivity(launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
          exitProcess(0)
        }) { Text(stringResource(R.string.r2_restart)) }
      } else {
        Button(
          enabled = recoveryReady(working, agreed, uri, username, password, pin, confirmPin),
          onClick = {
            scope.launch {
              working = true
              problem =
                runCatching {
                  withContext(Dispatchers.IO) {
                    val bytes = readRecoveryFile(context.contentResolver.openInputStream(requireNotNull(uri)))
                    val secret = password.toCharArray()
                    try {
                      BackupManager(context).restore(bytes, secret, OwnerRecovery(username, pin))
                    } finally {
                      secret.fill('\u0000')
                    }
                  }
                }.isFailure
              done = !problem
              password = ""
              pin = ""
              confirmPin = ""
              working = false
            }
          },
        ) { Text(stringResource(R.string.r2_restore_reset)) }
      }
    },
    dismissButton = { if (!done) TextButton(enabled = !working, onClick = onDismiss) { Text(stringResource(R.string.simple_cancel)) } },
  )
}

private fun readRecoveryFile(input: java.io.InputStream?): ByteArray =
  requireNotNull(input).use { stream ->
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var count = stream.read(buffer)
    while (count != -1) {
      require(output.size() + count <= 128 * 1024 * 1024) { "Backup is too large for this recovery flow" }
      output.write(buffer, 0, count)
      count = stream.read(buffer)
    }
    output.toByteArray()
  }

private fun recoveryReady(
  working: Boolean,
  agreed: Boolean,
  uri: Uri?,
  username: String,
  password: String,
  pin: String,
  confirmation: String,
): Boolean =
  !working &&
    agreed &&
    uri != null &&
    username.isNotBlank() &&
    password.isNotEmpty() &&
    PinPolicy.formatProblem(pin) == null &&
    pin == confirmation
