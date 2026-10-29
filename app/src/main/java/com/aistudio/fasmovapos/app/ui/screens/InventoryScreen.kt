package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.ui.components.filterChipLabel
import com.aistudio.fasmovapos.app.ui.language.categoryKey
import com.aistudio.fasmovapos.app.ui.language.categoryLabel
import com.aistudio.fasmovapos.app.ui.language.localizedProductName
import com.aistudio.fasmovapos.app.ui.language.productLabel
import com.aistudio.fasmovapos.app.ui.language.unitLabel
import com.aistudio.fasmovapos.app.ui.theme.FasMovaStatus

@Composable
fun InventoryScreen(
  products: List<ProductEntity>,
  lowStockProducts: List<ProductEntity>,
  onAddProduct: (String, String, String, String, String, Double, Double, Int, Int) -> Unit,
  onAdjustStock: (String, Int, Double?, Double?) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("All") }
  var showAddDialog by remember { mutableStateOf(false) }
  var selectedAdjustProduct by remember { mutableStateOf<ProductEntity?>(null) }

  val categories =
    listOf("All", "Low Stock") +
      products
        .map { categoryKey(it.category) }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

  val context = LocalContext.current
  val filteredList =
    products.filter { prod ->
      val matchesSearch =
        searchQuery.isBlank() ||
          prod.name.contains(searchQuery, ignoreCase = true) ||
          localizedProductName(context, prod.id, prod.name).contains(searchQuery, ignoreCase = true) ||
          prod.sku.contains(searchQuery, ignoreCase = true) ||
          prod.barcode.contains(searchQuery, ignoreCase = true)
      val matchesFilter =
        when (selectedFilter) {
          "All" -> true
          "Low Stock" -> prod.stockQuantity <= prod.minStockThreshold
          else -> categoryKey(prod.category).equals(categoryKey(selectedFilter), ignoreCase = true)
        }
      matchesSearch && matchesFilter
    }

  val totalStockValuationCost = products.sumOf { it.stockQuantity * it.costPrice }
  val totalStockValuationRetail = products.sumOf { it.stockQuantity * it.sellingPrice }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp),
  ) {
    // Top Hero Banner
    Card(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(110.dp),
      shape = RoundedCornerShape(12.dp),
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Image(
          painter = painterResource(id = R.drawable.img_pos_hero_1785386853893),
          contentDescription = stringResource(R.string.inventory_hero),
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.5f))
              .padding(16.dp),
        ) {
          Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
              text = stringResource(R.string.inventory_stock_management),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
            Text(
              text = stringResource(R.string.multi_branch_stock_tracking_low_stock),
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.8f),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Metrics Summary Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MetricCard(
        title = stringResource(R.string.total_skus),
        value = "${products.size}",
        subtitle = stringResource(R.string.active_catalog_items),
        modifier = Modifier.weight(1f),
      )
      MetricCard(
        title = stringResource(R.string.stock_valuation),
        value = "KES ${String.format("%.0f", totalStockValuationRetail)}",
        subtitle = stringResource(R.string.cost_kes, String.format("%.0f", totalStockValuationCost)),
        modifier = Modifier.weight(1.3f),
      )
      MetricCard(
        title = stringResource(R.string.low_stock_alerts),
        value = "${lowStockProducts.size}",
        subtitle = stringResource(R.string.action_required),
        color = if (lowStockProducts.isNotEmpty()) FasMovaStatus.danger else MaterialTheme.colorScheme.primary,
        modifier = Modifier.weight(1f),
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Search & Filter & Add Button Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text(stringResource(R.string.filter_inventory)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
        singleLine = true,
        modifier =
          Modifier
            .weight(1f)
            .testTag("inventory_search_field"),
        shape = RoundedCornerShape(12.dp),
      )

      Spacer(modifier = Modifier.width(12.dp))

      Button(
        onClick = { showAddDialog = true },
        modifier =
          Modifier
            .height(56.dp)
            .testTag("add_product_button"),
        shape = RoundedCornerShape(12.dp),
      ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
        Spacer(modifier = Modifier.width(4.dp))
        Text(stringResource(R.string.new_product))
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      categories.forEach { cat ->
        val isSel = selectedFilter == cat
        FilterChip(
          selected = isSel,
          onClick = { selectedFilter = cat },
          label = { Text(filterChipLabel(cat), fontSize = 12.sp) },
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Product Catalog List
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(filteredList) { prod ->
        InventoryItemRow(
          product = prod,
          onAdjust = { selectedAdjustProduct = prod },
        )
      }
    }
  }

  // Add Product Modal
  if (showAddDialog) {
    AddProductDialog(
      onDismiss = { showAddDialog = false },
      onConfirm = { sku, barcode, name, cat, unit, cost, sell, stock, minT ->
        onAddProduct(sku, barcode, name, cat, unit, cost, sell, stock, minT)
        showAddDialog = false
      },
    )
  }

  // Adjust Stock Modal
  selectedAdjustProduct?.let { prod ->
    AdjustStockDialog(
      product = prod,
      onDismiss = { selectedAdjustProduct = null },
      onConfirm = { changeQty, newCost, newSell ->
        onAdjustStock(prod.id, changeQty, newCost, newSell)
        selectedAdjustProduct = null
      },
    )
  }
}

@Composable
private fun MetricCard(
  title: String,
  value: String,
  subtitle: String,
  color: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
      Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
  }
}

@Composable
private fun InventoryItemRow(
  product: ProductEntity,
  onAdjust: () -> Unit,
) {
  val isLow = product.stockQuantity <= product.minStockThreshold

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
          .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isLow) FasMovaStatus.warningContainer else MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Inventory,
          contentDescription = stringResource(R.string.item),
          tint = if (isLow) FasMovaStatus.warning else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1.5f)) {
        Text(
          text = productLabel(product.id, product.name),
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          text = stringResource(R.string.sku_barcode, product.sku, product.barcode, categoryLabel(product.category)),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
        )
      }

      Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
        Text(
          text = "KES ${String.format("%.0f", product.sellingPrice)}",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = stringResource(R.string.cost_kes, String.format("%.0f", product.costPrice)),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isLow) FasMovaStatus.dangerContainer else FasMovaStatus.successContainer,
        modifier = Modifier.padding(horizontal = 4.dp),
      ) {
        Text(
          text = "${product.stockQuantity} ${unitLabel(product.unit)}",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = if (isLow) FasMovaStatus.onDangerContainer else FasMovaStatus.onSuccessContainer,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }

      IconButton(onClick = onAdjust, modifier = Modifier.testTag("adjust_stock_button_${product.id}")) {
        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.adjust), tint = MaterialTheme.colorScheme.primary)
      }
    }
  }
}

@Composable
private fun AddProductDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String, String, Double, Double, Int, Int) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var sku by remember { mutableStateOf("") }
  var barcode by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Household") }
  var unit by remember { mutableStateOf("Pcs") }
  var costPrice by remember { mutableStateOf("") }
  var sellingPrice by remember { mutableStateOf("") }
  var initialStock by remember { mutableStateOf("") }
  var minThreshold by remember { mutableStateOf("5") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(16.dp),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          stringResource(R.string.register_new_inventory_product),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = name, onValueChange = {
          name = it
        }, label = {
          Text(stringResource(R.string.product_name))
        }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("add_product_name"))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = sku, onValueChange = {
            sku = it
          }, label = { Text(stringResource(R.string.sku_auto)) }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = barcode, onValueChange = {
            barcode = it
          }, label = { Text(stringResource(R.string.barcode)) }, singleLine = true, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = category, onValueChange = {
            category = it
          }, label = { Text(stringResource(R.string.category)) }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = unit, onValueChange = {
            unit = it
          }, label = { Text(stringResource(R.string.unit_pcs_kg)) }, singleLine = true, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = costPrice,
            onValueChange = {
              costPrice = it
            },
            label = {
              Text(stringResource(R.string.cost_price_kes))
            },
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Number,
              ),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = sellingPrice,
            onValueChange = {
              sellingPrice = it
            },
            label = {
              Text(stringResource(R.string.selling_price_kes))
            },
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Number,
              ),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = initialStock,
            onValueChange = {
              initialStock = it
            },
            label = {
              Text(stringResource(R.string.initial_stock_qty))
            },
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Number,
              ),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = minThreshold,
            onValueChange = {
              minThreshold = it
            },
            label = {
              Text(stringResource(R.string.min_stock_alert))
            },
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Number,
              ),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (name.isNotBlank() && sellingPrice.isNotBlank()) {
                onConfirm(
                  sku,
                  barcode,
                  name,
                  category,
                  unit,
                  costPrice.toDoubleOrNull() ?: 0.0,
                  sellingPrice.toDoubleOrNull() ?: 0.0,
                  initialStock.toIntOrNull() ?: 0,
                  minThreshold.toIntOrNull() ?: 5,
                )
              }
            },
            modifier = Modifier.testTag("confirm_add_product_button"),
          ) {
            Text(stringResource(R.string.save_product))
          }
        }
      }
    }
  }
}

@Composable
private fun AdjustStockDialog(
  product: ProductEntity,
  onDismiss: () -> Unit,
  onConfirm: (Int, Double?, Double?) -> Unit,
) {
  var changeQtyStr by remember { mutableStateOf("10") }
  var isRestock by remember { mutableStateOf(true) }
  var newCostStr by remember { mutableStateOf(product.costPrice.toString()) }
  var newSellStr by remember { mutableStateOf(product.sellingPrice.toString()) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          stringResource(R.string.stock_adjustment, productLabel(product.id, product.name)),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
          stringResource(R.string.current_stock, product.stockQuantity, unitLabel(product.unit)),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(
            onClick = { isRestock = true },
            modifier = Modifier.weight(1f),
            colors =
              if (isRestock) {
                ButtonDefaults.outlinedButtonColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
              } else {
                ButtonDefaults.outlinedButtonColors()
              },
          ) {
            Text(stringResource(R.string.restock_add))
          }
          Spacer(modifier = Modifier.width(8.dp))
          OutlinedButton(
            onClick = { isRestock = false },
            modifier = Modifier.weight(1f),
            colors =
              if (!isRestock) {
                ButtonDefaults.outlinedButtonColors(
                  containerColor = FasMovaStatus.dangerContainer,
                )
              } else {
                ButtonDefaults.outlinedButtonColors()
              },
          ) {
            Text(stringResource(R.string.damage_deduct))
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = changeQtyStr,
          onValueChange = { changeQtyStr = it },
          label = { Text(stringResource(R.string.adjustment_quantity, unitLabel(product.unit))) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newCostStr,
            onValueChange = {
              newCostStr = it
            },
            label = {
              Text(stringResource(R.string.update_cost_price))
            },
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Number,
              ),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = newSellStr,
            onValueChange = {
              newSellStr = it
            },
            label = {
              Text(stringResource(R.string.update_sell_price))
            },
            keyboardOptions =
              KeyboardOptions(
                keyboardType = KeyboardType.Number,
              ),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val qty = (changeQtyStr.toIntOrNull() ?: 0) * (if (isRestock) 1 else -1)
              onConfirm(qty, newCostStr.toDoubleOrNull(), newSellStr.toDoubleOrNull())
            },
          ) {
            Text(stringResource(R.string.apply_adjustment))
          }
        }
      }
    }
  }
}
