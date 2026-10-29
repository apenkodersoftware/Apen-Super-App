package com.aistudio.fasmovapos.app.superapp

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

fun translated(
  context: Context,
  key: String,
): String {
  val resolved = if (key == "option_split") "option_mixed" else key
  val id =
    superLabelIds[resolved]
      ?: if (resolved.startsWith("option_")) superLabelIds.getValue("option_other") else error("Unknown UI label: $key")
  return context.getString(id)
}

@Composable fun S(key: String): String = translated(LocalContext.current, key)

@Composable fun dateLabel(value: String): String {
  val locale = LocalContext.current.resources.configuration.locales[0] ?: Locale.UK
  return if (value.isBlank()) {
    "—"
  } else {
    java.text
      .SimpleDateFormat("EEE d MMM yyyy", locale)
      .apply {
        timeZone = SuperDates.zone
      }.format(SuperDates.formatter("yyyy-MM-dd").parse(value)!!)
  }
}

enum class FieldKind { TEXT, AMOUNT, NUMBER, DATE, CHOICE, FLAG }

data class SuperField(
  val id: String,
  val label: String,
  val value: String = "",
  val kind: FieldKind = FieldKind.TEXT,
  val options: List<Pair<String, String>> = emptyList(),
)

@Composable
fun ChoiceField(
  label: String,
  value: String,
  options: UiList<Pair<String, String>>,
  onChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  var expanded by remember { mutableStateOf(false) }
  Column(modifier) {
    Text(label)
    OutlinedButton(enabled = enabled, onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
      Text(options.firstOrNull { it.first == value }?.second ?: "—")
    }
    DropdownMenu(expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
      options.forEach { (key, text) ->
        DropdownMenuItem(text = { Text(text) }, onClick = {
          onChange(key)
          expanded = false
        }, modifier = Modifier.heightIn(min = 48.dp).testTag("choice-$key"))
      }
    }
  }
}

@Composable
fun DateField(
  label: String,
  value: String,
  onChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val context = LocalContext.current
  Column(modifier) {
    Text(label)
    OutlinedButton(enabled = enabled, onClick = {
      val parts = value.split('-').map(String::toInt)
      DatePickerDialog(
        context,
        { _, year, month, day -> onChange(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)) },
        parts[0],
        parts[1] - 1,
        parts[2],
      ).show()
    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(dateLabel(value)) }
  }
}

@Composable
fun SuperForm(
  title: String,
  fields: UiList<SuperField>,
  busy: Boolean,
  error: String?,
  onDismiss: () -> Unit,
  onSave: (Map<String, String>) -> Unit,
  modifier: Modifier = Modifier,
  extra: @Composable () -> Unit = {},
  preview: @Composable (Map<String, String>) -> Unit = {},
) {
  val values = remember(fields.map { it.id }) { fields.associate { it.id to it.value }.toList().toMutableStateMap() }
  AlertDialog(
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = modifier.padding(12.dp).testTag("super-form"),
    onDismissRequest = {
      if (!busy)onDismiss()
    },
    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(Modifier.heightIn(max = 490.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        fields.forEach { f ->
          val value = values[f.id].orEmpty()
          when (f.kind) {
            FieldKind.CHOICE ->
              ChoiceField(
                f.label,
                value,
                uiList(f.options),
                { values[f.id] = it },
                Modifier.testTag("field-${f.id}"),
                enabled = !busy,
              )
            FieldKind.DATE -> DateField(f.label, value, { values[f.id] = it }, Modifier.testTag("field-${f.id}"), enabled = !busy)
            FieldKind.FLAG ->
              Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = !busy) { values[f.id] = (!(value == "true")).toString() },
              ) {
                Checkbox(enabled = !busy, checked = value == "true", onCheckedChange = { values[f.id] = it.toString() })
                Text(f.label, modifier = Modifier.padding(top = 10.dp))
              }
            else ->
              OutlinedTextField(
                value,
                onValueChange = { values[f.id] = it },
                label = { Text(f.label) },
                enabled = !busy,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions =
                  KeyboardOptions(
                    keyboardType =
                      when (f.kind) {
                        FieldKind.AMOUNT -> KeyboardType.Decimal
                        FieldKind.NUMBER -> KeyboardType.Number
                        else -> KeyboardType.Text
                      },
                  ),
                modifier = Modifier.fillMaxWidth().testTag("field-${f.id}"),
              )
          }
        }
        extra()
        preview(values.toMap())
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("form-error"))
      }
    },
    confirmButton = {
      Button(onClick = {
        onSave(values.toMap())
      }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp).testTag("action-save")) { Text(S(if (busy)"working" else "save")) }
    },
    dismissButton = { TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(S("cancel")) } },
  )
}

@Composable
fun optionLabels(keys: UiList<String>): List<Pair<String, String>> = keys.map { it to S("option_${it.lowercase(Locale.ROOT)}") }

fun costCategories(type: String): List<String> =
  when (type) {
    "DAIRY" -> listOf("FEED", "VET_AI", "LABOUR", "WATER_POWER", "TRANSPORT", "EQUIPMENT", "OTHER")
    "RENTAL" -> listOf("REPAIRS", "CARETAKER", "WATER", "ELECTRICITY", "SECURITY", "RATES", "AGENT", "OTHER")
    "RESTAURANT" -> listOf("CEREALS", "VEGETABLES", "MEAT_FISH", "OIL_GAS", "FUEL", "WATER", "WAGES", "RENT_PERMIT", "OTHER")
    else -> listOf("RENT_PERMIT", "WAGES", "TRANSPORT", "WATER_POWER", "OTHER")
  }
