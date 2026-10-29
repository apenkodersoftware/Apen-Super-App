package com.aistudio.fasmovapos.app.superapp

/** Charges, receipts and deductions commit together; a collection never creates earned income. */
class AccountRepository(
  private val store: RecordStore,
) {
  private val dao get() = store.dao

  fun receipt(
    businessId: String,
    partyId: String?,
    amountCents: Long,
    date: String,
    method: String,
    reference: String = "",
    deductions: List<Deduction> = emptyList(),
    saleId: String? = null,
  ): String =
    store.transaction {
      store.business(businessId)
      partyId?.let { store.party(businessId, it) }
      SuperDates.validate(date)
      require(amountCents in 0..SuperMoney.MAX_CENTS && method != "CREDIT") { "AMOUNT" }
      store.method(method)
      require(deductions.isEmpty() || store.business(businessId).type == Livelihood.DAIRY.name) { "DEDUCTION" }
      deductions.forEach {
        require(it.category !in setOf("SHARES", "ADVANCE", "LOAN_RECOVERY") || !it.countsAsCost) { "DEDUCTION" }
        SuperMoney.positive(it.amountCents)
        require(
          it.category in setOf("FEED", "VET_AI", "SHARES", "ADVANCE", "LOAN_RECOVERY", "OTHER"),
        ) { "DEDUCTION" }
      }
      val settled = deductions.fold(amountCents) { total, d -> Math.addExact(total, d.amountCents) }
      SuperMoney.positive(settled)
      val charges =
        dao
          .chargeRows(businessId)
          .filter {
            !it.voided &&
              (
                if (saleId !=
                  null
                ) {
                  it.sourceTable == "super_sales" && it.sourceId == saleId
                } else {
                  it.partyId == partyId && partyId != null
                }
              )
          }.sortedWith(compareBy({ it.occurredOn }, { it.id }))
      val available = if (saleId == null) store.balance(businessId, requireNotNull(partyId)) else charges.sumOf { store.openCharge(it) }
      require(settled <= available) { "OVERPAYMENT" }
      val id = store.id()
      val propertyId = charges.mapNotNull { it.propertyId }.distinct().singleOrNull()
      val receipt =
        ReceiptRow(id, businessId, partyId, amountCents, method, reference.trim().take(80), saleId, propertyId, date, store.now())
      store.audit("super_receipts", id, businessId, "Recorded") { dao.put(receipt) }
      var remaining = settled
      charges.forEach { charge ->
        val used = minOf(remaining, store.openCharge(charge).coerceAtLeast(0))
        if (used > 0) {
          dao.put(AllocationRow("$id:${charge.id}", businessId, id, charge.id, used, date, store.now()))
          remaining -= used
        }
      }
      require(remaining == 0L) { "ALLOCATION" }
      deductions.forEachIndexed { index, d ->
        val deductionId = "$id:deduction:$index"
        dao.put(DeductionRow(deductionId, businessId, id, d.category, d.amountCents, d.countsAsCost, date, store.now()))
        if (d.countsAsCost) expense(businessId, d.amountCents, date, d.category, "MIXED", "", "", deductionId, null)
      }
      store.ledger(
        LedgerRow(
          "receipt:$id",
          businessId,
          "super_receipts",
          id,
          partyId,
          receivedCents = amountCents,
          owedDeltaCents = -settled,
          settledCents =
            settled - amountCents,
          propertyId = propertyId,
          occurredOn = date,
          recordedAt = receipt.recordedAt,
        ),
      )
      id
    }

  fun expense(
    businessId: String,
    amountCents: Long,
    date: String,
    category: String,
    method: String = "CASH",
    reference: String = "",
    note: String = "",
    deductionId: String? = null,
    propertyId: String? = null,
  ): String =
    store.transaction {
      store.business(businessId)
      SuperMoney.positive(amountCents)
      SuperDates.validate(date)
      store.method(method)
      propertyId?.let { require(dao.propertyRow(it)?.businessId == businessId) { "PROPERTY" } }
      val id = store.id()
      val row =
        ExpenseRow(
          id,
          businessId,
          amountCents,
          store.name(category),
          method,
          reference.take(80),
          note.take(500),
          deductionId,
          propertyId,
          date,
          store.now(),
        )
      store.audit("super_expenses", id, businessId, "Recorded") { dao.put(row) }
      store.ledger(
        LedgerRow(
          "expense:$id",
          businessId,
          "super_expenses",
          id,
          spentCents = amountCents,
          propertyId = propertyId,
          occurredOn = date,
          recordedAt = row.recordedAt,
        ),
      )
      id
    }

  fun moneyIn(
    businessId: String,
    amountCents: Long,
    date: String,
    kind: String,
    method: String = "CASH",
    note: String = "",
  ): String =
    store.transaction {
      store.business(businessId)
      SuperMoney.positive(amountCents)
      SuperDates.validate(date)
      store.method(method)
      require(kind in setOf("OWNER_CAPITAL", "LOAN_PROCEEDS", "GRANT", "TRANSFER")) { "KIND" }
      val id = store.id()
      val row = MoneyInRow(id, businessId, amountCents, kind, method, note = note.take(500), occurredOn = date, recordedAt = store.now())
      store.audit("super_money_in", id, businessId, "Recorded") { dao.put(row) }
      store.ledger(
        LedgerRow(
          "money:$id",
          businessId,
          "super_money_in",
          id,
          otherInCents = amountCents,
          occurredOn = date,
          recordedAt = row.recordedAt,
        ),
      )
      id
    }

  fun allocations(receiptId: String): List<Allocation> {
    val row = requireNotNull(dao.receiptRow(receiptId)) { "RECEIPT" }
    return dao
      .allocationRows(row.businessId)
      .filter {
        !it.voided && it.receiptId == receiptId
      }.map { Allocation(it.chargeId, dao.chargeRow(it.chargeId)?.month.orEmpty(), it.amountCents) }
  }
}
