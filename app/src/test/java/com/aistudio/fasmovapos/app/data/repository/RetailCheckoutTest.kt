package com.aistudio.fasmovapos.app.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.CustomerPaymentEntity
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.payment.PaymentLine
import com.aistudio.fasmovapos.app.data.payment.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercises real Room writes, including an injected SQLite failure after a sale has started. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RetailCheckoutTest {
  private lateinit var db: FasMovaPOSDatabase
  private lateinit var repository: FasMovaPOSRepository
  private val product = ProductEntity("p-1", "BOWL", "", "Bowl", "Kitchenware", "Pcs", 60.0, 100.0, 5, 2, "b-1")
  private val customer = CustomerEntity("c-1", "Test customer", "", "", creditLimit = 500.0, currentDebt = 50.0)

  @Before
  fun setUp() {
    db =
      Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), FasMovaPOSDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository = FasMovaPOSRepository(db)
    runBlocking {
      db.productDao().insertProduct(product)
      db.customerDao().insertCustomer(customer)
    }
  }

  @After
  fun tearDown() = db.close()

  private suspend fun sell(
    items: List<CartItem> = listOf(CartItem(product, 2)),
    lines: List<PaymentLine> = listOf(PaymentLine(PaymentMethod.CASH, 200.0)),
    buyer: CustomerEntity? = null,
  ) = repository.processSale("b-1", "u-1", "Owner", items, 0.0, lines, buyer)

  private suspend fun refused(block: suspend () -> Unit) {
    try {
      block()
      fail("The invalid operation must be refused")
    } catch (_: IllegalArgumentException) {
      // Expected: no records should be written by the refused operation.
    }
  }

  @Test
  fun splitSale_savesBothTenders_andReducesStock() =
    runBlocking {
      val result =
        sell(lines = listOf(PaymentLine(PaymentMethod.CASH, 80.0), PaymentLine(PaymentMethod.MPESA, 120.0, reference = "QKJ9821LXM")))

      assertEquals(2, result.payments.size)
      assertEquals(3, db.productDao().getProductById(product.id)?.stockQuantity)
      assertEquals(
        1,
        db
          .saleDao()
          .getAllSales()
          .first()
          .size,
      )
    }

  @Test
  fun checkout_rechecksStoredStock_insteadOfTrustingTheCart() =
    runBlocking {
      db.productDao().updateProduct(product.copy(stockQuantity = 1))

      refused { sell() }

      assertTrue(
        db
          .saleDao()
          .getAllSales()
          .first()
          .isEmpty(),
      )
      assertEquals(1, db.productDao().getProductById(product.id)?.stockQuantity)
    }

  @Test
  fun repeatedProductLines_cannotSellMoreThanTheCombinedStock() =
    runBlocking {
      refused { sell(listOf(CartItem(product, 3), CartItem(product, 3)), listOf(PaymentLine(PaymentMethod.CASH, 600.0))) }

      assertTrue(
        db
          .saleDao()
          .getAllSales()
          .first()
          .isEmpty(),
      )
      assertEquals(5, db.productDao().getProductById(product.id)?.stockQuantity)
    }

  @Test
  fun emptyCart_andNegativeQuantity_areRefused() =
    runBlocking {
      refused { sell(emptyList(), listOf(PaymentLine(PaymentMethod.CASH, 0.0))) }
      refused { sell(listOf(CartItem(product, -1)), listOf(PaymentLine(PaymentMethod.CASH, -100.0))) }

      assertTrue(
        db
          .saleDao()
          .getAllSales()
          .first()
          .isEmpty(),
      )
    }

  @Test
  fun stalePrice_isRefusedBeforeTakingPayment() =
    runBlocking {
      db.productDao().updateProduct(product.copy(sellingPrice = 120.0))

      refused { sell() }

      assertTrue(
        db
          .saleDao()
          .getAllSales()
          .first()
          .isEmpty(),
      )
    }

  @Test
  fun creditLimit_isCheckedAgainstTheStoredBalance() =
    runBlocking {
      db.customerDao().updateCustomer(customer.copy(currentDebt = 450.0))

      refused { sell(lines = listOf(PaymentLine(PaymentMethod.CREDIT, 200.0)), buyer = customer) }

      assertEquals(450.0, requireNotNull(db.customerDao().getCustomerById(customer.id)).currentDebt, 0.001)
      assertTrue(
        db
          .saleDao()
          .getAllSales()
          .first()
          .isEmpty(),
      )
    }

  @Test
  fun creditSale_updatesDebt_andSpentTogether() =
    runBlocking {
      sell(lines = listOf(PaymentLine(PaymentMethod.CREDIT, 200.0)), buyer = customer)

      val updated = requireNotNull(db.customerDao().getCustomerById(customer.id))
      assertEquals(250.0, updated.currentDebt, 0.001)
      assertEquals(200.0, updated.totalSpent, 0.001)
    }

  @Test
  fun failureInLastWrite_rollsBackSale_stock_andCustomerBalance() =
    runBlocking {
      db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER refuse_queue BEFORE INSERT ON sync_queue BEGIN SELECT RAISE(ABORT, 'injected failure'); END",
      )

      try {
        sell(lines = listOf(PaymentLine(PaymentMethod.CREDIT, 200.0)), buyer = customer)
        fail("The injected database failure must propagate")
      } catch (_: android.database.sqlite.SQLiteException) {
        // The transaction must undo every preceding write, not just the queue insertion.
      }

      assertTrue(
        db
          .saleDao()
          .getAllSales()
          .first()
          .isEmpty(),
      )
      assertTrue(
        db
          .saleDao()
          .getAllSalePayments()
          .first()
          .isEmpty(),
      )
      assertEquals(5, db.productDao().getProductById(product.id)?.stockQuantity)
      assertEquals(50.0, requireNotNull(db.customerDao().getCustomerById(customer.id)).currentDebt, 0.001)
    }

  @Test
  fun reusedMobileMoneyReference_isRefusedByTheRepository() =
    runBlocking {
      val lines = listOf(PaymentLine(PaymentMethod.MPESA, 200.0, reference = "QKJ9821LXM"))
      sell(lines = lines)

      refused { sell(lines = lines) }

      assertEquals(
        1,
        db
          .saleDao()
          .getAllSales()
          .first()
          .size,
      )
    }

  @Test
  fun invalidTenderAmounts_doNotWriteASale() =
    runBlocking {
      refused { sell(lines = listOf(PaymentLine(PaymentMethod.CASH, Double.NaN))) }
      refused { sell(lines = listOf(PaymentLine(PaymentMethod.CASH, -10.0), PaymentLine(PaymentMethod.CARD, 210.0))) }

      assertTrue(
        db
          .saleDao()
          .getAllSales()
          .first()
          .isEmpty(),
      )
    }

  @Test
  fun repayment_cannotExceedDebt_orBeNegative() =
    runBlocking {
      refused { repository.recordCustomerPayment(repayment(60.0)) }
      refused { repository.recordCustomerPayment(repayment(-10.0)) }

      assertEquals(50.0, requireNotNull(db.customerDao().getCustomerById(customer.id)).currentDebt, 0.001)
      assertTrue(
        db
          .customerDao()
          .getAllCustomerPayments()
          .first()
          .isEmpty(),
      )
    }

  @Test
  fun exactRepayment_clearsDebt_andKeepsAReceipt() =
    runBlocking {
      repository.recordCustomerPayment(repayment(50.0))

      assertEquals(0.0, requireNotNull(db.customerDao().getCustomerById(customer.id)).currentDebt, 0.001)
      assertEquals(
        1,
        db
          .customerDao()
          .getAllCustomerPayments()
          .first()
          .size,
      )
    }

  private fun repayment(amount: Double) = CustomerPaymentEntity("pay-1", customer.id, customer.name, amount, "CASH", "cash-1")

  @Test
  fun twoCheckoutsForTheLastItem_onlyOneCommits() =
    runBlocking {
      db.productDao().updateProduct(product.copy(stockQuantity = 1))
      val outcomes =
        (1..2)
          .map {
            async(Dispatchers.IO) {
              runCatching { sell(listOf(CartItem(product, 1)), listOf(PaymentLine(PaymentMethod.CASH, 100.0))) }
            }
          }.awaitAll()

      assertEquals(1, outcomes.count { it.isSuccess })
      assertEquals(0, db.productDao().getProductById(product.id)?.stockQuantity)
      assertEquals(
        1,
        db
          .saleDao()
          .getAllSales()
          .first()
          .size,
      )
    }

  @Test
  fun decimalPrices_totalExactly_andDoNotCorruptCreditBalance() =
    runBlocking {
      val small = product.copy(sellingPrice = 0.1)
      db.productDao().updateProduct(small)
      sell(listOf(CartItem(small, 3)), listOf(PaymentLine(PaymentMethod.CREDIT, 0.3)), customer)

      assertEquals(50.3, requireNotNull(db.customerDao().getCustomerById(customer.id)).currentDebt, 0.00001)
      sell(listOf(CartItem(small, 1)), listOf(PaymentLine(PaymentMethod.CREDIT, 0.1)), customer)
      assertEquals(50.4, requireNotNull(db.customerDao().getCustomerById(customer.id)).currentDebt, 0.00001)
    }

  @Test
  fun repeatedRepayment_doesNotReduceDebtTwice() =
    runBlocking {
      repository.recordCustomerPayment(repayment(10.0))
      refused { repository.recordCustomerPayment(repayment(10.0)) }

      assertEquals(40.0, requireNotNull(db.customerDao().getCustomerById(customer.id)).currentDebt, 0.001)
      assertEquals(
        1,
        db
          .customerDao()
          .getAllCustomerPayments()
          .first()
          .size,
      )
    }

  @Test
  fun adjustment_cannotMakeStockNegative_orOverflow() =
    runBlocking {
      refused { repository.adjustProductStock(product.id, -6, null, null) }
      refused { repository.adjustProductStock(product.id, Int.MAX_VALUE, null, null) }

      assertEquals(5, db.productDao().getProductById(product.id)?.stockQuantity)
    }

  @Test
  fun invalidProduct_doesNotEnterTheCatalogue() =
    runBlocking {
      refused { repository.saveProduct(product.copy(id = "bad", stockQuantity = -1)) }
      refused { repository.saveProduct(product.copy(id = "bad", sellingPrice = Double.NaN)) }

      assertEquals(null, db.productDao().getProductById("bad"))
    }
}
