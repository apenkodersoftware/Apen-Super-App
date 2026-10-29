package com.aistudio.fasmovapos.app.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Build
import com.aistudio.fasmovapos.app.auth.AuthSessionStore
import com.aistudio.fasmovapos.app.data.companion.LoanPlansStore
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.data.records.RecordingMeta
import com.aistudio.fasmovapos.app.data.security.PinHasher
import com.aistudio.fasmovapos.app.data.security.PinPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Creates and restores whole-database backups. A backup is the SQLite file itself, encrypted by [BackupCrypto],
 * so it carries every sale, product, customer and staff record exactly as they are on this phone.
 */
data class OwnerRecovery(
  val username: String,
  val newPin: String,
)

class BackupManager(
  private val context: Context,
) {
  private val databaseFile: File get() = context.getDatabasePath(FasMovaPOSDatabase.DATABASE_NAME)

  /** Returns the encrypted backup bytes for the live database. */
  fun createBackup(password: CharArray): ByteArray {
    runBlocking(Dispatchers.IO) { LoanPlansStore(context).importLegacy() }
    // Fold the write-ahead log into the main file so the copy below is complete.
    FasMovaPOSDatabase
      .getDatabase(context)
      .openHelper.writableDatabase
      .query("PRAGMA wal_checkpoint(TRUNCATE)")
      .use {
        if (!it.moveToFirst() || it.getInt(0) != 0) throw java.io.IOException("Close active writes and retry the backup")
      }
    return BackupCrypto.encrypt(databaseFile.readBytes(), password, sha256Available = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
  }

  /**
   * Replaces the live database with the one inside [backup]. Throws [BackupFormatException] (and changes nothing) if
   * the password is wrong or the file is damaged, from a newer app, or not a FasMova database. The caller must
   * restart the app afterwards: every open repository still points at the old database.
   */
  fun restore(
    backup: ByteArray,
    password: CharArray,
    recovery: OwnerRecovery? = null,
  ) {
    val candidate = File(context.cacheDir, "restore-candidate.db")
    try {
      candidate.writeBytes(BackupCrypto.decrypt(backup, password))
      verifyDatabase(candidate)
      recovery?.let { resetCandidateOwner(candidate, it) }
      swapIn(candidate)
    } finally {
      candidate.delete()
    }
  }

  /** The backup password has already authenticated/decrypted this candidate; live records are unchanged. */
  private fun resetCandidateOwner(
    file: File,
    recovery: OwnerRecovery,
  ) {
    require(PinPolicy.formatProblem(recovery.newPin) == null) { "Choose a valid new PIN" }
    SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { candidate ->
      val ownerId =
        candidate
          .rawQuery(
            "SELECT id FROM staff_users WHERE username=? AND role='OWNER' AND isActive=1",
            arrayOf(recovery.username.trim()),
          ).use { if (it.moveToFirst()) it.getString(0) else null } ?: reject("The backup has no matching owner")
      candidate.rawQuery("SELECT pinHash,pinSalt FROM staff_users WHERE id!=? AND isActive=1", arrayOf(ownerId)).use { rows ->
        while (rows.moveToNext()) {
          if (PinHasher.verify(recovery.newPin, rows.getString(0), rows.getString(1))) reject("Choose a different PIN")
        }
      }
      val salt = PinHasher.generateSalt()
      val flag = if (candidate.version >= 3) ", mustChangePin=0" else ""
      candidate.execSQL(
        "UPDATE staff_users SET pinHash=?,pinSalt=?$flag WHERE id=?",
        arrayOf(PinHasher.hash(recovery.newPin, salt), salt, ownerId),
      )
    }
  }

  /** Any SQLite error here (not a database, corrupt pages) means the file is not a usable backup. */
  private fun verifyDatabase(file: File) {
    try {
      SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { checkContents(it) }
    } catch (e: SQLiteException) {
      throw BackupFormatException("The backup does not contain a valid database", e)
    }
  }

  private fun checkContents(db: SQLiteDatabase) {
    if (db.version > FasMovaPOSDatabase.VERSION) reject("This backup was made by a newer version of the app")
    if (db.version < 1) reject("The backup does not contain a FasMova database")
    val integrity = db.rawQuery("PRAGMA integrity_check", null).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else "" }
    if (integrity != "ok") reject("The backup is damaged")
    val hasStaff =
      db.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'staff_users'", null).use { cursor ->
        cursor.moveToFirst()
      }
    if (!hasStaff) reject("The backup does not contain a FasMova database")
  }

  private fun reject(message: String): Nothing = throw BackupFormatException(message)

  /** Swaps [candidate] in, keeping the old database aside so a failed copy can be undone. */
  private fun swapIn(candidate: File) {
    FasMovaPOSDatabase
      .getDatabase(context)
      .openHelper.writableDatabase
      .query("PRAGMA wal_checkpoint(TRUNCATE)")
      .use {
        if (!it.moveToFirst() || it.getInt(0) != 0) throw java.io.IOException("Close active writes and retry the backup")
      }
    FasMovaPOSDatabase.closeAndReset()
    val live = databaseFile
    val aside = File(live.path + ".before-restore")
    val sidecars = listOf(File(live.path + "-wal"), File(live.path + "-shm"))
    val hadLive = live.exists()
    if (hadLive) live.copyTo(aside, overwrite = true)
    runCatching {
      sidecars.forEach { it.delete() }
      candidate.copyTo(live, overwrite = true)
      // Old R1 backups had no notes. Do not import stale notes from this device after a restore.
      runBlocking(Dispatchers.IO) {
        FasMovaPOSDatabase.getDatabase(context).recordingDao().putMeta(RecordingMeta(LoanPlansStore.MIGRATED, "restored"))
        AuthSessionStore(context).clear()
      }
    }.getOrElse { error ->
      FasMovaPOSDatabase.closeAndReset()
      sidecars.forEach { it.delete() }
      if (hadLive) aside.copyTo(live, overwrite = true) else live.delete()
      throw error
    }
  }
}
