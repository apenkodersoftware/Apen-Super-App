package com.aistudio.fasmovapos.app.data.records

import androidx.sqlite.db.SupportSQLiteDatabase

/** Triggers share the transaction of the changed row, including writes through existing DAOs. */
object RecordingSchema {
  private val tables =
    listOf(
      "sales",
      "sale_items",
      "sale_payments",
      "expenses",
      "customer_payments",
      "products",
      "customers",
      "personal_loan_plans",
      "recorded_days",
    )

  fun create(db: SupportSQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS recording_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "entityTable TEXT NOT NULL, recordId TEXT NOT NULL, action TEXT NOT NULL, occurredAt INTEGER, " +
        "recordedAt INTEGER NOT NULL, beforeValue TEXT, afterValue TEXT, reason TEXT NOT NULL, actor TEXT NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS personal_loan_plans (id TEXT NOT NULL PRIMARY KEY, productId TEXT NOT NULL, amountCents INTEGER NOT NULL, dueDate TEXT NOT NULL)",
    )
    db.execSQL("CREATE TABLE IF NOT EXISTS recording_meta (id TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
    db.execSQL("CREATE TABLE IF NOT EXISTS recorded_days (date TEXT NOT NULL PRIMARY KEY, closed INTEGER NOT NULL)")
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS record_origins (entityTable TEXT NOT NULL, recordId TEXT NOT NULL, createdAt INTEGER, PRIMARY KEY(entityTable,recordId))",
    )
    tables.forEach { table ->
      db.execSQL(
        "INSERT OR IGNORE INTO record_origins(entityTable,recordId,createdAt) SELECT '$table',CAST(${if (table == "recorded_days") "date" else "id"} AS TEXT),NULL FROM `$table`",
      )
    }
    installTriggers(db)
  }

  fun installTriggers(db: SupportSQLiteDatabase) {
    tables.forEach { table ->
      val columns = mutableListOf<String>()
      db.query("PRAGMA table_info(`$table`)").use { c -> while (c.moveToNext()) columns += c.getString(1) }
      if (columns.isNotEmpty()) {
        listOf("INSERT", "UPDATE", "DELETE").forEach { action ->
          val row = if (action == "DELETE") "OLD" else "NEW"
          val key = if (table == "recorded_days") "date" else "id"
          val occurred = if ("timestamp" in columns) "$row.timestamp" else "NULL"
          val before = if (action == "INSERT") "NULL" else snapshot(columns, "OLD")
          val after = if (action == "DELETE") "NULL" else snapshot(columns, "NEW")
          val actor =
            when (table) {
              "sales" -> "$row.cashierId"
              "expenses" -> "$row.recordedBy"
              else -> "'local device'"
            }
          db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS apen_${table}_$action AFTER $action ON `$table` BEGIN " +
              (
                if (action ==
                  "INSERT"
                ) {
                  "INSERT OR IGNORE INTO record_origins(entityTable,recordId,createdAt) VALUES('$table',CAST(NEW.`$key` AS TEXT),strftime('%s','now') * 1000); "
                } else {
                  ""
                }
              ) +
              "INSERT INTO recording_events(entityTable,recordId,action,occurredAt,recordedAt,beforeValue,afterValue,reason,actor) " +
              "VALUES('$table',CAST($row.`$key` AS TEXT),'$action',$occurred," +
              "(strftime('%s','now') * 1000 + CAST(substr(strftime('%f','now'),4,3) AS INTEGER))," +
              "$before,$after,COALESCE((SELECT value FROM recording_meta WHERE id='audit_reason'),'Local $action; no user reason supplied'),$actor); END",
          )
        }
      }
    }
  }

  private fun snapshot(
    columns: List<String>,
    row: String,
  ): String = columns.joinToString(" || '; ' || ") { "'$it=' || quote($row.`$it`)" }
}
