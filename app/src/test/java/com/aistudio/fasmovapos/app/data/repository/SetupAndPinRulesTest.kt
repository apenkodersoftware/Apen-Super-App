package com.aistudio.fasmovapos.app.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.data.security.PinProblem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SetupAndPinRulesTest {
  private lateinit var db: FasMovaPOSDatabase
  private lateinit var repository: FasMovaPOSRepository

  @Before
  fun setUp() {
    db =
      Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), FasMovaPOSDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repository = FasMovaPOSRepository(db)
  }

  @After
  fun tearDown() = db.close()

  private fun setup(sample: Boolean) =
    runBlocking {
      repository.completeSetup("Mama Njeri Shop", "Njeri Wanjiku", "njeri", "4821", sample)
    }

  private fun staff() = runBlocking { repository.staffUsers.first() }

  @Test
  fun freshInstall_needsSetup_andSetupCreatesOnlyTheOwner() {
    assertFalse(runBlocking { repository.isSetupComplete.first() })

    setup(sample = false)

    assertTrue(runBlocking { repository.isSetupComplete.first() })
    val owner = staff().single()
    assertEquals("OWNER", owner.role)
    assertFalse(owner.mustChangePin)
    assertEquals("Mama Njeri Shop", runBlocking { repository.branches.first() }.single().name)
    assertNotNull(runBlocking { repository.signIn("njeri", "4821") })
    assertNull(runBlocking { repository.signIn("njeri", "1234") })
    assertTrue(runBlocking { repository.products.first() }.isEmpty())
  }

  @Test
  fun sampleData_addsStaffOnTemporaryPins_andProducts() {
    setup(sample = true)

    val byRole = staff().associateBy { it.role }
    assertTrue(byRole.getValue("MANAGER").mustChangePin)
    assertTrue(byRole.getValue("CASHIER").mustChangePin)
    assertTrue(runBlocking { repository.products.first() }.isNotEmpty())
    // Both sample staff share 1234, so the PIN-only quick switch must identify neither of them.
    assertNull(runBlocking { repository.verifyStaffPinForBranch(HQ_BRANCH_ID, "1234") })
    // They can still sign in with username + PIN, which is where the forced change happens.
    assertNotNull(runBlocking { repository.signIn("cashier@fasmovapos.com", "1234") })
  }

  @Test
  fun changePin_refusesWeakAndDuplicatePins_thenAcceptsAGoodOne() {
    setup(sample = true)
    val cashier = staff().first { it.role == "CASHIER" }

    assertEquals(PinProblem.NOT_FOUR_DIGITS, runBlocking { repository.changePin(cashier.id, "12") })
    assertEquals(PinProblem.NOT_FOUR_DIGITS, runBlocking { repository.changePin(cashier.id, "12a4") })
    assertEquals(PinProblem.IS_DEFAULT, runBlocking { repository.changePin(cashier.id, "1234") })
    assertEquals(PinProblem.IN_USE, runBlocking { repository.changePin(cashier.id, "4821") }) // the Owner's PIN

    assertNull(runBlocking { repository.changePin(cashier.id, "7305") })

    val updated = staff().first { it.id == cashier.id }
    assertFalse(updated.mustChangePin)
    assertNull(runBlocking { repository.signIn("cashier@fasmovapos.com", "1234") })
    assertNotNull(runBlocking { repository.signIn("cashier@fasmovapos.com", "7305") })
    // Now that the PIN is private, the quick switch can identify them.
    assertEquals(cashier.id, runBlocking { repository.verifyStaffPinForBranch(HQ_BRANCH_ID, "7305") }?.id)
  }

  @Test
  fun changePin_refusesTheCurrentTemporaryPin() {
    setup(sample = false)
    runBlocking { repository.addStaffUser("amina", "Amina Ali", "CASHIER", "5566", HQ_BRANCH_ID) }
    val amina = staff().first { it.username == "amina" }
    assertTrue(amina.mustChangePin)

    assertEquals(PinProblem.SAME_AS_CURRENT, runBlocking { repository.changePin(amina.id, "5566") })
    assertNull(runBlocking { repository.changePin(amina.id, "9042") })
  }

  @Test
  fun aMobileMoneyCodeAlreadyOnAnEarlierSale_isRecognised_evenWithoutPaymentRows() {
    assertFalse(runBlocking { repository.isMobileMoneyCodeUsed("QKJ9821LXM") })

    setup(sample = true) // the sample sale keeps its code only on the sale record

    assertTrue(runBlocking { repository.isMobileMoneyCodeUsed("QKJ9821LXM") })
    assertFalse(runBlocking { repository.isMobileMoneyCodeUsed("QKJ9821LXN") })
    assertFalse(runBlocking { repository.isMobileMoneyCodeUsed("QKJ9821") }) // a prefix is not a match
  }

  @Test
  fun aCodeInsideASplitSalesJoinedReference_isRecognised() {
    setup(sample = false)
    val sale = SampleData.firstSale().first.copy(id = "split-1", paymentReference = "CASH-123456 + ABCD1234XY")
    runBlocking { db.saleDao().insertSale(sale) }

    assertTrue(runBlocking { repository.isMobileMoneyCodeUsed("ABCD1234XY") })
    assertTrue(runBlocking { repository.isMobileMoneyCodeUsed("CASH-123456") })
    assertFalse(runBlocking { repository.isMobileMoneyCodeUsed("ABCD1234") })
  }

  @Test
  fun businessDetails_areOptionalAtSetup_andEditableLater() {
    setup(sample = false)
    val first = runBlocking { repository.branches.first() }.single()
    assertEquals("", first.phone)
    assertEquals("", first.taxPin)

    runBlocking { repository.updateBusinessDetails(first.id, "Njeri Traders", "0712 345 678", "A001234567Z") }

    val updated = runBlocking { repository.branches.first() }.single()
    assertEquals("Njeri Traders", updated.name)
    assertEquals("0712 345 678", updated.phone)
    assertEquals("A001234567Z", updated.taxPin)
  }
}
