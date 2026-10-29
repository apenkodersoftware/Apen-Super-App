package com.aistudio.fasmovapos.app.auth

import com.aistudio.fasmovapos.app.data.db.entities.StaffUserEntity

/**
 * App-level authentication state, gating access to the main POS shell in MainActivity.
 *
 * This is separate from [com.aistudio.fasmovapos.app.data.db.entities.StaffUserEntity]-based "quick cashier
 * switch" (see FasMovaPOSTopBar's PIN dialog) - that flow assumes an [AuthState.LoggedIn]
 * session already exists and just changes who's acting as the current cashier within it.
 */
sealed interface AuthState {
  /** Initial state while a previous session is being restored from local storage. */
  data object Loading : AuthState

  /** No active session - MainActivity should show LoginScreen. */
  data object LoggedOut : AuthState

  /** An authenticated session for [user] - MainActivity should show the POS shell. */
  data class LoggedIn(
    val user: StaffUserEntity,
  ) : AuthState
}
