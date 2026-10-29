package com.aistudio.fasmovapos.app.ui.simple

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity
import com.aistudio.fasmovapos.app.onboarding.TermsScreen
import com.aistudio.fasmovapos.app.ui.components.DisplayControl
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.theme.ThemeMode

@Composable
fun MoreScreen(
  branch: BranchEntity,
  displayControl: DisplayControl,
  canEditShop: Boolean,
  onNavigate: (String) -> Unit,
  onSaveShop: (String, String, String) -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var panel by rememberSaveable { mutableStateOf<String?>(null) }
  LazyColumn(
    modifier = modifier.fillMaxSize().padding(16.dp).testTag("simple_more_list"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item { Text(stringResource(R.string.simple_more), style = MaterialTheme.typography.headlineSmall) }
    item { SimpleMoreMenu(onNavigate) }
    item { MoreOption("language", R.string.simple_language) { panel = "LANGUAGE" } }
    if (canEditShop) item { MoreOption("shop", R.string.simple_shop_settings) { panel = "SHOP" } }
    item { MoreOption("help", R.string.simple_help) { panel = "HELP" } }
    item { MoreOption("terms", R.string.terms_review) { panel = "TERMS" } }
    item { MoreOption("logout", R.string.simple_logout, onLogout) }
  }
  when (panel) {
    "TERMS" ->
      Dialog(onDismissRequest = { panel = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        TermsScreen(onAccept = { panel = null }, reviewOnly = true, onLanguageChange = displayControl.onLanguageChange)
      }
    "LANGUAGE" ->
      AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 24.dp),
        onDismissRequest = { panel = null },
        title = { Text(stringResource(R.string.simple_language)) },
        text = {
          Column {
            TextButton(onClick = {
              panel = null
              displayControl.onLanguageChange(AppLanguage.ENGLISH)
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
              Text(stringResource(R.string.language_english), fontSize = 16.sp)
            }
            TextButton(onClick = {
              panel = null
              displayControl.onLanguageChange(AppLanguage.SWAHILI)
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
              Text(stringResource(R.string.language_swahili), fontSize = 16.sp)
            }
          }
        },
        confirmButton = { TextButton(onClick = { panel = null }) { Text(stringResource(R.string.simple_close), fontSize = 16.sp) } },
      )
    "SHOP" ->
      SimpleShopSettings(branch, displayControl, onDismiss = { panel = null }, onSave = {
        name,
        phone,
        tax,
        ->
        onSaveShop(name, phone, tax)
        panel =
          null
      })
    "HELP" ->
      AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 24.dp),
        onDismissRequest = { panel = null },
        title = { Text(stringResource(R.string.simple_help)) },
        text = {
          Text(
            stringResource(R.string.simple_help_body),
            fontSize = 16.sp,
            modifier = Modifier.verticalScroll(rememberScrollState()),
          )
        },
        confirmButton = { TextButton(onClick = { panel = null }) { Text(stringResource(R.string.simple_close), fontSize = 16.sp) } },
      )
  }
}

@Composable
private fun MoreOption(
  id: String,
  @StringRes label: Int,
  onClick: () -> Unit,
) {
  OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("simple_more_" + id)) {
    Text(stringResource(label), modifier = Modifier.fillMaxWidth(), fontSize = 16.sp, textAlign = TextAlign.Center)
  }
}

@Composable
private fun SimpleShopSettings(
  branch: BranchEntity,
  displayControl: DisplayControl,
  onDismiss: () -> Unit,
  onSave: (String, String, String) -> Unit,
) {
  var name by rememberSaveable { mutableStateOf(branch.name) }
  var phone by rememberSaveable { mutableStateOf(branch.phone) }
  var taxPin by rememberSaveable { mutableStateOf(branch.taxPin) }
  var details by rememberSaveable { mutableStateOf(false) }
  AlertDialog(
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.padding(horizontal = 24.dp),
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.simple_shop_settings)) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = name, onValueChange = {
          name = it
        }, label = {
          Text(stringResource(R.string.simple_shop_name))
        }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("simple_shop_name_field"))
        TextButton(onClick = { details = !details }) { Text(stringResource(R.string.simple_more_details), fontSize = 16.sp) }
        if (details) {
          OutlinedTextField(value = phone, onValueChange = {
            phone = it
          }, label = {
            Text(stringResource(R.string.simple_phone_optional))
          }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = taxPin, onValueChange = {
            taxPin = it.uppercase()
          }, label = { Text(stringResource(R.string.kra_pin_optional)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
          Text(stringResource(R.string.simple_display), fontSize = 16.sp)
          listOf(
            ThemeMode.SYSTEM to R.string.theme_system,
            ThemeMode.LIGHT to R.string.theme_light,
            ThemeMode.DARK to R.string.theme_dark,
          ).forEach { (mode, title) ->
            TextButton(onClick = {
              displayControl.onModeChange(mode)
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(title), fontSize = 16.sp) }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = {
        onSave(name.trim(), phone, taxPin)
      }, enabled = name.isNotBlank(), modifier = Modifier.testTag("simple_save_shop")) {
        Text(stringResource(R.string.simple_save), fontSize = 16.sp)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.simple_cancel), fontSize = 16.sp) } },
  )
}
