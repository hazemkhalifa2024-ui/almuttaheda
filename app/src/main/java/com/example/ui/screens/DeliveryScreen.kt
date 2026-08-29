package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusEmerald

@Composable
fun DeliveryScreen(
    devices: List<Device>,
    currentEmployeeName: String = "موظف الفرع",
    onDeliverDevice: (Device, deliveredByEmployee: String) -> Unit,
    onPrintReceipt: (Device) -> Unit,
    onDeviceClick: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var deviceToDeliver by remember { mutableStateOf<Device?>(null) }
    var deliveryEmployeeName by remember { mutableStateOf(currentEmployeeName) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("الكل") }

    val totalReadyCount = remember(devices) {
        devices.count { 
            it.status.lowercase() == "ready" || 
            it.status == "جاهز للتسليم" || 
            it.status.lowercase() == "unrepairable" || 
            it.status == "تعذر الإصلاح / ملغي"
        }
    }

    val totalDeliveredCount = remember(devices) {
        devices.count { it.status.lowercase() == "delivered" || it.status == "تم التسليم" }
    }

    val readyDevices = remember(devices, searchQuery, selectedStatusFilter) {
        devices.filter { dev ->
            val matchesTab = dev.status.lowercase() == "ready" || 
                             dev.status == "جاهز للتسليم" || 
                             dev.status.lowercase() == "unrepairable" || 
                             dev.status == "تعذر الإصلاح / ملغي"
            
            if (!matchesTab) return@filter false

            val matchesSearch = searchQuery.isBlank() || 
                    dev.customer_name.contains(searchQuery, ignoreCase = true) ||
                    (dev.customer_phone?.contains(searchQuery, ignoreCase = true) == true) ||
                    dev.device_name.contains(searchQuery, ignoreCase = true) ||
                    dev.ticketNumber.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedStatusFilter) {
                "جاهز للتسليم" -> dev.status.lowercase() == "ready" || dev.status == "جاهز للتسليم"
                "تعذر الإصلاح / ملغي" -> dev.status.lowercase() == "unrepairable" || dev.status == "تعذر الإصلاح / ملغي"
                else -> true
            }

            matchesSearch && matchesStatus
        }
    }

    val deliveredDevices = remember(devices, searchQuery) {
        devices.filter { dev ->
            val matchesTab = dev.status.lowercase() == "delivered" || dev.status == "تم التسليم"
            
            if (!matchesTab) return@filter false

            val matchesSearch = searchQuery.isBlank() || 
                    dev.customer_name.contains(searchQuery, ignoreCase = true) ||
                    (dev.customer_phone?.contains(searchQuery, ignoreCase = true) == true) ||
                    dev.device_name.contains(searchQuery, ignoreCase = true) ||
                    dev.ticketNumber.contains(searchQuery, ignoreCase = true)

            matchesSearch
        }
    }

    // Confirm Delivery Modal with Employee Custody Tracking
    deviceToDeliver?.let { dev ->
        AlertDialog(
            onDismissRequest = { deviceToDeliver = null },
            title = {
                Text(
                    text = "تسليم الجهاز للعميل وتحصيل المبلغ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "جهاز: ${dev.device_name} (${dev.ticketNumber})\n" +
                                "العميل: ${dev.customer_name}\n\n" +
                                "📊 تفاصيل الحساب المالي:\n" +
                                "• إجمالي تكلفة الصيانة: ${dev.estimated_cost ?: "0"} ج.م\n" +
                                "• العربون المدفوع سابقاً: ${dev.down_payment} ج.م\n" +
                                "• 💵 المبلغ المتبقي المطلوب تحصيله الآن: ${dev.remaining_balance} ج.م",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    OutlinedTextField(
                        value = deliveryEmployeeName,
                        onValueChange = { deliveryEmployeeName = it },
                        label = { Text("اسم موظف التسليم *") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = EmeraldPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeliverDevice(dev, deliveryEmployeeName.ifBlank { currentEmployeeName })
                        deviceToDeliver = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("تأكيد التسليم والتحصيل")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deviceToDeliver = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("delivery_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "تسليم الأجهزة وتصفية الحساب",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تسليم الأجهزة الجاهزة للعملاء وتحصيل المبالغ مع توثيق اسم موظف التسليم",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("جاهز للتسليم", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StatusEmerald.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("$totalReadyCount", fontSize = 11.sp, color = StatusEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("سجل التسليم", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("$totalDeliveredCount", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }
        }

        // Search Bar for all tabs
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم العميل، الهاتف، التذكرة، أو اسم الجهاز...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("delivery_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }

        // Status Filter Chips only for ready devices tab
        if (selectedTab == 0) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "تصفية حسب حالة الجهاز:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf("الكل", "جاهز للتسليم", "تعذر الإصلاح / ملغي").forEach { statusLabel ->
                            val isSelected = selectedStatusFilter == statusLabel
                            val bgColor = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgColor)
                                    .clickable { selectedStatusFilter = statusLabel }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("filter_status_$statusLabel")
                            ) {
                                Text(
                                    text = statusLabel,
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            if (readyDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = StatusEmerald,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "لا توجد أجهزة بانتظار التسليم حالياً",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(readyDevices, key = { it.id }) { device ->
                    ReadyForDeliveryCard(
                        device = device,
                        onDeliver = {
                            deliveryEmployeeName = currentEmployeeName
                            deviceToDeliver = device
                        },
                        onPrint = { onPrintReceipt(device) },
                        onClick = { onDeviceClick(device) }
                    )
                }
            }
        } else {
            if (deliveredDevices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "لا توجد أجهزة مسلّمة في السجل",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(deliveredDevices, key = { it.id }) { device ->
                    DeliveredDeviceCard(
                        device = device,
                        onPrint = { onPrintReceipt(device) },
                        onClick = { onDeviceClick(device) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReadyForDeliveryCard(
    device: Device,
    onDeliver: () -> Unit,
    onPrint: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.ticketNumber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = device.device_name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                StatusBadge(status = device.status)
            }

            Text(
                text = "العميل: ${device.customer_name} ${if (!device.customer_phone.isNullOrBlank()) "- ${device.customer_phone}" else ""}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!device.received_by_employee.isNullOrBlank()) {
                Text(
                    text = "👤 موظف الاستلام: ${device.received_by_employee}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!device.issue_description.isNullOrBlank()) {
                Text(
                    text = "العطل: ${device.issue_description}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (device.down_payment > 0) {
                        "المطلوب للتحصيل: ${device.remaining_balance} ج.م (من ${device.estimated_cost ?: "0"})"
                    } else {
                        "المبلغ المطلوب: ${device.estimated_cost ?: "0"} ج.م"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusEmerald
                )

                IconButton(onClick = onPrint) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "طباعة الإيصال",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onDeliver,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تأكيد التسليم واستلام المبلغ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DeliveredDeviceCard(
    device: Device,
    onPrint: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.ticketNumber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = device.device_name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = if (device.down_payment > 0) {
                        "${device.customer_name} • العربون: ${device.down_payment} ج.م • المتبقي: ${device.remaining_balance} ج.م"
                    } else {
                        "${device.customer_name} • التكلفة: ${device.estimated_cost ?: "0"} ج.م"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!device.delivered_by_employee.isNullOrBlank()) {
                    Text(
                        text = "👤 موظف التسليم: ${device.delivered_by_employee}",
                        fontSize = 10.sp,
                        color = EmeraldPrimary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status = device.status)
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onPrint) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "طباعة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

