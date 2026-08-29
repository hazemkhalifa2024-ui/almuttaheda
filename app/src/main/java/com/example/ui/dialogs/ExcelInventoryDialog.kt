package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InventoryPart
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedAccent
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue

@Composable
fun ExcelInventoryDialog(
    partsList: List<InventoryPart>,
    onImportParts: (List<InventoryPart>, replaceAll: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: استيراد من إكسيل, 1: تصدير إلى إكسيل
    var importCsvText by remember { mutableStateOf("") }
    var replaceAllMode by remember { mutableStateOf(false) }

    // Sample Excel template text
    val sampleTemplateCsv = remember {
        """
الباركود,اسم القطعة,التصنيف,موديل الجهاز,سعر التكلفة,سعر البيع,الرصيد المخزني,حد التنبيه
SCR-IP13-ORG,شاشة iPhone 13 أصلية OLED,شاشات,iPhone 13,1200,1500,10,2
BAT-SAM-A54,بطارية Samsung A54 5000mAh,بطاريات,Samsung A54,350,500,15,3
CHG-TYPC-FLEX,فلاتة شحن Type-C متعددة,فلاتات,عام,80,150,25,5
IC-PWR-PM8150,آيسي باور PM8150,آيسيهات,Xiaomi/Poco,250,400,8,2
GLS-IP11-FRT,باغة شاشة iPhone 11,باغات,iPhone 11,100,200,20,4
MIC-UNIV-SGL,مايك صوت ديجيتال مسطح,مايكات,عام,35,80,30,5
CAM-IP12-MAIN,كاميرا خلفية رئيسية iPhone 12,كاميرات,iPhone 12,850,1100,6,2
        """.trimIndent()
    }

    // Parse CSV Text into InventoryPart list
    val parsedParts = remember(importCsvText) {
        parseCsvToInventoryParts(importCsvText)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("excel_inventory_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header
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
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "استيراد وتصدير قطع الغيار بـ Excel / CSV",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "رفع قاعدة بيانات الأصناف دفعة واحدة لتسريع مسح الباركود",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("📥 رفع واستيراد من Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("📤 تصدير إلى Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // TAB 0: Import from Excel / CSV
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Action Bar: Paste / Template
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        if (text.isNotBlank()) {
                                            importCsvText = text
                                            Toast.makeText(context, "تم لصق البيانات من الحافظة", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("لصق من الحافظة", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    importCsvText = sampleTemplateCsv
                                    Toast.makeText(context, "تم تحميل نموذج إكسيل تجريبي جاهز", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تحميل نموذج جاهز", fontSize = 11.sp)
                            }
                        }

                        // Text Field for CSV input
                        OutlinedTextField(
                            value = importCsvText,
                            onValueChange = { importCsvText = it },
                            placeholder = {
                                Text(
                                    "الصق هنا محتوى ملف الإكسيل أو الـ CSV:\nالباركود, اسم القطعة, التصنيف, الموديل, سعر التكلفة, سعر البيع, الرصيد, حد التنبيه",
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Mode selector: Update vs Replace All
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("طريقة الاستيراد:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            FilterChip(
                                selected = !replaceAllMode,
                                onClick = { replaceAllMode = false },
                                label = { Text("دمج وتحديث المخزون", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = replaceAllMode,
                                onClick = { replaceAllMode = true },
                                label = { Text("استبدال كامل المخزون", fontSize = 10.sp) }
                            )
                        }

                        // Parsed Items Preview
                        if (parsedParts.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✅ تم استخراج ${parsedParts.size} صنف جاهز للحفظ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = "إجمالي القطع: ${parsedParts.sumOf { it.current_stock }}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldDark
                                    )
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(parsedParts) { part ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(part.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("🏷️ ${part.barcode} | 📂 ${part.category} | 📱 ${part.device_model}", fontSize = 10.sp, color = Slate400)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("تكلفة: ${part.unit_cost.toInt()} ج", fontSize = 11.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                                Text("بيع: ${part.selling_price.toInt()} ج | رصيد: ${part.current_stock}", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "قم بلصق بيانات ملف الإكسيل بالأعلى لمعاينة الأصناف قبل الحفظ",
                                    fontSize = 12.sp,
                                    color = Slate400
                                )
                            }
                        }

                        // Submit Bulk Import Button
                        Button(
                            onClick = {
                                if (parsedParts.isNotEmpty()) {
                                    onImportParts(parsedParts, replaceAllMode)
                                    onDismiss()
                                }
                            },
                            enabled = parsedParts.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_confirm_excel_import"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حفظ وإضافة جميع الأصناف (${parsedParts.size}) إلى المخزن فوراً",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // TAB 1: Export to Excel
                if (selectedTab == 1) {
                    val exportCsvContent = remember(partsList) {
                        generateExportCsv(partsList)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Summary Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("عدد الأصناف", fontSize = 10.sp, color = EmeraldPrimary)
                                    Text("${partsList.size} صنف", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusBlue.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("إجمالي القطع", fontSize = 10.sp, color = StatusBlue)
                                    Text("${partsList.sumOf { it.current_stock }} قطعة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StatusBlue)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusAmber.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("قيمة التكلفة", fontSize = 10.sp, color = StatusAmber)
                                    Text("${partsList.sumOf { it.unit_cost * it.current_stock }.toInt()} ج", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StatusAmber)
                                }
                            }
                        }

                        Text(
                            text = "معاينة ملف الإكسيل المُصدّر (CSV متوافق 100% مع Microsoft Excel واللغة العربية):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = exportCsvContent,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Action Buttons: Copy / Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Inventory CSV", exportCsvContent)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ بيانات الإكسيل للحافظة", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نسخ للإكسيل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "قاعدة بيانات قطع الغيار - الورشة المتحدة")
                                        putExtra(Intent.EXTRA_TEXT, exportCsvContent)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "تصدير ومشاركة ملف قطع الغيار"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة الملف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper to parse CSV or TSV lines
private fun parseCsvToInventoryParts(rawText: String): List<InventoryPart> {
    if (rawText.isBlank()) return emptyList()
    val lines = rawText.trim().split("\n")
    val result = mutableListOf<InventoryPart>()

    for ((index, line) in lines.withIndex()) {
        val trimmed = line.trim()
        if (trimmed.isBlank()) continue

        // Split by comma or tab or semicolon
        val delimiter = when {
            trimmed.contains("\t") -> "\t"
            trimmed.contains(";") -> ";"
            else -> ","
        }
        val cols = trimmed.split(delimiter).map { it.trim().removeSurrounding("\"") }

        // Skip header line if detected
        if (cols.firstOrNull()?.contains("باركود", ignoreCase = true) == true ||
            cols.firstOrNull()?.contains("barcode", ignoreCase = true) == true ||
            cols.firstOrNull()?.contains("الكود", ignoreCase = true) == true) {
            continue
        }

        if (cols.isNotEmpty() && cols[0].isNotBlank()) {
            val barcode = cols[0]
            val name = if (cols.size > 1 && cols[1].isNotBlank()) cols[1] else "صنف $barcode"
            val category = if (cols.size > 2 && cols[2].isNotBlank()) cols[2] else "قطع غيار"
            val deviceModel = if (cols.size > 3 && cols[3].isNotBlank()) cols[3] else "عام"
            val cost = if (cols.size > 4) cols[4].replace("ج", "").replace("$", "").trim().toDoubleOrNull() ?: 0.0 else 0.0
            val price = if (cols.size > 5) cols[5].replace("ج", "").replace("$", "").trim().toDoubleOrNull() ?: (cost * 1.3) else (cost * 1.3)
            val stock = if (cols.size > 6) cols[6].toIntOrNull() ?: 1 else 1
            val minAlert = if (cols.size > 7) cols[7].toIntOrNull() ?: 2 else 2

            result.add(
                InventoryPart(
                    id = System.currentTimeMillis() + index,
                    barcode = barcode,
                    name = name,
                    category = category,
                    device_model = deviceModel,
                    unit_cost = cost,
                    selling_price = price,
                    current_stock = stock,
                    min_alert_stock = minAlert
                )
            )
        }
    }
    return result
}

// Generate CSV with UTF-8 BOM so Excel on Windows & Mac renders Arabic perfectly
private fun generateExportCsv(parts: List<InventoryPart>): String {
    val sb = StringBuilder()
    // UTF-8 BOM
    sb.append('\uFEFF')
    sb.append("الباركود,اسم القطعة,التصنيف,موديل الجهاز,سعر التكلفة,سعر البيع,الرصيد المخزني,حد التنبيه\n")
    for (p in parts) {
        sb.append("\"${p.barcode}\",\"${p.name}\",\"${p.category}\",\"${p.device_model}\",${p.unit_cost},${p.selling_price},${p.current_stock},${p.min_alert_stock}\n")
    }
    return sb.toString()
}
