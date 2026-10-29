package com.aistudio.fasmovapos.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aistudio.fasmovapos.app.data.db.dao.BranchDao
import com.aistudio.fasmovapos.app.data.db.dao.CustomerDao
import com.aistudio.fasmovapos.app.data.db.dao.ExpenseDao
import com.aistudio.fasmovapos.app.data.db.dao.ProductDao
import com.aistudio.fasmovapos.app.data.db.dao.PurchaseOrderDao
import com.aistudio.fasmovapos.app.data.db.dao.SaleDao
import com.aistudio.fasmovapos.app.data.db.dao.StaffUserDao
import com.aistudio.fasmovapos.app.data.db.dao.SupplierDao
import com.aistudio.fasmovapos.app.data.db.dao.SyncQueueDao
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity
import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.CustomerPaymentEntity
import com.aistudio.fasmovapos.app.data.db.entities.ExpenseEntity
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.db.entities.PurchaseOrderEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleItemEntity
import com.aistudio.fasmovapos.app.data.db.entities.SalePaymentEntity
import com.aistudio.fasmovapos.app.data.db.entities.StaffUserEntity
import com.aistudio.fasmovapos.app.data.db.entities.SupplierEntity
import com.aistudio.fasmovapos.app.data.db.entities.SyncQueueEntity
import com.aistudio.fasmovapos.app.data.records.LoanPlanDao
import com.aistudio.fasmovapos.app.data.records.LoanPlanRow
import com.aistudio.fasmovapos.app.data.records.RecordOrigin
import com.aistudio.fasmovapos.app.data.records.RecordedDay
import com.aistudio.fasmovapos.app.data.records.RecordingDao
import com.aistudio.fasmovapos.app.data.records.RecordingEvent
import com.aistudio.fasmovapos.app.data.records.RecordingMeta
import com.aistudio.fasmovapos.app.data.records.RecordingSchema
import com.aistudio.fasmovapos.app.superapp.AllocationRow
import com.aistudio.fasmovapos.app.superapp.BusinessProfile
import com.aistudio.fasmovapos.app.superapp.ChargeRow
import com.aistudio.fasmovapos.app.superapp.CommerceItem
import com.aistudio.fasmovapos.app.superapp.CommerceLine
import com.aistudio.fasmovapos.app.superapp.CommerceSale
import com.aistudio.fasmovapos.app.superapp.DeductionRow
import com.aistudio.fasmovapos.app.superapp.ExpenseRow
import com.aistudio.fasmovapos.app.superapp.HistoryRow
import com.aistudio.fasmovapos.app.superapp.LedgerRow
import com.aistudio.fasmovapos.app.superapp.MilkDelivery
import com.aistudio.fasmovapos.app.superapp.MoneyInRow
import com.aistudio.fasmovapos.app.superapp.NoTradeRow
import com.aistudio.fasmovapos.app.superapp.Party
import com.aistudio.fasmovapos.app.superapp.PropertyRow
import com.aistudio.fasmovapos.app.superapp.ReceiptRow
import com.aistudio.fasmovapos.app.superapp.RentCharge
import com.aistudio.fasmovapos.app.superapp.RentalUnitRow
import com.aistudio.fasmovapos.app.superapp.SuperDao
import com.aistudio.fasmovapos.app.superapp.SuperSchema
import com.aistudio.fasmovapos.app.superapp.TenancyRow

@Database(
  entities = [
    BusinessProfile::class,
    Party::class,
    MilkDelivery::class,
    PropertyRow::class,
    RentalUnitRow::class,
    TenancyRow::class,
    RentCharge::class,
    CommerceItem::class,
    CommerceSale::class,
    CommerceLine::class,
    ChargeRow::class,
    ReceiptRow::class,
    DeductionRow::class,
    AllocationRow::class,
    ExpenseRow::class,
    MoneyInRow::class,
    LedgerRow::class,
    HistoryRow::class,
    NoTradeRow::class,

    RecordingEvent::class,
    RecordOrigin::class,
    LoanPlanRow::class,
    RecordingMeta::class,
    RecordedDay::class,
    ProductEntity::class,
    SaleEntity::class,
    SaleItemEntity::class,
    SalePaymentEntity::class,
    SupplierEntity::class,
    PurchaseOrderEntity::class,
    ExpenseEntity::class,
    CustomerEntity::class,
    CustomerPaymentEntity::class,
    BranchEntity::class,
    StaffUserEntity::class,
    SyncQueueEntity::class,
  ],
  version = FasMovaPOSDatabase.VERSION,
  exportSchema = true,
)
abstract class FasMovaPOSDatabase : RoomDatabase() {
  abstract fun superDao(): SuperDao

  abstract fun loanPlanDao(): LoanPlanDao

  abstract fun recordingDao(): RecordingDao

  abstract fun productDao(): ProductDao

  abstract fun saleDao(): SaleDao

  abstract fun supplierDao(): SupplierDao

  abstract fun purchaseOrderDao(): PurchaseOrderDao

  abstract fun expenseDao(): ExpenseDao

  abstract fun customerDao(): CustomerDao

  abstract fun branchDao(): BranchDao

  abstract fun staffUserDao(): StaffUserDao

  abstract fun syncQueueDao(): SyncQueueDao

  companion object {
    const val VERSION = 8
    internal val MIGRATION_7_8 =
      object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
          com.aistudio.fasmovapos.app.superapp.SuperPricingMigration
            .migrate(db)
        }
      }
    internal val MIGRATION_6_7 =
      object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
          SuperSchema.migrate(db)
        }
      }
    const val DATABASE_NAME = "fasmovalite_database"

    internal val MIGRATION_5_6 =
      object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
          RecordingSchema.create(db)
        }
      }

    /** Existing values retain their original whole-unit meaning; Lite loose goods opt into scale 1000. */
    internal val MIGRATION_4_5 =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE products ADD COLUMN quantityScale INTEGER NOT NULL DEFAULT 1")
          db.execSQL("ALTER TABLE products ADD COLUMN quarterPrice REAL NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE products ADD COLUMN halfPrice REAL NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE products ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE sale_items ADD COLUMN quantityScale INTEGER NOT NULL DEFAULT 1")
          db.execSQL("ALTER TABLE sale_items ADD COLUMN quantityUnit TEXT NOT NULL DEFAULT 'Pcs'")
          db.execSQL("ALTER TABLE sale_items ADD COLUMN pricingQuantity INTEGER NOT NULL DEFAULT 1")
        }
      }

    /** v2 adds per-leg payments (split payments, Airtel Money). Existing sales are kept as-is. */
    internal val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS sale_payments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
              "saleId TEXT NOT NULL, method TEXT NOT NULL, amount REAL NOT NULL, phoneNumber TEXT, " +
              "reference TEXT NOT NULL)",
          )
          db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_payments_saleId ON sale_payments (saleId)")
        }
      }

    /** v3 adds the "must change PIN" flag for staff who still hold a temporary PIN. Existing staff are unaffected. */
    internal val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE staff_users ADD COLUMN mustChangePin INTEGER NOT NULL DEFAULT 0")
        }
      }

    /** v4 adds the optional business phone and tax PIN that receipts print. Existing branches get empty values. */
    internal val MIGRATION_3_4 =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE branches ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE branches ADD COLUMN taxPin TEXT NOT NULL DEFAULT ''")
        }
      }

    @Volatile
    private var dbInstance: FasMovaPOSDatabase? = null

    /** Closes the shared instance so the database file can be replaced; the next getDatabase() reopens it. */
    fun closeAndReset() =
      synchronized(this) {
        dbInstance?.close()
        dbInstance = null
      }

    fun getDatabase(context: Context): FasMovaPOSDatabase =
      dbInstance ?: synchronized(this) {
        val instance =
          Room
            .databaseBuilder(
              context.applicationContext,
              FasMovaPOSDatabase::class.java,
              DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .addCallback(
              object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                  RecordingSchema.create(db)
                  SuperSchema.create(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                  db.execSQL("PRAGMA recursive_triggers=ON")
                  RecordingSchema.installTriggers(db)
                }
              },
            ).build()
        dbInstance = instance
        instance
      }
  }
}
