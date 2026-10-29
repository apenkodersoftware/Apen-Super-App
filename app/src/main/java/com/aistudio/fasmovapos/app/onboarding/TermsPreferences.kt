package com.aistudio.fasmovapos.app.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.onboardingDataStore by preferencesDataStore(name = "fasmovapos_onboarding")

/** The current Apen review document must be explicitly accepted, including after an older acceptance. */
class TermsPreferences internal constructor(
  private val store: DataStore<Preferences>,
) {
  constructor(context: Context) : this(context.onboardingDataStore)

  private val acceptedKey = booleanPreferencesKey("has_accepted_terms")
  private val revisionKey = intPreferencesKey("accepted_fasmovalite_terms_revision")

  val hasAcceptedTerms: Flow<Boolean> =
    store.data
      .catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
      }.map { prefs -> prefs[acceptedKey] == true && prefs[revisionKey] == CURRENT_REVISION }

  suspend fun acceptTerms() {
    store.edit { prefs ->
      prefs[acceptedKey] = true
      prefs[revisionKey] = CURRENT_REVISION
    }
  }

  companion object {
    const val CURRENT_REVISION = 2026102903
  }
}
