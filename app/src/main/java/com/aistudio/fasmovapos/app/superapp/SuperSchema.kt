package com.aistudio.fasmovapos.app.superapp
import androidx.sqlite.db.SupportSQLiteDatabase

/** SQL mirrors Room entities. Legacy business rows remain retained in the original tables. */
object SuperSchema {
  fun create(db: SupportSQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_businesses` (`id` TEXT NOT NULL PRIMARY KEY, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `demo` INTEGER NOT NULL, `currency` TEXT NOT NULL, `createdAt` INTEGER)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_parties` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `phone` TEXT NOT NULL, `priceCents` INTEGER NOT NULL, `cycle` TEXT NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_milk` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `partyId` TEXT NOT NULL, `session` TEXT NOT NULL, `litresMilli` INTEGER NOT NULL, `priceCents` INTEGER NOT NULL, `earnedCents` INTEGER NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_properties` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `name` TEXT NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_units` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `propertyId` TEXT NOT NULL, `label` TEXT NOT NULL, `rentCents` INTEGER NOT NULL, `dueDay` INTEGER NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_tenancies` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `unitId` TEXT NOT NULL, `partyId` TEXT NOT NULL, `moveIn` TEXT NOT NULL, `moveOut` TEXT, `depositCents` INTEGER NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_rent_charges` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `unitId` TEXT NOT NULL, `tenancyId` TEXT NOT NULL, `partyId` TEXT NOT NULL, `month` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `dueDate` TEXT NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_items` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `name` TEXT NOT NULL, `priceCents` INTEGER NOT NULL, `category` TEXT NOT NULL, `stockMilli` INTEGER, `unit` TEXT NOT NULL, `costCents` INTEGER NOT NULL, `minimumMilli` INTEGER NOT NULL, `photo` BLOB, `archived` INTEGER NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_sales` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `partyId` TEXT, `amountCents` INTEGER NOT NULL, `method` TEXT NOT NULL, `reference` TEXT NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_sale_lines` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `saleId` TEXT NOT NULL, `itemId` TEXT NOT NULL, `name` TEXT NOT NULL, `quantityMilli` INTEGER NOT NULL, `priceCents` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `unit` TEXT NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_charges` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `partyId` TEXT, `sourceTable` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `month` TEXT NOT NULL, `propertyId` TEXT, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_receipts` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `partyId` TEXT, `amountCents` INTEGER NOT NULL, `method` TEXT NOT NULL, `reference` TEXT NOT NULL, `saleId` TEXT, `propertyId` TEXT, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_deductions` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `receiptId` TEXT NOT NULL, `category` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `countsAsCost` INTEGER NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_allocations` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `receiptId` TEXT NOT NULL, `chargeId` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_expenses` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `category` TEXT NOT NULL, `method` TEXT NOT NULL, `reference` TEXT NOT NULL, `note` TEXT NOT NULL, `deductionId` TEXT, `propertyId` TEXT, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_money_in` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `kind` TEXT NOT NULL, `method` TEXT NOT NULL, `reference` TEXT NOT NULL, `note` TEXT NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_ledger` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `sourceTable` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `partyId` TEXT, `earnedCents` INTEGER NOT NULL, `receivedCents` INTEGER NOT NULL, `spentCents` INTEGER NOT NULL, `owedDeltaCents` INTEGER NOT NULL, `settledCents` INTEGER NOT NULL, `heldCents` INTEGER NOT NULL, `otherInCents` INTEGER NOT NULL, `propertyId` TEXT, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_history` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `recordId` TEXT NOT NULL, `sourceTable` TEXT NOT NULL, `beforeValue` TEXT, `afterValue` TEXT, `reason` TEXT NOT NULL, `actor` TEXT NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
    db.execSQL(
      "CREATE TABLE IF NOT EXISTS `super_no_trade` (`id` TEXT NOT NULL PRIMARY KEY, `businessId` TEXT NOT NULL, `occurredOn` TEXT NOT NULL, `recordedAt` INTEGER, `schemaVersion` INTEGER NOT NULL, `voided` INTEGER NOT NULL)",
    )
  }

  fun migrate(db: SupportSQLiteDatabase) {
    create(db)
    SuperLegacyImport.import(db)
  }
}
