package com.aistudio.fasmovapos.app.ui.screens

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.aistudio.fasmovapos.app.ui.BackupResult
import com.aistudio.fasmovapos.app.ui.theme.FasMovaPOSTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupScreenTest {
  @get:Rule val composeRule = createComposeRule()

  private fun show(
    isOwner: Boolean,
    isWorking: Boolean = false,
    lastBackupAt: Long? = null,
    result: BackupResult? = null,
  ) = composeRule.setContent {
    FasMovaPOSTheme {
      BackupScreen(
        isOwner = isOwner,
        isWorking = isWorking,
        lastBackupAt = lastBackupAt,
        result = result,
        onCreateBackup = { _, _ -> },
        onRestore = { _, _ -> },
        onExportSales = {},
      )
    }
  }

  @Test
  fun owner_canUseEveryAction_andIsToldWhenThereIsNoBackupYet() {
    show(isOwner = true)

    composeRule.onNodeWithTag("backup_create_button").assertIsEnabled()
    composeRule.onNodeWithTag("backup_export_button").assertIsEnabled()
    composeRule.onNodeWithTag("backup_restore_button").assertIsEnabled()
    composeRule.onNodeWithText("You have never made a backup").assertExists()
  }

  @Test
  fun anyoneElse_cannotBackUpOrRestore() {
    show(isOwner = false)

    composeRule.onNodeWithTag("backup_owner_only").assertExists()
    composeRule.onNodeWithTag("backup_create_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("backup_restore_button").assertIsNotEnabled()
  }

  @Test
  fun whileWorking_actionsAreDisabled_andAFailureIsExplained() {
    show(isOwner = true, isWorking = true, result = BackupResult.WRONG_PASSWORD_OR_INVALID)

    composeRule.onNodeWithTag("backup_create_button").assertIsNotEnabled()
    composeRule.onNodeWithText("Wrong password, or the file is not a valid backup.").assertExists()
  }

  @Test
  fun backupPassword_needsSixCharactersAndAMatchingRepeat() {
    show(isOwner = true)
    composeRule.onNodeWithTag("backup_create_button").performClick()
    val confirm = composeRule.onNodeWithTag("backup_password_confirm")

    confirm.assertIsNotEnabled()
    composeRule.onNodeWithTag("backup_password_field").performTextInput("abc12")
    composeRule.onNodeWithTag("backup_password_repeat_field").performTextInput("abc12")
    confirm.assertIsNotEnabled() // too short even though they match
    composeRule.onNodeWithTag("backup_password_field").performTextInput("3")
    confirm.assertIsNotEnabled() // long enough now, but the repeat differs
    composeRule.onNodeWithTag("backup_password_repeat_field").performTextInput("3")
    confirm.assertIsEnabled()
  }
}
