package com.aistudio.fasmovapos.app.superapp

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

interface SuperHistoryAccess {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: HistoryRow)

  @Query("SELECT * FROM super_history WHERE businessId = :businessId")
  fun historyRows(businessId: String): List<HistoryRow>

  @Query("SELECT * FROM super_history WHERE id = :id")
  fun historyRow(id: String): HistoryRow?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun put(row: NoTradeRow)

  @Query("SELECT * FROM super_no_trade WHERE businessId = :businessId")
  fun noTradeRows(businessId: String): List<NoTradeRow>

  @Query("SELECT * FROM super_no_trade WHERE id = :id")
  fun noTradeRow(id: String): NoTradeRow?
}
