package com.aistudio.fasmovapos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity

/** The Owner edits the business name and the optional phone and KRA PIN that receipts print. */
@Composable
fun BusinessDetailsDialog(
  branch: BranchEntity,
  onDismiss: () -> Unit,
  onSave: (name: String, phone: String, taxPin: String) -> Unit,
) {
  var name by remember { mutableStateOf(branch.name) }
  var phone by remember { mutableStateOf(branch.phone) }
  var taxPin by remember { mutableStateOf(branch.taxPin) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.business_details)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.business_details_hint), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.business_name)) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
          modifier = Modifier.fillMaxWidth().testTag("business_name_field"),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text(stringResource(R.string.business_phone_optional)) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth().testTag("business_phone_field"),
        )
        OutlinedTextField(
          value = taxPin,
          onValueChange = { taxPin = it.uppercase() },
          label = { Text(stringResource(R.string.kra_pin_optional)) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
          modifier = Modifier.fillMaxWidth().testTag("business_kra_field"),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onSave(name.trim(), phone.trim(), taxPin.trim()) },
        enabled = name.isNotBlank(),
        modifier = Modifier.testTag("business_details_save"),
      ) {
        Text(stringResource(R.string.save))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}
