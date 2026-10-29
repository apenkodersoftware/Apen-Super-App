package com.aistudio.fasmovapos.app.ui.language

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity
import com.aistudio.fasmovapos.app.data.repository.SampleData
import com.aistudio.fasmovapos.app.ui.components.receiptShareText
import com.aistudio.fasmovapos.app.ui.simple.CatalogState
import com.aistudio.fasmovapos.app.ui.simple.SimpleCatalog
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "sw-rKE-w360dp-h800dp")
class CatalogueTranslationTest {
  @get:Rule val rule = createComposeRule()

  @Test
  fun trainingProductLabelsTranslateWithoutMutatingStoredNamesOrStock() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val products = SampleData.products()
    products.forEach { assertNotEquals(it.name, localizedProductName(context, it.id, it.name)) }
    val bucket = products.first { it.id == "p-2" }
    assertEquals("Ndoo yenye mfuniko", localizedProductName(context, bucket.id, bucket.name))
    assertEquals("Bucket with lid", bucket.name)
    assertEquals(12, bucket.stockQuantity)
    assertEquals(350.0, bucket.sellingPrice, 0.0)
  }

  @Test
  fun swahiliSearchFindsTheTranslatedProductAndCategoryChipsTranslate() {
    rule.setContent {
      FasMovaPOSTheme { SimpleCatalog(CatalogState(SampleData.products(), "ndoo", "All"), {}, {}, {}, {}) }
    }
    rule.onNodeWithText("Ndoo yenye mfuniko").assertExists()
    rule.onNodeWithText("Bucket with lid").assertDoesNotExist()
    rule.onNodeWithText("Kikombe cha kauri").assertDoesNotExist()
    rule.onNodeWithText("Vifaa vya usafi").assertExists()
  }

  @Test
  fun customNamesAndRenamedTrainingItemsRemainExactlyAsEntered() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Asha Special", localizedProductName(context, "p-2", "Asha Special"))
    assertEquals("Bucket with lid", localizedProductName(context, "custom-id", "Bucket with lid"))
    assertEquals("Jose", localizedPersonName(context, "u-1", "Jose"))
  }

  @Test
  fun englishViewStillUsesTheOriginalEnglishTrainingLabel() {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val config = Configuration(base.resources.configuration).apply { setLocale(Locale.ENGLISH) }
    val context = base.createConfigurationContext(config)
    assertEquals("Bucket with lid", localizedProductName(context, "p-2", "Bucket with lid"))
  }

  @Test
  fun sharedReceiptUsesSwahiliProductAndContactLabelsWithoutChangingTheSale() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val (sale, items) = SampleData.firstSale()
    val branch = BranchEntity("b-1", "Duka", "HQ", "", true, "0712345678", "A001234567Z")
    val text = receiptShareText(context, sale, items, emptyList(), branch)
    assertTrue(text.contains("Beseni la plastiki"))
    assertTrue(text.contains("SIMU: 0712345678"))
    assertTrue(text.contains("PIN ya KRA: A001234567Z"))
    assertFalse(text.contains("Plastic basin"))
    assertEquals("Plastic basin", items.single().productName)
  }

  @Test
  fun englishAndSwahiliCategoryAliasesShareOneFilterWithoutRenamingRecords() {
    val original = SampleData.products().first { it.id == "p-1" }
    val translated = original.copy(id = "custom-basin", name = "Beseni jipya", category = "Vifaa vya nyumbani")
    rule.setContent {
      FasMovaPOSTheme { SimpleCatalog(CatalogState(listOf(original, translated), "", "Vifaa vya nyumbani"), {}, {}, {}, {}) }
    }
    rule.onAllNodesWithText("Vifaa vya nyumbani").assertCountEquals(1)
    rule.onNodeWithText("Beseni la plastiki").assertExists()
    rule.onNodeWithText("Beseni jipya").assertExists()
    assertEquals("Household", original.category)
    assertEquals("Vifaa vya nyumbani", translated.category)
    assertEquals("My custom category", categoryKey("My custom category"))
  }
}
