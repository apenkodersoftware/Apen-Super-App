package com.aistudio.fasmovapos.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Upgrades real databases from every shipped schema version, so an app update can never lose a shop's data. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {
  private val dbName = "migration-test"

  @get:Rule
  val helper =
    MigrationTestHelper(
      InstrumentationRegistry.getInstrumentation(),
      FasMovaPOSDatabase::class.java,
    )

  @Test
  fun migrate1To2_keepsRows_andAddsSalePayments() {
    helper.createDatabase(dbName, 1).apply {
      execSQL("INSERT INTO branches (id, name, code, location, isHeadquarters) VALUES ('b-1', 'Shop', 'HQ-01', '', 1)")
      close()
    }

    val db = helper.runMigrationsAndValidate(dbName, 2, true, FasMovaPOSDatabase.MIGRATION_1_2)

    db.query("SELECT name FROM branches WHERE id = 'b-1'").use {
      assertTrue(it.moveToFirst())
      assertEquals("Shop", it.getString(0))
    }
    db.query("SELECT COUNT(*) FROM sale_payments").use {
      assertTrue(it.moveToFirst())
      assertEquals(0, it.getInt(0))
    }
  }

  @Test
  fun migrate2To3_keepsStaff_andExistingPinsDoNotNeedChanging() {
    helper.createDatabase(dbName, 2).apply {
      execSQL(
        "INSERT INTO staff_users (id, username, fullName, role, pinHash, pinSalt, branchId, isActive) " +
          "VALUES ('u-1', 'owner', 'Joseph', 'OWNER', 'hash', 'salt', 'b-1', 1)",
      )
      close()
    }

    val db = helper.runMigrationsAndValidate(dbName, 3, true, FasMovaPOSDatabase.MIGRATION_2_3)

    db.query("SELECT fullName, pinHash, mustChangePin FROM staff_users WHERE id = 'u-1'").use {
      assertTrue(it.moveToFirst())
      assertEquals("Joseph", it.getString(0))
      assertEquals("hash", it.getString(1))
      assertEquals(0, it.getInt(2))
    }
  }

  @Test
  fun migrate3To4_keepsBranches_withEmptyReceiptDetails() {
    helper.createDatabase(dbName, 3).apply {
      execSQL("INSERT INTO branches (id, name, code, location, isHeadquarters) VALUES ('b-1', 'Shop', 'HQ-01', '', 1)")
      close()
    }

    val db = helper.runMigrationsAndValidate(dbName, 4, true, FasMovaPOSDatabase.MIGRATION_3_4)

    db.query("SELECT name, phone, taxPin FROM branches WHERE id = 'b-1'").use {
      assertTrue(it.moveToFirst())
      assertEquals("Shop", it.getString(0))
      assertEquals("", it.getString(1))
      assertEquals("", it.getString(2))
    }
  }

  @Test
  fun migrate4To5_preservesWholeItemsAndAddsExplicitScale() {
    helper.createDatabase(dbName, 4).apply {
      execSQL(
        "INSERT INTO products (id,sku,barcode,name,category,unit,costPrice,sellingPrice,stockQuantity,minStockThreshold,branchId,isSynced,updatedAt) VALUES ('p','P','','Rice','Food','Kg',100,160,10,1,'b-1',0,0)",
      )
      close()
    }
    val db = helper.runMigrationsAndValidate(dbName, 5, true, FasMovaPOSDatabase.MIGRATION_4_5)
    db.query("SELECT stockQuantity,quantityScale,isFavorite FROM products WHERE id = 'p'").use {
      assertTrue(it.moveToFirst())
      assertEquals(10, it.getInt(0))
      assertEquals(1, it.getInt(1))
      assertEquals(0, it.getInt(2))
    }
  }

  @Test
  fun migrate5To6PreservesLegacyRecordsAndNeverInventsEntryTimes() {
    helper.createDatabase(dbName, 5).apply {
      execSQL(
        "INSERT INTO products (id,sku,barcode,name,category,unit,costPrice,sellingPrice,stockQuantity,minStockThreshold,branchId,isSynced,updatedAt) VALUES ('p','P','','Rice','Food','Kg',100,160,10,1,'b-1',0,0)",
      )
      close()
    }
    val db = helper.runMigrationsAndValidate(dbName, 6, true, FasMovaPOSDatabase.MIGRATION_5_6)
    db.query("SELECT createdAt FROM record_origins WHERE entityTable='products' AND recordId='p'").use {
      assertTrue(it.moveToFirst())
      assertTrue(it.isNull(0))
    }
    db.execSQL("UPDATE products SET stockQuantity=20 WHERE id='p'")
    db.query("SELECT beforeValue,afterValue FROM recording_events WHERE entityTable='products'").use {
      assertTrue(it.moveToFirst())
      assertTrue(it.getString(0).contains("stockQuantity=10"))
      assertTrue(it.getString(1).contains("stockQuantity=20"))
    }
  }

  @Test
  fun migrateFromVersion1ToCurrent_runsTheWholeChain() {
    helper.createDatabase(dbName, 1).close()

    helper.runMigrationsAndValidate(
      dbName,
      FasMovaPOSDatabase.VERSION,
      true,
      FasMovaPOSDatabase.MIGRATION_1_2,
      FasMovaPOSDatabase.MIGRATION_2_3,
      FasMovaPOSDatabase.MIGRATION_3_4,
      FasMovaPOSDatabase.MIGRATION_4_5,
      FasMovaPOSDatabase.MIGRATION_5_6,
      FasMovaPOSDatabase.MIGRATION_6_7,
      FasMovaPOSDatabase.MIGRATION_7_8,
    )
  }
}
