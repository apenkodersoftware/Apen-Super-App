package com.aistudio.fasmovapos.app.data.backup

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.data.repository.FasMovaPOSRepository
import com.aistudio.fasmovapos.app.data.repository.HQ_BRANCH_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupManagerTest {
  private val app = ApplicationProvider.getApplicationContext<Application>()
  private val password = "shop-backup-pw".toCharArray()

  @Before
  fun setUp() = FasMovaPOSDatabase.closeAndReset()

  @After
  fun tearDown() = FasMovaPOSDatabase.closeAndReset()

  private fun repository() = FasMovaPOSRepository(FasMovaPOSDatabase.getDatabase(app))

  private fun usernames() = runBlocking { repository().staffUsers.first() }.map { it.username }

  @Test
  fun restore_bringsBackTheBackedUpData_andDropsLaterChanges() {
    runBlocking { repository().completeSetup("Mama Njeri Shop", "Njeri", "njeri", "4821", false) }
    val backup = BackupManager(app).createBackup(password)
    runBlocking { repository().addStaffUser("amina", "Amina Ali", "CASHIER", "5566", HQ_BRANCH_ID) }
    assertEquals(setOf("njeri", "amina"), usernames().toSet())

    BackupManager(app).restore(backup, password)

    assertEquals(listOf("njeri"), usernames())
    assertNotNull(runBlocking { repository().signIn("njeri", "4821") })
    assertNull(runBlocking { repository().signIn("amina", "5566") })
  }

  @Test
  fun wrongPassword_changesNothing() {
    runBlocking { repository().completeSetup("Shop", "Njeri", "njeri", "4821", false) }
    val backup = BackupManager(app).createBackup(password)
    runBlocking { repository().addStaffUser("amina", "Amina Ali", "CASHIER", "5566", HQ_BRANCH_ID) }

    assertThrows(BackupFormatException::class.java) { BackupManager(app).restore(backup, "wrong".toCharArray()) }

    assertEquals(setOf("njeri", "amina"), usernames().toSet())
  }

  @Test
  fun aFileThatIsNotADatabase_isRefused_andNothingChanges() {
    runBlocking { repository().completeSetup("Shop", "Njeri", "njeri", "4821", false) }
    val notADatabase = BackupCrypto.encrypt("this is just text, not sqlite".toByteArray(), password)

    assertThrows(BackupFormatException::class.java) { BackupManager(app).restore(notADatabase, password) }

    assertEquals(listOf("njeri"), usernames())
  }
}
