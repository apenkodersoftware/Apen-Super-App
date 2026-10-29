package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.security.PinPolicy
import com.aistudio.fasmovapos.app.data.security.PinProblem
import com.aistudio.fasmovapos.app.ui.message

/**
 * Shown right after sign-in while the user still holds a temporary PIN (new staff, sample staff). Nothing else in
 * the app is reachable until they choose their own; they can only sign out instead.
 */
@Composable
fun ChangePinScreen(
  isSaving: Boolean,
  problem: PinProblem?,
  onSave: (String) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // PINs are deliberately not saved across rotation or process death.
  var pin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  // The refusal from the last Save applies to the PIN that was tried; once the user edits, it no longer does.
  var attempts by remember { mutableIntStateOf(0) }
  var editedSinceAttempt by remember(attempts) { mutableStateOf(false) }

  val localProblem = if (pin.length == PinPolicy.LENGTH) PinPolicy.formatProblem(pin) else null
  val shownProblem = localProblem ?: problem.takeIf { attempts > 0 && !editedSinceAttempt }
  val pinsDiffer = confirmPin.length == PinPolicy.LENGTH && confirmPin != pin
  val canSave = !isSaving && PinPolicy.formatProblem(pin) == null && pin == confirmPin

  Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      Spacer(modifier = Modifier.height(56.dp))
      Text(
        text = stringResource(R.string.change_pin_title),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = stringResource(R.string.change_pin_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(24.dp))

      OutlinedTextField(
        value = pin,
        onValueChange = {
          if (it.length <= PinPolicy.LENGTH && it.all { c -> c.isDigit() }) {
            pin = it
            editedSinceAttempt = true
          }
        },
        label = { Text(stringResource(R.string.new_pin)) },
        singleLine = true,
        isError = shownProblem != null,
        supportingText = shownProblem?.let { shown -> { Text(shown.message()) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("change_pin_field"),
      )
      OutlinedTextField(
        value = confirmPin,
        onValueChange = {
          if (it.length <= PinPolicy.LENGTH && it.all { c -> c.isDigit() }) {
            confirmPin = it
            editedSinceAttempt = true
          }
        },
        label = { Text(stringResource(R.string.confirm_pin)) },
        singleLine = true,
        isError = pinsDiffer,
        supportingText = if (pinsDiffer) ({ Text(stringResource(R.string.pins_do_not_match)) }) else null,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("change_pin_confirm_field"),
      )

      Button(
        onClick = {
          attempts++
          onSave(pin)
        },
        enabled = canSave,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("change_pin_save_button"),
      ) {
        if (isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          Text(stringResource(R.string.save_pin))
        }
      }
      if (isSaving) {
        Text(
          stringResource(R.string.securing_data),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(stringResource(R.string.sign_out))
      }
    }
  }
}
