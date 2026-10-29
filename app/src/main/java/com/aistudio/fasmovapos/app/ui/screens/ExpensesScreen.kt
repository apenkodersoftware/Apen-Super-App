package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.ExpenseEntity
import com.aistudio.fasmovapos.app.ui.language.expenseCategoryLabel
import com.aistudio.fasmovapos.app.ui.theme.FasMovaStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpensesScreen(
  expenses: List<ExpenseEntity>,
  onAddExpense: (String, Double, String) -> Unit,
) {
  var showAddDialog by remember { mutableStateOf(false) }

  val totalExpenses = expenses.sumOf { it.amount }

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
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            stringResource(R.string.total_operating_expenses),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            "KES ${String.format("%.2f", totalExpenses)}",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = if (totalExpenses == 0.0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
          )
        }

        Button(
          onClick = { showAddDialog = true },
          modifier = Modifier.fillMaxWidth().testTag("add_expense_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
          Spacer(modifier = Modifier.width(4.dp))
          Text(stringResource(R.string.record_expense), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Expenses List
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (expenses.isEmpty()) item { Text(stringResource(R.string.r2_empty_expenses), style = MaterialTheme.typography.bodyLarge) }
      items(expenses) { exp ->
        ExpenseRow(expense = exp)
      }
    }
  }

  if (showAddDialog) {
    AddExpenseDialog(
      onDismiss = { showAddDialog = false },
      onConfirm = { cat, amt, desc ->
        onAddExpense(cat, amt, desc)
        showAddDialog = false
      },
    )
  }
}

@Composable
private fun ExpenseRow(expense: ExpenseEntity) {
  val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
  val dateStr = dateFormat.format(Date(expense.timestamp))

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
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = FasMovaStatus.dangerContainer,
          ) {
            Text(
              text = expenseCategoryLabel(expense.category),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = FasMovaStatus.onDangerContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = expense.expenseRef,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = expense.description,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
          text = stringResource(R.string.recorded_by, dateStr, expense.recordedBy),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
        )
      }

      Text(
        text = "KES ${String.format("%.2f", expense.amount)}",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.error,
      )
    }
  }
}

@Composable
private fun AddExpenseDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, Double, String) -> Unit,
) {
  val defaultCategory = stringResource(R.string.expense_rent)
  var category by remember { mutableStateOf(defaultCategory) }
  var amountStr by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(24.dp),
    ) {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
          stringResource(R.string.record_business_expense),
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text(stringResource(R.string.category_rent_wages_freight_etc), modifier = Modifier.fillMaxWidth()) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("expense_category_field"),
        )

        OutlinedTextField(
          value = amountStr,
          onValueChange = { amountStr = it },
          label = { Text(stringResource(R.string.amount_kes), modifier = Modifier.fillMaxWidth()) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("expense_amount_field"),
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text(stringResource(R.string.description_notes), modifier = Modifier.fillMaxWidth()) },
          modifier = Modifier.fillMaxWidth().testTag("expense_description_field"),
        )

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
          TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          }
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = {
              val amt = amountStr.toDoubleOrNull() ?: 0.0
              if (amt > 0 && description.isNotBlank()) {
                onConfirm(category, amt, description)
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("confirm_expense_button"),
          ) {
            Text(stringResource(R.string.log_expense), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          }
        }
      }
    }
  }
}
