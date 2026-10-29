package com.aistudio.fasmovapos.app.ui.simple

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp")
class SnackbarActionTest {
  @get:Rule val rule = createComposeRule()

  @Test
  fun stockSaveMessageDoesNotCoverTheSaleBarOrInterceptItsTap() {
    val state = SnackbarHostState()
    var charged = false
    rule.setContent {
      FasMovaPOSTheme {
        Scaffold(snackbarHost = { LiteSnackbarHost(state) }, bottomBar = { SimpleNavigation("POS", {}) }) { padding ->
          Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f))
            SimpleCartBar(1, 40.0, { charged = true })
          }
        }
        LaunchedEffect(Unit) { state.showSnackbar("Product added successfully", duration = SnackbarDuration.Indefinite) }
      }
    }
    val bar = rule.onNodeWithTag("simple_charge_bar").assertIsDisplayed()
    val message = rule.onNodeWithText("Product added successfully").assertIsDisplayed()
    assertTrue(message.fetchSemanticsNode().boundsInRoot.bottom <= bar.fetchSemanticsNode().boundsInRoot.top)
    bar.performClick()
    assertTrue(charged)
  }
}
