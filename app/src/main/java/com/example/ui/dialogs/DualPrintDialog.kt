package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Device
import com.example.data.model.PrinterConfig
import com.example.ui.components.BarcodeView
import com.example.ui.components.QrCodeView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.Slate400
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DualPrintDialog(
    device: Device,
    config: PrinterConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentDateStr = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date())
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StatusEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "الطباعة المزدوجة الفورية",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "طابعة 1 (الريسيت) + طابعة 2 (الاستيكر)",
                                fontSize = 11.sp,
                                color = StatusEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Dual Status Indicator Banner
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1010B981))
                        .border(1.dp, Color(0x3310B981), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "1. ${config.receiptPrinterName} (${config.receiptPaperWidth})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(14.dp))
                            Text("تم الإرسال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusEmerald)
                        }
                    }

                    HorizontalDivider(color = Color(0x2210B981))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Label, contentDescription = null, tint = StatusBlue, modifier = Modifier.size(16.dp))
                            Text(
                                text = "2. ${config.stickerPrinterName} (${config.stickerWidth})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(14.dp))
                            Text("تم الإرسال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusEmerald)
                        }
                    }
                }

                // Tabs for Switching Live Preview
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = EmeraldPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("معاينة الإيصال (طابعة 1)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("معاينة الاستيكر (طابعة 2)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Preview Content
                if (selectedTab == 0) {
                    // Receipt Thermal Preview (80mm / 58mm)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = config.shopName.ifBlank { "المتحدة للصيانة" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            if (config.shopPhone.isNotBlank()) {
                                Text(
                                    text = "هاتف: ${config.shopPhone}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = "تذكرة صيانة: ${device.ticketNumber}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                thickness = 1.dp,
                                color = Color.Black
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("العميل: ${device.customer_name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text(device.customer_phone ?: "", fontSize = 11.sp, color = Color(0xFF334155))
                                }
                                Text("الجهاز: ${device.device_name}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                if (!device.issue_description.isNullOrBlank()) {
                                    Text("العطل: ${device.issue_description}", fontSize = 10.sp, color = Color(0xFF475569))
                                }
                                val techName = when (device.technician?.lowercase()) {
                                    "tech1" -> "أحمد (هاردوير)"
                                    "tech2" -> "محمود (شاشات)"
                                    "hazem" -> "حازم"
                                    "admin" -> "المدير"
                                    else -> device.technician ?: "غير محدد"
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المستلم: ${device.received_by_employee ?: "-"}", fontSize = 10.sp, color = Color(0xFF475569))
                                    Text("الفني: $techName", fontSize = 10.sp, color = Color(0xFF475569))
                                }
                                if (!device.due_date.isNullOrBlank()) {
                                    Text("ميعاد التسليم: ${device.due_date}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                }
                                Text("التكلفة: ${device.estimated_cost ?: "0"} ج.م", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("التاريخ: $currentDateStr", fontSize = 9.sp, color = Color.Gray)
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                thickness = 1.dp,
                                color = Color.Black
                            )

                            QrCodeView(content = device.ticketNumber, size = 70.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            BarcodeView(content = device.ticketNumber, height = 32.dp, modifier = Modifier.width(160.dp))

                            Text(
                                text = config.footerNote.ifBlank { "شكراً لزيارتكم الورشة" },
                                fontSize = 9.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                } else {
                    // Sticker / Barcode Label Preview (50x30mm)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.5.dp, Color(0xFF64748B), RoundedCornerShape(8.dp))
                            .padding(10.dp)
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
                                Text(
                                    text = config.shopName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = device.ticketNumber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 1.dp,
                                color = Color.Black
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "العميل: ${device.customer_name}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "الجهاز: ${device.device_name}",
                                        fontSize = 10.sp,
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!device.issue_description.isNullOrBlank()) {
                                        Text(
                                            text = "العطل: ${device.issue_description}",
                                            fontSize = 9.sp,
                                            color = Color.DarkGray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    val techNameSticker = when (device.technician?.lowercase()) {
                                        "tech1" -> "أحمد (هاردوير)"
                                        "tech2" -> "محمود (شاشات)"
                                        "hazem" -> "حازم"
                                        "admin" -> "المدير"
                                        else -> device.technician ?: "غير محدد"
                                    }
                                    Text(
                                        text = "الفني: $techNameSticker",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    if (!device.due_date.isNullOrBlank()) {
                                        Text(
                                            text = "التسليم: ${device.due_date}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                QrCodeView(content = device.ticketNumber, size = 46.dp)
                            }

                            Spacer(modifier = Modifier.height(3.dp))
                            BarcodeView(content = device.ticketNumber, height = 26.dp, modifier = Modifier.fillMaxWidth())

                            Text(
                                text = "${device.ticketNumber} • ${device.received_by_employee ?: ""}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Action Buttons
                val scope = rememberCoroutineScope()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                // Print receipt
                                val receiptRes = com.example.data.printer.NetworkPrinterService.printReceipt(
                                    ipAddress = config.receiptPrinterIp,
                                    device = device,
                                    config = config
                                )
                                // Print sticker
                                val stickerRes = com.example.data.printer.NetworkPrinterService.printSticker(
                                    ipAddress = config.stickerPrinterIp,
                                    device = device,
                                    config = config
                                )
                                if (receiptRes.isSuccess && stickerRes.isSuccess) {
                                    android.widget.Toast.makeText(context, "تمت الطباعة المزدوجة بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    val errBuilder = StringBuilder()
                                    receiptRes.onFailure { errBuilder.append("الريسيت: ${it.message}. ") }
                                    stickerRes.onFailure { errBuilder.append("الاستيكر: ${it.message}. ") }
                                    android.widget.Toast.makeText(context, "خطأ في بعض الطابعات: $errBuilder", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusEmerald)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طباعة مزدوجة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "إيصال صيانة: ${device.ticketNumber}\nالعميل: ${device.customer_name}\nالجهاز: ${device.device_name}\nالتكلفة: ${device.estimated_cost ?: "0"} ج.م"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة بيانات الجهاز"))
                        },
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("مشاركة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إغلاق", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
