package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    devices: List<Device>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onDeviceClick: (Device) -> Unit,
    onNewDeviceClick: () -> Unit,
    onSendReportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayString = remember { todayFormat.format(Date()) }
    val isDark = isSystemInDarkTheme()

    val todayDevices = remember(devices) {
        devices.filter {
            it.created_at?.startsWith(todayString) == true || it.id > 0
        }
    }
    val readyDevices = remember(devices) {
        devices.filter { it.status == "ready" || it.status == "جاهز للتسليم" }
    }
    val deliveredRevenue = remember(devices) {
        devices.filter { it.status == "delivered" || it.status == "تم التسليم" }
            .sumOf { it.normalizedCost }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "نظرة عامة على الورشة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "إحصائيات مباشرة وتدفق حركة الأجهزة",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = EmeraldPrimaryLight,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Stats Grid in Immersive UI style
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImmersiveStatCard(
                        title = "أجهزة اليوم",
                        value = "${todayDevices.size}",
                        accentColor = StatusBlue,
                        liveBadge = "+${todayDevices.size} جديد اليوم",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    ImmersiveStatCard(
                        title = "جاهز للتسليم",
                        value = "${readyDevices.size}",
                        accentColor = StatusEmerald,
                        subtitle = "جاري تجهيز الفواتير",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImmersiveStatCard(
                        title = "إيراد التسليم",
                        value = "${deliveredRevenue.toInt()} ج.م",
                        accentColor = StatusAmber,
                        subtitle = "المبالغ المحصلة",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    ImmersiveStatCard(
                        title = "إجمالي الأجهزة",
                        value = "${devices.size}",
                        accentColor = StatusPurple,
                        subtitle = "سجل الصيانة الشامل",
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Action Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(EmeraldPrimary)
                    .clickable { onNewDeviceClick() }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "تسجيل جهاز صيانة جديد",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "إصدار تذكرة صيانة سريعة مع ملصق الباركود",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Send Report to Manager via n8n/WhatsApp
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(StatusAmber)
                    .clickable { onSendReportClick() }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إرسال التقرير المالي والإداري للمدير",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "مشاركة إحصائيات الدخل، العربون والمتبقي فوراً لـ n8n/WhatsApp",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Recent Operations Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر العمليات",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${devices.size} جهاز مسجل",
                    fontSize = 11.sp,
                    color = EmeraldPrimaryLight,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Devices List
        if (devices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Slate900.copy(alpha = 0.5f) else Color.White)
                        .border(1.dp, if (isDark) Slate800.copy(alpha = 0.6f) else Slate200, RoundedCornerShape(24.dp))
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لا توجد أجهزة مسجلة حتى الآن",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate400
                        )
                    }
                }
            }
        } else {
            items(devices.take(10), key = { it.id }) { device ->
                ImmersiveDeviceCard(
                    device = device,
                    isDark = isDark,
                    onClick = { onDeviceClick(device) }
                )
            }
        }
    }
}

@Composable
fun ImmersiveStatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    liveBadge: String? = null,
    subtitle: String? = null,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) Slate900.copy(alpha = 0.45f) else Color.White)
            .border(1.dp, if (isDark) Slate800.copy(alpha = 0.7f) else Slate200, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Slate400,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Slate900
            )

            if (liveBadge != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .alpha(pulseAlpha)
                            .background(StatusEmerald, CircleShape)
                    )
                    Text(
                        text = liveBadge,
                        fontSize = 10.sp,
                        color = StatusEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Slate500
                )
            }
        }
    }
}

@Composable
fun ImmersiveDeviceCard(
    device: Device,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pick device icon and theme tint based on model name
    val (deviceIcon, tintColor) = remember(device.device_name) {
        val name = device.device_name.lowercase()
        when {
            name.contains("laptop") || name.contains("dell") || name.contains("hp") || name.contains("macbook") || name.contains("victus") -> {
                Icons.Default.Laptop to StatusEmerald
            }
            name.contains("ipad") || name.contains("tab") -> {
                Icons.Default.Tablet to StatusPurple
            }
            else -> {
                Icons.Default.PhoneAndroid to StatusBlue
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) Slate900.copy(alpha = 0.6f) else Color.White)
            .border(1.dp, if (isDark) Slate800.copy(alpha = 0.5f) else Slate200, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Device Icon in rounded 2xl box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tintColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = deviceIcon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = device.device_name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${device.customer_name} • ${device.issue_description?.ifBlank { "فحص عام" } ?: "فحص عام"}",
                        fontSize = 11.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusBadge(status = device.status)
                Text(
                    text = device.ticketNumber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate500
                )
            }
        }
    }
}

