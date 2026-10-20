package com.example.data.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.SaleEntity
import com.example.domain.ShopProfile
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Official WhatsApp Business Platform Cloud API models
@JsonClass(generateAdapter = true)
data class WhatsAppMessageRequest(
    @Json(name = "messaging_product") val messagingProduct: String = "whatsapp",
    @Json(name = "recipient_type") val recipientType: String = "individual",
    @Json(name = "to") val to: String,
    @Json(name = "type") val type: String = "text",
    @Json(name = "text") val text: WhatsAppTextBody? = null
)

@JsonClass(generateAdapter = true)
data class WhatsAppTextBody(
    @Json(name = "preview_url") val previewUrl: Boolean = false,
    @Json(name = "body") val body: String
)

@JsonClass(generateAdapter = true)
data class WhatsAppMessageResponse(
    @Json(name = "messaging_product") val messagingProduct: String? = null,
    @Json(name = "contacts") val contacts: List<WhatsAppContact>? = null,
    @Json(name = "messages") val messages: List<WhatsAppMessageId>? = null
)

@JsonClass(generateAdapter = true)
data class WhatsAppContact(
    @Json(name = "input") val input: String,
    @Json(name = "wa_id") val waId: String
)

@JsonClass(generateAdapter = true)
data class WhatsAppMessageId(
    @Json(name = "id") val id: String
)

interface WhatsAppCloudApi {
    @POST("{phone_number_id}/messages")
    suspend fun sendMessage(
        @Path("phone_number_id") phoneNumberId: String,
        @Header("Authorization") authHeader: String,
        @Body request: WhatsAppMessageRequest
    ): Response<WhatsAppMessageResponse>
}

object WhatsAppManager {

    private val api: WhatsAppCloudApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://graph.facebook.com/v21.0/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(WhatsAppCloudApi::class.java)
    }

    fun formatReceiptMessage(shop: ShopProfile, sale: SaleEntity, customer: CustomerEntity?): String {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.timestamp))
        val dueInfo = if (sale.outstandingBalance > 0) {
            "🔴 *Due / Balance Amount:* ₹${String.format(Locale.US, "%.2f", sale.outstandingBalance)}\n" +
            "📌 *Current Total Khata Due:* ₹${String.format(Locale.US, "%.2f", customer?.outstandingBalance ?: sale.outstandingBalance)}"
        } else {
            "✅ *Payment Status:* FULLY PAID (Thank you!)"
        }

        return """
            🧾 *${shop.name}*
            📍 ${shop.address}
            📞 ${shop.phone}
            ───────────────────────
            *SALES RECEIPT / INVOICE*
            Invoice No: *${sale.invoiceNumber}*
            Date: $dateStr
            Customer: *${sale.customerName}*
            Payment Method: *${sale.paymentMethod}*
            ───────────────────────
            *Total Bill:* ₹${String.format(Locale.US, "%.2f", sale.totalAmount)}
            *Paid Amount:* ₹${String.format(Locale.US, "%.2f", sale.amountPaid)}
            $dueInfo
            ───────────────────────
            Thank you for shopping with us!
        """.trimIndent()
    }

    fun formatPaymentReminderMessage(shop: ShopProfile, customer: CustomerEntity, amount: Double): String {
        return "नमस्ते ${customer.name} जी, आपके खाते में ₹${String.format(Locale.US, "%.2f", amount)} बाकी हैं। कृपया भुगतान करने की सुविधा के अनुसार जानकारी दें। धन्यवाद — ${shop.name}"
    }

    fun launchWhatsAppIntent(context: Context, phone: String, message: String) {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            launchShareIntent(context, message, "Send Payment Reminder")
        }
    }

    fun launchShareIntent(context: Context, message: String, title: String = "Share Reminder") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }

    fun dialCustomer(context: Context, phone: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        context.startActivity(intent)
    }
}

