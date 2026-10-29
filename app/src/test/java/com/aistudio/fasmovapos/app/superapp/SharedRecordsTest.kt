package com.aistudio.fasmovapos.app.superapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SharedRecordsTest {
  private lateinit var db: FasMovaPOSDatabase
  private lateinit var r: SuperRepository

  @Before fun setup() {
    db =
      Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), FasMovaPOSDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    r = SuperRepository(db) { SuperDates.formatter("yyyy-MM-dd").parse("2026-10-29")!!.time + 12 * 3600000L }
  }

  @After fun close() {
    db.close()
  }

  @Test fun receiptAndSaleVoidsAreAtomicAndRestoreStockWithoutErasingOriginals() {
    val b = r.createBusiness(Livelihood.RETAIL, "Shop").id
    val i = r.item(b, "Pack", 14000, "GENERAL", 2000)
    val p = r.addParty(b, "Customer", "CUSTOMER").id
    val sale = r.sale(b, mapOf(i to 2000L), 10000, "2026-10-28", "CASH", p)
    val original = r.dao.commerceSale(sale)!!
    val receipt = r.receipt(b, p, 5000, "2026-10-29", "CASH")
    val before = r.dao.ledgerRows(b)
    assertThrows(IllegalArgumentException::class.java) { r.edits.sale(sale, "Incorrect sale") }
    assertEquals(before, r.dao.ledgerRows(b))
    r.edits.receipt(receipt, "Collection was entered in error")
    r.edits.sale(sale, "Incorrect sale")
    val s = r.snapshot(b, "2026-10-01", "2026-10-31")
    assertEquals(0L, s.earnedCents)
    assertEquals(0L, s.receivedCents)
    assertEquals(0L, s.owedCents)
    assertEquals(2000L, r.dao.commerceItem(i)!!.stockMilli)
    assertTrue(r.dao.commerceSale(sale)!!.voided)
    assertEquals(original.recordedAt, r.dao.commerceSale(sale)!!.recordedAt)
    assertTrue(r.history(sale).any { it.reason == "Incorrect sale" && it.before!!.contains("28000") })
  }

  @Test fun voidingDairyPaymentAlsoReversesItsCostDeductionExactlyOnce() {
    val b = r.createBusiness(Livelihood.DAIRY, "Dairy").id
    val p = r.addParty(b, "Society", "COOPERATIVE", 5000).id
    r.milk(b, p, "2026-10-28", "MORNING", "42", 5000)
    val receipt = r.receipt(b, p, 180000, "2026-10-29", "CASH", deductions = listOf(Deduction("FEED", 30000, true)))
    val cost = r.dao.expenseRows(b).single()
    assertThrows(IllegalArgumentException::class.java) { r.edits.expense(cost.id, "Wrong linked cost") }
    r.edits.receipt(receipt, "Payment entered in wrong account")
    val s = r.snapshot(b, "2026-10-01", "2026-10-31")
    assertEquals(210000L, s.earnedCents)
    assertEquals(210000L, s.owedCents)
    assertEquals(0L, s.receivedCents)
    assertEquals(0L, s.spentCents)
    assertTrue(r.dao.deductionRows(b).all { it.voided })
    assertThrows(IllegalArgumentException::class.java) { r.edits.receipt(receipt, "Repeated void") }
  }

  @Test fun equalCompletedPeriodsRequireRecordedDaysAndMissingDaysStayUnknown() {
    val b = r.createBusiness(Livelihood.RETAIL, "Shop").id
    val i = r.item(b, "Pack", 1000, "GENERAL", 10000)
    r.sale(b, mapOf(i to 1000L), 1000, "2026-10-27", "CASH")
    r.sale(b, mapOf(i to 2000L), 2000, "2026-10-28", "CASH")
    val day = r.periodReading(b, RecordPeriod("2026-10-28", "2026-10-28"))
    assertEquals(1, day.coverageDays)
    assertEquals(1000L, day.previous!!.earnedCents)
    assertNull(r.periodReading(b, RecordPeriod("2026-10-27", "2026-10-28")).previous)
    val missing = r.periodReading(b, RecordPeriod("2026-10-26", "2026-10-26"))
    assertEquals(0, missing.coverageDays)
    r.markNoTrade(b, "2026-10-26")
    r.markNoTrade(b, "2026-10-25")
    val knownZero = r.periodReading(b, RecordPeriod("2026-10-26", "2026-10-26"))
    assertEquals(1, knownZero.coverageDays)
    assertEquals(0L, knownZero.previous!!.earnedCents)
    r.moneyIn(b, 100000, "2026-10-29", "OWNER_CAPITAL")
    val current = r.periodReading(b, RecordPeriod("2026-10-29", "2026-10-29"))
    assertNull(current.previous)
    assertEquals(0L, current.snapshot.earnedCents)
    assertEquals(0L, current.snapshot.receivedCents)
  }

  @Test fun dairyAverageUsesRecordedDeliveryDaysAndExpenseEditsKeepEntryTime() {
    val b = r.createBusiness(Livelihood.DAIRY, "Dairy").id
    val p = r.addParty(b, "Buyer", "HOTEL", 5000).id
    r.milk(b, p, "2026-10-27", "MORNING", "20", 5000)
    r.milk(b, p, "2026-10-27", "EVENING", "20", 5000)
    r.milk(b, p, "2026-10-28", "MORNING", "20", 5000)
    val week = r.periodReading(b, RecordPeriod("2026-10-23", "2026-10-29"))
    assertEquals(2, week.milkDays)
    assertEquals(30000L, week.averageLitresMilli)
    val id = r.expense(b, 10000, "2026-10-28", "FEED")
    val original = r.dao.expenseRow(id)!!
    r.edits.expenseCorrection(id, 15000, "2026-10-27", "FEED", "Corrected receipt")
    assertEquals(original.recordedAt, r.dao.expenseRow(id)!!.recordedAt)
    assertEquals(15000L, r.snapshot(b, "2026-10-27", "2026-10-27").spentCents)
    assertTrue(
      r.history(id).any {
        it.reason == "Corrected receipt" &&
          it.before?.contains("10000") == true &&
          it.after?.contains("15000") == true
      },
    )
  }
}
