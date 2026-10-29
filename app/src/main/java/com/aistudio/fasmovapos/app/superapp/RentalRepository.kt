package com.aistudio.fasmovapos.app.superapp

class RentalRepository(
  private val store: RecordStore,
) {
  private val dao get() = store.dao

  fun property(
    businessId: String,
    name: String,
  ): String =
    store.transaction {
      store.type(businessId, Livelihood.RENTAL)
      val id = store.id()
      require(dao.propertyRows(businessId).none { !it.voided && it.name.equals(name.trim(), true) }) { "NAME" }
      val row = PropertyRow(id, businessId, store.name(name), SuperDates.today(store.now()), store.now())
      store.audit("super_properties", id, businessId, "Recorded") { dao.put(row) }
      id
    }

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
  ): String =
    store.transaction {
      store.type(businessId, Livelihood.RENTAL)
      require(dao.propertyRow(propertyId)?.businessId == businessId) { "PROPERTY" }
      SuperMoney.positive(rentCents)
      require(dueDay in 1..28) { "DUE_DAY" }
      SuperDates.validate(moveIn)
      require(dao.rentalUnitRows(businessId).count { !it.voided } < 50) { "UNIT_LIMIT" }
      require(
        dao.rentalUnitRows(businessId).none { !it.voided && it.propertyId == propertyId && it.label.equals(label.trim(), true) },
      ) { "NAME" }
      require(tenant.isNotBlank() || depositCents == 0L) { "PARTY" }
      val id = store.id()
      val row = RentalUnitRow(id, businessId, propertyId, store.name(label), rentCents, dueDay, moveIn, store.now())
      store.audit("super_units", id, businessId, "Recorded") { dao.put(row) }
      if (tenant.isNotBlank())tenancy(id, tenant, phone, moveIn, depositCents)
      id
    }

  fun tenancy(
    unitId: String,
    name: String,
    phone: String,
    moveIn: String,
    depositCents: Long = 0,
  ): String =
    store.transaction {
      val unit = requireNotNull(dao.rentalUnitRow(unitId)) { "UNIT" }
      require(!unit.voided) { "UNIT" }
      SuperDates.validate(moveIn)
      require(depositCents in 0..SuperMoney.MAX_CENTS && phone.length <= 40) { "AMOUNT" }
      require(
        dao.tenancyRows(unit.businessId).none {
          !it.voided && it.unitId == unitId && (it.moveOut == null || it.moveOut > moveIn)
        },
      ) { "TENANCY" }
      val party =
        Party(store.id(), unit.businessId, store.name(name), "TENANT", phone.trim(), occurredOn = moveIn, recordedAt = store.now())
      store.audit("super_parties", party.id, unit.businessId, "Recorded") { dao.put(party) }
      val row =
        TenancyRow(
          store.id(),
          unit.businessId,
          unitId,
          party.id,
          moveIn,
          depositCents = depositCents,
          occurredOn = moveIn,
          recordedAt = store.now(),
        )
      store.audit("super_tenancies", row.id, unit.businessId, "Recorded") { dao.put(row) }
      if (depositCents >
        0
      ) {
        store.ledger(
          LedgerRow(
            "deposit:${row.id}",
            unit.businessId,
            "super_tenancies",
            row.id,
            party.id,
            heldCents = depositCents,
            propertyId = unit.propertyId,
            occurredOn = moveIn,
            recordedAt = row.recordedAt,
          ),
        )
      }
      row.id
    }

  /** Deterministic tenancy/month IDs make repeated and missed-month preparation idempotent. */
  fun charges(
    businessId: String,
    throughMonth: String,
  ): Int =
    store.transaction {
      store.type(businessId, Livelihood.RENTAL)
      SuperDates.month(throughMonth)
      var count = 0
      dao.tenancyRows(businessId).filter { !it.voided }.forEach { tenant ->
        val unit = requireNotNull(dao.rentalUnitRow(tenant.unitId)) { "UNIT" }
        var month = tenant.moveIn.take(7)
        var months = 0
        while (month <= throughMonth) {
          require(months++ < 1200) { "DATE" }
          val start = "$month-01"
          val next = "${SuperDates.addMonths(month,1)}-01"
          val chargeOn = maxOf(start, tenant.moveIn)
          val due = maxOf("$month-${unit.dueDay.toString().padStart(2,'0')}", tenant.moveIn)
          if (tenant.moveIn < next && (tenant.moveOut == null || tenant.moveOut > chargeOn)) {
            val id = "rent:${tenant.id}:$month"
            if (dao.rentCharge(id) == null) {
              val row = RentCharge(id, businessId, unit.id, tenant.id, tenant.partyId, month, unit.rentCents, due, chargeOn, store.now())
              store.audit("super_rent_charges", id, businessId, "Recorded") { dao.put(row) }
              store.charge(
                ChargeRow(
                  id,
                  businessId,
                  tenant.partyId,
                  "super_rent_charges",
                  id,
                  unit.rentCents,
                  month,
                  unit.propertyId,
                  row.occurredOn,
                  row.recordedAt,
                ),
                unit.rentCents,
              )
              count++
            }
          }
          month = SuperDates.addMonths(month, 1)
        }
      }
      count
    }

  fun moveOut(
    unitId: String,
    vacantFrom: String,
    reason: String,
  ) = store.transaction {
    val unit = requireNotNull(dao.rentalUnitRow(unitId)) { "UNIT" }
    SuperDates.validate(vacantFrom)
    store.reason(reason)
    val tenant =
      dao.tenancyRows(unit.businessId).lastOrNull { !it.voided && it.unitId == unitId && it.moveOut == null }
        ?: throw IllegalArgumentException("TENANCY")
    require(vacantFrom >= tenant.moveIn) { "DATE" }
    val future = dao.rentCharges(unit.businessId).filter { !it.voided && it.tenancyId == tenant.id && it.occurredOn >= vacantFrom }
    future.forEach { r ->
      val charge = requireNotNull(dao.chargeRow(r.id))
      require(store.openCharge(charge) == charge.amountCents) { "IN_USE" }
    }
    store.audit("super_tenancies", tenant.id, unit.businessId, reason) { dao.put(tenant.copy(moveOut = vacantFrom)) }
    future.forEach { r ->
      store.audit("super_rent_charges", r.id, unit.businessId, reason) { dao.put(r.copy(voided = true)) }
      dao.chargeRow(r.id)?.let { dao.put(it.copy(voided = true)) }
      dao.ledgerRow("charge:${r.id}")?.let { dao.put(it.copy(voided = true)) }
    }
  }

  fun tenantParty(unitId: String): String {
    val unit = requireNotNull(dao.rentalUnitRow(unitId)) { "UNIT" }
    return requireNotNull(
      dao
        .tenancyRows(unit.businessId)
        .filter {
          !it.voided && it.unitId == unitId
        }.maxByOrNull { it.moveIn },
    ) { "TENANCY" }.partyId
  }

  fun reminder(unitId: String): String {
    val unit = requireNotNull(dao.rentalUnitRow(unitId)) { "UNIT" }
    val party = tenantParty(unitId)
    return "${dao.propertyRow(unit.propertyId)?.name.orEmpty()}, ${unit.label}: ${SuperMoney.label(store.balance(unit.businessId,party))}"
  }
}
