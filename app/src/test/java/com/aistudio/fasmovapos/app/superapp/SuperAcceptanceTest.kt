package com.aistudio.fasmovapos.app.superapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SuperAcceptanceTest {
  private lateinit var db: FasMovaPOSDatabase
  private lateinit var repo: SuperRepository

  @Before fun before() {
    db =
      Room
        .inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext<Context>(),
          FasMovaPOSDatabase::class.java,
        ).allowMainThreadQueries()
        .build()
    repo = SuperRepository(db)
  }

  @After fun after() {
    db.close()
  }

  private fun totals(b: String) = repo.snapshot(b, "2026-10-01", "2026-11-30")

  @Test fun satD_dairy42LitresPaymentDeductionExpenseAndCorrection() {
    val b = repo.createBusiness(Livelihood.DAIRY, "Demo Dairy", true).id
    val buyer = repo.addParty(b, "Demo Dairy Society", "COOPERATIVE", 5000).id
    val first = repo.milk(b, buyer, "2026-10-27", "MORNING", "12", 5000)
    repo.milk(b, buyer, "2026-10-27", "EVENING", "10", 5000)
    repo.milk(b, buyer, "2026-10-28", "MORNING", "11", 5000)
    repo.milk(b, buyer, "2026-10-28", "EVENING", "9", 5000)
    assertEquals(42000L, totals(b).litresMilli)
    assertEquals(210000L, totals(b).earnedCents)
    assertEquals(210000L, totals(b).owedCents)
    repo.receipt(b, buyer, 180000, "2026-10-28", "MPESA", "UJR2DEMO01", listOf(Deduction("FEED", 30000, true)))
    assertEquals(0L, totals(b).owedCents)
    assertEquals(180000L, totals(b).receivedCents)
    assertEquals(30000L, totals(b).spentCents)
    repo.expense(b, 50000, "2026-10-28", "VET_AI")
    assertEquals(130000L, totals(b).salesLessExpensesCents)
    repo.editMilk(first, "13", "Corrected measuring jug reading")
    assertEquals(43000L, totals(b).litresMilli)
    assertEquals(215000L, totals(b).earnedCents)
    assertEquals(5000L, totals(b).owedCents)
    assertEquals(135000L, totals(b).salesLessExpensesCents)
    assertTrue(
      repo.history(first).any {
        it.reason == "Corrected measuring jug reading" &&
          it.before!!.contains("12000") &&
          it.after!!.contains("13000")
      },
    )
  }

  @Test fun satL_monthlyChargesVacancyAndOldestArrearsAllocation() {
    val b = repo.createBusiness(Livelihood.RENTAL, "Demo Rentals", true).id
    val p = repo.property(b, "Demo Court")
    val a1 = repo.unit(b, p, "A1", 800000, 5, "A1 tenant", "2026-10-01")
    val a2 = repo.unit(b, p, "A2", 800000, 5, "A2 tenant", "2026-10-01")
    val b1 = repo.unit(b, p, "B1", 1200000, 5, "B1 tenant", "2026-10-01")
    assertEquals(3, repo.rentCharges(b, "2026-10"))
    assertEquals(2800000L, totals(b).earnedCents)
    repo.receipt(b, repo.tenantParty(a1), 800000, "2026-10-05", "MPESA", "UJR2RENT01")
    repo.receipt(b, repo.tenantParty(a2), 500000, "2026-10-05", "CASH")
    assertEquals(1300000L, totals(b).receivedCents)
    assertEquals(1500000L, totals(b).owedCents)
    repo.expense(b, 250000, "2026-10-12", "REPAIRS")
    assertEquals(1050000L, totals(b).receivedCents - totals(b).spentCents)
    repo.moveOut(b1, "2026-11-01", "Tenant leaving before November")
    assertEquals(2, repo.rentCharges(b, "2026-11"))
    assertEquals(0, repo.rentCharges(b, "2026-11"))
    assertEquals(3100000L, totals(b).owedCents)
    val paid = repo.receipt(b, repo.tenantParty(a2), 300000, "2026-11-02", "CASH")
    assertEquals(listOf("2026-10"), repo.allocations(paid).map { it.month })
    assertTrue(repo.reminder(a2).contains("A2"))
    assertTrue(repo.reminder(a2).contains("8,000"))
  }

  @Test fun satR_menuSalesTabShoppingAndNextDayCollection() {
    val b = repo.createBusiness(Livelihood.RESTAURANT, "Demo Kitchen", true).id
    val chai = repo.item(b, "Chai", 2000, "DRINKS")
    val chapati = repo.item(b, "Chapati", 2000, "FOOD")
    val githeri = repo.item(b, "Githeri", 6000, "FOOD")
    repo.item(b, "Ugali and sukuma", 8000, "FOOD")
    repo.item(b, "Samosa", 1500, "SNACKS")
    val regular = repo.addParty(b, "Demo regular", "CUSTOMER").id
    repo.sale(b, mapOf(chai to 25000, chapati to 15000, githeri to 18000), 188000, "2026-10-28", "CASH")
    repo.sale(b, mapOf(githeri to 2000), 0, "2026-10-28", "CREDIT", regular)
    assertEquals(200000L, totals(b).earnedCents)
    assertEquals(188000L, totals(b).receivedCents)
    assertEquals(12000L, totals(b).owedCents)
    repo.expense(b, 90000, "2026-10-28", "SHOPPING")
    assertEquals(110000L, totals(b).salesLessExpensesCents)
    repo.receipt(b, regular, 12000, "2026-10-29", "CASH")
    assertEquals(200000L, totals(b).earnedCents)
    assertEquals(200000L, totals(b).receivedCents)
    assertEquals(0L, totals(b).owedCents)
  }

  @Test fun satS_retailPartPaymentAndCollectionNeverDuplicateSale() {
    val b = repo.createBusiness(Livelihood.RETAIL, "Demo Shop", true).id
    val i = repo.item(b, "Household pack", 28000, "GENERAL", 10000)
    val customer = repo.addParty(b, "Demo shop customer", "CUSTOMER").id
    repo.sale(b, mapOf(i to 1000), 10000, "2026-10-28", "CASH", customer)
    assertEquals(28000L, totals(b).earnedCents)
    assertEquals(18000L, totals(b).owedCents)
    repo.receipt(b, customer, 5000, "2026-10-28", "CASH")
    assertEquals(28000L, totals(b).earnedCents)
    assertEquals(15000L, totals(b).receivedCents)
    assertEquals(13000L, totals(b).owedCents)
  }

  @Test fun satX_businessPartitionAndNonSalesMoney() {
    val dairy = repo.createBusiness(Livelihood.DAIRY, "Live dairy").id
    val shop = repo.createBusiness(Livelihood.RETAIL, "Isolated demo", true).id
    val buyer = repo.addParty(dairy, "Buyer", "HOTEL", 5000).id
    repo.milk(dairy, buyer, "2026-10-28", "MORNING", "0.5", 5000)
    repo.moneyIn(dairy, 100000, "2026-10-28", "OWNER_CAPITAL")
    assertEquals(2500L, totals(dairy).earnedCents)
    assertEquals(0L, totals(shop).earnedCents)
    assertEquals(0L, totals(dairy).receivedCents)
    assertThrows(IllegalArgumentException::class.java) { repo.milk(shop, buyer, "2026-10-28", "MORNING", "1", 5000) }
  }
}
