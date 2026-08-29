package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.data.model.InventoryPart
import com.example.data.model.PartMovementLog
import com.example.data.model.PartMovementType
import com.example.data.model.SessionUser
import com.example.ui.dialogs.CameraBarcodeScannerDialog
import com.example.ui.dialogs.ExcelInventoryDialog
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InventoryBarcodeScreen(
    session: SessionUser,
    partsList: List<InventoryPart>,
    movementLogs: List<PartMovementLog>,
    onReceivePart: (barcode: String, name: String, category: String, qty: Int, cost: Double, source: String, employee: String) -> Unit,
    onReturnSurplusToWarehouse: (barcode: String, qty: Int, employee: String, notes: String?) -> Unit,
    onLogDefectiveOrReturnSupplier: (barcode: String, qty: Int, type: String, reason: String, destination: String, employee: String) -> Unit,
    onImportParts: (List<InventoryPart>, Boolean) -> Unit = { _, _ -> },
    onOpenReports: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(0) } // 0: استلام بضاعة بالباركود, 1: رد زيادة للمخزن, 2: مرتجع وتوالف, 3: تقرير حركة اليوم
    var barcodeInput by remember { mutableStateOf("") }
    var partNameInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("شاشات") }
    var qtyInput by remember { mutableStateOf("1") }
    var costInput by remember { mutableStateOf("100") }
    var sourceInput by remember { mutableStateOf("المخزن الرئيسي") }
    var employeeInput by remember { mutableStateOf(session.displayName) }
    var notesInput by remember { mutableStateOf("") }
    var defectType by remember { mutableStateOf("defective_damaged") } // "defective_damaged" or "return_supplier"

    var showCameraScanner by remember { mutableStateOf(false) }
    var showExcelDialog by remember { mutableStateOf(false) }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    // Auto-fill part name if barcode matches existing stock
    fun onBarcodeChanged(newBarcode: String) {
        barcodeInput = newBarcode
        val existing = partsList.find { it.barcode.equals(newBarcode.trim(), ignoreCase = true) }
        if (existing != null) {
            partNameInput = existing.name
            categoryInput = existing.category
            costInput = existing.unit_cost.toInt().toString()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("inventory_barcode_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
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
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "إدارة بضاعة وقطع الغيار بالباركود",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "استلام الوارد، رد الزيادة نهاية اليوم، وتسجيل التوالف والمرتجع",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showExcelDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("open_excel_dialog_btn")
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إكسيل Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onOpenReports,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                                modifier = Modifier.testTag("open_inventory_reports_btn")
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("التقارير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Navigation Tabs
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("📥 استلام وارد", fontSize = 11.sp, fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("📤 رد للمخزن", fontSize = 11.sp, fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("⚠️ توالف ومرتجع", fontSize = 11.sp, fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = activeTab == 3,
                            onClick = { activeTab = 3 },
                            text = { Text("📊 تقرير اليوم", fontSize = 11.sp, fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }

        // TAB 0: Receive Inward Parts by Barcode
        if (activeTab == 0) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📥 استلام بضاعة وقطع غيار جديدة (بالباركود فقط)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Barcode Scan / Input Field
                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { onBarcodeChanged(it) },
                            label = { Text("باركود القطعة *") },
                            placeholder = { Text("امسح الباركود أو اكتب: SCR-IP13-ORG") },
                            leadingIcon = {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = EmeraldPrimary)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showCameraScanner = true },
                                    modifier = Modifier.testTag("scan_barcode_camera_btn_tab0")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "تشغيل الكاميرا للتصوير ومسح الباركود",
                                        tint = EmeraldPrimary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Part Name
                        OutlinedTextField(
                            value = partNameInput,
                            onValueChange = { partNameInput = it },
                            label = { Text("اسم وتفاصيل قطعة الغيار *") },
                            placeholder = { Text("مثال: شاشة ايفون 13 أصلية OLED") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Category selection
                        Text("نوع القطعة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("شاشات", "بطاريات", "آيسيهات", "فلاتات", "باغات", "أخرى").forEach { cat ->
                                FilterChip(
                                    selected = categoryInput == cat,
                                    onClick = { categoryInput = cat },
                                    label = { Text(cat, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = EmeraldPrimary
                                    )
                                )
                            }
                        }

                        // Qty & Unit Cost
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = qtyInput,
                                onValueChange = { qtyInput = it },
                                label = { Text("الكمية المستلمة *") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = costInput,
                                onValueChange = { costInput = it },
                                label = { Text("سعر التكلفة (ج)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Source: Warehouse vs Supplier
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("المخزن الرئيسي", "مورد خارجي (العالمية)", "مورد النخبة للقطع").forEach { src ->
                                FilterChip(
                                    selected = sourceInput == src,
                                    onClick = { sourceInput = src },
                                    label = { Text(src, fontSize = 10.sp) }
                                )
                            }
                        }

                        // Mandatory Employee Name
                        OutlinedTextField(
                            value = employeeInput,
                            onValueChange = { employeeInput = it },
                            label = { Text("اسم موظف الاستلام المسؤول *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Submit Button
                        Button(
                            onClick = {
                                val q = qtyInput.toIntOrNull() ?: 1
                                val c = costInput.toDoubleOrNull() ?: 100.0
                                onReceivePart(
                                    barcodeInput.trim(),
                                    partNameInput.trim().ifBlank { "قطعة غيار $barcodeInput" },
                                    categoryInput,
                                    q,
                                    c,
                                    sourceInput,
                                    employeeInput.trim().ifBlank { session.displayName }
                                )
                                barcodeInput = ""
                                partNameInput = ""
                                qtyInput = "1"
                            },
                            enabled = barcodeInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_confirm_receive_part"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تأكيد استلام البضاعة وإضافتها للرصيد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // TAB 1: Return Surplus to Warehouse at End of Day
        if (activeTab == 1) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📤 رد الزيادة إلى المخزن الرئيسي في نهاية اليوم",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "امسح باركود قطع الغيار المتبقية غير المستهلكة لإعادتها إلى المخزن وتصفير عهدة اليوم بدقة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { onBarcodeChanged(it) },
                            label = { Text("باركود القطعة المراد ردها *") },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = StatusAmber) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showCameraScanner = true },
                                    modifier = Modifier.testTag("scan_barcode_camera_btn_tab1")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "تشغيل الكاميرا للتصوير ومسح الباركود",
                                        tint = StatusAmber
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (partNameInput.isNotBlank()) {
                            Text("القطعة المحددة: $partNameInput", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }

                        OutlinedTextField(
                            value = qtyInput,
                            onValueChange = { qtyInput = it },
                            label = { Text("الكمية المرتجعة للمخزن *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = employeeInput,
                            onValueChange = { employeeInput = it },
                            label = { Text("اسم الموظف المسلم للمخزن *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("ملاحظات الرد (اختياري)") },
                            placeholder = { Text("مثال: فائض صيانة نهاية اليوم") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val q = qtyInput.toIntOrNull() ?: 1
                                onReturnSurplusToWarehouse(
                                    barcodeInput.trim(),
                                    q,
                                    employeeInput.trim().ifBlank { session.displayName },
                                    notesInput.ifBlank { "رد زيادة نهاية اليوم" }
                                )
                                barcodeInput = ""
                                partNameInput = ""
                                qtyInput = "1"
                                notesInput = ""
                            },
                            enabled = barcodeInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_confirm_return_surplus"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusAmber,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.AssignmentReturn, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تأكيد رد الزيادة إلى المخزن", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // TAB 2: Defective / Damaged & Supplier Returns
        if (activeTab == 2) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⚠️ تسجيل المرتجعات والتوالف بالباركود",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Choice: Defective in Workshop vs Return to Supplier
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = defectType == "defective_damaged",
                                onClick = { defectType = "defective_damaged" },
                                label = { Text("💥 توالف صيانة / كسر / تلف", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedAccent.copy(alpha = 0.2f),
                                    selectedLabelColor = RedAccent
                                )
                            )
                            FilterChip(
                                selected = defectType == "return_supplier",
                                onClick = { defectType = "return_supplier" },
                                label = { Text("🔄 مرتجع للمورد (عيب تصنيع)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StatusBlue.copy(alpha = 0.2f),
                                    selectedLabelColor = StatusBlue
                                )
                            )
                        }

                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { onBarcodeChanged(it) },
                            label = { Text("باركود القطعة التالفة / المرتجعة *") },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = RedAccent) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showCameraScanner = true },
                                    modifier = Modifier.testTag("scan_barcode_camera_btn_tab2")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "تشغيل الكاميرا للتصوير ومسح الباركود",
                                        tint = RedAccent
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (partNameInput.isNotBlank()) {
                            Text("القطعة: $partNameInput", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = qtyInput,
                            onValueChange = { qtyInput = it },
                            label = { Text("الكمية *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("سبب التلف أو الإرجاع *") },
                            placeholder = { Text("مثال: خطوط بالشاشة / لا تقبل الشحن / كسر فلاتة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = employeeInput,
                            onValueChange = { employeeInput = it },
                            label = { Text("اسم الموظف المسؤول عن الإرجاع/التسليم *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val q = qtyInput.toIntOrNull() ?: 1
                                onLogDefectiveOrReturnSupplier(
                                    barcodeInput.trim(),
                                    q,
                                    defectType,
                                    notesInput.trim().ifBlank { "تالف أو مرتجع" },
                                    if (defectType == "return_supplier") "المورد الخارجي" else "توالف الورشة",
                                    employeeInput.trim().ifBlank { session.displayName }
                                )
                                barcodeInput = ""
                                partNameInput = ""
                                qtyInput = "1"
                                notesInput = ""
                            },
                            enabled = barcodeInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_confirm_defect_log"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تأكيد تسجيل المرتجع / التوالف", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // TAB 3: Daily Movement, Consumption & Audit Trail Report
        if (activeTab == 3) {
            // KPI Summary Cards
            item {
                val totalInward = movementLogs.filter { it.type.startsWith("inward") }.sumOf { it.quantity }
                val totalConsumed = movementLogs.filter { it.type == "consumed_repair" }.sumOf { it.quantity }
                val totalSurplusReturned = movementLogs.filter { it.type == "return_warehouse" }.sumOf { it.quantity }
                val totalDamaged = movementLogs.filter { it.type == "defective_damaged" || it.type == "return_supplier" }.sumOf { it.quantity }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📊 ملخص حركة قطع الغيار لليوم ($todayDateStr):",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Received
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("الوارد اليوم", fontSize = 10.sp, color = EmeraldPrimary)
                                Text("$totalInward ق", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }
                        }

                        // Consumed in Repairs
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = StatusBlue.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("المستهلك بالصيانة", fontSize = 10.sp, color = StatusBlue)
                                Text("$totalConsumed ق", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StatusBlue)
                            }
                        }

                        // Returned to Warehouse
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = StatusAmber.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("المرتجع للمخزن", fontSize = 10.sp, color = StatusAmber)
                                Text("$totalSurplusReturned ق", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StatusAmber)
                            }
                        }

                        // Damaged
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = RedAccent.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("التوالف والمرتجع", fontSize = 10.sp, color = RedAccent)
                                Text("$totalDamaged ق", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RedAccent)
                            }
                        }
                    }
                }
            }

            // Movement Ledger
            item {
                Text(
                    text = "سجل الحركات التفصيلي اليوم (${movementLogs.size}):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (movementLogs.isEmpty()) {
                item {
                    Text(
                        text = "لا توجد حركات مسجلة بعد اليوم.",
                        fontSize = 12.sp,
                        color = Slate400,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(movementLogs) { log ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (log.type) {
                                                    "inward_warehouse", "inward_supplier" -> EmeraldPrimary.copy(alpha = 0.15f)
                                                    "consumed_repair" -> StatusBlue.copy(alpha = 0.15f)
                                                    "return_warehouse" -> StatusAmber.copy(alpha = 0.15f)
                                                    else -> RedAccent.copy(alpha = 0.15f)
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = log.typeArabicLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (log.type) {
                                                "inward_warehouse", "inward_supplier" -> EmeraldPrimary
                                                "consumed_repair" -> StatusBlue
                                                "return_warehouse" -> StatusAmber
                                                else -> RedAccent
                                            }
                                        )
                                    }

                                    Text(
                                        text = "${log.part_name} (${log.barcode})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "${log.quantity} قطعة",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "👤 الموظف المسؤول: ${log.employee_name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = log.timestamp,
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }

                            if (!log.notes.isNullOrBlank() || !log.source_destination.isNullOrBlank()) {
                                Text(
                                    text = "📌 ${log.source_destination} ${if (!log.notes.isNullOrBlank()) "- ${log.notes}" else ""}",
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCameraScanner) {
        CameraBarcodeScannerDialog(
            existingParts = partsList,
            onBarcodeScanned = { barcode ->
                onBarcodeChanged(barcode)
                showCameraScanner = false
            },
            onDismiss = { showCameraScanner = false }
        )
    }

    if (showExcelDialog) {
        ExcelInventoryDialog(
            partsList = partsList,
            onImportParts = onImportParts,
            onDismiss = { showExcelDialog = false }
        )
    }
}
