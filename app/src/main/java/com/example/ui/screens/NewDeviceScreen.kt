package com.example.ui.screens

import android.widget.Toast
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.User
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiAudioService
import com.example.data.model.CustomerProfile
import com.example.ui.dialogs.VoiceIntakeDialog
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusPurple

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewDeviceScreen(
    isLoading: Boolean,
    currentEmployeeName: String = "موظف الاستقبال",
    customerProfiles: List<CustomerProfile> = emptyList(),
    users: List<User> = emptyList(),
    geminiAudioService: GeminiAudioService = remember { GeminiAudioService() },
    onViewCustomerHistory: (CustomerProfile) -> Unit = {},
    onSaveDevice: (
        customerName: String,
        customerPhone: String?,
        deviceName: String,
        issueDescription: String?,
        estimatedCost: String?,
        downPayment: Double,
        technician: String?,
        receivedByEmployee: String?,
        photoUrl: String?,
        dueDate: String?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var estimatedCost by remember { mutableStateOf("") }
    var downPayment by remember { mutableStateOf("") }
    var receivedByEmployee by remember(currentEmployeeName) { mutableStateOf(currentEmployeeName) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var dueDate by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            // 1. Resize down to maximum dimension of 200px (Very small & compact)
            val maxDimension = 200
            val origW = bitmap.width
            val origH = bitmap.height
            val (newW, newH) = if (origW > origH) {
                maxDimension to (maxDimension * origH.toFloat() / origW).toInt()
            } else {
                (maxDimension * origW.toFloat() / origH).toInt() to maxDimension
            }
            val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)

            // 2. Ultra-compressed JPEG format (20% quality for minimum size)
            val byteArrayOutputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            photoBase64 = "data:image/jpeg;base64," + android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر فتح الكاميرا: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "يجب السماح بصلاحية الكاميرا لالتقاط صورة للجهاز", Toast.LENGTH_LONG).show()
        }
    }

    val isDark = isSystemInDarkTheme()

    val techOptions = remember(users) {
        if (users.isEmpty()) {
            listOf(
                "tech1" to "فني 1 - أحمد (هاردوير)",
                "tech2" to "فني 2 - محمود (شاشات وسوفتوير)",
                "hazem" to "حازم (hazem)",
                "admin" to "المدير (admin)"
            )
        } else {
            users.map { u ->
                val roleLabel = when (u.role.lowercase()) {
                    "admin" -> "المدير"
                    "technician" -> "فني صيانة"
                    else -> "مستخدم"
                }
                u.username to "${u.username} ($roleLabel)"
            }
        }
    }
    var selectedTech by remember(techOptions) { mutableStateOf(techOptions.firstOrNull() ?: ("tech1" to "فني 1 - أحمد (هاردوير)")) }
    var isTechDropdownExpanded by remember { mutableStateOf(false) }

    val commonIssues = listOf(
        "تغيير شاشة", "باغة مكسورة", "سوكت شحن", "بطارية منتفخة",
        "عطل باور / لا يفتح", "صيانة ماذر بورد", "سماعة / مايك", "سوفت وير / تفليش"
    )

    // Detect if this customer has prior history
    val matchingProfile = remember(customerPhone, customerName, customerProfiles) {
        val cleanPhone = customerPhone.trim()
        val cleanName = customerName.trim()
        when {
            cleanPhone.length >= 7 -> customerProfiles.find { it.phone.isNotBlank() && it.phone.contains(cleanPhone) }
            cleanName.length >= 3 -> customerProfiles.find { it.name.contains(cleanName, ignoreCase = true) }
            else -> null
        }
    }

    if (showVoiceDialog) {
        VoiceIntakeDialog(
            geminiAudioService = geminiAudioService,
            onDismiss = { showVoiceDialog = false },
            onApplyParsedData = { parsed ->
                if (parsed.customerName.isNotBlank()) customerName = parsed.customerName
                if (parsed.customerPhone.isNotBlank()) customerPhone = parsed.customerPhone
                if (parsed.deviceName.isNotBlank()) deviceName = parsed.deviceName
                if (parsed.issueDescription.isNotBlank()) issueDescription = parsed.issueDescription
                if (parsed.estimatedCost.isNotBlank()) estimatedCost = parsed.estimatedCost
                showVoiceDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("new_device_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "استلام جهاز جديد",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "أدخل بيانات العميل والجهاز لإصدار تذكرة الصيانة وطباعة الباركود",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Voice Intake Trigger Button (Gemini AI 3.5)
                Button(
                    onClick = { showVoiceDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("voice_intake_header_btn")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إدخال صوتي (AI)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Returning Customer Notification Banner
        if (matchingProfile != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Slate900 else Color.White)
                        .border(1.dp, StatusPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusPurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = StatusPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "عميل سابق: ${matchingProfile.name}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (matchingProfile.totalRepairs >= 3) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(StatusAmber.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("VIP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StatusAmber)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${matchingProfile.totalRepairs} أجهزة صيانة سابقة • إجمالي ${matchingProfile.totalSpent.toInt()} ج.م",
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                }
                            }

                            Button(
                                onClick = { onViewCustomerHistory(matchingProfile) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusPurple.copy(alpha = 0.15f),
                                    contentColor = StatusPurple
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("عرض السجل والأعطال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // One-tap fill for customer details if fields empty
                        if (customerName.isBlank() || customerPhone.isBlank()) {
                            Button(
                                onClick = {
                                    if (customerName.isBlank()) customerName = matchingProfile.name
                                    if (customerPhone.isBlank() && matchingProfile.phone.isNotBlank()) customerPhone = matchingProfile.phone
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                    contentColor = EmeraldPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إكمال البيانات تلقائياً من الملف", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

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
                    // Mandatory Employee Tracking Field
                    Column {
                        Text(
                            text = "اسم موظف الاستلام المسؤول *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = receivedByEmployee,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("received_by_employee_input"),
                            placeholder = { Text("اسم موظف الاستقبال أو الفرع") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Customer Name
                    Column {
                        Text(
                            text = "اسم العميل *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_name_input"),
                            placeholder = { Text("مثال: أحمد محمد علي", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    // Customer Phone
                    Column {
                        Text(
                            text = "رقم الهاتف (لإرسال SMS)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_phone_input"),
                            placeholder = { Text("01012345678", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    // Device Name / Model
                    Column {
                        Text(
                            text = "نوع وموديل الجهاز *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = deviceName,
                            onValueChange = { deviceName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("device_name_input"),
                            placeholder = { Text("مثال: Samsung A54 أو iPhone 13", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    // Assigned Technician
                    Column {
                        Text(
                            text = "الفني المسؤول",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = isTechDropdownExpanded,
                            onExpandedChange = { isTechDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedTech.second,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTechDropdownExpanded) },
                                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = EmeraldPrimary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = isTechDropdownExpanded,
                                onDismissRequest = { isTechDropdownExpanded = false }
                            ) {
                                techOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt.second) },
                                        onClick = {
                                            selectedTech = opt
                                            isTechDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Common Issues Quick Chips
                    Column {
                        Text(
                            text = "اختصارات الأعطال الشائعة:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonIssues.forEach { issue ->
                                SuggestionChip(
                                    onClick = {
                                        issueDescription = if (issueDescription.isBlank()) issue else "$issueDescription - $issue"
                                    },
                                    label = { Text(issue, fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Issue Description
                    Column {
                        Text(
                            text = "وصف العطل بالتفصيل",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = issueDescription,
                            onValueChange = { issueDescription = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("issue_description_input"),
                            placeholder = { Text("أدخل ملاحظات العطل أو الأجزاء المطلوبة...", fontSize = 13.sp) },
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    // Estimated Cost
                    Column {
                        Text(
                            text = "التكلفة التقديرية (ج.م)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = estimatedCost,
                            onValueChange = { estimatedCost = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("estimated_cost_input"),
                            placeholder = { Text("0", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EmeraldPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    // Down Payment / Deposit
                    Column {
                        Text(
                            text = "المبلغ المدفوع مقدماً / العربون (ج.م)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = downPayment,
                            onValueChange = { downPayment = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("down_payment_input"),
                            placeholder = { Text("0", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = EmeraldPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Quick deposit choice chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val depositChoices = listOf("0", "50", "100", "150", "200", "300", "500")
                            depositChoices.forEach { choice ->
                                SuggestionChip(
                                    onClick = { downPayment = choice },
                                    label = { Text("$choice ج.م", fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Delivery Due Date (ميعاد التسليم المتوقع)
                    Column {
                        Text(
                            text = "ميعاد التسليم المتوقع *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("due_date_input"),
                            placeholder = { Text("مثال: بعد يومين، غداً، 2026/08/25", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Quick due date choice chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val dueChoices = listOf(
                                "بعد ساعة",
                                "بعد ساعتين",
                                "اليوم ٣ عصرًا",
                                "اليوم ٥ مساءً",
                                "اليوم ٧ مساءً",
                                "اليوم ١٠ مساءً",
                                "غدًا",
                                "بعد يومين"
                            )
                            dueChoices.forEach { choice ->
                                SuggestionChip(
                                    onClick = { dueDate = choice },
                                    label = { Text(choice, fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Photo of the device upon receipt
                    Column {
                        Text(
                            text = "صورة الجهاز عند الاستلام (لتسهيل العثور عليه)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        if (capturedBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, EmeraldPrimary, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Device Photo",
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Clear photo button
                                Button(
                                    onClick = {
                                        capturedBitmap = null
                                        photoBase64 = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red.copy(alpha = 0.8f),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("حذف الصورة", fontSize = 11.sp)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        val hasCamPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasCamPermission) {
                                            try {
                                                cameraLauncher.launch(null)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "تعذر فتح الكاميرا: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Take Photo",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        "التقط صورة للجهاز بالكاميرا",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (customerName.isNotBlank() && deviceName.isNotBlank()) {
                                onSaveDevice(
                                    customerName,
                                    customerPhone.ifBlank { null },
                                    deviceName,
                                    issueDescription.ifBlank { null },
                                    estimatedCost.ifBlank { "0" },
                                    downPayment.toDoubleOrNull() ?: 0.0,
                                    selectedTech.first,
                                    receivedByEmployee.ifBlank { currentEmployeeName },
                                    photoBase64,
                                    dueDate.ifBlank { "غير محدد" }
                                )
                            }
                        },
                        enabled = !isLoading && customerName.isNotBlank() && deviceName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_device_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null)
                                Text(
                                    text = "حفظ واستلام وطباعة الإيصال والملصق",
                                    fontSize = 14.sp,
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

