package com.aistudio.fasmovapos.app.superapp

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

interface SuperAccountsAccess {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: ChargeRow)

  @Query("SELECT * FROM super_charges WHERE businessId = :businessId")
  fun chargeRows(businessId: String): List<ChargeRow>

  @Query("SELECT * FROM super_charges WHERE id = :id")
  fun chargeRow(id: String): ChargeRow?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: ReceiptRow)

  @Query("SELECT * FROM super_receipts WHERE businessId = :businessId")
  fun receiptRows(businessId: String): List<ReceiptRow>

  @Query("SELECT * FROM super_receipts WHERE id = :id")
  fun receiptRow(id: String): ReceiptRow?
}
