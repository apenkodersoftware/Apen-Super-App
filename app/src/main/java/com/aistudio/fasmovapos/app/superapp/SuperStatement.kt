package com.aistudio.fasmovapos.app.superapp

import com.aistudio.fasmovapos.app.BuildConfig
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.ui.simple.BusinessStatement
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/** One frozen, business-scoped snapshot for the preview, PDF and versioned CSV. No party or note fields. */
fun SuperRepository.businessStatement(
  businessId: String,
  period: RecordPeriod,
): BusinessStatement =
  store.transaction {
    val profile = store.business(businessId)
    val reading = periodReading(businessId, period)
    val now = store.now()
    val id = UUID.randomUUID().toString()
    val future = dao.ledgerRows(businessId).count { !it.voided && it.occurredOn > SuperDates.today(now) }
    BusinessStatement(
      id,
      listOf(
        "schema" to "apen-business-statement/2",
        "report_id" to id,
        "business_id" to profile.id,
        "business" to profile.name,
        "business_type" to profile.type,
        "demo_workspace" to profile.demo.toString(),
        "currency" to profile.currency,
        "app_version" to BuildConfig.VERSION_NAME,
        "database_schema" to FasMovaPOSDatabase.VERSION.toString(),
        "generated_at_epoch_ms" to now.toString(),
        "timezone" to SuperDates.zone.id,
        "period_from_inclusive" to period.from,
        "period_through_inclusive" to period.through,
        "period_totals_status" to if (reading.coverageDays > 0) "recorded_figures_only" else "unknown_no_records",
        "future_dated_posted_entries_as_of_generation" to future.toString(),
      ) + statementMoneyRows(profile.type, reading) + statementCoverageRows(reading) + statementDefinitions(),
    )
  }

private fun statementMoneyRows(
  type: String,
  r: PeriodReading,
): List<Pair<String, String>> {
  val s = r.snapshot
  val known = r.coverageDays > 0

  fun money(value: Long) = if (known) statementAmount(value) else ""
  val dairy = type == "DAIRY"
  val average =
    if (dairy && r.milkDays > 0) {
      BigDecimal(s.litresMilli)
        .movePointLeft(3)
        .divide(BigDecimal(r.milkDays), 3, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    } else {
      ""
    }
  return listOf(
    "earned_sales_or_rent_charged" to money(s.earnedCents),
    "received_against_business_charges" to money(s.receivedCents),
    "recorded_expenses" to money(s.spentCents),
    "difference_before_unrecorded_costs" to money(if (type == "RENTAL") s.receivedCents - s.spentCents else s.salesLessExpensesCents),
    "difference_basis" to if (type == "RENTAL") "received_less_recorded_expenses" else "earned_or_sales_less_recorded_expenses",
    "owed_as_of_generation_all_posted_dates" to statementAmount(s.owedCents),
    "other_money_in_not_sales_in_period" to money(s.otherInCents),
    "recorded_held_money_as_of_generation_not_income" to statementAmount(s.heldCents),
    "milk_litres_delivered" to if (dairy && known) SuperMoney.quantity(s.litresMilli) else "",
    "days_with_milk_deliveries" to if (dairy) r.milkDays.toString() else "",
    "average_litres_per_recorded_delivery_day" to average,
  )
}

private fun statementCoverageRows(r: PeriodReading): List<Pair<String, String>> =
  listOf(
    "calendar_days" to r.calendarDays.toString(),
    "days_with_entries" to r.snapshot.entryDays.toString(),
    "marked_no_trade_days_without_entries" to r.snapshot.noTradeDays.toString(),
    "days_without_records_or_marker" to (r.calendarDays - r.coverageDays).toString(),
    "entries_with_unknown_original_entry_time" to r.snapshot.unknownEntryTimes.toString(),
    "financial_change_events_recorded_in_period" to r.snapshot.changeEvents.toString(),
  )

private fun statementDefinitions(): List<Pair<String, String>> =
  listOf(
    "status" to "Self-recorded; not independently verified; not a credit score or promise of finance",
    "earned_definition" to "Milk delivery value, net shop or food sales, or rent charges posted for the period; not cash received",
    "received_definition" to "Receipts against business charges, including later debt collections; non-sales money excluded",
    "owed_definition" to
      "All currently saved, non-void posted records, across all dates including future-dated entries; not a historical period-end balance",
    "coverage_definition" to
      "Missing dates are unknown, not zero. Recorded days and no-trade markers are self-reported, not proof of complete books. Blank period amounts mean no records",
    "difference_limits" to
      "Not profit or cash/wallet balance; before cost of goods sold and other unrecorded costs; no reconciled cash or tax claim",
    "held_money_definition" to "Amounts recorded as held, not income; deposit refunds are not computed automatically",
    "privacy" to
      "Aggregate business figures only; individual customer, buyer and tenant names, phones, payment references and personal notes excluded",
    "audit_limits" to
      "Local device clock and local history; original entry times may be unknown. CSV digest checks consistency, not independent verification",
  )

private fun statementAmount(cents: Long): String = BigDecimal.valueOf(cents, 2).setScale(2).toPlainString()
