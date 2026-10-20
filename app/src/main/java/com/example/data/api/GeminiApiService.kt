package com.example.data.api

import com.example.BuildConfig
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.domain.FinancialReport
import com.example.domain.ShopProfile
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private val moshi = try {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    } catch (_: Throwable) {
        Moshi.Builder().build()
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: GeminiApi = retrofit.create(GeminiApi::class.java)

    suspend fun consultAdvisor(
        userPrompt: String,
        report: FinancialReport,
        products: List<ProductEntity>,
        customersWithDue: List<CustomerEntity>,
        shop: ShopProfile = ShopProfile(),
        preferHindi: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER")) {
                key.trim()
            } else {
                (System.getenv("GEMINI_API_KEY") ?: "").trim()
            }
        } catch (_: Throwable) {
            (System.getenv("GEMINI_API_KEY") ?: "").trim()
        }

        val hasValidKey = apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                !apiKey.contains("PLACEHOLDER")

        // First prepare the exact financial ground-truth data from live DB
        val totalDue = customersWithDue.sumOf { it.outstandingBalance }
        val lowStockItems = products.filter { it.stockQuantity <= it.lowStockThreshold }

        if (hasValidKey) {
            try {
                val systemContext = """
                    You are ASLAM AI, the personal shop business advisor, accountant and smart manager for ${shop.name} (${shop.tagline}).
                    
                    ROLE & CAPABILITIES:
                    - Advise the owner on business profits, optimal selling prices, margins, savings, and inventory management.
                    - Suggest best selling price: analyze buy price vs market rates to maximize profit while retaining customers.
                    - Answer financial questions accurately based ONLY on the live shop data provided below.
                    - Respond in clean, professional English or warm Hinglish depending on what the user asks.
                    - Keep formatting clean with bullet points and bold headers.
                    
                    CRITICAL FORMATTING RULES:
                    - NEVER use dollar signs ($) or double dollars ($$). Always use ₹ for Indian Rupees.
                    - NEVER use LaTeX math notation or weird signs like -$$ or \frac{} or \text{}.
                    - Always write numbers cleanly, e.g., ₹250 or ₹1,500.50.
                    
                    CURRENT LIVE SHOP DATA (DATABASE SOURCE OF TRUTH):
                    - Selected Period: ${report.periodLabel}
                    - Total Sales Revenue (Billed): ₹${String.format(Locale.US, "%.2f", report.totalSalesRevenue)}
                    - Cash/UPI Payments Received (In-Hand): ₹${String.format(Locale.US, "%.2f", report.totalCashReceived)}
                    - Cost of Goods Sold (COGS): ₹${String.format(Locale.US, "%.2f", report.cogs)}
                    - Gross Profit (Revenue - COGS): ₹${String.format(Locale.US, "%.2f", report.grossProfit)}
                    - Recorded Shop Operating Expenses: ₹${String.format(Locale.US, "%.2f", report.totalExpenses)}
                    - Net Business Profit: ₹${String.format(Locale.US, "%.2f", report.netProfit)}
                    - Total Customer Udhar (Outstanding): ₹${String.format(Locale.US, "%.2f", totalDue)}
                    - Total Products in Database: ${products.size}
                    - Current Inventory Cost Valuation: ₹${String.format(Locale.US, "%.2f", report.inventoryCostValue)}
                    - Current Inventory Retail Valuation: ₹${String.format(Locale.US, "%.2f", report.inventoryRetailValue)}
                    - Customers with pending udhar: ${if (customersWithDue.isEmpty()) "None (All Paid)" else customersWithDue.joinToString { "${it.name} (${it.phone}): ₹${String.format(Locale.US, "%.2f", it.outstandingBalance)}" }}
                    - Low Stock Alert Items: ${if (lowStockItems.isEmpty()) "None" else lowStockItems.joinToString { "${it.name} (${it.stockQuantity} ${it.unit} left, Threshold: ${it.lowStockThreshold})" }}
                    - Catalog Items with Margins: ${products.take(15).joinToString { "${it.name} (${it.unit}): Buy ₹${it.purchasePrice}, Sell ₹${it.sellingPrice}, Profit ₹${it.sellingPrice - it.purchasePrice}, Stock ${it.stockQuantity}" }}
                    
                    USER QUERY: $userPrompt
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = systemContext))
                        )
                    )
                )

                val response = api.generateContent(apiKey, request)
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!reply.isNullOrBlank()) {
                    val cleaned = reply
                        .replace("$$", "₹")
                        .replace("$", "₹")
                        .replace("₹-", "- ₹")
                        .replace("-₹", "- ₹")
                        .replace("\\text{", "")
                        .replace("\\frac{", "")
                        .replace("}", "")
                        .replace("{", "")
                        .replace("\\", "")
                    return@withContext cleaned
                }
            } catch (_: Throwable) {
                // Fallback smoothly and reliably to local calculation engine
            }
        }

        // Reliable local calculation analysis engine (Works 100% offline & without API key)
        return@withContext localAnalysis(userPrompt, report, products, customersWithDue, hasValidKey)
    }

    private fun localAnalysis(
        prompt: String,
        report: FinancialReport,
        products: List<ProductEntity>,
        customersWithDue: List<CustomerEntity>,
        hasValidKey: Boolean
    ): String {
        val lower = prompt.lowercase(Locale.ROOT)
        val sb = StringBuilder()

        val totalDue = customersWithDue.sumOf { it.outstandingBalance }
        val lowStock = products.filter { it.stockQuantity <= it.lowStockThreshold }

        // Specific product margin inquiry
        val matchedProduct = products.firstOrNull { lower.contains(it.name.lowercase(Locale.ROOT)) }
        if (matchedProduct != null && (lower.contains("margin") || lower.contains("profit") || lower.contains("rate") || lower.contains("price") || lower.contains("भाव") || lower.contains("मार्जिन"))) {
            val margin = matchedProduct.sellingPrice - matchedProduct.purchasePrice
            val marginPercent = if (matchedProduct.purchasePrice > 0) (margin / matchedProduct.purchasePrice) * 100 else 0.0
            sb.append("🏷️ **Product Analysis: ${matchedProduct.name}**\n\n")
            sb.append("• Buy Price (खरीद भाव): ₹${String.format(Locale.US, "%.2f", matchedProduct.purchasePrice)}\n")
            sb.append("• Sell Price (बिक्री भाव): ₹${String.format(Locale.US, "%.2f", matchedProduct.sellingPrice)}\n")
            sb.append("• Profit per ${matchedProduct.unit}: ₹${String.format(Locale.US, "%.2f", margin)} (${String.format(Locale.US, "%.1f", marginPercent)}%)\n")
            sb.append("• Current Stock in Hand: ${matchedProduct.stockQuantity.toInt()} ${matchedProduct.unit}\n")
            sb.append("• Total Stock Cost: ₹${String.format(Locale.US, "%.2f", matchedProduct.stockQuantity * matchedProduct.purchasePrice)}\n\n")
            val suggested = if (matchedProduct.purchasePrice > 0) matchedProduct.purchasePrice * 1.25 else matchedProduct.sellingPrice * 1.15
            sb.append("🎯 **Recommended Sell Rate:** ₹${String.format(Locale.US, "%.2f", suggested)} (25% optimal business margin)")
            return sb.toString()
        }

        if (lower.contains("cash") || lower.contains("कैश") || lower.contains("payment") || lower.contains("पेमेंट") || lower.contains("received") || lower.contains("जमा")) {
            sb.append("💵 **Cash & Collection Summary (${report.periodLabel})**\n\n")
            sb.append("• Total Payments Received (Cash/UPI): ₹${String.format(Locale.US, "%.2f", report.totalCashReceived)}\n")
            sb.append("• Total Invoiced Sales: ₹${String.format(Locale.US, "%.2f", report.totalSalesRevenue)}\n")
            val pending = (report.totalSalesRevenue - report.totalCashReceived).coerceAtLeast(0.0)
            if (pending > 0) {
                sb.append("• Pending Credit Balance (Udhar): ₹${String.format(Locale.US, "%.2f", pending)}\n")
            } else {
                sb.append("• Payment Status: All sales settled in cash/UPI!\n")
            }
            sb.append("• Total Customers currently with Dues: ${customersWithDue.size}\n")
        } else if (lower.contains("price") || lower.contains("sell") || lower.contains("rate") || lower.contains("बिक्री भाव") || lower.contains("कितने में") || lower.contains("suggest")) {
            sb.append("💡 **Selling Price & Margin Recommendation**\n\n")
            if (products.isEmpty()) {
                sb.append("No products added yet. Please add products with Buy and Sell prices to view profit suggestions.\n")
            } else {
                sb.append("Here is the margin analysis and suggested selling prices for your inventory:\n\n")
                products.take(6).forEach { p ->
                    val margin = p.sellingPrice - p.purchasePrice
                    val marginPercent = if (p.purchasePrice > 0) (margin / p.purchasePrice) * 100 else 0.0
                    val suggestedPrice = if (p.purchasePrice > 0) p.purchasePrice * 1.25 else p.sellingPrice * 1.15
                    sb.append("• **${p.name}**\n")
                    sb.append("   - Buy Price: ₹${String.format(Locale.US, "%.2f", p.purchasePrice)}\n")
                    sb.append("   - Current Sell Price: ₹${String.format(Locale.US, "%.2f", p.sellingPrice)}\n")
                    sb.append("   - Profit per Unit: ₹${String.format(Locale.US, "%.2f", margin)} (${String.format(Locale.US, "%.1f", marginPercent)}%)\n")
                    sb.append("   - 🎯 Recommended Selling Price: ₹${String.format(Locale.US, "%.2f", suggestedPrice)} (25% margin)\n\n")
                }
                sb.append("💡 **Pro Tip:** Keep high-volume bidi bundles at 15-20% margin for fast turnover, and specialty tobacco at 25-30% margin.")
            }
        } else if (lower.contains("profit") || lower.contains("saving") || lower.contains("बचत") || lower.contains("मुनाफा") || lower.contains("फायदा")) {
            sb.append("💰 **Profit & Savings Summary (${report.periodLabel})**\n\n")
            sb.append("1. Total Sales Revenue: ₹${String.format(Locale.US, "%.2f", report.totalSalesRevenue)}\n")
            sb.append("2. Cost of Goods Sold (Purchase Cost of Items Sold): ₹${String.format(Locale.US, "%.2f", report.cogs)}\n")
            sb.append("3. Gross Profit (Revenue - COGS): ₹${String.format(Locale.US, "%.2f", report.grossProfit)}\n")
            sb.append("4. Operating Expenses: ₹${String.format(Locale.US, "%.2f", report.totalExpenses)}\n")
            sb.append("───────────────────────\n")
            val status = if (report.netProfit >= 0) "✅ Net Business Profit" else "⚠️ Net Loss"
            sb.append("$status: ₹${String.format(Locale.US, "%.2f", report.netProfit)}\n\n")
            if (report.totalSalesRevenue > 0) {
                val netMargin = (report.netProfit / report.totalSalesRevenue) * 100
                sb.append("📈 Net Profit Margin: ${String.format(Locale.US, "%.1f", netMargin)}%\n")
            }
            sb.append("💡 Note: Unsold inventory is not deducted from profit. Profit is calculated purely on sold stock minus expenses.")
        } else if (lower.contains("sale") || lower.contains("revenue") || lower.contains("बिक्री") || lower.contains("कमाई")) {
            sb.append("📊 **Sales Report (${report.periodLabel})**\n\n")
            sb.append("• Total Invoiced Sales: ₹${String.format(Locale.US, "%.2f", report.totalSalesRevenue)}\n")
            sb.append("• Cash/UPI Received: ₹${String.format(Locale.US, "%.2f", report.totalCashReceived)}\n")
            sb.append("• Total Invoice Bills: ${report.salesCount}\n\n")
            if (report.totalSalesRevenue > report.totalCashReceived) {
                val pending = report.totalSalesRevenue - report.totalCashReceived
                sb.append("⚠️ Pending Credit / Udhar: ₹${String.format(Locale.US, "%.2f", pending)}")
            } else {
                sb.append("✅ All recorded sales are fully paid!")
            }
        } else if (lower.contains("udhar") || lower.contains("due") || lower.contains("credit") || lower.contains("बाकी") || lower.contains("बकाया") || lower.contains("owed")) {
            sb.append("👥 **Customer Outstanding Khata Ledger**\n\n")
            sb.append("• Total Outstanding Due: ₹${String.format(Locale.US, "%.2f", totalDue)}\n")
            sb.append("• Customers with Pending Dues: ${customersWithDue.size}\n\n")
            if (customersWithDue.isEmpty()) {
                sb.append("🎉 Great! No customers currently have outstanding balances.")
            } else {
                sb.append("Customer-wise Outstanding Breakdown:\n")
                customersWithDue.forEachIndexed { i, c ->
                    sb.append("${i + 1}. **${c.name}** (${c.phone}): ₹${String.format(Locale.US, "%.2f", c.outstandingBalance)}\n")
                }
                sb.append("\n💡 **Action:** Go to the Customers screen to send one-tap WhatsApp payment reminders.")
            }
        } else if (lower.contains("stock") || lower.contains("item") || lower.contains("inventory") || lower.contains("सामान") || lower.contains("reorder") || lower.contains("कम")) {
            sb.append("📦 **Stock & Inventory Status**\n\n")
            sb.append("• Total Products in Catalog: ${products.size}\n")
            sb.append("• Stock Value at Purchase Cost: ₹${String.format(Locale.US, "%.2f", report.inventoryCostValue)}\n")
            sb.append("• Estimated Retail Value: ₹${String.format(Locale.US, "%.2f", report.inventoryRetailValue)}\n\n")
            if (lowStock.isEmpty()) {
                sb.append("✅ All products have sufficient stock!")
            } else {
                sb.append("⚠️ **Low Stock Reorder Alerts (${lowStock.size} items):**\n")
                lowStock.forEach { p ->
                    sb.append("• **${p.name}**: only ${p.stockQuantity.toInt()} ${p.unit} remaining (Reorder threshold: ${p.lowStockThreshold.toInt()})\n")
                }
                sb.append("\n💡 **Recommendation:** Place a replenishment order with your supplier to prevent stockouts.")
            }
        } else {
            sb.append("नमस्ते असलम भाई! I am ASLAM AI, your digital shop accountant and manager.\n\n")
            sb.append("Current Shop Health (${report.periodLabel}):\n")
            sb.append("• Sales Revenue: ₹${String.format(Locale.US, "%.2f", report.totalSalesRevenue)}\n")
            sb.append("• Cash Received: ₹${String.format(Locale.US, "%.2f", report.totalCashReceived)}\n")
            sb.append("• Total Customer Udhar: ₹${String.format(Locale.US, "%.2f", totalDue)}\n")
            sb.append("• Net Business Profit: ₹${String.format(Locale.US, "%.2f", report.netProfit)}\n")
            sb.append("• Low Stock Items: ${lowStock.size}\n\n")
            sb.append("Quick Questions You Can Ask:\n")
            sb.append("• 'What were today\\'s sales and cash received?'\n")
            sb.append("• 'Who owes us udhar money and how much?'\n")
            sb.append("• 'What is today\\'s gross and net profit?'\n")
            sb.append("• 'Which products need restocking?'\n")
            sb.append("• 'What profit margin do you suggest for bidi?'")
        }

        return sb.toString()
    }
}
