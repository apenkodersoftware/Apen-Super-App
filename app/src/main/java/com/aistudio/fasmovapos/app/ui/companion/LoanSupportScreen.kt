package com.aistudio.fasmovapos.app.ui.companion

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.companion.InukaCatalogue
import com.aistudio.fasmovapos.app.data.companion.LoanPlan
import com.aistudio.fasmovapos.app.data.companion.LoanPlanInput
import com.aistudio.fasmovapos.app.data.companion.LoanPlansStore
import com.aistudio.fasmovapos.app.data.companion.LoanProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException
import java.util.UUID

@Composable
fun LoanSupportScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val store = remember { LoanPlansStore(context) }
  val scope = rememberCoroutineScope()
  var products by remember { mutableStateOf<List<LoanProduct>>(emptyList()) }
  var plans by remember { mutableStateOf<List<LoanPlan>>(emptyList()) }
  var problem by remember { mutableStateOf<Throwable?>(null) }
  var ready by remember { mutableStateOf(false) }
  var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
  var removeId by rememberSaveable { mutableStateOf<String?>(null) }
  LaunchedEffect(store) {
    try {
      products =
        withContext(Dispatchers.IO) {
          context.assets
            .open("inuka-product-facts.json")
            .bufferedReader()
            .use { InukaCatalogue.decode(it.readText()) }
        }
      store.plans.catch { problem = it }.collect {
        plans = it
        ready = true
      }
    } catch (error: IOException) {
      problem = error
    } catch (error: JSONException) {
      problem = error
    }
  }
  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("apen_loan_support"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding =
      androidx.compose.foundation.layout
        .PaddingValues(16.dp),
  ) {
    item { Text(stringResource(R.string.apen_loans), style = MaterialTheme.typography.headlineSmall) }
    item { Text(stringResource(R.string.apen_review_notice)) }
    item { Text(stringResource(R.string.apen_catalogue_date, InukaCatalogue.REVIEW_DATE)) }
    item {
      OutlinedButton(onClick = { openOfficialPage(context, InukaCatalogue.CONTACT) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.apen_contact))
      }
    }
    item { Text(stringResource(R.string.apen_my_plans), style = MaterialTheme.typography.titleLarge) }
    item { Text(stringResource(R.string.apen_plan_notice)) }
    if (problem != null) item { Text(stringResource(R.string.apen_storage_problem), color = MaterialTheme.colorScheme.error) }
    if (plans.isEmpty()) item { Text(stringResource(R.string.apen_empty_plans)) }
    items(plans, key = { "plan_" + it.id }) { plan ->
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(plan.productId, style = MaterialTheme.typography.titleMedium)
          Text(stringResource(R.string.apen_customer_entered))
          Text(LoanPlanInput.amountLabel(plan.amountCents) + " · " + plan.dueDate)
          TextButton(onClick = { removeId = plan.id }) { Text(stringResource(R.string.apen_remove_plan)) }
        }
      }
    }
    item { Text(stringResource(R.string.apen_products), style = MaterialTheme.typography.titleLarge) }
    item { Text(stringResource(R.string.apen_product_notice)) }
    items(products, key = LoanProduct::id) { product ->
      LoanProductCard(product = product, enabled = ready && problem == null, onPlan = { selectedId = product.id })
    }
    item {
      OutlinedButton(onClick = { openOfficialPage(context, InukaCatalogue.SOURCE) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.apen_open_catalogue))
      }
    }
  }
  selectedId?.let { id ->
    val product = products.find { it.id == id }
    if (product != null) {
      LoanPlanDialog(product = product, onDismiss = { selectedId = null }, onSave = { plan ->
        scope.launch {
          try {
            store.save(plan)
            selectedId = null
          } catch (error: IOException) {
            problem = error
          } catch (error: JSONException) {
            problem = error
          }
        }
      })
    }
  }
  removeId?.let { id ->
    AlertDialog(
      onDismissRequest = { removeId = null },
      title = { Text(stringResource(R.string.apen_remove_plan)) },
      text = { Text(stringResource(R.string.apen_remove_notice)) },
      confirmButton = {
        TextButton(onClick = {
          scope.launch {
            try {
              store.remove(id)
              removeId = null
            } catch (error: IOException) {
              problem = error
            } catch (error: JSONException) {
              problem = error
            }
          }
        }) { Text(stringResource(R.string.apen_remove_plan)) }
      },
      dismissButton = { TextButton(onClick = { removeId = null }) { Text(stringResource(R.string.apen_cancel)) } },
    )
  }
}

@Composable
private fun LoanProductCard(
  product: LoanProduct,
  enabled: Boolean,
  onPlan: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(modifier = modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(product.category, style = MaterialTheme.typography.labelLarge)
      Text(product.name, style = MaterialTheme.typography.titleMedium)
      Text(product.range)
      Text(product.tenor)
      Button(onClick = onPlan, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.apen_add_plan))
      }
    }
  }
}

@Composable
internal fun LoanPlanDialog(
  product: LoanProduct,
  onDismiss: () -> Unit,
  onSave: (LoanPlan) -> Unit,
) {
  var amount by rememberSaveable(product.id) { mutableStateOf("") }
  var date by rememberSaveable(product.id) { mutableStateOf("") }
  var submitted by remember { mutableStateOf(false) }
  val cents = LoanPlanInput.amountCents(amount)
  val valid = cents != null && LoanPlanInput.validDate(date)
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.apen_add_plan)) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(product.name)
        Text(stringResource(R.string.apen_plan_entry_note))
        OutlinedTextField(
          value = amount,
          onValueChange = { amount = it },
          label = { Text(stringResource(R.string.apen_next_amount)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("apen_plan_amount"),
        )
        OutlinedTextField(
          value = date,
          onValueChange = { date = it },
          label = { Text(stringResource(R.string.apen_due_date)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("apen_plan_date"),
        )
        if ((amount.isNotEmpty() || date.isNotEmpty()) && !valid) Text(stringResource(R.string.apen_invalid_plan))
      }
    },
    confirmButton = {
      TextButton(onClick = {
        if (cents != null && valid && !submitted) {
          submitted = true
          onSave(LoanPlan(UUID.randomUUID().toString(), product.id, cents, date))
        }
      }, enabled = valid && !submitted) { Text(stringResource(R.string.apen_save)) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.apen_cancel)) } },
  )
}

private fun openOfficialPage(
  context: Context,
  url: String,
) {
  try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
  } catch (error: ActivityNotFoundException) {
    Log.w("Apen", "No browser available for official support", error)
    Toast.makeText(context, context.getString(R.string.apen_no_browser, url), Toast.LENGTH_LONG).show()
  }
}
