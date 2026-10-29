package com.aistudio.fasmovapos.app.superapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SuperMoneyTest {
  @Test fun incorrectCommaGroupingCannotSilentlyChangeEnteredMoney() {
    assertEquals(100050L, SuperMoney.cents("1,000.50"))
    assertEquals(50L, SuperMoney.cents(".50"))
    assertThrows(IllegalArgumentException::class.java) { SuperMoney.cents("1,5") }
    assertThrows(IllegalArgumentException::class.java) { SuperMoney.cents("7,,000") }
  }
}
