package com.aistudio.fasmovapos.app.superapp

/** Shared sale mechanics; restaurant items have no stock and retail items use thousandths. */
class CommerceRepository(
  private val store: RecordStore,
  private val accounts: AccountRepository,
) {
  private val dao get() = store.dao

  fun item(
    businessId: String,
    name: String,
    priceCents: Long,
    category: String,
    stockMilli: Long? = null,
    unit: String = "Pcs",
  ): String =
    store.transaction {
      val profile = store.type(businessId, Livelihood.RETAIL, Livelihood.RESTAURANT)
      SuperMoney.positive(priceCents)
      val stock = if (profile.type == "RESTAURANT") null else stockMilli ?: 0L
      require(stock == null || stock in 0..100_000_000L) { "QUANTITY" }
      require(unit in setOf("Pcs", "Kg", "Ltr", "Box", "Bag")) { "UNIT" }
      require(unit !in setOf("Pcs", "Box", "Bag") || stock == null || stock % 1000L == 0L) { "QUANTITY" }
      val id = store.id()
      val row =
        CommerceItem(
          id,
          businessId,
          store.name(name),
          priceCents,
          category,
          stock,
          unit,
          occurredOn = SuperDates.today(store.now()),
          recordedAt = store.now(),
        )
      store.audit("super_items", id, businessId, "Recorded") { dao.put(row) }
      id
    }

  fun sale(
    businessId: String,
    quantities: Map<String, Long>,
    receivedCents: Long,
    date: String,
    method: String,
    partyId: String? = null,
    reference: String = "",
    discountCents: Long = 0,
  ): String =
    store.transaction {
      val profile = store.type(businessId, Livelihood.RETAIL, Livelihood.RESTAURANT)
      SuperDates.validate(date)
      store.method(method)
      require(quantities.isNotEmpty() && quantities.size <= 200) { "ITEM" }
      val items =
        quantities.map { (id, q) ->
          val item = store.item(businessId, id)
          require(q in 1..100_000_000L) { "QUANTITY" }
          if (item.unit in setOf("Pcs", "Box", "Bag")) require(q % 1000L == 0L) { "QUANTITY" }
          if (profile.type == "RETAIL") require(item.stockMilli != null && item.stockMilli >= q) { "STOCK" }
          item to q
        }
      val gross = items.fold(0L) { sum, (item, q) -> Math.addExact(sum, SuperPricing.lineTotal(item, q)) }
      require(discountCents in 0 until gross && (profile.type == "RETAIL" || discountCents == 0L)) { "AMOUNT" }
      val total = gross - discountCents
      SuperMoney.positive(total)
      require(receivedCents in 0..total && (receivedCents == total || partyId != null)) { "PARTY" }
      require(method != "CREDIT" || receivedCents == 0L) { "METHOD" }
      partyId?.let { store.party(businessId, it) }
      val id = store.id()
      val sale = CommerceSale(id, businessId, partyId, total, method, reference.take(80), date, store.now(), discountCents = discountCents)
      store.audit("super_sales", id, businessId, "Recorded") { dao.put(sale) }
      items.forEach { (item, q) ->
        val line =
          CommerceLine(
            store.id(),
            businessId,
            id,
            item.id,
            item.name,
            q,
            item.priceCents,
            SuperPricing.lineTotal(item, q),
            item.unit,
            date,
            store.now(),
          )
        dao.put(line)
        if (item.stockMilli !=
          null
        ) {
          store.audit("super_items", item.id, businessId, "Sale $id") { dao.put(item.copy(stockMilli = item.stockMilli - q)) }
        }
      }
      store.charge(
        ChargeRow("sale:$id", businessId, partyId, "super_sales", id, total, occurredOn = date, recordedAt = sale.recordedAt),
        total,
      )
      if (receivedCents > 0) accounts.receipt(businessId, partyId, receivedCents, date, method, reference, saleId = id)
      id
    }

  fun customSale(
    businessId: String,
    amountCents: Long,
    date: String,
    method: String,
  ): String =
    store.transaction {
      store.type(businessId, Livelihood.RESTAURANT)
      SuperMoney.positive(amountCents)
      val temp = item(businessId, "Custom amount", amountCents, "CUSTOM")
      val result = sale(businessId, mapOf(temp to 1000L), amountCents, date, method)
      val row = store.item(businessId, temp)
      store.audit("super_items", row.id, businessId, "Custom amount archived") { dao.put(row.copy(archived = true)) }
      result
    }

  fun adjustItem(
    id: String,
    newPriceCents: Long,
    addMilli: Long,
    reason: String,
  ) = store.transaction {
    val row = requireNotNull(dao.commerceItem(id)) { "ITEM" }
    store.reason(reason)
    SuperMoney.positive(newPriceCents)
    require(!row.voided) { "ITEM" }
    val newStock = row.stockMilli?.let { Math.addExact(it, addMilli).also { q -> require(q in 0..100_000_000L) { "QUANTITY" } } }
    require(newStock != null || addMilli == 0L) { "STOCK" }
    require(row.unit !in setOf("Pcs", "Box", "Bag") || newStock == null || newStock % 1000L == 0L) { "QUANTITY" }
    store.audit("super_items", id, row.businessId, reason) { dao.put(row.copy(priceCents = newPriceCents, stockMilli = newStock)) }
  }

  fun menuDetails(
    id: String,
    name: String,
    priceCents: Long,
    category: String,
    photo: ByteArray?,
    archived: Boolean,
    reason: String,
  ) = store.transaction {
    val row = requireNotNull(dao.commerceItem(id)) { "ITEM" }
    store.type(row.businessId, Livelihood.RESTAURANT)
    require(!row.voided && category in setOf("FOOD", "DRINKS", "SNACKS")) { "ITEM" }
    require(photo == null || photo.size <= 64 * 1024) { "PHOTO" }
    store.reason(reason)
    SuperMoney.positive(priceCents)
    store.audit("super_items", id, row.businessId, reason) {
      dao.put(row.copy(name = store.name(name), priceCents = priceCents, category = category, photo = photo, archived = archived))
    }
  }
}
