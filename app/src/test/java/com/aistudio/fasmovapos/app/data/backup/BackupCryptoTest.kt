package com.aistudio.fasmovapos.app.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {
  private val data = "sales, customers and staff".toByteArray()

  @Test
  fun roundTrip_returnsTheOriginalBytes() {
    val blob = BackupCrypto.encrypt(data, "correct horse".toCharArray())

    assertArrayEquals(data, BackupCrypto.decrypt(blob, "correct horse".toCharArray()))
  }

  @Test
  fun theFileDoesNotContainThePlainData() {
    val blob = BackupCrypto.encrypt(data, "pw-1234".toCharArray())

    assertFalse(String(blob, Charsets.ISO_8859_1).contains("customers"))
  }

  @Test
  fun wrongPassword_isRejected() {
    val blob = BackupCrypto.encrypt(data, "right".toCharArray())

    assertThrows(BackupFormatException::class.java) { BackupCrypto.decrypt(blob, "wrong".toCharArray()) }
  }

  @Test
  fun aFlippedByte_isDetected() {
    val blob = BackupCrypto.encrypt(data, "pw".toCharArray())
    blob[blob.size - 3] = (blob[blob.size - 3].toInt() xor 1).toByte()

    assertThrows(BackupFormatException::class.java) { BackupCrypto.decrypt(blob, "pw".toCharArray()) }
  }

  @Test
  fun notABackup_andTruncatedFiles_areRejectedCleanly() {
    assertThrows(BackupFormatException::class.java) { BackupCrypto.decrypt("hello world, not a backup".toByteArray(), "pw".toCharArray()) }
    assertThrows(BackupFormatException::class.java) { BackupCrypto.decrypt(byteArrayOf(1, 2), "pw".toCharArray()) }
    val blob = BackupCrypto.encrypt(data, "pw".toCharArray())
    assertThrows(BackupFormatException::class.java) { BackupCrypto.decrypt(blob.copyOf(20), "pw".toCharArray()) }
  }

  @Test
  fun legacyKeyDerivation_alsoRoundTrips() {
    val blob = BackupCrypto.encrypt(data, "pw".toCharArray(), sha256Available = false)

    assertArrayEquals(data, BackupCrypto.decrypt(blob, "pw".toCharArray()))
  }
}
