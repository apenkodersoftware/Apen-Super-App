package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.PurchaseOrderEntity
import com.aistudio.fasmovapos.app.data.db.entities.SupplierEntity
import com.aistudio.fasmovapos.app.ui.language.orderStatusLabel
import com.aistudio.fasmovapos.app.ui.language.personLabel

@Composable
fun SuppliersScreen(
  suppliers: List<SupplierEntity>,
  purchaseOrders: List<PurchaseOrderEntity>,
  onAddSupplier: (String, String, String, String, String) -> Unit,
  onCreatePO: (SupplierEntity, Double) -> Unit,
) {
  var showAddSupplierDialog by remember { mutableStateOf(false) }
  var selectedPoSupplier by remember { mutableStateOf<SupplierEntity?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(16.dp).testTag("supplier_list"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              stringResource(R.string.supplier_directory_pos),
              modifier = Modifier.fillMaxWidth(),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              stringResource(R.string.registered_suppliers, suppliers.size),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Button(
            onClick = { showAddSupplierDialog = true },
            modifier = Modifier.fillMaxWidth().testTag("add_supplier_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.new_supplier), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          }
        }
      }
    }
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        stringResource(R.string.active_vendors_debt_balances),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      )
    }
    items(suppliers, key = { "supplier:${it.id}" }) { sup ->
      SupplierRow(supplier = sup, onCreatePo = { selectedPoSupplier = sup })
    }
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        stringResource(R.string.recent_goods_received_notes_purchase_orders),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      )
    }
    items(purchaseOrders, key = { "order:${it.id}" }) { po ->
      PurchaseOrderRow(po = po)
    }
  }

  if (showAddSupplierDialog) {
    AddSupplierDialog(
      onDismiss = { showAddSupplierDialog = false },
      onConfirm = { name, phone, email, contact, addr ->
        onAddSupplier(name, phone, email, contact, addr)
        showAddSupplierDialog = false
      },
    )
  }

  selectedPoSupplier?.let { sup ->
    CreatePoDialog(
      supplier = sup,
      onDismiss = { selectedPoSupplier = null },
      onConfirm = { total ->
        onCreatePO(sup, total)
        selectedPoSupplier = null
      },
    )
  }
}

@Composable
private fun SupplierRow(
  supplier: SupplierEntity,
  onCreatePo: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          personLabel(supplier.id, supplier.name),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          stringResource(R.string.contact, supplier.contactPerson, supplier.phone),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          stringResource(R.string.address, supplier.address),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
        )
      }

      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = stringResource(R.string.balance_owed_kes, String.format("%.2f", supplier.balanceOwed)),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
          color = if (supplier.balanceOwed > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onCreatePo, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.create_po), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
      }
    }
  }
}

@Composable
private fun PurchaseOrderRow(po: PurchaseOrderEntity) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column {
        Text(
          "${po.poNumber} - ${personLabel(po.supplierId, po.supplierName)}",
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          stringResource(R.string.status_2, orderStatusLabel(po.status)),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      Text(
        "KES ${String.format("%.2f", po.totalAmount)}",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
      )
    }
  }
}

@Composable
private fun AddSupplierDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String, String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var contactPerson by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
          stringResource(R.string.register_supplier_vendor),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        OutlinedTextField(value = name, onValueChange = {
          name = it
        }, label = {
          Text(
            stringResource(R.string.company_name),
            modifier = Modifier.fillMaxWidth(),
          )
        }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("supplier_name"))
        OutlinedTextField(value = phone, onValueChange = {
          phone = it
        }, label = {
          Text(
            stringResource(R.string.phone_number),
            modifier = Modifier.fillMaxWidth(),
          )
        }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("supplier_phone"))
        OutlinedTextField(value = email, onValueChange = {
          email = it
        }, label = {
          Text(stringResource(R.string.email_address), modifier = Modifier.fillMaxWidth())
        }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contactPerson, onValueChange = {
          contactPerson = it
        }, label = {
          Text(stringResource(R.string.contact_person), modifier = Modifier.fillMaxWidth())
        }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = {
          address = it
        }, label = {
          Text(stringResource(R.string.location_address), modifier = Modifier.fillMaxWidth())
        }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
          TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          }
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = {
              if (name.isNotBlank() && phone.isNotBlank()) {
                onConfirm(name, phone, email, contactPerson, address)
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("confirm_supplier_button"),
          ) {
            Text(stringResource(R.string.save_supplier), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          }
        }
      }
    }
  }
}

@Composable
private fun CreatePoDialog(
  supplier: SupplierEntity,
  onDismiss: () -> Unit,
  onConfirm: (Double) -> Unit,
) {
  var amountStr by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
          stringResource(R.string.create_purchase_order, personLabel(supplier.id, supplier.name)),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it },
          label = { Text(stringResource(R.string.order_total_amount_kes), modifier = Modifier.fillMaxWidth()) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
          TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel), modifier = Modifier.fillMaxWidth())
          }
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = {
              val amt = amountStr.toDoubleOrNull() ?: 0.0
              if (amt > 0) {
                onConfirm(amt)
              }
            },
          ) {
            Text(stringResource(R.string.create_po_2), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          }
        }
      }
    }
  }
}
