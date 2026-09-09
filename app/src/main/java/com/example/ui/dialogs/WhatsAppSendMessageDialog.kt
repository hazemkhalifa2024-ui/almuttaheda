package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.model.Device
import com.example.data.model.ShopConfig
import com.example.data.model.WhatsAppConfig
import com.example.data.model.WhatsAppTemplate
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppSendMessageDialog(
    device: Device,
    shopConfig: ShopConfig,
    whatsAppConfig: WhatsAppConfig,
    templates: List<WhatsAppTemplate>,
    isSending: Boolean = false,
    onSendWebhook: (phoneNumber: String, message: String) -> Unit,
    onOpenWhatsAppApp: (phoneNumber: String, message: String) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val phone = device.customer_phone ?: ""

    // Select initial template based on device status
    val initialTemplate = remember(templates, device.status) {
        when (device.status.lowercase()) {
            "received" -> templates.find { it.category == "received" } ?: templates.firstOrNull()
            "ready", "done" -> templates.find { it.category == "ready" } ?: templates.firstOrNull()
            "delivered" -> templates.find { it.category == "delivered" } ?: templates.firstOrNull()
            "delayed" -> templates.find { it.category == "delayed" } ?: templates.firstOrNull()
            else -> templates.find { it.category == "estimated" } ?: templates.firstOrNull()
        } ?: WhatsAppTemplate.DEFAULT_TEMPLATES.first()
    }

    var selectedTemplate by remember { mutableStateOf(initialTemplate) }

    fun generateMessage(tpl: WhatsAppTemplate): String {
        return tpl.render(
            customerName = device.customer_name,
            deviceName = device.device_name,
            ticketNumber = device.ticketNumber,
            issue = device.issue_description ?: "",
            cost = device.estimated_cost ?: "0",
            downPayment = device.down_payment.toString(),
            remaining = device.remaining_balance.toString(),
            status = device.statusArabicLabel,
            shopName = shopConfig.name,
            shopPhone = shopConfig.phone ?: ""
        )
    }

    var messageText by remember { mutableStateOf(generateMessage(initialTemplate)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("whatsapp_send_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366)), // WhatsApp Green
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مراسلة العميل عبر واتساب",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "العميل: ${device.customer_name}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "إعدادات وقوالب الواتساب",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Phone & Device Mini Badge
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("رقم الهاتف:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (phone.isNotBlank()) phone else "لا يوجد رقم مسجل!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (phone.isNotBlank()) Color(0xFF128C7E) else MaterialTheme.colorScheme.error
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("الجهاز / التذكرة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${device.device_name} (#${device.ticketNumber})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Templates Carousel
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "اختر نموذج الرسالة السريع:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        templates.forEach { tpl ->
                            val isSelected = selectedTemplate.id == tpl.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTemplate = tpl
                                    messageText = generateMessage(tpl)
                                },
                                label = {
                                    Text(
                                        text = tpl.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF25D366).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF075E54)
                                ),
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFF25D366)) else null
                            )
                        }
                    }
                }

                // Message Body TextField (Editable)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "معاينة وتعديل نص الرسالة:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF25D366),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Primary: Send via Baileys / n8n Webhook
                    Button(
                        onClick = {
                            if (phone.isBlank()) {
                                Toast.makeText(context, "لا يوجد رقم هاتف مسجل لهذا العميل!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (whatsAppConfig.webhookUrl.isBlank()) {
                                Toast.makeText(context, "الرابط غير مهيأ، سيتم الفتح عبر تطبيق واتساب مباشرة", Toast.LENGTH_SHORT).show()
                                onOpenWhatsAppApp(phone, messageText)
                            } else {
                                onSendWebhook(phone, messageText)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366)
                        ),
                        enabled = !isSending && phone.isNotBlank()
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (whatsAppConfig.webhookUrl.isNotBlank()) "إرسال تلقائي (n8n / Baileys)" else "إرسال عبر واتساب",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 2. Secondary: Direct WhatsApp App Intent
                    OutlinedButton(
                        onClick = {
                            if (phone.isBlank()) {
                                Toast.makeText(context, "لا يوجد رقم هاتف مسجل لهذا العميل!", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            onOpenWhatsAppApp(phone, messageText)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF128C7E)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF128C7E)
                        ),
                        enabled = phone.isNotBlank()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فتح الدردشة في تطبيق واتساب الرسمي",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    // Cancel
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
