package com.aistudio.fasmovapos.app.data.companion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoanCompanionTest {
  @Test
  fun catalogueContainsAllPublishedProductsAcrossNineCategories() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val products =
      context.assets
        .open("inuka-product-facts.json")
        .bufferedReader()
        .use { InukaCatalogue.decode(it.readText()) }
    assertEquals(18, products.size)
    assertEquals(9, products.map { it.category }.distinct().size)
    assertEquals(18, products.map { it.id }.distinct().size)
    assertEquals("Varies by item", products.single { it.name == "Asset Finance – Green Energy" }.range)
    assertTrue(products.none { it.range == "KES 0" })
  }

  @Test
  fun amountsKeepExactMinorUnitsAndRejectMalformedOrNonPositiveInput() {
    assertEquals(12345L, LoanPlanInput.amountCents("123.45"))
    assertEquals(10L, LoanPlanInput.amountCents("0.10"))
    listOf("", "0", "-1", "1.234", "NaN", "1e5", "999999999999999999999").forEach {
      assertNull(it, LoanPlanInput.amountCents(it))
    }
  }

  @Test
  fun datesRejectImpossibleOrAmbiguousCalendarValues() {
    assertTrue(LoanPlanInput.validDate("2026-11-02"))
    assertTrue(LoanPlanInput.validDate("2028-02-29"))
    listOf("2026-02-29", "2026-11-31", "02/11/2026", "", "2026-13-01").forEach {
      assertFalse(it, LoanPlanInput.validDate(it))
    }
  }

  @Test
  fun codecRoundTripsWithoutChangingAmountsOrProductIdentity() {
    val plans = listOf(LoanPlan("a", "Inua Loan", 105L, "2026-11-02"), LoanPlan("b", "Afya Loan", 30000L, "2026-12-01"))
    assertEquals(plans, LoanPlanCodec.decode(LoanPlanCodec.encode(plans)))
    assertTrue(LoanPlanCodec.decode("[]").isEmpty())
  }

  @Test(expected = JSONException::class)
  fun corruptStoredPlansAreRejectedRatherThanSilentlyReset() {
    LoanPlanCodec.decode("broken data")
  }

  @Test
  fun savingAndReopeningStoreRetainsPlansAndRemovalIsSpecific() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val store = LoanPlansStore(context)
      val a = LoanPlan("persistence-test-a", "Inua Loan", 15000L, "2026-11-02")
      val b = LoanPlan("persistence-test-b", "Afya Loan", 9000L, "2026-11-03")
      try {
        store.save(a)
        store.save(b)
        store.save(a.copy(amountCents = 15100L))
        val reopened = LoanPlansStore(context).plans.first()
        assertEquals(1, reopened.count { it.id == a.id })
        assertEquals(15100L, reopened.single { it.id == a.id }.amountCents)
        store.remove(a.id)
        assertTrue(store.plans.first().any { it.id == b.id })
        assertFalse(store.plans.first().any { it.id == a.id })
      } finally {
        store.remove(a.id)
        store.remove(b.id)
      }
    }
}
