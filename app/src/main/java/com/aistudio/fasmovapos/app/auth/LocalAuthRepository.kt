package com.aistudio.fasmovapos.app.auth

import com.aistudio.fasmovapos.app.data.repository.FasMovaPOSRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real, working local implementation of [AuthRepository]: verifies username + PIN against the
 * Room-stored, PBKDF2-hashed staff record (see FasMovaPOSRepository.signIn /
 * com.aistudio.fasmovapos.app.data.security.PinHasher), and remembers the session via [AuthSessionStore].
 *
 * This is intentionally the ONLY implementation right now. There is no FirebaseAuthRepository
 * stub sitting alongside it - a class with unimplemented sign-in logic would just be dead code
 * that ktlint/detekt would (rightly) flag, and it isn't needed to get the swap-in seam right.
 * When Firebase Auth is wired in:
 *   1. Uncomment the four Firebase Auth / Credential Manager dependencies in app/build.gradle.kts
 *      (implementation(libs.firebase.auth), androidx.credentials, androidx.credentials.play.services, googleid)
 *   2. Add the real google-services.json for this app's Firebase project
 *   3. Write a FirebaseAuthRepository : AuthRepository that calls FirebaseAuth.getInstance()
 *      (phone-number/OTP sign-in is likely the better fit than email for this audience -
 *      see NAMOD architecture.md - since these are shop owners already M-Pesa/phone-centric)
 *   4. Change the single line in AuthRepositoryProvider.create() below to return it instead
 * No other file in the app needs to change - everything else talks to the AuthRepository
 * interface, not this class directly.
 */
class LocalAuthRepository(
  private val repository: FasMovaPOSRepository,
  private val sessionStore: AuthSessionStore,
) : AuthRepository {
  private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
  override val authState: StateFlow<AuthState> = _authState.asStateFlow()

  override suspend fun signIn(
    username: String,
    pin: String,
  ): Result<Unit> {
    val user =
      repository.signIn(username, pin)
        ?: return Result.failure(InvalidCredentialsException())
    sessionStore.setLoggedInUserId(user.id)
    _authState.value = AuthState.LoggedIn(user)
    return Result.success(Unit)
  }

  override suspend fun signOut() {
    sessionStore.clear()
    _authState.value = AuthState.LoggedOut
  }

  override suspend fun restoreSession() {
    val userId = sessionStore.currentUserId()
    val user = userId?.let { repository.getStaffUserById(it) }
    _authState.value = if (user != null) AuthState.LoggedIn(user) else AuthState.LoggedOut
  }
}

class InvalidCredentialsException : Exception("Invalid username or PIN")
