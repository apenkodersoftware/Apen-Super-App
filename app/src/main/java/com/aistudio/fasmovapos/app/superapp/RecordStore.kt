package com.aistudio.fasmovapos.app.superapp

import android.database.Cursor
import android.util.Base64
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Callable

object SuperDates {
  val zone: TimeZone get() = TimeZone.getTimeZone("Africa/Nairobi")

  fun formatter(pattern: String) =
    SimpleDateFormat(pattern, Locale.UK).apply {
      timeZone = zone
      isLenient = false
    }

  fun today(now: Long = System.currentTimeMillis()): String = formatter("yyyy-MM-dd").format(Date(now))

  fun validate(value: String): String {
    require(value.length == 10 && formatter("yyyy-MM-dd").format(requireNotNull(formatter("yyyy-MM-dd").parse(value))) == value) { "DATE" }
    require(value.take(4).toInt() in 1900..2100) { "DATE" }
    return value
  }

  fun month(value: String): String {
    validate("$value-01")
    return value
  }

  fun addDays(
    value: String,
    days: Int,
  ): String = change(value, Calendar.DAY_OF_MONTH, days)

  fun addMonths(
    value: String,
    months: Int,
  ): String = change("${month(value)}-01", Calendar.MONTH, months).take(7)

  private fun change(
    value: String,
    field: Int,
    amount: Int,
  ): String {
    val c =
      Calendar.getInstance(zone).apply {
        time = requireNotNull(formatter("yyyy-MM-dd").parse(validate(value)))
        add(field, amount)
      }
    return formatter("yyyy-MM-dd").format(c.time)
  }

  fun label(value: String): String =
    if (value.isBlank()) "—" else formatter("EEE d MMM yyyy").format(formatter("yyyy-MM-dd").parse(validate(value))!!)
}

object SuperMoney {
  const val MAX_CENTS = 100_000_000_000L

  fun positive(cents: Long): Long {
    require(cents in 1..MAX_CENTS) { "AMOUNT" }
    return cents
  }

  fun cents(text: String): Long {
    val input = text.trim()
    if (',' in input) require(input.matches(Regex("[0-9]{1,3}(,[0-9]{3})+(\\.[0-9]{0,2})?"))) { "AMOUNT" }
    return BigDecimal(input.replace(",", "")).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact().also {
      require(
        it in 0..MAX_CENTS,
      ) { "AMOUNT" }
    }
  }

  fun milli(text: String): Long =
    BigDecimal(text.trim()).setScale(3, RoundingMode.UNNECESSARY).movePointRight(3).longValueExact().also {
      require(
        it in 1..100_000_000L,
      ) { "QUANTITY" }
    }

  fun value(
    quantityMilli: Long,
    unitCents: Long,
  ): Long =
    BigDecimal(quantityMilli).multiply(BigDecimal(unitCents)).divide(BigDecimal(1000), 0, RoundingMode.HALF_UP).longValueExact().also {
      require(
        it in 0..MAX_CENTS,
      ) { "AMOUNT" }
    }

  fun label(cents: Long): String {
    val number = java.text.DecimalFormat(if (cents % 100L == 0L) "#,##0" else "#,##0.00", java.text.DecimalFormatSymbols(Locale.UK))
    return "KES ${number.format(BigDecimal(cents).movePointLeft(2))}"
  }

  fun quantity(milli: Long): String = BigDecimal(milli).movePointLeft(3).stripTrailingZeros().toPlainString()
}

/** All mutation callers use this same Room transaction and audit boundary. */
class RecordStore(
  val db: FasMovaPOSDatabase,
  val now: () -> Long = System::currentTimeMillis,
) {
  val dao: SuperDao get() = db.superDao()

  fun id(): String = UUID.randomUUID().toString()

  fun <T> transaction(block: () -> T): T = db.runInTransaction(Callable { block() })

  fun business(id: String): BusinessProfile = requireNotNull(dao.businessProfile(id)) { "BUSINESS" }

  fun type(
    id: String,
    vararg types: Livelihood,
  ): BusinessProfile =
    business(id).also {
      require(types.any { t -> t.name == it.type }) { "BUSINESS_TYPE" }
    }

  fun party(
    businessId: String,
    id: String,
  ): Party =
    requireNotNull(dao.party(id)) { "PARTY" }.also {
      require(
        it.businessId == businessId && !it.voided,
      ) { "PARTY" }
    }

  fun item(
    businessId: String,
    id: String,
  ): CommerceItem =
    requireNotNull(dao.commerceItem(id)) { "ITEM" }.also {
      require(
        it.businessId == businessId && !it.voided && !it.archived,
      ) { "ITEM" }
    }

  fun reason(value: String): String = value.trim().also { require(it.length in 3..500) { "REASON" } }

  fun name(value: String): String = value.trim().also { require(it.length in 1..120) { "NAME" } }

  fun method(value: String): String = value.also { require(it in setOf("CASH", "MPESA", "BANK", "CREDIT", "MIXED")) { "METHOD" } }

  fun audit(
    table: String,
    recordId: String,
    businessId: String,
    reason: String,
    block: () -> Unit,
  ) {
    val before = row(table, recordId)
    block()
    val after = row(table, recordId)
    if (before !=
      after
    ) {
      dao.put(
        HistoryRow(id(), businessId, recordId, table, before, after, reason, occurredOn = SuperDates.today(now()), recordedAt = now()),
      )
    }
  }

  fun sourceRecord(
    businessId: String,
    table: String,
    id: String,
  ): String? {
    business(businessId)
    val value = row(table, id) ?: return null
    val record = JSONObject(value)
    require(if (table == "super_businesses") id == businessId else record.optString("businessId") == businessId) { "BUSINESS" }
    return value
  }

  private fun row(
    table: String,
    id: String,
  ): String? {
    require(table in SuperTables.names) { "TABLE" }
    return db.openHelper.writableDatabase.query("SELECT * FROM `$table` WHERE id = ?", arrayOf(id)).use { c ->
      if (!c.moveToFirst()) {
        null
      } else {
        JSONObject()
          .apply {
            c.columnNames.forEachIndexed { i, key ->
              put(
                key,
                when (c.getType(i)) {
                  Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                  Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                  Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
                  Cursor.FIELD_TYPE_BLOB -> Base64.encodeToString(c.getBlob(i), Base64.NO_WRAP)
                  else -> c.getString(i)
                },
              )
            }
          }.toString()
      }
    }
  }

  fun ledger(row: LedgerRow) {
    dao.put(row)
  }

  fun charge(
    row: ChargeRow,
    earnedCents: Long,
  ) {
    dao.put(row)
    ledger(
      LedgerRow(
        "charge:${row.id}",
        row.businessId,
        row.sourceTable,
        row.sourceId,
        row.partyId,
        earnedCents = earnedCents,
        owedDeltaCents = row.amountCents,
        propertyId = row.propertyId,
        occurredOn = row.occurredOn,
        recordedAt = row.recordedAt,
      ),
    )
  }

  fun openCharge(row: ChargeRow): Long =
    row.amountCents - dao.allocationRows(row.businessId).filter { !it.voided && it.chargeId == row.id }.sumOf { it.amountCents }

  fun balance(
    businessId: String,
    partyId: String? = null,
  ): Long =
    dao
      .ledgerRows(businessId)
      .filter {
        !it.voided &&
          (partyId == null || it.partyId == partyId)
      }.sumOf { it.owedDeltaCents }

  fun history(recordId: String): List<ChangeHistory> =
    db.openHelper.readableDatabase
      .query("SELECT * FROM super_history WHERE recordId=? ORDER BY recordedAt,id", arrayOf(recordId))
      .use { c ->
        val rows = mutableListOf<ChangeHistory>()
        while (c.moveToNext()) {
          rows +=
            ChangeHistory(
              recordId,
              c.getString(c.getColumnIndexOrThrow("beforeValue")),
              c.getString(c.getColumnIndexOrThrow("afterValue")),
              c.getString(c.getColumnIndexOrThrow("reason")),
              c.getLong(c.getColumnIndexOrThrow("recordedAt")),
            )
        }
        rows
      }
}

object SuperTables {
  val names =
    setOf(
      "super_businesses",
      "super_parties",
      "super_milk",
      "super_properties",
      "super_units",
      "super_tenancies",
      "super_rent_charges",
      "super_items",
      "super_sales",
      "super_sale_lines",
      "super_charges",
      "super_receipts",
      "super_deductions",
      "super_allocations",
      "super_expenses",
      "super_money_in",
      "super_ledger",
      "super_history",
      "super_no_trade",
    )
}
