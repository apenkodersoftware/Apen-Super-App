package com.aistudio.fasmovapos.app.onboarding

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TermsRecordsContractTest {
  @Test fun renderedEnglishAndSwahiliTermsExplainNewRecordsAndSharingAndHaveRealParagraphs() {
    val app = ApplicationProvider.getApplicationContext<Context>()
    listOf("en" to "R3 BUSINESS RECORDS AND SHARING", "sw" to "REKODI NA USHIRIKI WA R3").forEach { (language, heading) ->
      val config = Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag("$language-KE")) }
      val body = app.createConfigurationContext(config).getString(R.string.terms_body)
      assertTrue(body.contains(heading))
      assertTrue(body.contains("29"))
      val scopeWords =
        if (language ==
          "en"
        ) {
          listOf("dairy", "rental", "food", "shop")
        } else {
          listOf("maziwa", "kupangisha", "chakula", "maduka")
        }
      scopeWords.forEach { assertTrue(body.lowercase().contains(it)) }
      assertTrue(body.contains("\n"))
      assertFalse(body.contains("\\n"))
      for (section in 1..11) assertTrue(body.contains("$section. "))
    }
  }
}
