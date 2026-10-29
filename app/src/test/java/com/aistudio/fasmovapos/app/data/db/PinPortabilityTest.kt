package com.aistudio.fasmovapos.app.data.db

import android.util.Base64
import com.aistudio.fasmovapos.app.data.security.PinHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PinPortabilityTest {
  @Test fun modernPhoneCanVerifyAnAndroidSevenPinAfterRestore() {
    val salt = PinHasher.generateSalt()
    val legacy =
      SecretKeyFactory
        .getInstance("PBKDF2WithHmacSHA1")
        .generateSecret(
          PBEKeySpec("4821".toCharArray(), Base64.decode(salt, Base64.NO_WRAP), 60_000, 256),
        ).encoded
    val hash = Base64.encodeToString(legacy, Base64.NO_WRAP)
    assertTrue(PinHasher.verify("4821", hash, salt))
    assertFalse(PinHasher.verify("4822", hash, salt))
  }
}
