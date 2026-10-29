package com.aistudio.fasmovapos.app.ui.simple

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.fasmovapos.app.BackupTab
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.data.repository.ProductMeasure
import com.aistudio.fasmovapos.app.ui.FasMovaPOSViewModel
import com.aistudio.fasmovapos.app.ui.components.DisplayControl
import com.aistudio.fasmovapos.app.ui.components.ReceiptModal
import com.aistudio.fasmovapos.app.ui.screens.CustomersScreen
import com.aistudio.fasmovapos.app.ui.screens.ExpensesScreen
import com.aistudio.fasmovapos.app.ui.screens.SalesHistoryScreen
import com.aistudio.fasmovapos.app.ui.screens.SuppliersScreen

@Composable
fun SimpleMainShell(
  displayControl: DisplayControl,
  modifier: Modifier = Modifier,
  viewModel: FasMovaPOSViewModel = viewModel(),
) {
  val storedTab by viewModel.currentTab.collectAsStateWithLifecycle()
  val primaryTabs = setOf("POS", "INVENTORY", "TODAY", "MORE")
  val secondaryTabs = setOf("EXPENSES", "CUSTOMERS", "SUPPLIERS", "BACKUP", "SALES", "REPORTS", "LOANS")
  val currentTab = visibleSimpleTab(storedTab, primaryTabs, secondaryTabs)
  var returnTab by rememberSaveable { mutableStateOf("MORE") }
  BackHandler(enabled = currentTab != "POS") { viewModel.setTab(if (currentTab in primaryTabs) "POS" else returnTab) }
  val currentBranch by viewModel.currentBranch.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

  val products by viewModel.products.collectAsStateWithLifecycle()
  val sales by viewModel.sales.collectAsStateWithLifecycle()
  val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
  val purchaseOrders by viewModel.purchaseOrders.collectAsStateWithLifecycle()
  val expenses by viewModel.expenses.collectAsStateWithLifecycle()
  val customers by viewModel.customers.collectAsStateWithLifecycle()
  val customerPayments by viewModel.customerPayments.collectAsStateWithLifecycle()

  val isReceiptOpen by viewModel.isReceiptOpen.collectAsStateWithLifecycle()
  val lastSaleEntity by viewModel.lastSaleEntity.collectAsStateWithLifecycle()
  val lastSaleItems by viewModel.lastSaleItems.collectAsStateWithLifecycle()
  val lastSalePayments by viewModel.lastSalePayments.collectAsStateWithLifecycle()
  val salePayments by viewModel.salePayments.collectAsStateWithLifecycle()
  val reportInput =
    ReportInput(
      sales.filter { it.branchId == currentBranch.id },
      salePayments,
      expenses.filter { it.branchId == currentBranch.id },
      customerPayments,
      customers,
    )

  val recordEvents by viewModel.recordingEvents.collectAsStateWithLifecycle()
  val recordOrigins by viewModel.recordOrigins.collectAsStateWithLifecycle()
  val recordedDays by viewModel.recordedDays.collectAsStateWithLifecycle()
  val recordContext = RecordContext(recordEvents, recordOrigins, recordedDays)

  val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(toastMessage) {
    toastMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearToast()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = { SimpleHeader(shopName = currentBranch.name, displayControl = displayControl) },
    bottomBar = {
      SimpleNavigation(
        currentTab =
          if (currentTab in
            primaryTabs
          ) {
            currentTab
          } else {
            returnTab
          },
        onTabSelect = viewModel::setTab,
      )
    },
    snackbarHost = { LiteSnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      if (currentTab in secondaryTabs) {
        TextButton(onClick = {
          viewModel.setTab(returnTab)
        }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.simple_back)) }
      }
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when (currentTab) {
          "MORE" ->
            MoreScreen(
              branch = currentBranch,
              displayControl = displayControl,
              canEditShop = currentUser.role == "OWNER",
              onNavigate = { tab ->
                returnTab = "MORE"
                viewModel.setTab(tab)
              },
              onSaveShop = viewModel::updateBusinessDetails,
              onLogout = {
                viewModel.setTab("POS")
                viewModel.signOut()
              },
            )
          "POS" ->
            SimpleSellScreen(onOpenStock = { viewModel.setTab("INVENTORY") }, onOpenCustomers = {
              returnTab = "POS"
              viewModel.setTab("CUSTOMERS")
            })

          "INVENTORY" ->
            SimpleStockScreen(
              state = StockState(products.filter { it.branchId == currentBranch.id }),
              onAdd = { item ->
                viewModel.addMeasuredProduct(
                  item.sku,
                  item.name,
                  item.category,
                  item.cost,
                  item.price,
                  item.quantity,
                  item.threshold,
                  ProductMeasure(item.quantityScale, item.quarterPrice, item.halfPrice, item.unit),
                )
              },
              onAdjust = { id, delta, cost, price -> viewModel.adjustStock(id, delta, cost, price) },
              onMeasuredAdjust = { product, delta, cost, price, quarter, half, reason ->
                viewModel.adjustStock(product.id, delta, cost, price, quarter, half, reason)
              },
              onFavorite = viewModel::setFavorite,
            )

          "TODAY", "REPORTS" ->
            BusinessReportScreen(
              reportInput,
              recordContext = recordContext,
              businessName = currentBranch.name,
              branchId = currentBranch.id,
              onHistory = {
                returnTab = "TODAY"
                viewModel.setTab("SALES")
              },
              onNoTrade = viewModel::setNoTradeDay,
              onExpense = {
                returnTab = "REPORTS"
                viewModel.setTab("EXPENSES")
              },
              onDebts = {
                returnTab = "REPORTS"
                viewModel.setTab("CUSTOMERS")
              },
            )

          "SALES" ->
            SalesHistoryScreen(
              sales = sales,
              salePayments = salePayments,
              onReprintReceipt = { sale -> viewModel.showReceipt(sale) },
            )

          "EXPENSES" ->
            ExpensesScreen(
              expenses = expenses,
              onAddExpense = { cat, amt, desc ->
                viewModel.addNewExpense(cat, amt, desc)
              },
            )

          "SUPPLIERS" ->
            SuppliersScreen(
              suppliers = suppliers,
              purchaseOrders = purchaseOrders,
              onAddSupplier = { name, phone, email, contact, addr ->
                viewModel.addNewSupplier(name, phone, email, contact, addr)
              },
              onCreatePO = { sup, total ->
                viewModel.createPurchaseOrder(sup, total)
              },
            )

          "CUSTOMERS" ->
            CustomersScreen(
              customers = customers,
              payments = customerPayments,
              records = DebtRecords(sales, salePayments),
              onAddCustomer = { name, phone, email, limit ->
                viewModel.addNewCustomer(name, phone, email, limit)
              },
              onRecordPayment = { cust, amt, method, ref ->
                viewModel.recordCustomerPayment(cust, amt, method, ref)
              },
            )

          "LOANS" ->
            com.aistudio.fasmovapos.app.ui.companion
              .LoanSupportScreen()
          "BACKUP" -> BackupTab(currentUser = currentUser)
        }
      }
    }

    // Printable Digital Receipt Overlay
    if (isReceiptOpen && lastSaleEntity != null) {
      ReceiptModal(
        sale = lastSaleEntity,
        items = lastSaleItems,
        payments = lastSalePayments,
        branch = currentBranch,
        onDismiss = { viewModel.dismissReceipt() },
      )
    }
  }
}

private fun visibleSimpleTab(
  stored: String,
  primary: Set<String>,
  secondary: Set<String>,
): String = stored.takeIf { it in primary || it in secondary } ?: "MORE"

/** Keep save confirmations above the fixed sale/payment actions instead of covering their touch targets. */
@Composable
internal fun LiteSnackbarHost(
  state: SnackbarHostState,
  modifier: Modifier = Modifier,
) {
  SnackbarHost(hostState = state, modifier = modifier.padding(bottom = 80.dp))
}
