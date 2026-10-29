package com.aistudio.fasmovapos.app.superapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Livelihood { DAIRY, RENTAL, RESTAURANT, RETAIL }

@Entity(tableName = "super_businesses")
data class BusinessProfile(
  @PrimaryKey val id: String,
  val name: String,
  val type: String,
  val demo: Boolean = false,
  val currency: String = "KES",
  val createdAt: Long? = null,
)

@Entity(tableName = "super_parties")
data class Party(
  @PrimaryKey val id: String,
  val businessId: String,
  val name: String,
  val kind: String,
  val phone: String = "",
  val priceCents: Long = 0,
  val cycle: String = "MONTHLY",
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_milk")
data class MilkDelivery(
  @PrimaryKey val id: String,
  val businessId: String,
  val partyId: String,
  val session: String,
  val litresMilli: Long,
  val priceCents: Long,
  val earnedCents: Long,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_properties")
data class PropertyRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val name: String,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_units")
data class RentalUnitRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val propertyId: String,
  val label: String,
  val rentCents: Long,
  val dueDay: Int,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_tenancies")
data class TenancyRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val unitId: String,
  val partyId: String,
  val moveIn: String,
  val moveOut: String? = null,
  val depositCents: Long = 0,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_rent_charges")
data class RentCharge(
  @PrimaryKey val id: String,
  val businessId: String,
  val unitId: String,
  val tenancyId: String,
  val partyId: String,
  val month: String,
  val amountCents: Long,
  val dueDate: String,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_items")
data class CommerceItem(
  @PrimaryKey val id: String,
  val businessId: String,
  val name: String,
  val priceCents: Long,
  val category: String,
  val stockMilli: Long? = null,
  val unit: String = "Pcs",
  val costCents: Long = 0,
  val minimumMilli: Long = 0,
  val photo: ByteArray? = null,
  val archived: Boolean = false,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
  @ColumnInfo(defaultValue = "0") val quarterPriceCents: Long = 0,
  @ColumnInfo(defaultValue = "0") val halfPriceCents: Long = 0,
)

@Entity(tableName = "super_sales")
data class CommerceSale(
  @PrimaryKey val id: String,
  val businessId: String,
  val partyId: String?,
  val amountCents: Long,
  val method: String,
  val reference: String = "",
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
  @ColumnInfo(defaultValue = "0") val discountCents: Long = 0,
)

@Entity(tableName = "super_sale_lines")
data class CommerceLine(
  @PrimaryKey val id: String,
  val businessId: String,
  val saleId: String,
  val itemId: String,
  val name: String,
  val quantityMilli: Long,
  val priceCents: Long,
  val amountCents: Long,
  val unit: String = "Pcs",
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_charges")
data class ChargeRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val partyId: String?,
  val sourceTable: String,
  val sourceId: String,
  val amountCents: Long,
  val month: String = "",
  val propertyId: String? = null,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_receipts")
data class ReceiptRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val partyId: String?,
  val amountCents: Long,
  val method: String,
  val reference: String = "",
  val saleId: String? = null,
  val propertyId: String? = null,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_deductions")
data class DeductionRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val receiptId: String,
  val category: String,
  val amountCents: Long,
  val countsAsCost: Boolean,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_allocations")
data class AllocationRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val receiptId: String,
  val chargeId: String,
  val amountCents: Long,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_expenses")
data class ExpenseRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val amountCents: Long,
  val category: String,
  val method: String = "CASH",
  val reference: String = "",
  val note: String = "",
  val deductionId: String? = null,
  val propertyId: String? = null,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_money_in")
data class MoneyInRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val amountCents: Long,
  val kind: String,
  val method: String = "CASH",
  val reference: String = "",
  val note: String = "",
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_ledger")
data class LedgerRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val sourceTable: String,
  val sourceId: String,
  val partyId: String? = null,
  val earnedCents: Long = 0,
  val receivedCents: Long = 0,
  val spentCents: Long = 0,
  val owedDeltaCents: Long = 0,
  val settledCents: Long = 0,
  val heldCents: Long = 0,
  val otherInCents: Long = 0,
  val propertyId: String? = null,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_history")
data class HistoryRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val recordId: String,
  val sourceTable: String,
  val beforeValue: String?,
  val afterValue: String?,
  val reason: String,
  val actor: String = "LOCAL_OWNER",
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

@Entity(tableName = "super_no_trade")
data class NoTradeRow(
  @PrimaryKey val id: String,
  val businessId: String,
  val occurredOn: String = "",
  val recordedAt: Long? = null,
  val schemaVersion: Int = 1,
  val voided: Boolean = false,
)

data class Deduction(
  val category: String,
  val amountCents: Long,
  val countsAsCost: Boolean,
)

data class ChangeHistory(
  val recordId: String,
  val before: String?,
  val after: String?,
  val reason: String,
  val recordedAt: Long,
)

data class Allocation(
  val chargeId: String,
  val month: String,
  val amountCents: Long,
)

data class SuperSnapshot(
  val earnedCents: Long = 0,
  val receivedCents: Long = 0,
  val spentCents: Long = 0,
  val owedCents: Long = 0,
  val litresMilli: Long = 0,
  val heldCents: Long = 0,
  val otherInCents: Long = 0,
  val entryDays: Int = 0,
  val unknownEntryTimes: Int = 0,
  val noTradeDays: Int = 0,
  val changeEvents: Int = 0,
) {
  val salesLessExpensesCents: Long get() = earnedCents - spentCents
}
