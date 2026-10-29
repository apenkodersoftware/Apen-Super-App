package com.aistudio.fasmovapos.app.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.auth.AuthRepository
import com.aistudio.fasmovapos.app.auth.AuthRepositoryProvider
import com.aistudio.fasmovapos.app.auth.AuthState
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity
import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.CustomerPaymentEntity
import com.aistudio.fasmovapos.app.data.db.entities.ExpenseEntity
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.db.entities.PurchaseOrderEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleItemEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import com.aistudio.fasmovapos.app.data.db.entities.StaffUserEntity
import com.aistudio.fasmovapos.app.data.db.entities.SupplierEntity
import com.aistudio.fasmovapos.app.data.db.entities.SyncQueueEntity
import com.aistudio.fasmovapos.app.data.payment.ConfirmationResult
import com.aistudio.fasmovapos.app.data.payment.ManualMobileMoneyConfirmer
import com.aistudio.fasmovapos.app.data.payment.MobileMoneyConfirmer
import com.aistudio.fasmovapos.app.data.payment.PaymentState
import com.aistudio.fasmovapos.app.data.repository.CartItem
import com.aistudio.fasmovapos.app.data.repository.FasMovaPOSRepository
import com.aistudio.fasmovapos.app.data.repository.HQ_BRANCH_ID
import com.aistudio.fasmovapos.app.data.repository.ProductMeasure
import com.aistudio.fasmovapos.app.data.security.PinProblem
import com.aistudio.fasmovapos.app.superapp.Livelihood
import com.aistudio.fasmovapos.app.ui.components.roleLabelRes
import com.aistudio.fasmovapos.app.ui.components.text
import com.aistudio.fasmovapos.app.ui.language.LanguageSettings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class FasMovaPOSViewModel(
  application: Application,
) : AndroidViewModel(application) {
  private val repository: FasMovaPOSRepository
  private val authRepository: AuthRepository

  // A failed save or lookup must never take the app down mid-sale: say so plainly and let the user retry.
  private val failureHandler =
    CoroutineExceptionHandler { _, _ -> showToast(str(R.string.something_went_wrong_try_again)) }

  init {
    val db = FasMovaPOSDatabase.getDatabase(application)
    repository = FasMovaPOSRepository(db)
    authRepository = AuthRepositoryProvider.create(application, repository)
    viewModelScope.launch(failureHandler) {
      authRepository.restoreSession()
    }
  }

  val authState: StateFlow<AuthState> = authRepository.authState

  private val recordingDao = FasMovaPOSDatabase.getDatabase(application).recordingDao()
  val recordingEvents = recordingDao.events().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val recordOrigins = recordingDao.origins().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val recordedDays = recordingDao.days().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun setNoTradeDay(
    date: String,
    closed: Boolean,
  ) = launchWrite {
    recordingDao.saveDay(
      com.aistudio.fasmovapos.app.data.records
        .RecordedDay(date, closed),
    )
  }

  /** Null while loading; false on a fresh install, which must run first-time setup before anyone can sign in. */
  val isSetupComplete: StateFlow<Boolean?> =
    repository.isSetupComplete
      .map<Boolean, Boolean?> { it }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  private val setupFlight = SingleFlight()
  val isSettingUp: StateFlow<Boolean> = setupFlight.isRunning

  /** Creates the business and its Owner, then signs the Owner straight in. */
  fun completeSetup(
    businessName: String,
    ownerName: String,
    username: String,
    pin: String,
    loadSampleData: Boolean,
    businessPhone: String = "",
    taxPin: String = "",
    livelihood: Livelihood = Livelihood.RETAIL,
    demo: Boolean = false,
  ) {
    viewModelScope.launch(failureHandler) {
      setupFlight.runIfIdle {
        repository.completeSetup(businessName, ownerName, username, pin, loadSampleData, businessPhone, taxPin, livelihood, demo)
        authRepository.signIn(username, pin)
      }
    }
  }

  /** Owner edits the business name and the optional phone and KRA PIN printed on receipts. */
  fun updateBusinessDetails(
    name: String,
    phone: String,
    taxPin: String,
  ) = launchWrite { repository.updateBusinessDetails(_currentBranch.value.id, name, phone, taxPin) }

  /** Reopens the receipt of an earlier sale, from the Sales list. */
  fun showReceipt(sale: SaleEntity) {
    viewModelScope.launch(failureHandler) {
      _lastSaleItems.value = repository.getSaleItems(sale.id)
      _lastSalePayments.value = repository.getSalePayments(sale.id)
      _lastSaleEntity.value = sale
      _isReceiptOpen.value = true
    }
  }

  private val pinFlight = SingleFlight()
  val isSavingPin: StateFlow<Boolean> = pinFlight.isRunning

  private val _pinProblem = MutableStateFlow<PinProblem?>(null)
  val pinProblem: StateFlow<PinProblem?> = _pinProblem.asStateFlow()

  /** Replaces the signed-in user's temporary PIN; on success the session reloads and the app opens. */
  fun changeOwnPin(newPin: String) {
    viewModelScope.launch(failureHandler) {
      pinFlight.runIfIdle {
        _pinProblem.value = null
        val problem = repository.changePin(_currentUser.value.id, newPin)
        _pinProblem.value = problem
        // restoreSession re-reads the user, so the "must change PIN" gate sees the cleared flag.
        if (problem == null) authRepository.restoreSession()
      }
    }
  }

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  private val _isAuthLoading = MutableStateFlow(false)
  val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

  fun signIn(
    username: String,
    pin: String,
  ) {
    viewModelScope.launch(failureHandler) {
      _isAuthLoading.value = true
      _authError.value = null
      val result = authRepository.signIn(username, pin)
      result
        .onSuccess {
          val loggedInUser = (authState.value as? AuthState.LoggedIn)?.user
          if (loggedInUser != null) {
            _currentUser.value = loggedInUser
          }
        }.onFailure {
          _authError.value = str(R.string.invalid_username_or_pin)
        }
      _isAuthLoading.value = false
    }
  }

  fun signOut() {
    _pinProblem.value = null
    viewModelScope.launch(failureHandler) {
      authRepository.signOut()
    }
  }

  // Database Flows
  val products: StateFlow<List<ProductEntity>> =
    repository.products
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val lowStockProducts: StateFlow<List<ProductEntity>> =
    repository.lowStockProducts
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val salePayments: StateFlow<List<SalePaymentEntity>> =
    repository.salePayments
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val sales: StateFlow<List<SaleEntity>> =
    repository.sales
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val suppliers: StateFlow<List<SupplierEntity>> =
    repository.suppliers
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val purchaseOrders: StateFlow<List<PurchaseOrderEntity>> =
    repository.purchaseOrders
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val expenses: StateFlow<List<ExpenseEntity>> =
    repository.expenses
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val customers: StateFlow<List<CustomerEntity>> =
    repository.customers
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val customerPayments: StateFlow<List<CustomerPaymentEntity>> =
    repository.customerPayments
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val branches: StateFlow<List<BranchEntity>> =
    repository.branches
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val staffUsers: StateFlow<List<StaffUserEntity>> =
    repository.staffUsers
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val pendingSyncQueue: StateFlow<List<SyncQueueEntity>> =
    repository.pendingSyncQueue
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val pendingSyncCount: StateFlow<Int> =
    repository.pendingSyncCount
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  // Active Navigation & Session States
  private val _currentTab = MutableStateFlow("POS")
  val currentTab: StateFlow<String> = _currentTab.asStateFlow()

  private val _currentBranch =
    MutableStateFlow(
      BranchEntity(HQ_BRANCH_ID, "", "HQ-01", "", true),
    )
  val currentBranch: StateFlow<BranchEntity> = _currentBranch.asStateFlow()

  private val _currentUser =
    MutableStateFlow(
      StaffUserEntity(
        id = "placeholder",
        username = "",
        fullName = "",
        role = "CASHIER",
        pinHash = "",
        pinSalt = "",
        branchId = "b-1",
        isActive = false,
      ),
    )
  val currentUser: StateFlow<StaffUserEntity> = _currentUser.asStateFlow()

  init {
    // Follow the stored branches, so the name entered at setup (or later edits) shows in the top bar.
    viewModelScope.launch(failureHandler) {
      repository.branches.collect { all ->
        val active = all.find { it.id == _currentBranch.value.id } ?: all.find { it.isHeadquarters }
        if (active != null) _currentBranch.value = active
      }
    }
  }

  init {
    // A restored session never goes through signIn(), so adopt the logged-in user from auth state.
    viewModelScope.launch(failureHandler) {
      authState.collect { state ->
        (state as? AuthState.LoggedIn)?.let { _currentUser.value = it.user }
      }
    }
  }

  // POS Cart & Checkout State
  private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
  val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

  // Discount mode/value and every payment field the cashier fills in at checkout.
  private val _paymentState = MutableStateFlow(PaymentState())
  val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

  /** Discount in KES, derived so a percentage discount follows the cart as items change. */
  val cartDiscount: StateFlow<Double> =
    combine(_cart, _paymentState) { cart, payment ->
      payment.discountAmount(cart.sumOf { it.lineTotal })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

  private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
  val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

  private val _posSearchQuery = MutableStateFlow("")
  val posSearchQuery: StateFlow<String> = _posSearchQuery.asStateFlow()

  private val _posCategoryFilter = MutableStateFlow("__FAVOURITES__")
  val posCategoryFilter: StateFlow<String> = _posCategoryFilter.asStateFlow()

  // Receipt Modal State
  private val _isReceiptOpen = MutableStateFlow(false)
  val isReceiptOpen: StateFlow<Boolean> = _isReceiptOpen.asStateFlow()

  private val _lastSaleRef = MutableStateFlow("")
  val lastSaleRef: StateFlow<String> = _lastSaleRef.asStateFlow()

  private val _lastSaleItems = MutableStateFlow<List<SaleItemEntity>>(emptyList())
  val lastSaleItems: StateFlow<List<SaleItemEntity>> = _lastSaleItems.asStateFlow()

  private val _lastSalePayments = MutableStateFlow<List<SalePaymentEntity>>(emptyList())
  val lastSalePayments: StateFlow<List<SalePaymentEntity>> = _lastSalePayments.asStateFlow()

  private val _lastSaleEntity = MutableStateFlow<SaleEntity?>(null)
  val lastSaleEntity: StateFlow<SaleEntity?> = _lastSaleEntity.asStateFlow()

  // Toast / Notification
  private val _toastMessage = MutableStateFlow<String?>(null)
  val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

  // Filtered POS Products
  val filteredPosProducts: StateFlow<List<ProductEntity>> =
    combine(
      products,
      posSearchQuery,
      posCategoryFilter,
    ) { list, query, category ->
      list.filter { prod ->
        val matchesQuery =
          query.isBlank() ||
            prod.name.contains(query, ignoreCase = true) ||
            prod.sku.contains(query, ignoreCase = true) ||
            prod.barcode.contains(query, ignoreCase = true)
        val matchesCategory = category == "All" || prod.category.equals(category, ignoreCase = true)
        matchesQuery && matchesCategory
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Actions
  fun setTab(tab: String) {
    _currentTab.value = tab
  }

  fun setPosSearch(query: String) {
    _posSearchQuery.value = query
  }

  fun setPosCategory(category: String) {
    _posCategoryFilter.value = category
  }

  fun addToCart(product: ProductEntity) = addMeasuredToCart(CartItem(product, product.quantityScale))

  fun addMeasuredToCart(item: CartItem) {
    val held = _cart.value.filter { it.product.id == item.product.id }.sumOf { it.quantity.toLong() }
    if (item.quantity <= 0 || held + item.quantity > item.product.stockQuantity || item.lineTotal <= 0) {
      showToast(str(R.string.maximum_available_stock_reached))
      return
    }
    val current = _cart.value.toMutableList()
    val index = current.indexOfFirst { it.lineKey == item.lineKey }
    if (index <
      0
    ) {
      current.add(item)
    } else {
      current[index] = current[index].copy(quantity = Math.addExact(current[index].quantity, item.quantity))
    }
    _cart.value = current
  }

  fun updateCartQuantity(
    productId: String,
    delta: Int,
  ) {
    val current = _cart.value.toMutableList()
    val index = current.indexOfFirst { it.lineKey == productId || it.product.id == productId }
    if (index < 0) return
    val item = current[index]
    val next = item.quantity.toLong() + delta
    val other = current.filterIndexed { i, line -> i != index && line.product.id == item.product.id }.sumOf { it.quantity.toLong() }
    when {
      next <= 0 -> current.removeAt(index)
      next + other > item.product.stockQuantity -> showToast(str(R.string.maximum_available_stock_reached))
      else -> current[index] = item.copy(quantity = next.toInt())
    }
    _cart.value = current
  }

  fun removeFromCart(productId: String) {
    _cart.value = _cart.value.filter { it.lineKey != productId && it.product.id != productId }
  }

  fun clearCart() {
    _cart.value = emptyList()
    _paymentState.value = PaymentState()
    _selectedCustomer.value = null
  }

  /** Applies [transform] to the payment panel state (discount, method, split, phone numbers). */
  fun updatePayment(transform: (PaymentState) -> PaymentState) {
    _paymentState.update(transform)
  }

  fun selectCustomer(customer: CustomerEntity?) {
    _selectedCustomer.value = customer
  }

  private val checkoutFlight = SingleFlight()

  // Manual today; a Safaricom / Airtel integration plugs in here without touching checkout.
  private val mobileMoneyConfirmer: MobileMoneyConfirmer = ManualMobileMoneyConfirmer { repository.isMobileMoneyCodeUsed(it) }
  private val writeFlight = SingleFlight()

  /** True while a checkout is being saved; the pay button is disabled meanwhile. */
  val isCheckingOut: StateFlow<Boolean> = checkoutFlight.isRunning

  /** Runs a data write once at a time, so a double tap on Save cannot create the record twice. */
  private fun launchWrite(block: suspend () -> Unit) {
    viewModelScope.launch(failureHandler) { writeFlight.runIfIdle(block) }
  }

  fun checkoutSale() {
    val currentCart = _cart.value
    if (currentCart.isEmpty()) {
      showToast(str(R.string.cart_is_empty))
      return
    }
    val cust = _selectedCustomer.value
    val payment = _paymentState.value
    val gross = currentCart.sumOf { it.lineTotal }
    val discount = payment.discountAmount(gross)
    val net = (gross - discount).coerceAtLeast(0.0)

    payment.validationError(net, hasCustomer = cust != null)?.let { issue ->
      showToast(issue.text(LanguageSettings.localize(getApplication())))
      return
    }

    // Main.immediate runs this up to the first suspension synchronously, so the guard is taken on the tap
    // itself. The cart only clears after processSale returns; without the guard a second tap would sell it again.
    viewModelScope.launch(failureHandler) {
      checkoutFlight.runIfIdle {
        val lines = payment.lines(net)
        // Each mobile-money leg must be confirmed (today: its SMS code is not one that already paid for another sale).
        val rejected =
          lines.filter { it.method.isMobileMoney }.firstOrNull {
            mobileMoneyConfirmer.confirm(
              it,
            ) is ConfirmationResult.Rejected
          }
        if (rejected != null) {
          showToast(str(R.string.payment_issue_code_used, rejected.reference))
          return@runIfIdle
        }
        val completed =
          repository.processSale(
            branchId = _currentBranch.value.id,
            cashierId = _currentUser.value.id,
            cashierName = _currentUser.value.fullName,
            cartItems = currentCart,
            discountAmount = discount,
            paymentLines = lines,
            customer = cust,
          )

        _lastSaleRef.value = completed.sale.transactionRef
        _lastSaleItems.value = completed.items
        _lastSalePayments.value = completed.payments
        _lastSaleEntity.value = completed.sale
        _isReceiptOpen.value = true

        // Clear Cart
        clearCart()
        showToast(str(R.string.sale_completed_successfully_ref, completed.sale.transactionRef))
      }
    }
  }

  fun dismissReceipt() {
    _isReceiptOpen.value = false
  }

  fun addNewProduct(
    sku: String,
    barcode: String,
    name: String,
    category: String,
    unit: String,
    costPrice: Double,
    sellingPrice: Double,
    stockQty: Int,
    minThreshold: Int,
  ) = addMeasuredProduct(sku, name, category, costPrice, sellingPrice, stockQty, minThreshold, ProductMeasure(unit = unit), barcode)

  fun addMeasuredProduct(
    sku: String,
    name: String,
    category: String,
    costPrice: Double,
    sellingPrice: Double,
    stockQty: Int,
    minThreshold: Int,
    measure: ProductMeasure,
    barcode: String = "",
  ) {
    launchWrite {
      val p =
        ProductEntity(
          id = "p-" + UUID.randomUUID().toString().take(8),
          sku = sku.ifBlank { "SKU-" + System.currentTimeMillis().toString().takeLast(6) },
          barcode = barcode.ifBlank { System.currentTimeMillis().toString() },
          name = name,
          category = category,
          unit = measure.unit,
          costPrice = costPrice,
          sellingPrice = sellingPrice,
          stockQuantity = stockQty,
          minStockThreshold = minThreshold,
          quantityScale = measure.scale,
          quarterPrice = measure.quarterPrice,
          halfPrice = measure.halfPrice,
          isFavorite = products.value.count { it.isFavorite } < 12,
          branchId = _currentBranch.value.id,
        )
      repository.saveProduct(p)
      showToast(str(R.string.product_added_successfully, name))
    }
  }

  fun setFavorite(
    productId: String,
    favorite: Boolean,
  ) = launchWrite { repository.setFavorite(productId, favorite) }

  fun adjustStock(
    productId: String,
    changeQty: Int,
    costPrice: Double? = null,
    sellingPrice: Double? = null,
    quarterPrice: Double? = null,
    halfPrice: Double? = null,
    reason: String = "Stock or price adjustment",
  ) {
    launchWrite {
      repository.adjustProductStock(productId, changeQty, costPrice, sellingPrice, quarterPrice, halfPrice, reason)
      showToast(str(R.string.stock_updated))
    }
  }

  fun addNewExpense(
    category: String,
    amount: Double,
    description: String,
  ) {
    launchWrite {
      val exp =
        ExpenseEntity(
          id = "exp-" + UUID.randomUUID().toString().take(8),
          expenseRef = "EXP-" + System.currentTimeMillis().toString().takeLast(6),
          branchId = _currentBranch.value.id,
          category = category,
          amount = amount,
          description = description,
          recordedBy = _currentUser.value.fullName,
        )
      repository.addExpense(exp)
      showToast(str(R.string.expense_logged_kes, String.format("%.2f", amount)))
    }
  }

  fun addNewCustomer(
    name: String,
    phone: String,
    email: String,
    creditLimit: Double,
  ) {
    launchWrite {
      val cust =
        CustomerEntity(
          id = "c-" + UUID.randomUUID().toString().take(8),
          name = name,
          phone = phone,
          email = email,
          creditLimit = creditLimit,
        )
      repository.addCustomer(cust)
      showToast(str(R.string.customer_added, name))
    }
  }

  fun recordCustomerPayment(
    customer: CustomerEntity,
    amount: Double,
    method: String,
    ref: String,
  ) {
    launchWrite {
      val p =
        CustomerPaymentEntity(
          id = "pay-" + UUID.randomUUID().toString().take(8),
          customerId = customer.id,
          customerName = customer.name,
          amountPaid = amount,
          paymentMethod = method,
          reference = ref.ifBlank { "REF-" + System.currentTimeMillis().toString().takeLast(6) },
        )
      repository.recordCustomerPayment(p)
      showToast(str(R.string.payment_kes_recorded_for, String.format("%.2f", amount), customer.name))
    }
  }

  fun addNewSupplier(
    name: String,
    phone: String,
    email: String,
    contactPerson: String,
    address: String,
  ) {
    launchWrite {
      val sup =
        SupplierEntity(
          id = "sup-" + UUID.randomUUID().toString().take(8),
          name = name,
          phone = phone,
          email = email,
          contactPerson = contactPerson,
          address = address,
        )
      repository.addSupplier(sup)
      showToast(str(R.string.supplier_registered, name))
    }
  }

  fun createPurchaseOrder(
    supplier: SupplierEntity,
    totalAmount: Double,
  ) {
    launchWrite {
      val po =
        PurchaseOrderEntity(
          id = "po-" + UUID.randomUUID().toString().take(8),
          poNumber = "PO-" + System.currentTimeMillis().toString().takeLast(6),
          supplierId = supplier.id,
          supplierName = supplier.name,
          branchId = _currentBranch.value.id,
          totalAmount = totalAmount,
        )
      repository.createPurchaseOrder(po)
      showToast(str(R.string.purchase_order_created, po.poNumber))
    }
  }

  fun addNewStaffUser(
    username: String,
    fullName: String,
    role: String,
    pin: String,
  ) {
    launchWrite {
      repository.addStaffUser(username, fullName, role, pin, _currentBranch.value.id)
      showToast(str(R.string.staff_user_registered, fullName))
    }
  }

  fun switchBranch(branch: BranchEntity) {
    _currentBranch.value = branch
    showToast(str(R.string.switched_active_branch_to, branch.name))
  }

  fun authenticateAndSwitchStaff(
    pin: String,
    onResult: (Boolean) -> Unit,
  ) {
    viewModelScope.launch(failureHandler) {
      val user = repository.verifyStaffPinForBranch(_currentBranch.value.id, pin)
      if (user != null) {
        _currentUser.value = user
        val roleName = str(roleLabelRes(user.role))
        showToast(str(R.string.logged_in_as, user.fullName, roleName))
        onResult(true)
      } else {
        showToast(str(R.string.invalid_security_pin))
        onResult(false)
      }
    }
  }

  /** A string in the language the user picked (the Application context is not localized by itself). */
  private fun str(
    @StringRes id: Int,
    vararg args: Any,
  ): String = LanguageSettings.localize(getApplication()).getString(id, *args)

  private fun showToast(msg: String) {
    _toastMessage.value = msg
  }

  fun clearToast() {
    _toastMessage.value = null
  }
}
