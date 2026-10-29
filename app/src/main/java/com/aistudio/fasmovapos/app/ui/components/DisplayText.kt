package com.aistudio.fasmovapos.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aistudio.fasmovapos.app.R
import com.aistudio.fasmovapos.app.ui.language.categoryLabel

/** Staff role as stored ("OWNER") shown in the user's language. Unknown roles show as stored. */
@Composable
fun roleLabel(role: String): String =
  when (role) {
    "OWNER" -> stringResource(R.string.role_owner)
    "MANAGER" -> stringResource(R.string.role_manager)
    "CASHIER" -> stringResource(R.string.role_cashier)
    else -> role
  }

/** Sync status as stored ("SYNCED") shown in the user's language. */
@Composable
fun syncStatusLabel(status: String): String =
  when (status) {
    "SYNCED" -> stringResource(R.string.sync_status_synced)
    "PENDING" -> stringResource(R.string.sync_status_pending)
    "FAILED" -> stringResource(R.string.sync_status_failed)
    "LOCAL" -> stringResource(R.string.sync_status_local)
    else -> status
  }

/** Product filter chips keep English keys ("All", "Low Stock", category names); labels are translated without changing the filter keys. */
@Composable
fun filterChipLabel(key: String): String =
  when (key) {
    "All" -> stringResource(R.string.filter_all)
    "Low Stock" -> stringResource(R.string.low_stock)
    else -> categoryLabel(key)
  }

/** Resource for a role's display name; unknown roles fall back to the cashier label. */
fun roleLabelRes(role: String): Int =
  when (role) {
    "OWNER" -> R.string.role_owner
    "MANAGER" -> R.string.role_manager
    else -> R.string.role_cashier
  }
