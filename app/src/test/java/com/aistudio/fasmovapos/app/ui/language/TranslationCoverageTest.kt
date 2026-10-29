package com.aistudio.fasmovapos.app.ui.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class TranslationCoverageTest {
  @Test
  fun englishAndSwahiliHaveEveryKeyAndMatchingFormatArguments() {
    val english = resources("values")
    val swahili = resources("values-sw")
    assertEquals(english.keys, swahili.keys)
    val format = Regex("%[0-9$.*]*[sdf]")
    english.forEach { (key, value) ->
      assertEquals(
        key,
        format
          .findAll(value)
          .map {
            it.value
          }.toList()
          .sorted(),
        format
          .findAll(swahili.getValue(key))
          .map { it.value }
          .toList()
          .sorted(),
      )
    }
  }

  @Test
  fun bothTermsDocumentsIdentifyApenAndKeepAllElevenSections() {
    for (folder in listOf("values", "values-sw")) {
      val body = resources(folder).getValue("terms_body")
      assertTrue(body.contains("APEN REVIEW"))
      assertFalse(body.contains("FasMova", ignoreCase = true))
      assertFalse(body.contains("MaliMali", ignoreCase = true))
      for (section in 1..11) assertTrue("Missing section $section in $folder", body.contains("$section. "))
    }
  }

  private fun resources(folder: String): Map<String, String> {
    val root = File("src/main/res/$folder")
    assertTrue("Resource directory missing: $root", root.isDirectory)
    val result = mutableMapOf<String, String>()
    root.listFiles { file -> file.extension == "xml" }!!.forEach { file ->
      val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
      val children = document.documentElement.childNodes
      for (index in 0 until children.length) {
        val element = children.item(index)
        if (element.nodeName == "string") {
          val key = element.attributes.getNamedItem("name").nodeValue
          assertFalse("Duplicate string $key", result.containsKey(key))
          result[key] = element.textContent
        }
      }
    }
    return result
  }
}
