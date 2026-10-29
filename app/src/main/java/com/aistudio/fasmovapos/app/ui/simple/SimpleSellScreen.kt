package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.repository.RetailQuantity
import com.aistudio.fasmovapos.app.ui.language.categoryKey
import com.aistudio.fasmovapos.app.ui.language.categoryLabel
import com.aistudio.fasmovapos.app.ui.language.localizedProductName
import com.aistudio.fasmovapos.app.ui.language.productLabel
import com.aistudio.fasmovapos.app.ui.theme.FasMovaStatus
import java.math.BigDecimal
import java.math.RoundingMode

/** Display formatting only; checkout and persisted money are calculated by the existing model. */
fun displayAmount(amount: Double): String {
  val rounded = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
  return if (rounded.stripTrailingZeros().scale() <= 0) rounded.setScale(0).toPlainString() else rounded.toPlainString()
}

@Composable
fun simpleMoney(amount: Double): String = stringResource(R.string.simple_money, displayAmount(amount))

data class CatalogState(
  val products: List<ProductEntity>,
  val searchQuery: String,
  val categoryFilter: String,
)

@Composable
fun SimpleCatalog(
  state: CatalogState,
  onSearchChange: (String) -> Unit,
  onCategoryChange: (String) -> Unit,
  onAddToCart: (ProductEntity) -> Unit,
  onOpenStock: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val products = state.products
  val searchQuery = state.searchQuery
  val categoryFilter = categoryKey(state.categoryFilter)
  val categories =
    listOf("__FAVOURITES__", "All") +
      products
        .map { categoryKey(it.category) }
        .filter { it.isNotBlank() && it != "All" }
        .distinct()
        .sorted()
  val activeCategory = categoryFilter.takeIf { it in categories } ?: "All"
  val visible =
    products.filter { product ->
      (
        activeCategory == "All" ||
          (activeCategory == "__FAVOURITES__" && product.isFavorite) ||
          categoryKey(product.category) == activeCategory
      ) &&
        listOf(product.name, localizedProductName(context, product.id, product.name), product.sku, product.barcode).any {
          it.contains(searchQuery, ignoreCase = true)
        }
    }
  Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      label = { Text(stringResource(R.string.simple_search_items)) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("simple_search"),
    )
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      categories.forEach { category ->
        FilterChip(
          selected = activeCategory == category,
          onClick = { onCategoryChange(category) },
          label = {
            Text(
              when (category) {
                "All" -> stringResource(R.string.simple_all)
                "__FAVOURITES__" -> stringResource(R.string.lite_favourites)
                else -> categoryLabel(category)
              },
              fontSize = 16.sp,
            )
          },
          modifier = Modifier.heightIn(min = 48.dp),
        )
      }
    }
    if (visible.isEmpty()) {
      Text(stringResource(if (products.isEmpty()) R.string.simple_empty_shop else R.string.simple_no_matches), fontSize = 16.sp)
      Button(
        onClick =
          if (products.isEmpty()) {
            onOpenStock
          } else {
            (
              {
                onSearchChange("")
                onCategoryChange("All")
              }
            )
          },
      ) {
        Text(stringResource(if (products.isEmpty()) R.string.simple_first_item else R.string.simple_clear_search), fontSize = 16.sp)
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(visible, key = { it.id }) { product -> SimpleProductCard(product, onAddToCart) }
      }
    }
  }
}

@Composable
private fun SimpleProductCard(
  product: ProductEntity,
  onAdd: (ProductEntity) -> Unit,
) {
  Card(
    onClick = { onAdd(product) },
    enabled = product.stockQuantity > 0,
    colors =
      CardDefaults.cardColors(
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurface,
      ),
    modifier = Modifier.fillMaxWidth().heightIn(min = 144.dp).testTag("simple_product_${product.id}"),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        productLabel(product.id, product.name),
        modifier = Modifier.fillMaxWidth(),
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Medium,
      )
      Text(
        simpleMoney(product.sellingPrice),
        modifier = Modifier.fillMaxWidth(),
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
      )
      if (product.quantityScale != 1) Text("/ " + product.unit, fontSize = 16.sp)
      Text(
        if (product.quantityScale == 1) {
          pluralStringResource(R.plurals.simple_stock_left, product.stockQuantity, product.stockQuantity)
        } else {
          RetailQuantity.label(product.stockQuantity, product.quantityScale, product.unit)
        },
        modifier = Modifier.fillMaxWidth(),
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color =
          when {
            product.stockQuantity == 0 -> FasMovaStatus.danger
            product.stockQuantity <= product.minStockThreshold -> FasMovaStatus.warning
            else -> MaterialTheme.colorScheme.onSurface
          },
      )
    }
  }
}
