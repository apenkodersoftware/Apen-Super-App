package com.aistudio.fasmovapos.app.superapp

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

interface SuperSettlementAccess {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: DeductionRow)

  @Query("SELECT * FROM super_deductions WHERE businessId = :businessId")
  fun deductionRows(businessId: String): List<DeductionRow>

  @Query("SELECT * FROM super_deductions WHERE id = :id")
  fun deductionRow(id: String): DeductionRow?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: AllocationRow)

  @Query("SELECT * FROM super_allocations WHERE businessId = :businessId")
  fun allocationRows(businessId: String): List<AllocationRow>

  @Query("SELECT * FROM super_allocations WHERE id = :id")
  fun allocationRow(id: String): AllocationRow?
}
