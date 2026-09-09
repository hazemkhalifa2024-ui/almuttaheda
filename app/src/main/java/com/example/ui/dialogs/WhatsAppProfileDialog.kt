package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.model.WhatsAppConfig
import com.example.data.model.WhatsAppSendMethod
import com.example.data.model.WhatsAppTemplate
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusEmerald
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppProfileDialog(
    config: WhatsAppConfig,
    templates: List<WhatsAppTemplate>,
    onSaveConfig: (WhatsAppConfig) -> Unit,
    onSaveTemplates: (List<WhatsAppTemplate>) -> Unit,
    onTestConnection: suspend (WhatsAppConfig) -> Result<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = الربط والسيرفر, 1 = تخصيص القوالب

    // Config state
    var webhookUrl by remember { mutableStateOf(config.webhookUrl) }
    var apiToken by remember { mutableStateOf(config.apiToken) }
    var senderPhone by remember { mutableStateOf(config.senderPhoneNumber) }
    var sendMethod by remember { mutableStateOf(config.sendMethod) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    // Templates state
    var currentTemplates by remember { mutableStateOf(templates) }
    var templateToEdit by remember { mutableStateOf<WhatsAppTemplate?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("whatsapp_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "بروفايل الواتساب والربط (Baileys / n8n)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "إعداد خادم الإرسال وقوالب الرسائل المخصصة للورشة",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Tabs: 0 = إعدادات السيرفر, 1 = قوالب الرسائل
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = Color(0xFF128C7E),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("إعدادات السيرفر والربط", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("تخصيص قوالب الرسائل (${currentTemplates.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }

                // Content based on tab
                if (selectedTab == 0) {
                    // SERVER & WEBHOOK CONFIG
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF25D366).copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF128C7E))
                                    Text(
                                        text = "المنظومة مهيأة للتكامل التلقائي مع مكتبة Baileys وسيناريوهات n8n على سيرفرك الخاص. سيتم إرسال الحدث والرسالة بتنسيق JSON نظيف وسريع.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = webhookUrl,
                                onValueChange = { webhookUrl = it },
                                label = { Text("رابط Webhook (n8n / Baileys Endpoint)") },
                                placeholder = { Text("http://your-server-ip:5678/webhook/whatsapp-send") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = apiToken,
                                onValueChange = { apiToken = it },
                                label = { Text("رمز التحقق السري / API Secret (اختياري)") },
                                placeholder = { Text("مثال: Bearer token أو X-Api-Key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = senderPhone,
                                onValueChange = { senderPhone = it },
                                label = { Text("رقم واتساب المحل الرسمي (المرسل)") },
                                placeholder = { Text("01023613682") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                            )
                        }

                        // Send Method Selection
                        item {
                            Text("آلية الإرسال المفضلة:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WhatsAppSendMethod.entries.forEach { method ->
                                    val isSel = sendMethod == method
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { sendMethod = method },
                                        label = { Text(method.label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF25D366).copy(alpha = 0.2f),
                                            selectedLabelColor = Color(0xFF075E54)
                                        )
                                    )
                                }
                            }
                        }

                        // Test Connection Button
                        item {
                            Button(
                                onClick = {
                                    if (webhookUrl.isBlank()) {
                                        Toast.makeText(context, "يرجى كتابة رابط Webhook أولاً", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isTesting = true
                                    testResult = null
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        val res = onTestConnection(
                                            config.copy(
                                                webhookUrl = webhookUrl,
                                                apiToken = apiToken,
                                                senderPhoneNumber = senderPhone,
                                                sendMethod = sendMethod
                                            )
                                        )
                                        isTesting = false
                                        testResult = res.fold(
                                            onSuccess = { it },
                                            onFailure = { it.message ?: "فشل الاتصال" }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                enabled = !isTesting
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جاري فحص الاتصال بـ n8n...")
                                } else {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("فحص الاتصال بالسيرفر التجريبي")
                                }
                            }
                            if (testResult != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = testResult!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (testResult!!.contains("نجح") || testResult!!.contains("200")) StatusEmerald else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    // TEMPLATES MANAGER
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "قوالب الرسائل الجاهزة:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        currentTemplates = WhatsAppTemplate.DEFAULT_TEMPLATES
                                        onSaveTemplates(currentTemplates)
                                        Toast.makeText(context, "تمت استعادة القوالب الافتراضية", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("استعادة الافتراضي", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("قالب جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // List of templates
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentTemplates) { tpl ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { templateToEdit = tpl }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Message,
                                                    contentDescription = null,
                                                    tint = Color(0xFF128C7E),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = tpl.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Row {
                                                IconButton(
                                                    onClick = { templateToEdit = tpl },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
                                                }
                                                if (!tpl.isDefault) {
                                                    IconButton(
                                                        onClick = {
                                                            currentTemplates = currentTemplates.filter { it.id != tpl.id }
                                                            onSaveTemplates(currentTemplates)
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tpl.content,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }

                        // Tags helper box
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "المتغيرات المدعومة في القوالب:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "{customer_name}, {device_name}, {ticket_number}, {issue}, {cost}, {down_payment}, {remaining}, {status}, {shop_name}, {shop_phone}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF128C7E),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                // Footer Save
                Button(
                    onClick = {
                        val newCfg = config.copy(
                            webhookUrl = webhookUrl,
                            apiToken = apiToken,
                            senderPhoneNumber = senderPhone,
                            sendMethod = sendMethod
                        )
                        onSaveConfig(newCfg)
                        onSaveTemplates(currentTemplates)
                        Toast.makeText(context, "تم حفظ بروفايل وإعدادات الواتساب بنجاح", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ التغييرات", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    // Edit Template Dialog
    if (templateToEdit != null) {
        val tpl = templateToEdit!!
        var editTitle by remember(tpl) { mutableStateOf(tpl.title) }
        var editContent by remember(tpl) { mutableStateOf(tpl.content) }

        AlertDialog(
            onDismissRequest = { templateToEdit = null },
            title = { Text("تعديل قالب الرسالة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("عنوان القالب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("نص الرسالة مع المتغيرات") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentTemplates = currentTemplates.map {
                            if (it.id == tpl.id) it.copy(title = editTitle, content = editContent) else it
                        }
                        onSaveTemplates(currentTemplates)
                        templateToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text("حفظ التعديل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add New Template Dialog
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة قالب رسالة جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("عنوان القالب (مثال: عطل شاشة معقد)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("نص الرسالة") },
                        placeholder = { Text("مرحباً {customer_name}...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                            val newTpl = WhatsAppTemplate(
                                id = UUID.randomUUID().toString().take(8),
                                title = newTitle,
                                category = "custom",
                                content = newContent,
                                isDefault = false
                            )
                            currentTemplates = currentTemplates + newTpl
                            onSaveTemplates(currentTemplates)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text("إضافة", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
