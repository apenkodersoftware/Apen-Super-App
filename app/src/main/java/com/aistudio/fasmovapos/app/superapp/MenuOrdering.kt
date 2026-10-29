package com.aistudio.fasmovapos.app.superapp

/** Keep missing counts a Long as well; mixed Comparable types crash after the first sale. */
fun menuBySales(
  items: List<CommerceItem>,
  lines: List<CommerceLine>,
): List<CommerceItem> {
  val counts: Map<String, Long> =
    lines
      .filter { !it.voided }
      .groupBy { it.itemId }
      .mapValues { (_, rows) -> rows.sumOf { it.quantityMilli } }
  return items
    .filter { !it.voided && !it.archived }
    .sortedWith(compareByDescending<CommerceItem> { counts[it.id] ?: 0L }.thenBy { it.name })
}
