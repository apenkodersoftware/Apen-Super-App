package com.aistudio.fasmovapos.app.data.repository

import com.aistudio.fasmovapos.app.data.db.entities.CustomerEntity
import com.aistudio.fasmovapos.app.data.db.entities.ExpenseEntity
import com.aistudio.fasmovapos.app.data.db.entities.ProductEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleEntity
import com.aistudio.fasmovapos.app.data.db.entities.SaleItemEntity
import com.aistudio.fasmovapos.app.data.db.entities.SupplierEntity

/** Fictional training records loaded only when the owner opts in during setup. */
internal object SampleData {
  fun products(): List<ProductEntity> =
    liteProducts() +
      listOf(
        product("p-1", "BASIN", "Plastic basin", "Household", 180.0, 250.0, 20),
        product("p-2", "BUCKET", "Bucket with lid", "Household", 250.0, 350.0, 12),
        product("p-3", "BOWL", "Stainless steel bowl", "Kitchenware", 80.0, 120.0, 24),
        product("p-4", "MUG", "Ceramic mug", "Kitchenware", 90.0, 150.0, 18),
        product("p-5", "PLATE", "Dinner plate", "Kitchenware", 110.0, 180.0, 30),
        product("p-6", "BROOM", "Household broom", "Cleaning", 80.0, 120.0, 3),
        product("p-7", "SOAP", "Laundry soap bar", "Cleaning", 50.0, 70.0, 36),
        product("p-8", "PEGS", "Clothes pegs packet", "Household", 40.0, 60.0, 15),
        product("p-9", "BOOK", "Exercise book", "Stationery", 35.0, 50.0, 40),
        product("p-10", "BULB", "LED bulb", "Electrical", 100.0, 160.0, 4),
      )

  private fun liteProducts(): List<ProductEntity> =
    listOf(
      retailProduct("lite-sugar", "Loose sugar", "Food", "Kg", 120.0, 160.0, 15000),
      retailProduct("lite-rice", "Loose rice", "Food", "Kg", 130.0, 180.0, 25000),
      retailProduct("lite-beans", "Loose beans", "Food", "Kg", 100.0, 160.0, 10000),
      retailProduct("lite-oil", "Loose liquid cooking oil", "Food", "Ltr", 180.0, 240.0, 20000),
      retailProduct("lite-fat", "Solid cooking fat", "Food", "Kg", 160.0, 220.0, 10000),
      retailProduct("lite-milk", "Fresh milk packet", "Drinks", "Pcs", 50.0, 60.0, 20),
      retailProduct("lite-bread", "Bread loaf", "Food", "Pcs", 50.0, 65.0, 12),
      retailProduct("lite-flour", "Maize flour 2 kg packet", "Food", "Pcs", 120.0, 150.0, 20),
      retailProduct("lite-salt", "Salt 500 g packet", "Food", "Pcs", 25.0, 35.0, 30),
      retailProduct("lite-tea", "Tea leaves 100 g packet", "Food", "Pcs", 60.0, 80.0, 15),
      retailProduct("lite-water", "Water 500 ml bottle", "Drinks", "Pcs", 20.0, 30.0, 36),
      retailProduct("lite-soda", "Bottled soda", "Drinks", "Pcs", 30.0, 40.0, 24),
      retailProduct("lite-tissue", "Tissue roll", "Personal care", "Pcs", 25.0, 35.0, 25),
      retailProduct("lite-matches", "Matchbox", "Household", "Pcs", 5.0, 10.0, 50),
      retailProduct("lite-eggs", "Single egg", "Food", "Pcs", 12.0, 15.0, 60),
    )

  private fun retailProduct(
    id: String,
    name: String,
    category: String,
    unit: String,
    cost: Double,
    price: Double,
    stock: Int,
  ): ProductEntity =
    ProductEntity(
      id,
      id.uppercase(),
      "",
      name,
      category,
      unit,
      cost,
      price,
      stock,
      if (unit == "Pcs") 5 else 2000,
      HQ_BRANCH_ID,
      quantityScale = if (unit == "Pcs") 1 else 1000,
      isFavorite = true,
    )

  private fun product(
    id: String,
    sku: String,
    name: String,
    category: String,
    cost: Double,
    price: Double,
    stock: Int,
  ) = ProductEntity(id, sku, "", name, category, "Pcs", cost, price, stock, 5, HQ_BRANCH_ID)

  fun suppliers(): List<SupplierEntity> = listOf(SupplierEntity("s-1", "Training supplier", "", "", "", ""))

  fun customers(): List<CustomerEntity> = listOf(CustomerEntity("c-1", "Training customer", "", "", creditLimit = 2000.0))

  fun expenses(): List<ExpenseEntity> = emptyList()

  fun firstSale(): Pair<SaleEntity, List<SaleItemEntity>> {
    val saleId = "training-sale-1"
    val items =
      listOf(
        SaleItemEntity(
          saleId = saleId,
          productId = "p-1",
          productName = "Plastic basin",
          quantity = 1,
          unitPrice = 250.0,
          totalPrice = 250.0,
        ),
      )
    val sale =
      SaleEntity(
        id = saleId,
        transactionRef = "TRAINING-001",
        branchId = HQ_BRANCH_ID,
        cashierId = "u-1",
        cashierName = "Training owner",
        totalAmount = 250.0,
        discountAmount = 0.0,
        taxAmount = 0.0,
        netAmount = 250.0,
        paymentMethod = "MPESA",
        paymentReference = "QKJ9821LXM",
        customerId = null,
        customerName = null,
        syncStatus = "LOCAL",
      )
    return sale to items
  }
}
