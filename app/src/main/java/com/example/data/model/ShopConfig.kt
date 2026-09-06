package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShopConfig(
    @Json(name = "id") val id: String = "default_shop",
    @Json(name = "name") val name: String = "المتحدة للصيانة",
    @Json(name = "logo_url") val logoUrl: String? = null,
    @Json(name = "address") val address: String? = "العنوان الافتراضي",
    @Json(name = "phone") val phone: String? = "01000000000",
    @Json(name = "receipt_footer") val receiptFooter: String? = "شكراً لزيارتكم - ضمان شهر ضد عيوب الصيانة",
    @Json(name = "sticker_layout") val stickerLayout: String? = "default", // e.g. "default", "compact"
    @Json(name = "subscription_status") val subscriptionStatus: String = "active", // "active", "expired"
    @Json(name = "subscription_expires_at") val subscriptionExpiresAt: String? = null, // "yyyy-MM-dd"
    @Json(name = "user_limit") val userLimit: Int = 10,
    @Json(name = "active_user_count") val activeUserCount: Int = 1
) {
    val isSubscriptionActive: Boolean
        get() {
            if (subscriptionStatus.lowercase() == "expired") return false
            if (subscriptionExpiresAt == null) return true
            return try {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val expiryDate = format.parse(subscriptionExpiresAt)
                val currentDate = java.util.Date()
                !currentDate.after(expiryDate)
            } catch (e: Exception) {
                true // fallback
            }
        }

    val isLimitExceeded: Boolean
        get() = activeUserCount > userLimit
}
