package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "username") val username: String = "",
    @Json(name = "password") val password: String = "",
    @Json(name = "role") val role: String = "technician",
    @Json(name = "permissions") val permissions: String? = null,
    @Json(name = "shop_id") val shopId: String? = "default_shop",
    @Json(name = "device_id") val deviceId: String? = null
) {
    fun parsePermissions(): List<String> {
        if (role == "super_admin") {
            return listOf("saas_management", "dashboard", "new", "delivery", "management", "permissions", "printer", "settings")
        }
        if (!permissions.isNullOrBlank()) {
            val list = try {
                val clean = permissions.trim().removeSurrounding("[", "]").replace("\"", "")
                if (clean.isBlank()) emptyList() else clean.split(",").map { it.trim() }
            } catch (e: Exception) {
                emptyList()
            }
            if (list.isNotEmpty()) return list
        }
        if (role == "admin") {
            return listOf("dashboard", "new", "delivery", "management", "customer_history", "technician_workspace", "technician_performance", "inventory_barcode", "inventory_reports", "permissions", "user_management", "printer", "settings", "server_monitor")
        }
        if (role == "reception") {
            return listOf("dashboard", "new", "delivery", "management", "customer_history", "printer")
        }
        if (role == "technician") {
            return listOf("technician_workspace", "management")
        }
        return listOf("management")
    }
}

data class SessionUser(
    val username: String,
    val role: String,
    val displayName: String,
    val allowedScreens: List<String>,
    val shopId: String = "default_shop",
    val shopConfig: ShopConfig = ShopConfig()
)
