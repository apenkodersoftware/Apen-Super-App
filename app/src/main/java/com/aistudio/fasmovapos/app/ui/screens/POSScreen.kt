package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.payment.PaymentState
import com.aistudio.fasmovapos.app.data.payment.formatAmount
import com.aistudio.fasmovapos.app.data.repository.CartItem
import com.aistudio.fasmovapos.app.ui.components.PaymentPanel
import com.aistudio.fasmovapos.app.ui.components.filterChipLabel
import com.aistudio.fasmovapos.app.ui.components.text
import com.aistudio.fasmovapos.app.ui.language.categoryKey
import com.aistudio.fasmovapos.app.ui.language.categoryLabel
import com.aistudio.fasmovapos.app.ui.language.localizedProductName
import com.aistudio.fasmovapos.app.ui.language.personLabel
import com.aistudio.fasmovapos.app.ui.language.productLabel
import com.aistudio.fasmovapos.app.ui.language.unitLabel
import com.aistudio.fasmovapos.app.ui.theme.FasMovaStatus

private val COMPACT_WIDTH = 600.dp

/** The totals + payment block may use at most this share of the screen height. */
private const val PAYMENT_AREA_MAX_SCREEN_FRACTION = 0.55f

/** Cart and checkout callbacks, grouped so the cart panel stays under the parameter-count limit. */
private class CartActions(
  val onClearCart: () -> Unit,
  val onUpdateCartQty: (String, Int) -> Unit,
  val onRemoveFromCart: (String) -> Unit,
  val onUpdatePayment: ((PaymentState) -> PaymentState) -> Unit,
  val onSelectCustomer: (CustomerEntity?) -> Unit,
  val onCheckout: () -> Unit,
)

@Composable
fun POSScreen(
  products: List<ProductEntity>,
  searchQuery: String,
  categoryFilter: String,
  cart: List<CartItem>,
  discount: Double,
  payment: PaymentState,
  selectedCustomer: CustomerEntity?,
  customers: List<CustomerEntity>,
  onSearchChange: (String) -> Unit,
  onCategoryChange: (String) -> Unit,
  onAddToCart: (ProductEntity) -> Unit,
  onUpdateCartQty: (String, Int) -> Unit,
  onRemoveFromCart: (String) -> Unit,
  onClearCart: () -> Unit,
  onUpdatePayment: ((PaymentState) -> PaymentState) -> Unit,
  onSelectCustomer: (CustomerEntity?) -> Unit,
  onCheckout: () -> Unit,
  isCheckingOut: Boolean = false,
) {
  val actions =
    CartActions(
      onClearCart = onClearCart,
      onUpdateCartQty = onUpdateCartQty,
      onRemoveFromCart = onRemoveFromCart,
      onUpdatePayment = onUpdatePayment,
      onSelectCustomer = onSelectCustomer,
      onCheckout = onCheckout,
    )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    // Phones are too narrow for a side-by-side catalog and cart, so they get a tab switch instead.
    val compact = maxWidth < COMPACT_WIDTH
    var showCart by rememberSaveable { mutableStateOf(false) }

    val catalogPanel: @Composable (Modifier) -> Unit = { panelModifier ->
      CatalogPanel(
        modifier = panelModifier,
        compact = compact,
        products = products,
        searchQuery = searchQuery,
        categoryFilter = categoryFilter,
        onSearchChange = onSearchChange,
        onCategoryChange = onCategoryChange,
        onAddToCart = onAddToCart,
      )
    }
    val cartPanel: @Composable (Modifier) -> Unit = { panelModifier ->
      CartPanel(
        modifier = panelModifier,
        cart = cart,
        discount = discount,
        payment = payment,
        selectedCustomer = selectedCustomer,
        customers = customers,
        actions = actions,
        isCheckingOut = isCheckingOut,
      )
    }

    if (compact) {
      Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = if (showCart) 1 else 0) {
          Tab(
            selected = !showCart,
            onClick = { showCart = false },
            text = { Text(stringResource(R.string.catalog)) },
            modifier = Modifier.testTag("pos_tab_catalog"),
          )
          Tab(
            selected = showCart,
            onClick = { showCart = true },
            text = { Text(stringResource(R.string.cart, cart.sumOf { it.quantity })) },
            modifier = Modifier.testTag("pos_tab_cart"),
          )
        }
        if (showCart) cartPanel(Modifier.weight(1f)) else catalogPanel(Modifier.weight(1f))
      }
    } else {
      Row(modifier = Modifier.fillMaxSize()) {
        catalogPanel(Modifier.weight(1.4f))
        Divider(
          modifier =
            Modifier
              .fillMaxHeight()
              .width(1.dp),
          color = MaterialTheme.colorScheme.outlineVariant,
        )
        cartPanel(Modifier.weight(1f))
      }
    }
  }
}

/** Product Search & Catalog Grid. */
@Composable
private fun CatalogPanel(
  compact: Boolean,
  products: List<ProductEntity>,
  searchQuery: String,
  categoryFilter: String,
  onSearchChange: (String) -> Unit,
  onCategoryChange: (String) -> Unit,
  onAddToCart: (ProductEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val categories =
    listOf("All") +
      products
        .map { categoryKey(it.category) }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
  val context = LocalContext.current
  val visibleProducts =
    products.filter {
      (categoryFilter == "All" || categoryKey(it.category).equals(categoryKey(categoryFilter), ignoreCase = true)) &&
        (
          searchQuery.isBlank() ||
            listOf(it.name, localizedProductName(context, it.id, it.name), it.sku, it.barcode).any { value ->
              value.contains(searchQuery, ignoreCase = true)
            }
        )
    }

  Column(
    modifier =
      modifier
        .fillMaxHeight()
        .padding(12.dp),
  ) {
    Text(stringResource(R.string.record_only_notice), style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(8.dp))

    // Search by name, SKU or a barcode entered with a keyboard/scanner.
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = {
          Text(
            text = if (compact) stringResource(R.string.search_products) else stringResource(R.string.search_product_by_name_sku_or),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
        singleLine = true,
        modifier =
          Modifier
            .weight(1f)
            .testTag("pos_search_field"),
        shape = RoundedCornerShape(12.dp),
      )

      Spacer(modifier = Modifier.width(8.dp))
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Category Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      categories.forEach { cat ->
        val isSel = categoryKey(categoryFilter) == cat
        FilterChip(
          selected = isSel,
          onClick = { onCategoryChange(cat) },
          label = { Text(filterChipLabel(cat), fontSize = 12.sp) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Product Grid
    if (visibleProducts.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.ShoppingBag,
            contentDescription = stringResource(R.string.empty),
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp),
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.no_matching_products_found),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(visibleProducts) { prod ->
          ProductPosCard(
            product = prod,
            onAdd = { onAddToCart(prod) },
          )
        }
      }
    }
  }
}

/** Shopping Cart & Checkout Panel. */
@Composable
private fun CartPanel(
  cart: List<CartItem>,
  discount: Double,
  payment: PaymentState,
  selectedCustomer: CustomerEntity?,
  customers: List<CustomerEntity>,
  actions: CartActions,
  isCheckingOut: Boolean,
  modifier: Modifier = Modifier,
) {
  val grossTotal = cart.sumOf { it.lineTotal }
  val netTotal = (grossTotal - discount).coerceAtLeast(0.0)

  Surface(
    modifier = modifier.fillMaxHeight(),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(12.dp),
    ) {
      // Cart Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.AddShoppingCart,
            contentDescription = stringResource(R.string.cart_2),
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.cart, cart.sumOf { it.quantity }),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
        }

        if (cart.isNotEmpty()) {
          TextButton(onClick = actions.onClearCart) {
            Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
          }
        }
      }

      CartCustomerPicker(
        selectedCustomer = selectedCustomer,
        customers = customers,
        onSelectCustomer = actions.onSelectCustomer,
      )

      // Cart Items List
      LazyColumn(
        modifier =
          Modifier
            .weight(1f)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(cart) { item ->
          CartItemRow(
            item = item,
            onIncrement = { actions.onUpdateCartQty(item.product.id, 1) },
            onDecrement = { actions.onUpdateCartQty(item.product.id, -1) },
            onRemove = { actions.onRemoveFromCart(item.product.id) },
          )
        }
      }

      Divider(color = MaterialTheme.colorScheme.outlineVariant)

      val paymentAreaMaxHeight = (LocalConfiguration.current.screenHeightDp * PAYMENT_AREA_MAX_SCREEN_FRACTION).dp

      // Totals + payment scroll together so the cart list and the PAY button stay visible on phones.
      Column(
        modifier =
          Modifier
            .heightIn(max = paymentAreaMaxHeight)
            .verticalScroll(rememberScrollState()),
      ) {
        CartTotals(
          grossTotal = grossTotal,
          discount = discount,
          netTotal = netTotal,
        )

        PaymentPanel(
          payment = payment,
          netTotal = netTotal,
          grossTotal = grossTotal,
          onUpdate = actions.onUpdatePayment,
        )
      }

      val paymentError = payment.validationError(netTotal, hasCustomer = selectedCustomer != null)
      if (paymentError != null && cart.isNotEmpty()) {
        Text(
          text = paymentError.text(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Checkout Button
      Button(
        onClick = actions.onCheckout,
        enabled = cart.isNotEmpty() && paymentError == null && !isCheckingOut,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("checkout_button"),
        shape = RoundedCornerShape(12.dp),
      ) {
        if (isCheckingOut) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          Text(
            text = stringResource(R.string.pay_kes, String.format("%.2f", netTotal)),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
        }
      }
    }
  }
}

/** Customer Selection Dropdown. */
@Composable
private fun CartCustomerPicker(
  selectedCustomer: CustomerEntity?,
  customers: List<CustomerEntity>,
  onSelectCustomer: (CustomerEntity?) -> Unit,
) {
  var showCustomerMenu by remember { mutableStateOf(false) }
  Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier =
        Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
          .clickable { showCustomerMenu = true }
          .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.PersonAdd,
          contentDescription = stringResource(R.string.customer),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text =
            selectedCustomer?.let { "${personLabel(it.id, it.name)} (${it.phone})" }
              ?: stringResource(R.string.select_customer_optional_credit),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = if (selectedCustomer != null) FontWeight.SemiBold else FontWeight.Normal,
          color =
            if (selectedCustomer != null) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.outline
            },
        )
      }
    }

    DropdownMenu(
      expanded = showCustomerMenu,
      onDismissRequest = { showCustomerMenu = false },
    ) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.walk_in_cash_customer_none)) },
        onClick = {
          onSelectCustomer(null)
          showCustomerMenu = false
        },
      )
      customers.forEach { c ->
        DropdownMenuItem(
          text = { Text(stringResource(R.string.debt_kes, personLabel(c.id, c.name), String.format("%.0f", c.currentDebt))) },
          onClick = {
            onSelectCustomer(c)
            showCustomerMenu = false
          },
        )
      }
    }
  }
}

/** Subtotal & Totals Calculation Card. */
@Composable
private fun CartTotals(
  grossTotal: Double,
  discount: Double,
  netTotal: Double,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
  ) {
    TotalsRow(stringResource(R.string.subtotal), "KES ${String.format("%.2f", grossTotal)}")
    if (discount > 0) {
      TotalsRow(stringResource(R.string.discount), "- KES ${formatAmount(discount)}")
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        stringResource(R.string.net_total),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        "KES ${String.format("%.2f", netTotal)}",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun TotalsRow(
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun ProductPosCard(
  product: ProductEntity,
  onAdd: () -> Unit,
) {
  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable { onAdd() }
        .testTag("product_card_${product.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Category Badge & Stock Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = categoryLabel(product.category),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }

        Surface(
          shape = RoundedCornerShape(4.dp),
          color =
            when {
              product.stockQuantity <= 0 -> FasMovaStatus.dangerContainer
              product.stockQuantity <= product.minStockThreshold -> FasMovaStatus.warningContainer
              else -> FasMovaStatus.successContainer
            },
        ) {
          Text(
            text = "${product.stockQuantity} ${unitLabel(product.unit)}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color =
              when {
                product.stockQuantity <= 0 -> FasMovaStatus.onDangerContainer
                product.stockQuantity <= product.minStockThreshold -> FasMovaStatus.onWarningContainer
                else -> FasMovaStatus.onSuccessContainer
              },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = productLabel(product.id, product.name),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        maxLines = 2,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Text(
        text = stringResource(R.string.sku, product.sku),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "KES ${String.format("%.0f", product.sellingPrice)}",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
        )

        IconButton(
          onClick = onAdd,
          enabled = product.stockQuantity > 0,
          modifier =
            Modifier
              .size(32.dp)
              .background(
                if (product.stockQuantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                CircleShape,
              ).testTag("add_to_cart_${product.id}"),
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.add),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun CartItemRow(
  item: CartItem,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onRemove: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = productLabel(item.product.id, item.product.name),
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
          maxLines = 1,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "KES ${String.format("%.0f", item.product.sellingPrice)} × ${item.quantity}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDecrement, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.decrease), modifier = Modifier.size(14.dp))
        }
        Text(
          text = "${item.quantity}",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          modifier = Modifier.padding(horizontal = 6.dp),
        )
        IconButton(onClick = onIncrement, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.Add, contentDescription = stringResource(R.string.increase), modifier = Modifier.size(14.dp))
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Text(
        text = "KES ${String.format(java.util.Locale.ROOT, "%.2f", item.lineTotal)}",
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
      )

      IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
        Icon(
          Icons.Default.DeleteOutline,
          contentDescription = stringResource(R.string.remove),
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}
