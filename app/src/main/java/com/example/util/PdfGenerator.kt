package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.domain.FinancialReport
import com.example.domain.SaleItem
import com.example.domain.ShopProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun generateSaleReceipt(
        context: Context,
        sale: SaleEntity,
        items: List<SaleItem>,
        shop: ShopProfile = ShopProfile()
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val primaryPaint = Paint().apply {
            color = Color.rgb(15, 81, 50) // Emerald
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Header Background
        canvas.drawRect(0f, 0f, 595f, 110f, primaryPaint)
        canvas.drawText(shop.name, 40f, 45f, titlePaint)
        
        val subPaint = Paint().apply {
            color = Color.rgb(255, 243, 205)
            textSize = 12f
            isAntiAlias = true
        }
        canvas.drawText(shop.tagline, 40f, 70f, subPaint)
        canvas.drawText("${shop.address} | Tel: ${shop.phone}", 40f, 90f, subPaint)

        // Receipt Banner
        boldPaint.textSize = 16f
        boldPaint.color = Color.rgb(15, 81, 50)
        canvas.drawText("TAX INVOICE / SALE RECEIPT", 40f, 140f, boldPaint)
        
        boldPaint.textSize = 13f
        canvas.drawText("Invoice No: ${sale.invoiceNumber}", 40f, 165f, boldPaint)
        canvas.drawText("Date: ${dateFormat.format(Date(sale.timestamp))}", 320f, 165f, boldPaint)

        canvas.drawLine(40f, 180f, 555f, 180f, linePaint)

        // Customer Info
        canvas.drawText("Customer: ${sale.customerName}", 40f, 205f, boldPaint)
        canvas.drawText("Payment Method: ${sale.paymentMethod}", 320f, 205f, textPaint)

        canvas.drawLine(40f, 220f, 555f, 220f, linePaint)

        // Table Header
        val headerPaint = Paint().apply {
            color = Color.rgb(240, 244, 240)
            style = Paint.Style.FILL
        }
        canvas.drawRect(40f, 235f, 555f, 260f, headerPaint)
        
        boldPaint.textSize = 11f
        canvas.drawText("Item / Description", 50f, 252f, boldPaint)
        canvas.drawText("Qty", 300f, 252f, boldPaint)
        canvas.drawText("Rate", 380f, 252f, boldPaint)
        canvas.drawText("Amount (Rs)", 470f, 252f, boldPaint)

        var y = 285f
        for (item in items) {
            canvas.drawText(item.productName, 50f, y, textPaint)
            canvas.drawText("${item.quantity} ${item.unit}", 300f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.unitPrice), 380f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.lineTotal), 470f, y, boldPaint)
            y += 24f
        }

        canvas.drawLine(40f, y + 10f, 555f, y + 10f, linePaint)
        y += 35f

        // Totals Box
        boldPaint.textSize = 13f
        canvas.drawText("Total Bill:", 320f, y, boldPaint)
        canvas.drawText(String.format(Locale.US, "Rs. %.2f", sale.totalAmount), 460f, y, boldPaint)
        y += 24f

        canvas.drawText("Amount Paid:", 320f, y, textPaint)
        val greenPaint = Paint().apply {
            color = Color.rgb(25, 135, 84)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(String.format(Locale.US, "Rs. %.2f", sale.amountPaid), 460f, y, greenPaint)
        y += 24f

        val redPaint = Paint().apply {
            color = if (sale.outstandingBalance > 0) Color.rgb(220, 53, 69) else Color.rgb(25, 135, 84)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Remaining Udhar:", 320f, y, redPaint)
        canvas.drawText(String.format(Locale.US, "Rs. %.2f", sale.outstandingBalance), 460f, y, redPaint)
        y += 50f

        // Footer
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("Thank you for your business! | Computer generated receipt by ASLAM RECORD", 120f, y, footerPaint)

        document.finishPage(page)

        val dir = File(context.cacheDir, "receipts")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Sale_${sale.invoiceNumber}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun generatePaymentReceipt(
        context: Context,
        payment: PaymentEntity,
        currentOutstanding: Double,
        shop: ShopProfile = ShopProfile()
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val primaryPaint = Paint().apply {
            color = Color.rgb(15, 81, 50)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Header Background
        canvas.drawRect(0f, 0f, 595f, 110f, primaryPaint)
        canvas.drawText(shop.name, 40f, 45f, titlePaint)
        
        val subPaint = Paint().apply {
            color = Color.rgb(255, 243, 205)
            textSize = 12f
            isAntiAlias = true
        }
        canvas.drawText(shop.tagline, 40f, 70f, subPaint)
        canvas.drawText("${shop.address} | Tel: ${shop.phone}", 40f, 90f, subPaint)

        // Banner
        boldPaint.textSize = 16f
        boldPaint.color = Color.rgb(15, 81, 50)
        canvas.drawText("PAYMENT RECEIVED VOUCHER", 40f, 140f, boldPaint)

        boldPaint.textSize = 13f
        boldPaint.color = Color.BLACK
        canvas.drawText("Receipt No: ${payment.receiptNumber}", 40f, 165f, boldPaint)
        canvas.drawText("Date: ${dateFormat.format(Date(payment.timestamp))}", 320f, 165f, boldPaint)

        canvas.drawLine(40f, 180f, 555f, 180f, linePaint)

        canvas.drawText("Received From: ${payment.customerName}", 40f, 210f, boldPaint)
        canvas.drawText("Mode of Payment: ${payment.paymentMethod}", 320f, 210f, textPaint)

        canvas.drawLine(40f, 230f, 555f, 230f, linePaint)

        // Amount Box
        val boxPaint = Paint().apply {
            color = Color.rgb(240, 248, 245)
            style = Paint.Style.FILL
        }
        canvas.drawRect(40f, 250f, 555f, 340f, boxPaint)

        boldPaint.textSize = 15f
        boldPaint.color = Color.rgb(25, 135, 84)
        canvas.drawText("Amount Received:", 60f, 290f, boldPaint)
        boldPaint.textSize = 22f
        canvas.drawText(String.format(Locale.US, "Rs. %.2f", payment.amount), 230f, 290f, boldPaint)

        textPaint.textSize = 11f
        canvas.drawText("Note / Narration: ${payment.notes}", 60f, 320f, textPaint)

        // Balance status
        var y = 380f
        boldPaint.textSize = 14f
        boldPaint.color = Color.BLACK
        canvas.drawText("Customer Current Ledger Balance:", 40f, y, boldPaint)
        
        val balPaint = Paint().apply {
            color = if (currentOutstanding > 0) Color.rgb(220, 53, 69) else Color.rgb(25, 135, 84)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(String.format(Locale.US, "Rs. %.2f", currentOutstanding), 320f, y, balPaint)
        y += 60f

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("Payment verified and recorded in ASLAM RECORD ledger.", 130f, y, footerPaint)

        document.finishPage(page)

        val dir = File(context.cacheDir, "receipts")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Payment_${payment.receiptNumber}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun generateInventoryReport(
        context: Context,
        products: List<ProductEntity>,
        shop: ShopProfile = ShopProfile()
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val primaryPaint = Paint().apply {
            color = Color.rgb(15, 81, 50)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Header
        canvas.drawRect(0f, 0f, 595f, 90f, primaryPaint)
        canvas.drawText("${shop.name} - STOCK & INVENTORY REPORT", 30f, 40f, titlePaint)
        
        val subPaint = Paint().apply {
            color = Color.rgb(255, 243, 205)
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("Generated on: ${dateFormat.format(Date())} | Total Products: ${products.size}", 30f, 70f, subPaint)

        // Summary bar
        val totalCost = products.sumOf { it.stockQuantity * it.purchasePrice }
        val totalRetail = products.sumOf { it.stockQuantity * it.sellingPrice }
        val lowStockCount = products.count { it.stockQuantity <= it.lowStockThreshold }

        val barPaint = Paint().apply {
            color = Color.rgb(240, 245, 242)
            style = Paint.Style.FILL
        }
        canvas.drawRect(30f, 105f, 565f, 140f, barPaint)

        boldPaint.textSize = 11f
        canvas.drawText(String.format(Locale.US, "Cost Value: Rs. %.2f", totalCost), 40f, 127f, boldPaint)
        canvas.drawText(String.format(Locale.US, "Retail Value: Rs. %.2f", totalRetail), 220f, 127f, boldPaint)
        val alertPaint = Paint().apply {
            color = if (lowStockCount > 0) Color.rgb(220, 53, 69) else Color.rgb(25, 135, 84)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Low Stock: $lowStockCount items", 420f, 127f, alertPaint)

        // Table Header
        canvas.drawRect(30f, 150f, 565f, 172f, primaryPaint)
        val tableHeadPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Product Name", 35f, 165f, tableHeadPaint)
        canvas.drawText("Category", 175f, 165f, tableHeadPaint)
        canvas.drawText("Stock", 260f, 165f, tableHeadPaint)
        canvas.drawText("Buy (Rs)", 330f, 165f, tableHeadPaint)
        canvas.drawText("Sell (Rs)", 395f, 165f, tableHeadPaint)
        canvas.drawText("Valuation (Rs)", 465f, 165f, tableHeadPaint)

        var y = 190f
        for (p in products) {
            if (y > 800f) break // Single page fit or max rows
            val isLow = p.stockQuantity <= p.lowStockThreshold
            val namePaint = if (isLow) {
                Paint().apply {
                    color = Color.rgb(220, 53, 69)
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            } else textPaint

            canvas.drawText(if (isLow) "! ${p.name}" else p.name, 35f, y, namePaint)
            canvas.drawText(p.category, 175f, y, textPaint)
            canvas.drawText("${p.stockQuantity} ${p.unit}", 260f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", p.purchasePrice), 330f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", p.sellingPrice), 395f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", p.stockQuantity * p.purchasePrice), 465f, y, textPaint)
            y += 20f
        }

        document.finishPage(page)

        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Stock_Report_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun generateBusinessReport(
        context: Context,
        report: FinancialReport,
        shop: ShopProfile = ShopProfile()
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val primaryPaint = Paint().apply {
            color = Color.rgb(15, 81, 50)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Header
        canvas.drawRect(0f, 0f, 595f, 100f, primaryPaint)
        canvas.drawText("${shop.name} - PROFIT & LOSS REPORT", 40f, 45f, titlePaint)
        
        val subPaint = Paint().apply {
            color = Color.rgb(255, 243, 205)
            textSize = 11f
            isAntiAlias = true
        }
        canvas.drawText("Period: ${report.periodLabel} | Date: ${shortDateFormat.format(Date())}", 40f, 75f, subPaint)

        var y = 140f
        
        fun drawRow(label: String, amount: Double, isBold: Boolean = false, colorCode: Int? = null) {
            val p = if (isBold) {
                Paint().apply {
                    color = colorCode ?: Color.BLACK
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
            } else {
                Paint().apply {
                    color = colorCode ?: Color.BLACK
                    textSize = 12f
                    isAntiAlias = true
                }
            }
            canvas.drawText(label, 50f, y, p)
            canvas.drawText(String.format(Locale.US, "Rs. %.2f", amount), 420f, y, p)
            canvas.drawLine(50f, y + 8f, 545f, y + 8f, linePaint)
            y += 32f
        }

        boldPaint.color = Color.rgb(15, 81, 50)
        canvas.drawText("REVENUE & CASH COLLECTION", 50f, y, boldPaint)
        y += 26f

        drawRow("Total Sales Revenue (Billed)", report.totalSalesRevenue)
        drawRow("Total Payments Received (Cash In Hand/UPI)", report.totalCashReceived, isBold = true, colorCode = Color.rgb(25, 135, 84))

        y += 15f
        boldPaint.color = Color.rgb(15, 81, 50)
        canvas.drawText("COST & PROFITABILITY", 50f, y, boldPaint)
        y += 26f

        drawRow("Cost of Goods Sold (COGS)", report.cogs)
        drawRow("Gross Profit (Revenue - COGS)", report.grossProfit, isBold = true)
        drawRow("Recorded Operating Expenses", report.totalExpenses, colorCode = Color.rgb(220, 53, 69))
        
        val netColor = if (report.netProfit >= 0) Color.rgb(25, 135, 84) else Color.rgb(220, 53, 69)
        drawRow("NET BUSINESS PROFIT", report.netProfit, isBold = true, colorCode = netColor)

        y += 15f
        boldPaint.color = Color.rgb(15, 81, 50)
        canvas.drawText("BALANCE SHEET / VALUATION", 50f, y, boldPaint)
        y += 26f

        drawRow("Total Outstanding Customer Udhar (Receivable)", report.totalUdharOutstanding, isBold = true, colorCode = Color.rgb(220, 53, 69))
        drawRow("Current Inventory Cost Valuation", report.inventoryCostValue)
        drawRow("Current Inventory Retail Valuation", report.inventoryRetailValue)

        y += 40f
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("Calculated from verified local database transactions. No simulated figures.", 110f, y, footerPaint)

        document.finishPage(page)

        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Financial_Report_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun sharePdf(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share $title")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
