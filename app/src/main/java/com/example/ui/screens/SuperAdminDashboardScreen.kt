package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShopConfig
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    shops: List<ShopConfig>,
    onLoadShops: () -> Unit,
    onAddShop: (String, String, String, String, String, String, String, Int) -> Unit,
    onUpdateSubscription: (String, Boolean, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var shopToEdit by remember { mutableStateOf<ShopConfig?>(null) }

    LaunchedEffect(Unit) {
        onLoadShops()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_shop_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("إضافة ورشة/محل جديد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        modifier = modifier.fillMaxSize().testTag("saas_management_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "منصة إدارة المحلات والورش المشتركة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "بصفتك مدير المنصة، يمكنك هنا تسجيل المحلات والورش الجديدة، تمديد الاشتراكات، وإدارة حدود المستخدمين لكل فرع معزول.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            if (shops.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد ورش مسجلة حالياً أو يرجى السحب للتحديث.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(shops) { shop ->
                    ShopItemCard(
                        shop = shop,
                        onEditClick = { shopToEdit = shop }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateShopDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { id, name, addr, phone, footer, status, expires, maxUsers ->
                onAddShop(id, name, addr, phone, footer, status, expires, maxUsers)
                showCreateDialog = false
            }
        )
    }

    if (shopToEdit != null) {
        EditSubscriptionDialog(
            shop = shopToEdit!!,
            onDismiss = { shopToEdit = null },
            onConfirm = { id, isActive, expires, limit ->
                onUpdateSubscription(id, isActive, expires, limit)
                shopToEdit = null
            }
        )
    }
}

@Composable
fun ShopItemCard(
    shop: ShopConfig,
    onEditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (shop.isSubscriptionActive) EmeraldPrimary.copy(alpha = 0.1f)
                            else RedAccent.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = if (shop.isSubscriptionActive) EmeraldPrimary else RedAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "المعرف الفريد: ${shop.id}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (shop.isSubscriptionActive) EmeraldPrimary.copy(alpha = 0.15f)
                            else RedAccent.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (shop.isSubscriptionActive) "نشط" else "منتهي الصلاحية",
                        color = if (shop.isSubscriptionActive) EmeraldPrimary else RedAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("العنوان والاتصال:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${shop.address} | ${shop.phone ?: "لا يوجد"}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("حد الموظفين المتاحين:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${shop.activeUserCount} من أصل ${shop.userLimit} مستخدم", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("تاريخ انتهاء الاشتراك:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(shop.subscriptionExpiresAt ?: "مفتوح", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EmeraldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onEditClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تحديث حالة الترخيص والحدود", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShopDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String, Int) -> Unit
) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var receiptFooter by remember { mutableStateOf("") }
    var userLimit by remember { mutableStateOf("5") }
    var expiresAt by remember { mutableStateOf("2027-12-31") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "تسجيل فرع/محل جديد في المنصة",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it },
                        label = { Text("معرف الفرع الفريد (بالإنجليزي - بدون مسافات)") },
                        placeholder = { Text("مثال: united_cairo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المحل / الورشة (عربي)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان الفعلي للورشة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف للتواصل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("ملاحظة أسفل الفاتورة (الضمان)") },
                        placeholder = { Text("مثال: ضمان شهر ضد عيوب الصيانة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = userLimit,
                        onValueChange = { userLimit = it },
                        label = { Text("الحد الأقصى لعدد حسابات الموظفين") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = expiresAt,
                        onValueChange = { expiresAt = it },
                        label = { Text("تاريخ انتهاء الترخيص (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (id.isNotBlank() && name.isNotBlank()) {
                        val limitInt = userLimit.toIntOrNull() ?: 5
                        onConfirm(id, name, address, phone, receiptFooter, "active", expiresAt, limitInt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("إتمام الحفظ والترخيص", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubscriptionDialog(
    shop: ShopConfig,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, String, Int) -> Unit
) {
    var isActive by remember { mutableStateOf(shop.isSubscriptionActive) }
    var expiresAt by remember { mutableStateOf(shop.subscriptionExpiresAt ?: "2027-12-31") }
    var userLimit by remember { mutableStateOf(shop.userLimit.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "تحديث اشتراك: ${shop.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("حالة تفعيل الحساب والاشتراك:", fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }

                OutlinedTextField(
                    value = expiresAt,
                    onValueChange = { expiresAt = it },
                    label = { Text("تاريخ انتهاء الاشتراك (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = userLimit,
                    onValueChange = { userLimit = it },
                    label = { Text("الحد الأقصى لعدد حسابات الموظفين") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limitInt = userLimit.toIntOrNull() ?: shop.userLimit
                    onConfirm(shop.id, isActive, expiresAt, limitInt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("تحديث الترخيص فورياً", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
