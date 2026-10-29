package com.aistudio.fasmovapos.app.superapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.ui.simple.BusinessStatement
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SuperStatementTest {
  private lateinit var db: FasMovaPOSDatabase
  private lateinit var r: SuperRepository
  private val period = RecordPeriod("2026-10-01", "2026-10-31")

  @Before fun setup() {
    db =
      Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), FasMovaPOSDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    r = SuperRepository(db) { SuperDates.formatter("yyyy-MM-dd").parse("2026-10-29")!!.time }
  }

  @After fun close() {
    db.close()
  }

  @Test fun allFourTypesUseTheSameScopedFormatAndExcludePrivateFields() {
    val reports =
      Livelihood.entries.map { type ->
        val b = r.createBusiness(type, "Demo $type", true).id
        val party =
          if (type == Livelihood.RENTAL) {
            val property = r.property(b, "SECRET_PROPERTY")
            val unit = r.unit(b, property, "SECRET_UNIT", 10000, 5, "SECRET_PERSON", "2026-10-01", 5000, "0712345678")
            r.rentCharges(b, "2026-10")
            r.tenantParty(unit)
          } else {
            r.addParty(b, "SECRET_PERSON", if (type == Livelihood.DAIRY) "COOPERATIVE" else "CUSTOMER", 5000, "0712345678").id
          }
        when (type) {
          Livelihood.DAIRY -> r.milk(b, party, "2026-10-28", "MORNING", "2", 5000)
          Livelihood.RENTAL -> Unit
          else -> {
            val item = r.item(b, "SECRET_ITEM", 10000, "GENERAL", 10000)
            r.sale(b, mapOf(item to 1000L), 0, "2026-10-28", "CASH", party)
          }
        }
        r.receipt(b, party, 5000, "2026-10-29", "MPESA", "SECRET_REFERENCE")
        r.expense(b, 1000, "2026-10-29", "OTHER", note = "SECRET_PERSONAL_NOTE")
        r.moneyIn(b, 90000, "2026-10-29", "OWNER_CAPITAL")
        val before = r.dao.ledgerRows(b)
        val report = r.businessStatement(b, period)
        val rows = report.rows.toMap()
        assertEquals("100.00", rows["earned_sales_or_rent_charged"])
        assertEquals("50.00", rows["received_against_business_charges"])
        assertEquals("10.00", rows["recorded_expenses"])
        assertEquals(if (type == Livelihood.RENTAL) "40.00" else "90.00", rows["difference_before_unrecorded_costs"])
        assertEquals("50.00", rows["owed_as_of_generation_all_posted_dates"])
        assertEquals("900.00", rows["other_money_in_not_sales_in_period"])
        assertEquals(if (type == Livelihood.RENTAL) "50.00" else "0.00", rows["recorded_held_money_as_of_generation_not_income"])
        assertEquals(b, rows["business_id"])
        assertEquals("apen-business-statement/2", rows["schema"])
        assertEquals(before, r.dao.ledgerRows(b))
        assertFalse(report.csv().contains("SECRET_"))
        assertFalse(report.csv().contains("0712345678"))
        val app = ApplicationProvider.getApplicationContext<Context>()
        for (language in listOf("en", "sw")) {
          val config =
            android.content.res
              .Configuration(app.resources.configuration)
              .apply { setLocale(java.util.Locale.forLanguageTag("$language-KE")) }
          val rendered = superStatementText(app.createConfigurationContext(config), report).lines.joinToString("\n")
          assertFalse(rendered.contains("SECRET_"))
          assertTrue(rendered.contains(report.digest()))
        }
        report
      }
    assertEquals(4, reports.size)
    reports.forEach { assertEquals(reports.first().rows.map { it.first }, it.rows.map { row -> row.first }) }
    assertEquals(4, reports.map { it.id }.distinct().size)
  }

  @Test fun missingPeriodIsUnknownAndLaterCollectionDoesNotCreateAnotherSale() {
    val b = r.createBusiness(Livelihood.DAIRY, "Dairy").id
    val p = r.addParty(b, "Society", "COOPERATIVE", 5000).id
    r.markNoTrade(b, "2026-10-28")
    r.milk(b, p, "2026-10-28", "MORNING", "2", 5000)
    r.receipt(b, p, 6000, "2026-11-01", "CASH")
    val october = r.businessStatement(b, period)
    assertEquals("0.00", october.rows.toMap()["received_against_business_charges"])
    assertEquals("40.00", october.rows.toMap()["owed_as_of_generation_all_posted_dates"])
    val old = r.businessStatement(b, RecordPeriod("2026-09-01", "2026-09-01")).rows.toMap()
    assertEquals("unknown_no_records", old["period_totals_status"])
    assertEquals("", old["earned_sales_or_rent_charged"])
    assertEquals("1", old["days_without_records_or_marker"])
    val frozen = october.csv()
    r.expense(b, 500, "2026-10-29", "FEED")
    assertEquals(frozen, october.csv())
    val overlap = r.businessStatement(b, RecordPeriod("2026-10-28", "2026-10-28")).rows.toMap()
    assertEquals("1", overlap["days_with_entries"])
    assertEquals("0", overlap["marked_no_trade_days_without_entries"])
  }

  @Test fun negativeDifferenceIsNumericWhileUntrustedFormulaTextStaysEscaped() {
    val report = BusinessStatement("id", listOf("difference_before_unrecorded_costs" to "-500.00", "business" to "=HYPERLINK(\"unsafe\")"))
    assertTrue(report.csv().contains("\"-500.00\""))
    assertTrue(report.csv().contains("\"'=HYPERLINK"))
    assertEquals(64, report.digest().length)
  }
}
