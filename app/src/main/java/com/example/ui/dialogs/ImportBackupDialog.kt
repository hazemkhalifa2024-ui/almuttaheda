package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.example.data.backup.JsonBackupManager
import com.example.data.backup.ParsedBackup
import com.example.data.model.Device
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedAccent
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald

@Composable
fun ImportBackupDialog(
    initialJsonText: String? = null,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onConfirmRestore: (devices: List<Device>, replaceAll: Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    var pastedJson by remember { mutableStateOf(initialJsonText ?: "") }
    var parsedBackup by remember {
        mutableStateOf<ParsedBackup?>(
            if (!initialJsonText.isNullOrBlank()) {
                JsonBackupManager.parseJsonBackup(initialJsonText).getOrNull()
            } else null
        )
    }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isReplaceAllMode by remember { mutableStateOf(false) } // false = merge, true = replace
    var isManualPasteExpanded by remember { mutableStateOf(false) }

    fun tryParse(jsonString: String) {
        parseError = null
        if (jsonString.isBlank()) {
            parseError = "الرجاء لصق نص JSON أو اختيار ملف للتحليل"
            return
        }
        val result = JsonBackupManager.parseJsonBackup(jsonString)
        result.onSuccess {
            parsedBackup = it
            parseError = null
        }.onFailure { err ->
            parsedBackup = null
            parseError = err.message ?: "خطأ في قراءة ملف JSON"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("import_backup_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StatusBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = StatusBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "استيراد واستعادة البيانات",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "استعادة الأجهزة من ملف نسخة احتياطية JSON",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Slate400)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                if (parsedBackup == null) {
                    // Step 1: Selection & Input
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "اختر طريقة استيراد النسخة الاحتياطية:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // File Picker Button
                        Button(
                            onClick = onPickFile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_json_file_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "اختيار ملف JSON من الذاكرة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Toggle manual paste
                        OutlinedButton(
                            onClick = { isManualPasteExpanded = !isManualPasteExpanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("toggle_paste_json_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isManualPasteExpanded) "إخفاء خانة اللصق اليدوي" else "أو لصق نص JSON يدوياً",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isManualPasteExpanded) {
                            OutlinedTextField(
                                value = pastedJson,
                                onValueChange = {
                                    pastedJson = it
                                    parseError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .testTag("paste_json_input"),
                                placeholder = { Text("الصق محتوى ملف JSON هنا...", fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 5
                            )

                            Button(
                                onClick = { tryParse(pastedJson) },
                                enabled = pastedJson.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("analyze_pasted_json_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("تحليل وقراءة البيانات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Error message if any
                        if (parseError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RedAccent.copy(alpha = 0.1f))
                                    .border(1.dp, RedAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = RedAccent, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = parseError ?: "",
                                        fontSize = 11.sp,
                                        color = RedAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Step 2: Preview & Confirmation
                    val backup = parsedBackup!!
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Summary Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDark) Slate900 else StatusEmerald.copy(alpha = 0.08f))
                                .border(1.dp, StatusEmerald.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(StatusEmerald.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(
                                        text = "تم العثور على ${backup.metadata.deviceCount} جهاز في النسخة الاحتياطية",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تاريخ التصدير: ${backup.metadata.exportedAt}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Sample Devices Preview
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "معاينة عينة من الأجهزة الواردة في الملف:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Slate850 else Slate200.copy(alpha = 0.5f))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(backup.devices.take(5)) { dev ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldPrimary)
                                                Text(text = "${dev.customer_name} (${dev.device_name})", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            }
                                            Text(text = dev.statusArabicLabel, fontSize = 10.sp, color = StatusEmerald, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Mode Selection
                        Text(
                            text = "طريقة الاستعادة:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Option 1: Merge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isReplaceAllMode) EmeraldPrimary.copy(alpha = 0.1f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (!isReplaceAllMode) EmeraldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { isReplaceAllMode = false }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !isReplaceAllMode,
                                onClick = { isReplaceAllMode = false },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "دمج وتحديث البيانات (Merge & Add)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "إضافة الأجهزة الجديدة وتحديث المشتركة دون مسح الأجهزة الحالية",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Option 2: Replace All
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isReplaceAllMode) RedAccent.copy(alpha = 0.1f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isReplaceAllMode) RedAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { isReplaceAllMode = true }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isReplaceAllMode,
                                onClick = { isReplaceAllMode = true },
                                colors = RadioButtonDefaults.colors(selectedColor = RedAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "استبدال قاعدة البيانات بالكامل (Replace All)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReplaceAllMode) RedAccent else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "مسح الأجهزة الحالية وتعيين أجهزة النسخة الاحتياطية فقط",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    parsedBackup = null
                                    pastedJson = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تغيير الملف", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    onConfirmRestore(backup.devices, isReplaceAllMode)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .height(48.dp)
                                    .testTag("confirm_restore_backup_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isReplaceAllMode) RedAccent else EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isReplaceAllMode) "تأكيد الاستبدال الشامل" else "تأكيد الاستعادة والدمج",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
