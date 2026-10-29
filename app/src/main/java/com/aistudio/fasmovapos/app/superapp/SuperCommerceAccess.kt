package com.aistudio.fasmovapos.app.superapp

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

interface SuperCommerceAccess {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: CommerceItem)

  @Query("SELECT * FROM super_items WHERE businessId = :businessId")
  fun commerceItems(businessId: String): List<CommerceItem>

  @Query("SELECT * FROM super_items WHERE id = :id")
  fun commerceItem(id: String): CommerceItem?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: CommerceSale)

  @Query("SELECT * FROM super_sales WHERE businessId = :businessId")
  fun commerceSales(businessId: String): List<CommerceSale>

  @Query("SELECT * FROM super_sales WHERE id = :id")
  fun commerceSale(id: String): CommerceSale?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: CommerceLine)

  @Query("SELECT * FROM super_sale_lines WHERE businessId = :businessId")
  fun commerceLines(businessId: String): List<CommerceLine>

  @Query("SELECT * FROM super_sale_lines WHERE id = :id")
  fun commerceLine(id: String): CommerceLine?
}
