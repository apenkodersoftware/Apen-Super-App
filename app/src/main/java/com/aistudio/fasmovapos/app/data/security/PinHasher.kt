package com.aistudio.fasmovapos.app.data.security

import android.os.Build
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 hashing for the local staff-cashier PIN.
 *
 * WHY THIS EXISTS: the original AI Studio scaffold stored [StaffUserEntity.pin] and
 * queried it in plaintext (`SELECT * FROM staff_users WHERE pin = :pin`). That is a
 * "reject before it reaches a real device" finding under NAMOD engineering-standards.md
 * §9/§3 (mobile secrets). This gives every staff PIN a unique random salt and a
 * one-way hash, so the raw PIN is never persisted or queryable.
 *
 * SCOPE NOTE: a 4-digit PIN has only 10,000 possible values, so this is defense against
 * a casual DB dump / lost-device scenario (the realistic threat for an offline POS
 * quick-switch PIN), not a substitute for a real credential. Iteration counts below are
 * deliberately modest (not the ~300k+ recommended for a real password hash) so cashier
 * PIN entry at checkout still feels instant on a mid/low-range device. If this identifier
 * is ever promoted to a real password/credential, re-tune iteration counts accordingly.
 *
 * API NOTE: PBKDF2WithHmacSHA256 requires API 26+; this project's minSdk is 24, so we
 * fall back to PBKDF2WithHmacSHA1 (available since API 1) below that, with a higher
 * iteration count to compensate for the weaker underlying PRF.
 */
object PinHasher {
  private const val SALT_BYTES = 16
  private const val KEY_LENGTH_BITS = 256
  private const val ITERATIONS_SHA256 = 20_000
  private const val ITERATIONS_SHA1_FALLBACK = 60_000

  private val secureRandom = SecureRandom()

  /** Generates a new random salt, Base64-encoded for storage as a Room TEXT column. */
  fun generateSalt(): String {
    val salt = ByteArray(SALT_BYTES)
    secureRandom.nextBytes(salt)
    return Base64.encodeToString(salt, Base64.NO_WRAP)
  }

  /** Hashes [pin] with [saltBase64], returning a Base64-encoded digest for storage. */
  fun hash(
    pin: String,
    saltBase64: String,
  ): String = hashWithAlgorithm(pin, saltBase64, algorithmForCurrentApi())

  private fun hashWithAlgorithm(
    pin: String,
    saltBase64: String,
    algorithm: String,
  ): String {
    val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
    val iterations = if (algorithm == ALGORITHM_SHA256) ITERATIONS_SHA256 else ITERATIONS_SHA1_FALLBACK
    val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
    val digest = SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
    return Base64.encodeToString(digest, Base64.NO_WRAP)
  }

  /** Constant-time comparison of a freshly entered [pin] against the stored hash/salt pair. */
  fun verify(
    pin: String,
    storedHashBase64: String,
    storedSaltBase64: String,
  ): Boolean {
    // R1 stored untagged hashes. A newer phone must also verify Android 7's SHA1 format after restore.
    return listOf(ALGORITHM_SHA256, ALGORITHM_SHA1).any { algorithm ->
      runCatching { hashWithAlgorithm(pin, storedSaltBase64, algorithm) }
        .getOrNull()
        ?.let { constantTimeEquals(it, storedHashBase64) } == true
    }
  }

  private fun constantTimeEquals(
    a: String,
    b: String,
  ): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
      result = result or (a[i].code xor b[i].code)
    }
    return result == 0
  }

  private fun algorithmForCurrentApi(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ALGORITHM_SHA256 else ALGORITHM_SHA1

  private const val ALGORITHM_SHA256 = "PBKDF2WithHmacSHA256"
  private const val ALGORITHM_SHA1 = "PBKDF2WithHmacSHA1"
}
