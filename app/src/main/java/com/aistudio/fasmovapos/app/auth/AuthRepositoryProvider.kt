package com.aistudio.fasmovapos.app.auth

import android.content.Context
import com.aistudio.fasmovapos.app.data.repository.FasMovaPOSRepository

/**
 * Single construction point for the app's [AuthRepository]. This is the seam: when Firebase
 * Auth is wired in (see the migration notes on [LocalAuthRepository]), only the line below
 * needs to change - nothing else in the app constructs an AuthRepository directly.
 */
object AuthRepositoryProvider {
  fun create(
    context: Context,
    repository: FasMovaPOSRepository,
  ): AuthRepository = LocalAuthRepository(repository, AuthSessionStore(context.applicationContext))
}
