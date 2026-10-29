package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.aistudio.fasmovapos.app.data.db.entities.BranchEntity
import com.aistudio.fasmovapos.app.data.repository.SampleData
import com.aistudio.fasmovapos.app.ui.components.DisplayControl
import com.aistudio.fasmovapos.app.ui.language.AppLanguage
import com.aistudio.fasmovapos.app.ui.screens.CustomersScreen
import com.aistudio.fasmovapos.app.ui.screens.ExpensesScreen
import com.aistudio.fasmovapos.app.ui.screens.SuppliersScreen
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import com.aistudio.fasmovapos.app.ui.theme.ThemeMode
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "sw-rKE-w360dp-h800dp")
class SwahiliMenuWidthTest {
  @get:Rule val rule = createComposeRule()

  @Before
  fun useLargeSystemTextForMainAndDialogWindows() {
    RuntimeEnvironment.setFontScale(1.5f)
  }

  @After
  fun restoreSystemText() {
    RuntimeEnvironment.setFontScale(1f)
  }

  private fun show(content: @Composable () -> Unit) {
    rule.setContent {
      FasMovaPOSTheme {
        SimpleModeTheme {
          val density = LocalDensity.current
          CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) { content() }
        }
      }
    }
  }

  private fun readable(text: String) {
    val node = rule.onNodeWithText(text, useUnmergedTree = true)
    node.assertIsDisplayed()
    val layouts = mutableListOf<TextLayoutResult>()
    node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
    assertFalse(
      "Clipped Kiswahili: $text, size=${layouts.single().size}, width=${layouts.single().didOverflowWidth}, height=${layouts.single().didOverflowHeight}, lines=${layouts.single().lineCount}",
      layouts.single().hasVisualOverflow,
    )
  }

  @Test
  fun longTranslatedTrainingNameAndPriceFitTheCatalogueCard() {
    val product = SampleData.products().first { it.id == "p-3" }
    show { SimpleCatalog(CatalogState(listOf(product), "", "All"), {}, {}, {}, {}) }
    readable("Bakuli la chuma lisilopata kutu")
    readable("KES 120")
    readable("Zimebaki 24")
  }

  @Test
  fun moreMenuKeepsLongOptionsAndTermsReachable() {
    show {
      MoreScreen(
        BranchEntity("b-1", "Duka", "HQ", "", true),
        DisplayControl(ThemeMode.SYSTEM, {
        }, AppLanguage.SWAHILI, {}),
        true,
        {},
        { _, _, _ -> },
        {},
      )
    }
    rule.onNodeWithTag("simple_more_list").performScrollToNode(hasTestTag("simple_more_shop"))
    readable("Mipangilio ya duka")
    rule.onNodeWithTag("simple_more_list").performScrollToNode(hasTestTag("simple_more_terms"))
    readable("Masharti na faragha")
    rule.onNodeWithTag("simple_more_list").performScrollToNode(hasTestTag("simple_more_logout"))
    readable("Toka")
  }

  @Test
  fun customerHeadingAndAddActionFitWithoutCompetingForWidth() {
    show { CustomersScreen(emptyList(), emptyList(), { _, _, _, _ -> }, { _, _, _, _ -> }) }
    readable("Jumla ya madeni")
    readable("Ongeza mteja")
  }

  @Test
  fun expenseHeadingAndAddActionFitWithoutCompetingForWidth() {
    show { ExpensesScreen(emptyList(), { _, _, _ -> }) }
    readable("Jumla ya matumizi")
    readable("Ongeza matumizi")
  }

  @Test
  fun supplierHeadingAndAddActionFitWithoutCompetingForWidth() {
    show { SuppliersScreen(emptyList(), emptyList(), { _, _, _, _, _ -> }, { _, _ -> }) }
    readable("Wasambazaji na maagizo")
    readable("Ongeza msambazaji")
  }

  @Test
  fun expenseFormStartsInSwahiliAndItsActionsRemainReadable() {
    show { ExpensesScreen(emptyList(), { _, _, _ -> }) }
    rule.onNodeWithText("Ongeza matumizi").performClick()
    readable("Rekodi matumizi")
    readable("Aina ya matumizi")
    rule.onNodeWithText("Kodi ya pango").assertExists()
    rule.onNodeWithText("Rent").assertDoesNotExist()
    rule.onNodeWithText("Hifadhi matumizi").performScrollTo()
    readable("Hifadhi matumizi")
    readable("Ghairi")
  }

  @Test
  fun customerFormScrollsToReadableActionsWithLargeText() {
    show { CustomersScreen(emptyList(), emptyList(), { _, _, _, _ -> }, { _, _, _, _ -> }) }
    rule.onNodeWithText("Ongeza mteja").performClick()
    rule.onNodeWithText("Hifadhi mteja").performScrollTo()
    rule.onNodeWithText("Kikomo cha deni (KES)", useUnmergedTree = true).performScrollTo()
    readable("Kikomo cha deni (KES)")
    val labelBounds = rule.onNodeWithText("Kikomo cha deni (KES)", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    val emailLabel = rule.onNodeWithText("Barua Pepe", useUnmergedTree = true).fetchSemanticsNode()
    val emailField = generateSequence(emailLabel) { it.parent }.first { it.config.contains(SemanticsActions.SetText) }
    val emailBounds = emailField.boundsInRoot
    assertTrue("Floating credit label overlaps the email field", labelBounds.top >= emailBounds.bottom)
    rule.onNodeWithText("Hifadhi mteja").performScrollTo()
    readable("Hifadhi mteja")
    readable("Ghairi")
  }

  @Test
  fun supplierFormScrollsToReadableActionsWithLargeText() {
    show { SuppliersScreen(emptyList(), emptyList(), { _, _, _, _, _ -> }, { _, _ -> }) }
    rule.onNodeWithText("Ongeza msambazaji").performClick()
    rule.onNodeWithText("Hifadhi Msambazaji").performScrollTo()
    readable("Hifadhi Msambazaji")
    readable("Ghairi")
  }

  @Test
  fun translatedCustomerDetailsUseTheFullCardWidthAndOneScrollingList() {
    show { CustomersScreen(SampleData.customers(), emptyList(), { _, _, _, _ -> }, { _, _, _, _ -> }) }
    readable("Mteja wa mafunzo")
    rule.onNodeWithText("Deni la Sasa: KES 0.00").performScrollTo()
    readable("Deni la Sasa: KES 0.00")
    rule.onNodeWithText("Malipo ya madeni").performScrollTo()
    readable("Malipo ya madeni")
  }

  @Test
  fun translatedSupplierDetailsAndOrderActionUseTheFullCardWidth() {
    show { SuppliersScreen(SampleData.suppliers(), emptyList(), { _, _, _, _, _ -> }, { _, _ -> }) }
    readable("Msambazaji wa mafunzo")
    rule.onNodeWithText("+ Unda Agizo").performScrollTo()
    readable("+ Unda Agizo")
  }
}
