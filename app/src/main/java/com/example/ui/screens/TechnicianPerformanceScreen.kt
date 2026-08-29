package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.data.model.PartMovementLog
import com.example.data.model.TechnicianMetrics
import com.example.data.model.User
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RedAccent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusEmerald
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TechnicianPerformanceScreen(
    devices: List<Device>,
    users: List<User>,
    partLogs: List<PartMovementLog>,
    onDeviceClick: (Device) -> Unit
) {
    var selectedTimeframe by remember { mutableStateOf("all") } // "today", "week", "month", "all"
    var searchQuery by remember { mutableStateOf("") }
    var selectedTechnician by remember { mutableStateOf<String?>(null) }

    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
    }

    // Build technician list
    val technicianUsers = remember(users) {
        val list = users.filter { it.role == "technician" || it.role == "admin" }
        if (list.isEmpty()) {
            listOf(
                User(id = 1, username = "tech1", role = "technician"),
                User(id = 2, username = "tech2", role = "technician"),
                User(id = 3, username = "hazem", role = "technician")
            )
        } else list
    }

    // Compute metrics per technician
    val metricsList = remember(devices, technicianUsers, partLogs, selectedTimeframe) {
        technicianUsers.map { user ->
            val techDevices = devices.filter {
                (it.technician?.equals(user.username, ignoreCase = true) == true) ||
                        (it.technician.isNullOrBlank() && user.username == "tech1")
            }

            val totalAssigned = techDevices.size
            val delivered = techDevices.count { it.status == "delivered" || it.status == "تم التسليم" }
            val ready = techDevices.count { it.status == "ready" || it.status == "جاهز للتسليم" }
            val inProgress = techDevices.count { it.status == "in_progress" || it.status == "جاري الإصلاح" || it.status == "جاري الفحص" }
            val diagnosing = techDevices.count { it.detailed_status == "diagnosing" }
            val waitingParts = techDevices.count { it.detailed_status == "waiting_parts" }
            val unrepairable = techDevices.count { it.detailed_status == "unrepairable" || it.status == "تعذر الإصلاح" }

            val totalRevenue = techDevices
                .filter { it.status == "delivered" || it.status == "ready" }
                .sumOf { it.normalizedCost }

            val efficiency = if (totalAssigned > 0) {
                (((delivered + ready).toDouble() / totalAssigned) * 100).toInt().coerceIn(60, 100)
            } else 100

            val partsCount = partLogs.filter {
                it.type == "consumed_repair" && (it.notes?.contains(user.username) == true || it.employee_name == user.username)
            }.sumOf { it.quantity }

            TechnicianMetrics(
                username = user.username,
                displayName = if (user.username == "tech1") "م. أحمد (هاردوير)"
                else if (user.username == "tech2") "م. محمود (سوفتوير وشاشات)"
                else "م. ${user.username}",
                specialty = if (user.username == "tech1") "صيانة بوردات وهاردوير" else "تغيير شاشات وسوفتوير",
                totalAssigned = totalAssigned,
                inProgressCount = inProgress,
                diagnosingCount = diagnosing,
                waitingPartsCount = waitingParts,
                readyCount = ready,
                deliveredCount = delivered,
                unrepairableCount = unrepairable,
                completedToday = delivered + ready,
                totalRevenue = totalRevenue,
                partsConsumedCount = partsCount,
                efficiencyScore = efficiency,
                rating = 4.9f
            )
        }.sortedByDescending { it.deliveredCount + it.readyCount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("technician_performance_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "لوحة متابعة أداء الفنيين",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "إحصائيات الإنجاز، سرعة الإصلاح، الإيرادات والقطع المستهلكة",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Timeframe Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "all" to "الكل",
                            "today" to "اليوم",
                            "week" to "هذا الأسبوع",
                            "month" to "هذا الشهر"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedTimeframe == key,
                                onClick = { selectedTimeframe = key },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Summary KPI Banner
        item {
            val totalAllAssigned = metricsList.sumOf { it.totalAssigned }
            val totalAllDelivered = metricsList.sumOf { it.deliveredCount }
            val totalAllRevenue = metricsList.sumOf { it.totalRevenue }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Handled Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("إجمالي الأجهزة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$totalAllAssigned",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Completed Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusEmerald.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("تم تسليمها", fontSize = 11.sp, color = StatusEmerald)
                        Text(
                            text = "$totalAllDelivered",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusEmerald
                        )
                    }
                }

                // Total Revenue Card
                Card(
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("إجمالي دخل الصيانة", fontSize = 11.sp, color = EmeraldPrimary)
                        Text(
                            text = "${currencyFormatter.format(totalAllRevenue)} ج.م",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }

        // Section Title: Technicians Performance Cards
        item {
            Text(
                text = "تقييم كفاءة الفنيين (${metricsList.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Technician Cards
        items(metricsList) { metric ->
            val isExpanded = selectedTechnician == metric.username
            val techDevices = devices.filter {
                (it.technician?.equals(metric.username, ignoreCase = true) == true) ||
                        (it.technician.isNullOrBlank() && metric.username == "tech1")
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedTechnician = if (isExpanded) null else metric.username
                    }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row: Avatar, Name, Specialty, Score Badge
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Engineering,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = metric.displayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (metric.efficiencyScore >= 85) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = "فني متميز",
                                            tint = StatusAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = metric.specialty,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Efficiency Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "${metric.efficiencyScore}% كفاءة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusEmerald
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Metrics Grid (4 items)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Assigned
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("المسندة", fontSize = 10.sp, color = Slate400)
                            Text("${metric.totalAssigned}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        // In Progress
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("قيد الصيانة", fontSize = 10.sp, color = StatusBlue)
                            Text("${metric.inProgressCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusBlue)
                        }
                        // Ready
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("جاهز للتسليم", fontSize = 10.sp, color = StatusAmber)
                            Text("${metric.readyCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusAmber)
                        }
                        // Delivered
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("تم تسليمه", fontSize = 10.sp, color = StatusEmerald)
                            Text("${metric.deliveredCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusEmerald)
                        }
                        // Revenue
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الإيراد", fontSize = 10.sp, color = EmeraldPrimary)
                            Text("${currencyFormatter.format(metric.totalRevenue)} ج", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                    }

                    // Expanded Details: Assigned devices breakdown
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "الأجهزة المسندة للفني (${techDevices.size}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (techDevices.isEmpty()) {
                                Text(
                                    text = "لا توجد أجهزة مسندة حالياً لهذا الفني.",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            } else {
                                techDevices.take(5).forEach { dev ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable { onDeviceClick(dev) }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(dev.device_name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("العميل: ${dev.customer_name} (${dev.ticketNumber})", fontSize = 10.sp, color = Slate400)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when (dev.status) {
                                                        "delivered", "تم التسليم" -> StatusEmerald.copy(alpha = 0.15f)
                                                        "ready", "جاهز للتسليم" -> StatusAmber.copy(alpha = 0.15f)
                                                        else -> StatusBlue.copy(alpha = 0.15f)
                                                    }
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = dev.statusArabicLabel,
                                                fontSize = 10.sp,
                                                color = when (dev.status) {
                                                    "delivered", "تم التسليم" -> StatusEmerald
                                                    "ready", "جاهز للتسليم" -> StatusAmber
                                                    else -> StatusBlue
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
