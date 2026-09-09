package com.example.data.model

import com.squareup.moshi.JsonClass

enum class WhatsAppSendMethod(val label: String) {
    BOTH("إرسال عبر السيرفر مع بديل واتساب المباشر"),
    WEBHOOK_ONLY("عبر خادم n8n / Baileys فقط"),
    APP_DIRECT("مباشرة عبر تطبيق واتساب الرسمي")
}

@JsonClass(generateAdapter = true)
data class WhatsAppConfig(
    val enabled: Boolean = true,
    val webhookUrl: String = "",
    val apiToken: String = "",
    val sendMethod: WhatsAppSendMethod = WhatsAppSendMethod.BOTH,
    val senderPhoneNumber: String = "",
    val autoSendOnStatusChange: Boolean = false
)

@JsonClass(generateAdapter = true)
data class WhatsAppTemplate(
    val id: String,
    val title: String,
    val category: String, // "received", "estimated", "ready", "delivered", "delayed", "custom"
    val content: String,
    val isDefault: Boolean = false
) {
    fun render(
        customerName: String,
        deviceName: String,
        ticketNumber: String,
        issue: String,
        cost: String,
        downPayment: String,
        remaining: String,
        status: String,
        shopName: String,
        shopPhone: String
    ): String {
        return content
            .replace("{customer_name}", customerName)
            .replace("{device_name}", deviceName)
            .replace("{ticket_number}", ticketNumber)
            .replace("{issue}", issue.ifBlank { "فحص عام" })
            .replace("{cost}", cost.ifBlank { "0" })
            .replace("{down_payment}", downPayment.ifBlank { "0" })
            .replace("{remaining}", remaining.ifBlank { "0" })
            .replace("{status}", status)
            .replace("{shop_name}", shopName)
            .replace("{shop_phone}", shopPhone)
    }

    companion object {
        val DEFAULT_TEMPLATES = listOf(
            WhatsAppTemplate(
                id = "received",
                title = "استلام جهاز جديد",
                category = "received",
                content = "مرحباً يا {customer_name}، تم استلام جهازك ({device_name}) في {shop_name} بنجاح.\nرقم إيصال الصيانة: #{ticket_number}\nالعطل المذكور: {issue}\nسنقوم بفحص الجهاز وإبلاغك بالتقرير والتكلفة قريباً.\nللاستفسار: {shop_phone}",
                isDefault = true
            ),
            WhatsAppTemplate(
                id = "estimated",
                title = "تم الفحص وتحديد التكلفة",
                category = "estimated",
                content = "أهلاً يا {customer_name}، تم الانتهاء من فحص جهازك ({device_name}) رقم #{ticket_number}.\nبيان العطل: {issue}\nالتكلفة التقديرية للإصلاح: {cost} ج.م.\nيرجى الرد بالموافقة أو الاتصال بنا للبدء في الصيانة: {shop_phone}",
                isDefault = true
            ),
            WhatsAppTemplate(
                id = "ready",
                title = "تم الإصلاح وجاهز للتسليم",
                category = "ready",
                content = "بشرى سارة يا {customer_name}! تم إصلاح جهازك ({device_name}) بنجاح وبأعلى جودة.\nرقم الإيصال: #{ticket_number}\nالمبلغ المتبقي للتحصيل: {remaining} ج.م.\nالجهاز بانتظار استلامك في {shop_name} تشرفنا في أي وقت!\nالعنوان والتواصل: {shop_phone}",
                isDefault = true
            ),
            WhatsAppTemplate(
                id = "delivered",
                title = "تم التسليم وتفعيل الضمان",
                category = "delivered",
                content = "شكراً لزيارتكم يا {customer_name}!\nتم تسليم جهازك ({device_name}) رقم #{ticket_number} مع شهادة ضمان معتمدة من {shop_name}.\nيسعدنا دائماً خدمتكم وتقييمكم لخدمتنا. يومكم سعيد!",
                isDefault = true
            ),
            WhatsAppTemplate(
                id = "delayed",
                title = "تحديث حالة / انتظار قطع غيار",
                category = "delayed",
                content = "مرحباً {customer_name}، نود إحاطتك علماً بخصوص جهازك ({device_name}) رقم #{ticket_number} أنه قيد المتابعة وبانتظار وصول قطعة الغيار الأصلية المطلوبة لضمان أفضل نتيجة صيانة.\nشاكرين سعة صدركم وسنوافيكم بالانتهاء قريباً.",
                isDefault = true
            ),
            WhatsAppTemplate(
                id = "custom",
                title = "رسالة عامة سريعة",
                category = "custom",
                content = "مرحباً {customer_name}، بخصوص جهازك ({device_name}) رقم #{ticket_number} في مركز {shop_name}...\nللتواصل: {shop_phone}",
                isDefault = true
            )
        )
    }
}
