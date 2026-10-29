package com.aistudio.fasmovapos.app.superapp

import androidx.sqlite.db.SupportSQLiteDatabase

/** Existing measured-item prices are explicit offers, not invented profit margins. */
object SuperPricing {
  fun lineTotal(
    item: CommerceItem,
    quantityMilli: Long,
  ): Long =
    when {
      item.unit != "Pcs" && quantityMilli == 250L && item.quarterPriceCents > 0 -> item.quarterPriceCents
      item.unit != "Pcs" && quantityMilli == 500L && item.halfPriceCents > 0 -> item.halfPriceCents
      else -> SuperMoney.value(quantityMilli, item.priceCents)
    }
}

/** Additive migration: existing dairy/rent/food rows and original retail tables remain untouched. */
object SuperPricingMigration {
  fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE super_items ADD COLUMN quarterPriceCents INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE super_items ADD COLUMN halfPriceCents INTEGER NOT NULL DEFAULT 0")
    db.execSQL("ALTER TABLE super_sales ADD COLUMN discountCents INTEGER NOT NULL DEFAULT 0")
    db.execSQL(
      "UPDATE super_items SET quarterPriceCents = CAST(round((SELECT quarterPrice * 100 FROM products WHERE products.id = super_items.id)) AS INTEGER), halfPriceCents = CAST(round((SELECT halfPrice * 100 FROM products WHERE products.id = super_items.id)) AS INTEGER) WHERE id IN (SELECT id FROM products)",
    )
    db.execSQL(
      "UPDATE super_sales SET discountCents = CAST(round((SELECT discountAmount * 100 FROM sales WHERE sales.id = super_sales.id)) AS INTEGER) WHERE id IN (SELECT id FROM sales)",
    )
  }
}
