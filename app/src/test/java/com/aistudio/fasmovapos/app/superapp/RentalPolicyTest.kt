package com.aistudio.fasmovapos.app.superapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RentalPolicyTest {
  @Test fun missedMonthsMidMonthMoveoutAndDepositAreDistinct() {
    val db =
      Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), FasMovaPOSDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      val repo = SuperRepository(db)
      val b = repo.createBusiness(Livelihood.RENTAL, "Demo policies", true).id
      val p = repo.property(b, "Court")
      val u = repo.unit(b, p, "A1", 800000, 5, "Tenant", "2026-08-20", 800000)
      assertEquals(3, repo.rentCharges(b, "2026-10"))
      assertEquals(0, repo.rentCharges(b, "2026-10"))
      val first = repo.dao.rentCharges(b).first { it.month == "2026-08" }
      assertEquals("2026-08-20", first.occurredOn)
      assertEquals("2026-08-20", first.dueDate)
      repo.moveOut(u, "2026-10-03", "Departed after monthly charge")
      assertEquals(0, repo.rentCharges(b, "2026-11"))
      val s = repo.snapshot(b, "2026-08-01", "2026-11-30")
      assertEquals(2400000L, s.earnedCents)
      assertEquals(0L, s.receivedCents)
      assertEquals(800000L, s.heldCents)
      assertEquals(2400000L, s.owedCents)
    } finally {
      db.close()
    }
  }
}
