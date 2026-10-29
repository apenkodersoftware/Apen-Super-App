package com.aistudio.fasmovapos.app.superapp

import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase

class SuperRepository(
  db: FasMovaPOSDatabase,
  now: () -> Long = System::currentTimeMillis,
) {
  val store = RecordStore(db, now)
  val dao get() = store.dao
  val rental = RentalRepository(store)
  val dairy = DairyRepository(store)
  val edits = RecordEdits(store)
  val accounts = AccountRepository(store)
  val commerce = CommerceRepository(store, accounts)

  fun createBusiness(
    type: Livelihood,
    name: String,
    demo: Boolean = false,
  ): BusinessProfile =
    store.transaction {
      require(dao.businesses().size < 100) { "BUSINESS_LIMIT" }
      val row = BusinessProfile(store.id(), store.name(name), type.name, demo, createdAt = store.now())
      store.audit("super_businesses", row.id, row.id, "Recorded") { dao.put(row) }
      row
    }

  fun addParty(
    businessId: String,
    name: String,
    kind: String,
    priceCents: Long = 0,
    phone: String = "",
    cycle: String = "MONTHLY",
  ): Party =
    store.transaction {
      store.business(businessId)
      require(priceCents in 0..SuperMoney.MAX_CENTS && phone.length <= 40 && cycle in setOf("DAILY", "WEEKLY", "MONTHLY")) { "PARTY" }
      require(kind in setOf("COOPERATIVE", "PROCESSOR", "BROKER", "HOTEL", "NEIGHBOUR", "CUSTOMER", "TENANT")) { "PARTY" }
      val row =
        Party(store.id(), businessId, store.name(name), kind, phone.trim(), priceCents, cycle, SuperDates.today(store.now()), store.now())
      store.audit("super_parties", row.id, businessId, "Recorded") { dao.put(row) }
      row
    }

  fun receipt(
    businessId: String,
    partyId: String,
    amountCents: Long,
    date: String,
    method: String,
    reference: String = "",
    deductions: List<Deduction> = emptyList(),
  ): String = accounts.receipt(businessId, partyId, amountCents, date, method, reference, deductions)

  fun expense(
    businessId: String,
    amountCents: Long,
    date: String,
    category: String,
    method: String = "CASH",
    reference: String = "",
    note: String = "",
    propertyId: String? = null,
  ): String = accounts.expense(businessId, amountCents, date, category, method, reference, note, propertyId = propertyId)

  fun moneyIn(
    businessId: String,
    amountCents: Long,
    date: String,
    kind: String,
  ): String = accounts.moneyIn(businessId, amountCents, date, kind)

  fun item(
    businessId: String,
    name: String,
    priceCents: Long,
    category: String,
    stockMilli: Long? = null,
  ): String = commerce.item(businessId, name, priceCents, category, stockMilli)

  fun sale(
    businessId: String,
    quantities: Map<String, Long>,
    receivedCents: Long,
    date: String,
    method: String,
    partyId: String? = null,
  ): String = commerce.sale(businessId, quantities, receivedCents, date, method, partyId)

  fun history(recordId: String): List<ChangeHistory> = store.history(recordId)

  fun allocations(receiptId: String): List<Allocation> = accounts.allocations(receiptId)

  fun snapshot(
    businessId: String,
    from: String,
    through: String,
  ): SuperSnapshot =
    store.transaction {
      store.business(businessId)
      SuperDates.validate(from)
      SuperDates.validate(through)
      require(from <= through) { "DATE" }
      val all = dao.ledgerRows(businessId).filter { !it.voided }
      val rows = all.filter { it.occurredOn in from..through }
      val days = rows.map { it.occurredOn }.distinct()
      val closed = dao.noTradeRows(businessId).count { !it.voided && it.occurredOn in from..through && it.occurredOn !in days }
      SuperSnapshot(
        rows.sumOf { it.earnedCents },
        rows.sumOf { it.receivedCents },
        rows.sumOf { it.spentCents },
        all.sumOf { it.owedDeltaCents },
        dao
          .milkDeliverys(businessId)
          .filter {
            !it.voided && it.occurredOn in from..through
          }.sumOf { it.litresMilli },
        all.sumOf { it.heldCents },
        rows.sumOf { it.otherInCents },
        days.size,
        rows.count {
          it.recordedAt ==
            null
        },
        closed,
        dao.historyRows(businessId).count {
          it.beforeValue != null &&
            it.sourceTable in setOf("super_milk", "super_sales", "super_expenses", "super_receipts", "super_rent_charges") &&
            it.occurredOn in from..through
        },
      )
    }

  fun markNoTrade(
    businessId: String,
    date: String,
  ) = store.transaction {
    store.business(businessId)
    SuperDates.validate(date)
    require(date <= SuperDates.today(store.now())) { "DATE" }
    require(dao.ledgerRows(businessId).none { !it.voided && it.occurredOn == date }) { "HAS_ENTRIES" }
    val id = "$businessId:$date"
    val old = dao.noTradeRow(id)
    store.audit("super_no_trade", id, businessId, "Recorded") { dao.put(NoTradeRow(id, businessId, date, old?.recordedAt ?: store.now())) }
  }

  fun milk(
    businessId: String,
    partyId: String,
    date: String,
    session: String,
    litres: String,
    priceCents: Long,
  ): String = dairy.milk(businessId, partyId, date, session, litres, priceCents)

  fun editMilk(
    id: String,
    litres: String,
    reason: String,
  ): Unit = dairy.edit(id, litres, reason)

  fun property(
    businessId: String,
    name: String,
  ): String = rental.property(businessId, name)

  fun unit(
    businessId: String,
    propertyId: String,
    label: String,
    rentCents: Long,
    dueDay: Int,
    tenant: String,
    moveIn: String,
    depositCents: Long = 0,
    phone: String = "",
  ): String = rental.unit(businessId, propertyId, label, rentCents, dueDay, tenant, moveIn, depositCents, phone)

  fun rentCharges(
    businessId: String,
    throughMonth: String,
  ): Int = rental.charges(businessId, throughMonth)

  fun moveOut(
    unitId: String,
    vacantFrom: String,
    reason: String,
  ): Unit = rental.moveOut(unitId, vacantFrom, reason)

  fun tenantParty(unitId: String): String = rental.tenantParty(unitId)

  fun reminder(unitId: String): String = rental.reminder(unitId)

  fun updateItemDetails(
    id: String,
    costCents: Long,
    minimumMilli: Long,
    photo: ByteArray?,
    archived: Boolean,
    reason: String,
  ) = store.transaction {
    val old = requireNotNull(dao.commerceItem(id)) { "ITEM" }
    store.reason(reason)
    require(costCents in 0..SuperMoney.MAX_CENTS && minimumMilli in 0..100_000_000L) { "AMOUNT" }
    require(photo == null || photo.size <= 65536) { "PHOTO" }
    store.audit("super_items", id, old.businessId, reason) {
      dao.put(old.copy(costCents = costCents, minimumMilli = minimumMilli, photo = photo, archived = archived))
    }
  }

  fun renameBusiness(
    id: String,
    name: String,
    reason: String,
  ) = store.transaction {
    val old = store.business(id)
    store.reason(reason)
    store.audit("super_businesses", id, id, reason) { dao.put(old.copy(name = store.name(name))) }
  }

  fun retailDetails(
    id: String,
    name: String,
    category: String,
    costCents: Long,
    minimumMilli: Long,
    quarterCents: Long,
    halfCents: Long,
    reason: String,
  ) = store.transaction {
    val row = requireNotNull(dao.commerceItem(id)) { "ITEM" }
    store.type(row.businessId, Livelihood.RETAIL)
    require(!row.voided && costCents in 0..SuperMoney.MAX_CENTS && minimumMilli in 0..100_000_000L) { "AMOUNT" }
    require(quarterCents in 0..SuperMoney.MAX_CENTS && halfCents in 0..SuperMoney.MAX_CENTS) { "AMOUNT" }
    require(row.unit in setOf("Kg", "Ltr") || (quarterCents == 0L && halfCents == 0L)) { "QUANTITY" }
    store.reason(reason)
    store.audit("super_items", id, row.businessId, reason) {
      dao.put(
        row.copy(
          name = store.name(name),
          category = store.name(category),
          costCents = costCents,
          minimumMilli = minimumMilli,
          quarterPriceCents = quarterCents,
          halfPriceCents = halfCents,
        ),
      )
    }
  }
}
