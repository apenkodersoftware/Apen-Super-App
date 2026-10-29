package com.aistudio.fasmovapos.app.superapp

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SuperFoundationTest {
  @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FasMovaPOSDatabase::class.java)

  @Test fun migrationKeepsRowsAndUnknownEntryTimes() {
    val name = "super-migration"
    helper.createDatabase(name, 6).use { db ->
      db.execSQL("INSERT INTO branches VALUES ('b-1','Original shop','HQ','',1,'','')")
      db.execSQL("INSERT INTO staff_users VALUES ('owner','owner','Owner','OWNER','hash','salt','b-1',1,0)")
      db.execSQL("INSERT INTO customers VALUES ('c','Customer','','',2000,130,280,0)")
      db.execSQL("INSERT INTO products VALUES ('p','sku','','Item','General','Pcs',100,280,10,1,'b-1',0,0,1,0,0,0)")
      db.execSQL(
        "INSERT INTO sales VALUES ('s','ref','b-1','owner','Owner',280,0,0,280,'SPLIT','ref','c','Customer',1793134800000,'PENDING')",
      )
      db.execSQL("INSERT INTO sale_items VALUES (1,'s','p','Item',1,280,280,1,'Pcs',1)")
      db.execSQL("INSERT INTO sale_payments VALUES (1,'s','CASH',100,NULL,'cash')")
      db.execSQL("INSERT INTO sale_payments VALUES (2,'s','CREDIT',180,NULL,'credit')")
      db.execSQL("INSERT INTO customer_payments VALUES ('pay','c','Customer',50,'CASH','collected',1793134800001,0)")
      db.execSQL("INSERT INTO personal_loan_plans VALUES ('note','inua',30000,'2026-10-30')")
      db.execSQL("INSERT INTO record_origins VALUES ('sales','s',NULL)")
    }
    val migrated = helper.runMigrationsAndValidate(name, 7, true, FasMovaPOSDatabase.MIGRATION_6_7)
    migrated.query("SELECT currentDebt FROM customers WHERE id='c'").use {
      assertTrue(it.moveToFirst())
      assertEquals(130.0, it.getDouble(0), 0.0)
    }
    migrated.query("SELECT amountCents FROM personal_loan_plans WHERE id='note'").use {
      assertTrue(it.moveToFirst())
      assertEquals(30000L, it.getLong(0))
    }
    migrated.query("SELECT recordedAt FROM super_sales WHERE id='s'").use {
      assertTrue(it.moveToFirst())
      assertTrue(it.isNull(0))
    }
    migrated.query("SELECT sum(earnedCents),sum(receivedCents),sum(owedDeltaCents) FROM super_ledger").use {
      assertTrue(it.moveToFirst())
      assertEquals(28000L, it.getLong(0))
      assertEquals(15000L, it.getLong(1))
      assertEquals(13000L, it.getLong(2))
    }
    migrated.query("SELECT count(*) FROM super_businesses").use {
      it.moveToFirst()
      assertEquals(1, it.getInt(0))
    }
    migrated.query("SELECT stockMilli FROM super_items WHERE id='p'").use {
      it.moveToFirst()
      assertEquals(10000L, it.getLong(0))
    }
    migrated.close()
  }

  @Test fun invalidCrossBusinessItemAndOverpaymentRollbackWithoutPartialRows() {
    val db =
      Room
        .inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext<Context>(),
          FasMovaPOSDatabase::class.java,
        ).allowMainThreadQueries()
        .build()
    try {
      val repo = SuperRepository(db)
      val first = repo.createBusiness(Livelihood.RETAIL, "First").id
      val second = repo.createBusiness(Livelihood.RETAIL, "Second", true).id
      val item = repo.item(first, "Item", 28000, "General", 10000)
      val buyer = repo.addParty(first, "Buyer", "CUSTOMER").id
      assertThrows(IllegalArgumentException::class.java) { repo.sale(second, mapOf(item to 1000L), 28000, "2026-10-28", "CASH") }
      assertTrue(repo.dao.commerceSales(second).isEmpty())
      repo.sale(first, mapOf(item to 1000L), 10000, "2026-10-28", "CASH", buyer)
      val before = repo.dao.ledgerRows(first)
      assertThrows(IllegalArgumentException::class.java) { repo.receipt(first, buyer, 18001, "2026-10-28", "CASH") }
      assertEquals(before, repo.dao.ledgerRows(first))
      assertEquals(0L, repo.snapshot(second, "2026-10-01", "2026-10-31").earnedCents)
    } finally {
      db.close()
    }
  }
}
