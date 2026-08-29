package com.example.data.api

import com.example.data.model.Device
import com.example.data.model.N8nConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class N8nWebhookService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendEvent(
        config: N8nConfig,
        eventType: String,
        device: Device? = null,
        extraData: Map<String, Any?> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!config.enabled || config.webhookUrl.isBlank()) {
            return@withContext Result.failure(Exception("n8n Webhook غير مفعل أو الرابط فارغ"))
        }

        try {
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date())
            val rootJson = JSONObject().apply {
                put("event", eventType)
                put("timestamp", nowStr)
                put("source", "Al-Muttahida Maintenance App")

                if (device != null) {
                    val devObj = JSONObject().apply {
                        put("id", device.id)
                        put("ticketNumber", device.ticketNumber)
                        put("customerName", device.customer_name)
                        put("customerPhone", device.customer_phone ?: "")
                        put("deviceName", device.device_name)
                        put("issueDescription", device.issue_description ?: "")
                        put("estimatedCost", device.estimated_cost ?: "0")
                        put("downPayment", device.down_payment)
                        put("remainingBalance", device.remaining_balance)
                        put("technician", device.technician ?: "")
                        put("status", device.status)
                        put("detailedStatus", device.detailed_status ?: device.status)
                        put("statusArabic", device.statusArabicLabel)
                        put("receivedByEmployee", device.received_by_employee ?: "")
                        put("deliveredByEmployee", device.delivered_by_employee ?: "")
                        put("technicianNotes", device.technician_notes ?: "")
                        put("partsUsedSummary", device.parts_used_summary ?: "")
                    }
                    put("device", devObj)
                }

                if (extraData.isNotEmpty()) {
                    val extraObj = JSONObject()
                    extraData.forEach { (k, v) ->
                        extraObj.put(k, v ?: JSONObject.NULL)
                    }
                    put("extra", extraObj)
                }
            }

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val requestBuilder = Request.Builder()
                .url(config.webhookUrl.trim())
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "AlMuttahidaMaintenanceApp/1.0")

            if (config.authSecret.isNotBlank()) {
                requestBuilder.addHeader("X-Webhook-Secret", config.authSecret.trim())
                requestBuilder.addHeader("Authorization", "Bearer ${config.authSecret.trim()}")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: "OK"
                Result.success("نجح إرسال الـ Webhook إلى n8n بنجاح (HTTP ${response.code})")
            } else {
                Result.failure(Exception("استجابة n8n بالخطأ: HTTP ${response.code} ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال بـ n8n: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun testConnection(config: N8nConfig): Result<String> = withContext(Dispatchers.IO) {
        val testDevice = Device(
            id = 999,
            customer_name = "عميل تجريبي n8n",
            customer_phone = "01000000000",
            device_name = "Samsung Galaxy S24 Ultra",
            issue_description = "تجربة ربط Webhook n8n مع المنظومة",
            estimated_cost = "1500",
            technician = "hazem",
            status = "received",
            detailed_status = "received",
            received_by_employee = "تجربة النظام"
        )
        sendEvent(
            config = config,
            eventType = "test_connection",
            device = testDevice,
            extraData = mapOf("test" to true, "message" to "رسالة اختبار اتصال ناجحة من تطبيق المتحدة للصيانة")
        )
    }
}
