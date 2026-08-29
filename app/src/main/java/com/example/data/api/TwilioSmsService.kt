package com.example.data.api

import android.util.Base64
import android.util.Log
import com.example.data.model.SmsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TwilioSmsService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun sendSms(
        config: SmsConfig,
        toPhone: String,
        messageBody: String
    ): SmsSendResult = withContext(Dispatchers.IO) {
        if (!config.enabled) {
            return@withContext SmsSendResult(
                isSuccess = false,
                errorMessage = "خدمة الرسائل غير مفعلة في الإعدادات",
                isSimulated = false
            )
        }

        val sid = config.accountSid.trim()
        val token = config.authToken.trim()
        val from = config.fromNumber.trim()

        // Normalize Phone number for international delivery if needed (e.g., Egypt +20)
        val formattedTo = formatPhoneNumber(toPhone.trim())
        if (formattedTo.isBlank()) {
            return@withContext SmsSendResult(
                isSuccess = false,
                errorMessage = "رقم هاتف العميل غير صالح",
                isSimulated = false
            )
        }

        // If credentials are empty, return simulated success with info
        if (sid.isBlank() || token.isBlank() || from.isBlank()) {
            Log.d("TwilioSmsService", "Twilio credentials empty, simulating SMS to $formattedTo: $messageBody")
            return@withContext SmsSendResult(
                isSuccess = true,
                errorMessage = null,
                isSimulated = true,
                previewMessage = messageBody
            )
        }

        try {
            val url = "https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json"
            val credentials = "$sid:$token"
            val authHeader = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

            val formBody = FormBody.Builder()
                .add("To", formattedTo)
                .add("From", from)
                .add("Body", messageBody)
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Log.d("TwilioSmsService", "SMS sent successfully: $responseBody")
                SmsSendResult(isSuccess = true, isSimulated = false)
            } else {
                Log.e("TwilioSmsService", "Failed to send SMS: HTTP ${response.code} - $responseBody")
                SmsSendResult(
                    isSuccess = false,
                    errorMessage = "خطأ في إرسال الرسالة من Twilio (${response.code})",
                    isSimulated = false
                )
            }
        } catch (e: Exception) {
            Log.e("TwilioSmsService", "Exception sending SMS", e)
            SmsSendResult(
                isSuccess = false,
                errorMessage = "تعذر إرسال SMS: ${e.localizedMessage ?: "خطأ في الشبكة"}",
                isSimulated = false
            )
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        var clean = phone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        if (clean.startsWith("+")) {
            return clean
        }
        if (clean.startsWith("00")) {
            return "+" + clean.substring(2)
        }
        // If Egyptian local number starting with 01
        if (clean.startsWith("01") && clean.length == 11) {
            return "+20" + clean.substring(1)
        }
        if (clean.startsWith("1") && clean.length == 10) {
            return "+20$clean"
        }
        return if (clean.isNotBlank()) "+$clean" else ""
    }
}

data class SmsSendResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val isSimulated: Boolean = false,
    val previewMessage: String? = null
)
