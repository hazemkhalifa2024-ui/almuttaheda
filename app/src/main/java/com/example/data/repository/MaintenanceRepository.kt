package com.example.data.repository

import com.example.data.api.RetrofitClient
import com.example.data.model.Device
import com.example.data.model.InventoryPart
import com.example.data.model.PartMovementLog
import com.example.data.model.User
import com.example.data.model.ShopConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * MaintenanceRepository - Cloud Mirror Mode
 * Pure cloud-first repository that mirrors PostgreSQL / PostgREST server state in real time.
 */
class MaintenanceRepository(private val context: android.content.Context? = null) {
    private val api get() = RetrofitClient.apiService

    private val _shops = MutableStateFlow<List<ShopConfig>>(emptyList())
    val shops = _shops.asStateFlow()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private fun parseHttpError(code: Int, errorBody: String?): String {
        val snippet = errorBody?.take(250)?.trim() ?: ""
        return when (code) {
            401 -> "خطأ في المصادقة (401 Unauthorized): يرجى مراجعة إعدادات السيرفر أو مفتاح API Key / Basic Auth."
            404 -> "الجدول غير موجود في قاعدة بيانات السيرفر (404 Not Found): يرجى التأكد من تشغيل سكريبت إنشاء الجداول في PostgreSQL."
            400 -> "خطأ في بنية البيانات (400 Bad Request): $snippet"
            409 -> "البيانات مسجلة مسبقاً (409 Conflict): معرّف المحل أو اسم المستخدم موجود بالفعل."
            500 -> "خطأ داخلي في السيرفر (500 Internal Server Error): $snippet"
            else -> "استجابة السيرفر كود $code: $snippet"
        }
    }

    private fun getDeviceId(): String {
        return try {
            context?.let { ctx ->
                android.provider.Settings.Secure.getString(ctx.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            } ?: "DEVICE-ONLINE-MIRROR"
        } catch (e: Exception) {
            "DEVICE-ONLINE-MIRROR"
        }
    }

    // ==========================================
    // SHOPS (SAAS)
    // ==========================================

    suspend fun fetchAllShops(): Result<List<ShopConfig>> = withContext(Dispatchers.IO) {
        try {
            var response = api.getAllShops()
            if (response.code() == 404) {
                response = api.getAllShopsLower()
            }
            if (response.isSuccessful && response.body() != null) {
                val remoteList = response.body()!!
                _shops.value = remoteList
                Result.success(remoteList)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر الاتصال بالسيرفر لجلب قائمة المحلات: ${e.localizedMessage}"))
        }
    }

    suspend fun fetchShopConfig(shopId: String): Result<ShopConfig> = withContext(Dispatchers.IO) {
        try {
            var response = api.getShopConfig("eq.$shopId")
            if (response.code() == 404) {
                response = api.getShopConfigLower("eq.$shopId")
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val shop = response.body()!!.first()
                updateLocalShopState(shop)
                Result.success(shop)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر جلب إعدادات المحل من السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun createShop(
        id: String,
        name: String,
        address: String,
        phone: String,
        receiptFooter: String,
        subscriptionStatus: String,
        subscriptionExpiresAt: String,
        userLimit: Int
    ): Result<ShopConfig> = withContext(Dispatchers.IO) {
        val payload = mapOf<String, Any>(
            "id" to id,
            "name" to name,
            "address" to address,
            "phone" to phone,
            "receipt_footer" to receiptFooter,
            "subscription_status" to subscriptionStatus,
            "subscription_expires_at" to subscriptionExpiresAt,
            "user_limit" to userLimit,
            "active_user_count" to 1
        )
        try {
            var response = api.createShop(payload)
            if (response.code() == 404) {
                response = api.createShopLower(payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                _shops.value = _shops.value.filter { it.id != created.id } + created
                Result.success(created)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر تسجيل المحل على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun updateShop(
        id: String,
        updates: Map<String, Any>
    ): Result<ShopConfig> = withContext(Dispatchers.IO) {
        try {
            var response = api.updateShop("eq.$id", updates)
            if (response.code() == 404) {
                response = api.updateShopLower("eq.$id", updates)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val remoteUpdated = response.body()!!.first()
                updateLocalShopState(remoteUpdated)
                Result.success(remoteUpdated)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر تحديث المحل على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun deleteShop(shopId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var resp = api.deleteShop("eq.$shopId")
            if (resp.code() == 404) {
                resp = api.deleteShopLower("eq.$shopId")
            }
            if (resp.isSuccessful) {
                _shops.value = _shops.value.filter { it.id != shopId }
                Result.success(true)
            } else {
                val err = parseHttpError(resp.code(), resp.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر حذف المحل من السيرفر: ${e.localizedMessage}"))
        }
    }

    private fun updateLocalShopState(shop: ShopConfig) {
        val current = _shops.value.toMutableList()
        val index = current.indexOfFirst { it.id == shop.id }
        if (index >= 0) {
            current[index] = shop
        } else {
            current.add(0, shop)
        }
        _shops.value = current
    }

    // ==========================================
    // AUTH & USERS
    // ==========================================

    suspend fun login(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        val uName = username.trim()
        val uPass = password.trim()

        // 1. Superadmin instant bypass
        if (uName.lowercase() == "superadmin" && uPass == "super123") {
            val superadminUser = User(
                id = 9999,
                username = "superadmin",
                password = "",
                role = "super_admin",
                permissions = "[\"saas_management\",\"server_monitor\",\"dashboard\",\"management\",\"permissions\",\"user_management\",\"settings\"]",
                shopId = "super_admin"
            )
            return@withContext Result.success(superadminUser)
        }

        // 2. Direct online validation against PostgreSQL
        try {
            var response = api.loginUser("eq.$uName", "eq.$uPass")
            if (response.code() == 404) {
                response = api.loginUserLower("eq.$uName", "eq.$uPass")
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                var loggedInUser = response.body()!!.first()
                val currentDevice = getDeviceId()

                if (loggedInUser.role != "super_admin") {
                    val dbDeviceId = loggedInUser.deviceId
                    if (!dbDeviceId.isNullOrBlank()) {
                        if (dbDeviceId != currentDevice) {
                            return@withContext Result.failure(
                                Exception("عذراً، هذا الحساب مرتبط بجهاز آخر! لا يمكن الدخول إلا من الهاتف المسجل أو بالتواصل مع الإدارة لإلغاء القفل.")
                            )
                        }
                    } else {
                        try {
                            var updateResp = api.updateUser("eq.${loggedInUser.id}", mapOf("device_id" to currentDevice))
                            if (updateResp.code() == 404) {
                                updateResp = api.updateUserLower("eq.${loggedInUser.id}", mapOf("device_id" to currentDevice))
                            }
                            if (updateResp.isSuccessful && !updateResp.body().isNullOrEmpty()) {
                                loggedInUser = updateResp.body()!!.first()
                            }
                        } catch (e: Exception) {
                            // Non-fatal
                        }
                    }
                }

                _users.value = _users.value.filter { it.id != loggedInUser.id } + loggedInUser
                return@withContext Result.success(loggedInUser)
            } else if (response.isSuccessful) {
                // Check if the username exists to provide a specific error message
                val userExists = try {
                    var userCheckResp = api.getUserByUsername("eq.$uName")
                    if (userCheckResp.code() == 404) {
                        userCheckResp = api.getUserByUsernameLower("eq.$uName")
                    }
                    userCheckResp.isSuccessful && !userCheckResp.body().isNullOrEmpty()
                } catch (e: Exception) {
                    false
                }

                if (userExists) {
                    return@withContext Result.failure(Exception("كلمة المرور غير صحيحة! يرجى التأكد وإعادة المحاولة."))
                } else {
                    return@withContext Result.failure(Exception("اسم المستخدم ($uName) غير مسجل في النظام! يرجى التأكد من كتابة الاسم وبادئة المحل بشكل صحيح."))
                }
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                return@withContext Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("تعذر الاتصال بالسيرفر للتحقق من الدخول: ${e.localizedMessage ?: "تأكد من تشغيل السيرفر وصحة الرابط"}"))
        }
    }

    suspend fun fetchAllUsers(): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            var response = api.getAllUsers(null)
            if (response.code() == 404) {
                response = api.getAllUsersLower(null)
            }
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                _users.value = list
                Result.success(list)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر جلب كافة المستخدمين من السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun updateShopActiveUserCount(shopId: String, count: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var response = api.updateShop("eq.$shopId", mapOf("active_user_count" to count))
            if (response.code() == 404) {
                api.updateShopLower("eq.$shopId", mapOf("active_user_count" to count))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUsers(shopId: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            var response = api.getAllUsers("eq.$shopId")
            if (response.code() == 404) {
                response = api.getAllUsersLower("eq.$shopId")
            }
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                _users.value = list
                // Sync count to server
                try {
                    api.updateShop("eq.$shopId", mapOf("active_user_count" to list.size))
                } catch (ignored: Exception) {}
                Result.success(list)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر جلب مستخدمي الفرع من السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun createUser(
        username: String,
        password: String,
        role: String,
        permissions: String,
        shopId: String
    ): Result<User> = withContext(Dispatchers.IO) {
        // Strict limit check before creating
        try {
            var usersResp = api.getAllUsers("eq.$shopId")
            if (usersResp.code() == 404) usersResp = api.getAllUsersLower("eq.$shopId")
            val existingList = usersResp.body() ?: _users.value.filter { it.shopId == shopId }
            val currentCount = existingList.size

            var shopResp = api.getShopConfig("eq.$shopId")
            if (shopResp.code() == 404) shopResp = api.getShopConfigLower("eq.$shopId")
            val shop = shopResp.body()?.firstOrNull() ?: _shops.value.find { it.id == shopId }
            val limit = shop?.userLimit ?: 5

            if (currentCount >= limit) {
                return@withContext Result.failure(
                    Exception("تم الوصول إلى الحد الأقصى للمستخدمين المسموح به لهذا المحل ($currentCount من أصل $limit مستخدم). يرجى ترقية باقة المحل في شاشة إدارة المنصة أولاً.")
                )
            }
        } catch (e: Exception) {
            // If check fails due to network, proceed carefully or log
        }

        val payload = mapOf<String, Any>(
            "username" to username,
            "password" to password,
            "role" to role,
            "permissions" to permissions,
            "shop_id" to shopId
        )
        try {
            var response = api.createUser(payload)
            if (response.code() == 404) {
                response = api.createUserLower(payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                _users.value = _users.value.filter { it.id != created.id } + created
                // Sync new active user count
                val newCount = _users.value.count { it.shopId == shopId }
                try {
                    api.updateShop("eq.$shopId", mapOf("active_user_count" to newCount))
                } catch (ignored: Exception) {}
                Result.success(created)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر تسجيل المستخدم على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun updateUser(
        userId: Long,
        username: String,
        password: String,
        role: String,
        permissions: String?
    ): Result<User> = withContext(Dispatchers.IO) {
        val payload = mutableMapOf<String, Any>(
            "username" to username,
            "password" to password,
            "role" to role
        )
        if (permissions != null) {
            payload["permissions"] = permissions
        }
        try {
            var response = api.updateUser("eq.$userId", payload)
            if (response.code() == 404) {
                response = api.updateUserLower("eq.$userId", payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                _users.value = _users.value.map { if (it.id == userId) updated else it }
                Result.success(updated)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر تحديث بيانات المستخدم على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun updateUserPermissions(userId: Long, permissionsJson: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var response = api.updateUser("eq.$userId", mapOf("permissions" to permissionsJson))
            if (response.code() == 404) {
                response = api.updateUserLower("eq.$userId", mapOf("permissions" to permissionsJson))
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                _users.value = _users.value.map { if (it.id == userId) updated else it }
                Result.success(true)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر حفظ الصلاحيات على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun saveUserPermissions(userId: Long, permissionsJson: String): Result<Boolean> =
        updateUserPermissions(userId, permissionsJson)

    suspend fun resetUserDeviceId(userId: Long): Result<User> = withContext(Dispatchers.IO) {
        val payload = mapOf<String, Any>(
            "device_id" to ""
        )
        try {
            var response = api.updateUser("eq.$userId", payload)
            if (response.code() == 404) {
                response = api.updateUserLower("eq.$userId", payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                _users.value = _users.value.map { if (it.id == userId) updated else it }
                Result.success(updated)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر فك قفل الجهاز على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun deleteUser(userId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val userToDelete = _users.value.find { it.id == userId }
        val shopId = userToDelete?.shopId
        try {
            var response = api.deleteUser("eq.$userId")
            if (response.code() == 404) {
                response = api.deleteUserLower("eq.$userId")
            }
            if (response.isSuccessful) {
                _users.value = _users.value.filter { it.id != userId }
                if (!shopId.isNullOrBlank()) {
                    val newCount = _users.value.count { it.shopId == shopId }
                    try {
                        api.updateShop("eq.$shopId", mapOf("active_user_count" to newCount))
                    } catch (ignored: Exception) {}
                }
                Result.success(true)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر حذف المستخدم من السيرفر: ${e.localizedMessage}"))
        }
    }

    // ==========================================
    // MAINTENANCE DEVICES
    // ==========================================

    suspend fun fetchDevices(shopId: String): Result<List<Device>> = withContext(Dispatchers.IO) {
        try {
            var response = api.getDevices("eq.$shopId")
            if (response.code() == 404) {
                response = api.getDevicesLower("eq.$shopId")
            }
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                _devices.value = list
                Result.success(list)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر جلب الأجهزة من السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun createDevice(
        customerName: String,
        customerPhone: String?,
        deviceName: String,
        issueDescription: String?,
        estimatedCost: String?,
        downPayment: Double = 0.0,
        technician: String?,
        receivedByEmployee: String? = null,
        photoUrl: String? = null,
        dueDate: String? = null,
        shopId: String
    ): Result<Device> = withContext(Dispatchers.IO) {
        val payload = mutableMapOf<String, Any>(
            "customer_name" to customerName,
            "customer_phone" to (customerPhone ?: ""),
            "device_name" to deviceName,
            "issue_description" to (issueDescription ?: ""),
            "estimated_cost" to (estimatedCost?.toDoubleOrNull() ?: 0.0),
            "down_payment" to downPayment,
            "technician" to (technician ?: "tech1"),
            "status" to "received",
            "received_by_employee" to (receivedByEmployee ?: "موظف الاستلام"),
            "detailed_status" to "diagnosing",
            "shop_id" to shopId
        )
        if (photoUrl != null && photoUrl.isNotBlank()) payload["photo_url"] = photoUrl
        if (dueDate != null && dueDate.isNotBlank() && dueDate != "غير محدد") {
            payload["due_date"] = dueDate
        }

        try {
            var response = api.createDevice(payload)
            if (response.code() == 404) {
                response = api.createDeviceLower(payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val created = response.body()!!.first()
                _devices.value = listOf(created) + _devices.value
                Result.success(created)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر تسجيل الجهاز على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun updateDeviceDetailed(
        id: Long,
        status: String,
        detailedStatus: String,
        notes: String? = null,
        partsUsed: String? = null,
        finalCost: String? = null,
        deliveredByEmployee: String? = null
    ): Result<Device> = withContext(Dispatchers.IO) {
        val payload = mutableMapOf<String, Any>(
            "status" to status,
            "detailed_status" to detailedStatus
        )
        if (notes != null) payload["technician_notes"] = notes
        if (partsUsed != null) payload["parts_used_summary"] = partsUsed
        if (finalCost != null) {
            payload["estimated_cost"] = finalCost.toDoubleOrNull() ?: 0.0
        }
        if (deliveredByEmployee != null) payload["delivered_by_employee"] = deliveredByEmployee

        try {
            var response = api.updateDevice("eq.$id", payload)
            if (response.code() == 404) {
                response = api.updateDeviceLower("eq.$id", payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                _devices.value = _devices.value.map { if (it.id == id) updated else it }
                Result.success(updated)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر تحديث الجهاز على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun updateDeviceStatus(id: Long, newStatus: String): Result<Device> = withContext(Dispatchers.IO) {
        val payload = mapOf<String, Any>("status" to newStatus)
        try {
            var response = api.updateDevice("eq.$id", payload)
            if (response.code() == 404) {
                response = api.updateDeviceLower("eq.$id", payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updated = response.body()!!.first()
                _devices.value = _devices.value.map { if (it.id == id) updated else it }
                Result.success(updated)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر تحديث حالة الجهاز على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun deleteDevice(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var response = api.deleteDevice("eq.$id")
            if (response.code() == 404) {
                response = api.deleteDeviceLower("eq.$id")
            }
            if (response.isSuccessful) {
                _devices.value = _devices.value.filter { it.id != id }
                Result.success(true)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر حذف الجهاز من السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun restoreDevices(importedDevices: List<Device>, replaceAll: Boolean): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            if (replaceAll) {
                _devices.value = importedDevices.sortedByDescending { it.id }
                Result.success(Pair(importedDevices.size, 0))
            } else {
                val currentMap = _devices.value.associateBy { it.id }.toMutableMap()
                var updatedCount = 0
                var newCount = 0

                for (dev in importedDevices) {
                    if (currentMap.containsKey(dev.id)) {
                        currentMap[dev.id] = dev
                        updatedCount++
                    } else {
                        currentMap[dev.id] = dev
                        newCount++
                    }
                }

                _devices.value = currentMap.values.toList().sortedByDescending { it.id }
                Result.success(Pair(newCount, updatedCount))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // INVENTORY & SPARE PARTS
    // ==========================================

    suspend fun fetchInventoryParts(shopId: String): Result<List<InventoryPart>> = withContext(Dispatchers.IO) {
        try {
            var response = api.getInventoryParts("eq.$shopId")
            if (response.code() == 404) {
                response = api.getInventoryPartsLower("eq.$shopId")
            }
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر جلب قطع الغيار من السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun createInventoryPart(part: InventoryPart, shopId: String): Result<InventoryPart> = withContext(Dispatchers.IO) {
        val payload = mapOf<String, Any>(
            "id" to part.id,
            "barcode" to part.barcode,
            "name" to part.name,
            "category" to part.category,
            "device_model" to part.device_model,
            "current_stock" to part.current_stock,
            "unit_cost" to part.unit_cost,
            "selling_price" to part.selling_price,
            "min_alert_stock" to part.min_alert_stock,
            "shop_id" to shopId
        )
        try {
            var response = api.createInventoryPart(payload)
            if (response.code() == 404) {
                response = api.createInventoryPartLower(payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر إضافة قطعة الغيار على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun updateInventoryPart(id: Long, part: InventoryPart): Result<InventoryPart> = withContext(Dispatchers.IO) {
        val payload = mapOf<String, Any>(
            "barcode" to part.barcode,
            "name" to part.name,
            "category" to part.category,
            "device_model" to part.device_model,
            "current_stock" to part.current_stock,
            "unit_cost" to part.unit_cost,
            "selling_price" to part.selling_price,
            "min_alert_stock" to part.min_alert_stock
        )
        try {
            var response = api.updateInventoryPart("eq.$id", payload)
            if (response.code() == 404) {
                response = api.updateInventoryPartLower("eq.$id", payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر تحديث قطعة الغيار على السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun fetchPartMovements(shopId: String): Result<List<PartMovementLog>> = withContext(Dispatchers.IO) {
        try {
            var response = api.getPartMovements("eq.$shopId")
            if (response.code() == 404) {
                response = api.getPartMovementsLower("eq.$shopId")
            }
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر جلب سجل الحركات من السيرفر: ${e.localizedMessage}"))
        }
    }

    suspend fun createPartMovement(movement: PartMovementLog, shopId: String): Result<PartMovementLog> = withContext(Dispatchers.IO) {
        val payload = mutableMapOf<String, Any>(
            "id" to movement.id,
            "barcode" to movement.barcode,
            "part_name" to movement.part_name,
            "type" to movement.type,
            "quantity" to movement.quantity,
            "source_destination" to movement.source_destination,
            "employee_name" to movement.employee_name,
            "timestamp" to movement.timestamp,
            "shop_id" to shopId
        )
        if (movement.device_id != null) payload["device_id"] = movement.device_id
        if (movement.device_ticket != null) payload["device_ticket"] = movement.device_ticket
        if (movement.notes != null) payload["notes"] = movement.notes

        try {
            var response = api.createPartMovement(payload)
            if (response.code() == 404) {
                response = api.createPartMovementLower(payload)
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                val err = parseHttpError(response.code(), response.errorBody()?.string())
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("تعذر تسجيل حركة القطعة على السيرفر: ${e.localizedMessage}"))
        }
    }
}
