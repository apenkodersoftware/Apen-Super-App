package com.aistudio.fasmovapos.app.onboarding

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

// FileStorage chooses its move implementation by Android SDK; SDK 0 stubs use a Windows-incompatible rename path.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TermsAcceptanceTest {
  @get:Rule val folder = TemporaryFolder()
  private val legacy = booleanPreferencesKey("has_accepted_terms")
  private val revision = intPreferencesKey("accepted_fasmovalite_terms_revision")

  @Test
  fun priorAcceptanceRequiresExplicitAcceptanceOfTheCorrectedDocument() =
    runTest {
      val job = SupervisorJob()
      val store = createStore(File(folder.root, "legacy.preferences_pb"), job)
      try {
        store.edit {
          it[legacy] = true
          it[stringPreferencesKey("retained_marker")] = "keep"
        }
        val terms = TermsPreferences(store)
        assertFalse(terms.hasAcceptedTerms.first())
        terms.acceptTerms()
        assertTrue(terms.hasAcceptedTerms.first())
        assertEquals("keep", store.data.first()[stringPreferencesKey("retained_marker")])
        assertEquals(TermsPreferences.CURRENT_REVISION, store.data.first()[revision])
      } finally {
        job.cancelAndJoin()
      }
    }

  @Test
  fun acceptanceSurvivesClosingAndReopeningThePreferenceStore() =
    runTest {
      val file = File(folder.root, "restart.preferences_pb")
      val firstJob = SupervisorJob()
      val firstStore = createStore(file, firstJob)
      try {
        TermsPreferences(firstStore).acceptTerms()
      } finally {
        firstJob.cancelAndJoin()
      }
      val secondJob = SupervisorJob()
      val secondStore = createStore(file, secondJob)
      try {
        assertTrue(TermsPreferences(secondStore).hasAcceptedTerms.first())
      } finally {
        secondJob.cancelAndJoin()
      }
    }

  @Test
  fun aStaleDocumentRevisionDoesNotBypassTheGate() =
    runTest {
      val job = SupervisorJob()
      val store = createStore(File(folder.root, "stale.preferences_pb"), job)
      try {
        store.edit {
          it[legacy] = true
          it[revision] = TermsPreferences.CURRENT_REVISION - 1
        }
        assertFalse(TermsPreferences(store).hasAcceptedTerms.first())
      } finally {
        job.cancelAndJoin()
      }
    }

  private fun createStore(
    file: File,
    job: Job,
  ) = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job), produceFile = { file })
}
