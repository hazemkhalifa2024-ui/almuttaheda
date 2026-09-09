package com.example.data.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.Device
import com.example.data.model.ShopConfig
import com.example.data.model.WhatsAppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class WhatsAppService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun formatPhoneNumber(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.startsWith("01") && digits.length == 11 -> "2$digits"
            digits.startsWith("201") -> digits
            else -> digits
        }
    }

    suspend fun sendViaWebhook(
        config: WhatsAppConfig,
        shopConfig: ShopConfig,
        rawPhone: String,
        message: String,
        device: Device? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val formattedNumber = formatPhoneNumber(rawPhone)
        if (config.webhookUrl.isBlank()) {
            return@withContext Result.failure(Exception("رابط Webhook الخاص بـ n8n أو Baileys غير مدخل في إعدادات الواتساب"))
        }

        try {
            val root = JSONObject().apply {
                put("event", "whatsapp_send")
                put("recipient", formattedNumber)
                put("phone", formattedNumber)
                put("rawPhone", rawPhone)
                put("message", message)
                put("timestamp", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date()))
                put("shopId", shopConfig.id)
                put("shopName", shopConfig.name)
                put("shopPhone", shopConfig.phone ?: "")

                if (device != null) {
                    val dev = JSONObject().apply {
                        put("id", device.id)
                        put("ticketNumber", device.ticketNumber)
                        put("customerName", device.customer_name)
                        put("deviceName", device.device_name)
                        put("issue", device.issue_description ?: "")
                        put("cost", device.estimated_cost ?: "0")
                        put("status", device.status)
                        put("statusArabic", device.statusArabicLabel)
                    }
                    put("device", dev)
                }
            }

            val reqBody = root.toString().toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(config.webhookUrl.trim())
                .post(reqBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "MottahedaMaintenanceApp-WhatsApp/1.0")

            if (config.apiToken.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${config.apiToken.trim()}")
                reqBuilder.addHeader("X-Api-Key", config.apiToken.trim())
            }

            val response = client.newCall(reqBuilder.build()).execute()
            if (response.isSuccessful) {
                Result.success("تم إرسال رسالة الواتساب بنجاح عبر السيرفر (HTTP ${response.code})")
            } else {
                Result.failure(Exception("استجابة السيرفر/n8n بالرمز: HTTP ${response.code} ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الوصول لسيرفر الواتساب: ${e.localizedMessage ?: "تأكد من تشغيل n8n وسيرفر Baileys"}"))
        }
    }

    fun openDirectWhatsAppIntent(context: Context, rawPhone: String, message: String): Result<Boolean> {
        return try {
            val formatted = formatPhoneNumber(rawPhone)
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formatted&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("تعذر فتح تطبيق واتساب: ${e.localizedMessage}"))
        }
    }
}
