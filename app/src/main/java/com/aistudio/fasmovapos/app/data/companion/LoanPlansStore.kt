package com.aistudio.fasmovapos.app.data.companion

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import com.aistudio.fasmovapos.app.data.records.LoanPlanRow
import com.aistudio.fasmovapos.app.data.records.RecordOrigin
import com.aistudio.fasmovapos.app.data.records.RecordingMeta
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.loanPlansStore by preferencesDataStore(name = "apen_personal_loan_plans")

object LoanPlanCodec {
  fun decode(text: String): List<LoanPlan> {
    val rows = JSONArray(text)
    return List(rows.length()) { index ->
      val row = rows.getJSONObject(index)
      LoanPlan(row.getString("id"), row.getString("productId"), row.getLong("amountCents"), row.getString("dueDate"))
    }
  }

  fun encode(plans: List<LoanPlan>): String =
    JSONArray()
      .apply {
        plans.forEach { plan ->
          put(
            JSONObject()
              .put("id", plan.id)
              .put("productId", plan.productId)
              .put("amountCents", plan.amountCents)
              .put("dueDate", plan.dueDate),
          )
        }
      }.toString()
}

class LoanPlansStore(
  context: Context,
) {
  private val legacy = context.applicationContext.loanPlansStore
  private val db = FasMovaPOSDatabase.getDatabase(context)
  private val dao = db.recordingDao()
  private val noteDao = db.loanPlanDao()
  private val key = stringPreferencesKey("personal_plans_v1")
  val plans =
    flow {
      importLegacy()
      emitAll(noteDao.plans().map { rows -> rows.map { LoanPlan(it.id, it.productId, it.amountCents, it.dueDate) } })
    }

  /** Idempotent import. The marker is inside the backed-up database, preventing stale notes after restore. */
  suspend fun importLegacy() {
    if (dao.meta(MIGRATED) != null) return
    val old = LoanPlanCodec.decode(legacy.data.first()[key] ?: "[]")
    old.forEach { require(it.amountCents > 0 && LoanPlanInput.validDate(it.dueDate)) }
    db.withTransaction {
      if (dao.meta(MIGRATED) == null) {
        old.forEach {
          noteDao.importPlan(LoanPlanRow(it.id, it.productId, it.amountCents, it.dueDate))
          dao.saveOrigin(RecordOrigin("personal_loan_plans", it.id, null))
        }
        dao.putMeta(RecordingMeta(MIGRATED, "done"))
      }
    }
  }

  suspend fun save(plan: LoanPlan) {
    require(plan.id.isNotBlank() && plan.amountCents > 0 && LoanPlanInput.validDate(plan.dueDate))
    importLegacy()
    noteDao.savePlan(LoanPlanRow(plan.id, plan.productId, plan.amountCents, plan.dueDate))
  }

  suspend fun remove(id: String) {
    importLegacy()
    noteDao.removePlan(id)
  }

  companion object {
    const val MIGRATED = "personal_plans_imported_v2"
  }
}
