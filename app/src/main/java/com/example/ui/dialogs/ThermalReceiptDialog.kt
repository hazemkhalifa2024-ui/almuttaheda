package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.Device
import com.example.data.model.PrinterConfig
import com.example.ui.components.BarcodeView
import com.example.ui.components.QrCodeView
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThermalReceiptDialog(
    device: Device,
    config: PrinterConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentDateStr = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date())

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
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "معاينة إيصال الاستلام الحراري",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // White Thermal Paper Box
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
                            text = config.shopName.ifBlank { "المتحدة للصيانة" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        if (config.shopPhone.isNotBlank()) {
                            Text(
                                text = "هاتف: ${config.shopPhone}",
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "تذكرة صيانة: ${device.ticketNumber}",
                            fontSize = 14.sp,
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
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("العميل: ${device.customer_name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(device.customer_phone ?: "", fontSize = 12.sp, color = Color(0xFF334155))
                            }
                            Text("الجهاز: ${device.device_name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            if (!device.issue_description.isNullOrBlank()) {
                                Text("العطل: ${device.issue_description}", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                            val techName = when (device.technician?.lowercase()) {
                                "tech1" -> "أحمد (هاردوير)"
                                "tech2" -> "محمود (شاشات)"
                                "hazem" -> "حازم"
                                "admin" -> "المدير"
                                else -> device.technician ?: "غير محدد"
                            }
                            Text("الفني المسؤول: $techName", fontSize = 11.sp, color = Color(0xFF475569))
                            if (!device.due_date.isNullOrBlank()) {
                                Text("ميعاد التسليم المتوقع: ${device.due_date}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }
                            Text("التكلفة: ${device.estimated_cost ?: "0"} ج.م", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("التاريخ: $currentDateStr", fontSize = 10.sp, color = Color.Gray)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = Color.Black
                        )

                        // QR Code
                        QrCodeView(content = device.ticketNumber, size = 80.dp)

                        Spacer(modifier = Modifier.height(6.dp))

                        // Barcode
                        BarcodeView(content = device.ticketNumber, height = 38.dp, modifier = Modifier.width(180.dp))

                        Text(
                            text = config.footerNote.ifBlank { "شكراً لزيارتكم الورشة" },
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Print & Share Buttons
                val scope = rememberCoroutineScope()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val result = com.example.data.printer.NetworkPrinterService.printReceipt(
                                    ipAddress = config.receiptPrinterIp,
                                    device = device,
                                    config = config
                                )
                                result.onSuccess {
                                    android.widget.Toast.makeText(context, "تم إرسال أمر الطباعة بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                                }.onFailure { e ->
                                    android.widget.Toast.makeText(context, "خطأ في الاتصال بالطابعة: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("direct_print_receipt_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طباعة مباشرة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            shareReceiptText(context, device, config, currentDateStr)
                        },
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                            .testTag("share_receipt_button"),
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

private fun shareReceiptText(
    context: Context,
    device: Device,
    config: PrinterConfig,
    dateStr: String
) {
    val text = buildString {
        appendLine("===============================")
        appendLine("       ${config.shopName}       ")
        if (config.shopPhone.isNotBlank()) appendLine("هاتف: ${config.shopPhone}")
        appendLine("تذكرة صيانة رقم: ${device.ticketNumber}")
        appendLine("===============================")
        appendLine("العميل: ${device.customer_name}")
        if (!device.customer_phone.isNullOrBlank()) appendLine("الهاتف: ${device.customer_phone}")
        appendLine("الجهاز: ${device.device_name}")
        if (!device.issue_description.isNullOrBlank()) appendLine("العطل: ${device.issue_description}")
        appendLine("التكلفة التقديرية: ${device.estimated_cost ?: "0"} ج.م")
        appendLine("التاريخ: $dateStr")
        appendLine("الحالة: ${device.statusArabicLabel}")
        appendLine("===============================")
        appendLine(config.footerNote)
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة إيصال الصيانة"))
}
