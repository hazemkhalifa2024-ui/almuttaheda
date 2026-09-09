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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShopConfig
import com.example.data.model.User
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    shops: List<ShopConfig>,
    users: List<User> = emptyList(),
    onLoadShops: () -> Unit,
    onAddShop: (String, String, String, String, String, String, String, Int, String?, String?) -> Unit,
    onUpdateSubscription: (String, Boolean, String, Int) -> Unit,
    onDeleteShop: (String) -> Unit = {},
    onAddUserToShop: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onResetUserDeviceId: (Long) -> Unit = {},
    onDeleteUser: (Long) -> Unit = {},
    onUpdateUser: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    currentServerUrl: String = "http://179.198.203.86:8000/rest/v1/",
    currentAnonKey: String = "",
    currentBasicUser: String = "",
    currentBasicPass: String = "",
    onSaveServerConfig: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onTestConnection: suspend (String, String, String, String) -> Result<String> = { _, _, _, _ -> Result.success("OK") },
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showServerConfigDialog by remember { mutableStateOf(false) }
    var shopToEdit by remember { mutableStateOf<ShopConfig?>(null) }
    var shopForUsersManagement by remember { mutableStateOf<ShopConfig?>(null) }
    var shopToDelete by remember { mutableStateOf<ShopConfig?>(null) }

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
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldPrimary.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "منصة إدارة المحلات والورش المشتركة",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "تسجيل المحلات، تمديد الاشتراكات، وإدارة صلاحيات ومستخدمي كل فرع.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { showServerConfigDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "إعدادات السيرفر والمصادقة", tint = EmeraldPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showServerConfigDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إعدادات الربط بالسيرفر والمصادقة (Basic Auth / API Key)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
                                "لا توجد محلات مسجلة حتى الآن",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(shops) { shop ->
                    val shopUsers = users.filter { it.shopId == shop.id }
                    ShopItemCard(
                        shop = shop,
                        shopUsersCount = shopUsers.size,
                        onEditClick = { shopToEdit = shop },
                        onManageUsersClick = { shopForUsersManagement = shop },
                        onDeleteClick = { shopToDelete = shop }
                    )
                }
            }
        }
    }

    if (shopToDelete != null) {
        val s = shopToDelete!!
        AlertDialog(
            onDismissRequest = { shopToDelete = null },
            title = {
                Text("تأكيد حذف المحل", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف محل \"${s.name}\" المعرف بـ (${s.id})؟ سيتم إزالة المحل من القائمة.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteShop(s.id)
                        shopToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("حذف المحل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { shopToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showCreateDialog) {
        CreateShopDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { id, name, addr, phone, footer, status, expires, maxUsers, adminUser, adminPass ->
                onAddShop(id, name, addr, phone, footer, status, expires, maxUsers, adminUser, adminPass)
                showCreateDialog = false
            }
        )
    }

    if (showServerConfigDialog) {
        ServerConnectionDialog(
            initialUrl = currentServerUrl,
            initialAnonKey = currentAnonKey,
            initialBasicUser = currentBasicUser,
            initialBasicPass = currentBasicPass,
            onDismiss = { showServerConfigDialog = false },
            onSave = { url, key, u, p ->
                onSaveServerConfig(url, key, u, p)
                showServerConfigDialog = false
            },
            onTestConnection = onTestConnection
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

    if (shopForUsersManagement != null) {
        val selectedShop = shopForUsersManagement!!
        val shopUsers = users.filter { it.shopId == selectedShop.id }
        ShopUsersManagementDialog(
            shop = selectedShop,
            shopUsers = shopUsers,
            onDismiss = { shopForUsersManagement = null },
            onAddUser = { username, password, role ->
                onAddUserToShop(username, password, role, selectedShop.id)
            },
            onResetDevice = { userId ->
                onResetUserDeviceId(userId)
            },
            onDeleteUser = { userId ->
                onDeleteUser(userId)
            },
            onUpdateUser = { userId, username, password, role ->
                onUpdateUser(userId, username, password, role)
            }
        )
    }
}

@Composable
fun ShopItemCard(
    shop: ShopConfig,
    shopUsersCount: Int,
    onEditClick: () -> Unit,
    onManageUsersClick: () -> Unit,
    onDeleteClick: () -> Unit = {}
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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
                    Text(
                        "$shopUsersCount من أصل ${shop.userLimit} مستخدم",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (shopUsersCount >= shop.userLimit) RedAccent else EmeraldPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("تاريخ انتهاء الاشتراك:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(shop.subscriptionExpiresAt ?: "مفتوح", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EmeraldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onManageUsersClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                        contentColor = EmeraldPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المستخدمين ($shopUsersCount)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحديث الترخيص", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                if (shop.id != "default_shop") {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedAccent.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف المحل", tint = RedAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShopDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String, Int, String?, String?) -> Unit
) {
    // Generate an automatic unique shop ID e.g. shop_4829
    fun generateRandomShopId(): String = "shop_${(1000..9999).random()}"
    fun generateRandomPassword(): String = (100000..999999).random().toString()

    var id by remember { mutableStateOf(generateRandomShopId()) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var receiptFooter by remember { mutableStateOf("ضمان صيانة معتمد") }
    var userLimit by remember { mutableStateOf("5") }
    var expiresAt by remember { mutableStateOf("2027-12-31") }

    // First time admin user credentials
    var adminUsername by remember { mutableStateOf("admin") }
    var adminPassword by remember { mutableStateOf("123456") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    "تسجيل ورشة/محل جديد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        "بيانات الفرع والورشة الأساسية:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newName ->
                            name = newName
                            // If admin username was default, suggest a custom one
                            if (adminUsername == "admin" || adminUsername.startsWith("admin_")) {
                                val cleanName = newName.trim().take(5)
                                if (cleanName.isNotBlank()) {
                                    adminUsername = "admin_$cleanName"
                                }
                            }
                        },
                        label = { Text("اسم المحل / الورشة (عربي) *") },
                        placeholder = { Text("مثال: ورشة الأهرام للصيانة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it },
                        label = { Text("المعرف الفريد للمحل (تم التوليد تلقائياً) *") },
                        placeholder = { Text("shop_1234") },
                        trailingIcon = {
                            IconButton(onClick = { id = generateRandomShopId() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "توليد معرف جديد", tint = EmeraldPrimary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان الفعلي للورشة") },
                        placeholder = { Text("مثال: القاهرة - وسط البلد") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم هاتف المحل للتواصل") },
                        placeholder = { Text("01xxxxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("ملاحظة أسفل الفاتورة (الضمان)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = userLimit,
                            onValueChange = { userLimit = it },
                            label = { Text("حد الموظفين") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = expiresAt,
                            onValueChange = { expiresAt = it },
                            label = { Text("تاريخ الانتهاء") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = EmeraldPrimary.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Text(
                                    "بيانات حساب المدير الأول للمحل (لتسجيل الدخول فوراً):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                            Text(
                                "سيتم إنشاء هذا الحساب تلقائياً بصلاحيات مدير كاملة لتمكين صاحب المحل من الدخول فوراً.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = adminUsername,
                                onValueChange = { adminUsername = it },
                                label = { Text("اسم مستخدم المدير") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = adminPassword,
                                onValueChange = { adminPassword = it },
                                label = { Text("كلمة مرور المدير") },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { adminPassword = generateRandomPassword() }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "توليد كلمة سر عشوائية", tint = EmeraldPrimary)
                                        }
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (id.isNotBlank() && name.isNotBlank()) {
                        val limitInt = userLimit.toIntOrNull() ?: 5
                        onConfirm(
                            id.trim(),
                            name.trim(),
                            address.trim(),
                            phone.trim(),
                            receiptFooter.trim(),
                            "active",
                            expiresAt.trim(),
                            limitInt,
                            adminUsername.trim().ifBlank { null },
                            adminPassword.trim().ifBlank { null }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("تسجيل المحل والحساب فوراً", fontWeight = FontWeight.Bold, color = Color.White)
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
fun ShopUsersManagementDialog(
    shop: ShopConfig,
    shopUsers: List<User>,
    onDismiss: () -> Unit,
    onAddUser: (String, String, String) -> Unit,
    onResetDevice: (Long) -> Unit,
    onDeleteUser: (Long) -> Unit,
    onUpdateUser: (Long, String, String, String) -> Unit
) {
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = EmeraldPrimary)
                        Text(
                            "مستخدمو ورشة: ${shop.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }
                Text(
                    "المعرف الفريد: ${shop.id} | الإجمالي: ${shopUsers.size} من ${shop.userLimit} مسموح",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (shopUsers.size.toFloat() / shop.userLimit.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (shopUsers.size >= shop.userLimit) RedAccent else EmeraldPrimary,
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showAddUserDialog = true },
                    enabled = shopUsers.size < shop.userLimit,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (shopUsers.size < shop.userLimit) "إضافة مستخدم جديد لهذا المحل"
                        else "تم الوصول للحد الأقصى لاشتراك المحل (${shop.userLimit})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (shopUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PersonOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا يوجد مستخدمون مسجلون في هذا المحل بعد.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        items(shopUsers) { user ->
                            ShopUserCard(
                                user = user,
                                onResetDevice = { onResetDevice(user.id) },
                                onEdit = { userToEdit = user },
                                onDelete = { userToDelete = user }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("تم")
            }
        }
    )

    if (showAddUserDialog) {
        AddUserToShopDialog(
            shopName = shop.name,
            onDismiss = { showAddUserDialog = false },
            onConfirm = { u, p, r ->
                onAddUser(u, p, r)
                showAddUserDialog = false
            }
        )
    }

    if (userToEdit != null) {
        EditUserForShopDialog(
            user = userToEdit!!,
            onDismiss = { userToEdit = null },
            onConfirm = { id, u, p, r ->
                onUpdateUser(id, u, p, r)
                userToEdit = null
            }
        )
    }

    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("تأكيد حذف المستخدم", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف حساب المستخدم (${userToDelete?.username}) نهائياً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.id?.let { onDeleteUser(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("نعم، حذف الحساب", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ShopUserCard(
    user: User,
    onResetDevice: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (user.role == "admin") Icons.Default.Shield else Icons.Default.Person,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = user.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        val roleLabel = when (user.role) {
                            "admin" -> "مدير فرع"
                            "technician" -> "فني صيانة"
                            else -> "موظف استقبال"
                        }
                        Text(
                            text = "الدور: $roleLabel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Device Lock Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (user.deviceId.isNullOrBlank()) EmeraldPrimary.copy(alpha = 0.1f)
                        else RedAccent.copy(alpha = 0.1f)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (user.deviceId.isNullOrBlank()) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (user.deviceId.isNullOrBlank()) EmeraldPrimary else RedAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (user.deviceId.isNullOrBlank()) "غير مقيد بجهاز (متاح للتفعيل)"
                        else "مقيد بجهاز: ${user.deviceId.take(10)}...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (user.deviceId.isNullOrBlank()) EmeraldPrimary else RedAccent
                    )
                }

                if (!user.deviceId.isNullOrBlank()) {
                    TextButton(
                        onClick = onResetDevice,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = RedAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فك القيد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RedAccent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserToShopDialog(
    shopName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val cleanPrefix = shopName.trim().ifBlank { "shop" }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("technician") }

    val finalUsername = remember(username, cleanPrefix) {
        val trimmed = username.trim()
        if (trimmed.startsWith("$cleanPrefix-")) trimmed else "$cleanPrefix-$trimmed"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مستخدم لفرع: $shopName", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم") },
                    prefix = { Text("$cleanPrefix-", fontWeight = FontWeight.Bold, color = EmeraldPrimary) },
                    supportingText = if (username.isNotBlank()) {
                        { Text("اسم الدخول: $finalUsername", fontSize = 11.sp, color = EmeraldPrimary) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("الدور / الوظيفة:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("admin" to "مدير", "technician" to "فني", "reception" to "استقبال").forEach { (rKey, rLabel) ->
                        FilterChip(
                            selected = role == rKey,
                            onClick = { role = rKey },
                            label = { Text(rLabel, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        onConfirm(finalUsername, password.trim(), role)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("حفظ المستخدم", color = Color.White)
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
fun EditUserForShopDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(user.username) }
    var password by remember { mutableStateOf(user.password) }
    var role by remember { mutableStateOf(user.role) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات: ${user.username}", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرور") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("الدور / الوظيفة:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("admin" to "مدير", "technician" to "فني", "reception" to "استقبال").forEach { (rKey, rLabel) ->
                        FilterChip(
                            selected = role == rKey,
                            onClick = { role = rKey },
                            label = { Text(rLabel, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        onConfirm(user.id, username, password, role)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("تحديث البيانات", color = Color.White)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConnectionDialog(
    initialUrl: String,
    initialAnonKey: String,
    initialBasicUser: String,
    initialBasicPass: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onTestConnection: suspend (String, String, String, String) -> Result<String>
) {
    var url by remember { mutableStateOf(initialUrl) }
    var anonKey by remember { mutableStateOf(initialAnonKey) }
    var basicUser by remember { mutableStateOf(initialBasicUser) }
    var basicPass by remember { mutableStateOf(initialBasicPass) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Cloud, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    "إعدادات الربط بالسيرفر والمصادقة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        "اضبط بيانات الاتصال بسيرفر Supabase الخاص بك (Self-Hosted أو Cloud)، وبيانات المصادقة الأساسية (Basic Auth) إن وجدت:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }

                item {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("رابط الـ REST API للسيرفر") },
                        placeholder = { Text("http://179.198.203.86:8000/rest/v1/") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = anonKey,
                        onValueChange = { anonKey = it },
                        label = { Text("مفتاح Supabase (Anon Key / Service Key)") },
                        placeholder = { Text("JWT Token الخاص بـ Supabase") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "المصادقة الأساسية (HTTP Basic Auth) - إن كانت مفعلة على السيرفر:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                item {
                    OutlinedTextField(
                        value = basicUser,
                        onValueChange = { basicUser = it },
                        label = { Text("اسم مستخدم Basic Auth (اختياري)") },
                        placeholder = { Text("مثال: supabase أو admin") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = basicPass,
                        onValueChange = { basicPass = it },
                        label = { Text("كلمة سر Basic Auth (اختياري)") },
                        placeholder = { Text("كلمة مرور السيرفر") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (testResult != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSuccess) EmeraldPrimary.copy(alpha = 0.12f) else RedAccent.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isSuccess) EmeraldPrimary else RedAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = testResult!!,
                                    fontSize = 12.sp,
                                    color = if (isSuccess) EmeraldPrimary else RedAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            testResult = null
                            scope.launch {
                                val res = onTestConnection(url, anonKey, basicUser, basicPass)
                                isTesting = false
                                res.onSuccess { msg ->
                                    isSuccess = true
                                    testResult = msg
                                }.onFailure { err ->
                                    isSuccess = false
                                    testResult = err.message ?: "فشل الاتصال بالسيرفر"
                                }
                            }
                        },
                        enabled = !isTesting && url.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ فحص الاتصال بالسيرفر...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فحص الاتصال المباشر بالسيرفر الآن", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(url, anonKey, basicUser, basicPass)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("حفظ وتطبيق فوراً", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
