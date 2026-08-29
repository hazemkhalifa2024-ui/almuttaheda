package com.example.data.backup

import com.example.data.model.Device
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val appName: String,
    val version: Int,
    val exportedAt: String,
    val deviceCount: Int
)

data class ParsedBackup(
    val metadata: BackupMetadata,
    val devices: List<Device>
)

object JsonBackupManager {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
    private val displayDateFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))

    fun exportToJson(devices: List<Device>): String {
        val root = JSONObject()
        val now = Date()
        
        root.put("app", "المتحدة للصيانة - Mottaheda Maintenance")
        root.put("version", 1)
        root.put("exported_at", isoFormat.format(now))
        root.put("exported_at_formatted", displayDateFormat.format(now))
        root.put("total_devices", devices.size)

        val devicesArray = JSONArray()
        for (dev in devices) {
            val devObj = JSONObject()
            devObj.put("id", dev.id)
            devObj.put("customer_name", dev.customer_name)
            devObj.put("customer_phone", dev.customer_phone ?: "")
            devObj.put("device_name", dev.device_name)
            devObj.put("issue_description", dev.issue_description ?: "")
            devObj.put("estimated_cost", dev.estimated_cost ?: "0")
            devObj.put("technician", dev.technician ?: "tech1")
            devObj.put("status", dev.status)
            devObj.put("created_at", dev.created_at ?: isoFormat.format(now))
            devicesArray.put(devObj)
        }

        root.put("devices", devicesArray)
        return root.toString(2)
    }

    fun parseJsonBackup(jsonString: String): Result<ParsedBackup> {
        return try {
            val cleanJson = jsonString.trim()
            val parsedDevices = mutableListOf<Device>()
            var appName = "المتحدة للصيانة"
            var version = 1
            var exportedAt = isoFormat.format(Date())

            if (cleanJson.startsWith("{")) {
                val root = JSONObject(cleanJson)
                appName = root.optString("app", "المتحدة للصيانة")
                version = root.optInt("version", 1)
                exportedAt = root.optString("exported_at", root.optString("exported_at_formatted", isoFormat.format(Date())))

                val devicesArray = root.optJSONArray("devices") ?: root.optJSONArray("data")
                if (devicesArray != null) {
                    for (i in 0 until devicesArray.length()) {
                        val obj = devicesArray.optJSONObject(i) ?: continue
                        val dev = parseDeviceObject(obj, i.toLong() + 1)
                        parsedDevices.add(dev)
                    }
                }
            } else if (cleanJson.startsWith("[")) {
                val array = JSONArray(cleanJson)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val dev = parseDeviceObject(obj, i.toLong() + 1)
                    parsedDevices.add(dev)
                }
            } else {
                return Result.failure(IllegalArgumentException("تنسيق JSON غير صالح، يجب أن يبدأ بـ { أو ["))
            }

            if (parsedDevices.isEmpty()) {
                return Result.failure(IllegalArgumentException("لم يتم العثور على أي أجهزة مسجلة في ملف النسخة الاحتياطية"))
            }

            val metadata = BackupMetadata(
                appName = appName,
                version = version,
                exportedAt = exportedAt,
                deviceCount = parsedDevices.size
            )

            Result.success(ParsedBackup(metadata, parsedDevices))
        } catch (e: Exception) {
            Result.failure(Exception("خطأ في قراءة ملف النسخة الاحتياطية: ${e.localizedMessage}"))
        }
    }

    private fun parseDeviceObject(obj: JSONObject, fallbackId: Long): Device {
        val id = obj.optLong("id", fallbackId)
        val customerName = obj.optString("customer_name", obj.optString("customerName", "عميل"))
        val customerPhone = obj.optString("customer_phone", obj.optString("customerPhone", ""))
        val deviceName = obj.optString("device_name", obj.optString("deviceName", "هاتف"))
        val issueDesc = obj.optString("issue_description", obj.optString("issueDescription", ""))
        val cost = obj.optString("estimated_cost", obj.optString("estimatedCost", "0"))
        val tech = obj.optString("technician", "tech1")
        val status = obj.optString("status", "received")
        val createdAt = obj.optString("created_at", obj.optString("createdAt", null))

        return Device(
            id = id,
            customer_name = customerName,
            customer_phone = customerPhone.ifBlank { null },
            device_name = deviceName,
            issue_description = issueDesc.ifBlank { null },
            estimated_cost = cost,
            technician = tech,
            status = status,
            created_at = createdAt
        )
    }
}
