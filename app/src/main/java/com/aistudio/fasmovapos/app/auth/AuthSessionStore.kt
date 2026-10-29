package com.aistudio.fasmovapos.app.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.authDataStore by preferencesDataStore(name = "fasmovapos_auth_session")

/**
 * Persists ONLY a local reference to which staff account is currently signed in - never a
 * password, PIN, or auth token. That is a deliberate boundary: DataStore Preferences is not
 * encrypted at rest (androidx.security:security-crypto/EncryptedSharedPreferences was
 * evaluated and skipped - Google deprecated it in 2025 in favour of not caching raw secrets
 * client-side at all). A signed-in user-id is not itself sensitive; if a real credential or
 * token needs local persistence, that should go through Android Keystore-backed storage, not
 * this class - and once real Firebase Auth is wired in, FirebaseAuth's own SDK manages ID
 * token persistence internally, so this store may not need to hold anything beyond what it
 * already does.
 */
class AuthSessionStore(
  private val context: Context,
) {
  private val loggedInUserIdKey = stringPreferencesKey("logged_in_user_id")

  val loggedInUserId: Flow<String?> =
    context.authDataStore.data
      .catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
      }.map { prefs -> prefs[loggedInUserIdKey] }

  suspend fun currentUserId(): String? = loggedInUserId.first()

  suspend fun setLoggedInUserId(userId: String) {
    context.authDataStore.edit { prefs -> prefs[loggedInUserIdKey] = userId }
  }

  suspend fun clear() {
    context.authDataStore.edit { prefs -> prefs.remove(loggedInUserIdKey) }
  }
}
