package com.example.data.model

data class N8nConfig(
    val enabled: Boolean = false,
    val webhookUrl: String = "",
    val authSecret: String = "",
    val triggerOnDeviceCreated: Boolean = true,
    val triggerOnStatusChanged: Boolean = true,
    val triggerOnDeviceReady: Boolean = true,
    val triggerOnDeviceDelivered: Boolean = true,
    val triggerOnInventoryAlert: Boolean = true
)
