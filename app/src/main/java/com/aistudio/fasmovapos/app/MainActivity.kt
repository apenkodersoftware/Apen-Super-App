package com.aistudio.fasmovapos.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.auth.AuthState
import com.aistudio.fasmovapos.app.data.db.entities.StaffUserEntity
import com.aistudio.fasmovapos.app.onboarding.SplashScreen
import com.aistudio.fasmovapos.app.onboarding.TermsPreferences
import com.aistudio.fasmovapos.app.onboarding.TermsScreen
import com.aistudio.fasmovapos.app.superapp.SuperFirstRun
import com.aistudio.fasmovapos.app.superapp.SuperMainShell
import com.aistudio.fasmovapos.app.superapp.SuperOwnerOnly
import com.aistudio.fasmovapos.app.ui.BackupResult
import com.aistudio.fasmovapos.app.ui.BackupViewModel
import com.aistudio.fasmovapos.app.ui.FasMovaPOSViewModel
import com.aistudio.fasmovapos.app.ui.components.DisplayControl
import com.aistudio.fasmovapos.app.ui.components.FasMovaPOSNavBar
import com.aistudio.fasmovapos.app.ui.components.FasMovaPOSTopBar
import com.aistudio.fasmovapos.app.ui.components.ReceiptModal
import com.aistudio.fasmovapos.app.ui.components.TabMenuState
import com.aistudio.fasmovapos.app.ui.components.isCompactWidth
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import com.aistudio.fasmovapos.app.ui.screens.AnalyticsScreen
import com.aistudio.fasmovapos.app.ui.screens.BackupScreen
import com.aistudio.fasmovapos.app.ui.screens.ChangePinScreen
import com.aistudio.fasmovapos.app.ui.screens.CustomersScreen
import com.aistudio.fasmovapos.app.ui.screens.ExpensesScreen
import com.aistudio.fasmovapos.app.ui.screens.InventoryScreen
import com.aistudio.fasmovapos.app.ui.screens.LoginScreen
import com.aistudio.fasmovapos.app.ui.screens.POSScreen
import com.aistudio.fasmovapos.app.ui.screens.SalesHistoryScreen
import com.aistudio.fasmovapos.app.ui.screens.StaffScreen
import com.aistudio.fasmovapos.app.ui.screens.SuppliersScreen
import com.aistudio.fasmovapos.app.ui.simple.SimpleMode
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import com.aistudio.fasmovapos.app.ui.theme.ThemeMode
import com.aistudio.fasmovapos.app.ui.theme.ThemePreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(LanguageSettings.localize(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    setContent {
      val context = LocalContext.current
      val themePrefs = remember { ThemePreferences(context.applicationContext) }
      val scope = rememberCoroutineScope()
      val themeMode by themePrefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
      val darkTheme =
        when (themeMode) {
          ThemeMode.SYSTEM -> isSystemInDarkTheme()
          ThemeMode.LIGHT -> false
          ThemeMode.DARK -> true
        }

      // Status/navigation bar icons follow the chosen theme, not just the system one.
      DisposableEffect(darkTheme) {
        enableEdgeToEdge(
          statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
          navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
        )
        onDispose {}
      }

      val changeLanguage: (AppLanguage) -> Unit = { language ->
        LanguageSettings.set(context, language)
        recreate()
      }
      FasMovaPOSTheme(darkTheme = darkTheme) {
        OnboardingGate(onLanguageChange = changeLanguage) {
          FasMovaPOSApp(
            displayControl =
              DisplayControl(
                mode = themeMode,
                onModeChange = { mode -> scope.launch { themePrefs.setThemeMode(mode) } },
                language = LanguageSettings.get(context),
                onLanguageChange = changeLanguage,
              ),
          )
        }
      }
    }
  }

  private companion object {
    val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
    val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
  }
}

@Composable
fun OnboardingGate(
  onLanguageChange: (AppLanguage) -> Unit,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val termsPrefs = remember { TermsPreferences(context.applicationContext) }
  val scope = rememberCoroutineScope()
  val accepted by termsPrefs.hasAcceptedTerms.collectAsStateWithLifecycle(initialValue = null)
  var splashDone by rememberSaveable { mutableStateOf(false) }

  when {
    !splashDone -> SplashScreen(onFinish = { splashDone = true })
    accepted == null -> AuthLoadingScreen()
    accepted == false -> TermsScreen(onAccept = { scope.launch { termsPrefs.acceptTerms() } }, onLanguageChange = onLanguageChange)
    else -> content()
  }
}

@Composable
fun FasMovaPOSApp(
  displayControl: DisplayControl,
  viewModel: FasMovaPOSViewModel = viewModel(),
) {
  val authState by viewModel.authState.collectAsStateWithLifecycle()
  val setupComplete by viewModel.isSetupComplete.collectAsStateWithLifecycle()
  val isSettingUp by viewModel.isSettingUp.collectAsStateWithLifecycle()

  // Fresh install: set up the business first. While setup runs (it signs the Owner in at the end) stay on a
  // spinner so the Login screen cannot flash between the two steps.
  if (setupComplete == null || (isSettingUp && setupComplete == true)) {
    AuthLoadingScreen(message = if (isSettingUp) R.string.setting_up_shop else null)
    return
  }
  if (setupComplete == false) {
    SuperFirstRun(isSettingUp, displayControl) { business, owner, username, pin, sample, phone, taxPin, type, demo ->
      viewModel.completeSetup(business, owner, username, pin, sample, phone, taxPin, type, demo)
    }
    return
  }

  when (val state = authState) {
    is AuthState.Loading -> AuthLoadingScreen()
    is AuthState.LoggedOut -> {
      val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
      val authError by viewModel.authError.collectAsStateWithLifecycle()
      LoginScreen(
        isLoading = isAuthLoading,
        errorMessage = authError,
        onSignIn = { username, pin -> viewModel.signIn(username, pin) },
        displayControl = displayControl,
      )
    }
    is AuthState.LoggedIn ->
      if (state.user.mustChangePin) {
        val isSavingPin by viewModel.isSavingPin.collectAsStateWithLifecycle()
        val pinProblem by viewModel.pinProblem.collectAsStateWithLifecycle()
        ChangePinScreen(
          isSaving = isSavingPin,
          problem = pinProblem,
          onSave = { viewModel.changeOwnPin(it) },
          onSignOut = { viewModel.signOut() },
        )
      } else {
        if (SimpleMode.ENABLED) {
          if (state.user.role ==
            "OWNER"
          ) {
            SuperMainShell(displayControl = displayControl, onLogout = viewModel::signOut, user = state.user)
          } else {
            SuperOwnerOnly(onLogout = viewModel::signOut)
          }
        } else {
          FasMovaPOSMainShell(viewModel = viewModel, displayControl = displayControl)
        }
      }
  }
}

@Composable
private fun AuthLoadingScreen(
  @StringRes message: Int? = null,
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
      CircularProgressIndicator()
      if (message != null) {
        Text(
          text = stringResource(message),
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun FasMovaPOSMainShell(
  viewModel: FasMovaPOSViewModel,
  displayControl: DisplayControl,
) {
  val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
  val currentBranch by viewModel.currentBranch.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

  val products by viewModel.products.collectAsStateWithLifecycle()
  val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
  val sales by viewModel.sales.collectAsStateWithLifecycle()
  val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
  val purchaseOrders by viewModel.purchaseOrders.collectAsStateWithLifecycle()
  val expenses by viewModel.expenses.collectAsStateWithLifecycle()
  val customers by viewModel.customers.collectAsStateWithLifecycle()
  val customerPayments by viewModel.customerPayments.collectAsStateWithLifecycle()
  val branches by viewModel.branches.collectAsStateWithLifecycle()
  val staffUsers by viewModel.staffUsers.collectAsStateWithLifecycle()

  val cart by viewModel.cart.collectAsStateWithLifecycle()
  val cartDiscount by viewModel.cartDiscount.collectAsStateWithLifecycle()
  val payment by viewModel.paymentState.collectAsStateWithLifecycle()
  val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
  val posSearchQuery by viewModel.posSearchQuery.collectAsStateWithLifecycle()
  val posCategoryFilter by viewModel.posCategoryFilter.collectAsStateWithLifecycle()

  val isReceiptOpen by viewModel.isReceiptOpen.collectAsStateWithLifecycle()
  val isCheckingOut by viewModel.isCheckingOut.collectAsStateWithLifecycle()
  val lastSaleEntity by viewModel.lastSaleEntity.collectAsStateWithLifecycle()
  val lastSaleItems by viewModel.lastSaleItems.collectAsStateWithLifecycle()
  val lastSalePayments by viewModel.lastSalePayments.collectAsStateWithLifecycle()
  val salePayments by viewModel.salePayments.collectAsStateWithLifecycle()

  val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(toastMessage) {
    toastMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearToast()
    }
  }

  // Phones navigate from a dropdown on the top bar; wider screens keep the bottom bar.
  val useDropdownNav = isCompactWidth()

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      FasMovaPOSTopBar(
        currentBranch = currentBranch,
        branches = branches,
        currentUser = currentUser,
        onBranchSelected = { viewModel.switchBranch(it) },
        onSaveBusinessDetails = { name, phone, taxPin -> viewModel.updateBusinessDetails(name, phone, taxPin) },
        onPinAuthenticate = { pin, callback -> viewModel.authenticateAndSwitchStaff(pin, callback) },
        onSignOut = { viewModel.signOut() },
        displayControl = displayControl,
        tabMenu =
          if (useDropdownNav) {
            TabMenuState(currentTab = currentTab, onTabSelected = { viewModel.setTab(it) })
          } else {
            null
          },
      )
    },
    bottomBar = {
      if (!useDropdownNav) {
        FasMovaPOSNavBar(
          currentTab = currentTab,
          onTabSelected = { viewModel.setTab(it) },
        )
      }
    },
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding),
    ) {
      when (currentTab) {
        "POS" ->
          POSScreen(
            products = products,
            searchQuery = posSearchQuery,
            categoryFilter = posCategoryFilter,
            cart = cart,
            discount = cartDiscount,
            payment = payment,
            selectedCustomer = selectedCustomer,
            customers = customers,
            onSearchChange = { viewModel.setPosSearch(it) },
            onCategoryChange = { viewModel.setPosCategory(it) },
            onAddToCart = { viewModel.addToCart(it) },
            onUpdateCartQty = { id, delta -> viewModel.updateCartQuantity(id, delta) },
            onRemoveFromCart = { viewModel.removeFromCart(it) },
            onClearCart = { viewModel.clearCart() },
            onUpdatePayment = { viewModel.updatePayment(it) },
            onSelectCustomer = { viewModel.selectCustomer(it) },
            onCheckout = { viewModel.checkoutSale() },
            isCheckingOut = isCheckingOut,
          )

        "INVENTORY" ->
          InventoryScreen(
            products = products,
            lowStockProducts = lowStockProducts,
            onAddProduct = { sku, bcode, name, cat, unit, cost, sell, stock, minT ->
              viewModel.addNewProduct(sku, bcode, name, cat, unit, cost, sell, stock, minT)
            },
            onAdjustStock = { id, qty, cost, sell ->
              viewModel.adjustStock(id, qty, cost, sell)
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
            onAddCustomer = { name, phone, email, limit ->
              viewModel.addNewCustomer(name, phone, email, limit)
            },
            onRecordPayment = { cust, amt, method, ref ->
              viewModel.recordCustomerPayment(cust, amt, method, ref)
            },
          )

        "ANALYTICS" ->
          AnalyticsScreen(
            sales = sales,
            salePayments = salePayments,
            expenses = expenses,
            products = products,
          )

        "STAFF" ->
          StaffScreen(
            staffUsers = staffUsers,
            currentUser = currentUser,
            onAddStaff = { uname, fname, role, pin ->
              viewModel.addNewStaffUser(uname, fname, role, pin)
            },
          )

        "BACKUP" -> BackupTab(currentUser = currentUser)
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

/** Hosts the Backup screen with its own ViewModel, and restarts the app once a restore has replaced the database. */
@Composable
internal fun BackupTab(
  currentUser: StaffUserEntity,
  includeLegacyCsv: Boolean = true,
  viewModel: BackupViewModel = viewModel(),
) {
  val context = LocalContext.current
  val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()
  val lastBackupAt by viewModel.lastBackupAt.collectAsStateWithLifecycle()
  val result by viewModel.result.collectAsStateWithLifecycle()

  // Every open repository still points at the replaced database, so a fresh process is the only safe way on.
  LaunchedEffect(result) {
    if (result == BackupResult.RESTORED) {
      delay(RESTART_DELAY_MS)
      val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
      context.startActivity(launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
      exitProcess(0)
    }
  }
  DisposableEffect(Unit) { onDispose { viewModel.consumeResult() } }

  BackupScreen(
    isOwner = currentUser.role == "OWNER",
    isWorking = isWorking,
    lastBackupAt = lastBackupAt,
    result = result,
    onCreateBackup = { uri, password -> viewModel.createBackup(uri, password) },
    onRestore = { uri, password -> viewModel.restoreBackup(uri, password) },
    onExportSales = { viewModel.exportSalesCsv(it) },
    includeLegacyCsv = includeLegacyCsv,
  )
}

private const val RESTART_DELAY_MS = 1500L
