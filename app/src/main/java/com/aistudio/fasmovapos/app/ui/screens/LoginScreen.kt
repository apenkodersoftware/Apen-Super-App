package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.ui.components.DisplayButtons
import com.aistudio.fasmovapos.app.ui.components.DisplayControl

/**
 * App-level sign-in gate, shown by MainActivity while [com.aistudio.fasmovapos.app.auth.AuthState] is
 * LoggedOut. Reuses the same username + PIN credential as the staff record - not a separate
 * account system - see FasMovaPOSRepository.signIn / com.aistudio.fasmovapos.app.data.security.PinHasher.
 */
@Composable
fun LoginScreen(
  isLoading: Boolean,
  errorMessage: String?,
  onSignIn: (username: String, pin: String) -> Unit,
  displayControl: DisplayControl? = null,
) {
  // rememberSaveable so a typed username survives rotation; the PIN is deliberately not persisted.
  var username by rememberSaveable { mutableStateOf("") }
  var pin by remember { mutableStateOf("") }
  var recovering by remember { mutableStateOf(false) }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center,
  ) {
    // Language and theme can be chosen before signing in.
    if (displayControl != null) {
      DisplayButtons(
        displayControl = displayControl,
        modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
      )
    }
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          // Scrolls and lifts above the keyboard, so the PIN field and Sign in stay reachable on small phones.
          .safeDrawingPadding()
          .verticalScroll(rememberScrollState())
          .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier =
          Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Storefront,
          contentDescription = stringResource(R.string.malimali_logo),
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(36.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = stringResource(R.string.sign_in_to_continue),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(32.dp))

      OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text(stringResource(R.string.username_email)) },
        singleLine = true,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().testTag("login_username_field"),
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = pin,
        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
        label = { Text(stringResource(R.string.pin_4_digit_pin)) },
        singleLine = true,
        enabled = !isLoading,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth().testTag("login_pin_field"),
      )

      AnimatedVisibility(visible = errorMessage != null) {
        Text(
          text = errorMessage.orEmpty(),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(top = 8.dp),
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = { onSignIn(username, pin) },
        enabled = !isLoading && username.isNotBlank() && pin.length == 4,
        modifier = Modifier.fillMaxWidth().testTag("login_submit_button"),
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          Text(stringResource(R.string.sign_in))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      Text(stringResource(R.string.r2_pin_explain))
      TextButton(enabled = !isLoading, onClick = { recovering = true }) { Text(stringResource(R.string.r2_recover)) }

      Text(
        text = stringResource(R.string.offline_first_retail_pos_for_kenyan),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
  if (recovering) RecoveryDialog { recovering = false }
}
