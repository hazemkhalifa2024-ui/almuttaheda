package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiAudioService
import com.example.data.api.N8nWebhookService
import com.example.data.api.TwilioSmsService
import com.example.data.backup.JsonBackupManager
import com.example.data.backup.ParsedBackup
import com.example.data.model.CustomerProfile
import com.example.data.model.Device
import com.example.data.model.InventoryPart
import com.example.data.model.N8nConfig
import com.example.data.model.PartMovementLog
import com.example.data.model.PrinterConfig
import com.example.data.model.SessionUser
import com.example.data.model.SmsConfig
import com.example.data.model.SmsLog
import com.example.data.model.TechnicianMetrics
import com.example.data.model.TechnicianNotification
import com.example.data.model.User
import com.example.data.model.ShopConfig
import com.example.data.repository.MaintenanceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ToastEvent {
    data class Success(val message: String) : ToastEvent()
    data class Error(val message: String) : ToastEvent()
    data class Info(val message: String) : ToastEvent()
}

enum class AppScreen(val id: String, val title: String) {
    DASHBOARD("dashboard", "الرئيسية"),
    NEW_DEVICE("new", "استلام جديد"),
    DELIVERY("delivery", "تسليم"),
    MANAGEMENT("management", "إدارة الأجهزة"),
    CUSTOMER_HISTORY("customer_history", "سجل العملاء"),
    TECHNICIAN_WORKSPACE("technician_workspace", "ورشة الفني"),
    TECHNICIAN_PERFORMANCE("technician_performance", "أداء الفنيين"),
    INVENTORY_BARCODE("inventory_barcode", "قطع الغيار والباركود"),
    INVENTORY_REPORTS("inventory_reports", "تقارير قطع الغيار"),
    PERMISSIONS("permissions", "الصلاحيات"),
    USER_MANAGEMENT("user_management", "إدارة المستخدمين"),
    PRINTER("printer", "الطباعة"),
    SETTINGS("settings", "الإعدادات"),
    SERVER_MONITOR("server_monitor", "السيرفر وقاعدة البيانات"),
    SAAS_MANAGEMENT("saas_management", "إدارة المنصة والمحلات")
}

data class UiState(
    val session: SessionUser? = null,
    val currentScreen: AppScreen = AppScreen.DASHBOARD,
    val devices: List<Device> = emptyList(),
    val users: List<User> = emptyList(),
    val shops: List<ShopConfig> = emptyList(),
    val customerProfiles: List<CustomerProfile> = emptyList(),
    val inventoryParts: List<InventoryPart> = emptyList(),
    val movementLogs: List<PartMovementLog> = emptyList(),
    val technicianNotifications: List<TechnicianNotification> = emptyList(),
    val smsLogs: List<SmsLog> = emptyList(),
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val isDarkMode: Boolean = true,
    val selectedDeviceForDetail: Device? = null,
    val selectedCustomerProfile: CustomerProfile? = null,
    val deviceForDelayNotification: Device? = null,
    val receiptToPrint: Device? = null,
    val stickerToPrint: Device? = null,
    val dualPrintDevice: Device? = null,
    val printerConfig: PrinterConfig = PrinterConfig(),
    val smsConfig: SmsConfig = SmsConfig(),
    val n8nConfig: N8nConfig = N8nConfig()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MaintenanceRepository(application)
    private val twilioService = TwilioSmsService()
    private val n8nService = N8nWebhookService()
    val geminiAudioService = GeminiAudioService()
    private val prefs = application.getSharedPreferences("mottaheda_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<ToastEvent>()
    val toastEvents: SharedFlow<ToastEvent> = _toastEvents.asSharedFlow()

    val isSubscriptionLocked: Boolean
        get() {
            val session = _uiState.value.session ?: return false
            val config = session.shopConfig ?: return false
            return !config.isSubscriptionActive || config.isLimitExceeded
        }

    init {
        com.example.data.api.RetrofitClient.init(application)
        loadPersistedState()
        viewModelScope.launch {
            repository.devices.collect { list ->
                val profiles = computeCustomerProfiles(list)
                _uiState.value = _uiState.value.copy(
                    devices = list,
                    customerProfiles = profiles
                )
                val currentSelectedPhone = _uiState.value.selectedCustomerProfile?.phone
                if (!currentSelectedPhone.isNullOrBlank()) {
                    val updatedProfile = profiles.find { it.phone == currentSelectedPhone }
                    if (updatedProfile != null) {
                        _uiState.value = _uiState.value.copy(selectedCustomerProfile = updatedProfile)
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.users.collect { list ->
                _uiState.value = _uiState.value.copy(users = list)
            }
        }
        viewModelScope.launch {
            repository.shops.collect { list ->
                _uiState.value = _uiState.value.copy(shops = list)
            }
        }
    }

    private fun computeCustomerProfiles(deviceList: List<Device>): List<CustomerProfile> {
        val grouped = deviceList.groupBy {
            val phone = it.customer_phone?.trim() ?: ""
            if (phone.isNotBlank()) phone else "NAME:${it.customer_name.trim()}"
        }

        return grouped.map { (key, devices) ->
            val primaryName = devices.firstOrNull { it.customer_name.isNotBlank() }?.customer_name ?: "عميل"
            val phone = if (key.startsWith("NAME:")) "" else key
            val completed = devices.count { it.status.lowercase() in listOf("delivered", "ready", "تم التسليم", "جاهز للتسليم") }
            val inProgress = devices.count { it.status.lowercase() in listOf("in_progress", "received", "جاري الإصلاح", "جاري الفحص", "تم الاستلام") }
            val totalSpent = devices.filter { it.status.lowercase() in listOf("delivered", "تم التسليم") }
                .sumOf { it.normalizedCost }
            val sortedDevices = devices.sortedByDescending { it.id }
            val lastDate = sortedDevices.firstOrNull()?.created_at

            CustomerProfile(
                phone = phone,
                name = primaryName,
                totalRepairs = devices.size,
                completedRepairs = completed,
                inProgressRepairs = inProgress,
                totalSpent = totalSpent,
                lastVisitDate = lastDate,
                devices = sortedDevices
            )
        }.sortedByDescending { it.totalRepairs }
    }

    private fun loadPersistedState() {
        val dark = prefs.getBoolean("dark_mode", true)
        val savedUser = prefs.getString("session_username", null)
        val savedRole = prefs.getString("session_role", null)
        val savedName = prefs.getString("session_name", null)
        val savedPerms = prefs.getStringSet("session_perms", null)

        val shopName = prefs.getString("printer_shop_name", "المتحدة للصيانة") ?: "المتحدة للصيانة"
        val shopPhone = prefs.getString("printer_shop_phone", "01000000000") ?: "01000000000"
        val footerNote = prefs.getString("printer_footer_note", "شكراً لزيارتكم الورشة - ضمان شهر ضد عيوب الصيانة") ?: "شكراً لزيارتكم الورشة - ضمان شهر ضد عيوب الصيانة"
        val receiptPrinterName = prefs.getString("printer_receipt_name", "طابعة ريسيت العميل (80mm)") ?: "طابعة ريسيت العميل (80mm)"
        val receiptPrinterIp = prefs.getString("printer_receipt_ip", "192.168.1.100") ?: "192.168.1.100"
        val paperWidth = prefs.getString("printer_paper_width", "80mm") ?: "80mm"
        val autoPrintReceipt = prefs.getBoolean("printer_auto_receipt", true)
        val stickerPrinterName = prefs.getString("printer_sticker_name", "طابعة ملصقات الباركود (50x30mm)") ?: "طابعة ملصقات الباركود (50x30mm)"
        val stickerPrinterIp = prefs.getString("printer_sticker_ip", "192.168.1.101") ?: "192.168.1.101"
        val stickerWidth = prefs.getString("printer_sticker_width", "50mm x 30mm") ?: "50mm x 30mm"
        val autoPrintSticker = prefs.getBoolean("printer_auto_sticker", true)
        val dualAutoPrint = prefs.getBoolean("printer_dual_auto_print", true)
        val printStickerTwice = prefs.getBoolean("printer_sticker_twice", false)

        val smsEnabled = prefs.getBoolean("sms_enabled", true)
        val smsSid = prefs.getString("sms_sid", "") ?: ""
        val smsToken = prefs.getString("sms_token", "") ?: ""
        val smsFrom = prefs.getString("sms_from", "") ?: ""
        val autoReady = prefs.getBoolean("sms_auto_ready", true)
        val autoDelay = prefs.getBoolean("sms_auto_delay", true)
        val readyTpl = prefs.getString("sms_ready_tpl", "مرحباً {name}، جهازك ({device}) تذكرة {ticket} جاهز للاستلام من ورشة {shop}. المبلغ: {cost} ج.م. هاتف: {phone}") ?: ""
        val delayTpl = prefs.getString("sms_delay_tpl", "مرحباً {name}، نعتذر عن التأخير في صيانة جهازك ({device}) تذكرة {ticket} بسبب: {reason}. نعمل على إنهائه قريباً. ورشة {shop}" ) ?: ""

        val n8nEnabled = prefs.getBoolean("n8n_enabled", false)
        val n8nUrl = prefs.getString("n8n_webhook_url", "") ?: ""
        val n8nSecret = prefs.getString("n8n_auth_secret", "") ?: ""
        val n8nTrigCreate = prefs.getBoolean("n8n_trig_create", true)
        val n8nTrigStatus = prefs.getBoolean("n8n_trig_status", true)
        val n8nTrigReady = prefs.getBoolean("n8n_trig_ready", true)
        val n8nTrigDelivered = prefs.getBoolean("n8n_trig_delivered", true)

        val printerConfig = PrinterConfig(
            shopName = shopName,
            shopPhone = shopPhone,
            footerNote = footerNote,
            receiptPrinterName = receiptPrinterName,
            receiptPrinterIp = receiptPrinterIp,
            receiptPaperWidth = paperWidth,
            autoPrintReceiptOnIntake = autoPrintReceipt,
            stickerPrinterName = stickerPrinterName,
            stickerPrinterIp = stickerPrinterIp,
            stickerWidth = stickerWidth,
            autoPrintStickerOnIntake = autoPrintSticker,
            dualAutoPrintOnIntake = dualAutoPrint,
            printStickerTwiceOnSave = printStickerTwice
        )

        val n8nConfig = N8nConfig(
            enabled = n8nEnabled,
            webhookUrl = n8nUrl,
            authSecret = n8nSecret,
            triggerOnDeviceCreated = n8nTrigCreate,
            triggerOnStatusChanged = n8nTrigStatus,
            triggerOnDeviceReady = n8nTrigReady,
            triggerOnDeviceDelivered = n8nTrigDelivered
        )

        val smsConfig = SmsConfig(
            enabled = smsEnabled,
            accountSid = smsSid,
            authToken = smsToken,
            fromNumber = smsFrom,
            autoSendOnReady = autoReady,
            autoSendOnDelay = autoDelay,
            readyTemplate = readyTpl.ifBlank { "مرحباً {name}، جهازك ({device}) تذكرة {ticket} جاهز للاستلام من ورشة {shop}. المبلغ: {cost} ج.م. هاتف: {phone}" },
            delayTemplate = delayTpl.ifBlank { "مرحباً {name}، نعتذر عن التأخير في صيانة جهازك ({device}) تذكرة {ticket} بسبب: {reason}. نعمل على إنهائه قريباً. ورشة {shop}" }
        )

        // Ensure Login Screen always appears on app startup
        val session: SessionUser? = null

        _uiState.value = _uiState.value.copy(
            isDarkMode = dark,
            session = null,
            printerConfig = printerConfig,
            smsConfig = smsConfig,
            n8nConfig = n8nConfig
        )
    }

    fun importInventoryParts(imported: List<InventoryPart>, replaceAll: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            var savedCount = 0
            for (part in imported) {
                val existing = _uiState.value.inventoryParts.find { it.barcode.trim().equals(part.barcode.trim(), ignoreCase = true) }
                if (existing != null) {
                    val updated = existing.copy(
                        name = part.name,
                        category = part.category,
                        device_model = part.device_model,
                        unit_cost = part.unit_cost,
                        selling_price = part.selling_price,
                        current_stock = if (replaceAll) part.current_stock else (existing.current_stock + part.current_stock),
                        min_alert_stock = part.min_alert_stock
                    )
                    repository.updateInventoryPart(existing.id, updated)
                } else {
                    val shopId = _uiState.value.session?.shopId ?: "default_shop"
                    repository.createInventoryPart(part, shopId)
                }
                savedCount++
            }
            val shopId = _uiState.value.session?.shopId ?: "default_shop"
            repository.fetchInventoryParts(shopId).onSuccess { latest ->
                _uiState.value = _uiState.value.copy(inventoryParts = latest)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
            _toastEvents.emit(ToastEvent.Success("تمت المزامنة وحفظ $savedCount صنف في قاعدة البيانات الرئيسية بنجاح"))
        }
    }

    fun toggleDarkMode() {
        val newDark = !_uiState.value.isDarkMode
        _uiState.value = _uiState.value.copy(isDarkMode = newDark)
        prefs.edit().putBoolean("dark_mode", newDark).apply()
    }

    fun clearLoginError() {
        _uiState.value = _uiState.value.copy(loginError = null)
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loginError = null)
            val result = repository.login(username.trim(), password.trim())

            result.onSuccess { user ->
                val shopId = user.shopId ?: "default_shop"
                val configResult = repository.fetchShopConfig(shopId)
                val shopConfig = configResult.getOrDefault(ShopConfig(id = shopId))

                _uiState.value = _uiState.value.copy(isLoading = false, loginError = null)

                val allowed = user.parsePermissions()
                val session = SessionUser(
                    username = user.username,
                    role = user.role,
                    displayName = user.username,
                    allowedScreens = allowed,
                    shopId = shopId,
                    shopConfig = shopConfig
                )
                val targetScreen = if (user.role == "super_admin") AppScreen.SAAS_MANAGEMENT else if (user.role == "technician") AppScreen.TECHNICIAN_WORKSPACE else AppScreen.DASHBOARD
                _uiState.value = _uiState.value.copy(
                    session = session, 
                    currentScreen = targetScreen,
                    loginError = null,
                    printerConfig = _uiState.value.printerConfig.copy(
                        shopName = shopConfig.name,
                        shopPhone = shopConfig.phone ?: "01000000000",
                        footerNote = shopConfig.receiptFooter ?: "شكراً لزيارتكم الورشة - ضمان شهر ضد عيوب الصيانة"
                    )
                )
                prefs.edit()
                    .putString("session_username", session.username)
                    .putString("session_role", session.role)
                    .putString("session_name", session.displayName)
                    .putString("session_shop_id", session.shopId)
                    .putStringSet("session_perms", allowed.toSet())
                    .apply()

                _toastEvents.emit(ToastEvent.Success("مرحباً بك، ${session.displayName}"))
                refreshData()
            }.onFailure { err ->
                val errorMsg = err.message ?: "خطأ في تسجيل الدخول"
                _uiState.value = _uiState.value.copy(isLoading = false, loginError = errorMsg)
                _toastEvents.emit(ToastEvent.Error(errorMsg))
            }
        }
    }

    fun logout() {
        _uiState.value = _uiState.value.copy(session = null)
        prefs.edit()
            .remove("session_username")
            .remove("session_role")
            .remove("session_name")
            .remove("session_shop_id")
            .remove("session_perms")
            .apply()
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun refreshData() {
        val shopId = _uiState.value.session?.shopId ?: "default_shop"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.fetchDevices(shopId)
            if (_uiState.value.session?.role == "admin") {
                repository.fetchUsers(shopId)
            }
            repository.fetchInventoryParts(shopId).onSuccess { parts ->
                _uiState.value = _uiState.value.copy(inventoryParts = parts)
            }
            repository.fetchPartMovements(shopId).onSuccess { movements ->
                _uiState.value = _uiState.value.copy(movementLogs = movements)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun createDevice(
        customerName: String,
        customerPhone: String?,
        deviceName: String,
        issueDescription: String?,
        estimatedCost: String?,
        downPayment: Double = 0.0,
        technician: String?,
        receivedByEmployee: String? = null,
        photoUrl: String? = null,
        dueDate: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val empName = receivedByEmployee?.trim()?.ifBlank { null } ?: _uiState.value.session?.displayName ?: "موظف الاستلام"
            val targetTech = technician ?: "tech1"
            val shopId = _uiState.value.session?.shopId ?: "default_shop"
            val result = repository.createDevice(
                customerName = customerName.trim(),
                customerPhone = customerPhone?.trim(),
                deviceName = deviceName.trim(),
                issueDescription = issueDescription?.trim(),
                estimatedCost = estimatedCost?.trim(),
                downPayment = downPayment,
                technician = targetTech,
                receivedByEmployee = empName,
                photoUrl = photoUrl,
                dueDate = dueDate,
                shopId = shopId
            )
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess { created ->
                _toastEvents.emit(ToastEvent.Success("تم استلام الجهاز بنجاح: MUT-${created.id} (الموظف: $empName)"))

                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                val notif = TechnicianNotification(
                    id = System.currentTimeMillis(),
                    technicianUsername = targetTech,
                    deviceId = created.id,
                    deviceTicket = created.ticketNumber,
                    deviceName = created.device_name,
                    customerName = created.customer_name,
                    title = "جهاز جديد مسند إليك: ${created.device_name}",
                    message = "تم استلام جهاز ${created.device_name} برقم تذكرة ${created.ticketNumber} بواسطة $empName. العطل: ${created.issue_description ?: "فحص وصيانة"}",
                    isRead = false,
                    timestamp = nowStr
                )

                // Check Dual Auto Print Setting vs Single Print
                val printerCfg = _uiState.value.printerConfig
                val shouldDualPrint = printerCfg.dualAutoPrintOnIntake
                val shouldReceiptPrint = printerCfg.autoPrintReceiptOnIntake && !shouldDualPrint
                val shouldStickerTwicePrint = printerCfg.printStickerTwiceOnSave

                _uiState.value = _uiState.value.copy(
                    technicianNotifications = listOf(notif) + _uiState.value.technicianNotifications,
                    dualPrintDevice = if (shouldDualPrint) created else null,
                    receiptToPrint = if (shouldReceiptPrint) created else null,
                    stickerToPrint = if (shouldStickerTwicePrint) created else null,
                    currentScreen = AppScreen.DASHBOARD
                )

                // Dispatch n8n Webhook for New Device Intake
                val n8nCfg = _uiState.value.n8nConfig
                if (n8nCfg.enabled && n8nCfg.triggerOnDeviceCreated) {
                    viewModelScope.launch {
                        n8nService.sendEvent(
                            config = n8nCfg,
                            eventType = "device_created",
                            device = created,
                            extraData = mapOf(
                                "action" to "intake",
                                "received_by" to empName,
                                "down_payment" to created.down_payment,
                                "remaining_balance" to created.remaining_balance,
                                "estimated_cost" to (created.estimated_cost ?: "0")
                            )
                        )
                    }
                }
            }.onFailure { err ->
                _toastEvents.emit(ToastEvent.Error("حدث خطأ أثناء حفظ الجهاز: ${err.localizedMessage ?: err.message ?: "تفاصيل غير متوفرة"}"))
            }
        }
    }

    fun updateStatus(device: Device, newStatusKey: String) {
        viewModelScope.launch {
            val mappedStatus = when (newStatusKey) {
                "تم الاستلام" -> "received"
                "جاري الفحص", "جارى الإصلاح", "جاري الإصلاح" -> "in_progress"
                "جاهز للتسليم" -> "ready"
                "تم التسليم" -> "delivered"
                else -> newStatusKey
            }
            val result = repository.updateDeviceStatus(device.id, mappedStatus)
            result.onSuccess { updated ->
                _toastEvents.emit(ToastEvent.Success("تم تحديث حالة الجهاز إلى ${updated.statusArabicLabel}"))
                if (_uiState.value.selectedDeviceForDetail?.id == updated.id) {
                    _uiState.value = _uiState.value.copy(selectedDeviceForDetail = updated)
                }

                // Automatic SMS Trigger when device becomes Ready for pickup
                if (mappedStatus == "ready" && _uiState.value.smsConfig.autoSendOnReady) {
                    sendReadySms(updated)
                }

                // Dispatch n8n Webhook for Status Update
                val n8nCfg = _uiState.value.n8nConfig
                if (n8nCfg.enabled && (n8nCfg.triggerOnStatusChanged || (mappedStatus == "ready" && n8nCfg.triggerOnDeviceReady))) {
                    viewModelScope.launch {
                        n8nService.sendEvent(
                            config = n8nCfg,
                            eventType = if (mappedStatus == "ready") "device_ready" else "status_changed",
                            device = updated,
                            extraData = mapOf("old_status" to device.status, "new_status" to mappedStatus)
                        )
                    }
                }
            }.onFailure {
                _toastEvents.emit(ToastEvent.Error("فشل تحديث الحالة"))
            }
        }
    }

    fun updateDetailedStatus(device: Device, newDetailedStatus: String, mainStatus: String, notes: String?) {
        viewModelScope.launch {
            val updatedNotes = if (!notes.isNullOrBlank()) {
                val nowStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                if (device.technician_notes.isNullOrBlank()) "[$nowStr] $notes" else "${device.technician_notes}\n[$nowStr] $notes"
            } else device.technician_notes

            val result = repository.updateDeviceDetailed(
                id = device.id,
                status = mainStatus,
                detailedStatus = newDetailedStatus,
                notes = updatedNotes
            )
            result.onSuccess { updated ->
                _toastEvents.emit(ToastEvent.Success("تم تحديث مرحلة الصيانة إلى: ${updated.statusArabicLabel}"))
                if (_uiState.value.selectedDeviceForDetail?.id == updated.id) {
                    _uiState.value = _uiState.value.copy(selectedDeviceForDetail = updated)
                }
                if ((mainStatus == "ready" || mainStatus == "جاهز للتسليم") && _uiState.value.smsConfig.autoSendOnReady) {
                    sendReadySms(updated)
                }

                val n8nCfg = _uiState.value.n8nConfig
                if (n8nCfg.enabled && n8nCfg.triggerOnStatusChanged) {
                    viewModelScope.launch {
                        n8nService.sendEvent(
                            config = n8nCfg,
                            eventType = "detailed_status_changed",
                            device = updated,
                            extraData = mapOf("detailed_status" to newDetailedStatus, "notes" to (notes ?: ""))
                        )
                    }
                }
            }.onFailure {
                _toastEvents.emit(ToastEvent.Error("فشل تحديث مرحلة الصيانة"))
            }
        }
    }

    fun attachPartToDevice(
        device: Device,
        barcode: String,
        partName: String,
        quantity: Int,
        cost: Double
    ) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val currentEmployee = _uiState.value.session?.displayName ?: "الفني"
            val movement = PartMovementLog(
                id = System.currentTimeMillis() % 1000000,
                barcode = barcode,
                part_name = partName,
                type = "consumed_repair",
                quantity = quantity,
                device_id = device.id,
                device_ticket = device.ticketNumber,
                source_destination = "صيانة جهاز ${device.ticketNumber} (${device.device_name})",
                employee_name = currentEmployee,
                notes = "استهلاك في أمر الصيانة",
                timestamp = nowStr
            )

            val updatedInventory = _uiState.value.inventoryParts.map { p ->
                if (p.barcode.equals(barcode.trim(), ignoreCase = true)) {
                    p.copy(current_stock = (p.current_stock - quantity).coerceAtLeast(0))
                } else p
            }

            val targetPart = updatedInventory.find { it.barcode.equals(barcode.trim(), ignoreCase = true) }
            if (targetPart != null) {
                repository.updateInventoryPart(targetPart.id, targetPart)
            }
            val shopId = _uiState.value.session?.shopId ?: "default_shop"
            repository.createPartMovement(movement, shopId)

            val newPartsUsed = if (device.parts_used_summary.isNullOrBlank()) "$partName ($quantity ق)" else "${device.parts_used_summary}, $partName ($quantity ق)"
            val addedCost = (device.estimated_cost?.toDoubleOrNull() ?: 0.0) + cost

            repository.updateDeviceDetailed(
                id = device.id,
                status = device.status,
                detailedStatus = device.detailed_status ?: "in_repair",
                partsUsed = newPartsUsed,
                finalCost = addedCost.toInt().toString()
            )

            _uiState.value = _uiState.value.copy(
                inventoryParts = updatedInventory,
                movementLogs = listOf(movement) + _uiState.value.movementLogs
            )
            _toastEvents.emit(ToastEvent.Success("تم صرف وتركيب القطعة ($partName) على جهاز ${device.ticketNumber}"))
        }
    }

    fun dismissNotification(id: Long) {
        _uiState.value = _uiState.value.copy(
            technicianNotifications = _uiState.value.technicianNotifications.filter { it.id != id }
        )
    }

    fun deliverDeviceWithEmployee(device: Device, deliveredByEmployee: String) {
        viewModelScope.launch {
            val result = repository.updateDeviceDetailed(
                id = device.id,
                status = "delivered",
                detailedStatus = "delivered",
                deliveredByEmployee = deliveredByEmployee
            )
            result.onSuccess { updated ->
                _toastEvents.emit(ToastEvent.Success("تم تسليم جهاز ${updated.ticketNumber} بنجاح بواسطة: $deliveredByEmployee"))
                if (_uiState.value.selectedDeviceForDetail?.id == updated.id) {
                    _uiState.value = _uiState.value.copy(selectedDeviceForDetail = updated)
                }

                // Dispatch n8n Webhook for Device Delivered
                val n8nCfg = _uiState.value.n8nConfig
                if (n8nCfg.enabled && n8nCfg.triggerOnDeviceDelivered) {
                    viewModelScope.launch {
                        n8nService.sendEvent(
                            config = n8nCfg,
                            eventType = "device_delivered",
                            device = updated,
                            extraData = mapOf("delivered_by" to deliveredByEmployee)
                        )
                    }
                }
            }.onFailure {
                _toastEvents.emit(ToastEvent.Error("فشل تسجيل تسليم الجهاز"))
            }
        }
    }

    fun receiveInventoryPart(
        barcode: String,
        name: String,
        category: String,
        qty: Int,
        cost: Double,
        source: String,
        employee: String
    ) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val cleanBarcode = barcode.trim()
            val existing = _uiState.value.inventoryParts.find { it.barcode.equals(cleanBarcode, ignoreCase = true) }

            val updatedParts = if (existing != null) {
                _uiState.value.inventoryParts.map {
                    if (it.barcode.equals(cleanBarcode, ignoreCase = true)) {
                        it.copy(current_stock = it.current_stock + qty, unit_cost = cost)
                    } else it
                }
            } else {
                val newPart = InventoryPart(
                    id = System.currentTimeMillis() % 1000000,
                    barcode = cleanBarcode,
                    name = name.trim(),
                    category = category,
                    device_model = "عام",
                    current_stock = qty,
                    unit_cost = cost,
                    selling_price = cost * 1.3
                )
                _uiState.value.inventoryParts + newPart
            }

            val shopId = _uiState.value.session?.shopId ?: "default_shop"
            val targetPart = updatedParts.find { it.barcode.equals(cleanBarcode, ignoreCase = true) }
            if (targetPart != null) {
                if (existing != null) {
                    repository.updateInventoryPart(targetPart.id, targetPart)
                } else {
                    repository.createInventoryPart(targetPart, shopId)
                }
            }

            val movement = PartMovementLog(
                id = System.currentTimeMillis() % 1000000,
                barcode = cleanBarcode,
                part_name = name,
                type = if (source.contains("مورد")) "inward_supplier" else "inward_warehouse",
                quantity = qty,
                device_id = null,
                device_ticket = null,
                source_destination = source,
                employee_name = employee,
                notes = "استلام وارد بالباركود",
                timestamp = nowStr
            )
            repository.createPartMovement(movement, shopId)

            _uiState.value = _uiState.value.copy(
                inventoryParts = updatedParts,
                movementLogs = listOf(movement) + _uiState.value.movementLogs
            )
            _toastEvents.emit(ToastEvent.Success("تم استلام $qty قطعة من ($name) بواسطة: $employee"))
        }
    }

    fun returnSurplusToWarehouse(barcode: String, qty: Int, employee: String, notes: String?) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val cleanBarcode = barcode.trim()
            val existing = _uiState.value.inventoryParts.find { it.barcode.equals(cleanBarcode, ignoreCase = true) }

            val updatedParts = _uiState.value.inventoryParts.map {
                if (it.barcode.equals(cleanBarcode, ignoreCase = true)) {
                    it.copy(current_stock = (it.current_stock - qty).coerceAtLeast(0))
                } else it
            }

            val targetPart = updatedParts.find { it.barcode.equals(cleanBarcode, ignoreCase = true) }
            if (targetPart != null) {
                repository.updateInventoryPart(targetPart.id, targetPart)
            }

            val movement = PartMovementLog(
                id = System.currentTimeMillis() % 1000000,
                barcode = cleanBarcode,
                part_name = existing?.name ?: "قطعة $cleanBarcode",
                type = "return_warehouse",
                quantity = qty,
                device_id = null,
                device_ticket = null,
                source_destination = "المخزن الرئيسي",
                employee_name = employee,
                notes = notes ?: "رد زيادة وفائض نهاية اليوم",
                timestamp = nowStr
            )
            val shopId = _uiState.value.session?.shopId ?: "default_shop"
            repository.createPartMovement(movement, shopId)

            _uiState.value = _uiState.value.copy(
                inventoryParts = updatedParts,
                movementLogs = listOf(movement) + _uiState.value.movementLogs
            )
            _toastEvents.emit(ToastEvent.Success("تم رد الفائض ($qty قطعة) إلى المخزن الرئيسي بنجاح"))
        }
    }

    fun logDefectiveOrSupplierReturn(
        barcode: String,
        qty: Int,
        type: String,
        reason: String,
        destination: String,
        employee: String
    ) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
            val cleanBarcode = barcode.trim()
            val existing = _uiState.value.inventoryParts.find { it.barcode.equals(cleanBarcode, ignoreCase = true) }

            val updatedParts = _uiState.value.inventoryParts.map {
                if (it.barcode.equals(cleanBarcode, ignoreCase = true)) {
                    it.copy(current_stock = (it.current_stock - qty).coerceAtLeast(0))
                } else it
            }

            val targetPart = updatedParts.find { it.barcode.equals(cleanBarcode, ignoreCase = true) }
            if (targetPart != null) {
                repository.updateInventoryPart(targetPart.id, targetPart)
            }

            val movement = PartMovementLog(
                id = System.currentTimeMillis() % 1000000,
                barcode = cleanBarcode,
                part_name = existing?.name ?: "قطعة $cleanBarcode",
                type = type,
                quantity = qty,
                device_id = null,
                device_ticket = null,
                source_destination = destination,
                employee_name = employee,
                notes = reason,
                timestamp = nowStr
            )
            val shopId = _uiState.value.session?.shopId ?: "default_shop"
            repository.createPartMovement(movement, shopId)

            _uiState.value = _uiState.value.copy(
                inventoryParts = updatedParts,
                movementLogs = listOf(movement) + _uiState.value.movementLogs
            )
            _toastEvents.emit(ToastEvent.Success("تم تسجيل الحركة ($reason) بنجاح"))
        }
    }

    fun sendReadySms(device: Device) {
        val phone = device.customer_phone?.trim() ?: ""
        if (phone.isBlank()) {
            viewModelScope.launch {
                _toastEvents.emit(ToastEvent.Info("لا يوجد رقم هاتف مسجل للعميل لإرسال SMS"))
            }
            return
        }

        val config = _uiState.value.smsConfig
        val message = config.readyTemplate
            .replace("{name}", device.customer_name)
            .replace("{device}", device.device_name)
            .replace("{ticket}", device.ticketNumber)
            .replace("{shop}", _uiState.value.printerConfig.shopName)
            .replace("{cost}", device.estimated_cost ?: "0")
            .replace("{phone}", _uiState.value.printerConfig.shopPhone)

        dispatchSms(
            recipientPhone = phone,
            customerName = device.customer_name,
            ticketNumber = device.ticketNumber,
            message = message,
            type = "ready"
        )
    }

    fun openDelayDialog(device: Device) {
        _uiState.value = _uiState.value.copy(deviceForDelayNotification = device)
    }

    fun closeDelayDialog() {
        _uiState.value = _uiState.value.copy(deviceForDelayNotification = null)
    }

    fun sendDelaySms(device: Device, reason: String) {
        val phone = device.customer_phone?.trim() ?: ""
        if (phone.isBlank()) {
            viewModelScope.launch {
                _toastEvents.emit(ToastEvent.Info("لا يوجد رقم هاتف مسجل للعميل لإرسال إشعار التأخير"))
            }
            return
        }

        val config = _uiState.value.smsConfig
        val cleanReason = reason.ifBlank { "انتظار قطع الغيار الأصلية وإعادة الاختبار" }
        val message = config.delayTemplate
            .replace("{name}", device.customer_name)
            .replace("{device}", device.device_name)
            .replace("{ticket}", device.ticketNumber)
            .replace("{reason}", cleanReason)
            .replace("{shop}", _uiState.value.printerConfig.shopName)

        dispatchSms(
            recipientPhone = phone,
            customerName = device.customer_name,
            ticketNumber = device.ticketNumber,
            message = message,
            type = "delay"
        )
        closeDelayDialog()
    }

    private fun dispatchSms(
        recipientPhone: String,
        customerName: String,
        ticketNumber: String,
        message: String,
        type: String
    ) {
        viewModelScope.launch {
            val config = _uiState.value.smsConfig
            val result = twilioService.sendSms(config, recipientPhone, message)

            val log = SmsLog(
                recipientPhone = recipientPhone,
                customerName = customerName,
                ticketNumber = ticketNumber,
                message = message,
                status = if (result.isSuccess) (if (result.isSimulated) "simulated" else "success") else "failed",
                type = type
            )
            _uiState.value = _uiState.value.copy(smsLogs = listOf(log) + _uiState.value.smsLogs)

            if (result.isSuccess) {
                if (result.isSimulated) {
                    _toastEvents.emit(ToastEvent.Success("تم إرسال SMS تجريبي إلى $recipientPhone بنجاح"))
                } else {
                    _toastEvents.emit(ToastEvent.Success("تم إرسال رسالة SMS عبر Twilio إلى $recipientPhone"))
                }
            } else {
                _toastEvents.emit(ToastEvent.Error(result.errorMessage ?: "فشل إرسال SMS"))
            }
        }
    }

    fun openDeviceDetail(device: Device) {
        _uiState.value = _uiState.value.copy(selectedDeviceForDetail = device)
    }

    fun closeDeviceDetail() {
        _uiState.value = _uiState.value.copy(selectedDeviceForDetail = null)
    }

    fun openCustomerProfile(profile: CustomerProfile) {
        _uiState.value = _uiState.value.copy(selectedCustomerProfile = profile)
    }

    fun openCustomerProfileByDevice(device: Device) {
        val phone = device.customer_phone?.trim() ?: ""
        val profile = _uiState.value.customerProfiles.find {
            (phone.isNotBlank() && it.phone == phone) || (phone.isBlank() && it.name.trim() == device.customer_name.trim())
        } ?: CustomerProfile(
            phone = phone,
            name = device.customer_name,
            totalRepairs = 1,
            completedRepairs = if (device.status in listOf("delivered", "ready")) 1 else 0,
            inProgressRepairs = if (device.status !in listOf("delivered", "ready")) 1 else 0,
            totalSpent = if (device.status == "delivered") device.normalizedCost else 0.0,
            lastVisitDate = device.created_at,
            devices = listOf(device)
        )
        _uiState.value = _uiState.value.copy(selectedCustomerProfile = profile)
    }

    fun closeCustomerProfile() {
        _uiState.value = _uiState.value.copy(selectedCustomerProfile = null)
    }

    fun openDualPrint(device: Device) {
        _uiState.value = _uiState.value.copy(dualPrintDevice = device)
    }

    fun closeDualPrint() {
        _uiState.value = _uiState.value.copy(dualPrintDevice = null)
    }

    fun openReceiptPrint(device: Device) {
        _uiState.value = _uiState.value.copy(receiptToPrint = device)
    }

    fun closeReceiptPrint() {
        _uiState.value = _uiState.value.copy(receiptToPrint = null)
    }

    fun openStickerPrint(device: Device) {
        _uiState.value = _uiState.value.copy(stickerToPrint = device)
    }

    fun closeStickerPrint() {
        _uiState.value = _uiState.value.copy(stickerToPrint = null)
    }

    fun savePrinterConfig(newConfig: PrinterConfig) {
        _uiState.value = _uiState.value.copy(printerConfig = newConfig)
        prefs.edit()
            .putString("printer_shop_name", newConfig.shopName)
            .putString("printer_shop_phone", newConfig.shopPhone)
            .putString("printer_footer_note", newConfig.footerNote)
            .putString("printer_receipt_name", newConfig.receiptPrinterName)
            .putString("printer_receipt_ip", newConfig.receiptPrinterIp)
            .putString("printer_paper_width", newConfig.receiptPaperWidth)
            .putBoolean("printer_auto_receipt", newConfig.autoPrintReceiptOnIntake)
            .putString("printer_sticker_name", newConfig.stickerPrinterName)
            .putString("printer_sticker_ip", newConfig.stickerPrinterIp)
            .putString("printer_sticker_width", newConfig.stickerWidth)
            .putBoolean("printer_auto_sticker", newConfig.autoPrintStickerOnIntake)
            .putBoolean("printer_dual_auto_print", newConfig.dualAutoPrintOnIntake)
            .putBoolean("printer_sticker_twice", newConfig.printStickerTwiceOnSave)
            .apply()
        viewModelScope.launch {
            _toastEvents.emit(ToastEvent.Success("تم حفظ إعدادات الطابعتين والطباعة المزدوجة بنجاح"))
        }
    }

    fun saveN8nConfig(newConfig: N8nConfig) {
        _uiState.value = _uiState.value.copy(n8nConfig = newConfig)
        prefs.edit()
            .putBoolean("n8n_enabled", newConfig.enabled)
            .putString("n8n_webhook_url", newConfig.webhookUrl)
            .putString("n8n_auth_secret", newConfig.authSecret)
            .putBoolean("n8n_trig_create", newConfig.triggerOnDeviceCreated)
            .putBoolean("n8n_trig_status", newConfig.triggerOnStatusChanged)
            .putBoolean("n8n_trig_ready", newConfig.triggerOnDeviceReady)
            .putBoolean("n8n_trig_delivered", newConfig.triggerOnDeviceDelivered)
            .apply()
        viewModelScope.launch {
            _toastEvents.emit(ToastEvent.Success("تم حفظ إعدادات الربط مع n8n بنجاح"))
        }
    }

    fun testN8nWebhook() {
        val cfg = _uiState.value.n8nConfig
        if (!cfg.enabled || cfg.webhookUrl.isBlank()) {
            viewModelScope.launch {
                _toastEvents.emit(ToastEvent.Error("الرجاء تفعيل الـ Webhook وإدخال الرابط أولاً"))
            }
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = n8nService.testConnection(cfg)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { msg ->
                _toastEvents.emit(ToastEvent.Success(msg))
            }.onFailure { err ->
                _toastEvents.emit(ToastEvent.Error(err.localizedMessage ?: "فشل الاتصال بـ n8n"))
            }
        }
    }

    fun sendDailyReportToN8n() {
        viewModelScope.launch {
            val config = _uiState.value.n8nConfig
            if (!config.enabled || config.webhookUrl.isBlank()) {
                _toastEvents.emit(ToastEvent.Error("ميزة أتمتة n8n غير مفعلة أو الرابط فارغ، يرجى تفعيلها من الإعدادات أولاً"))
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Compile stats
            val list = _uiState.value.devices
            val totalDevicesCount = list.size
            val receivedToday = list.filter { it.status == "received" || it.status == "تم الاستلام" }.size
            val readyCount = list.filter { it.status == "ready" || it.status == "جاهز للتسليم" }.size
            val deliveredCount = list.filter { it.status == "delivered" || it.status == "تم التسليم" }.size
            val totalRevenueDelivered = list.filter { it.status == "delivered" || it.status == "تم التسليم" }.sumOf { it.normalizedCost }
            val totalDownPayments = list.sumOf { it.down_payment }
            val outstandingToCollect = list.filter { it.status != "delivered" && it.status != "unrepairable" }.sumOf { it.remaining_balance }

            val payload = mapOf(
                "action" to "daily_report",
                "manager_report" to true,
                "total_devices" to totalDevicesCount,
                "received_today" to receivedToday,
                "ready_for_delivery" to readyCount,
                "delivered_count" to deliveredCount,
                "revenue_collected" to totalRevenueDelivered,
                "total_down_payments_held" to totalDownPayments,
                "outstanding_balances_remaining" to outstandingToCollect,
                "report_timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )

            val result = n8nService.sendEvent(config, "daily_report", null, payload)
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess { msg ->
                _toastEvents.emit(ToastEvent.Success("تم إرسال التقرير الشامل للمدير بنجاح!"))
            }.onFailure { err ->
                _toastEvents.emit(ToastEvent.Error("فشل إرسال التقرير: ${err.localizedMessage}"))
            }
        }
    }

    fun saveSmsConfig(newConfig: SmsConfig) {
        _uiState.value = _uiState.value.copy(smsConfig = newConfig)
        prefs.edit()
            .putBoolean("sms_enabled", newConfig.enabled)
            .putString("sms_sid", newConfig.accountSid)
            .putString("sms_token", newConfig.authToken)
            .putString("sms_from", newConfig.fromNumber)
            .putBoolean("sms_auto_ready", newConfig.autoSendOnReady)
            .putBoolean("sms_auto_delay", newConfig.autoSendOnDelay)
            .putString("sms_ready_tpl", newConfig.readyTemplate)
            .putString("sms_delay_tpl", newConfig.delayTemplate)
            .apply()
        viewModelScope.launch {
            _toastEvents.emit(ToastEvent.Success("تم حفظ إعدادات الرسائل النصية وTwilio"))
        }
    }

    fun testSendSms(testPhone: String) {
        if (testPhone.isBlank()) {
            viewModelScope.launch { _toastEvents.emit(ToastEvent.Error("الرجاء إدخال رقم هاتف للاختبار")) }
            return
        }
        dispatchSms(
            recipientPhone = testPhone,
            customerName = "عميل تجريبي",
            ticketNumber = "MUT-TEST",
            message = "رسالة تجريبية من ورشة ${_uiState.value.printerConfig.shopName}: خدمة الرسائل النصية Twilio تعمل بنجاح.",
            type = "test"
        )
    }

    fun savePermissionsForUser(userId: Long, allowedScreens: List<String>) {
        viewModelScope.launch {
            val json = "[${allowedScreens.joinToString(",") { "\"$it\"" }}]"
            val result = repository.saveUserPermissions(userId, json)
            result.onSuccess {
                _toastEvents.emit(ToastEvent.Success("تم حفظ الصلاحيات بنجاح"))
            }.onFailure {
                _toastEvents.emit(ToastEvent.Error("فشل حفظ الصلاحيات"))
            }
        }
    }

    fun createUser(username: String, password: String, role: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val defaultPerms = when (role) {
                "admin" -> "[\"dashboard\",\"new\",\"delivery\",\"management\",\"permissions\",\"user_management\",\"printer\",\"settings\"]"
                "reception" -> "[\"dashboard\",\"new\",\"delivery\",\"management\",\"customer_history\",\"printer\"]"
                "technician" -> "[\"technician_workspace\",\"management\"]"
                else -> "[\"dashboard\",\"new\",\"delivery\",\"management\"]"
            }
            val currentSession = _uiState.value.session
            val shopId = currentSession?.shopId ?: "default_shop"
            val rawShopName = currentSession?.shopConfig?.name?.trim()?.ifBlank { null }
                ?: _uiState.value.shops.find { it.id == shopId }?.name?.trim()?.ifBlank { null }
                ?: shopId
            val shopPrefix = rawShopName.replace(" ", "_")

            val rawUser = username.trim()
            val finalUsername = if (rawUser.startsWith("$rawShopName-") || rawUser.startsWith("$shopPrefix-")) {
                rawUser
            } else {
                "$rawShopName-$rawUser"
            }

            val result = repository.createUser(finalUsername, password, role, defaultPerms, shopId)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { user ->
                _toastEvents.emit(ToastEvent.Success("تم إنشاء حساب المستخدم ${user.username} بنجاح"))
            }.onFailure { e ->
                _toastEvents.emit(ToastEvent.Error("فشل إنشاء الحساب: ${e.localizedMessage}"))
            }
        }
    }

    fun updateUser(userId: Long, username: String, password: String, role: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.updateUser(userId, username, password, role, null)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { user ->
                _toastEvents.emit(ToastEvent.Success("تم تحديث بيانات المستخدم ${user.username} بنجاح"))
            }.onFailure { e ->
                _toastEvents.emit(ToastEvent.Error("فشل تحديث البيانات: ${e.localizedMessage}"))
            }
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.deleteUser(userId)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess {
                _toastEvents.emit(ToastEvent.Success("تم حذف حساب المستخدم بنجاح"))
            }.onFailure { e ->
                _toastEvents.emit(ToastEvent.Error("فشل حذف الحساب: ${e.localizedMessage}"))
            }
        }
    }

    fun resetUserDeviceId(userId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.resetUserDeviceId(userId)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { user ->
                _toastEvents.emit(ToastEvent.Success("تم فك قيد الهاتف للموظف ${user.username} بنجاح! جاهز للتفعيل على هاتف آخر."))
            }.onFailure { e ->
                _toastEvents.emit(ToastEvent.Error("فشل فك قيد هاتف المستخدم: ${e.localizedMessage}"))
            }
        }
    }

    fun shareWhatsApp(context: Context, device: Device) {
        val phone = device.customer_phone?.trim()?.replace("+", "")?.replace(" ", "") ?: ""
        val message = "مرحباً ${device.customer_name}، بخصوص جهازك (${device.device_name}) في ورشة المتحدة للصيانة: تذكرة رقم MUT-${device.id}، الحالة الحالية: ${device.statusArabicLabel}."
        try {
            val url = if (phone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(sendIntent, "مشاركة تفاصيل التذكرة"))
        }
    }

    fun exportDatabaseToJson(): String {
        return JsonBackupManager.exportToJson(_uiState.value.devices)
    }

    fun parseBackupJson(jsonString: String): Result<ParsedBackup> {
        return JsonBackupManager.parseJsonBackup(jsonString)
    }

    fun restoreBackup(importedDevices: List<Device>, replaceAll: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.restoreDevices(importedDevices, replaceAll)
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.onSuccess { (newCount, updatedCount) ->
                if (replaceAll) {
                    _toastEvents.emit(ToastEvent.Success("تم استبدال قاعدة البيانات بنجاح واستعادة $newCount جهاز"))
                } else {
                    _toastEvents.emit(ToastEvent.Success("تم دمج البيانات بنجاح ($newCount جهاز جديد، $updatedCount جهاز تم تحديثه)"))
                }
            }.onFailure { err ->
                _toastEvents.emit(ToastEvent.Error("فشلت عملية استعادة البيانات: ${err.message}"))
            }
        }
    }

    fun loadShops() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.fetchAllShops().onSuccess { shopList ->
                _uiState.value = _uiState.value.copy(shops = shopList)
            }.onFailure { err ->
                _toastEvents.emit(ToastEvent.Error("فشل في تحميل قائمة المحلات: ${err.message}"))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun addNewShop(
        id: String,
        name: String,
        address: String,
        phone: String,
        receiptFooter: String,
        subscriptionStatus: String,
        subscriptionExpiresAt: String,
        userLimit: Int,
        adminUsername: String? = null,
        adminPassword: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val cleanId = id.trim().ifBlank { "shop_${(1000..9999).random()}" }
            val result = repository.createShop(
                id = cleanId,
                name = name.trim(),
                address = address.trim(),
                phone = phone.trim(),
                receiptFooter = receiptFooter.trim(),
                subscriptionStatus = subscriptionStatus,
                subscriptionExpiresAt = subscriptionExpiresAt,
                userLimit = userLimit
            )
            if (!adminUsername.isNullOrBlank() && !adminPassword.isNullOrBlank()) {
                val adminPerms = "[\"dashboard\",\"new\",\"delivery\",\"management\",\"permissions\",\"user_management\",\"printer\",\"settings\"]"
                val rawShopName = name.trim().ifBlank { cleanId }
                val shopPrefix = rawShopName.replace(" ", "_")
                val rawAdminUser = adminUsername.trim()
                val finalAdminUser = if (rawAdminUser.startsWith("$rawShopName-") || rawAdminUser.startsWith("$shopPrefix-")) {
                    rawAdminUser
                } else {
                    "$rawShopName-$rawAdminUser"
                }
                repository.createUser(finalAdminUser, adminPassword.trim(), "admin", adminPerms, cleanId)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { shop ->
                _toastEvents.emit(ToastEvent.Success("تم تسجيل المحل ${shop.name} وحساب المدير بنجاح"))
                loadShops()
            }.onFailure { err ->
                _toastEvents.emit(ToastEvent.Error("فشل في إنشاء المحل: ${err.message}"))
            }
        }
    }

    fun createUserForShop(username: String, password: String, role: String, shopId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val defaultPerms = when (role) {
                "admin" -> "[\"dashboard\",\"new\",\"delivery\",\"management\",\"permissions\",\"user_management\",\"printer\",\"settings\"]"
                "reception" -> "[\"dashboard\",\"new\",\"delivery\",\"management\",\"customer_history\",\"printer\"]"
                "technician" -> "[\"technician_workspace\",\"management\"]"
                else -> "[\"dashboard\",\"new\",\"delivery\",\"management\"]"
            }
            val targetShop = _uiState.value.shops.find { it.id == shopId }
            val rawShopName = targetShop?.name?.trim()?.ifBlank { null } ?: shopId
            val shopPrefix = rawShopName.replace(" ", "_")

            val rawUser = username.trim()
            val finalUsername = if (rawUser.startsWith("$rawShopName-") || rawUser.startsWith("$shopPrefix-")) {
                rawUser
            } else {
                "$rawShopName-$rawUser"
            }

            val result = repository.createUser(finalUsername, password.trim(), role, defaultPerms, shopId)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess { user ->
                _toastEvents.emit(ToastEvent.Success("تم إنشاء حساب المستخدم ${user.username} في الفرع بنجاح"))
                loadShops()
            }.onFailure { e ->
                _toastEvents.emit(ToastEvent.Error("فشل إنشاء الحساب: ${e.localizedMessage}"))
            }
        }
    }

    fun updateShopSubscription(
        id: String,
        isActive: Boolean,
        expiryDate: String,
        userLimit: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val updates = mapOf(
                "subscription_status" to (if (isActive) "active" else "expired"),
                "subscription_expires_at" to expiryDate,
                "user_limit" to userLimit
            )
            val result = repository.updateShop(id, updates)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess {
                _toastEvents.emit(ToastEvent.Success("تم تحديث اشتراك المحل بنجاح"))
                loadShops()
            }.onFailure { err ->
                _toastEvents.emit(ToastEvent.Error("فشل التحديث: ${err.message}"))
            }
        }
    }

    fun deleteShop(shopId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.deleteShop(shopId)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onSuccess {
                _toastEvents.emit(ToastEvent.Success("تم حذف المحل بنجاح"))
                loadShops()
            }.onFailure { e ->
                _toastEvents.emit(ToastEvent.Error("فشل حذف المحل: ${e.message}"))
            }
        }
    }

    fun getServerUrl(): String = com.example.data.api.RetrofitClient.getServerUrl()
    fun getAnonKey(): String = com.example.data.api.RetrofitClient.getAnonKey()
    fun getBasicUser(): String = com.example.data.api.RetrofitClient.getBasicUser()
    fun getBasicPass(): String = com.example.data.api.RetrofitClient.getBasicPass()

    fun updateServerConfig(url: String, anonKey: String, basicUser: String, basicPass: String) {
        com.example.data.api.RetrofitClient.updateConfig(getApplication(), url, anonKey, basicUser, basicPass)
        viewModelScope.launch {
            _toastEvents.emit(ToastEvent.Success("تم حفظ وتحديث إعدادات السيرفر بنجاح"))
            loadShops()
        }
    }

    suspend fun testServerConnection(url: String, anonKey: String, basicUser: String, basicPass: String): Result<String> {
        return com.example.data.api.RetrofitClient.testConnection(url, anonKey, basicUser, basicPass)
    }
}
