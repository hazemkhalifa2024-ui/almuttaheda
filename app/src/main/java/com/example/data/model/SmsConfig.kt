package com.example.data.model

data class SmsConfig(
    val enabled: Boolean = true,
    val accountSid: String = "",
    val authToken: String = "",
    val fromNumber: String = "",
    val autoSendOnReady: Boolean = true,
    val autoSendOnDelay: Boolean = true,
    val readyTemplate: String = "مرحباً {name}، جهازك ({device}) تذكرة {ticket} جاهز للاستلام من ورشة {shop}. المبلغ: {cost} ج.م. هاتف: {phone}",
    val delayTemplate: String = "مرحباً {name}، نعتذر عن التأخير في صيانة جهازك ({device}) تذكرة {ticket} بسبب: {reason}. نعمل على إنهائه قريباً. ورشة {shop}"
)

data class SmsLog(
    val id: String = System.currentTimeMillis().toString(),
    val recipientPhone: String,
    val customerName: String,
    val ticketNumber: String,
    val message: String,
    val status: String, // "success", "failed", "simulated"
    val timestamp: Long = System.currentTimeMillis(),
    val type: String // "ready", "delay", "custom"
)
