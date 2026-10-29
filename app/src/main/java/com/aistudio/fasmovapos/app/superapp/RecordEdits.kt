package com.aistudio.fasmovapos.app.superapp

/** Voids preserve domain rows, original entry times and before/after history. */
class RecordEdits(
  private val store: RecordStore,
) {
  private val dao get() = store.dao

  fun expense(
    id: String,
    reason: String,
  ) = store.transaction {
    val row = requireNotNull(dao.expenseRow(id)) { "EXPENSE" }
    store.reason(reason)
    require(row.deductionId == null) { "IN_USE" }
    voidExpense(row, reason)
  }

  private fun voidExpense(
    row: ExpenseRow,
    reason: String,
  ) {
    require(!row.voided) { "ALREADY_VOID" }
    store.audit("super_expenses", row.id, row.businessId, reason) { dao.put(row.copy(voided = true)) }
    dao.ledgerRow("expense:${row.id}")?.let { dao.put(it.copy(voided = true)) }
  }

  fun receipt(
    id: String,
    reason: String,
  ) = store.transaction {
    val row = requireNotNull(dao.receiptRow(id)) { "RECEIPT" }
    store.reason(reason)
    require(row.saleId == null || row.partyId != null) { "IN_USE" }
    voidReceipt(row, reason)
  }

  private fun voidReceipt(
    row: ReceiptRow,
    reason: String,
  ) {
    require(!row.voided) { "ALREADY_VOID" }
    store.audit("super_receipts", row.id, row.businessId, reason) { dao.put(row.copy(voided = true)) }
    dao.allocationRows(row.businessId).filter { !it.voided && it.receiptId == row.id }.forEach { a ->
      store.audit("super_allocations", a.id, row.businessId, reason) { dao.put(a.copy(voided = true)) }
    }
    dao.deductionRows(row.businessId).filter { !it.voided && it.receiptId == row.id }.forEach { d ->
      store.audit("super_deductions", d.id, row.businessId, reason) { dao.put(d.copy(voided = true)) }
      dao.expenseRows(row.businessId).filter { !it.voided && it.deductionId == d.id }.forEach { voidExpense(it, reason) }
    }
    dao.ledgerRow("receipt:${row.id}")?.let { dao.put(it.copy(voided = true)) }
  }

  fun sale(
    id: String,
    reason: String,
  ) = store.transaction {
    val row = requireNotNull(dao.commerceSale(id)) { "SALE" }
    store.reason(reason)
    require(!row.voided) { "ALREADY_VOID" }
    val charge =
      requireNotNull(dao.chargeRows(row.businessId).firstOrNull { it.sourceTable == "super_sales" && it.sourceId == id }) { "CHARGE" }
    val receipts = dao.receiptRows(row.businessId).filter { !it.voided && it.saleId == id }
    val receiptIds = receipts.map { it.id }.toSet()
    require(dao.allocationRows(row.businessId).none { !it.voided && it.chargeId == charge.id && it.receiptId !in receiptIds }) { "IN_USE" }
    receipts.forEach { voidReceipt(it, reason) }
    dao.commerceLines(row.businessId).filter { !it.voided && it.saleId == id }.forEach { line ->
      val item = requireNotNull(dao.commerceItem(line.itemId)) { "ITEM" }
      require(item.businessId == row.businessId) { "BUSINESS" }
      item.stockMilli?.let { quantity ->
        store.audit(
          "super_items",
          item.id,
          row.businessId,
          reason,
        ) { dao.put(item.copy(stockMilli = Math.addExact(quantity, line.quantityMilli))) }
      }
      store.audit("super_sale_lines", line.id, row.businessId, reason) { dao.put(line.copy(voided = true)) }
    }
    store.audit("super_sales", id, row.businessId, reason) { dao.put(row.copy(voided = true)) }
    dao.put(charge.copy(voided = true))
    dao.ledgerRow("charge:${charge.id}")?.let { dao.put(it.copy(voided = true)) }
  }

  fun rentCharge(
    id: String,
    amountCents: Long,
    reason: String,
  ) = store.transaction {
    val row = requireNotNull(dao.rentCharge(id)) { "RENT" }
    store.reason(reason)
    require(!row.voided) { "ALREADY_VOID" }
    SuperMoney.positive(amountCents)
    val charge = requireNotNull(dao.chargeRow(id)) { "CHARGE" }
    require(amountCents >= charge.amountCents - store.openCharge(charge)) { "IN_USE" }
    store.audit("super_rent_charges", id, row.businessId, reason) { dao.put(row.copy(amountCents = amountCents)) }
    store.charge(charge.copy(amountCents = amountCents), amountCents)
  }

  fun expenseCorrection(
    id: String,
    amountCents: Long,
    date: String,
    category: String,
    reason: String,
  ) = store.transaction {
    val row = requireNotNull(dao.expenseRow(id)) { "EXPENSE" }
    store.reason(reason)
    SuperMoney.positive(amountCents)
    SuperDates.validate(date)
    require(!row.voided && row.deductionId == null) { "IN_USE" }
    store.audit("super_expenses", id, row.businessId, reason) {
      dao.put(row.copy(amountCents = amountCents, occurredOn = date, category = store.name(category)))
    }
    val ledger = requireNotNull(dao.ledgerRow("expense:$id"))
    dao.put(ledger.copy(spentCents = amountCents, occurredOn = date))
  }

  fun moneyIn(
    id: String,
    reason: String,
  ) = store.transaction {
    val row = requireNotNull(dao.moneyInRow(id)) { "AMOUNT" }
    store.reason(reason)
    require(!row.voided) { "ALREADY_VOID" }
    store.audit("super_money_in", id, row.businessId, reason) { dao.put(row.copy(voided = true)) }
    dao.ledgerRow("money:$id")?.let { dao.put(it.copy(voided = true)) }
  }
}
