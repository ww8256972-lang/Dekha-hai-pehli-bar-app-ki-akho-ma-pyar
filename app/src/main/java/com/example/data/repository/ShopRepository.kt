package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.domain.FinancialReport
import com.example.domain.SaleItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ShopRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val customerDao = database.customerDao()
    private val saleDao = database.saleDao()
    private val paymentDao = database.paymentDao()
    private val expenseDao = database.expenseDao()
    private val supplierDao = database.supplierDao()

    private val moshi = try {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    } catch (_: Throwable) {
        Moshi.Builder().build()
    }
    private val saleItemListType = Types.newParameterizedType(List::class.java, SaleItem::class.java)
    private val saleItemAdapter = moshi.adapter<List<SaleItem>>(saleItemListType)

    fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()
    fun getLowStockProducts(): Flow<List<ProductEntity>> = productDao.getLowStockProducts()
    suspend fun insertProduct(product: ProductEntity): Long = productDao.insertProduct(product)
    suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: ProductEntity) = productDao.deleteProduct(product)

    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    fun getCustomersWithDue(): Flow<List<CustomerEntity>> = customerDao.getCustomersWithDue()
    suspend fun insertCustomer(customer: CustomerEntity): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: CustomerEntity) = customerDao.deleteCustomer(customer)
    suspend fun getCustomerById(id: Long): CustomerEntity? = customerDao.getCustomerById(id)

    fun getAllSales(): Flow<List<SaleEntity>> = saleDao.getAllSales()
    fun getSalesByCustomer(customerId: Long): Flow<List<SaleEntity>> = saleDao.getSalesByCustomer(customerId)
    fun getAllPayments(): Flow<List<PaymentEntity>> = paymentDao.getAllPayments()
    fun getPaymentsByCustomer(customerId: Long): Flow<List<PaymentEntity>> = paymentDao.getPaymentsByCustomer(customerId)
    fun getAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

    fun getAllSuppliers(): Flow<List<SupplierEntity>> = supplierDao.getAllSuppliers()
    suspend fun insertSupplier(supplier: SupplierEntity): Long = supplierDao.insertSupplier(supplier)

    suspend fun recordSale(
        customerId: Long?,
        customerName: String,
        items: List<SaleItem>,
        totalAmount: Double,
        amountPaid: Double,
        paymentMethod: String,
        notes: String = ""
    ): Pair<SaleEntity, PaymentEntity?> = withContext(Dispatchers.IO) {
        val count = saleDao.getSalesCount()
        val invoiceNumber = "INV-${1000 + count + 1}"
        val outstanding = (totalAmount - amountPaid).coerceAtLeast(0.0)
        val status = when {
            outstanding <= 0.0 -> "PAID"
            amountPaid > 0.0 -> "PARTIAL"
            else -> "UNPAID"
        }

        val itemsJson = saleItemAdapter.toJson(items)
        val sale = SaleEntity(
            invoiceNumber = invoiceNumber,
            customerId = customerId,
            customerName = customerName,
            totalAmount = totalAmount,
            amountPaid = amountPaid,
            outstandingBalance = outstanding,
            paymentMethod = paymentMethod,
            status = status,
            itemsJson = itemsJson,
            notes = notes,
            timestamp = System.currentTimeMillis()
        )
        val saleId = saleDao.insertSale(sale)
        val savedSale = sale.copy(id = saleId)

        // Deduct inventory
        for (item in items) {
            productDao.deductStock(item.productId, item.quantity)
        }

        // Update customer outstanding balance if applicable
        if (customerId != null && outstanding > 0.0) {
            customerDao.addDue(customerId, outstanding)
        }

        // Record initial payment record if amount paid > 0
        var savedPayment: PaymentEntity? = null
        if (amountPaid > 0.0) {
            val payCount = paymentDao.getPaymentsCount()
            val receiptNumber = "REC-${2000 + payCount + 1}"
            val payment = PaymentEntity(
                receiptNumber = receiptNumber,
                customerId = customerId,
                customerName = customerName,
                saleId = saleId,
                amount = amountPaid,
                paymentMethod = paymentMethod,
                paymentType = "SALE_PAYMENT",
                notes = "Received during $invoiceNumber",
                timestamp = System.currentTimeMillis()
            )
            val paymentId = paymentDao.insertPayment(payment)
            savedPayment = payment.copy(id = paymentId)
        }

        Pair(savedSale, savedPayment)
    }

    suspend fun recordBalancePayment(
        customerId: Long,
        customerName: String,
        amount: Double,
        paymentMethod: String,
        notes: String = ""
    ): PaymentEntity = withContext(Dispatchers.IO) {
        val payCount = paymentDao.getPaymentsCount()
        val receiptNumber = "REC-${2000 + payCount + 1}"
        val payment = PaymentEntity(
            receiptNumber = receiptNumber,
            customerId = customerId,
            customerName = customerName,
            saleId = null,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentType = "BALANCE_PAYMENT",
            notes = notes.ifEmpty { "Udhar Khata Payment" },
            timestamp = System.currentTimeMillis()
        )
        val paymentId = paymentDao.insertPayment(payment)
        customerDao.reduceDue(customerId, amount)
        payment.copy(id = paymentId)
    }

    fun parseSaleItems(itemsJson: String): List<SaleItem> {
        return try {
            saleItemAdapter.fromJson(itemsJson) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun generateFinancialReport(startTime: Long, endTime: Long, label: String): FinancialReport = withContext(Dispatchers.IO) {
        val sales = saleDao.getSalesBetween(startTime, endTime).first()
        val payments = paymentDao.getPaymentsBetween(startTime, endTime).first()
        val expenses = expenseDao.getExpensesBetween(startTime, endTime).first()
        val products = productDao.getAllProducts().first()
        val customers = customerDao.getAllCustomers().first()

        val totalSalesRevenue = sales.sumOf { it.totalAmount }
        val totalCashReceived = payments.sumOf { it.amount }
        val totalExpenses = expenses.sumOf { it.amount }

        var totalCogs = 0.0
        for (sale in sales) {
            val items = parseSaleItems(sale.itemsJson)
            for (item in items) {
                totalCogs += (item.purchasePrice * item.quantity)
            }
        }

        val grossProfit = totalSalesRevenue - totalCogs
        val netProfit = grossProfit - totalExpenses
        val totalUdharOutstanding = customers.sumOf { it.outstandingBalance }

        val inventoryCostValue = products.sumOf { it.stockQuantity * it.purchasePrice }
        val inventoryRetailValue = products.sumOf { it.stockQuantity * it.sellingPrice }

        FinancialReport(
            periodLabel = label,
            totalSalesRevenue = totalSalesRevenue,
            totalCashReceived = totalCashReceived,
            totalPurchases = totalCogs, // Goods sold cost
            cogs = totalCogs,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            totalUdharOutstanding = totalUdharOutstanding,
            inventoryCostValue = inventoryCostValue,
            inventoryRetailValue = inventoryRetailValue,
            salesCount = sales.size,
            customerCount = customers.size
        )
    }
}
