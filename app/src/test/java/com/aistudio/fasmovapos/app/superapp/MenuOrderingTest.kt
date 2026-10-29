package com.aistudio.fasmovapos.app.superapp

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuOrderingTest {
  @Test fun soldItemsRiseAboveUnsoldItemsAfterFirstSaleWithoutMixedNumberCrash() {
    val chai = CommerceItem("tea", "b", "Chai", 2000, "DRINKS")
    val chapati = CommerceItem("bread", "b", "Chapati", 2000, "FOOD")
    val githeri = CommerceItem("meal", "b", "Githeri", 6000, "FOOD")
    val sale = CommerceLine("line", "b", "sale", "meal", "Githeri", 1000, 6000, 6000)
    assertEquals(listOf("Githeri", "Chai", "Chapati"), menuBySales(listOf(chapati, githeri, chai), listOf(sale)).map { it.name })
    assertEquals(
      listOf("Chai", "Chapati", "Githeri"),
      menuBySales(listOf(chapati, githeri, chai), listOf(sale.copy(voided = true))).map {
        it.name
      },
    )
    assertEquals(listOf("Chai", "Chapati"), menuBySales(listOf(chapati, githeri.copy(archived = true), chai), listOf(sale)).map { it.name })
  }
}
