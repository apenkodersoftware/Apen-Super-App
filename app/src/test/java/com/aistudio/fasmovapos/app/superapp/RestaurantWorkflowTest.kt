package com.aistudio.fasmovapos.app.superapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RestaurantWorkflowTest {
  @Test fun menuCorrectionsArchiveAndBoundedPhotoPreserveHistoryAndSales() {
    val db =
      Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), FasMovaPOSDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      val repo = SuperRepository(db)
      val b = repo.createBusiness(Livelihood.RESTAURANT, "Demo food", true).id
      val item = repo.item(b, "Tea", 2000, "DRINKS")
      assertNull(repo.dao.commerceItem(item)!!.stockMilli)
      repo.sale(b, mapOf(item to 1000L), 2000, "2026-10-28", "CASH")
      repo.commerce.menuDetails(item, "Chai", 2500, "DRINKS", byteArrayOf(1, 2), true, "New menu price")
      assertEquals(2000L, repo.snapshot(b, "2026-10-28", "2026-10-28").earnedCents)
      assertTrue(repo.history(item).any { it.reason == "New menu price" && it.before!!.contains("Tea") && it.after!!.contains("Chai") })
      assertThrows(IllegalArgumentException::class.java) { repo.sale(b, mapOf(item to 1000L), 2500, "2026-10-28", "CASH") }
      assertThrows(IllegalArgumentException::class.java) {
        repo.commerce.menuDetails(item, "Oversized", 2500, "DRINKS", ByteArray(65537), false, "Invalid large photo")
      }
      assertEquals("Chai", repo.dao.commerceItem(item)!!.name)
      assertTrue(repo.dao.commerceItem(item)!!.archived)
    } finally {
      db.close()
    }
  }
}
