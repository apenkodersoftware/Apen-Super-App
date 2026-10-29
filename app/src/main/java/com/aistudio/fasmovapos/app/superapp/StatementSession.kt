package com.aistudio.fasmovapos.app.superapp

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aistudio.fasmovapos.app.ui.simple.BusinessStatement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owned by the summary, so resume refreshes and recycled lazy items cannot dismiss or cancel an export. */
@Stable
class StatementSession(
  private val businessId: String,
  private val scope: CoroutineScope,
  private val read: (String, RecordPeriod) -> BusinessStatement,
) {
  var statement by mutableStateOf<BusinessStatement?>(null)
    private set
  var working by mutableStateOf(false)
    private set
  var problem by mutableStateOf(false)
    private set

  fun prepare(period: RecordPeriod) {
    if (working) return
    working = true
    problem = false
    scope.launch {
      try {
        statement = withContext(Dispatchers.IO) { read(businessId, period) }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: android.database.sqlite.SQLiteException) {
        problem = true
      } catch (_: IllegalArgumentException) {
        problem = true
      } catch (_: IllegalStateException) {
        problem = true
      } finally {
        working = false
      }
    }
  }

  fun dismiss() {
    statement = null
  }
}
