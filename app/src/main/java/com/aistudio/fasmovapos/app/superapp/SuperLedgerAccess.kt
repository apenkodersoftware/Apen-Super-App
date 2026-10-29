package com.aistudio.fasmovapos.app.superapp

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

interface SuperLedgerAccess {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: ExpenseRow)

  @Query("SELECT * FROM super_expenses WHERE businessId = :businessId")
  fun expenseRows(businessId: String): List<ExpenseRow>

  @Query("SELECT * FROM super_expenses WHERE id = :id")
  fun expenseRow(id: String): ExpenseRow?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: MoneyInRow)

  @Query("SELECT * FROM super_money_in WHERE businessId = :businessId")
  fun moneyInRows(businessId: String): List<MoneyInRow>

  @Query("SELECT * FROM super_money_in WHERE id = :id")
  fun moneyInRow(id: String): MoneyInRow?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: LedgerRow)

  @Query("SELECT * FROM super_ledger WHERE businessId = :businessId")
  fun ledgerRows(businessId: String): List<LedgerRow>

  @Query("SELECT * FROM super_ledger WHERE id = :id")
  fun ledgerRow(id: String): LedgerRow?
}
