package com.aistudio.fasmovapos.app.superapp

import android.content.Context
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.ui.simple.BusinessStatement
import com.aistudio.fasmovapos.app.ui.simple.StatementText
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date

fun superStatementText(
  context: Context,
  statement: BusinessStatement,
): StatementText {
  val rows = statement.rows.toMap()

  fun value(key: String) = rows.getValue(key)

  fun text(key: String) = translated(context, key)

  fun money(key: String): String =
    value(key).takeIf { it.isNotBlank() }?.let {
      SuperMoney.label(BigDecimal(it).movePointRight(2).longValueExact())
    } ?: "-"
  val locale = context.resources.configuration.locales[0]

  fun date(value: String) =
    SimpleDateFormat("EEE d MMM yyyy", locale)
      .apply { timeZone = SuperDates.zone }
      .format(requireNotNull(SuperDates.formatter("yyyy-MM-dd").parse(value)))
  val generated =
    SimpleDateFormat("EEE d MMM yyyy HH:mm", locale)
      .apply { timeZone = SuperDates.zone }
      .format(Date(value("generated_at_epoch_ms").toLong()))
  val type = value("business_type")
  val rental = type == "RENTAL"
  val earnedLabel =
    when (type) {
      "DAIRY" -> "earned"
      "RENTAL" -> "expected"
      else -> "sales"
    }
  val lines =
    mutableListOf(
      text("statement"),
      value("business"),
      text(type.lowercase()),
      text("self_recorded"),
      text("period_from") + ": " + date(value("period_from_inclusive")) + " - " + date(value("period_through_inclusive")),
      context.getString(R.string.r2_generated, generated, value("timezone")),
    )
  if (value("demo_workspace") == "true") lines += text("demo_banner")
  lines +=
    listOf(
      text(earnedLabel) + ": " + money("earned_sales_or_rent_charged"),
      text("statement_earned_definition"),
      text("received") + ": " + money("received_against_business_charges"),
      text("spent") + ": " + money("recorded_expenses"),
      text(if (rental) "collected_less_expenses" else "sales_less_expenses") + ": " + money("difference_before_unrecorded_costs"),
      text(if (rental) "rental_difference_help" else "before_cogs"),
      text("owed_as_now") + ": " + money("owed_as_of_generation_all_posted_dates"),
      text("owed_as_now_help"),
      text("received_limit"),
      text("other_money") + ": " + money("other_money_in_not_sales_in_period"),
      text("other_money_help"),
      text("held_recorded") + ": " + money("recorded_held_money_as_of_generation_not_income"),
      text("held_recorded_help"),
    )
  val future = value("future_dated_posted_entries_as_of_generation").toInt()
  if (future > 0) lines += context.getString(superLabelIds.getValue("future_posted"), future)
  if (type == "DAIRY") {
    lines +=
      listOf(
        text("litres") + ": " + value("milk_litres_delivered").ifBlank { "-" } + " L",
        text("average_recorded_day") + ": " + value("average_litres_per_recorded_delivery_day").ifBlank { "-" } + " L",
        text("average_day_help"),
      )
  }
  lines +=
    listOf(
      context.getString(
        R.string.r2_coverage_counts,
        value("days_with_entries").toInt(),
        value("marked_no_trade_days_without_entries").toInt(),
        value("days_without_records_or_marker").toInt(),
      ),
      text("coverage_help"),
      text("unknown_entry_times") + ": " + value("entries_with_unknown_original_entry_time"),
      context.getString(R.string.r2_changes, value("financial_change_events_recorded_in_period").toInt()),
      text("statement_private"),
      text("report_id") + ": " + statement.id,
      "Apen " + value("app_version") + " | " + value("schema"),
      context.getString(R.string.r2_digest_notice),
      "SHA-256 (CSV): " + statement.digest(),
    )
  return StatementText(lines)
}
