package com.aistudio.fasmovapos.app.data.repository

import androidx.room.withTransaction
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
import com.aistudio.fasmovapos.app.data.payment.PaymentLine
import com.aistudio.fasmovapos.app.data.payment.PaymentMethod
import com.aistudio.fasmovapos.app.data.payment.SPLIT_PAYMENT_CODE
import com.aistudio.fasmovapos.app.data.payment.isValidMobileCode
import com.aistudio.fasmovapos.app.data.payment.normalizeMobileCode
import com.aistudio.fasmovapos.app.data.security.PinHasher
import com.aistudio.fasmovapos.app.data.security.PinPolicy
import com.aistudio.fasmovapos.app.data.security.PinProblem
import com.aistudio.fasmovapos.app.superapp.Livelihood
import com.aistudio.fasmovapos.app.superapp.SuperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/** The head-office branch every business gets at setup; all later data defaults to it. */
const val HQ_BRANCH_ID = "b-1"

class FasMovaPOSRepository(
  private val db: FasMovaPOSDatabase,
) {
  val products: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
  val lowStockProducts: Flow<List<ProductEntity>> = db.productDao().getLowStockProducts()
  val sales: Flow<List<SaleEntity>> = db.saleDao().getAllSales()
  val salePayments: Flow<List<SalePaymentEntity>> = db.saleDao().getAllSalePayments()
  val suppliers: Flow<List<SupplierEntity>> = db.supplierDao().getAllSuppliers()
  val purchaseOrders: Flow<List<PurchaseOrderEntity>> = db.purchaseOrderDao().getAllPurchaseOrders()
  val expenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()
  val customers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
  val customerPayments: Flow<List<CustomerPaymentEntity>> = db.customerDao().getAllCustomerPayments()
  val branches: Flow<List<BranchEntity>> = db.branchDao().getAllBranches()
  val staffUsers: Flow<List<StaffUserEntity>> = db.staffUserDao().getAllStaffUsers()
  val pendingSyncQueue: Flow<List<SyncQueueEntity>> = db.syncQueueDao().getPendingSyncItems()
  val pendingSyncCount: Flow<Int> = db.syncQueueDao().getPendingCount()

  /** False on a fresh install: no staff exist yet, so the app must run first-time setup before sign-in. */
  val isSetupComplete: Flow<Boolean> = db.staffUserDao().getAllStaffUsers().map { it.isNotEmpty() }

  /**
   * First-time setup: creates the business's head-office branch and its Owner (who chose their own PIN).
   * With [loadSampleData] it also adds training data - sample products, customers, suppliers and two staff
   * (Manager, Cashier) whose PIN is [PinPolicy.DEFAULT_PIN] until they change it on first sign-in.
   */
  suspend fun completeSetup(
    businessName: String,
    ownerName: String,
    username: String,
    ownerPin: String,
    loadSampleData: Boolean,
    businessPhone: String = "",
    taxPin: String = "",
    livelihood: Livelihood = Livelihood.RETAIL,
    demo: Boolean = false,
  ) = db.withTransaction {
    db.branchDao().insertBranches(listOf(BranchEntity(HQ_BRANCH_ID, businessName, "HQ-01", "", true, businessPhone.trim(), taxPin.trim())))
    val salt = PinHasher.generateSalt()
    db.staffUserDao().insertStaffUser(
      StaffUserEntity(
        id = "u-1",
        username = username,
        fullName = ownerName,
        role = "OWNER",
        pinHash = PinHasher.hash(ownerPin, salt),
        pinSalt = salt,
        branchId = HQ_BRANCH_ID,
        isActive = true,
      ),
    )
    SuperRepository(db).createBusiness(livelihood, businessName, demo)
    if (loadSampleData) seedSampleData()
  }

  private suspend fun seedSampleData() =
    withContext(Dispatchers.IO) {
      run {
        val managerSalt = PinHasher.generateSalt()
        val cashierSalt = PinHasher.generateSalt()
        val s2 =
          StaffUserEntity(
            "u-2",
            "manager@fasmovapos.com",
            "Sarah Kimani (Store Manager)",
            "MANAGER",
            pinHash = PinHasher.hash(PinPolicy.DEFAULT_PIN, managerSalt),
            pinSalt = managerSalt,
            branchId = HQ_BRANCH_ID,
            isActive = true,
            mustChangePin = true,
          )
        val s3 =
          StaffUserEntity(
            "u-3",
            "cashier@fasmovapos.com",
            "Dennis Otieno (Cashier)",
            "CASHIER",
            pinHash = PinHasher.hash(PinPolicy.DEFAULT_PIN, cashierSalt),
            pinSalt = cashierSalt,
            branchId = HQ_BRANCH_ID,
            isActive = true,
            mustChangePin = true,
          )
        db.staffUserDao().insertStaffUsers(listOf(s2, s3))

        db.productDao().insertProducts(SampleData.products())
        SampleData.suppliers().forEach { db.supplierDao().insertSupplier(it) }
        SampleData.customers().forEach { db.customerDao().insertCustomer(it) }
        SampleData.expenses().forEach { db.expenseDao().insertExpense(it) }
        val (sale, saleItems) = SampleData.firstSale()
        db.saleDao().insertSale(sale)
        db.saleDao().insertSaleItems(saleItems)
      }
    }

  /** A saved sale with the rows needed to show its receipt. */
  data class CompletedSale(
    val sale: SaleEntity,
    val items: List<SaleItemEntity>,
    val payments: List<SalePaymentEntity>,
  )

  /**
   * Saves a sale paid by [paymentLines] (one line normally, two for a split). The lines must
   * add up to the net total; any CREDIT line is added to [customer]'s debt.
   */
  suspend fun processSale(
    branchId: String,
    cashierId: String,
    cashierName: String,
    cartItems: List<CartItem>,
    discountAmount: Double,
    paymentLines: List<PaymentLine>,
    customer: CustomerEntity?,
  ): CompletedSale =
    db.withTransaction {
      validateCart(branchId, cartItems)
      val saleId = UUID.randomUUID().toString()
      val txRef = "FL-" + saleId.uppercase()
      val gross =
        cartItems.fold(BigDecimal.ZERO) { sum, item ->
          sum +
            retailAmount(item.lineTotal)
        }
      val discount = retailAmount(discountAmount)
      require(discount <= gross) { "Discount cannot exceed the sale total" }
      val net = gross - discount
      val grossTotal = gross.toDouble()
      val netTotal = net.toDouble()
      // Tax is not configured for this first version. Do not invent a tax rate or tax invoice.
      val taxAmount = 0.0

      require(paymentLines.isNotEmpty()) { "A sale needs at least one payment line" }
      val paid = paymentLines.fold(BigDecimal.ZERO) { sum, line -> sum + retailAmount(line.amount) }
      require(paid.compareTo(net) == 0) { "Payment lines must add up to the sale total" }
      val creditAmount =
        paymentLines
          .filter { it.method == PaymentMethod.CREDIT }
          .fold(BigDecimal.ZERO) { sum, line ->
            sum +
              retailAmount(line.amount)
          }.toDouble()
      val buyer = customer?.let { requireNotNull(db.customerDao().getCustomerById(it.id)) { "Customer no longer exists" } }
      require(creditAmount == 0.0 || buyer != null) { "A credit sale needs a customer" }
      if (creditAmount > 0.0 && buyer != null) {
        require(retailAmount(buyer.currentDebt) + retailAmount(creditAmount) <= retailAmount(buyer.creditLimit)) {
          "Customer credit limit exceeded"
        }
      }
      val referencedLines = validatedPaymentReferences(paymentLines)
      val isSplit = referencedLines.size > 1

      val sale =
        SaleEntity(
          id = saleId,
          transactionRef = txRef,
          branchId = branchId,
          cashierId = cashierId,
          cashierName = cashierName,
          totalAmount = grossTotal,
          discountAmount = discountAmount,
          taxAmount = taxAmount,
          netAmount = netTotal,
          paymentMethod = if (isSplit) SPLIT_PAYMENT_CODE else referencedLines.first().method.name,
          paymentReference = referencedLines.joinToString(" + ") { it.reference },
          customerId = buyer?.id,
          customerName = buyer?.name,
          timestamp = System.currentTimeMillis(),
          syncStatus = "LOCAL",
        )

      val itemsEntities =
        cartItems.map { item ->
          SaleItemEntity(
            saleId = saleId,
            productId = item.product.id,
            productName = item.product.name,
            quantity = item.quantity,
            unitPrice = item.pricingPrice,
            totalPrice = item.lineTotal,
            quantityScale = item.product.quantityScale,
            quantityUnit = item.product.unit,
            pricingQuantity = item.pricingQuantity,
          )
        }

      val paymentEntities =
        referencedLines.map {
          SalePaymentEntity(
            saleId = saleId,
            method = it.method.name,
            amount = it.amount,
            phoneNumber = it.phoneNumber,
            reference = it.reference,
          )
        }

      db.saleDao().insertSale(sale)
      db.saleDao().insertSaleItems(itemsEntities)
      db.saleDao().insertSalePayments(paymentEntities)

      // Decrement stock for each item
      cartItems.forEach { item ->
        db.productDao().decrementStock(item.product.id, item.quantity)
      }

      // Only the credit portion becomes debt; the whole sale counts towards what the customer spent.
      if (buyer != null) {
        db.customerDao().updateCustomerDebtAndSpent(buyer.id, creditAmount, netTotal)
      }

      // Preserve a local outbox record; no backend synchronization is configured.
      val methodsSummary = referencedLines.joinToString(" + ") { it.method.name }
      db.syncQueueDao().enqueue(
        SyncQueueEntity(
          entityName = "Sale",
          entityId = saleId,
          operation = "INSERT",
          payloadSnippet = "Sale $txRef: KES ${String.format("%.2f", netTotal)} ($methodsSummary)",
        ),
      )

      CompletedSale(sale, itemsEntities, paymentEntities)
    }

  private suspend fun validateCart(
    branchId: String,
    items: List<CartItem>,
  ) {
    require(items.isNotEmpty()) { "Cart is empty" }
    require(items.all { it.quantity > 0 }) { "Sale quantities must be positive" }
    items.groupBy { it.product.id }.forEach { (id, lines) ->
      val stored = requireNotNull(db.productDao().getProductById(id)) { "Product no longer exists" }
      require(stored.branchId == branchId) { "Product belongs to another shop" }
      require(lines.sumOf { it.quantity.toLong() } <= stored.stockQuantity.toLong()) { "Insufficient stock for " + stored.name }
      require(lines.all { retailAmount(it.product.sellingPrice).compareTo(retailAmount(stored.sellingPrice)) == 0 }) {
        "Price changed for " + stored.name + ". Remove it from the cart and add it again."
      }
      require(lines.all { it.product.quantityScale == stored.quantityScale && it.product.unit == stored.unit }) {
        "Product unit changed. Remove it from the sale and add it again."
      }
      lines.forEach { line ->
        val expectedPrice =
          when (line.pricingQuantity) {
            stored.quantityScale -> stored.sellingPrice
            250 -> stored.quarterPrice.takeIf { stored.quantityScale == 1000 && it > 0 }
            500 -> stored.halfPrice.takeIf { stored.quantityScale == 1000 && it > 0 }
            else -> null
          }
        require(expectedPrice != null && retailAmount(line.pricingPrice) == retailAmount(expectedPrice)) {
          "Portion price changed. Remove it from the sale and add it again."
        }
      }
    }
  }

  private suspend fun validatedPaymentReferences(lines: List<PaymentLine>): List<PaymentLine> {
    val mobileCodes = lines.filter { it.method.isMobileMoney }.map { normalizeMobileCode(it.reference) }
    require(mobileCodes.all { isValidMobileCode(it) }) { "Enter the mobile-money reference from the payment message" }
    require(mobileCodes.distinct().size == mobileCodes.size) { "A mobile-money reference can only be used once" }
    for (code in mobileCodes) {
      require(db.saleDao().countUsesOfReference(code) == 0 && db.customerDao().countMobileReference(code) == 0) {
        "Mobile-money reference already recorded"
      }
    }
    return lines.map {
      it.copy(reference = if (it.method.isMobileMoney) normalizeMobileCode(it.reference) else newPaymentReference(it.method))
    }
  }

  private fun retailAmount(value: Double): BigDecimal {
    require(value.isFinite() && value >= 0.0) { "Amounts must be finite and nonnegative" }
    val amount = BigDecimal.valueOf(value)
    require(amount.stripTrailingZeros().scale() <= 2) { "Amounts must have at most two decimal places" }
    return amount.setScale(2, RoundingMode.UNNECESSARY)
  }

  /** Internal reference for payments that carry no customer-supplied code (cash, card, credit). */
  private fun newPaymentReference(method: PaymentMethod): String {
    val suffix =
      UUID
        .randomUUID()
        .toString()
        .take(8)
        .uppercase()
    return "${method.name}-$suffix"
  }

  suspend fun saveProduct(product: ProductEntity) =
    db.withTransaction {
      require(product.name.isNotBlank() && product.category.isNotBlank() && product.unit.isNotBlank()) {
        "Enter the product name, category and unit"
      }
      require(product.stockQuantity >= 0 && product.minStockThreshold >= 0) { "Stock and thresholds cannot be negative" }
      require(
        product.quantityScale == 1 || (product.quantityScale == 1000 && product.unit in listOf("Kg", "Ltr")),
      ) { "Invalid product measure" }
      retailAmount(product.quarterPrice)
      retailAmount(product.halfPrice)
      val old = db.productDao().getProductById(product.id)
      require(old == null || (old.quantityScale == product.quantityScale && old.unit == product.unit)) {
        "Create a new product to change its unit"
      }
      retailAmount(product.costPrice)
      require(retailAmount(product.sellingPrice).signum() > 0) { "Selling price must be positive" }
      db.productDao().insertProduct(product)
      db.syncQueueDao().enqueue(
        SyncQueueEntity(
          entityName = "Product",
          entityId = product.id,
          operation = "UPSERT",
          payloadSnippet = "Product: ${product.name} (Stock: ${product.stockQuantity})",
        ),
      )
    }

  suspend fun setFavorite(
    productId: String,
    favorite: Boolean,
  ) = db.withTransaction {
    val product = requireNotNull(db.productDao().getProductById(productId))
    db.productDao().updateProduct(product.copy(isFavorite = favorite))
  }

  suspend fun adjustProductStock(
    productId: String,
    changeAmount: Int,
    costPrice: Double?,
    sellingPrice: Double?,
    quarterPrice: Double? = null,
    halfPrice: Double? = null,
    reason: String = "Stock or price adjustment",
  ) = db.withTransaction {
    require(reason.isNotBlank()) { "Enter a reason for the change" }
    val existing = requireNotNull(db.productDao().getProductById(productId)) { "Product no longer exists" }
    val newStock = existing.stockQuantity.toLong() + changeAmount
    require(newStock in 0..Int.MAX_VALUE.toLong()) { "Adjustment would put stock outside the valid range" }
    quarterPrice?.let { retailAmount(it) }
    halfPrice?.let { retailAmount(it) }
    costPrice?.let { retailAmount(it) }
    sellingPrice?.let { require(retailAmount(it).signum() > 0) { "Selling price must be positive" } }
    val updated =
      existing.copy(
        stockQuantity = newStock.toInt(),
        costPrice = costPrice ?: existing.costPrice,
        sellingPrice = sellingPrice ?: existing.sellingPrice,
        quarterPrice = quarterPrice ?: existing.quarterPrice,
        halfPrice = halfPrice ?: existing.halfPrice,
        updatedAt = System.currentTimeMillis(),
      )
    db.recordingDao().putMeta(
      com.aistudio.fasmovapos.app.data.records
        .RecordingMeta("audit_reason", reason.trim()),
    )
    db.productDao().updateProduct(updated)
    db.recordingDao().removeMeta("audit_reason")
    db.syncQueueDao().enqueue(
      SyncQueueEntity(
        entityName = "ProductStock",
        entityId = productId,
        operation = "UPDATE",
        payloadSnippet = "Stock Adjust ${existing.name}: ${existing.stockQuantity} -> $newStock",
      ),
    )
  }

  suspend fun addExpense(expense: ExpenseEntity) =
    db.withTransaction {
      require(expense.category.isNotBlank() && retailAmount(expense.amount).signum() > 0) { "Enter a valid expense" }
      db.expenseDao().insertExpense(expense)
      db.syncQueueDao().enqueue(
        SyncQueueEntity(
          entityName = "Expense",
          entityId = expense.id,
          operation = "INSERT",
          payloadSnippet = "Expense ${expense.category}: KES ${expense.amount}",
        ),
      )
    }

  suspend fun addCustomer(customer: CustomerEntity) =
    db.withTransaction {
      require(customer.name.isNotBlank()) { "Enter the customer name" }
      retailAmount(customer.creditLimit)
      retailAmount(customer.currentDebt)
      db.customerDao().insertCustomer(customer)
      db.syncQueueDao().enqueue(
        SyncQueueEntity(
          entityName = "Customer",
          entityId = customer.id,
          operation = "INSERT",
          payloadSnippet = "Customer: ${customer.name}",
        ),
      )
    }

  suspend fun recordCustomerPayment(payment: CustomerPaymentEntity) =
    db.withTransaction {
      val customer = requireNotNull(db.customerDao().getCustomerById(payment.customerId)) { "Customer no longer exists" }
      val amount = retailAmount(payment.amountPaid)
      require(amount.signum() > 0) { "Repayment must be positive" }
      require(amount <= retailAmount(customer.currentDebt)) { "Repayment exceeds the outstanding debt" }
      require(db.customerDao().getPaymentById(payment.id) == null) { "Repayment already recorded" }
      require(payment.paymentMethod in listOf("CASH", "MPESA", "AIRTEL", "BANK")) { "Choose a payment method" }
      val reference =
        if (payment.paymentMethod in listOf("MPESA", "AIRTEL")) {
          val code = normalizeMobileCode(payment.reference)
          require(isValidMobileCode(code)) { "Enter the payment reference" }
          require(db.saleDao().countUsesOfReference(code) == 0 && db.customerDao().countMobileReference(code) == 0) {
            "Payment reference already recorded"
          }
          code
        } else {
          payment.reference
        }
      db.customerDao().insertPayment(payment.copy(reference = reference, customerName = customer.name))
      val newDebt = (retailAmount(customer.currentDebt) - amount).toDouble()
      db.customerDao().updateCustomer(customer.copy(currentDebt = newDebt))
      db.syncQueueDao().enqueue(
        SyncQueueEntity(
          entityName = "CustomerPayment",
          entityId = payment.id,
          operation = "INSERT",
          payloadSnippet = "Payment from ${payment.customerName}: KES ${payment.amountPaid}",
        ),
      )
    }

  suspend fun addSupplier(supplier: SupplierEntity) =
    withContext(Dispatchers.IO) {
      db.supplierDao().insertSupplier(supplier)
      db.syncQueueDao().enqueue(
        SyncQueueEntity(
          entityName = "Supplier",
          entityId = supplier.id,
          operation = "INSERT",
          payloadSnippet = "Supplier: ${supplier.name}",
        ),
      )
    }

  suspend fun createPurchaseOrder(po: PurchaseOrderEntity) =
    withContext(Dispatchers.IO) {
      db.purchaseOrderDao().insertPurchaseOrder(po)
      db.syncQueueDao().enqueue(
        SyncQueueEntity(
          entityName = "PurchaseOrder",
          entityId = po.id,
          operation = "INSERT",
          payloadSnippet = "PO ${po.poNumber}: KES ${po.totalAmount}",
        ),
      )
    }

  suspend fun addStaffUser(
    username: String,
    fullName: String,
    role: String,
    plainPin: String,
    branchId: String,
  ) = withContext(Dispatchers.IO) {
    val salt = PinHasher.generateSalt()
    val user =
      StaffUserEntity(
        id = "u-" + UUID.randomUUID().toString().take(8),
        username = username,
        fullName = fullName,
        role = role,
        pinHash = PinHasher.hash(plainPin, salt),
        pinSalt = salt,
        branchId = branchId,
        mustChangePin = true, // the owner chose this PIN for them, so they set their own at first sign-in
      )
    db.staffUserDao().insertStaffUser(user)
  }

  /** App-level sign-in: identifies the staff account by username, then verifies the PIN hash. */
  suspend fun signIn(
    username: String,
    plainPin: String,
  ): StaffUserEntity? =
    withContext(Dispatchers.IO) {
      val user = db.staffUserDao().getUserByUsername(username) ?: return@withContext null
      if (PinHasher.verify(plainPin, user.pinHash, user.pinSalt)) user else null
    }

  suspend fun getStaffUserById(id: String): StaffUserEntity? =
    withContext(Dispatchers.IO) {
      db
        .staffUserDao()
        .getAllStaffUsers()
        .first()
        .find { it.id == id }
    }

  /**
   * Quick cashier switch: PIN alone, scoped to the active branch (small staff list, hash-checked in app code).
   * Staff still on a temporary PIN are skipped, and a PIN that matches more than one person identifies nobody,
   * so a shared PIN can never log someone in as somebody else.
   */
  suspend fun verifyStaffPinForBranch(
    branchId: String,
    plainPin: String,
  ): StaffUserEntity? =
    withContext(Dispatchers.IO) {
      db
        .staffUserDao()
        .getActiveStaffForBranch(branchId)
        .filter { !it.mustChangePin && PinHasher.verify(plainPin, it.pinHash, it.pinSalt) }
        .singleOrNull()
    }

  /**
   * Replaces the PIN of [userId] with [newPin] and clears the "must change" flag. Returns why it was refused,
   * or null on success. A PIN is refused if it is malformed, the public default, the current PIN, or already
   * used by another active person (the quick switch identifies people by PIN alone).
   */
  suspend fun changePin(
    userId: String,
    newPin: String,
  ): PinProblem? =
    withContext(Dispatchers.IO) {
      PinPolicy.formatProblem(newPin)?.let { return@withContext it }
      val user = db.staffUserDao().getUserById(userId) ?: return@withContext PinProblem.NOT_FOUR_DIGITS
      if (PinHasher.verify(newPin, user.pinHash, user.pinSalt)) {
        return@withContext PinProblem.SAME_AS_CURRENT
      }
      val takenByOther =
        db.staffUserDao().getActiveStaff().any { it.id != userId && PinHasher.verify(newPin, it.pinHash, it.pinSalt) }
      if (takenByOther) return@withContext PinProblem.IN_USE
      val salt = PinHasher.generateSalt()
      db.staffUserDao().updateStaffUser(
        user.copy(pinHash = PinHasher.hash(newPin, salt), pinSalt = salt, mustChangePin = false),
      )
      null
    }

  /** True if a mobile-money confirmation code has already paid for a sale, so one SMS cannot pay twice. */
  suspend fun isMobileMoneyCodeUsed(code: String): Boolean =
    withContext(Dispatchers.IO) {
      db.saleDao().countUsesOfReference(code) > 0 ||
        db.customerDao().countMobileReference(code) > 0
    }

  suspend fun getSalePayments(saleId: String): List<SalePaymentEntity> =
    withContext(Dispatchers.IO) { db.saleDao().getSalePayments(saleId) }

  /** Updates the business's name and the optional contact details printed on receipts. */
  suspend fun updateBusinessDetails(
    branchId: String,
    name: String,
    phone: String,
    taxPin: String,
  ) = withContext(Dispatchers.IO) {
    val branch =
      db
        .branchDao()
        .getAllBranches()
        .first()
        .find { it.id == branchId } ?: return@withContext
    db.branchDao().updateBranch(branch.copy(name = name.trim(), phone = phone.trim(), taxPin = taxPin.trim()))
  }

  suspend fun getSaleItems(saleId: String): List<SaleItemEntity> =
    withContext(Dispatchers.IO) {
      db.saleDao().getSaleItems(saleId)
    }
}

data class CartItem(
  val product: ProductEntity,
  var quantity: Int,
  val pricingQuantity: Int = product.quantityScale,
  val pricingPrice: Double = product.sellingPrice,
) {
  val lineKey: String get() = "${product.id}:$pricingQuantity:$pricingPrice"
  val lineTotal: Double get() = RetailQuantity.amount(pricingPrice, quantity, pricingQuantity)
  val quantityLabel: String get() = RetailQuantity.label(quantity, product.quantityScale, product.unit)
}
