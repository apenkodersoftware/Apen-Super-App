package com.aistudio.fasmovapos.app.ui.simple

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.ui.FasMovaPOSViewModel

@Composable
fun SimpleSellScreen(
  onOpenStock: () -> Unit,
  onOpenCustomers: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: FasMovaPOSViewModel = viewModel(),
) {
  val products by viewModel.products.collectAsStateWithLifecycle()
  val branch by viewModel.currentBranch.collectAsStateWithLifecycle()
  val cart by viewModel.cart.collectAsStateWithLifecycle()
  val payment by viewModel.paymentState.collectAsStateWithLifecycle()
  val customer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
  val customers by viewModel.customers.collectAsStateWithLifecycle()
  val saving by viewModel.isCheckingOut.collectAsStateWithLifecycle()
  val query by viewModel.posSearchQuery.collectAsStateWithLifecycle()
  val category by viewModel.posCategoryFilter.collectAsStateWithLifecycle()
  var measured by remember { mutableStateOf<ProductEntity?>(null) }
  var reviewing by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(cart.isEmpty()) {
    if (cart.isEmpty()) {
      reviewing = false
      if (viewModel.cart.value.isEmpty()) viewModel.clearCart()
    }
  }
  BackHandler(enabled = reviewing) { if (!saving) reviewing = false }
  val visibleProducts = remember(products, branch.id) { products.filter { it.branchId == branch.id } }
  val catalogState = remember(visibleProducts, query, category) { CatalogState(visibleProducts, query, category) }
  Column(modifier = modifier.fillMaxSize()) {
    if (reviewing && cart.isNotEmpty()) {
      SimpleChargeScreen(
        state = ChargeState(cart, payment, customers, customer, saving),
        actions =
          ChargeActions(
            changeQuantity = viewModel::updateCartQuantity,
            remove = viewModel::removeFromCart,
            updatePayment = viewModel::updatePayment,
            selectCustomer = viewModel::selectCustomer,
            openCustomers = onOpenCustomers,
            complete = viewModel::checkoutSale,
            back = { reviewing = false },
          ),
      )
    } else {
      SimpleCatalog(
        state = catalogState,
        onSearchChange = viewModel::setPosSearch,
        onCategoryChange = viewModel::setPosCategory,
        onAddToCart = { if (it.quantityScale == 1) viewModel.addToCart(it) else measured = it },
        onOpenStock = onOpenStock,
        modifier = Modifier.weight(1f),
      )
      if (cart.isNotEmpty()) {
        val gross = cart.sumOf { it.lineTotal }
        SimpleCartBar(
          itemCount = cart.size,
          total = (gross - payment.discountAmount(gross)).coerceAtLeast(0.0),
          onCharge = { reviewing = true },
        )
      }
    }
  }
  measured?.let { product ->
    QuantityDialog(
      product = product,
      held = cart.filter { it.product.id == product.id }.sumOf { it.quantity.toLong() },
      onDismiss = { measured = null },
      onAdd = {
        viewModel.addMeasuredToCart(it)
        measured = null
      },
    )
  }
}

@Composable
fun SimpleCartBar(
  itemCount: Int,
  total: Double,
  onCharge: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Button(
    onClick = onCharge,
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .heightIn(min = 64.dp)
        .testTag("simple_charge_bar"),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        pluralStringResource(R.plurals.simple_cart_count, itemCount, itemCount, simpleMoney(total)),
        fontSize = 18.sp,
        modifier = Modifier.weight(1f),
      )
      Text(
        stringResource(R.string.simple_charge),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp),
      )
    }
  }
}
