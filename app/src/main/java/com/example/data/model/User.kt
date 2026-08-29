package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "username") val username: String = "",
    @Json(name = "password") val password: String = "",
    @Json(name = "role") val role: String = "technician",
    @Json(name = "permissions") val permissions: String? = null
) {
    fun parsePermissions(): List<String> {
        if (role == "admin") {
            return listOf("dashboard", "new", "delivery", "management", "permissions", "printer", "settings")
        }
        if (permissions.isNullOrBlank()) {
            return listOf("management")
        }
        return try {
            val clean = permissions.trim().removeSurrounding("[", "]").replace("\"", "")
            if (clean.isBlank()) listOf("management") else clean.split(",").map { it.trim() }
        } catch (e: Exception) {
            listOf("management")
        }
    }
}

data class SessionUser(
    val username: String,
    val role: String,
    val displayName: String,
    val allowedScreens: List<String>
)
