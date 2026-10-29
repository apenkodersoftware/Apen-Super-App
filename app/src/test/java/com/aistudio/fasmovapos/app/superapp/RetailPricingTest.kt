package com.aistudio.fasmovapos.app.superapp

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RetailPricingTest {
  @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FasMovaPOSDatabase::class.java)

  @Test fun existingRetailTierPricesMigrateThroughBothSuperSchemas() {
    val name = "retail-tier-migration"
    helper.createDatabase(name, 6).use { db ->
      db.execSQL("INSERT INTO branches VALUES ('b-1','Original shop','HQ','',1,'','')")
      db.execSQL("INSERT INTO staff_users VALUES ('owner','owner','Owner','OWNER','hash','salt','b-1',1,0)")
      db.execSQL("INSERT INTO products VALUES ('p','sku','','Sugar','General','Kg',100,180,10000,1000,'b-1',0,0,1000,50,95,0)")
    }
    helper.runMigrationsAndValidate(name, 8, true, FasMovaPOSDatabase.MIGRATION_6_7, FasMovaPOSDatabase.MIGRATION_7_8).use { migrated ->
      migrated.query("SELECT quarterPriceCents,halfPriceCents,stockMilli FROM super_items WHERE id='p'").use {
        it.moveToFirst()
        assertEquals(5000L, it.getLong(0))
        assertEquals(9500L, it.getLong(1))
        assertEquals(10000L, it.getLong(2))
      }
      migrated.query("SELECT quarterPrice,halfPrice FROM products WHERE id='p'").use {
        it.moveToFirst()
        assertEquals(50.0, it.getDouble(0), 0.0)
        assertEquals(95.0, it.getDouble(1), 0.0)
      }
    }
  }

  @Test fun tierPricesDiscountAndPartialCollectionRemainExactAndAtomic() {
    val db =
      Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), FasMovaPOSDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      val r = SuperRepository(db)
      val b = r.createBusiness(Livelihood.RETAIL, "Tier shop").id
      val item = r.commerce.item(b, "Sugar", 18000, "GENERAL", 10000, "Kg")
      r.retailDetails(item, "Sugar", "GENERAL", 10000, 1000, 5000, 9500, "Configured recorded pack prices")
      val row = r.dao.commerceItem(item)!!
      assertEquals(5000L, SuperPricing.lineTotal(row, 250))
      assertEquals(9500L, SuperPricing.lineTotal(row, 500))
      assertEquals(27000L, SuperPricing.lineTotal(row, 1500))
      val customer = r.addParty(b, "Customer", "CUSTOMER").id
      r.commerce.sale(b, mapOf(item to 250L), 2000, "2026-10-28", "CASH", customer, discountCents = 500)
      var totals = r.snapshot(b, "2026-10-28", "2026-10-28")
      assertEquals(4500L, totals.earnedCents)
      assertEquals(2000L, totals.receivedCents)
      assertEquals(2500L, totals.owedCents)
      r.receipt(b, customer, 1000, "2026-10-28", "CASH")
      totals = r.snapshot(b, "2026-10-28", "2026-10-28")
      assertEquals(4500L, totals.earnedCents)
      assertEquals(3000L, totals.receivedCents)
      assertEquals(1500L, totals.owedCents)
      val before = r.dao.ledgerRows(b)
      assertThrows(IllegalArgumentException::class.java) {
        r.commerce.sale(b, mapOf(item to 500L), 0, "2026-10-28", "CREDIT", customer, discountCents = 9500)
      }
      assertEquals(before, r.dao.ledgerRows(b))
      assertEquals(9750L, r.dao.commerceItem(item)!!.stockMilli)
    } finally {
      db.close()
    }
  }
}
