package com.aistudio.fasmovapos.app.ui.simple

import androidx.room.withTransaction
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import kotlinx.coroutines.flow.first

/** A preview is built from one database transaction, not independently loading screen projections. */
suspend fun loadBusinessStatement(
  db: FasMovaPOSDatabase,
  branchId: String,
  businessName: String,
  period: ReportPeriod,
): BusinessStatement =
  db.withTransaction {
    val data =
      ReportInput(
        db
          .saleDao()
          .getAllSales()
          .first()
          .filter { it.branchId == branchId },
        db.saleDao().getAllSalePayments().first(),
        db
          .expenseDao()
          .getAllExpenses()
          .first()
          .filter { it.branchId == branchId },
        db.customerDao().getAllCustomerPayments().first(),
        db.customerDao().getAllCustomers().first(),
      )
    val context = RecordContext(db.recordingDao().events().first(), db.recordingDao().origins().first(), db.recordingDao().days().first())
    val now = System.currentTimeMillis()
    val comparison = comparePeriods(data, period, now)
    BusinessStatement.create(businessName, comparison, recordingCoverage(data, context, comparison.currentWindow), now)
  }
