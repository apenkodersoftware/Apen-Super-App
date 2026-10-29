package com.aistudio.fasmovapos.app.ui

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.data.payment.PaymentMethod
import com.aistudio.fasmovapos.app.data.payment.PaymentState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Drives the real ViewModel and Room database: two taps on Pay in a row must record one sale. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CheckoutDoubleTapTest {
  private val app = ApplicationProvider.getApplicationContext<Application>()
  private lateinit var db: FasMovaPOSDatabase

  @Before
  fun setUp() {
    FasMovaPOSDatabase.closeAndReset()
    db = FasMovaPOSDatabase.getDatabase(app)
  }

  @After
  fun tearDown() = FasMovaPOSDatabase.closeAndReset()

  private fun saleCount() =
    runBlocking {
      db
        .saleDao()
        .getAllSales()
        .first()
        .size
    }

  /** Setup hashes three PINs before the sample catalog appears, so wait for it instead of guessing a delay. */
  private fun awaitSampleCatalog() {
    repeat(CATALOG_WAIT_TRIES) {
      shadowOf(Looper.getMainLooper()).idle()
      if (runBlocking { db.productDao().getAllProducts().first() }.isNotEmpty()) return
      Thread.sleep(CATALOG_WAIT_STEP_MS)
    }
  }

  private fun settle() {
    repeat(20) {
      shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(50)
    }
  }

  @Test
  fun doubleTapOnCheckout_recordsOneSale() {
    val viewModel = FasMovaPOSViewModel(app)
    viewModel.completeSetup("Test Shop", "Test Owner", "owner", "4821", loadSampleData = true)
    awaitSampleCatalog()
    val product = runBlocking { db.productDao().getAllProducts().first() }.first { it.stockQuantity >= 2 }
    val salesBefore = saleCount()

    viewModel.addToCart(product)
    viewModel.checkoutSale()
    viewModel.checkoutSale()
    settle()

    assertEquals(1, saleCount() - salesBefore)
    val stockAfter = runBlocking { db.productDao().getProductById(product.id) }?.stockQuantity
    assertEquals(product.stockQuantity - 1, stockAfter)
    assertTrue(viewModel.isCheckingOut.value.not())
  }

  @Test
  fun aMobileMoneyCode_cannotPayForTwoSales() {
    val viewModel = FasMovaPOSViewModel(app)
    viewModel.completeSetup("Test Shop", "Test Owner", "owner", "4821", loadSampleData = true)
    awaitSampleCatalog()
    val product = runBlocking { db.productDao().getAllProducts().first() }.first { it.stockQuantity >= 3 }
    val salesBefore = saleCount()
    val mpesa = { state: PaymentState -> state.copy(method = PaymentMethod.MPESA, reference = "pqr4567stu") }

    viewModel.addToCart(product)
    viewModel.updatePayment(mpesa)
    viewModel.checkoutSale()
    settle()
    assertEquals(1, saleCount() - salesBefore)
    assertEquals("PQR4567STU", runBlocking { db.saleDao().getAllSalePayments().first() }.single { it.method == "MPESA" }.reference)

    viewModel.addToCart(product)
    viewModel.updatePayment(mpesa)
    viewModel.checkoutSale() // same SMS code again
    settle()

    assertEquals(1, saleCount() - salesBefore)
  }

  @Test
  fun theCodeOfASampleSale_cannotBeReusedAtCheckout() {
    val viewModel = FasMovaPOSViewModel(app)
    viewModel.completeSetup("Test Shop", "Test Owner", "owner", "4821", loadSampleData = true)
    awaitSampleCatalog()
    val product = runBlocking { db.productDao().getAllProducts().first() }.first { it.stockQuantity >= 3 }
    val salesBefore = saleCount()

    viewModel.addToCart(product)
    viewModel.updatePayment { it.copy(method = PaymentMethod.MPESA, reference = "QKJ9821LXM") } // the sample sale's code
    viewModel.checkoutSale()
    settle()

    assertEquals(0, saleCount() - salesBefore)
  }
}

private const val CATALOG_WAIT_TRIES = 600
private const val CATALOG_WAIT_STEP_MS = 100L
