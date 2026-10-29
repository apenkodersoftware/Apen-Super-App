package com.aistudio.fasmovapos.app.data.repository

import java.math.BigDecimal
import java.math.RoundingMode

/** Quantities are whole base units (pieces, grams or millilitres), never floating point. */
data class ProductMeasure(
  val scale: Int = 1,
  val quarterPrice: Double = 0.0,
  val halfPrice: Double = 0.0,
  val unit: String = "Pcs",
)

object RetailQuantity {
  const val MEASURE_SCALE = 1000

  fun parse(
    value: String,
    scale: Int,
  ): Int? =
    try {
      require(scale == 1 || scale == MEASURE_SCALE)
      value
        .trim()
        .toBigDecimal()
        .multiply(scale.toBigDecimal())
        .intValueExact()
        .takeIf { it >= 0 }
    } catch (_: IllegalArgumentException) {
      null
    } catch (_: ArithmeticException) {
      null
    }

  fun decimal(
    base: Int,
    scale: Int,
  ): String =
    base
      .toBigDecimal()
      .divide(scale.toBigDecimal())
      .stripTrailingZeros()
      .toPlainString()

  fun label(
    base: Int,
    scale: Int,
    unit: String,
  ): String =
    when {
      scale == MEASURE_SCALE && base < MEASURE_SCALE -> "$base ${if (unit == "Kg") "g" else "ml"}"
      else -> "${decimal(base, scale)} $unit"
    }

  fun amount(
    price: Double,
    quantity: Int,
    pricingQuantity: Int,
  ): Double {
    require(price.isFinite() && price >= 0 && quantity >= 0 && pricingQuantity > 0)
    return BigDecimal
      .valueOf(price)
      .multiply(quantity.toBigDecimal())
      .divide(pricingQuantity.toBigDecimal(), 2, RoundingMode.HALF_UP)
      .toDouble()
  }

  /** A money quote never silently gives more than the requested budget at the proportional rate. */
  fun forBudget(
    budget: String,
    price: Double,
    scale: Int,
  ): Int? =
    try {
      require(price.isFinite() && price > 0)
      budget
        .toBigDecimal()
        .multiply(scale.toBigDecimal())
        .divide(BigDecimal.valueOf(price), 0, RoundingMode.DOWN)
        .intValueExact()
        .takeIf { it > 0 }
    } catch (_: IllegalArgumentException) {
      null
    } catch (_: ArithmeticException) {
      null
    }
}
