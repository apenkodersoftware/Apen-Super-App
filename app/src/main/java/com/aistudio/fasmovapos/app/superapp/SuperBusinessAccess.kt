package com.aistudio.fasmovapos.app.superapp

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

interface SuperBusinessAccess {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: BusinessProfile)

  @Query("SELECT * FROM super_businesses ORDER BY demo, name")
  fun businesses(): List<BusinessProfile>

  @Query("SELECT * FROM super_businesses ORDER BY demo, name")
  fun observeBusinesses(): Flow<List<BusinessProfile>>

  @Query("SELECT * FROM super_businesses WHERE id = :id")
  fun businessProfile(id: String): BusinessProfile?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: Party)

  @Query("SELECT * FROM super_parties WHERE businessId = :businessId")
  fun partys(businessId: String): List<Party>

  @Query("SELECT * FROM super_parties WHERE id = :id")
  fun party(id: String): Party?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: MilkDelivery)

  @Query("SELECT * FROM super_milk WHERE businessId = :businessId")
  fun milkDeliverys(businessId: String): List<MilkDelivery>

  @Query("SELECT * FROM super_milk WHERE id = :id")
  fun milkDelivery(id: String): MilkDelivery?
}
