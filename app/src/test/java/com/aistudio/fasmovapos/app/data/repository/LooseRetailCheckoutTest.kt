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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LooseRetailCheckoutTest {
  private lateinit var db: FasMovaPOSDatabase
  private lateinit var repository: FasMovaPOSRepository
  private val beans =
    ProductEntity("loose-beans", "BEANS", "", "Beans", "Food", "Kg", 100.0, 160.0, 10_000, 1000, "b-1", quantityScale = 1000)
  private val customer = CustomerEntity("buyer", "Amina", "", "", creditLimit = 2000.0)

  @Before
  fun prepare() {
    db =
      Room
        .inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext<Application>(),
          FasMovaPOSDatabase::class.java,
        ).allowMainThreadQueries()
        .build()
    repository = FasMovaPOSRepository(db)
    runBlocking {
      repository.saveProduct(beans)
      repository.addCustomer(customer)
    }
  }

  @After
  fun close() = db.close()

  private suspend fun sell(
    items: List<CartItem>,
    payment: List<PaymentLine>? = null,
    buyer: CustomerEntity? = null,
  ) = repository.processSale(
    "b-1",
    "owner",
    "Owner",
    items,
    0.0,
    payment ?: listOf(PaymentLine(PaymentMethod.CASH, items.sumOf { it.lineTotal })),
    buyer,
  )

  private suspend fun refuses(block: suspend () -> Unit) {
    try {
      block()
      fail("Invalid operation accepted")
    } catch (_: IllegalArgumentException) {
    }
  }

  @Test
  fun quarterHalfAndOneAndThreeQuartersLeaveExactlySevenAndHalfKg() =
    runBlocking {
      for (quantity in listOf(250, 500, 1750)) {
        val saved = sell(listOf(CartItem(beans, quantity)))
        assertEquals(RetailQuantity.amount(160.0, quantity, 1000), saved.sale.netAmount, 0.0)
        assertEquals(saved.sale.totalAmount, saved.items.sumOf { it.totalPrice }, 0.0)
        assertEquals(1000, saved.items.single().quantityScale)
        assertEquals("Kg", saved.items.single().quantityUnit)
      }
      assertEquals(7500, db.productDao().getProductById(beans.id)!!.stockQuantity)
    }

  @Test
  fun partPaymentAndRepaymentKeepSaleCreditAndReceiptsDistinct() =
    runBlocking {
      val saved =
        sell(
          listOf(CartItem(beans, 1750)),
          listOf(PaymentLine(PaymentMethod.CASH, 100.0), PaymentLine(PaymentMethod.CREDIT, 180.0)),
          customer,
        )
      assertEquals(280.0, saved.sale.netAmount, 0.0)
      assertEquals(
        180.0,
        repository.customers
          .first()
          .single()
          .currentDebt,
        0.0,
      )
      repository.recordCustomerPayment(CustomerPaymentEntity("repayment", customer.id, customer.name, 50.0, "CASH", "cash"))
      assertEquals(
        130.0,
        repository.customers
          .first()
          .single()
          .currentDebt,
        0.0,
      )
      assertEquals(
        280.0,
        repository.sales
          .first()
          .single()
          .netAmount,
        0.0,
      )
      assertEquals(
        50.0,
        repository.customerPayments
          .first()
          .single()
          .amountPaid,
        0.0,
      )
    }

  @Test
  fun explicitPortionPriceAndHistoricalUnitAreSaved() =
    runBlocking {
      val priced = beans.copy(quarterPrice = 45.0)
      repository.saveProduct(priced)
      val saved = sell(listOf(CartItem(priced, 500, 250, 45.0)))
      assertEquals(90.0, saved.sale.netAmount, 0.0)
      assertEquals(250, saved.items.single().pricingQuantity)
      repository.adjustProductStock(priced.id, 0, null, 200.0, 50.0, null)
      val old = repository.getSaleItems(saved.sale.id).single()
      assertEquals(45.0, old.unitPrice, 0.0)
      assertEquals(90.0, old.totalPrice, 0.0)
      refuses { sell(listOf(CartItem(priced, 250, 250, 45.0))) }
      assertEquals(9500, db.productDao().getProductById(priced.id)!!.stockQuantity)
    }

  @Test
  fun volumeAndSolidFatUseSeparateDimensionsAndExactStock() =
    runBlocking {
      val oil = beans.copy(id = "oil", unit = "Ltr", name = "Liquid oil", stockQuantity = 20_000)
      val fat = beans.copy(id = "fat", name = "Solid cooking fat")
      repository.saveProduct(oil)
      repository.saveProduct(fat)
      sell(listOf(CartItem(oil, 250), CartItem(oil, 500), CartItem(fat, 500)))
      assertEquals(19_250, db.productDao().getProductById(oil.id)!!.stockQuantity)
      assertEquals(9500, db.productDao().getProductById(fat.id)!!.stockQuantity)
      refuses { repository.saveProduct(oil.copy(unit = "Kg")) }
    }

  @Test
  fun combinedLinesCannotOversellAndLastPortionIsAtomic() =
    runBlocking {
      refuses { sell(listOf(CartItem(beans, 6000), CartItem(beans, 6000))) }
      assertTrue(repository.sales.first().isEmpty())
      repository.adjustProductStock(beans.id, -9750, null, null)
      val results =
        (1..2)
          .map {
            async(Dispatchers.IO) {
              try {
                sell(listOf(CartItem(beans, 250)))
                true
              } catch (_: IllegalArgumentException) {
                false
              }
            }
          }.awaitAll()
      assertEquals(1, results.count { it })
      assertEquals(0, db.productDao().getProductById(beans.id)!!.stockQuantity)
    }

  @Test
  fun mobileReferenceCannotBeReusedBetweenRepaymentsOrASale() =
    runBlocking {
      sell(listOf(CartItem(beans, 1750)), listOf(PaymentLine(PaymentMethod.CREDIT, 280.0)), customer)
      repository.recordCustomerPayment(CustomerPaymentEntity("first", customer.id, customer.name, 50.0, "MPESA", "QKJ9821LXM"))
      refuses { repository.recordCustomerPayment(CustomerPaymentEntity("second", customer.id, customer.name, 50.0, "MPESA", "QKJ9821LXM")) }
      refuses { sell(listOf(CartItem(beans, 250)), listOf(PaymentLine(PaymentMethod.MPESA, 40.0, reference = "QKJ9821LXM"))) }
      assertEquals(
        230.0,
        repository.customers
          .first()
          .single()
          .currentDebt,
        0.0,
      )
      assertEquals(1, repository.customerPayments.first().size)
    }

  @Test
  fun invalidPortionRateAndOverflowCannotWriteSaleOrStock() =
    runBlocking {
      refuses { sell(listOf(CartItem(beans, 250, 250, 1.0))) }
      refuses { sell(listOf(CartItem(beans, 0))) }
      refuses { repository.adjustProductStock(beans.id, Int.MAX_VALUE, null, null) }
      assertTrue(repository.sales.first().isEmpty())
      assertEquals(10_000, db.productDao().getProductById(beans.id)!!.stockQuantity)
    }
}
