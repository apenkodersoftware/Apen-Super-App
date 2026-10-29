package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.security.PinPolicy
import com.aistudio.fasmovapos.app.ui.components.DisplayButtons
import com.aistudio.fasmovapos.app.ui.components.DisplayControl
import com.aistudio.fasmovapos.app.ui.message

/**
 * First-launch setup: names the business, creates the Owner with a PIN they choose, and optionally loads
 * training data. Replaces shipping every install with the same demo accounts.
 */
@Composable
fun SetupScreen(
  isLoading: Boolean,
  onComplete: (
    businessName: String,
    ownerName: String,
    username: String,
    pin: String,
    loadSampleData: Boolean,
    businessPhone: String,
    taxPin: String,
  ) -> Unit,
  modifier: Modifier = Modifier,
  displayControl: DisplayControl? = null,
  scopeText: String? = null,
) {
  var businessName by rememberSaveable { mutableStateOf("") }
  var businessPhone by rememberSaveable { mutableStateOf("") }
  var taxPin by rememberSaveable { mutableStateOf("") }
  var ownerName by rememberSaveable { mutableStateOf("") }
  var recovering by remember { mutableStateOf(false) }
  var username by rememberSaveable { mutableStateOf("owner") }
  // PINs are deliberately not saved across rotation or process death.
  var pin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }

  val pinProblem = if (pin.length == PinPolicy.LENGTH) PinPolicy.formatProblem(pin) else null
  val pinsDiffer = confirmPin.length == PinPolicy.LENGTH && confirmPin != pin
  val canStart =
    !isLoading &&
      businessName.isNotBlank() &&
      ownerName.isNotBlank() &&
      username.isNotBlank() &&
      PinPolicy.formatProblem(pin) == null &&
      pin == confirmPin

  Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    if (displayControl != null) {
      DisplayButtons(
        displayControl = displayControl,
        modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
      )
    }
    // Scrolls and lifts above the keyboard, so the last field and the Start button stay reachable.
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp),
    ) {
      Spacer(modifier = Modifier.height(56.dp))
      Text(
        text = stringResource(R.string.setup_title),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = stringResource(R.string.setup_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(scopeText ?: stringResource(R.string.r2_scope))
      TextButton(enabled = !isLoading, onClick = {
        recovering = true
      }, modifier = Modifier.testTag("setup_recover_backup")) { Text(stringResource(R.string.r2_recover)) }
      Spacer(modifier = Modifier.height(24.dp))

      SetupField(businessName, { businessName = it }, R.string.business_name, "setup_business_field", words = true)
      SetupField(businessPhone, { businessPhone = it }, R.string.business_phone_optional, "setup_phone_field", phone = true)
      SetupField(taxPin, { taxPin = it.uppercase().trim() }, R.string.kra_pin_optional, "setup_kra_field")
      SetupField(ownerName, { ownerName = it }, R.string.your_full_name, "setup_owner_name_field", words = true)
      Text(stringResource(R.string.r2_pin_explain))
      SetupField(username, { username = it.trim() }, R.string.setup_username, "setup_username_field")
      SetupField(
        value = pin,
        onValueChange = { if (it.length <= PinPolicy.LENGTH && it.all { c -> c.isDigit() }) pin = it },
        label = R.string.choose_pin,
        tag = "setup_pin_field",
        isPin = true,
        error = pinProblem?.message(),
      )
      SetupField(
        value = confirmPin,
        onValueChange = { if (it.length <= PinPolicy.LENGTH && it.all { c -> c.isDigit() }) confirmPin = it },
        label = R.string.confirm_pin,
        tag = "setup_confirm_pin_field",
        isPin = true,
        error = if (pinsDiffer) stringResource(R.string.pins_do_not_match) else null,
        last = true,
      )

      Text(stringResource(R.string.r2_empty_setup))

      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = { onComplete(businessName.trim(), ownerName.trim(), username, pin, false, businessPhone.trim(), taxPin) },
        enabled = canStart,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("setup_start_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          Text(stringResource(R.string.start_using_app))
        }
      }
      if (isLoading) {
        Text(
          stringResource(R.string.securing_data),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
  if (recovering) RecoveryDialog { recovering = false }
}

@Composable
private fun SetupField(
  value: String,
  onValueChange: (String) -> Unit,
  label: Int,
  tag: String,
  isPin: Boolean = false,
  words: Boolean = false,
  phone: Boolean = false,
  error: String? = null,
  last: Boolean = false,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(stringResource(label)) },
    singleLine = true,
    isError = error != null,
    supportingText = error?.let { message -> { Text(message) } },
    visualTransformation = if (isPin) PasswordVisualTransformation() else VisualTransformation.None,
    keyboardOptions =
      KeyboardOptions(
        keyboardType =
          when {
            isPin -> KeyboardType.NumberPassword
            phone -> KeyboardType.Phone
            else -> KeyboardType.Text
          },
        capitalization = if (words) KeyboardCapitalization.Words else KeyboardCapitalization.None,
        imeAction = if (last) ImeAction.Done else ImeAction.Next,
      ),
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag(tag),
  )
}
