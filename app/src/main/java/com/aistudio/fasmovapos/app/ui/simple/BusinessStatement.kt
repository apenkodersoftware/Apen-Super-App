package com.aistudio.fasmovapos.app.ui.simple

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.aistudio.fasmovapos.app.BuildConfig
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/** Immutable preview/export snapshot. No individual customer, cashier, telephone or payment-reference fields. */
data class BusinessStatement(
  val id: String,
  val rows: List<Pair<String, String>>,
) {
  fun csv(): String =
    (
      listOf("field,value") +
        rows.map { (key, value) ->
          val numericDifference = key == "difference_before_unrecorded_costs" && value.matches(Regex("-?[0-9]+\\.[0-9]{2}"))
          "${cell(key)},${cell(value, numericDifference)}"
        }
    ).joinToString("\r\n", postfix = "\r\n")

  fun digest(): String =
    MessageDigest.getInstance("SHA-256").digest(csv().toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

  fun pdf(lines: List<String>): ByteArray {
    val document = PdfDocument()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
    val wrapped = lines.flatMap { wrap(it, paint) }
    try {
      wrapped.chunked(43).forEachIndexed { index, pageLines ->
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, index + 1).create())
        pageLines.forEachIndexed { lineIndex, line -> page.canvas.drawText(line, 36f, 46f + lineIndex * 17f, paint) }
        page.canvas.drawText("Apen | ${index + 1} | $id", 36f, 813f, paint)
        document.finishPage(page)
      }
      return ByteArrayOutputStream().use {
        document.writeTo(it)
        it.toByteArray()
      }
    } finally {
      document.close()
    }
  }

  companion object {
    fun create(
      business: String,
      comparison: PeriodComparison,
      coverage: RecordCoverage,
      now: Long,
      zone: TimeZone = TimeZone.getDefault(),
    ): BusinessStatement {
      val r = comparison.current

      fun amount(value: Double) = String.format(Locale.US, "%.2f", value)
      val id = UUID.randomUUID().toString()
      return BusinessStatement(
        id,
        listOf(
          "schema" to "apen-business-statement/1",
          "report_id" to id,
          "business" to business,
          "currency" to "KES",
          "app_version" to BuildConfig.VERSION_NAME,
          "generated_at_epoch_ms" to now.toString(),
          "timezone" to zone.id,
          "period_start_inclusive_epoch_ms" to comparison.currentWindow.start.toString(),
          "period_end_exclusive_epoch_ms" to comparison.currentWindow.end.toString(),
          "sales_after_discounts" to amount(r.sales),
          "received_payments_including_old_debt_collections" to amount(r.received),
          "recorded_expenses" to amount(r.expenses),
          "sales_less_recorded_expenses_before_cogs_not_profit" to amount(r.estimate),
          "credit_issued_in_period" to amount(r.credit),
          "old_debt_collections_in_period" to amount(r.collected),
          "outstanding_customer_debt_as_of_generation_not_period_end" to amount(r.outstanding),
          "days_with_entries" to coverage.entryDays.toString(),
          "marked_no_trade_days_without_entries" to coverage.closedDays.toString(),
          "days_without_records_or_no_trade_marker" to coverage.unknownDays.toString(),
          "entries_with_unknown_original_entry_time" to coverage.legacyEntries.toString(),
          "financial_change_events_recorded_in_period" to coverage.corrections.toString(),
          "status" to "Self-recorded; not independently verified; not a credit score or promise of finance",
          "limits" to
            "Partial current period; missing records are unknown, not zero; no historical cost of goods sold or reconciled cash balance",
          "privacy" to "Aggregate business figures only; individual customer names, phones and payment references excluded",
          "audit_limits" to
            "Local device clock and local history; edits may be legitimate; original entry times unavailable for legacy records",
        ),
      )
    }

    private fun cell(
      value: String,
      numeric: Boolean = false,
    ): String {
      val safe = if (!numeric && value.firstOrNull() in setOf('=', '+', '-', '@', '\t', '\r')) "'$value" else value
      return "\"" + safe.replace("\"", "\"\"") + "\""
    }

    private fun wrap(
      text: String,
      paint: Paint,
    ): List<String> {
      if (text.isEmpty()) return listOf("")
      val lines = mutableListOf<String>()
      var rest = text
      while (rest.isNotEmpty()) {
        val count = paint.breakText(rest, true, 520f, null).coerceAtLeast(1)
        val space = rest.lastIndexOf(' ', count.coerceAtMost(rest.lastIndex))
        val end = if (count < rest.length && space > 0) space else count
        lines += rest.take(end)
        rest = rest.drop(end).trimStart()
      }
      return lines
    }
  }
}
