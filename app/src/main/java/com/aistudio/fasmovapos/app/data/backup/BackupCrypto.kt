package com.aistudio.fasmovapos.app.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** The backup could not be opened: wrong password, a damaged file, or not a FasMova backup at all. */
class BackupFormatException(
  message: String,
  cause: Throwable? = null,
) : IOException(message, cause)

/**
 * Password-protects a backup file. A backup holds sales, customers and staff PIN hashes and is meant to travel
 * (Drive, WhatsApp, a memory card), so it is encrypted with AES-256-GCM using a key derived from a password the
 * Owner chooses. GCM also detects any tampering or corruption.
 *
 * File layout: magic "FMBK" | version | kdf id | iterations | salt | iv | ciphertext+tag. Every parameter needed to
 * reopen the file is stored in the header, so key-derivation settings can change in future versions.
 */
object BackupCrypto {
  private val MAGIC = byteArrayOf('F'.code.toByte(), 'L'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
  private const val FORMAT_VERSION = 1
  private const val KDF_SHA256 = 1
  private const val KDF_SHA1 = 2
  private const val ITERATIONS_SHA256 = 210_000
  private const val ITERATIONS_SHA1 = 600_000
  private const val SALT_BYTES = 16
  private const val IV_BYTES = 12
  private const val KEY_BITS = 256
  private const val TAG_BITS = 128
  private const val MAX_ITERATIONS = 5_000_000

  private val random = SecureRandom()

  /** Encrypts [plain]. [sha256Available] is false only on Android 7, where PBKDF2-SHA256 does not exist. */
  fun encrypt(
    plain: ByteArray,
    password: CharArray,
    sha256Available: Boolean = true,
  ): ByteArray {
    val kdf = if (sha256Available) KDF_SHA256 else KDF_SHA1
    val iterations = if (sha256Available) ITERATIONS_SHA256 else ITERATIONS_SHA1
    val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
    val iv = ByteArray(IV_BYTES).also(random::nextBytes)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, iterations, kdf), GCMParameterSpec(TAG_BITS, iv))
    val out = ByteArrayOutputStream()
    DataOutputStream(out).apply {
      write(MAGIC)
      writeByte(FORMAT_VERSION)
      writeByte(kdf)
      writeInt(iterations)
      write(salt)
      write(iv)
      write(cipher.doFinal(plain))
    }
    return out.toByteArray()
  }

  /** Decrypts a file made by [encrypt]; throws [BackupFormatException] if the password is wrong or the file is damaged. */
  fun decrypt(
    blob: ByteArray,
    password: CharArray,
  ): ByteArray {
    val input = DataInputStream(ByteArrayInputStream(blob))
    try {
      val magic = ByteArray(MAGIC.size).also(input::readFully)
      if (!magic.contentEquals(MAGIC)) reject("Not a FasMova backup file")
      if (input.readUnsignedByte() != FORMAT_VERSION) reject("Backup was made by a newer app version")
      val kdf = input.readUnsignedByte()
      val iterations = input.readInt()
      if ((kdf != KDF_SHA256 && kdf != KDF_SHA1) || iterations !in 1..MAX_ITERATIONS) reject("Unknown backup format")
      val salt = ByteArray(SALT_BYTES).also(input::readFully)
      val iv = ByteArray(IV_BYTES).also(input::readFully)
      val encrypted = input.readBytes()
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations, kdf), GCMParameterSpec(TAG_BITS, iv))
      return cipher.doFinal(encrypted)
    } catch (e: GeneralSecurityException) {
      throw BackupFormatException("Wrong password, or the backup file is damaged", e)
    } catch (e: java.io.EOFException) {
      throw BackupFormatException("The backup file is incomplete", e)
    }
  }

  private fun reject(message: String): Nothing = throw BackupFormatException(message)

  private fun deriveKey(
    password: CharArray,
    salt: ByteArray,
    iterations: Int,
    kdf: Int,
  ): SecretKeySpec {
    val algorithm = if (kdf == KDF_SHA256) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"
    val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
    return SecretKeySpec(SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded, "AES")
  }
}
