package com.aistudio.fasmovapos.app.superapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.fasmovapos.app.data.db.FasMovaPOSDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI snapshots are business-scoped and are read in one database transaction. */
data class BusinessData(
  val profile: BusinessProfile,
  val parties: List<Party>,
  val milk: List<MilkDelivery>,
  val properties: List<PropertyRow>,
  val units: List<RentalUnitRow>,
  val tenancies: List<TenancyRow>,
  val rents: List<RentCharge>,
  val items: List<CommerceItem>,
  val sales: List<CommerceSale>,
  val lines: List<CommerceLine>,
  val charges: List<ChargeRow>,
  val receipts: List<ReceiptRow>,
  val allocations: List<AllocationRow>,
  val deductions: List<DeductionRow>,
  val expenses: List<ExpenseRow>,
  val ledger: List<LedgerRow>,
  val history: List<HistoryRow>,
  val money: List<MoneyInRow>,
  val today: SuperSnapshot,
  val month: SuperSnapshot,
  val capturedAt: Long,
) {
  fun partyBalance(id: String): Long = ledger.filter { !it.voided && it.partyId == id }.sumOf { it.owedDeltaCents }

  fun chargeBalance(id: String): Long =
    charges.firstOrNull { it.id == id && !it.voided }?.let { c ->
      c.amountCents -
        allocations.filter { !it.voided && it.chargeId == id }.sumOf { it.amountCents }
    }
      ?: 0L

  fun tenancy(
    unitId: String,
    month: String,
  ): TenancyRow? {
    val today = SuperDates.today()
    val asOf =
      when {
        month == today.take(7) -> today
        month < today.take(7) -> SuperDates.addDays(SuperDates.addMonths(month, 1) + "-01", -1)
        else -> month + "-01"
      }
    return tenancies.lastOrNull {
      !it.voided && it.unitId == unitId && it.moveIn <= asOf && (it.moveOut == null || it.moveOut > asOf)
    }
  }
}

data class SuperUiState(
  val businesses: List<BusinessProfile> = emptyList(),
  val data: BusinessData? = null,
  val loading: Boolean = true,
  val working: Boolean = false,
  val error: String? = null,
)

class SuperViewModel(
  application: Application,
) : AndroidViewModel(application) {
  private val db = FasMovaPOSDatabase.getDatabase(application)
  val repo = SuperRepository(db)
  val actions = SuperActions(::act, ::clearError, ::choose, repo::periodReading, repo.store::sourceRecord, repo::businessStatement)
  private val prefs = application.getSharedPreferences("apen_super", Application.MODE_PRIVATE)
  private var chosen = prefs.getString("business", null)
  private val _state = MutableStateFlow(SuperUiState())
  val state = _state.asStateFlow()

  init {
    viewModelScope.launch {
      repo.dao.observeBusinesses().collect { profiles ->
        _state.value = _state.value.copy(businesses = profiles)
        val id = chosen?.takeIf { key -> profiles.any { it.id == key } } ?: profiles.firstOrNull()?.id
        if (id != null) choose(id) else _state.value = _state.value.copy(loading = false, data = null)
      }
    }
  }

  fun choose(id: String) {
    if (_state.value.working)return
    chosen = id
    prefs.edit().putString("business", id).apply()
    _state.value = _state.value.copy(data = null, loading = true, error = null)
    refresh()
  }

  fun clearError() {
    _state.value = _state.value.copy(error = null)
  }

  fun refresh() {
    val id = chosen ?: return
    viewModelScope.launch {
      try {
        val data = withContext(Dispatchers.IO) { read(id) }
        if (chosen == id)_state.value = _state.value.copy(data = data, loading = false)
      } catch (e: IllegalArgumentException) {
        if (chosen == id)_state.value = _state.value.copy(loading = false, error = e.message)
      }
    }
  }

  private fun read(id: String): BusinessData =
    repo.store.transaction {
      val dao = repo.dao
      val now = repo.store.now()
      val today = SuperDates.today(now)
      val profile = repo.store.business(id)
      if (profile.type == "RENTAL") repo.rentCharges(id, today.take(7))
      BusinessData(
        profile,
        dao.partys(id),
        dao.milkDeliverys(id),
        dao.propertyRows(id),
        dao.rentalUnitRows(id),
        dao.tenancyRows(id),
        dao.rentCharges(id),
        dao.commerceItems(id),
        dao.commerceSales(id),
        dao.commerceLines(id),
        dao.chargeRows(id),
        dao.receiptRows(id),
        dao.allocationRows(id),
        dao.deductionRows(id),
        dao.expenseRows(id),
        dao.ledgerRows(id),
        dao.historyRows(id),
        dao.moneyInRows(id),
        repo.snapshot(id, today, today),
        repo.snapshot(
          id,
          today.take(7) + "-01",
          today,
        ),
        now,
      )
    }

  fun act(
    onSuccess: () -> Unit = {},
    work: (SuperRepository) -> Unit,
  ) {
    if (_state.value.working)return
    _state.value = _state.value.copy(working = true, error = null)
    val id = chosen
    viewModelScope.launch {
      try {
        val data =
          withContext(Dispatchers.IO) {
            work(repo)
            id?.let { read(it) }
          }
        if (chosen == id)_state.value = _state.value.copy(data = data ?: _state.value.data, working = false, error = null)
        onSuccess()
      } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
      } catch (e: ArithmeticException) {
        failedAction(id, e, "AMOUNT")
      } catch (e: NumberFormatException) {
        failedAction(id, e, "AMOUNT")
      } catch (e: IllegalArgumentException) {
        failedAction(id, e, e.message)
      } catch (e: android.database.sqlite.SQLiteException) {
        failedAction(id, e, "ERROR")
      } catch (e: java.io.IOException) {
        failedAction(id, e, "ERROR")
      } catch (e: IllegalStateException) {
        failedAction(id, e, "ERROR")
      }
    }
  }

  private fun failedAction(
    id: String?,
    cause: Exception,
    key: String?,
  ) {
    android.util.Log.w("ApenRecords", "Could not save a business record", cause)
    if (chosen == id) _state.value = _state.value.copy(working = false, error = key ?: "ERROR")
  }
}
