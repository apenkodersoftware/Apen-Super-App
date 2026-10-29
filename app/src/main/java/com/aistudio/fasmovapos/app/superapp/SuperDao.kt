package com.aistudio.fasmovapos.app.superapp

import androidx.room.Dao

@Dao
interface SuperDao :
  SuperBusinessAccess,
  SuperRentalPropertyAccess,
  SuperRentalChargeAccess,
  SuperCommerceAccess,
  SuperAccountsAccess,
  SuperSettlementAccess,
  SuperLedgerAccess,
  SuperHistoryAccess
