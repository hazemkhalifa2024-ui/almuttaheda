package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.model.Device
import com.example.data.model.N8nConfig
import com.example.data.model.SessionUser
import com.example.data.model.SmsConfig
import com.example.ui.dialogs.ExportBackupDialog
import com.example.ui.dialogs.ImportBackupDialog
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RedAccent
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    sessionUser: SessionUser?,
    smsConfig: SmsConfig,
    n8nConfig: N8nConfig = N8nConfig(),
    devices: List<Device> = emptyList(),
    onToggleDarkMode: () -> Unit,
    onSaveSmsConfig: (SmsConfig) -> Unit,
    onSaveN8nConfig: (N8nConfig) -> Unit = {},
    onTestSms: (phone: String) -> Unit,
    onTestN8nWebhook: () -> Unit = {},
    onExportJson: () -> String = { "" },
    onRestoreBackup: (devices: List<Device>, replaceAll: Boolean) -> Unit = { _, _ -> },
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val isAdmin = sessionUser?.role == "admin"

    var enabled by remember(smsConfig) { mutableStateOf(smsConfig.enabled) }
    var accountSid by remember(smsConfig) { mutableStateOf(smsConfig.accountSid) }
    var authToken by remember(smsConfig) { mutableStateOf(smsConfig.authToken) }
    var fromNumber by remember(smsConfig) { mutableStateOf(smsConfig.fromNumber) }
    var autoSendOnReady by remember(smsConfig) { mutableStateOf(smsConfig.autoSendOnReady) }
    var autoSendOnDelay by remember(smsConfig) { mutableStateOf(smsConfig.autoSendOnDelay) }
    var readyTemplate by remember(smsConfig) { mutableStateOf(smsConfig.readyTemplate) }
    var delayTemplate by remember(smsConfig) { mutableStateOf(smsConfig.delayTemplate) }

    // n8n State
    var n8nEnabled by remember(n8nConfig) { mutableStateOf(n8nConfig.enabled) }
    var n8nWebhookUrl by remember(n8nConfig) { mutableStateOf(n8nConfig.webhookUrl) }
    var n8nAuthSecret by remember(n8nConfig) { mutableStateOf(n8nConfig.authSecret) }
    var n8nTrigCreate by remember(n8nConfig) { mutableStateOf(n8nConfig.triggerOnDeviceCreated) }
    var n8nTrigStatus by remember(n8nConfig) { mutableStateOf(n8nConfig.triggerOnStatusChanged) }
    var n8nTrigReady by remember(n8nConfig) { mutableStateOf(n8nConfig.triggerOnDeviceReady) }
    var n8nTrigDelivered by remember(n8nConfig) { mutableStateOf(n8nConfig.triggerOnDeviceDelivered) }

    var testPhone by remember { mutableStateOf("") }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonCache by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var pickedJsonContentForImport by remember { mutableStateOf<String?>(null) }

    // SAF Launcher for saving exported JSON file
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && exportJsonCache.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(exportJsonCache.toByteArray())
                }
                Toast.makeText(context, "تم حفظ ملف النسخة الاحتياطية بنجاح", Toast.LENGTH_LONG).show()
                showExportDialog = false
            } catch (e: Exception) {
                Toast.makeText(context, "فشل حفظ الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // SAF Launcher for picking JSON file to import
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                }
                if (!content.isNullOrBlank()) {
                    pickedJsonContentForImport = content
                    showImportDialog = true
                } else {
                    Toast.makeText(context, "الملف المحدد فارغ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ في قراءة الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "الإعدادات العامة والنسخ الاحتياطي",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "إدارة النسخ الاحتياطي لقاعدة البيانات، خدمة الرسائل Twilio، والحساب",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ==========================================
        // DATABASE BACKUP & RESTORE SECTION (JSON)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("database_backup_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAdmin) StatusEmerald.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section Header
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
                                    .background(if (isAdmin) StatusEmerald.copy(alpha = 0.15f) else Slate400.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = if (isAdmin) StatusEmerald else Slate400,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "النسخ الاحتياطي واستعادة البيانات",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isAdmin) EmeraldPrimary.copy(alpha = 0.15f) else Slate400.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isAdmin) "صلاحية المدير" else "للمدير فقط",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAdmin) EmeraldPrimary else Slate400
                                        )
                                    }
                                }
                                Text(
                                    text = "تصدير كافة أجهزة الورشة إلى ملف JSON يدوي أو استعادتها",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    if (isAdmin) {
                        // Admin: Snapshot & Actions
                        // Quick Database Status Stats
                        val totalDevicesCount = devices.size
                        val totalDelivered = devices.count { it.status.lowercase() in listOf("delivered", "تم التسليم") }
                        val inProgressCount = devices.count { it.status.lowercase() !in listOf("delivered", "تم التسليم") }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "إجمالي الأجهزة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "$totalDevicesCount جهاز",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusEmerald
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "قيد الصيانة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "$inProgressCount جهاز",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusAmber
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "تم تسليمها", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "$totalDelivered جهاز",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusBlue
                                    )
                                }
                            }
                        }

                        // Action Buttons: Export & Import
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Export Button
                            Button(
                                onClick = {
                                    val json = onExportJson()
                                    exportJsonCache = json
                                    showExportDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_json_backup_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تصدير JSON",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Import Button
                            Button(
                                onClick = {
                                    pickedJsonContentForImport = null
                                    showImportDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("import_json_backup_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "استيراد واستعادة",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Informational Note
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Slate850 else Slate200.copy(alpha = 0.4f))
                                .padding(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = StatusEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "يتيح لك تصدير JSON حفظ نسخة غير محدودة من قاعدة البيانات ونقلها لأي جهاز آخر أو حفظها على Google Drive.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Non-admin locked notice
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StatusAmber.copy(alpha = 0.1f))
                                .border(1.dp, StatusAmber.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockPerson,
                                    contentDescription = null,
                                    tint = StatusAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "خاصية مقيدة بمدير النظام",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "عمليات تصدير واستعادة قاعدة بيانات الأجهزة متاحة حصرياً لحسابات المديرين (Admin).",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SMS & Twilio Gateway Configuration Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StatusEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = StatusEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "بوابة الرسائل النصية (Twilio SMS)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "إشعار العملاء تلقائياً عند جاهزية الجهاز أو تأخر الصيانة",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                        }

                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Account SID
                    Column {
                        Text(
                            text = "Twilio Account SID",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = accountSid,
                            onValueChange = { accountSid = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("twilio_sid_input"),
                            placeholder = { Text("ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Auth Token
                    Column {
                        Text(
                            text = "Twilio Auth Token",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = authToken,
                            onValueChange = { authToken = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("twilio_token_input"),
                            placeholder = { Text("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Twilio From Number / Sender ID
                    Column {
                        Text(
                            text = "رقم هاتف الإرسال (Twilio Phone Number / Sender ID)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = fromNumber,
                            onValueChange = { fromNumber = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("twilio_from_input"),
                            placeholder = { Text("+1234567890 أو Mottaheda") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Automatic Triggers Switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "إرسال SMS تلقائي عند الجاهزية",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "توليد رسالة فور تحويل حالة الجهاز إلى 'جاهز للتسليم'",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                        Switch(
                            checked = autoSendOnReady,
                            onCheckedChange = { autoSendOnReady = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "إرسال SMS إشعار التأخير",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "إرسال رسالة عند إبلاغ العميل بوجود تأخير لقطع الغيار",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                        Switch(
                            checked = autoSendOnDelay,
                            onCheckedChange = { autoSendOnDelay = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }

                    // Ready SMS Template
                    Column {
                        Text(
                            text = "نص رسالة الجاهزية للاستلام (Ready Template)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = readyTemplate,
                            onValueChange = { readyTemplate = it },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Text(
                            text = "المتغيرات المتاحة: {name}, {device}, {ticket}, {cost}, {shop}, {phone}",
                            fontSize = 10.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Delay SMS Template
                    Column {
                        Text(
                            text = "نص رسالة تأخير الصيانة (Delay Template)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = delayTemplate,
                            onValueChange = { delayTemplate = it },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Text(
                            text = "المتغيرات المتاحة: {name}, {device}, {ticket}, {reason}, {shop}",
                            fontSize = 10.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Save SMS Button
                    Button(
                        onClick = {
                            onSaveSmsConfig(
                                SmsConfig(
                                    enabled = enabled,
                                    accountSid = accountSid,
                                    authToken = authToken,
                                    fromNumber = fromNumber,
                                    autoSendOnReady = autoSendOnReady,
                                    autoSendOnDelay = autoSendOnDelay,
                                    readyTemplate = readyTemplate,
                                    delayTemplate = delayTemplate
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_sms_config_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ إعدادات الرسائل النصية", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Live Test Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Slate850 else Slate200.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "اختبار الإرسال الفوري (SMS Test)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = testPhone,
                                    onValueChange = { testPhone = it },
                                    placeholder = { Text("01012345678", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("test_phone_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                Button(
                                    onClick = { onTestSms(testPhone) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("send_test_sms_btn")
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إرسال تجربة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Account Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Text(
                                text = sessionUser?.displayName ?: "المستخدم",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "نوع الحساب: ${if (sessionUser?.role == "admin") "مدير النظام (Admin)" else "فني صيانة (Technician)"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Dark Mode Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Text(
                                text = "الوضع الليلي (Dark Mode)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isDarkMode) "المظهر الداكن مفعل" else "المظهر الفاتح مفعل",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary
                        ),
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        // n8n Workflow Automation Card
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
                    // Header & Switch
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StatusAmber.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("n8n", fontWeight = FontWeight.Black, fontSize = 14.sp, color = StatusAmber)
                            }
                            Column {
                                Text(
                                    text = "أتمتة العمليات وربط n8n (Webhooks)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "إرسال أحداث الصيانة فوراً لـ n8n (واتساب، تيليجرام، Google Sheets)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = n8nEnabled,
                            onCheckedChange = { n8nEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StatusAmber
                            )
                        )
                    }

                    if (n8nEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Webhook URL
                        Column {
                            Text(
                                text = "رابط Webhook الخاص بـ n8n",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = n8nWebhookUrl,
                                onValueChange = { n8nWebhookUrl = it },
                                placeholder = { Text("https://n8n.your-server.com/webhook/repair-flow") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StatusAmber,
                                    cursorColor = StatusAmber
                                )
                            )
                        }

                        // Auth Secret (Optional)
                        Column {
                            Text(
                                text = "مفتاح الأمان / Bearer Token (اختياري)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = n8nAuthSecret,
                                onValueChange = { n8nAuthSecret = it },
                                placeholder = { Text("مثال: secret_token_12345") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = StatusAmber,
                                    cursorColor = StatusAmber
                                )
                            )
                        }

                        // Trigger Checkboxes
                        Text(
                            text = "الأحداث المشغلة للإرسال إلى n8n:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عند استلام جهاز جديد وحفظه", fontSize = 12.sp)
                            Switch(checked = n8nTrigCreate, onCheckedChange = { n8nTrigCreate = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عند تغير مرحلة الصيانة أو إضافة ملاحظات", fontSize = 12.sp)
                            Switch(checked = n8nTrigStatus, onCheckedChange = { n8nTrigStatus = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عند اكتمال الإصلاح وجهوزية الجهاز للتسليم", fontSize = 12.sp)
                            Switch(checked = n8nTrigReady, onCheckedChange = { n8nTrigReady = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عند تسليم الجهاز للعميل نهائياً", fontSize = 12.sp)
                            Switch(checked = n8nTrigDelivered, onCheckedChange = { n8nTrigDelivered = it })
                        }

                        // Test and Save Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onTestN8nWebhook,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تجربة Webhook", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val newCfg = N8nConfig(
                                        enabled = n8nEnabled,
                                        webhookUrl = n8nWebhookUrl,
                                        authSecret = n8nAuthSecret,
                                        triggerOnDeviceCreated = n8nTrigCreate,
                                        triggerOnStatusChanged = n8nTrigStatus,
                                        triggerOnDeviceReady = n8nTrigReady,
                                        triggerOnDeviceDelivered = n8nTrigDelivered
                                    )
                                    onSaveN8nConfig(newCfg)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusAmber,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("حفظ إعدادات n8n", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Backend Connection Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = StatusEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = "حالة الاتصال بقاعدة البيانات",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "متصل مباشرة مع Supabase REST API (ndhflfzjxzgfvruzddyi)",
                            fontSize = 11.sp,
                            color = StatusEmerald
                        )
                    }
                }
            }
        }

        // System Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "حول التطبيق",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "المتحدة للصيانة - الإصدار 1.2.0",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "نظام متكامل لإدارة ورش صيانة الهواتف، إرسال إشعارات SMS عبر Twilio، تتبع سجل العملاء، والنسخ الاحتياطي اليدوي JSON لقاعدة البيانات.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Logout Button
        item {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedAccent.copy(alpha = 0.15f),
                    contentColor = RedAccent
                )
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("تسجيل الخروج من النظام", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        ExportBackupDialog(
            jsonContent = exportJsonCache,
            devicesCount = devices.size,
            onDismiss = { showExportDialog = false },
            onSaveToFile = {
                val fileName = "mottaheda_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.json"
                saveFileLauncher.launch(fileName)
            },
            onShareText = { content ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, content)
                    putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - المتحدة للصيانة")
                }
                context.startActivity(Intent.createChooser(sendIntent, "مشاركة النسخة الاحتياطية"))
            },
            onCopyText = { content ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Mottaheda Backup JSON", content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "تم نسخ نص JSON إلى الحافظة", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        ImportBackupDialog(
            initialJsonText = pickedJsonContentForImport,
            onDismiss = {
                showImportDialog = false
                pickedJsonContentForImport = null
            },
            onPickFile = {
                pickFileLauncher.launch("*/*")
            },
            onConfirmRestore = { importedDevices, replaceAll ->
                onRestoreBackup(importedDevices, replaceAll)
                showImportDialog = false
                pickedJsonContentForImport = null
            }
        )
    }
}

