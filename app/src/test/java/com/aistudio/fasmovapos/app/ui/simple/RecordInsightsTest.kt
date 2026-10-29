package com.aistudio.fasmovapos.app.ui.simple

import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.records.RecordOrigin
import com.aistudio.fasmovapos.app.data.records.RecordedDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RecordInsightsTest {
  private val zone = TimeZone.getTimeZone("Africa/Nairobi")

  private fun at(
    day: Int,
    hour: Int = 12,
  ): Long =
    Calendar
      .getInstance(zone)
      .apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, 0)
      }.timeInMillis

  private fun sale(
    id: String,
    day: Int,
    amount: Double = 250.0,
  ) = SaleEntity(id, id, "b", "u", "PRIVATE CASHIER", amount, 0.0, 0.0, amount, "CASH", "SECRET REF", "c", "PRIVATE CUSTOMER", at(day))

  private fun input(vararg rows: SaleEntity) = ReportInput(rows.toList(), emptyList(), emptyList(), emptyList(), emptyList())

  @Test fun noPreviousRecordsAreUnknownRatherThanZeroGrowth() {
    val comparison = comparePeriods(input(sale("new", 28)), ReportPeriod.DAY, at(28, 18), zone)
    assertFalse(comparison.hasPreviousRecords)
    assertEquals(250.0, comparison.current.sales, 0.0)
  }

  @Test fun onlyComparableElapsedPortionsAreIncludedAndFutureRecordsAreExcluded() {
    val data = input(sale("old", 27, 100.0), sale("new", 28), sale("future", 29, 9000.0))
    val comparison = comparePeriods(data, ReportPeriod.DAY, at(28, 18), zone)
    assertTrue(comparison.hasPreviousRecords)
    assertEquals(at(28, 18) + 1, comparison.currentWindow.end)
    assertEquals(at(27, 18) + 1, comparison.previousWindow.end)
    assertEquals(250.0, comparison.current.sales, 0.0)
    assertEquals(100.0, comparison.previous.sales, 0.0)
  }

  @Test fun closedUnknownAndRecordedDaysRemainSeparateAndEntriesOverrideClosedMarker() {
    val data = input(sale("s", 28))
    val context =
      RecordContext(
        origins = listOf(RecordOrigin("sales", "s", at(28))),
        days = listOf(RecordedDay("2026-10-27", true), RecordedDay("2026-10-28", true)),
      )
    val c = recordingCoverage(data, context, comparePeriods(data, ReportPeriod.WEEK, at(28, 18), zone).currentWindow, zone)
    assertEquals(1, c.entryDays)
    assertEquals(1, c.closedDays)
    assertEquals(1, c.unknownDays)
    assertEquals(0, c.legacyEntries)
  }

  @Test fun legacyEntryTimeIsNeverInvented() {
    val data = input(sale("legacy", 28))
    val coverage = recordingCoverage(data, RecordContext(), reportWindow(ReportPeriod.DAY, at(28), zone), zone)
    assertEquals(1, coverage.legacyEntries)
  }

  @Test fun leapYearAndShortMonthsKeepValidCalendarComparisonDates() {
    val now =
      Calendar
        .getInstance(zone)
        .apply {
          clear()
          set(2028, Calendar.FEBRUARY, 29, 13, 0)
        }.timeInMillis
    val data = input()
    val year = comparePeriods(data, ReportPeriod.YEAR, now, zone)
    assertEquals("2027-02-28", localDate(year.previousWindow.end - 1, zone))
    val march =
      Calendar
        .getInstance(zone)
        .apply {
          clear()
          set(2026, Calendar.MARCH, 31, 13, 0)
        }.timeInMillis
    val month = comparePeriods(data, ReportPeriod.MONTH, march, zone)
    assertEquals("2026-02-28", localDate(month.previousWindow.end - 1, zone))
  }

  @Test fun aggregateExportExcludesIndividualIdentitiesAndReferencesAndLabelsLimits() {
    val data = input(sale("s", 28))
    val comparison = comparePeriods(data, ReportPeriod.DAY, at(28, 18), zone)
    val statement =
      BusinessStatement.create(
        "=Dangerous shop",
        comparison,
        recordingCoverage(data, RecordContext(), comparison.currentWindow, zone),
        at(28, 18),
        zone,
      )
    val csv = statement.csv()
    assertFalse(csv.contains("PRIVATE CUSTOMER"))
    assertFalse(csv.contains("PRIVATE CASHIER"))
    assertFalse(csv.contains("SECRET REF"))
    assertTrue(csv.contains("\"'=Dangerous shop\""))
    assertTrue(csv.contains("not independently verified"))
    assertTrue(csv.contains("not_profit"))
    assertTrue(csv.contains("not_period_end"))
    assertEquals(64, statement.digest().length)
    assertFalse(statement.digest() == statement.copy(rows = statement.rows + ("altered" to "yes")).digest())
  }

  @Test fun expensesAloneDoNotInventZeroSalesInThePreviousPeriod() {
    val data =
      input(
        sale("new", 28),
      ).copy(
        expenses =
          listOf(
            com.aistudio.fasmovapos.app.data.db.entities
              .ExpenseEntity("e", "E", "b", "Rent", 30.0, "", "Owner", at(27)),
          ),
      )
    assertFalse(comparePeriods(data, ReportPeriod.DAY, at(28, 18), zone).hasPreviousRecords)
  }
}
