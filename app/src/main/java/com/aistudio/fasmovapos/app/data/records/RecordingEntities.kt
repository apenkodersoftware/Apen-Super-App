package com.aistudio.fasmovapos.app.data.records

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Local history, not independently authenticated evidence. Snapshots retain original field values. */
@Entity(tableName = "recording_events")
data class RecordingEvent(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val entityTable: String,
  val recordId: String,
  val action: String,
  val occurredAt: Long?,
  val recordedAt: Long,
  val beforeValue: String?,
  val afterValue: String?,
  val reason: String,
  val actor: String,
)

@Entity(tableName = "personal_loan_plans")
data class LoanPlanRow(
  @PrimaryKey val id: String,
  val productId: String,
  val amountCents: Long,
  val dueDate: String,
)

@Entity(tableName = "recording_meta")
data class RecordingMeta(
  @PrimaryKey val id: String,
  val value: String,
)

@Entity(tableName = "recorded_days")
data class RecordedDay(
  @PrimaryKey val date: String,
  val closed: Boolean,
)

@Entity(tableName = "record_origins", primaryKeys = ["entityTable", "recordId"])
data class RecordOrigin(
  val entityTable: String,
  val recordId: String,
  val createdAt: Long?,
)

@Dao
interface RecordingDao {
  @Query("SELECT * FROM record_origins")
  fun origins(): Flow<List<RecordOrigin>>

  @Query("SELECT * FROM recording_events ORDER BY id DESC")
  fun events(): Flow<List<RecordingEvent>>

  @Query("DELETE FROM recording_meta WHERE id = :id")
  suspend fun removeMeta(id: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveOrigin(row: RecordOrigin)

  @Query("SELECT value FROM recording_meta WHERE id = :id")
  suspend fun meta(id: String): String?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun putMeta(row: RecordingMeta)

  @Query("SELECT * FROM recorded_days ORDER BY date")
  fun days(): Flow<List<RecordedDay>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveDay(row: RecordedDay)
}

@Dao
interface LoanPlanDao {
  @Query("SELECT * FROM personal_loan_plans ORDER BY dueDate, id")
  fun plans(): Flow<List<LoanPlanRow>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun savePlan(row: LoanPlanRow)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun importPlan(row: LoanPlanRow)

  @Query("DELETE FROM personal_loan_plans WHERE id = :id")
  suspend fun removePlan(id: String)
}
