package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.repository.CartItem
import com.aistudio.fasmovapos.app.data.repository.RetailQuantity
import com.aistudio.fasmovapos.app.ui.language.productLabel

@Composable
fun QuantityDialog(
  product: ProductEntity,
  held: Long,
  onDismiss: () -> Unit,
  onAdd: (CartItem) -> Unit,
) {
  var value by remember(product.id) { mutableStateOf("0.25") }
  var byMoney by remember(product.id) { mutableStateOf(false) }
  var entryScale by remember(product.id) { mutableIntStateOf(1000) }
  var pricingQuantity by remember(product.id) { mutableIntStateOf(product.quantityScale) }
  var pricingPrice by remember(product.id) { mutableStateOf(product.sellingPrice) }
  val quantity =
    if (byMoney) {
      RetailQuantity.forBudget(
        value,
        product.sellingPrice,
        product.quantityScale,
      )
    } else {
      RetailQuantity.parse(value, entryScale)
    }
  val quote = quantity?.let { CartItem(product, it, pricingQuantity, pricingPrice) }
  val available = (product.stockQuantity.toLong() - held).coerceAtLeast(0).toInt()
  val valid = quote != null && quote.quantity > 0 && quote.quantity <= available && quote.lineTotal > 0
  AlertDialog(
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.padding(horizontal = 16.dp),
    onDismissRequest = onDismiss,
    title = { Text(productLabel(product.id, product.name)) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(simpleMoney(product.sellingPrice) + " / " + product.unit)
        Text(stringResource(R.string.lite_available, RetailQuantity.label(available, product.quantityScale, product.unit)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(selected = !byMoney, onClick = {
            byMoney = false
            value = "0.25"
            entryScale = 1000
          }, label = { Text(stringResource(R.string.lite_by_quantity)) })
          FilterChip(selected = byMoney, onClick = {
            byMoney = true
            value = ""
            pricingQuantity = product.quantityScale
            pricingPrice = product.sellingPrice
          }, label = { Text(stringResource(R.string.lite_by_money)) })
        }
        if (!byMoney) {
          Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(250, 500, 1000).forEach { base ->
              Button(onClick = {
                entryScale = 1000
                value = RetailQuantity.decimal(base, 1000)
                val preset = portionQuote(product, base)
                pricingQuantity = preset.pricingQuantity
                pricingPrice = preset.pricingPrice
              }, modifier = Modifier.testTag("lite_portion_$base")) {
                Text(RetailQuantity.label(base, 1000, product.unit))
              }
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1000, 1).forEach { scale ->
              FilterChip(selected = entryScale == scale, onClick = {
                value = quantity?.let { RetailQuantity.decimal(it, scale) }.orEmpty()
                entryScale = scale
              }, label = {
                Text(
                  if (scale == 1000) {
                    product.unit
                  } else if (product.unit == "Kg") {
                    "g"
                  } else {
                    "ml"
                  },
                )
              })
            }
          }
        }
        OutlinedTextField(
          value = value,
          onValueChange = {
            value = it
            pricingQuantity = product.quantityScale
            pricingPrice = product.sellingPrice
          },
          label = { Text(if (byMoney) stringResource(R.string.lite_budget) else stringResource(R.string.simple_quantity)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("lite_quantity_input"),
        )
        if (quote != null && quote.quantity > 0) {
          Text(quote.quantityLabel + " — " + simpleMoney(quote.lineTotal), modifier = Modifier.testTag("lite_quantity_quote"))
          if (pricingQuantity != product.quantityScale) Text(stringResource(R.string.lite_portion_price))
        }
        if (byMoney) Text(stringResource(R.string.lite_budget_note))
        if (!valid) Text(stringResource(R.string.lite_quantity_error))
      }
    },
    confirmButton = {
      Button(onClick = { quote?.let(onAdd) }, enabled = valid, modifier = Modifier.testTag("lite_add_quantity")) {
        Text(stringResource(R.string.lite_add_sale))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.simple_cancel)) } },
  )
}

private fun portionQuote(
  product: ProductEntity,
  base: Int,
): CartItem {
  val price =
    when (base) {
      250 -> product.quarterPrice
      500 -> product.halfPrice
      else -> 0.0
    }
  return if (price > 0) CartItem(product, base, base, price) else CartItem(product, base)
}
