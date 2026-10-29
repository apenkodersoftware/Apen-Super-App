package com.aistudio.fasmovapos.app.ui.simple

import org.junit.Assert.assertEquals
import org.junit.Test

class StockProjectionTest {
  @Test
  fun lowStockComesFirstEvenWhenAHealthyItemHasFewerPieces() {
    val healthy = simpleProduct("healthy", "Healthy", stock = 3).copy(minStockThreshold = 2)
    val low = simpleProduct("low", "Low", stock = 8).copy(minStockThreshold = 10)
    val empty = simpleProduct("empty", "Empty", stock = 0)
    val original = listOf(healthy, low, empty)
    assertEquals(listOf("empty", "low", "healthy"), sortedStock(original).map { it.id })
    assertEquals(listOf(3, 8, 0), original.map { it.stockQuantity })
  }
}
