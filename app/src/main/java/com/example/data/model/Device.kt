package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Device(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "customer_name") val customer_name: String = "",
    @Json(name = "customer_phone") val customer_phone: String? = "",
    @Json(name = "device_name") val device_name: String = "",
    @Json(name = "issue_description") val issue_description: String? = "",
    @Json(name = "estimated_cost") val estimated_cost: String? = "0",
    @Json(name = "down_payment") val down_payment: Double = 0.0,
    @Json(name = "technician") val technician: String? = "tech1",
    @Json(name = "status") val status: String = "received",
    @Json(name = "created_at") val created_at: String? = null,
    @Json(name = "received_by_employee") val received_by_employee: String? = "موظف الاستقبال",
    @Json(name = "delivered_by_employee") val delivered_by_employee: String? = null,
    @Json(name = "detailed_status") val detailed_status: String? = "received",
    @Json(name = "technician_notes") val technician_notes: String? = null,
    @Json(name = "parts_used_summary") val parts_used_summary: String? = null,
    @Json(name = "delivered_at") val delivered_at: String? = null,
    @Json(name = "diagnosed_at") val diagnosed_at: String? = null,
    @Json(name = "photo_url") val photo_url: String? = null,
    @Json(name = "due_date") val due_date: String? = null,
    @Json(name = "shop_id") val shopId: String? = null
) {
    val ticketNumber: String
        get() = "MUT-$id"

    val normalizedCost: Double
        get() = estimated_cost?.toDoubleOrNull() ?: 0.0

    val remaining_balance: Double
        get() = (normalizedCost - down_payment).coerceAtLeast(0.0)

    val statusArabicLabel: String
        get() = when (status.lowercase()) {
            "received", "تم الاستلام" -> "تم الاستلام"
            "diagnosing", "جاري الفحص والتشخيص" -> "جاري الفحص والتشخيص"
            "waiting_parts", "في انتظار قطع الغيار" -> "في انتظار قطع الغيار"
            "in_progress", "جاري الفحص", "جارى الإصلاح", "جاري الإصلاح" -> "جاري الإصلاح"
            "quality_check", "في انتظار اختبار الجودة" -> "في انتظار اختبار الجودة"
            "ready", "جاهز للتسليم" -> "جاهز للتسليم"
            "delivered", "تم التسليم" -> "تم التسليم"
            "unrepairable", "تعذر الإصلاح / ملغي" -> "تعذر الإصلاح / ملغي"
            else -> status
        }
}

