package com.example.data.model

data class CustomerProfile(
    val phone: String,
    val name: String,
    val totalRepairs: Int,
    val completedRepairs: Int,
    val inProgressRepairs: Int,
    val totalSpent: Double,
    val lastVisitDate: String?,
    val devices: List<Device>
)
