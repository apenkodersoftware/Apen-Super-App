package com.aistudio.fasmovapos.app.superapp

data class RecordPeriod(
  val from: String,
  val through: String,
) {
  init {
    SuperDates.validate(from)
    SuperDates.validate(through)
    require(from <= through) { "DATE" }
  }

  val days: Int get() {
    var count = 1
    var date = from
    while (date < through) {
      require(count < 3660) { "DATE" }
      date = SuperDates.addDays(date, 1)
      count++
    }
    return count
  }

  fun previous(): RecordPeriod = RecordPeriod(SuperDates.addDays(from, -days), SuperDates.addDays(from, -1))
}

data class PeriodReading(
  val period: RecordPeriod,
  val snapshot: SuperSnapshot,
  val coverageDays: Int,
  val calendarDays: Int,
  val milkDays: Int,
  val previous: SuperSnapshot?,
) {
  val averageLitresMilli: Long? get() = if (milkDays == 0) null else snapshot.litresMilli / milkDays
}

/** Missing dates stay unknown. Current-day records have no activity time, so intraday comparisons are unavailable. */
fun SuperRepository.periodReading(
  businessId: String,
  period: RecordPeriod,
): PeriodReading =
  store.transaction {
    fun coverage(p: RecordPeriod): Int {
      val posted = dao.ledgerRows(businessId).filter { !it.voided && it.occurredOn in p.from..p.through }.map { it.occurredOn }
      val closed = dao.noTradeRows(businessId).filter { !it.voided && it.occurredOn in p.from..p.through }.map { it.occurredOn }
      return (posted + closed).toSet().size
    }
    val snapshot = snapshot(businessId, period.from, period.through)
    val prior = period.previous()
    val comparable = period.through < SuperDates.today(store.now()) && coverage(period) == period.days && coverage(prior) == prior.days
    PeriodReading(
      period,
      snapshot,
      coverage(period),
      period.days,
      dao
        .milkDeliverys(
          businessId,
        ).filter { !it.voided && it.occurredOn in period.from..period.through }
        .map { it.occurredOn }
        .distinct()
        .size,
      if (comparable) snapshot(businessId, prior.from, prior.through) else null,
    )
  }
