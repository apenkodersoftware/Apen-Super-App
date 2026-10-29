package com.aistudio.fasmovapos.app.ui

import android.app.Application
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.fasmovapos.app.data.backup.BackupFormatException
import com.aistudio.fasmovapos.app.data.backup.BackupManager
import com.aistudio.fasmovapos.app.data.backup.SalesCsv
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** What the Backup screen tells the user after an action finishes. */
enum class BackupResult {
  BACKUP_SAVED,
  SALES_EXPORTED,
  RESTORED,
  WRONG_PASSWORD_OR_INVALID,
  FAILED,
}

/** Backup, restore and spreadsheet export of the shop's data. Kept apart from the main ViewModel on purpose. */
class BackupViewModel(
  application: Application,
) : AndroidViewModel(application) {
  private val manager = BackupManager(application)
  private val prefs = application.getSharedPreferences(PREFS, Application.MODE_PRIVATE)
  private val flight = SingleFlight()

  val isWorking: StateFlow<Boolean> = flight.isRunning

  private val _result = MutableStateFlow<BackupResult?>(null)
  val result: StateFlow<BackupResult?> = _result.asStateFlow()

  private val _lastBackupAt = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0 })
  val lastBackupAt: StateFlow<Long?> = _lastBackupAt.asStateFlow()

  fun consumeResult() {
    _result.value = null
  }

  /** Writes an encrypted backup of everything to [uri], chosen by the user. */
  fun createBackup(
    uri: Uri,
    password: CharArray,
  ) = execute(BackupResult.BACKUP_SAVED) {
    val bytes = manager.createBackup(password)
    write(uri, bytes)
    val now = System.currentTimeMillis()
    prefs.edit { putLong(KEY_LAST_BACKUP, now) }
    _lastBackupAt.value = now
  }

  /** Replaces all data on this phone with the backup at [uri]. The screen restarts the app on [BackupResult.RESTORED]. */
  fun restoreBackup(
    uri: Uri,
    password: CharArray,
  ) = execute(BackupResult.RESTORED) {
    manager.restore(read(uri), password)
  }

  fun exportSalesCsv(uri: Uri) =
    execute(BackupResult.SALES_EXPORTED) {
      val sales =
        FasMovaPOSDatabase
          .getDatabase(getApplication())
          .saleDao()
          .getAllSales()
          .first()
      write(uri, SalesCsv.build(sales).toByteArray(Charsets.UTF_8))
    }

  private fun execute(
    success: BackupResult,
    block: suspend () -> Unit,
  ) {
    viewModelScope.launch {
      flight.runIfIdle {
        _result.value = null
        _result.value =
          runCatching { withContext(Dispatchers.IO) { block() } }.fold(
            onSuccess = { success },
            onFailure = { error ->
              when (error) {
                is CancellationException -> throw error
                is BackupFormatException -> BackupResult.WRONG_PASSWORD_OR_INVALID
                else -> BackupResult.FAILED // unreadable or unwritable file, revoked access, full storage
              }
            },
          )
      }
    }
  }

  private fun write(
    uri: Uri,
    bytes: ByteArray,
  ) {
    val out = getApplication<Application>().contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")
    out.use { it.write(bytes) }
  }

  private fun read(uri: Uri): ByteArray {
    val input = getApplication<Application>().contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
    return input.use { it.readBytes() }
  }

  private companion object {
    const val PREFS = "fasmovapos_backup"
    const val KEY_LAST_BACKUP = "last_backup_at"
  }
}
