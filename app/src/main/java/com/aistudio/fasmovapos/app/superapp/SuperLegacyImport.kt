package com.aistudio.fasmovapos.app.superapp

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** One-time schema-6 import. Original tables, IDs, references, history and nullable origins are retained. */
object SuperLegacyImport {
  private data class LegacyStamp(
    val date: String,
    val recorded: Any?,
  )

  fun import(db: SupportSQLiteDatabase) {
    if (db.query("SELECT count(*) FROM staff_users").use {
        it.moveToFirst()
        it.getInt(0)
      } == 0
    ) {
      return
    }
    val branches = read(db, "SELECT * FROM branches")
    branches.forEach { b ->
      put(
        db,
        "super_businesses",
        mapOf(
          "id" to "legacy:${b["id"]}",
          "name" to b["name"],
          "type" to "RETAIL",
          "demo" to false,
          "currency" to "KES",
          "createdAt" to null,
        ),
      )
    }
    val first = branches.firstOrNull()?.get("id")?.toString() ?: return
    branches.forEach { b ->
      val biz = "legacy:${b["id"]}"
      read(db, "SELECT * FROM customers").forEach { c ->
        put(
          db,
          "super_parties",
          mapOf(
            "id" to "$biz:${c["id"]}",
            "businessId" to biz,
            "name" to c["name"],
            "kind" to "CUSTOMER",
            "phone" to c["phone"],
            "priceCents" to 0L,
            "cycle" to "MONTHLY",
            "recordedAt" to origin(db, "customers", c["id"]),
          ),
        )
      }
    }
    read(db, "SELECT * FROM products").forEach { p ->
      val scale = number(p, "quantityScale").coerceAtLeast(1)
      put(
        db,
        "super_items",
        mapOf(
          "id" to p["id"],
          "businessId" to "legacy:${p["branchId"]}",
          "name" to p["name"],
          "priceCents" to cents(p, "sellingPrice"),
          "category" to p["category"],
          "stockMilli" to number(p, "stockQuantity") * 1000 / scale,
          "unit" to p["unit"],
          "costCents" to cents(p, "costPrice"),
          "minimumMilli" to number(p, "minStockThreshold") * 1000 / scale,
          "photo" to null,
          "archived" to false,
          "recordedAt" to origin(db, "products", p["id"]),
        ),
      )
    }
    read(db, "SELECT * FROM sales ORDER BY timestamp,id").forEach { sale ->
      val biz = "legacy:${sale["branchId"]}"
      val id = sale["id"].toString()
      val date = day(number(sale, "timestamp"))
      val party = sale["customerId"]?.let { "$biz:$it" }
      val recorded = origin(db, "sales", id)
      val total = cents(sale, "netAmount")
      put(
        db,
        "super_sales",
        mapOf(
          "id" to id,
          "businessId" to biz,
          "partyId" to party,
          "amountCents" to total,
          "method" to sale["paymentMethod"],
          "reference" to sale["paymentReference"],
          "occurredOn" to date,
          "recordedAt" to recorded,
        ),
      )
      charge(
        db,
        id,
        biz,
        party,
        total,
        LegacyStamp(date, recorded),
        "super_sales",
        id,
        total,
      )
      val payments = read(db, "SELECT * FROM sale_payments WHERE saleId=?", arrayOf(id))
      val received =
        if (payments.isEmpty()) {
          if (sale["paymentMethod"] ==
            "CREDIT"
          ) {
            0L
          } else {
            total
          }
        } else {
          payments.filter { it["method"] != "CREDIT" }.sumOf { cents(it, "amount") }
        }
      if (received >
        0
      ) {
        receipt(
          db,
          "sale-receipt:$id",
          biz,
          party,
          received,
          LegacyStamp(date, recorded),
          sale["paymentMethod"].toString(),
          sale["paymentReference"].toString(),
          id,
        )
      }
    }
    read(db, "SELECT i.*,s.branchId,s.timestamp FROM sale_items i JOIN sales s ON i.saleId=s.id").forEach { i ->
      val scale = number(i, "quantityScale").coerceAtLeast(1)
      put(
        db,
        "super_sale_lines",
        mapOf(
          "id" to "legacy-line:${i["id"]}",
          "businessId" to "legacy:${i["branchId"]}",
          "saleId" to i["saleId"],
          "itemId" to i["productId"],
          "name" to i["productName"],
          "quantityMilli" to number(i, "quantity") * 1000 / scale,
          "priceCents" to (cents(i, "unitPrice") * scale / number(i, "pricingQuantity").coerceAtLeast(1)),
          "amountCents" to cents(i, "totalPrice"),
          "unit" to i["quantityUnit"],
          "occurredOn" to day(number(i, "timestamp")),
          "recordedAt" to origin(db, "sale_items", i["id"]),
        ),
      )
    }
    read(db, "SELECT * FROM customer_payments ORDER BY timestamp,id").forEach { p ->
      receipt(
        db,
        p["id"].toString(),
        "legacy:$first",
        "legacy:$first:${p["customerId"]}",
        cents(p, "amountPaid"),
        LegacyStamp(day(number(p, "timestamp")), origin(db, "customer_payments", p["id"])),
        p["paymentMethod"].toString(),
        p["reference"].toString(),
        null,
      )
    }
    read(db, "SELECT * FROM customers").forEach { p ->
      val biz = "legacy:$first"
      val party = "$biz:${p["id"]}"
      val calculated =
        db.query("SELECT coalesce(sum(owedDeltaCents),0) FROM super_ledger WHERE partyId=?", arrayOf(party)).use {
          it.moveToFirst()
          it.getLong(0)
        }
      val adjustment = cents(p, "currentDebt") - calculated
      if (adjustment !=
        0L
      ) {
        charge(
          db,
          "opening:${p["id"]}",
          biz,
          party,
          adjustment,
          LegacyStamp("", null),
          "legacy_opening_balance",
          p["id"].toString(),
          0,
        )
      }
    }
    importCostsAndHistory(db, first)
  }

  private fun importCostsAndHistory(
    db: SupportSQLiteDatabase,
    first: String,
  ) {
    read(db, "SELECT * FROM expenses").forEach { e ->
      val id = e["id"].toString()
      val biz = "legacy:${e["branchId"]}"
      val date = day(number(e, "timestamp"))
      val time = origin(db, "expenses", id)
      val amount = cents(e, "amount")
      put(
        db,
        "super_expenses",
        mapOf(
          "id" to id,
          "businessId" to biz,
          "amountCents" to amount,
          "category" to e["category"],
          "method" to "CASH",
          "reference" to e["expenseRef"],
          "note" to e["description"],
          "deductionId" to null,
          "propertyId" to null,
          "occurredOn" to date,
          "recordedAt" to time,
        ),
      )
      ledger(db, "expense:$id", biz, "super_expenses", id, date, time, mapOf("spentCents" to amount))
    }
    read(db, "SELECT * FROM recording_events").forEach { e ->
      put(
        db,
        "super_history",
        mapOf(
          "id" to "legacy-history:${e["id"]}",
          "businessId" to "legacy:$first",
          "recordId" to e["recordId"],
          "sourceTable" to "legacy:${e["entityTable"]}",
          "beforeValue" to e["beforeValue"],
          "afterValue" to e["afterValue"],
          "reason" to e["reason"],
          "actor" to e["actor"],
          "occurredOn" to day(number(e, "recordedAt")),
          "recordedAt" to e["recordedAt"],
        ),
      )
    }
    read(db, "SELECT * FROM recorded_days WHERE closed=1").forEach { d ->
      put(
        db,
        "super_no_trade",
        mapOf(
          "id" to "legacy:$first:${d["date"]}",
          "businessId" to "legacy:$first",
          "occurredOn" to d["date"],
          "recordedAt" to null,
        ),
      )
    }
  }

  private fun charge(
    db: SupportSQLiteDatabase,
    id: String,
    biz: String,
    party: String?,
    amount: Long,
    stamp: LegacyStamp,
    table: String,
    source: String,
    earned: Long,
  ) {
    val date = stamp.date
    val recorded = stamp.recorded

    put(
      db,
      "super_charges",
      mapOf(
        "id" to id,
        "businessId" to biz,
        "partyId" to party,
        "sourceTable" to table,
        "sourceId" to source,
        "amountCents" to amount,
        "month" to "",
        "propertyId" to null,
        "occurredOn" to date,
        "recordedAt" to recorded,
      ),
    )
    ledger(
      db,
      "charge:$id",
      biz,
      table,
      source,
      date,
      recorded,
      mapOf(
        "partyId" to party,
        "earnedCents" to earned,
        "owedDeltaCents" to amount,
      ),
    )
  }

  private fun receipt(
    db: SupportSQLiteDatabase,
    id: String,
    biz: String,
    party: String?,
    amount: Long,
    stamp: LegacyStamp,
    method: String,
    reference: String,
    sale: String?,
  ) {
    val date = stamp.date
    val recorded = stamp.recorded

    put(
      db,
      "super_receipts",
      mapOf(
        "id" to id,
        "businessId" to biz,
        "partyId" to party,
        "amountCents" to amount,
        "method" to method,
        "reference" to reference,
        "saleId" to sale,
        "propertyId" to null,
        "occurredOn" to date,
        "recordedAt" to recorded,
      ),
    )
    ledger(
      db,
      "receipt:$id",
      biz,
      "super_receipts",
      id,
      date,
      recorded,
      mapOf(
        "partyId" to party,
        "receivedCents" to amount,
        "owedDeltaCents" to -amount,
      ),
    )
    val charges =
      if (sale !=
        null
      ) {
        read(db, "SELECT * FROM super_charges WHERE businessId=? AND sourceId=?", arrayOf(biz, sale))
      } else {
        read(db, "SELECT * FROM super_charges WHERE businessId=? AND partyId=? ORDER BY occurredOn,id", arrayOf(biz, party!!))
      }
    var left = amount
    charges.forEach { c ->
      val allocated =
        db.query("SELECT coalesce(sum(amountCents),0) FROM super_allocations WHERE chargeId=?", arrayOf(c["id"])).use {
          it.moveToFirst()
          it.getLong(0)
        }
      val take = minOf(left, (number(c, "amountCents") - allocated).coerceAtLeast(0))
      if (take >
        0
      ) {
        put(
          db,
          "super_allocations",
          mapOf(
            "id" to "$id:${c["id"]}",
            "businessId" to biz,
            "receiptId" to id,
            "chargeId" to c["id"],
            "amountCents" to take,
            "occurredOn" to date,
            "recordedAt" to recorded,
          ),
        )
        left -= take
      }
    }
  }

  private fun ledger(
    db: SupportSQLiteDatabase,
    id: String,
    biz: String,
    table: String,
    source: String,
    date: String,
    recorded: Any?,
    amounts: Map<String, Any?>,
  ) {
    val values =
      mutableMapOf<String, Any?>(
        "id" to id,
        "businessId" to biz,
        "sourceTable" to table,
        "sourceId" to source,
        "partyId" to null,
        "earnedCents" to 0L,
        "receivedCents" to 0L,
        "spentCents" to 0L,
        "owedDeltaCents" to 0L,
        "settledCents" to 0L,
        "heldCents" to 0L,
        "otherInCents" to 0L,
        "propertyId" to null,
        "occurredOn" to date,
        "recordedAt" to recorded,
      )
    values.putAll(amounts)
    put(db, "super_ledger", values)
  }

  private fun put(
    db: SupportSQLiteDatabase,
    table: String,
    values: Map<String, Any?>,
  ) {
    val content = ContentValues()
    val fields =
      if (table ==
        "super_businesses"
      ) {
        values
      } else {
        mapOf("occurredOn" to "", "recordedAt" to null, "schemaVersion" to 1, "voided" to false) + values
      }
    fields.forEach { (key, value) ->
      when (value) {
        null -> content.putNull(key)
        is Boolean -> content.put(key, if (value) 1 else 0)
        is Number -> content.put(key, value.toLong())
        else -> content.put(key, value.toString())
      }
    }
    db.insert(table, SQLiteDatabase.CONFLICT_ABORT, content)
  }

  private fun cents(
    row: Map<String, Any?>,
    key: String,
  ): Long =
    java.math
      .BigDecimal(row[key].toString())
      .movePointRight(2)
      .setScale(0, java.math.RoundingMode.HALF_UP)
      .longValueExact()

  private fun day(epoch: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.UK)
      .apply {
        timeZone = TimeZone.getTimeZone("Africa/Nairobi")
      }.format(Date(epoch))

  private fun read(
    db: SupportSQLiteDatabase,
    sql: String,
    args: Array<out Any?> = emptyArray(),
  ): List<Map<String, Any?>> =
    db.query(sql, args).use { c ->
      val result = mutableListOf<Map<String, Any?>>()
      while (c.moveToNext()) {
        result +=
          c.columnNames
            .mapIndexed { index, name ->
              name to
                when (c.getType(index)) {
                  Cursor.FIELD_TYPE_NULL -> null
                  Cursor.FIELD_TYPE_INTEGER -> c.getLong(index)
                  Cursor.FIELD_TYPE_FLOAT -> c.getDouble(index)
                  else -> c.getString(index)
                }
            }.toMap()
      }
      result
    }
}

private fun origin(
  db: SupportSQLiteDatabase,
  table: String,
  id: Any?,
): Long? =
  db.query("SELECT createdAt FROM record_origins WHERE entityTable=? AND recordId=?", arrayOf(table, id.toString())).use {
    if (it.moveToFirst() &&
      !it.isNull(0)
    ) {
      it.getLong(0)
    } else {
      null
    }
  }

private fun number(
  row: Map<String, Any?>,
  key: String,
): Long = (row[key] as? Number)?.toLong() ?: 0L
