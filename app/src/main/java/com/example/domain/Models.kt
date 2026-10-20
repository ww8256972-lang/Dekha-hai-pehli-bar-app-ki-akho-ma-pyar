package com.example.domain

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SaleItem(
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val lineTotal: Double,
    val purchasePrice: Double = 0.0
)

data class FinancialReport(
    val periodLabel: String = "This Month",
    val totalSalesRevenue: Double = 0.0,
    val totalCashReceived: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val cogs: Double = 0.0,
    val grossProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalUdharOutstanding: Double = 0.0,
    val inventoryCostValue: Double = 0.0,
    val inventoryRetailValue: Double = 0.0,
    val salesCount: Int = 0,
    val customerCount: Int = 0
)

data class ShopProfile(
    val name: String = "ASLAM RECORD",
    val phone: String = "+91 98765 43210",
    val address: String = "Main Bazar, Shop No. 12",
    val tagline: String = "Bidi Business & Accounts Ledger"
)

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "aslam_ai"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
