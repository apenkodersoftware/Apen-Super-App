package com.aistudio.fasmovapos.app.data.backup

import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Sales as a spreadsheet file (opens in Excel / Google Sheets) for the accountant. Contains no PINs or staff data. */
object SalesCsv {
  private const val HEADER = "Date,Receipt,Cashier,Customer,Payment method,Payment reference,Gross,Discount,VAT,Net"
  private val QUOTE_TRIGGERS = setOf(',', '"', '\n', '\r')
  private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')

  fun build(
    sales: List<SaleEntity>,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { this.timeZone = timeZone }
    val rows =
      sales.sortedBy { it.timestamp }.map { sale ->
        listOf(
          format.format(Date(sale.timestamp)),
          text(sale.transactionRef),
          text(sale.cashierName),
          text(sale.customerName.orEmpty()),
          text(sale.paymentMethod),
          text(sale.paymentReference),
          money(sale.totalAmount),
          money(sale.discountAmount),
          money(sale.taxAmount),
          money(sale.netAmount),
        ).joinToString(",")
      }
    return (listOf(HEADER) + rows).joinToString("\r\n", postfix = "\r\n")
  }

  private fun money(value: Double) = String.format(Locale.US, "%.2f", value)

  /** Quotes the cell when needed, and defuses spreadsheet formulas ("=...", "+...") that a name could smuggle in. */
  private fun text(value: String): String {
    val safe = if (value.firstOrNull() in FORMULA_STARTS) "'$value" else value
    return if (safe.any { it in QUOTE_TRIGGERS }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
  }
}
