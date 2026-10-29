package com.aistudio.fasmovapos.app.superapp

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

interface SuperRentalPropertyAccess {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: PropertyRow)

  @Query("SELECT * FROM super_properties WHERE businessId = :businessId")
  fun propertyRows(businessId: String): List<PropertyRow>

  @Query("SELECT * FROM super_properties WHERE id = :id")
  fun propertyRow(id: String): PropertyRow?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: RentalUnitRow)

  @Query("SELECT * FROM super_units WHERE businessId = :businessId")
  fun rentalUnitRows(businessId: String): List<RentalUnitRow>

  @Query("SELECT * FROM super_units WHERE id = :id")
  fun rentalUnitRow(id: String): RentalUnitRow?
}
