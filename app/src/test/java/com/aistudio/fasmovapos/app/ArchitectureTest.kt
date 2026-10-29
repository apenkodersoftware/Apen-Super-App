package com.aistudio.fasmovapos.app

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class ArchitectureTest {
  private val forbiddenUiPrefixes =
    listOf(
      "androidx.compose.ui",
      "androidx.compose.material",
      "androidx.compose.foundation",
      "android.widget",
      "android.view",
    )

  @Test
  fun `view models should not import android ui classes`() {
    Konsist
      .scopeFromProduction()
      .classes()
      .withNameEndingWith("ViewModel")
      .assertFalse { clazz ->
        clazz.containingFile.hasImport { imp -> forbiddenUiPrefixes.any { imp.name.startsWith(it) } }
      }
  }
}
