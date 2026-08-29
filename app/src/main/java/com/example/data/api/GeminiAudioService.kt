package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class VoiceParsedData(
    @Json(name = "customer_name") val customerName: String = "",
    @Json(name = "customer_phone") val customerPhone: String = "",
    @Json(name = "device_name") val deviceName: String = "",
    @Json(name = "issue_description") val issueDescription: String = "",
    @Json(name = "estimated_cost") val estimatedCost: String = "",
    @Json(name = "technician") val technician: String = "tech1",
    @Json(name = "full_transcription") val fullTranscription: String = ""
)

class GeminiAudioService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val parsedAdapter = moshi.adapter(VoiceParsedData::class.java)

    /**
     * Sends recorded audio directly to gemini-3.5-flash to transcribe Arabic speech and
     * extract structured repair ticket fields.
     */
    suspend fun transcribeAudioWithGemini(
        audioBase64: String,
        mimeType: String = "audio/mp4"
    ): Result<VoiceParsedData> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("مفتاح Gemini API غير مهيأ. الرجاء إضافة المفتاح في لوحة أسرار AI Studio.")
            )
        }

        val prompt = """
            أنت مساعد ذكي مخصص لورشة صيانة الهواتف (المتحدة للصيانة).
            استمع للتسجيل الصوتي المرفق باللغة العربية بدقة، وقم بتفريغه نصياً واستخراج البيانات التالية بصيغة JSON حصرياً بدون أي مقدمات أو علامات markdown إضافية:
            - customer_name: اسم العميل (مثال: أحمد علي)
            - customer_phone: رقم هاتف العميل (مثال: 01012345678)
            - device_name: نوع وموديل الجهاز (مثال: iPhone 13 Pro Max أو Samsung S22)
            - issue_description: وصف العطل بالتفصيل (مثال: شاشة مكسورة والجهاز لا يستجيب للشحن)
            - estimated_cost: التكلفة التقديرية بالأرقام فقط إن ذُكرت (مثال: 1500)
            - technician: الفني المقترح (مثال: tech1 أو tech2 أو عام)
            - full_transcription: النص الكامل للتفريغ الصوتي بدقة
        """.trimIndent()

        try {
            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Text instruction part
            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)

            // Audio inline data part
            val audioPart = JSONObject()
            val inlineData = JSONObject()
            inlineData.put("mimeType", mimeType)
            inlineData.put("data", audioBase64)
            audioPart.put("inlineData", inlineData)
            partsArray.put(audioPart)

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // Generation config with JSON response format
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.2)
            val respFormat = JSONObject()
            respFormat.put("mimeType", "application/json")
            genConfig.put("responseFormat", respFormat)
            rootJson.put("generationConfig", genConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiAudioService", "Gemini API error: ${response.code} - $responseBody")
                return@withContext Result.failure(Exception("خطأ في الاتصال بـ Gemini: ${response.code}"))
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val candidateContent = firstCandidate?.optJSONObject("content")
            val candParts = candidateContent?.optJSONArray("parts")
            val textResult = candParts?.optJSONObject(0)?.optString("text") ?: ""

            val cleanedJson = cleanJsonString(textResult)
            val parsed = parsedAdapter.fromJson(cleanedJson) ?: VoiceParsedData(fullTranscription = textResult)
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e("GeminiAudioService", "Audio parsing exception", e)
            Result.failure(Exception("تعذر معالجة الصوت: ${e.localizedMessage}"))
        }
    }

    /**
     * Parses spoken text (captured via speech recognizer or voice dictation) using gemini-3.5-flash
     * into structured repair ticket fields.
     */
    suspend fun parseSpokenTextWithGemini(spokenText: String): Result<VoiceParsedData> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (spokenText.isBlank()) {
            return@withContext Result.failure(Exception("النص الصوتي فارغ"))
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local fallback extraction if API key is not yet set
            val fallback = localExtractFields(spokenText)
            return@withContext Result.success(fallback)
        }

        val prompt = """
            أنت ذكاء اصطناعي متخصص في ورشة صيانة الهواتف.
            قم بتحليل النص المنطوق التالي واستخرج الحقول التالية بصيغة JSON فقط:
            النص المنطوق: "$spokenText"

            الحقول المطلوبة:
            {
              "customer_name": "اسم العميل",
              "customer_phone": "رقم الهاتف بدون مسافات",
              "device_name": "اسم وموديل الجهاز بدقة مثل iPhone 11 Pro أو Redmi Note 10",
              "issue_description": "تفاصيل العطل والشكوى",
              "estimated_cost": "التكلفة التقديرية بالأرقام فقط",
              "technician": "اسم الفني أو tech1",
              "full_transcription": "$spokenText"
            }
        """.trimIndent()

        try {
            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            val textPart = JSONObject()
            textPart.put("text", prompt)
            partsArray.put(textPart)

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            val genConfig = JSONObject()
            genConfig.put("temperature", 0.1)
            val respFormat = JSONObject()
            respFormat.put("mimeType", "application/json")
            genConfig.put("responseFormat", respFormat)
            rootJson.put("generationConfig", genConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Fallback to local heuristic extraction
                val localParsed = localExtractFields(spokenText)
                return@withContext Result.success(localParsed)
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val candidateContent = firstCandidate?.optJSONObject("content")
            val candParts = candidateContent?.optJSONArray("parts")
            val textResult = candParts?.optJSONObject(0)?.optString("text") ?: ""

            val cleaned = cleanJsonString(textResult)
            val parsed = parsedAdapter.fromJson(cleaned) ?: localExtractFields(spokenText)
            Result.success(parsed)
        } catch (e: Exception) {
            val localParsed = localExtractFields(spokenText)
            Result.success(localParsed)
        }
    }

    private fun cleanJsonString(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun localExtractFields(text: String): VoiceParsedData {
        var phone = ""
        val phoneRegex = Regex("""(\+?\d{10,14}|01\d{9})""")
        val phoneMatch = phoneRegex.find(text)
        if (phoneMatch != null) {
            phone = phoneMatch.value
        }

        var cost = ""
        val costRegex = Regex("""(\d+)\s*(جنيه|جنية|ج|ريال|دولار|ليرة|درهم)?""")
        val costMatch = costRegex.find(text)
        if (costMatch != null) {
            val num = costMatch.groupValues[1]
            if (num != phone) {
                cost = num
            }
        }

        return VoiceParsedData(
            customerName = "",
            customerPhone = phone,
            deviceName = "",
            issueDescription = text,
            estimatedCost = cost,
            technician = "tech1",
            fullTranscription = text
        )
    }
}
