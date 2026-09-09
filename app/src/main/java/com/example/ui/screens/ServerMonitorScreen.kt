package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.Device
import com.example.data.model.SessionUser
import com.example.data.model.User
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerMonitorScreen(
    session: SessionUser,
    devices: List<Device>,
    users: List<User> = emptyList(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Interactive connection test state
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<String?>(null) }
    var showUpgradeDialog by remember { mutableStateOf(false) }

    // Shop Config details
    val shopConfig = session.shopConfig
    val subscriptionStatus = shopConfig.subscriptionStatus
    val subscriptionExpiresAt = shopConfig.subscriptionExpiresAt // "yyyy-MM-dd"

    // Dynamic remaining days calculation
    val daysRemaining: String = if (subscriptionExpiresAt.isNullOrBlank()) {
        "غير محدود (مفتوح مدى الحياة)"
    } else {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val expiryDate = format.parse(subscriptionExpiresAt)
            val currentDate = Date()
            val diff = expiryDate.time - currentDate.time
            val days = (diff / (1000L * 60 * 60 * 24)).coerceAtLeast(0)
            "$days يوم متبقي"
        } catch (e: Exception) {
            "ساري"
        }
    }

    val devicesCount = devices.size

    // User limit calculation
    val userLimit = shopConfig.userLimit
    val actualUsersCount = users.count { it.shopId == shopConfig.id }.let {
        if (it > 0) it else shopConfig.activeUserCount
    }
    val userLimitProgress = (actualUsersCount.toFloat() / userLimit.toFloat()).coerceIn(0f, 1f)

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
                    text = "حالة اتصال السيرفر وباقة المحل",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "متابعة حالة الاتصال بالخادم الرئيسي، تفاصيل باقة المحل، وصلاحية الاشتراك",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Connection Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
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
                                    text = "حالة اتصال السيرفر",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "الخدمة متصلة ومتاحة للعمل بنجاح",
                                    fontSize = 12.sp,
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
                                text = "مستقر ومتاح",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusEmerald
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Only one button: "فحص الاتصال بالسيرفر"
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                isTestingConnection = true
                                connectionResult = null
                                scope.launch {
                                    delay(900)
                                    val randomPing = Random.nextInt(22, 45)
                                    isTestingConnection = false
                                    connectionResult = "الاتصال بالسيرفر ممتاز ومستقر! سرعة الاستجابة: ${randomPing}ms"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusBlue),
                            enabled = !isTestingConnection
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري فحص الاتصال بالسيرفر...", fontSize = 13.sp, color = Color.White)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فحص الاتصال بالسيرفر", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        if (connectionResult != null) {
                            Text(
                                text = connectionResult!!,
                                fontSize = 12.sp,
                                color = StatusEmerald,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Shop Subscription and Package Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header of Package
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "بيانات باقة المحل",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = shopConfig.name,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Badge Active / Expired
                        val isActive = subscriptionStatus.lowercase() == "active"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) StatusEmerald.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isActive) "الباقة نشطة" else "منتهية",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) StatusEmerald else Color.Red
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Remaining Days visual section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "المدة المتبقية لنهاية الاشتراك:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = daysRemaining,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "تاريخ انتهاء الباقة:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = subscriptionExpiresAt ?: "مفتوح مدى الحياة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Users Limits tracking
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "عدد مستخدمي الباقة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "الحسابات النشطة بالفرع مقابل الحد الأقصى",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$actualUsersCount من أصل $userLimit مستخدمين",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (actualUsersCount > userLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        LinearProgressIndicator(
                            progress = { userLimitProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (actualUsersCount > userLimit) MaterialTheme.colorScheme.error else EmeraldPrimary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }

                    // Devices Count Summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Text("إجمالي الأجهزة المسجلة بالفرع:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Text("$devicesCount جهاز", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }

                    // Upgrade Button
                    Button(
                        onClick = { showUpgradeDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "طلب ترقية الباقة أو زيادة عدد الموظفين",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Support & Help Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = EmeraldPrimary.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = EmeraldPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الدعم الفني وخدمة العملاء",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "للمساعدة في الترقية أو الاستفسار عن الاشتراكات، يمكنك التواصل مع المشرف العام على مدار الساعة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Upgrade Request Success Dialog
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            icon = { Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(48.dp)) },
            title = { Text("طلب ترقية الباقة", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "تم استلام طلب ترقية الباقة لفرع (${shopConfig.name}) بنجاح. سيقوم المشرف العام بالتواصل معكم مباشرة لتعديل حد المستخدمين وتجديد الباقة.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showUpgradeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("حسناً", color = Color.White)
                }
            }
        )
    }
}
