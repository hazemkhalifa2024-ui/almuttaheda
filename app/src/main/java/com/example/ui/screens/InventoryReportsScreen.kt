package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.data.model.InventoryPart
import com.example.data.model.PartMovementLog
import com.example.data.model.SessionUser
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RedAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportDateFilter(val label: String) {
    TODAY("اليوم"),
    YESTERDAY("أمس"),
    LAST_7_DAYS("آخر 7 أيام"),
    THIS_MONTH("هذا الشهر"),
    ALL("الكل")
}

enum class ReportSourceCategory(val label: String, val code: String) {
    ALL("الكل", "all"),
    CONSUMED_REPAIR("المستهلك في الصيانة", "consumed_repair"),
    INWARD_WAREHOUSE("الوارد من المخزن", "inward_warehouse"),
    INWARD_SUPPLIER("الوارد من مورد خارجي", "inward_supplier"),
    RETURN_WAREHOUSE("رد زيادة للمخزن", "return_warehouse"),
    DEFECTIVE_AND_SUPPLIER("التوالف والمرتجع", "defective_and_supplier")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryReportsScreen(
    session: SessionUser?,
    partsList: List<InventoryPart>,
    movementLogs: List<PartMovementLog>,
    devices: List<Device>,
    onDeviceClick: (Device) -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("سجل العمليات والتدقيق", "الاستهلاك اليومي للصيانة", "تحليل المصادر والموردين", "مسؤولية الموظفين")

    var dateFilter by remember { mutableStateOf(ReportDateFilter.TODAY) }
    var sourceCategoryFilter by remember { mutableStateOf(ReportSourceCategory.ALL) }
    var selectedEmployeeFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Collect list of distinct employees from logs
    val allEmployees = remember(movementLogs) {
        movementLogs.map { it.employee_name.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Reference dates for filtering
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val yesterdayDateStr = remember {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    val currentMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }

    // Filter movement logs according to criteria
    val filteredLogs = remember(movementLogs, dateFilter, sourceCategoryFilter, selectedEmployeeFilter, searchQuery) {
        movementLogs.filter { log ->
            // Date Filter
            val matchesDate = when (dateFilter) {
                ReportDateFilter.TODAY -> log.timestamp.startsWith(todayDateStr)
                ReportDateFilter.YESTERDAY -> log.timestamp.startsWith(yesterdayDateStr)
                ReportDateFilter.LAST_7_DAYS -> {
                    log.timestamp.startsWith(todayDateStr) || log.timestamp.startsWith(yesterdayDateStr) || log.timestamp.isNotBlank()
                }
                ReportDateFilter.THIS_MONTH -> log.timestamp.startsWith(currentMonthStr)
                ReportDateFilter.ALL -> true
            }

            // Source/Type Category Filter
            val matchesCategory = when (sourceCategoryFilter) {
                ReportSourceCategory.ALL -> true
                ReportSourceCategory.CONSUMED_REPAIR -> log.type == "consumed_repair"
                ReportSourceCategory.INWARD_WAREHOUSE -> log.type == "inward_warehouse"
                ReportSourceCategory.INWARD_SUPPLIER -> log.type == "inward_supplier"
                ReportSourceCategory.RETURN_WAREHOUSE -> log.type == "return_warehouse"
                ReportSourceCategory.DEFECTIVE_AND_SUPPLIER -> log.type in listOf("defective_damaged", "return_supplier")
            }

            // Employee Filter
            val matchesEmployee = selectedEmployeeFilter == null || log.employee_name.trim().equals(selectedEmployeeFilter?.trim(), ignoreCase = true)

            // Search Query Filter
            val q = searchQuery.trim()
            val matchesSearch = if (q.isBlank()) true else {
                log.part_name.contains(q, ignoreCase = true) ||
                        log.barcode.contains(q, ignoreCase = true) ||
                        log.employee_name.contains(q, ignoreCase = true) ||
                        log.source_destination.contains(q, ignoreCase = true) ||
                        (log.device_ticket?.contains(q, ignoreCase = true) == true) ||
                        (log.notes?.contains(q, ignoreCase = true) == true)
            }

            matchesDate && matchesCategory && matchesEmployee && matchesSearch
        }.sortedByDescending { it.id }
    }

    // Compute Metrics based on filtered set
    val totalConsumedQty = remember(filteredLogs) {
        filteredLogs.filter { it.type == "consumed_repair" }.sumOf { it.quantity }
    }
    val totalInwardWarehouseQty = remember(filteredLogs) {
        filteredLogs.filter { it.type == "inward_warehouse" }.sumOf { it.quantity }
    }
    val totalInwardSupplierQty = remember(filteredLogs) {
        filteredLogs.filter { it.type == "inward_supplier" }.sumOf { it.quantity }
    }
    val totalSurplusReturnedQty = remember(filteredLogs) {
        filteredLogs.filter { it.type == "return_warehouse" }.sumOf { it.quantity }
    }
    val totalDefectiveQty = remember(filteredLogs) {
        filteredLogs.filter { it.type in listOf("defective_damaged", "return_supplier") }.sumOf { it.quantity }
    }

    // Cost mappings
    val partCostMap = remember(partsList) { partsList.associate { it.barcode.lowercase().trim() to it.unit_cost } }
    val totalConsumedCost = remember(filteredLogs, partCostMap) {
        filteredLogs.filter { it.type == "consumed_repair" }.sumOf { log ->
            val unitCost = partCostMap[log.barcode.lowercase().trim()] ?: 150.0
            unitCost * log.quantity
        }
    }

    // Tab 1 Precomputations: Consumed Logs grouped by day
    val consumedLogs = remember(filteredLogs) {
        filteredLogs.filter { it.type == "consumed_repair" }
    }
    val groupedByDay = remember(consumedLogs) {
        consumedLogs.groupBy {
            val ts = it.timestamp.trim()
            if (ts.length >= 10) ts.substring(0, 10) else "غير محدد"
        }
    }

    // Tab 2 Precomputations: Source Analysis
    val inwardLogs = remember(filteredLogs) {
        filteredLogs.filter { it.type in listOf("inward_warehouse", "inward_supplier") }
    }
    val warehouseLogs = remember(inwardLogs) { inwardLogs.filter { it.type == "inward_warehouse" } }
    val supplierLogs = remember(inwardLogs) { inwardLogs.filter { it.type == "inward_supplier" } }
    val warehouseTotalQty = remember(warehouseLogs) { warehouseLogs.sumOf { it.quantity } }
    val supplierTotalQty = remember(supplierLogs) { supplierLogs.sumOf { it.quantity } }
    val grandInwardQty = (warehouseTotalQty + supplierTotalQty).coerceAtLeast(1)
    val warehousePercent = ((warehouseTotalQty.toFloat() / grandInwardQty.toFloat()) * 100).toInt()
    val supplierPercent = 100 - warehousePercent
    val groupedSuppliers = remember(supplierLogs) {
        supplierLogs.groupBy { it.source_destination.ifBlank { "مورد عام" } }
    }

    // Tab 3 Precomputations: Grouped by Employee
    val groupedByEmployee = remember(filteredLogs) {
        filteredLogs.groupBy { it.employee_name.ifBlank { "موظف غير محدد" } }
    }

    // Helper to generate clipboard / shareable summary text
    val generateReportSummaryText = {
        buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("📋 تقرير استهلاك وحركة قطع الغيار - المتحدة للصيانة")
            appendLine("📅 الفترة: ${dateFilter.label} (${todayDateStr})")
            if (selectedEmployeeFilter != null) {
                appendLine("👤 الموظف المسؤول المصفى: $selectedEmployeeFilter")
            }
            appendLine("═══════════════════════════════════════")
            appendLine("📊 ملخص المؤشرات:")
            appendLine("• المنصرف والمستهلك في الصيانة: $totalConsumedQty قطعة (قيمة تقريبية: ${totalConsumedCost.toInt()} ج.م)")
            appendLine("• الوارد من المخزن الرئيسي: $totalInwardWarehouseQty قطعة")
            appendLine("• الوارد من الموردين الخارجيين: $totalInwardSupplierQty قطعة")
            appendLine("• المرتجع والتوالف: $totalDefectiveQty قطعة")
            appendLine("• الفائض المردود للمخزن: $totalSurplusReturnedQty قطعة")
            appendLine("───────────────────────────────────────")
            appendLine("📝 تفاصيل العمليات الأخيرة (${filteredLogs.size} عملية):")
            filteredLogs.take(15).forEachIndexed { i, log ->
                val sourceLabel = if (log.type == "consumed_repair") {
                    "جهاز ${log.device_ticket ?: "صيانة"}"
                } else log.source_destination
                appendLine("${i + 1}. [${log.typeArabicLabel}] ${log.part_name} (${log.quantity} ق)")
                appendLine("   المصدر/الوجهة: $sourceLabel | الموظف: ${log.employee_name} | ${log.timestamp}")
            }
            if (filteredLogs.size > 15) {
                appendLine("   ... والمزيد (${filteredLogs.size - 15} عمليات أخرى)")
            }
            appendLine("═══════════════════════════════════════")
            appendLine("تم التوليد آلياً من نظام ورشة المتحدة للصيانة")
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("inventory_reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Title & Action Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(EmeraldPrimary, EmeraldDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "تقارير حركة واستهلاك قطع الغيار",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تحليل الاستهلاك اليومي والمصادر وتوثيق الموظف المسؤول",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Share / Copy Report Summary
                        IconButton(
                            onClick = {
                                val summary = generateReportSummaryText()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Inventory Report", summary))
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, summary)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة تقرير قطع الغيار"))
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.12f))
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة التقرير",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top KPIs Metric Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Consumed in Repairs Card
                    ReportMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "المستهلك بالصيانة",
                        value = "$totalConsumedQty ق",
                        subValue = "≈ ${totalConsumedCost.toInt()} ج.م",
                        icon = Icons.Default.TrendingDown,
                        iconTint = RedAccent,
                        backgroundGradient = listOf(RedAccent.copy(alpha = 0.12f), RedAccent.copy(alpha = 0.03f))
                    )

                    // Inward from Warehouse Card
                    ReportMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "وارد من المخزن",
                        value = "$totalInwardWarehouseQty ق",
                        subValue = "المخزن الرئيسي",
                        icon = Icons.Default.Warehouse,
                        iconTint = EmeraldPrimary,
                        backgroundGradient = listOf(EmeraldPrimary.copy(alpha = 0.12f), EmeraldPrimary.copy(alpha = 0.03f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Inward from Supplier Card
                    ReportMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "وارد من موردين",
                        value = "$totalInwardSupplierQty ق",
                        subValue = "شراء وتوريد مباشر",
                        icon = Icons.Default.LocalShipping,
                        iconTint = StatusBlue,
                        backgroundGradient = listOf(StatusBlue.copy(alpha = 0.12f), StatusBlue.copy(alpha = 0.03f))
                    )

                    // Defective / Return Card
                    ReportMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "مرتجع وتوالف",
                        value = "${totalDefectiveQty + totalSurplusReturnedQty} ق",
                        subValue = "فائض/توالف/مرتجع",
                        icon = Icons.Default.Warning,
                        iconTint = StatusAmber,
                        backgroundGradient = listOf(StatusAmber.copy(alpha = 0.12f), StatusAmber.copy(alpha = 0.03f))
                    )
                }
            }
        }

        // Search and Filters Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_search_field"),
                        placeholder = { Text("بحث برقم الباركود، اسم القطعة، الموظف، رقم التذكرة...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    // Date Period Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "الفترة:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ReportDateFilter.values()) { filter ->
                                val isSelected = dateFilter == filter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { dateFilter = filter },
                                    label = { Text(filter.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = EmeraldPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Source Category Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "المصدر / النوع:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(ReportSourceCategory.values()) { cat ->
                                val isSelected = sourceCategoryFilter == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { sourceCategoryFilter = cat },
                                    label = { Text(cat.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = EmeraldPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Employee Filter Chips (Audit trail filter)
                    if (allEmployees.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "الموظف المسؤول:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    val isSelected = selectedEmployeeFilter == null
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedEmployeeFilter = null },
                                        label = { Text("الجميع", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                            selectedLabelColor = EmeraldPrimary
                                        )
                                    )
                                }
                                items(allEmployees) { emp ->
                                    val isSelected = selectedEmployeeFilter == emp
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedEmployeeFilter = if (isSelected) null else emp },
                                        label = { Text(emp, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSelected) EmeraldPrimary else Slate400)
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                            selectedLabelColor = EmeraldPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sub Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        // Active Tab Content
        when (selectedTab) {
            0 -> {
                // Tab 0: Sijil al-3amaliyat (Detailed Audit Log of movements)
                if (filteredLogs.isEmpty()) {
                    item {
                        EmptyReportPlaceholder("لا توجد حركات مسجلة تطابق خيارات التصفية الحالية.")
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سجل العمليات والتوثيق (${filteredLogs.size} حركة)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "موثقة باسم الموظف والوقت",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(filteredLogs, key = { it.id }) { log ->
                        AuditMovementCard(
                            log = log,
                            partCost = partCostMap[log.barcode.lowercase().trim()] ?: 0.0
                        )
                    }
                }
            }

            1 -> {
                // Tab 1: Daily Consumed Parts in Maintenance
                if (consumedLogs.isEmpty()) {
                    item {
                        EmptyReportPlaceholder("لا توجد قطع غيار مستهلكة في صيانة الأجهزة خلال الفترة المحددة.")
                    }
                } else {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("إجمالي المنصرف لصيانة الأجهزة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    Text("تم صرفها وتركيبها بواسطة الفنيين في أوامر الصيانة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$totalConsumedQty قطعة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    Text("≈ ${totalConsumedCost.toInt()} ج.م", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    groupedByDay.forEach { (dateStr, dayLogs) ->
                        val dayQty = dayLogs.sumOf { it.quantity }
                        val dayCost = dayLogs.sumOf { (partCostMap[it.barcode.lowercase().trim()] ?: 150.0) * it.quantity }

                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Day Header
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
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                            }
                                            Column {
                                                Text(
                                                    text = if (dateStr == todayDateStr) "اليوم ($dateStr)" else if (dateStr == yesterdayDateStr) "أمس ($dateStr)" else dateStr,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text("${dayLogs.size} عمليات صرف وتركيب", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("$dayQty قطعة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                            Text("القيمة: ${dayCost.toInt()} ج.م", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    // List of Items consumed in this Day
                                    dayLogs.forEach { log ->
                                        val cost = partCostMap[log.barcode.lowercase().trim()] ?: 0.0
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(log.part_name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("باركود: ${log.barcode}", fontSize = 10.sp, color = EmeraldPrimary)
                                                    if (!log.device_ticket.isNullOrBlank()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(EmeraldDark.copy(alpha = 0.2f))
                                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("تذكرة ${log.device_ticket}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimaryLight)
                                                        }
                                                    }
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                                    Text("الفني / الموظف المسؤول: ${log.employee_name}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(RedAccent.copy(alpha = 0.12f))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("-${log.quantity} ق", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RedAccent)
                                                }
                                                if (cost > 0) {
                                                    Text("${(cost * log.quantity).toInt()} ج.م", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Tab 2: Source Breakdown (المخزن الرئيسي vs الموردين الخارجيين)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "مقارنة مصادر التوريد والاستلام",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Visual Progress Bar Comparison
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(EmeraldPrimary))
                                        Text("المخزن الرئيسي: $warehouseTotalQty ق ($warehousePercent%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(StatusBlue))
                                        Text("موردون خارجيون: $supplierTotalQty ق ($supplierPercent%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { warehousePercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = EmeraldPrimary,
                                    trackColor = StatusBlue
                                )
                            }
                        }
                    }
                }

                // Section 1: Main Warehouse Breakdown
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Warehouse, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text("الوارد من المخزن الرئيسي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                        Text("تحويلات من إدارة المخازن المركزية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("$warehouseTotalQty قطعة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }

                            if (warehouseLogs.isEmpty()) {
                                Text("لا توجد واردات مسجلة من المخزن الرئيسي في هذه الفترة.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                warehouseLogs.forEach { log ->
                                    AuditLogRowCompact(log = log, badgeColor = EmeraldPrimary)
                                }
                            }
                        }
                    }
                }

                // Section 2: External Suppliers Breakdown
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StatusBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(StatusBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = StatusBlue, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text("الوارد من الموردين الخارجيين", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusBlue)
                                        Text("${groupedSuppliers.size} موردين معتمدين", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("$supplierTotalQty قطعة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusBlue)
                            }

                            if (supplierLogs.isEmpty()) {
                                Text("لا توجد واردات مسجلة من موردين خارجيين في هذه الفترة.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                groupedSuppliers.forEach { (supName, logs) ->
                                    val supQty = logs.sumOf { it.quantity }
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(supName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                Text("$supQty قطعة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusBlue)
                                            }
                                            logs.forEach { log ->
                                                AuditLogRowCompact(log = log, badgeColor = StatusBlue)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Tab 3: Employee Accountability & Audit (مسؤولية وتدقيق الموظفين)
                if (groupedByEmployee.isEmpty()) {
                    item {
                        EmptyReportPlaceholder("لا توجد عمليات مسجلة للموظفين في هذه الفترة.")
                    }
                } else {
                    item {
                        Text(
                            text = "تقرير تدقيق الموظفين ومسؤولية الحركات (${groupedByEmployee.size} موظفين)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    groupedByEmployee.forEach { (empName, empLogs) ->
                        val receivedQty = empLogs.filter { it.type in listOf("inward_warehouse", "inward_supplier") }.sumOf { it.quantity }
                        val empConsumedQty = empLogs.filter { it.type == "consumed_repair" }.sumOf { it.quantity }
                        val returnedQty = empLogs.filter { it.type in listOf("return_warehouse", "return_supplier", "defective_damaged") }.sumOf { it.quantity }

                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Employee Header
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
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(EmeraldPrimary.copy(alpha = 0.8f), EmeraldDark)
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = empName.take(1).uppercase(),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = empName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${empLogs.size} حركات موثقة بالنظام",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(EmeraldPrimary.copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("موثق ومعتمد", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                        }
                                    }

                                    // Metric breakdown for this employee
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        MiniEmpStatBox(
                                            modifier = Modifier.weight(1f),
                                            label = "استلام وارد",
                                            count = "$receivedQty ق",
                                            color = EmeraldPrimary
                                        )
                                        MiniEmpStatBox(
                                            modifier = Modifier.weight(1f),
                                            label = "صرف صيانة",
                                            count = "$empConsumedQty ق",
                                            color = RedAccent
                                        )
                                        MiniEmpStatBox(
                                            modifier = Modifier.weight(1f),
                                            label = "فائض ومرتجع",
                                            count = "$returnedQty ق",
                                            color = StatusAmber
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    // Recent operations by this employee
                                    Text("آخر العمليات المنفذة بواسطة $empName:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    empLogs.take(5).forEach { log ->
                                        AuditLogRowCompact(log = log, badgeColor = if (log.type == "consumed_repair") RedAccent else EmeraldPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    iconTint: Color,
    backgroundGradient: List<Color>
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(backgroundGradient))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                    }
                }
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subValue, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = iconTint)
            }
        }
    }
}

@Composable
fun AuditMovementCard(
    log: PartMovementLog,
    partCost: Double
) {
    val (typeColor, typeBg) = when (log.type) {
        "consumed_repair" -> RedAccent to RedAccent.copy(alpha = 0.12f)
        "inward_warehouse" -> EmeraldPrimary to EmeraldPrimary.copy(alpha = 0.12f)
        "inward_supplier" -> StatusBlue to StatusBlue.copy(alpha = 0.12f)
        "return_warehouse" -> EmeraldPrimary to EmeraldPrimary.copy(alpha = 0.12f)
        "defective_damaged" -> StatusAmber to StatusAmber.copy(alpha = 0.12f)
        "return_supplier" -> RedAccent to RedAccent.copy(alpha = 0.12f)
        else -> Slate400 to Slate400.copy(alpha = 0.12f)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Type Badge + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = log.typeArabicLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Slate400, modifier = Modifier.size(12.dp))
                    Text(log.timestamp, fontSize = 10.sp, color = Slate400)
                }
            }

            // Part Name and Barcode + Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(log.part_name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("باركود: ${log.barcode}", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                        if (!log.device_ticket.isNullOrBlank()) {
                            Text("• تذكرة: ${log.device_ticket}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${log.quantity} قطعة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                    if (partCost > 0) {
                        Text(
                            text = "≈ ${(partCost * log.quantity).toInt()} ج.م",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            // Footer: Source/Destination + Responsible Employee (Documented)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source / Destination info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (log.type == "inward_supplier") Icons.Default.LocalShipping else Icons.Default.Warehouse,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "الجهة: ${log.source_destination}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Responsible Employee Capsule
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                    Text(
                        text = "المسؤول: ${log.employee_name}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!log.notes.isNullOrBlank()) {
                Text(
                    text = "ملاحظات: ${log.notes}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AuditLogRowCompact(
    log: PartMovementLog,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(log.part_name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(log.barcode, fontSize = 9.sp, color = EmeraldPrimary)
                Text("• المسؤول: ${log.employee_name}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("+${log.quantity} ق", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor)
            Text(log.timestamp.takeLast(5), fontSize = 9.sp, color = Slate400)
        }
    }
}

@Composable
fun MiniEmpStatBox(
    modifier: Modifier = Modifier,
    label: String,
    count: String,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(count, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun EmptyReportPlaceholder(message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
