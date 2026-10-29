package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "sw-rKE-w360dp-h800dp")
class SimpleFontScaleTest {
  @get:Rule val rule = createComposeRule()

  private fun assertUnclipped(text: String) {
    val node = rule.onNodeWithText(text, useUnmergedTree = true)
    val layouts = mutableListOf<TextLayoutResult>()
    node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> assertTrue(action(layouts)) }
    val layout = layouts.single()
    assertFalse(
      "Text '$text' overflows: size=${layout.size}, width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}, lines=${layout.lineCount}",
      layout.hasVisualOverflow,
    )
    assertTrue(node.fetchSemanticsNode().boundsInRoot.height + 1 >= layouts.single().size.height)
  }

  @Test
  fun swahiliNavigationFitsAt360dpWithLargeSystemText() {
    rule.setContent {
      FasMovaPOSTheme {
        SimpleModeTheme {
          val density = LocalDensity.current
          CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
            SimpleNavigation("POS", {})
          }
        }
      }
    }
    listOf("Uza", "Bidhaa", "Muhtasari", "Zaidi").forEach(::assertUnclipped)
  }

  @Test
  fun longProductNamesAndPricesRemainReadableWithLargeSystemText() {
    val product = simpleProduct("large", "Beseni la chuma lenye kishikio", price = 1250.50)
    rule.setContent {
      FasMovaPOSTheme {
        SimpleModeTheme {
          val density = LocalDensity.current
          CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
            SimpleCatalog(CatalogState(listOf(product), "", "All"), {}, {}, {}, {})
          }
        }
      }
    }
    assertUnclipped(product.name)
    assertUnclipped("KES 1250.50")
    assertUnclipped("Zimebaki 12")
  }
}
