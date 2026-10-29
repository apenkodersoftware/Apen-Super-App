package com.aistudio.fasmovapos.app.data.backup

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.CustomerPaymentEntity
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.payment.PaymentLine
import com.aistudio.fasmovapos.app.data.payment.PaymentMethod
import com.aistudio.fasmovapos.app.data.repository.CartItem
import com.aistudio.fasmovapos.app.data.repository.FasMovaPOSRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LooseBackupTest {
  private val app = ApplicationProvider.getApplicationContext<Application>()
  private val password = "review-backup-4821".toCharArray()

  private fun repository() = FasMovaPOSRepository(FasMovaPOSDatabase.getDatabase(app))

  @Before fun prepare() = FasMovaPOSDatabase.closeAndReset()

  @After fun close() = FasMovaPOSDatabase.closeAndReset()

  @Test
  fun fractionalStockReceiptAndDebtSurviveRestartAndEncryptedRestore() =
    runBlocking {
      val repo = repository()
      repo.completeSetup("Loose goods shop", "Owner", "owner", "4821", false)
      val product = ProductEntity("beans", "B", "", "Beans", "Food", "Kg", 100.0, 160.0, 10_000, 1000, "b-1", quantityScale = 1000)
      val customer = CustomerEntity("buyer", "Amina", "", "")
      repo.saveProduct(product)
      repo.addCustomer(customer)
      for (quantity in listOf(250, 500)) {
        repo.processSale(
          "b-1",
          "u-1",
          "Owner",
          listOf(CartItem(product, quantity)),
          0.0,
          listOf(PaymentLine(PaymentMethod.CASH, CartItem(product, quantity).lineTotal)),
          null,
        )
      }
      val sale =
        repo.processSale(
          "b-1",
          "u-1",
          "Owner",
          listOf(CartItem(product, 1750)),
          0.0,
          listOf(PaymentLine(PaymentMethod.CASH, 100.0), PaymentLine(PaymentMethod.CREDIT, 180.0)),
          customer,
        )
      repo.recordCustomerPayment(CustomerPaymentEntity("pay", "buyer", "Amina", 50.0, "CASH", "r"))
      val backup = BackupManager(app).createBackup(password)
      FasMovaPOSDatabase.closeAndReset()
      assertEquals(
        7500,
        repository()
          .products
          .first()
          .single()
          .stockQuantity,
      )
      repository().adjustProductStock("beans", 10_000, null, null)
      BackupManager(app).restore(backup, password)
      assertEquals(
        7500,
        repository()
          .products
          .first()
          .single()
          .stockQuantity,
      )
      assertEquals(
        130.0,
        repository()
          .customers
          .first()
          .single()
          .currentDebt,
        0.0,
      )
      val line = repository().getSaleItems(sale.sale.id).single()
      assertEquals(1750, line.quantity)
      assertEquals("Kg", line.quantityUnit)
      assertEquals(1000, line.quantityScale)
      assertEquals(280.0, line.totalPrice, 0.0)
    }

  @Test
  fun siblingAppBackupHeaderIsRejected() {
    val backup = BackupCrypto.encrypt("data".toByteArray(), password)
    backup[1] = 'M'.code.toByte()
    assertThrows(BackupFormatException::class.java) { BackupCrypto.decrypt(backup, password) }
  }
}
