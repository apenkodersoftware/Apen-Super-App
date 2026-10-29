package com.aistudio.fasmovapos.app.superapp

/** Dairy delivery is the earnings source; a settlement's cash and deductions are separate. */
class DairyRepository(
  private val store: RecordStore,
) {
  private val dao get() = store.dao

  fun milk(
    businessId: String,
    partyId: String,
    date: String,
    session: String,
    litres: String,
    priceCents: Long,
  ): String =
    store.transaction {
      store.type(businessId, Livelihood.DAIRY)
      store.party(businessId, partyId)
      SuperDates.validate(date)
      require(session in setOf("MORNING", "EVENING")) { "SESSION" }
      SuperMoney.positive(priceCents)
      val quantity = SuperMoney.milli(litres)
      val amount = SuperMoney.value(quantity, priceCents)
      SuperMoney.positive(amount)
      val id = store.id()
      val row = MilkDelivery(id, businessId, partyId, session, quantity, priceCents, amount, date, store.now())
      store.audit("super_milk", id, businessId, "Recorded") { dao.put(row) }
      post(row)
      id
    }

  fun edit(
    id: String,
    litres: String,
    reason: String,
  ) = store.transaction {
    val old = requireNotNull(dao.milkDelivery(id)) { "MILK" }
    require(!old.voided) { "MILK" }
    store.reason(reason)
    val q = SuperMoney.milli(litres)
    val row = old.copy(litresMilli = q, earnedCents = SuperMoney.value(q, old.priceCents))
    val charge = requireNotNull(dao.chargeRow("milk:$id"))
    require(row.earnedCents >= charge.amountCents - store.openCharge(charge)) { "IN_USE" }
    store.audit("super_milk", id, row.businessId, reason) { dao.put(row) }
    post(row)
  }

  fun void(
    id: String,
    reason: String,
  ) = store.transaction {
    val row = requireNotNull(dao.milkDelivery(id)) { "MILK" }
    store.reason(reason)
    require(!row.voided) { "MILK" }
    val charge = requireNotNull(dao.chargeRow("milk:$id"))
    require(store.openCharge(charge) == charge.amountCents) { "IN_USE" }
    store.audit("super_milk", id, row.businessId, reason) { dao.put(row.copy(voided = true)) }
    dao.put(charge.copy(voided = true))
    dao.ledgerRow("charge:${charge.id}")?.let { dao.put(it.copy(voided = true)) }
  }

  private fun post(row: MilkDelivery) {
    store.charge(
      ChargeRow(
        "milk:${row.id}",
        row.businessId,
        row.partyId,
        "super_milk",
        row.id,
        row.earnedCents,
        occurredOn = row.occurredOn,
        recordedAt = row.recordedAt,
      ),
      row.earnedCents,
    )
  }
}
