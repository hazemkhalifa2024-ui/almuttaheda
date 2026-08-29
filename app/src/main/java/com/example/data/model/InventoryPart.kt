package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InventoryPart(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "barcode") val barcode: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "category") val category: String = "قطع غيار",
    @Json(name = "device_model") val device_model: String = "عام",
    @Json(name = "current_stock") val current_stock: Int = 0,
    @Json(name = "unit_cost") val unit_cost: Double = 0.0,
    @Json(name = "selling_price") val selling_price: Double = 0.0,
    @Json(name = "min_alert_stock") val min_alert_stock: Int = 2
)

enum class PartMovementType(val code: String, val arabicLabel: String) {
    INWARD_WAREHOUSE("inward_warehouse", "وارد من المخزن الرئيسي"),
    INWARD_SUPPLIER("inward_supplier", "وارد من مورد خارجي"),
    CONSUMED_REPAIR("consumed_repair", "منصرف ومستهلك في صيانة جهاز"),
    RETURN_WAREHOUSE("return_warehouse", "رد زيادة للمخزن (نهاية اليوم)"),
    DEFECTIVE_DAMAGED("defective_damaged", "توالف وعيوب صيانة"),
    RETURN_SUPPLIER("return_supplier", "مرتجع للمورد")
}

@JsonClass(generateAdapter = true)
data class PartMovementLog(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "barcode") val barcode: String = "",
    @Json(name = "part_name") val part_name: String = "",
    @Json(name = "type") val type: String = "inward_warehouse",
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "device_id") val device_id: Long? = null,
    @Json(name = "device_ticket") val device_ticket: String? = null,
    @Json(name = "source_destination") val source_destination: String = "المخزن الرئيسي",
    @Json(name = "employee_name") val employee_name: String = "موظف الاستلام",
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "timestamp") val timestamp: String = ""
) {
    val typeArabicLabel: String
        get() = when (type) {
            "inward_warehouse" -> "وارد من المخزن"
            "inward_supplier" -> "وارد من مورد"
            "consumed_repair" -> "منصرف لصيانة جهاز"
            "return_warehouse" -> "رد زيادة للمخزن"
            "defective_damaged" -> "توالف وعيوب"
            "return_supplier" -> "مرتجع للمورد"
            else -> type
        }
}

@JsonClass(generateAdapter = true)
data class PartUsageItem(
    @Json(name = "barcode") val barcode: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "cost") val cost: Double = 0.0,
    @Json(name = "used_by_technician") val used_by_technician: String = "",
    @Json(name = "timestamp") val timestamp: String = ""
)
