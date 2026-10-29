package com.aistudio.fasmovapos.app.data.companion

import org.json.JSONObject
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

data class LoanProduct(
  val id: String,
  val category: String,
  val name: String,
  val range: String,
  val tenor: String,
)

object InukaCatalogue {
  const val SOURCE = "https://inukaafrica.com/products.php"
  const val CONTACT = "https://inukaafrica.com/contact.php"
  const val REVIEW_DATE = "2026-10-28"

  fun decode(json: String): List<LoanProduct> {
    val products = JSONObject(json).getJSONArray("products")
    return List(products.length()) { index ->
      val item = products.getJSONObject(index)
      val name = item.getString("name")
      LoanProduct(
        id = name,
        category = item.getString("category"),
        name = name,
        range = item.getString("amount_label"),
        tenor = item.getString("published_tenor"),
      )
    }
  }
}

data class LoanPlan(
  val id: String,
  val productId: String,
  val amountCents: Long,
  val dueDate: String,
)

object LoanPlanInput {
  fun amountCents(text: String): Long? =
    try {
      val value = text.trim()
      if (!Regex("[0-9]{1,10}(\\.[0-9]{1,2})?").matches(value)) {
        null
      } else {
        BigDecimal(value).movePointRight(2).longValueExact().takeIf { it > 0 }
      }
    } catch (error: ArithmeticException) {
      throw IllegalArgumentException("Amount is outside the supported range", error)
    }

  fun validDate(text: String): Boolean {
    if (!Regex("20[0-9]{2}-[0-9]{2}-[0-9]{2}").matches(text)) return false
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    return runCatching { format.parse(text)?.let { format.format(it) == text } == true }.getOrDefault(false)
  }

  fun amountLabel(cents: Long): String = "KES " + BigDecimal.valueOf(cents, 2).toPlainString()
}
