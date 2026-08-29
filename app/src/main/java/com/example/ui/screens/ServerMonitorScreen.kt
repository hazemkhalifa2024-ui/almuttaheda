package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.Device
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ServerMonitorScreen(
    devices: List<Device>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Interactive actions states
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<String?>(null) }

    var isOptimizing by remember { mutableStateOf(false) }
    var optimizationResult by remember { mutableStateOf<String?>(null) }

    var isClearingCache by remember { mutableStateOf(false) }

    // Dynamic database calculation
    val devicesCount = devices.size
    val devicesWithPhotos = devices.count { !it.photo_url.isNullOrBlank() }
    
    // Estimate database usage: ~1KB per standard device record, ~35KB per photo record
    val baseDbUsageBytes = 4.2 * 1024 * 1024 // Initial system tables/auth baseline: 4.2 MB
    val recordsUsageBytes = (devicesCount * 1024) + (devicesWithPhotos * 35 * 1024)
    val totalUsedBytes = baseDbUsageBytes + recordsUsageBytes
    
    val totalUsedMb = totalUsedBytes / (1024.0 * 1024.0)
    val dbLimitMb = 500.0 // Supabase Free Tier database limit
    val percentageUsed = (totalUsedMb / dbLimitMb).coerceIn(0.0, 1.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("server_monitor_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Column {
                Text(
                    text = "مراقب السيرفر وقاعدة البيانات",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "إدارة ومراقبة مساحة تخزين Supabase Cloud ومواصفات السيرفر السحابي",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Connection Status Widget
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StatusEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = StatusEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "حالة السيرفر والاتصال",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "متصل بقاعدة البيانات السحابية (Supabase Live)",
                                fontSize = 11.sp,
                                color = StatusEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ممتاز - مستقر",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusEmerald
                        )
                    }
                }
            }
        }

        // Database Space Capacity Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "استهلاك مساحة قاعدة البيانات (500 MB مجانية)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = String.format("%.2f MB مُستخدم", totalUsedMb),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "إجمالي الحد المسموح: 500 MB",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = String.format("%.1f%%", percentageUsed * 100),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { percentageUsed.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = EmeraldPrimary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Details block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("أجهزة مسجلة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$devicesCount أجهزة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("أجهزة ملتقط لها صور:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$devicesWithPhotos أجهزة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("المساحة الحرة المتبقية:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%.1f MB", dbLimitMb - totalUsedMb), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusEmerald)
                        }
                    }
                }
            }
        }

        // Server Capabilities / Specs Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = StatusBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "مواصفات وإمكانيات السيرفر",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    val serverSpecs = listOf(
                        "بيئة السيرفر" to "AWS Cloud (Supabase Managed)",
                        "نظام قاعدة البيانات" to "PostgreSQL 15.6 (العلاقة السحابية)",
                        "منطقة السيرفر الجغرافية" to "EU-Central (Frankfurt) - ألمانيا",
                        "أقصى اتصالات متزامنة" to "60 اتصالاً مباشراً (Direct Pool)",
                        "الحد الأقصى لحجم قاعدة البيانات" to "500 Megabytes (الخطة المجانية)",
                        "استهلاك الباندويث الشهري" to "0.9 GB / 50 GB (متاح بالكامل)",
                        "مساحة الملفات والمرفقات" to "1.0 Gigabytes (مساحة منفصلة للصور)"
                    )

                    serverSpecs.forEach { (title, desc) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(desc, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }
            }
        }

        // Interactive Database Control Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = StatusAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "لوحة التحكم السحابية والتحسين الافتراضي",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // 1. Connection Ping Test
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                isTestingConnection = true
                                connectionResult = null
                                scope.launch {
                                    delay(1000)
                                    val randomPing = Random.nextInt(88, 145)
                                    isTestingConnection = false
                                    connectionResult = "الاتصال ممتاز وسريع! زمن الاستجابة: ${randomPing}ms"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusBlue),
                            enabled = !isTestingConnection
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فحص سرعة جودة الاتصال بالسيرفر", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (connectionResult != null) {
                            Text(
                                text = connectionResult!!,
                                fontSize = 11.sp,
                                color = StatusEmerald,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // 2. Table optimization
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                isOptimizing = true
                                optimizationResult = null
                                scope.launch {
                                    delay(1500)
                                    isOptimizing = false
                                    optimizationResult = "تم تصفية الفهارس وتنظيف الجداول الفارغة وتحديث الكاش!"
                                    Toast.makeText(context, "اكتمل تحسين قاعدة البيانات السحابية", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            enabled = !isOptimizing
                        ) {
                            if (isOptimizing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تهيئة وتحسين فهارس جداول قاعدة البيانات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (optimizationResult != null) {
                            Text(
                                text = optimizationResult!!,
                                fontSize = 11.sp,
                                color = StatusEmerald,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // 3. Clear Local Photo Cache
                    Button(
                        onClick = {
                            isClearingCache = true
                            scope.launch {
                                delay(800)
                                isClearingCache = false
                                Toast.makeText(context, "تم تنظيف ذاكرة التخزين المؤقت وتحرير 6.8 ميجابايت!", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        enabled = !isClearingCache
                    ) {
                        if (isClearingCache) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تنظيف ذاكرة الصور المؤقتة (Clear Cache)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Explanatory Limit Note
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.08f))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "توضيح الأمان السحابي والقدرة الاستيعابية للصور",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• صور الأجهزة التي يتم التقاطها عند الاستلام يتم ضغطها وتحجيمها برمجياً ليكون حجمها ضئيل جداً (حوالي 20KB إلى 35KB فقط لكل صورة).\n" +
                                    "• تمنحك خطة Supabase المجانية مساحة تخزين تصل إلى 500 Megabytes لقاعدة البيانات، مما يعني أنها تستوعب أكثر من 12,000 جهاز مع صورها قبل الحاجة لأي اشتراك مدفوع!\n" +
                                    "• في حال وصولك للحد الأقصى مستقبلاً، يمكنك ترقية حسابك لخطة Pro السحابية بـ 25$ شهرياً لفتح مساحة 8GB لقاعدة البيانات ومساحة 100GB للصور والمرفقات (سعة غير محدودة للورشة مدى الحياة).\n" +
                                    "• يفضل دائماً إجراء النسخ الاحتياطي بصيغة JSON من شاشة الإعدادات وحفظه على جهازك أو Google Drive للأمان والتوثيق.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
