package com.aistudio.fasmovapos.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetailQuantityTest {
  @Test
  fun quarterHalfAndMixedQuantitiesBecomeExactBaseUnits() {
    assertEquals(250, RetailQuantity.parse("0.25", 1000))
    assertEquals(500, RetailQuantity.parse("0.5", 1000))
    assertEquals(1750, RetailQuantity.parse("1.75", 1000))
    assertEquals(50_000, RetailQuantity.parse("50", 1000))
    assertEquals("1.75 Kg", RetailQuantity.label(1750, 1000, "Kg"))
    assertEquals("250 ml", RetailQuantity.label(250, 1000, "Ltr"))
  }

  @Test
  fun unsupportedPrecisionNegativeAndOverflowAreRefused() {
    listOf("0.0001", "-1", "NaN", "Infinity", "2147484", "").forEach { assertNull(it, RetailQuantity.parse(it, 1000)) }
    assertNull(RetailQuantity.parse("0.5", 1))
    assertNull(RetailQuantity.parse("1", 100))
  }

  @Test
  fun canonicalLineAmountsAndPortionsUseDecimalRounding() {
    assertEquals(40.0, RetailQuantity.amount(160.0, 250, 1000), 0.0)
    assertEquals(80.0, RetailQuantity.amount(160.0, 500, 1000), 0.0)
    assertEquals(280.0, RetailQuantity.amount(160.0, 1750, 1000), 0.0)
    assertEquals(90.0, RetailQuantity.amount(45.0, 500, 250), 0.0)
    assertEquals(0.03, RetailQuantity.amount(0.1, 250, 1000), 0.0)
  }

  @Test
  fun moneyQuoteShowsMeasuredQuantityAndActualChargeWithinBudget() {
    val quantity = RetailQuantity.forBudget("30", 180.0, 1000)!!
    assertEquals(166, quantity)
    assertEquals(29.88, RetailQuantity.amount(180.0, quantity, 1000), 0.0)
    assertNull(RetailQuantity.forBudget("-30", 180.0, 1000))
    assertNull(RetailQuantity.forBudget("NaN", 180.0, 1000))
  }
}
