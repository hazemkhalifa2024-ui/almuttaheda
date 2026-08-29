package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.data.model.PrinterConfig
import com.example.data.printer.NetworkPrinterService
import com.example.ui.components.BarcodeView
import com.example.ui.components.QrCodeView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.Slate400
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterConfigScreen(
    currentConfig: PrinterConfig,
    onSaveConfig: (PrinterConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedSectionTab by remember { mutableIntStateOf(0) }

    // Shop Info
    var shopName by remember(currentConfig) { mutableStateOf(currentConfig.shopName) }
    var shopPhone by remember(currentConfig) { mutableStateOf(currentConfig.shopPhone) }
    var footerNote by remember(currentConfig) { mutableStateOf(currentConfig.footerNote) }

    // Printer 1: Customer Receipt
    var receiptPrinterName by remember(currentConfig) { mutableStateOf(currentConfig.receiptPrinterName) }
    var receiptPrinterIp by remember(currentConfig) { mutableStateOf(currentConfig.receiptPrinterIp) }
    var receiptPaperWidth by remember(currentConfig) { mutableStateOf(currentConfig.receiptPaperWidth) }
    var autoPrintReceiptOnIntake by remember(currentConfig) { mutableStateOf(currentConfig.autoPrintReceiptOnIntake) }

    // Printer 2: Barcode Sticker
    var stickerPrinterName by remember(currentConfig) { mutableStateOf(currentConfig.stickerPrinterName) }
    var stickerPrinterIp by remember(currentConfig) { mutableStateOf(currentConfig.stickerPrinterIp) }
    var stickerWidth by remember(currentConfig) { mutableStateOf(currentConfig.stickerWidth) }
    var autoPrintStickerOnIntake by remember(currentConfig) { mutableStateOf(currentConfig.autoPrintStickerOnIntake) }

    // Master Dual Print
    var dualAutoPrintOnIntake by remember(currentConfig) { mutableStateOf(currentConfig.dualAutoPrintOnIntake) }

    val paperOptions = listOf(
        "80mm" to "طابعة حرارية عريضة 80mm (ESC/POS القياسية)",
        "58mm" to "طابعة حرارية مدمجة 58mm (بلوتوث/محمولة)"
    )
    var isPaperDropdownExpanded by remember { mutableStateOf(false) }

    val stickerOptions = listOf(
        "50mm x 30mm" to "ملصق باركود قياسي (50x30mm) - الأفضل للهواتف",
        "40mm x 30mm" to "ملصق باركود مدمج (40x30mm)",
        "40mm x 25mm" to "ملصق باركود صغير (40x25mm)"
    )
    var isStickerDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("printer_config_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Header
        item {
            Column {
                Text(
                    text = "إعدادات الطابعتين الحراريتين والباركود",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "ضبط طابعة ريسيت العميل وطابعة ملصقات الباركود والطباعة المزدوجة الفورية عند الاستلام",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Master Dual Auto-Print Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (dualAutoPrintOnIntake) Color(0x1510B981) else MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (dualAutoPrintOnIntake) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (dualAutoPrintOnIntake) EmeraldPrimary else Slate400.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = if (dualAutoPrintOnIntake) Color.White else Slate400)
                        }
                        Column {
                            Text(
                                text = "الطباعة المزدوجة التلقائية فور الاستلام",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "عند حفظ استلام الجهاز تطبع الطابعتان فوراً (الريسيت + الاستيكر) معاً",
                                fontSize = 11.sp,
                                color = if (dualAutoPrintOnIntake) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = dualAutoPrintOnIntake,
                        onCheckedChange = { dualAutoPrintOnIntake = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }
            }
        }

        // Tabs to switch between Printer 1, Printer 2, and Shop Info
        item {
            TabRow(
                selectedTabIndex = selectedSectionTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = EmeraldPrimary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSectionTab == 0,
                    onClick = { selectedSectionTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("1. طابعة الريسيت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedSectionTab == 1,
                    onClick = { selectedSectionTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("2. طابعة الاستيكر", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedSectionTab == 2,
                    onClick = { selectedSectionTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("بيانات الورشة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        // Tab Content
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedSectionTab) {
                        0 -> {
                            // PRINTER 1: RECEIPT
                            Text(
                                text = "إعدادات الطابعة الأولى: إيصال العميل الحراري (Receipt)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )

                            // Printer 1 Name
                            Column {
                                Text("اسم طابعة الريسيت / المعرف", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = receiptPrinterName,
                                    onValueChange = { receiptPrinterName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = EmeraldPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Printer 1 IP / Port
                            Column {
                                Text("عنوان IP أو عنوان البلوتوث للطابعة", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = receiptPrinterIp,
                                    onValueChange = { receiptPrinterIp = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Router, contentDescription = null, tint = EmeraldPrimary) },
                                    placeholder = { Text("مثال: 192.168.1.100 أو BT:00:11:22:33:44") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Paper Size
                            Column {
                                Text("مقاس ورق الريسيت", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                ExposedDropdownMenuBox(
                                    expanded = isPaperDropdownExpanded,
                                    onExpandedChange = { isPaperDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = paperOptions.find { it.first == receiptPaperWidth }?.second ?: receiptPaperWidth,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaperDropdownExpanded) },
                                        leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = EmeraldPrimary) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isPaperDropdownExpanded,
                                        onDismissRequest = { isPaperDropdownExpanded = false }
                                    ) {
                                        paperOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt.second) },
                                                onClick = {
                                                    receiptPaperWidth = opt.first
                                                    isPaperDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Auto Print Switch for Receipt
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("طباعة الريسيت تلقائياً", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("إصدار إيصال للعميل فور حفظ الجهاز", fontSize = 11.sp, color = Slate400)
                                }
                                Switch(
                                    checked = autoPrintReceiptOnIntake,
                                    onCheckedChange = { autoPrintReceiptOnIntake = it }
                                )
                            }

                            // Test Print Button for Receipt
                            OutlinedButton(
                                onClick = {
                                    if (receiptPrinterIp.isBlank()) {
                                        Toast.makeText(context, "الرجاء إدخال عنوان IP الصحيح أولاً", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "جاري الاتصال بالطابعة وإرسال الإيصال التجريبي...", Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            val dummyDevice = Device(
                                                id = 1234L,
                                                customer_name = "عميل تجريبي",
                                                customer_phone = "01001234567",
                                                device_name = "سامسونج جالاكسي S23",
                                                issue_description = "شاشة مكسورة وبطارية منتفخة لا تشحن",
                                                estimated_cost = "1850",
                                                technician = "tech1",
                                                due_date = "2026-09-01",
                                                status = "intake",
                                                created_at = "2026-08-28"
                                            )
                                            val activeConfig = PrinterConfig(
                                                shopName = shopName,
                                                shopPhone = shopPhone,
                                                footerNote = footerNote,
                                                receiptPrinterName = receiptPrinterName,
                                                receiptPrinterIp = receiptPrinterIp,
                                                receiptPaperWidth = receiptPaperWidth,
                                                autoPrintReceiptOnIntake = autoPrintReceiptOnIntake,
                                                stickerPrinterName = stickerPrinterName,
                                                stickerPrinterIp = stickerPrinterIp,
                                                stickerWidth = stickerWidth,
                                                autoPrintStickerOnIntake = autoPrintStickerOnIntake
                                            )
                                            val result = NetworkPrinterService.printReceipt(
                                                ipAddress = receiptPrinterIp,
                                                device = dummyDevice,
                                                config = activeConfig
                                            )
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "تمت الطباعة التجريبية بنجاح! تم إرسال الإيصال إلى $receiptPrinterName ($receiptPrinterIp)", Toast.LENGTH_LONG).show()
                                            } else {
                                                val errMsg = result.exceptionOrNull()?.message ?: "خطأ غير معروف"
                                                Toast.makeText(context, "فشلت الطباعة! تأكد من الاتصال بـ $receiptPrinterIp.\nالخطأ: $errMsg", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة تجريبية لإيصال العميل (Test Receipt)")
                            }
                        }

                        1 -> {
                            // PRINTER 2: STICKER
                            Text(
                                text = "إعدادات الطابعة الثانية: ملصق باركود الجهاز (Sticker)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusBlue
                            )

                            // Printer 2 Name
                            Column {
                                Text("اسم طابعة الاستيكر / الموديل (TSPL / ESC)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = stickerPrinterName,
                                    onValueChange = { stickerPrinterName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = StatusBlue) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Printer 2 IP
                            Column {
                                Text("عنوان IP أو عنوان البلوتوث لطابعة الاستيكر", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = stickerPrinterIp,
                                    onValueChange = { stickerPrinterIp = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Router, contentDescription = null, tint = StatusBlue) },
                                    placeholder = { Text("مثال: 192.168.1.101 أو BT:00:11:22:33:45") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Sticker Size
                            Column {
                                Text("أبعاد ملصق الباركود (الاستيكر)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                ExposedDropdownMenuBox(
                                    expanded = isStickerDropdownExpanded,
                                    onExpandedChange = { isStickerDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = stickerOptions.find { it.first == stickerWidth }?.second ?: stickerWidth,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStickerDropdownExpanded) },
                                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = StatusBlue) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isStickerDropdownExpanded,
                                        onDismissRequest = { isStickerDropdownExpanded = false }
                                    ) {
                                        stickerOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt.second) },
                                                onClick = {
                                                    stickerWidth = opt.first
                                                    isStickerDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Auto Print Switch for Sticker
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("طباعة ملصق الباركود تلقائياً", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("إصدار ملصق بالباركود للصق على الهاتف فور الاستلام", fontSize = 11.sp, color = Slate400)
                                }
                                Switch(
                                    checked = autoPrintStickerOnIntake,
                                    onCheckedChange = { autoPrintStickerOnIntake = it }
                                )
                            }

                            // Test Print Button for Sticker
                            OutlinedButton(
                                onClick = {
                                    if (stickerPrinterIp.isBlank()) {
                                        Toast.makeText(context, "الرجاء إدخال عنوان IP الصحيح أولاً", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "جاري الاتصال بالطابعة وإرسال ملصق تجريبي...", Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            val dummyDevice = Device(
                                                id = 1234L,
                                                customer_name = "عميل تجريبي",
                                                customer_phone = "01001234567",
                                                device_name = "سامسونج جالاكسي S23",
                                                issue_description = "شاشة مكسورة وبطارية منتفخة لا تشحن",
                                                estimated_cost = "1850",
                                                technician = "tech1",
                                                due_date = "2026-09-01",
                                                status = "intake",
                                                created_at = "2026-08-28"
                                            )
                                            val activeConfig = PrinterConfig(
                                                shopName = shopName,
                                                shopPhone = shopPhone,
                                                footerNote = footerNote,
                                                receiptPrinterName = receiptPrinterName,
                                                receiptPrinterIp = receiptPrinterIp,
                                                receiptPaperWidth = receiptPaperWidth,
                                                autoPrintReceiptOnIntake = autoPrintReceiptOnIntake,
                                                stickerPrinterName = stickerPrinterName,
                                                stickerPrinterIp = stickerPrinterIp,
                                                stickerWidth = stickerWidth,
                                                autoPrintStickerOnIntake = autoPrintStickerOnIntake
                                            )
                                            val result = NetworkPrinterService.printSticker(
                                                ipAddress = stickerPrinterIp,
                                                device = dummyDevice,
                                                config = activeConfig
                                            )
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "تمت الطباعة التجريبية بنجاح! تم إرسال ملصق تجريبي إلى $stickerPrinterName ($stickerPrinterIp)", Toast.LENGTH_LONG).show()
                                            } else {
                                                val errMsg = result.exceptionOrNull()?.message ?: "خطأ غير معروف"
                                                Toast.makeText(context, "فشلت طباعة الملصق! تأكد من الاتصال بـ $stickerPrinterIp.\nالخطأ: $errMsg", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة تجريبية لملصق الباركود (Test Sticker)")
                            }
                        }

                        2 -> {
                            // SHOP & FOOTER INFO
                            Text(
                                text = "بيانات المحل والترويسة للفواتير والإيصالات",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )

                            // Shop Name
                            Column {
                                Text("اسم الورشة / المحل", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = shopName,
                                    onValueChange = { shopName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = EmeraldPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Shop Phone
                            Column {
                                Text("رقم هاتف الورشة للعملاء", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = shopPhone,
                                    onValueChange = { shopPhone = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Footer Note
                            Column {
                                Text("شروط الضمان والملاحظات أسفل الريسيت", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = footerNote,
                                    onValueChange = { footerNote = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    maxLines = 3,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Master Save Button
                    Button(
                        onClick = {
                            val newConfig = PrinterConfig(
                                shopName = shopName,
                                shopPhone = shopPhone,
                                footerNote = footerNote,
                                receiptPrinterName = receiptPrinterName,
                                receiptPrinterIp = receiptPrinterIp,
                                receiptPaperWidth = receiptPaperWidth,
                                autoPrintReceiptOnIntake = autoPrintReceiptOnIntake,
                                stickerPrinterName = stickerPrinterName,
                                stickerPrinterIp = stickerPrinterIp,
                                stickerWidth = stickerWidth,
                                autoPrintStickerOnIntake = autoPrintStickerOnIntake,
                                dualAutoPrintOnIntake = dualAutoPrintOnIntake
                            )
                            onSaveConfig(newConfig)
                            Toast.makeText(context, "تم حفظ إعدادات الطابعتين بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_printer_config_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وتثبيت إعدادات الطابعتين", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Preview Title
        item {
            Text(
                text = "معاينة حية لشكل الطباعة:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Live Preview Box (Receipt or Sticker based on selected tab)
        item {
            if (selectedSectionTab == 1) {
                // Sticker Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFF64748B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(shopName.ifBlank { "المتحدة للصيانة" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("MUT-101", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color.Black)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("العميل: أحمد محمود", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("الجهاز: Samsung S23 Ultra", fontSize = 10.sp, color = Color.Black)
                                Text("العطل: تغيير شاشة", fontSize = 9.sp, color = Color.DarkGray)
                            }
                            QrCodeView(content = "MUT-101", size = 46.dp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        BarcodeView(content = "MUT-101", height = 28.dp, modifier = Modifier.fillMaxWidth())
                        Text("MUT-101 • $stickerWidth", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            } else {
                // Receipt Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = shopName.ifBlank { "المتحدة للصيانة" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "هاتف: ${shopPhone.ifBlank { "-" }}",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "تذكرة صيانة: MUT-101",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.Black
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("العميل: أحمد محمود", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                                Text("01012345678", fontSize = 11.sp, color = Color.DarkGray)
                            }
                            Text("الجهاز: Samsung S23 Ultra", fontSize = 11.sp, color = Color.Black)
                            Text("العطل: تغيير شاشة أصلية + صيانة سوكت", fontSize = 10.sp, color = Color.DarkGray)
                            Text("التكلفة: 2500 ج.م", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.Black
                        )

                        QrCodeView(content = "MUT-101", size = 70.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        BarcodeView(content = "MUT-101", height = 34.dp, modifier = Modifier.width(170.dp))

                        Text(
                            text = footerNote.ifBlank { "شكراً لزيارتكم الورشة" },
                            fontSize = 9.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
