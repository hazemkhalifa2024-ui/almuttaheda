package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val PREFS_NAME = "server_connection_prefs"
    private const val KEY_SERVER_URL = "custom_server_url"
    private const val KEY_ANON_KEY = "custom_anon_key"
    private const val KEY_BASIC_USER = "custom_basic_user"
    private const val KEY_BASIC_PASS = "custom_basic_pass"

    const val DEFAULT_SERVER_URL = "http://179.198.203.86:8000/rest/v1/"
    const val DEFAULT_ANON_KEY = "30a3cd6d27c3db8b492d0ca4df87fe039a6b9d2fb940cb0f0231eb03bdc1db74"
    const val DEFAULT_SERVICE_ROLE_KEY = "ba1af342ef32441936564abe8124019ace1e51d7199deb9609304298a10fbafa"
    const val DEFAULT_SERVICE_JWT = "eyJhbGciOiAiSFMyNTYiLCAidHlwIjogIkpXVCJ9.eyJyb2xlIjogInNlcnZpY2Vfcm9sZSIsICJpc3MiOiAic3VwYWJhc2UiLCAiaWF0IjogMTYwMDAwMDAwMCwgImV4cCI6IDI1MDAwMDAwMDB9.9kBf4OWXi-_TumOln81l3whxZyG1mXRMn1RGCBIDtJ4"

    @Volatile
    private var currentUrl: String = DEFAULT_SERVER_URL
    @Volatile
    private var currentAnonKey: String = DEFAULT_SERVICE_ROLE_KEY
    @Volatile
    private var currentBasicUser: String = ""
    @Volatile
    private var currentBasicPass: String = ""

    private var sharedPrefs: SharedPreferences? = null

    fun init(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs?.let { prefs ->
            var savedUrl = prefs.getString(KEY_SERVER_URL, null)
            if (!savedUrl.isNullOrBlank()) {
                savedUrl = savedUrl.trim()
                if (savedUrl.startsWith("/")) savedUrl = savedUrl.substring(1)
                if (!savedUrl.endsWith("/")) savedUrl = "$savedUrl/"
                currentUrl = savedUrl
            } else {
                currentUrl = DEFAULT_SERVER_URL
            }

            val savedKey = prefs.getString(KEY_ANON_KEY, null)
            currentAnonKey = if (!savedKey.isNullOrBlank() && savedKey != "YOUR_SELF_HOSTED_ANON_KEY") {
                savedKey.trim()
            } else {
                DEFAULT_SERVICE_ROLE_KEY
            }

            currentBasicUser = prefs.getString(KEY_BASIC_USER, "") ?: ""
            currentBasicPass = prefs.getString(KEY_BASIC_PASS, "") ?: ""
        }
        rebuildService()
    }

    fun getServerUrl(): String = currentUrl
    fun getAnonKey(): String = currentAnonKey
    fun getBasicUser(): String = currentBasicUser
    fun getBasicPass(): String = currentBasicPass

    fun updateConfig(context: Context, url: String, anonKey: String, basicUser: String, basicPass: String) {
        var cleanUrl = url.trim()
        if (cleanUrl.isNotBlank()) {
            if (cleanUrl.startsWith("/")) cleanUrl = cleanUrl.substring(1)
            if (!cleanUrl.endsWith("/")) cleanUrl = "$cleanUrl/"
            currentUrl = cleanUrl
        }
        val cleanKey = anonKey.trim()
        currentAnonKey = if (cleanKey.isNotBlank() && cleanKey != "YOUR_SELF_HOSTED_ANON_KEY") {
            cleanKey
        } else {
            DEFAULT_SERVICE_ROLE_KEY
        }
        currentBasicUser = basicUser.trim()
        currentBasicPass = basicPass.trim()

        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs?.edit()
            ?.putString(KEY_SERVER_URL, currentUrl)
            ?.putString(KEY_ANON_KEY, currentAnonKey)
            ?.putString(KEY_BASIC_USER, currentBasicUser)
            ?.putString(KEY_BASIC_PASS, currentBasicPass)
            ?.apply()

        rebuildService()
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // 1. Authorization header:
        if (currentBasicUser.isNotBlank() && currentBasicPass.isNotBlank()) {
            val basicCredentials = Credentials.basic(currentBasicUser, currentBasicPass)
            requestBuilder.header("Authorization", basicCredentials)
        } else {
            // PostgREST requires a valid 3-part JWT in the Bearer token
            val bearerToken = if (currentAnonKey.count { it == '.' } == 2) {
                currentAnonKey
            } else {
                DEFAULT_SERVICE_JWT
            }
            requestBuilder.header("Authorization", "Bearer $bearerToken")
        }

        // 2. Apikey header (Envoy gateway requirement)
        val apiKeyToSend = if (currentAnonKey.isNotBlank() && currentAnonKey != "YOUR_SELF_HOSTED_ANON_KEY") {
            currentAnonKey
        } else {
            DEFAULT_SERVICE_ROLE_KEY
        }
        requestBuilder.header("apikey", apiKeyToSend)

        requestBuilder.header("Content-Type", "application/json")
        requestBuilder.header("Prefer", "return=representation")
        requestBuilder.method(original.method, original.body)
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Volatile
    private var _apiService: SupabaseApiService? = null

    val apiService: SupabaseApiService
        get() {
            if (_apiService == null) {
                synchronized(this) {
                    if (_apiService == null) {
                        _apiService = createRetrofitService()
                    }
                }
            }
            return _apiService!!
        }

    private fun rebuildService() {
        synchronized(this) {
            _apiService = createRetrofitService()
        }
    }

    private fun createRetrofitService(): SupabaseApiService {
        val safeUrl = if (currentUrl.endsWith("/")) currentUrl else "$currentUrl/"
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApiService::class.java)
    }

    suspend fun testConnection(
        testUrl: String,
        testKey: String,
        basicUser: String,
        basicPass: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            var url = testUrl.trim()
            if (url.startsWith("/")) url = url.substring(1)
            if (!url.endsWith("/")) url = "$url/"
            val targetEndpoint = "${url}Shops?select=id&limit=1"

            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()

            val requestBuilder = Request.Builder().url(targetEndpoint)
            if (basicUser.isNotBlank() && basicPass.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(basicUser.trim(), basicPass.trim()))
            } else {
                val bearerToken = if (testKey.count { it == '.' } == 2) {
                    testKey.trim()
                } else {
                    DEFAULT_SERVICE_JWT
                }
                requestBuilder.header("Authorization", "Bearer $bearerToken")
            }

            val apiKeyToSend = if (testKey.isNotBlank() && testKey != "YOUR_SELF_HOSTED_ANON_KEY") {
                testKey.trim()
            } else {
                DEFAULT_SERVICE_ROLE_KEY
            }
            requestBuilder.header("apikey", apiKeyToSend)

            val response = client.newCall(requestBuilder.build()).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success("تم الاتصال بالسيرفر وقاعدة البيانات بنجاح! كود الاستجابة: $code")
            } else if (code == 401) {
                Result.failure(Exception("خطأ في المصادقة (401 Unauthorized): يرجى مراجعة إعدادات السيرفر أو مفتاح API Key."))
            } else if (code == 404) {
                Result.failure(Exception("الرابط غير صحيح أو جدول Shops غير موجود (404 Not Found)."))
            } else {
                Result.failure(Exception("رد السيرفر بكود $code: ${body.take(100)}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الوصول للسيرفر: ${e.localizedMessage}"))
        }
    }
}

