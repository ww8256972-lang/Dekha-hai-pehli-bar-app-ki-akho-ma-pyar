package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Double,
    val unit: String = "Packet",
    val description: String = "",
    val lowStockThreshold: Double = 10.0,
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val outstandingBalance: Double = 0.0,
    val points: Int = 0,
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String,
    val totalAmount: Double,
    val amountPaid: Double,
    val outstandingBalance: Double,
    val paymentMethod: String, // "Cash", "UPI", "Credit/Udhar"
    val status: String, // "PAID", "PARTIAL", "UNPAID"
    val itemsJson: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptNumber: String,
    val customerId: Long? = null,
    val customerName: String,
    val saleId: Long? = null,
    val amount: Double,
    val paymentMethod: String = "Cash", // "Cash", "UPI", "Bank"
    val paymentType: String = "BALANCE_PAYMENT", // "SALE_PAYMENT", "BALANCE_PAYMENT"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val amount: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val company: String = "",
    val balance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
