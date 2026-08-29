package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.CustomerProfile
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

@Composable
fun CustomerHistoryScreen(
    customerProfiles: List<CustomerProfile>,
    onSelectCustomer: (CustomerProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val filteredProfiles = remember(customerProfiles, searchQuery) {
        if (searchQuery.isBlank()) {
            customerProfiles
        } else {
            val q = searchQuery.trim().lowercase()
            customerProfiles.filter { profile ->
                profile.name.lowercase().contains(q) ||
                profile.phone.contains(q) ||
                profile.devices.any { dev ->
                    dev.device_name.lowercase().contains(q) ||
                    dev.ticketNumber.lowercase().contains(q) ||
                    (dev.issue_description?.lowercase()?.contains(q) == true)
                }
            }
        }
    }

    val totalVip = remember(customerProfiles) { customerProfiles.count { it.totalRepairs >= 3 } }
    val totalRevenue = remember(customerProfiles) { customerProfiles.sumOf { it.totalSpent } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_history_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "سجل وبيانات العملاء",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "تاريخ الأجهزة، الأعطال السابقة، وتفاصيل الزيارات المتكررة",
                    fontSize = 11.sp,
                    color = Slate400
                )
            }
        }

        // Summary Statistics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetricCard(
                    title = "إجمالي العملاء",
                    value = "${customerProfiles.size}",
                    color = StatusBlue,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = "عملاء دائمون (VIP)",
                    value = "$totalVip",
                    color = StatusAmber,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = "إجمالي الإيراد",
                    value = "${totalRevenue.toInt()} ج.م",
                    color = StatusEmerald,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_search_input"),
                placeholder = { Text("بحث باسم العميل، رقم الهاتف، أو نوع الجهاز...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Slate400)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // List Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قائمة ملفات العملاء (${filteredProfiles.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
            }
        }

        // Customer Profile Items
        if (filteredProfiles.isEmpty()) {
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
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لا توجد سجلات تطابق البحث",
                            fontSize = 14.sp,
                            color = Slate400
                        )
                    }
                }
            }
        } else {
            items(filteredProfiles, key = { it.phone.ifBlank { it.name } }) { profile ->
                CustomerProfileItemCard(
                    profile = profile,
                    isDark = isDark,
                    onClick = { onSelectCustomer(profile) },
                    onCall = {
                        if (profile.phone.isNotBlank()) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.phone}")))
                            } catch (e: Exception) {}
                        }
                    },
                    onWhatsApp = {
                        if (profile.phone.isNotBlank()) {
                            val clean = profile.phone.replace("+", "").replace(" ", "")
                            val url = "https://api.whatsapp.com/send?phone=$clean"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (e: Exception) {}
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Slate900.copy(alpha = 0.45f) else Color.White)
            .border(1.dp, if (isDark) Slate800.copy(alpha = 0.7f) else Slate200, RoundedCornerShape(20.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun CustomerProfileItemCard(
    profile: CustomerProfile,
    isDark: Boolean,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVip = profile.totalRepairs >= 3

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) Slate900.copy(alpha = 0.6f) else Color.White)
            .border(1.dp, if (isDark) Slate800.copy(alpha = 0.5f) else Slate200, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isVip) StatusAmber.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(1).uppercase(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVip) StatusAmber else EmeraldPrimaryLight
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Slate900
                            )
                            if (isVip) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(StatusAmber.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = StatusAmber,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            "VIP",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusAmber
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = if (profile.phone.isNotBlank()) profile.phone else "بدون رقم هاتف",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                // Total Spend & Repairs Counter
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${profile.totalSpent.toInt()} ج.م",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimaryLight
                    )
                    Text(
                        text = "${profile.totalRepairs} أجهزة صيانة",
                        fontSize = 10.sp,
                        color = Slate400
                    )
                }
            }

            // Latest Device preview chip
            val latestDevice = profile.devices.firstOrNull()
            if (latestDevice != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Slate850 else Slate100)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "آخر جهاز: ${latestDevice.device_name} (${latestDevice.ticketNumber})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = latestDevice.statusArabicLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimaryLight
                        )
                    }
                }
            }

            // Bottom Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (profile.phone.isNotBlank()) {
                        IconButton(
                            onClick = onCall,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusBlue.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "اتصال",
                                tint = StatusBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onWhatsApp,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusEmerald.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "واتساب",
                                tint = StatusEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "عرض السجل الكامل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimaryLight
                    )
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = EmeraldPrimaryLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
