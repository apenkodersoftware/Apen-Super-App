package com.aistudio.fasmovapos.app.superapp

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

interface SuperRentalChargeAccess {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: TenancyRow)

  @Query("SELECT * FROM super_tenancies WHERE businessId = :businessId")
  fun tenancyRows(businessId: String): List<TenancyRow>

  @Query("SELECT * FROM super_tenancies WHERE id = :id")
  fun tenancyRow(id: String): TenancyRow?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: RentCharge)

  @Query("SELECT * FROM super_rent_charges WHERE businessId = :businessId")
  fun rentCharges(businessId: String): List<RentCharge>

  @Query("SELECT * FROM super_rent_charges WHERE id = :id")
  fun rentCharge(id: String): RentCharge?
}
