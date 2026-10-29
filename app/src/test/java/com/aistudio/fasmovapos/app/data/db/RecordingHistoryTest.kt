package com.aistudio.fasmovapos.app.data.db

import android.app.Application
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.backup.BackupFormatException
import com.aistudio.fasmovapos.app.data.backup.BackupManager
import com.aistudio.fasmovapos.app.data.backup.OwnerRecovery
import com.aistudio.fasmovapos.app.data.companion.LoanPlan
import com.aistudio.fasmovapos.app.data.companion.LoanPlansStore
import com.aistudio.fasmovapos.app.data.db.entities.ExpenseEntity
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.repository.FasMovaPOSRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordingHistoryTest {
  private val app = ApplicationProvider.getApplicationContext<Application>()
  private lateinit var db: FasMovaPOSDatabase

  @Before fun setUp() {
    FasMovaPOSDatabase.closeAndReset()
    app.deleteDatabase(FasMovaPOSDatabase.DATABASE_NAME)
    db = FasMovaPOSDatabase.getDatabase(app)
  }

  @After fun tearDown() = FasMovaPOSDatabase.closeAndReset()

  @Test fun changedStockRetainsOriginalAndReasonInTheSameTransaction() =
    runBlocking {
      val repo = FasMovaPOSRepository(db)
      repo.saveProduct(ProductEntity("p", "P", "", "Rice", "Food", "Kg", 100.0, 150.0, 1000, 100, "b", quantityScale = 1000))
      repo.adjustProductStock("p", 500, null, null, reason = "Delivery counted")
      val event =
        db
          .recordingDao()
          .events()
          .first()
          .first { it.entityTable == "products" && it.action == "UPDATE" }
      assertTrue(event.beforeValue!!.contains("stockQuantity=1000"))
      assertTrue(event.afterValue!!.contains("stockQuantity=1500"))
      assertEquals("Delivery counted", event.reason)
      assertNotNull(
        db
          .recordingDao()
          .origins()
          .first()
          .single { it.recordId == "p" }
          .createdAt,
      )
    }

  @Test fun failedTransactionRollsBackTheRecordAndItsHistory() =
    runBlocking {
      runCatching {
        db.withTransaction {
          db.expenseDao().insertExpense(ExpenseEntity("e", "E", "b", "Rent", 100.0, "", "owner", 1L))
          error("Injected save failure")
        }
      }
      assertTrue(
        db
          .expenseDao()
          .getAllExpenses()
          .first()
          .isEmpty(),
      )
      assertTrue(
        db
          .recordingDao()
          .events()
          .first()
          .isEmpty(),
      )
      assertTrue(
        db
          .recordingDao()
          .origins()
          .first()
          .isEmpty(),
      )
    }

  @Test fun noteBackupRestoresTheSnapshotAndDoesNotResurrectLaterNotes() =
    runBlocking {
      FasMovaPOSRepository(db).completeSetup("Test shop", "Owner", "owner", "4821", false)
      val store = LoanPlansStore(app)
      val first = LoanPlan("first", "Inua Loan", 30000, "2026-10-30")
      val later = LoanPlan("later", "Afya Loan", 10000, "2026-11-02")
      store.save(first)
      val password = "review-backup-123".toCharArray()
      val backup = BackupManager(app).createBackup(password)
      store.remove("first")
      store.save(later)
      BackupManager(app).restore(backup, password)
      assertEquals(listOf(first), LoanPlansStore(app).plans.first())
      assertTrue(
        FasMovaPOSDatabase
          .getDatabase(app)
          .recordingDao()
          .events()
          .first()
          .isNotEmpty(),
      )
    }

  @Test fun ownerRecoveryNeedsTheBackupPasswordAndPreservesBackedUpRecords() =
    runBlocking {
      FasMovaPOSRepository(db).completeSetup("Test shop", "Owner", "owner", "4821", false)
      val first = LoanPlan("recover", "Inua Loan", 30000, "2026-10-30")
      LoanPlansStore(app).save(first)
      val password = "review-backup-123".toCharArray()
      val backup = BackupManager(app).createBackup(password)
      assertThrows(BackupFormatException::class.java) {
        BackupManager(app).restore(backup, "wrong-password".toCharArray(), OwnerRecovery("owner", "7392"))
      }
      assertNotNull(FasMovaPOSRepository(db).signIn("owner", "4821"))
      assertThrows(BackupFormatException::class.java) {
        BackupManager(app).restore(backup, password, OwnerRecovery("not-the-owner", "7392"))
      }
      BackupManager(app).restore(backup, password, OwnerRecovery("owner", "7392"))
      val restored = FasMovaPOSRepository(FasMovaPOSDatabase.getDatabase(app))
      assertNotNull(restored.signIn("owner", "7392"))
      assertNull(restored.signIn("owner", "4821"))
      assertEquals(listOf(first), LoanPlansStore(app).plans.first())
    }

  @Test fun statementReadsPersistedRecordsWithoutWaitingForScreenFlows() =
    runBlocking {
      val branch = com.aistudio.fasmovapos.app.data.repository.HQ_BRANCH_ID
      FasMovaPOSRepository(db).completeSetup("Snapshot shop", "Owner", "owner", "4821", false)
      FasMovaPOSRepository(db).addExpense(ExpenseEntity("snapshot-expense", "E", branch, "Transport", 30.0, "Review", "Owner"))
      val statement =
        com.aistudio.fasmovapos.app.ui.simple.loadBusinessStatement(
          db,
          branch,
          "Snapshot shop",
          com.aistudio.fasmovapos.app.ui.simple.ReportPeriod.DAY,
        )
      assertEquals("30.00", statement.rows.toMap().getValue("recorded_expenses"))
      assertEquals("0", statement.rows.toMap().getValue("entries_with_unknown_original_entry_time"))
    }
}
