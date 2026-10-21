package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val productsRestored: Int = 0,
    val customersRestored: Int = 0,
    val suppliersRestored: Int = 0,
    val salesRestored: Int = 0,
    val paymentsRestored: Int = 0,
    val expensesRestored: Int = 0
)

object BackupManager {

    suspend fun createBackupFile(context: Context, database: AppDatabase): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appId", "com.aslamrecord.app")
        root.put("appName", "ASLAM RECORD")
        val timestamp = System.currentTimeMillis()
        root.put("timestamp", timestamp)
        root.put("formattedDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp)))

        // 1. Products (Bidi & Dalmot)
        val products = database.productDao().getAllProducts().first()
        val productsArr = JSONArray()
        for (p in products) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("category", p.category)
            obj.put("purchasePrice", p.purchasePrice)
            obj.put("sellingPrice", p.sellingPrice)
            obj.put("stockQuantity", p.stockQuantity)
            obj.put("unit", p.unit)
            obj.put("description", p.description)
            obj.put("lowStockThreshold", p.lowStockThreshold)
            obj.put("imageUri", p.imageUri)
            obj.put("createdAt", p.createdAt)
            productsArr.put(obj)
        }
        root.put("products", productsArr)

        // 2. Customers
        val customers = database.customerDao().getAllCustomers().first()
        val customersArr = JSONArray()
        for (c in customers) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("address", c.address)
            obj.put("notes", c.notes)
            obj.put("outstandingBalance", c.outstandingBalance)
            obj.put("points", c.points)
            obj.put("imageUri", c.imageUri)
            obj.put("createdAt", c.createdAt)
            customersArr.put(obj)
        }
        root.put("customers", customersArr)

        // 3. Suppliers
        val suppliers = database.supplierDao().getAllSuppliers().first()
        val suppliersArr = JSONArray()
        for (s in suppliers) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("phone", s.phone)
            obj.put("company", s.company)
            obj.put("balance", s.balance)
            obj.put("createdAt", s.createdAt)
            suppliersArr.put(obj)
        }
        root.put("suppliers", suppliersArr)

        // 4. Sales
        val sales = database.saleDao().getAllSales().first()
        val salesArr = JSONArray()
        for (sale in sales) {
            val obj = JSONObject()
            obj.put("id", sale.id)
            obj.put("invoiceNumber", sale.invoiceNumber)
            obj.put("customerId", sale.customerId ?: -1L)
            obj.put("customerName", sale.customerName)
            obj.put("totalAmount", sale.totalAmount)
            obj.put("amountPaid", sale.amountPaid)
            obj.put("outstandingBalance", sale.outstandingBalance)
            obj.put("paymentMethod", sale.paymentMethod)
            obj.put("status", sale.status)
            obj.put("itemsJson", sale.itemsJson)
            obj.put("notes", sale.notes)
            obj.put("timestamp", sale.timestamp)
            salesArr.put(obj)
        }
        root.put("sales", salesArr)

        // 5. Payments
        val payments = database.paymentDao().getAllPayments().first()
        val paymentsArr = JSONArray()
        for (pay in payments) {
            val obj = JSONObject()
            obj.put("id", pay.id)
            obj.put("receiptNumber", pay.receiptNumber)
            obj.put("customerId", pay.customerId ?: -1L)
            obj.put("customerName", pay.customerName)
            obj.put("saleId", pay.saleId ?: -1L)
            obj.put("amount", pay.amount)
            obj.put("paymentMethod", pay.paymentMethod)
            obj.put("paymentType", pay.paymentType)
            obj.put("notes", pay.notes)
            obj.put("timestamp", pay.timestamp)
            paymentsArr.put(obj)
        }
        root.put("payments", paymentsArr)

        // 6. Expenses
        val expenses = database.expenseDao().getAllExpenses().first()
        val expensesArr = JSONArray()
        for (e in expenses) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("title", e.title)
            obj.put("category", e.category)
            obj.put("amount", e.amount)
            obj.put("notes", e.notes)
            obj.put("timestamp", e.timestamp)
            expensesArr.put(obj)
        }
        root.put("expenses", expensesArr)

        // Write to cache/backups directory
        val backupsDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))
        val file = File(backupsDir, "aslam_record_backup_$dateStr.json")
        file.writeText(root.toString(2), Charsets.UTF_8)
        file
    }

    fun shareBackupFile(context: Context, backupFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ASLAM RECORD Backup - ${backupFile.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share or Save ASLAM RECORD Backup"))
    }

    suspend fun restoreFromJson(context: Context, database: AppDatabase, jsonContent: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonContent)
            if (!root.has("appName") && !root.has("version")) {
                return@withContext RestoreResult(false, "Invalid backup format: missing ASLAM RECORD metadata.")
            }

            var productsRestored = 0
            var customersRestored = 0
            var suppliersRestored = 0
            var salesRestored = 0
            var paymentsRestored = 0
            var expensesRestored = 0

            // 1. Restore Products
            if (root.has("products")) {
                val arr = root.getJSONArray("products")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val product = ProductEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        category = obj.optString("category", "Bidi"),
                        purchasePrice = obj.optDouble("purchasePrice", 0.0),
                        sellingPrice = obj.optDouble("sellingPrice", 0.0),
                        stockQuantity = obj.optDouble("stockQuantity", 0.0),
                        unit = obj.optString("unit", "Packet"),
                        description = obj.optString("description", ""),
                        lowStockThreshold = obj.optDouble("lowStockThreshold", 10.0),
                        imageUri = obj.optString("imageUri", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.productDao().insertProduct(product)
                    productsRestored++
                }
            }

            // 2. Restore Customers
            if (root.has("customers")) {
                val arr = root.getJSONArray("customers")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val customer = CustomerEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        phone = obj.optString("phone", ""),
                        address = obj.optString("address", ""),
                        notes = obj.optString("notes", ""),
                        outstandingBalance = obj.optDouble("outstandingBalance", 0.0),
                        points = obj.optInt("points", 0),
                        imageUri = obj.optString("imageUri", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.customerDao().insertCustomer(customer)
                    customersRestored++
                }
            }

            // 3. Restore Suppliers
            if (root.has("suppliers")) {
                val arr = root.getJSONArray("suppliers")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val supplier = SupplierEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        phone = obj.optString("phone", ""),
                        company = obj.optString("company", ""),
                        balance = obj.optDouble("balance", 0.0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.supplierDao().insertSupplier(supplier)
                    suppliersRestored++
                }
            }

            // 4. Restore Sales
            if (root.has("sales")) {
                val arr = root.getJSONArray("sales")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val custId = obj.optLong("customerId", -1L)
                    val sale = SaleEntity(
                        id = obj.optLong("id", 0L),
                        invoiceNumber = obj.getString("invoiceNumber"),
                        customerId = if (custId > 0) custId else null,
                        customerName = obj.optString("customerName", "Cash Customer"),
                        totalAmount = obj.optDouble("totalAmount", 0.0),
                        amountPaid = obj.optDouble("amountPaid", 0.0),
                        outstandingBalance = obj.optDouble("outstandingBalance", 0.0),
                        paymentMethod = obj.optString("paymentMethod", "Cash"),
                        status = obj.optString("status", "PAID"),
                        itemsJson = obj.optString("itemsJson", "[]"),
                        notes = obj.optString("notes", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    database.saleDao().insertSale(sale)
                    salesRestored++
                }
            }

            // 5. Restore Payments
            if (root.has("payments")) {
                val arr = root.getJSONArray("payments")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val custId = obj.optLong("customerId", -1L)
                    val saleId = obj.optLong("saleId", -1L)
                    val payment = PaymentEntity(
                        id = obj.optLong("id", 0L),
                        receiptNumber = obj.getString("receiptNumber"),
                        customerId = if (custId > 0) custId else null,
                        customerName = obj.optString("customerName", "Customer"),
                        saleId = if (saleId > 0) saleId else null,
                        amount = obj.optDouble("amount", 0.0),
                        paymentMethod = obj.optString("paymentMethod", "Cash"),
                        paymentType = obj.optString("paymentType", "BALANCE_PAYMENT"),
                        notes = obj.optString("notes", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    database.paymentDao().insertPayment(payment)
                    paymentsRestored++
                }
            }

            // 6. Restore Expenses
            if (root.has("expenses")) {
                val arr = root.getJSONArray("expenses")
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val expense = ExpenseEntity(
                        id = obj.optLong("id", 0L),
                        title = obj.getString("title"),
                        category = obj.optString("category", "General"),
                        amount = obj.optDouble("amount", 0.0),
                        notes = obj.optString("notes", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    database.expenseDao().insertExpense(expense)
                    expensesRestored++
                }
            }

            RestoreResult(
                success = true,
                message = "Backup restored successfully with complete data integrity!",
                productsRestored = productsRestored,
                customersRestored = customersRestored,
                suppliersRestored = suppliersRestored,
                salesRestored = salesRestored,
                paymentsRestored = paymentsRestored,
                expensesRestored = expensesRestored
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                message = "Failed to restore backup: ${e.message}"
            )
        }
    }
}
