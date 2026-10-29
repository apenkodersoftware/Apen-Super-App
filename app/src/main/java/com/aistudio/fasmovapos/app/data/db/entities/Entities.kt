package com.aistudio.fasmovapos.app.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
  @PrimaryKey val id: String,
  val sku: String,
  val barcode: String,
  val name: String,
  val category: String,
  val unit: String, // e.g., Pcs, Kg, Ltr, Box, Bag
  val costPrice: Double,
  val sellingPrice: Double,
  val stockQuantity: Int,
  val minStockThreshold: Int,
  val branchId: String,
  val isSynced: Boolean = false,
  val updatedAt: Long = System.currentTimeMillis(),
  @ColumnInfo(defaultValue = "1") val quantityScale: Int = 1,
  @ColumnInfo(defaultValue = "0") val quarterPrice: Double = 0.0,
  @ColumnInfo(defaultValue = "0") val halfPrice: Double = 0.0,
  @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
)

@Entity(tableName = "sales")
data class SaleEntity(
  @PrimaryKey val id: String,
  val transactionRef: String,
  val branchId: String,
  val cashierId: String,
  val cashierName: String,
  val totalAmount: Double,
  val discountAmount: Double,
  val taxAmount: Double,
  val netAmount: Double,
  val paymentMethod: String, // CASH, MPESA, CARD, CREDIT
  val paymentReference: String,
  val customerId: String?,
  val customerName: String?,
  val timestamp: Long = System.currentTimeMillis(),
  val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val saleId: String,
  val productId: String,
  val productName: String,
  val quantity: Int,
  val unitPrice: Double,
  val totalPrice: Double,
  @ColumnInfo(defaultValue = "1") val quantityScale: Int = 1,
  @ColumnInfo(defaultValue = "'Pcs'") val quantityUnit: String = "Pcs",
  @ColumnInfo(defaultValue = "1") val pricingQuantity: Int = 1,
)

/** One payment leg of a sale. Sales made before split payments existed have no rows here. */
@Entity(tableName = "sale_payments", indices = [Index("saleId")])
data class SalePaymentEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val saleId: String,
  val method: String, // CASH, MPESA, AIRTEL, CARD, CREDIT
  val amount: Double,
  val phoneNumber: String? = null,
  val reference: String = "",
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
  @PrimaryKey val id: String,
  val name: String,
  val phone: String,
  val email: String,
  val contactPerson: String,
  val address: String,
  val balanceOwed: Double = 0.0,
  val isSynced: Boolean = false,
)

@Entity(tableName = "purchase_orders")
data class PurchaseOrderEntity(
  @PrimaryKey val id: String,
  val poNumber: String,
  val supplierId: String,
  val supplierName: String,
  val branchId: String,
  val totalAmount: Double,
  val status: String = "RECEIVED", // PENDING, RECEIVED, CANCELLED
  val timestamp: Long = System.currentTimeMillis(),
  val isSynced: Boolean = false,
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
  @PrimaryKey val id: String,
  val expenseRef: String,
  val branchId: String,
  val category: String, // Rent, Wages, Utilities, Transport, Stock Freight, Taxes, Miscellaneous
  val amount: Double,
  val description: String,
  val recordedBy: String,
  val timestamp: Long = System.currentTimeMillis(),
  val isSynced: Boolean = false,
)

@Entity(tableName = "customers")
data class CustomerEntity(
  @PrimaryKey val id: String,
  val name: String,
  val phone: String,
  val email: String,
  val creditLimit: Double = 50000.0,
  val currentDebt: Double = 0.0,
  val totalSpent: Double = 0.0,
  val isSynced: Boolean = false,
)

@Entity(tableName = "customer_payments")
data class CustomerPaymentEntity(
  @PrimaryKey val id: String,
  val customerId: String,
  val customerName: String,
  val amountPaid: Double,
  val paymentMethod: String,
  val reference: String,
  val timestamp: Long = System.currentTimeMillis(),
  val isSynced: Boolean = false,
)

@Entity(tableName = "branches")
data class BranchEntity(
  @PrimaryKey val id: String,
  val name: String,
  val code: String,
  val location: String,
  val isHeadquarters: Boolean = false,
  // Optional contact details printed on receipts; empty means "not entered", and nothing is printed.
  @ColumnInfo(defaultValue = "''") val phone: String = "",
  @ColumnInfo(defaultValue = "''") val taxPin: String = "",
)

@Entity(tableName = "staff_users")
data class StaffUserEntity(
  @PrimaryKey val id: String,
  val username: String,
  val fullName: String,
  val role: String, // OWNER, MANAGER, CASHIER
  // Salted PBKDF2 hash of the staff PIN - see com.aistudio.fasmovapos.app.data.security.PinHasher.
  // Never store or query the raw PIN (see engineering-standards.md §3/§9).
  val pinHash: String,
  val pinSalt: String,
  val branchId: String,
  val isActive: Boolean = true,
  // True until the user replaces a temporary PIN (new staff, sample staff); blocks the app until changed.
  @ColumnInfo(defaultValue = "0") val mustChangePin: Boolean = false,
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val entityName: String,
  val entityId: String,
  val operation: String, // INSERT, UPDATE, DELETE
  val payloadSnippet: String,
  val status: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
  val timestamp: Long = System.currentTimeMillis(),
)
