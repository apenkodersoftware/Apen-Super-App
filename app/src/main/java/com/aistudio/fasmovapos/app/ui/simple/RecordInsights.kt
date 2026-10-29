package com.aistudio.fasmovapos.app.ui.simple

import com.aistudio.fasmovapos.app.data.records.RecordOrigin
import com.aistudio.fasmovapos.app.data.records.RecordedDay
import com.aistudio.fasmovapos.app.data.records.RecordingEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class RecordContext(
  val events: List<RecordingEvent> = emptyList(),
  val origins: List<RecordOrigin> = emptyList(),
  val days: List<RecordedDay> = emptyList(),
)

data class RecordCoverage(
  val entryDays: Int,
  val closedDays: Int,
  val unknownDays: Int,
  val legacyEntries: Int,
  val corrections: Int,
)

data class PeriodComparison(
  val currentWindow: ReportWindow,
  val previousWindow: ReportWindow,
  val current: BusinessReport,
  val previous: BusinessReport,
  val hasPreviousRecords: Boolean,
)

fun localDate(
  time: Long,
  zone: TimeZone = TimeZone.getDefault(),
): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.format(Date(time))

/** Current period to now, compared with the previous calendar period through its corresponding local time. */
fun comparePeriods(
  data: ReportInput,
  period: ReportPeriod,
  now: Long,
  zone: TimeZone = TimeZone.getDefault(),
): PeriodComparison {
  val previousNow =
    Calendar
      .getInstance(zone)
      .apply {
        timeInMillis = now
        add(
          when (period) {
            ReportPeriod.DAY -> Calendar.DAY_OF_MONTH
            ReportPeriod.WEEK -> Calendar.WEEK_OF_YEAR
            ReportPeriod.MONTH -> Calendar.MONTH
            ReportPeriod.YEAR -> Calendar.YEAR
          },
          -1,
        )
      }.timeInMillis
  val currentWindow = reportWindow(period, now, zone).let { it.copy(end = minOf(it.end, now + 1)) }
  val previousWindow = reportWindow(period, previousNow, zone).let { it.copy(end = minOf(it.end, previousNow + 1)) }
  return PeriodComparison(
    currentWindow,
    previousWindow,
    businessReport(data, currentWindow),
    businessReport(data, previousWindow),
    data.sales.any { previousWindow.contains(it.timestamp) },
  )
}

fun recordingCoverage(
  data: ReportInput,
  context: RecordContext,
  window: ReportWindow,
  zone: TimeZone = TimeZone.getDefault(),
): RecordCoverage {
  val dates = mutableSetOf<String>()
  val day =
    Calendar.getInstance(zone).apply {
      timeInMillis = window.start
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
  while (day.timeInMillis < window.end) {
    dates += localDate(day.timeInMillis, zone)
    day.add(Calendar.DAY_OF_MONTH, 1)
  }
  val entryDays = entryTimes(data).filter(window::contains).map { localDate(it, zone) }.toSet()
  val closed =
    context.days
      .filter { it.closed && it.date in dates && it.date !in entryDays }
      .map { it.date }
      .toSet()
  val origins = context.origins.associateBy { it.entityTable to it.recordId }
  val records =
    data.sales.filter { window.contains(it.timestamp) }.map { "sales" to it.id } +
      data.expenses.filter { window.contains(it.timestamp) }.map { "expenses" to it.id } +
      data.collections.filter { window.contains(it.timestamp) }.map { "customer_payments" to it.id }
  return RecordCoverage(
    entryDays.size,
    closed.size,
    (dates - entryDays - closed).size,
    records.count { origins[it]?.createdAt == null },
    context.events.count {
      it.entityTable in setOf("sales", "expenses", "customer_payments") && it.action != "INSERT" && window.contains(it.recordedAt)
    },
  )
}

private fun entryTimes(data: ReportInput): List<Long> =
  data.sales.map { it.timestamp } + data.expenses.map { it.timestamp } + data.collections.map { it.timestamp }
