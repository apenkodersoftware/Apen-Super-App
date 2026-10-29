package com.aistudio.fasmovapos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
  @Query("SELECT * FROM products ORDER BY name ASC")
  fun getAllProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE branchId = :branchId ORDER BY name ASC")
  fun getProductsByBranch(branchId: String): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
  suspend fun getProductById(id: String): ProductEntity?

  @Query("SELECT * FROM products WHERE stockQuantity <= minStockThreshold")
  fun getLowStockProducts(): Flow<List<ProductEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProduct(product: ProductEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProducts(products: List<ProductEntity>)

  @Update
  suspend fun updateProduct(product: ProductEntity)

  @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity WHERE id = :productId")
  suspend fun decrementStock(
    productId: String,
    quantity: Int,
  )

  @Query("UPDATE products SET stockQuantity = stockQuantity + :quantity WHERE id = :productId")
  suspend fun incrementStock(
    productId: String,
    quantity: Int,
  )

  @Query("DELETE FROM products WHERE id = :id")
  suspend fun deleteProductById(id: String)
}

@Dao
interface SaleDao {
  @Query("SELECT * FROM sales ORDER BY timestamp DESC")
  fun getAllSales(): Flow<List<SaleEntity>>

  @Query("SELECT * FROM sales WHERE branchId = :branchId ORDER BY timestamp DESC")
  fun getSalesByBranch(branchId: String): Flow<List<SaleEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSale(sale: SaleEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSaleItems(items: List<SaleItemEntity>)

  @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
  suspend fun getSaleItems(saleId: String): List<SaleItemEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSalePayments(payments: List<SalePaymentEntity>)

  // How many earlier payments carry this code. It can sit on a payment row, or only on the sale itself (sample data,
  // sales from before per-payment rows); a split sale joins its codes as "A + B", so match the whole value or
  // either side of " + ".
  @Query(
    "SELECT (SELECT COUNT(*) FROM sale_payments WHERE reference = :reference) + " +
      "(SELECT COUNT(*) FROM sales WHERE paymentReference = :reference " +
      "OR paymentReference LIKE :reference || ' + %' OR paymentReference LIKE '% + ' || :reference)",
  )
  suspend fun countUsesOfReference(reference: String): Int

  @Query("SELECT * FROM sale_payments WHERE saleId = :saleId")
  suspend fun getSalePayments(saleId: String): List<SalePaymentEntity>

  @Query("SELECT * FROM sale_payments")
  fun getAllSalePayments(): Flow<List<SalePaymentEntity>>

  @Query("UPDATE sales SET syncStatus = 'SYNCED' WHERE id = :saleId")
  suspend fun markSaleSynced(saleId: String)
}

@Dao
interface SupplierDao {
  @Query("SELECT * FROM suppliers ORDER BY name ASC")
  fun getAllSuppliers(): Flow<List<SupplierEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSupplier(supplier: SupplierEntity)

  @Update
  suspend fun updateSupplier(supplier: SupplierEntity)

  @Query("DELETE FROM suppliers WHERE id = :id")
  suspend fun deleteSupplier(id: String)
}

@Dao
interface PurchaseOrderDao {
  @Query("SELECT * FROM purchase_orders ORDER BY timestamp DESC")
  fun getAllPurchaseOrders(): Flow<List<PurchaseOrderEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPurchaseOrder(po: PurchaseOrderEntity)
}

@Dao
interface ExpenseDao {
  @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
  fun getAllExpenses(): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses WHERE branchId = :branchId ORDER BY timestamp DESC")
  fun getExpensesByBranch(branchId: String): Flow<List<ExpenseEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExpense(expense: ExpenseEntity)

  @Query("DELETE FROM expenses WHERE id = :id")
  suspend fun deleteExpense(id: String)
}

@Dao
interface CustomerDao {
  @Query("SELECT * FROM customers ORDER BY name ASC")
  fun getAllCustomers(): Flow<List<CustomerEntity>>

  @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
  suspend fun getCustomerById(id: String): CustomerEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomer(customer: CustomerEntity)

  @Update
  suspend fun updateCustomer(customer: CustomerEntity)

  @Query(
    "UPDATE customers SET currentDebt = ROUND(currentDebt + :amount, 2), totalSpent = ROUND(totalSpent + :totalSpentInc, 2) WHERE id = :customerId",
  )
  suspend fun updateCustomerDebtAndSpent(
    customerId: String,
    amount: Double,
    totalSpentInc: Double,
  )

  @Query("SELECT * FROM customer_payments WHERE id = :id LIMIT 1")
  suspend fun getPaymentById(id: String): CustomerPaymentEntity?

  @Query("SELECT COUNT(*) FROM customer_payments WHERE paymentMethod IN ('MPESA','AIRTEL') AND reference = :reference")
  suspend fun countMobileReference(reference: String): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPayment(payment: CustomerPaymentEntity)

  @Query("SELECT * FROM customer_payments ORDER BY timestamp DESC")
  fun getAllCustomerPayments(): Flow<List<CustomerPaymentEntity>>
}

@Dao
interface BranchDao {
  @Query("SELECT * FROM branches ORDER BY name ASC")
  fun getAllBranches(): Flow<List<BranchEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBranch(branch: BranchEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBranches(branches: List<BranchEntity>)

  @Update
  suspend fun updateBranch(branch: BranchEntity)
}

@Dao
interface StaffUserDao {
  @Query("SELECT * FROM staff_users ORDER BY fullName ASC")
  fun getAllStaffUsers(): Flow<List<StaffUserEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStaffUser(staffUser: StaffUserEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStaffUsers(users: List<StaffUserEntity>)

  @Update
  suspend fun updateStaffUser(staffUser: StaffUserEntity)

  @Query("SELECT * FROM staff_users WHERE id = :id LIMIT 1")
  suspend fun getUserById(id: String): StaffUserEntity?

  @Query("SELECT * FROM staff_users WHERE isActive = 1")
  suspend fun getActiveStaff(): List<StaffUserEntity>

  @Query("SELECT * FROM staff_users WHERE username = :username AND isActive = 1 LIMIT 1")
  suspend fun getUserByUsername(username: String): StaffUserEntity?

  // PBKDF2 hashes can't be matched in SQL (each row has a different salt), so PIN
  // verification fetches the small branch-scoped active-staff set and checks each
  // hash in app code via PinHasher.verify(). See FasMovaPOSRepository.verifyStaffPinForBranch.
  @Query("SELECT * FROM staff_users WHERE branchId = :branchId AND isActive = 1")
  suspend fun getActiveStaffForBranch(branchId: String): List<StaffUserEntity>
}

@Dao
interface SyncQueueDao {
  @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
  fun getPendingSyncItems(): Flow<List<SyncQueueEntity>>

  @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
  fun getPendingCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun enqueue(item: SyncQueueEntity)

  @Query("UPDATE sync_queue SET status = 'SYNCED' WHERE id = :id")
  suspend fun markSynced(id: Long)

  @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
  suspend fun clearSynced()
}
