package com.aistudio.fasmovapos.app.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Auth boundary for the app. This interface is the swap point for real Firebase Auth -
 * see AuthRepositoryProvider.kt for exactly what changes when that happens.
 *
 * Today the only implementation is [LocalAuthRepository], which checks the locally-stored,
 * PBKDF2-hashed staff PIN (see com.aistudio.fasmovapos.app.data.security.PinHasher) and persists nothing
 * more sensitive than a local user-id marker (see [AuthSessionStore]). No network call, no
 * real credential, no Firebase project required yet - that's the deliberate "closer, not
 * reaching" boundary for this pass.
 */
interface AuthRepository {
  val authState: StateFlow<AuthState>

  /** Attempts sign-in with [username] + [pin]; updates [authState] on success. */
  suspend fun signIn(
    username: String,
    pin: String,
  ): Result<Unit>

  /** Clears the local session and updates [authState] to [AuthState.LoggedOut]. */
  suspend fun signOut()

  /** Called once at app start to restore a previous session, if any, into [authState]. */
  suspend fun restoreSession()
}
