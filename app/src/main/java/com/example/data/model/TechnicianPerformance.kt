package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TechnicianMetrics(
    val username: String,
    val displayName: String,
    val specialty: String = "صيانة عامة",
    val totalAssigned: Int = 0,
    val inProgressCount: Int = 0,
    val diagnosingCount: Int = 0,
    val waitingPartsCount: Int = 0,
    val readyCount: Int = 0,
    val deliveredCount: Int = 0,
    val unrepairableCount: Int = 0,
    val completedToday: Int = 0,
    val totalRevenue: Double = 0.0,
    val partsConsumedCount: Int = 0,
    val efficiencyScore: Int = 95,
    val averageTurnaroundHours: Double = 3.5,
    val rating: Float = 4.8f
)

@JsonClass(generateAdapter = true)
data class TechnicianNotification(
    val id: Long = 0,
    val technicianUsername: String,
    val deviceId: Long,
    val deviceTicket: String,
    val deviceName: String,
    val customerName: String,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val timestamp: String
)
