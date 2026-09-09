package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.data.model.InventoryPart
import com.example.data.model.SessionUser
import com.example.data.model.TechnicianNotification
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RedAccent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TechnicianWorkspaceScreen(
    session: SessionUser,
    devices: List<Device>,
    availableParts: List<InventoryPart>,
    notifications: List<TechnicianNotification>,
    onUpdateDetailedStatus: (Device, detailedStatus: String, mainStatus: String, notes: String?) -> Unit,
    onAttachPartToDevice: (Device, barcode: String, partName: String, qty: Int, cost: Double) -> Unit,
    onDeviceClick: (Device) -> Unit,
    onDismissNotification: (Long) -> Unit,
    onWhatsAppClick: (Device) -> Unit = {}
) {
    var selectedFilterTab by remember { mutableStateOf("all") } // "all", "new_received", "diagnosing", "in_repair", "ready"
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state for attaching part to device
    var deviceForPartAttach by remember { mutableStateOf<Device?>(null) }
    var partBarcodeQuery by remember { mutableStateOf("") }
    var partQty by remember { mutableStateOf("1") }

    // Dialog state for adding technician diagnosis notes
    var deviceForNotes by remember { mutableStateOf<Device?>(null) }
    var techNotesText by remember { mutableStateOf("") }

    // Filter devices assigned to this technician (or all if admin)
    val techDevices = remember(devices, session.username, session.role, selectedFilterTab, searchQuery) {
        devices.filter { dev ->
            val matchTech = if (session.role == "admin") true
            else dev.technician.equals(session.username, ignoreCase = true) ||
                    (dev.technician.isNullOrBlank() && session.username == "tech1")

            val matchSearch = searchQuery.isBlank() ||
                    dev.device_name.contains(searchQuery, ignoreCase = true) ||
                    dev.customer_name.contains(searchQuery, ignoreCase = true) ||
                    dev.ticketNumber.contains(searchQuery, ignoreCase = true)

            val matchTab = when (selectedFilterTab) {
                "new_received" -> dev.status == "received" || dev.status == "تم الاستلام"
                "diagnosing" -> dev.detailed_status == "diagnosing" || dev.status == "جاري الفحص"
                "in_repair" -> dev.status == "in_progress" || dev.status == "جاري الإصلاح" || dev.detailed_status == "waiting_parts"
                "ready" -> dev.status == "ready" || dev.status == "جاهز للتسليم"
                else -> true
            }

            matchTech && matchSearch && matchTab
        }
    }

    // Newly received devices count for notification badge
    val newAssignedDevices = remember(devices, session.username) {
        devices.filter {
            (it.status == "received" || it.status == "تم الاستلام") &&
                    (session.role == "admin" || it.technician.equals(session.username, ignoreCase = true) || (it.technician.isNullOrBlank() && session.username == "tech1"))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("technician_workspace_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Notification Alert Banner for New Device Assignment
        if (newAssignedDevices.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldDark.copy(alpha = 0.9f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔔 لديك ${newAssignedDevices.size} جهاز جديد بانتظار الفحص والبدء!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "آخر جهاز: ${newAssignedDevices.first().device_name} للعميل ${newAssignedDevices.first().customer_name}",
                                fontSize = 11.sp,
                                color = EmeraldPrimaryLight
                            )
                        }
                        Button(
                            onClick = { selectedFilterTab = "new_received" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = EmeraldDark
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("عرض الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Header & Search
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Handyman,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "واجهة الفني - طاولة الصيانة",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "الفني الحالي: ${session.displayName} (${techDevices.size} جهاز متاح)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث باسم الجهاز أو العميل أو رقم التذكرة...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Workflow Status Filter Tabs
                    ScrollableTabRow(
                        selectedTabIndex = when (selectedFilterTab) {
                            "all" -> 0
                            "new_received" -> 1
                            "diagnosing" -> 2
                            "in_repair" -> 3
                            "ready" -> 4
                            else -> 0
                        },
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        listOf(
                            "all" to "الكل (${techDevices.size})",
                            "new_received" to "جديد بانتظار الفحص",
                            "diagnosing" to "قيد التشخيص",
                            "in_repair" to "قيد التصليح والقطع",
                            "ready" to "جاهز للتسليم"
                        ).forEachIndexed { index, (key, title) ->
                            Tab(
                                selected = selectedFilterTab == key,
                                onClick = { selectedFilterTab = key },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedFilterTab == key) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Device Cards with Precision Status Progression Buttons
        if (techDevices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusEmerald,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "لا توجد أجهزة في هذا التصنيف حالياً",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "جميع الأجهزة تم فحصها أو لا توجد أجهزة مسندة جديدة.",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }
            }
        }

        items(techDevices) { device ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tech_device_card_${device.id}")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Ticket #, Device Name, Current Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = device.ticketNumber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                            Text(
                                text = device.device_name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (device.status) {
                                        "ready", "جاهز للتسليم" -> StatusEmerald.copy(alpha = 0.15f)
                                        "delivered", "تم التسليم" -> Slate700.copy(alpha = 0.2f)
                                        "in_progress", "جاري الإصلاح", "جاري الفحص" -> StatusBlue.copy(alpha = 0.15f)
                                        else -> StatusAmber.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = device.statusArabicLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (device.status) {
                                    "ready", "جاهز للتسليم" -> StatusEmerald
                                    "delivered", "تم التسليم" -> Slate400
                                    "in_progress", "جاري الإصلاح", "جاري الفحص" -> StatusBlue
                                    else -> StatusAmber
                                }
                            )
                        }
                    }

                    // Customer & Issue Summary
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "👤 العميل: ${device.customer_name} ${if (!device.customer_phone.isNullOrBlank()) "(${device.customer_phone})" else ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "⚠️ العطل: ${device.issue_description ?: "غير محدد"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!device.technician_notes.isNullOrBlank()) {
                            Text(
                                text = "📝 ملاحظات الفحص والتشخيص: ${device.technician_notes}",
                                fontSize = 11.sp,
                                color = StatusBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (!device.parts_used_summary.isNullOrBlank()) {
                            Text(
                                text = "🔩 قطع الغيار المستخدمة: ${device.parts_used_summary}",
                                fontSize = 11.sp,
                                color = StatusEmerald,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "👤 موظف الاستلام: ${device.received_by_employee ?: "الاستقبال"}",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Precision Status Progression Workflow Buttons
                    Text(
                        text = "⚙️ تحديث الحالة بدقة والتحكم في سير الصيانة:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Diagnosing
                        Button(
                            onClick = {
                                onUpdateDetailedStatus(device, "diagnosing", "جاري الفحص", "تم بدء الفحص والتشخيص الإلكتروني")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (device.detailed_status == "diagnosing") StatusBlue else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (device.detailed_status == "diagnosing") Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("١. بدء الفحص", fontSize = 10.sp)
                        }

                        // 2. Waiting Parts
                        Button(
                            onClick = {
                                onUpdateDetailedStatus(device, "waiting_parts", "جاري الإصلاح", "في انتظار توفير قطع الغيار")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (device.detailed_status == "waiting_parts") StatusAmber else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (device.detailed_status == "waiting_parts") Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("٢. طلب قطع غيار", fontSize = 10.sp)
                        }

                        // 3. In Repair
                        Button(
                            onClick = {
                                onUpdateDetailedStatus(device, "in_repair", "جاري الإصلاح", "جاري الصيانة واللحام/التركيب")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (device.detailed_status == "in_repair" || device.status == "جاري الإصلاح") StatusBlue else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (device.detailed_status == "in_repair" || device.status == "جاري الإصلاح") Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("٣. قيد الصيانة الفعلية", fontSize = 10.sp)
                        }

                        // 4. Quality Check
                        Button(
                            onClick = {
                                onUpdateDetailedStatus(device, "quality_check", "جاري الفحص", "تم الانتهاء وجاري اختبار الجودة")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (device.detailed_status == "quality_check") EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (device.detailed_status == "quality_check") Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("٤. اختبار الجودة", fontSize = 10.sp)
                        }

                        // 5. Repaired & Ready
                        Button(
                            onClick = {
                                onUpdateDetailedStatus(device, "ready", "جاهز للتسليم", "تم الإصلاح بنجاح واختبار الجهاز")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusEmerald,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("٥. تم الإصلاح وجاهز للتسليم", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // 6. Unrepairable / Cancelled
                        Button(
                            onClick = {
                                onUpdateDetailedStatus(device, "unrepairable", "تعذر الإصلاح", "تعذر الإصلاح بسبب تلف المعالج أو عدم توفر بديل")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedAccent.copy(alpha = 0.15f),
                                contentColor = RedAccent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعذر الإصلاح", fontSize = 10.sp)
                        }
                    }

                    // Bottom Action Row: Attach Parts & Add Diagnostic Notes & WhatsApp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                deviceForPartAttach = device
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("قطعة غيار", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                deviceForNotes = device
                                techNotesText = device.technician_notes ?: ""
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تقرير الفحص", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onWhatsAppClick(device) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "واتساب", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مراسلة واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal: Attach Spare Part by Barcode to this device
    deviceForPartAttach?.let { dev ->
        AlertDialog(
            onDismissRequest = { deviceForPartAttach = null },
            title = {
                Text(
                    text = "إرفاق قطعة غيار بالباركود (${dev.device_name})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "اختر أو امسح باركود قطعة الغيار ليتم خصمها من المخزن وربطها بالتذكرة ${dev.ticketNumber}:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = partBarcodeQuery,
                        onValueChange = { partBarcodeQuery = it },
                        label = { Text("الباركود أو اسم القطعة") },
                        placeholder = { Text("مثال: SCR-IP13 أو بطارية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Suggestion from available parts
                    val matchedPart = availableParts.find {
                        it.barcode.equals(partBarcodeQuery.trim(), ignoreCase = true) ||
                                it.name.contains(partBarcodeQuery.trim(), ignoreCase = true)
                    }

                    if (matchedPart != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("✅ تم العثور على: ${matchedPart.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                Text("الرصيد المتاح: ${matchedPart.current_stock} قطعة | التكلفة: ${matchedPart.unit_cost} ج", fontSize = 11.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = partQty,
                        onValueChange = { partQty = it },
                        label = { Text("الكمية المستخدمة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qtyInt = partQty.toIntOrNull() ?: 1
                        val matched = availableParts.find {
                            it.barcode.equals(partBarcodeQuery.trim(), ignoreCase = true) ||
                                    it.name.contains(partBarcodeQuery.trim(), ignoreCase = true)
                        }
                        val finalBarcode = matched?.barcode ?: partBarcodeQuery.ifBlank { "PART-GEN" }
                        val finalName = matched?.name ?: partBarcodeQuery.ifBlank { "قطعة غيار صيانة" }
                        val finalCost = (matched?.unit_cost ?: 100.0) * qtyInt

                        onAttachPartToDevice(dev, finalBarcode, finalName, qtyInt, finalCost)
                        deviceForPartAttach = null
                        partBarcodeQuery = ""
                        partQty = "1"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("تأكيد وخصم من المخزن")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deviceForPartAttach = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Modal: Add Technician Diagnostic Notes
    deviceForNotes?.let { dev ->
        AlertDialog(
            onDismissRequest = { deviceForNotes = null },
            title = {
                Text(
                    text = "تدوين ملاحظات الفحص والتشخيص (${dev.ticketNumber})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اكتب تفاصيل القياسات، العطل الفعلي، وفولتيات البوردة:",
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = techNotesText,
                        onValueChange = { techNotesText = it },
                        label = { Text("تقرير الفني") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDetailedStatus(
                            dev,
                            dev.detailed_status ?: "diagnosing",
                            dev.status,
                            techNotesText
                        )
                        deviceForNotes = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("حفظ التقرير")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deviceForNotes = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
