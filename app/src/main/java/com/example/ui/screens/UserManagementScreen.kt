package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    users: List<User>,
    currentUsername: String,
    onCreateUser: (String, String, String) -> Unit,
    onUpdateUser: (Long, String, String, String) -> Unit,
    onDeleteUser: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_user_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                    Text("إنشاء حساب جديد", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier.fillMaxSize().testTag("user_management_screen")
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
                        text = "إدارة المستخدمين وحسابات الموظفين",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "يمكنك إضافة حسابات فنيين وموظفين جدد، تعديل كلمات المرور أو الصلاحيات، وحذف الحسابات القديمة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            if (users.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد حسابات مستخدمين حالياً.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(users, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        isCurrentUser = user.username.equals(currentUsername, ignoreCase = true),
                        onEdit = { userToEdit = user },
                        onDelete = { userToDelete = user }
                    )
                }
            }
        }
    }

    // Dialog: Create User
    if (showCreateDialog) {
        UserFormDialog(
            title = "إنشاء حساب جديد للموظف",
            confirmLabel = "إنشاء الحساب",
            onDismiss = { showCreateDialog = false },
            onSubmit = { username, password, role ->
                onCreateUser(username, password, role)
                showCreateDialog = false
            }
        )
    }

    // Dialog: Edit User
    userToEdit?.let { user ->
        UserFormDialog(
            title = "تعديل بيانات الحساب (${user.username})",
            confirmLabel = "حفظ التعديلات",
            initialUsername = user.username,
            initialPassword = user.password,
            initialRole = user.role,
            isEditMode = true,
            onDismiss = { userToEdit = null },
            onSubmit = { username, password, role ->
                onUpdateUser(user.id, username, password, role)
                userToEdit = null
            }
        )
    }

    // Dialog: Delete Confirmation
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RedAccent, modifier = Modifier.size(40.dp)) },
            title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف حساب المستخدم (${user.username})؟ لن يتمكن من تسجيل الدخول إلى النظام مرة أخرى.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(user.id)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("حذف نهائي", color = Color.White)
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
fun UserCard(
    user: User,
    isCurrentUser: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAdmin = user.role == "admin"
    val cardColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val borderColor = if (isCurrentUser) {
        EmeraldPrimary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon based on Role
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isAdmin) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.Engineering,
                        contentDescription = null,
                        tint = if (isAdmin) EmeraldPrimary else MaterialTheme.colorScheme.secondary
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCurrentUser) {
                            Badge(containerColor = EmeraldPrimary, contentColor = Color.White) {
                                Text("أنت", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isAdmin) "مدير النظام" else "فني صيانة",
                            fontSize = 12.sp,
                            color = if (isAdmin) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isAdmin) FontWeight.Medium else FontWeight.Normal
                        )
                        Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "رقم التعريف: ${user.id}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل")
                }
                
                // Don't allow deleting current admin or main admin to avoid lockouts!
                if (!isCurrentUser && user.username != "admin") {
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = RedAccent)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormDialog(
    title: String,
    confirmLabel: String,
    initialUsername: String = "",
    initialPassword: String = "",
    initialRole: String = "technician",
    isEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var role by remember { mutableStateOf(initialRole) }
    var passwordVisible by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        usernameError = false
                    },
                    label = { Text("اسم المستخدم") },
                    placeholder = { Text("أدخل اسم فريد (مثل: ahmad)") },
                    isError = usernameError,
                    singleLine = true,
                    enabled = !isEditMode, // prevent username modification in edit mode for ease of use
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                if (usernameError) {
                    Text("يرجى إدخال اسم مستخدم صحيح (3 أحرف على الأقل)", color = RedAccent, fontSize = 11.sp)
                }

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = false
                    },
                    label = { Text("كلمة المرور") },
                    placeholder = { Text("أدخل كلمة المرور") },
                    isError = passwordError,
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                if (passwordError) {
                    Text("يرجى إدخال كلمة مرور صحيحة (4 أحرف على الأقل)", color = RedAccent, fontSize = 11.sp)
                }

                // Role Selector (Admin / Tech)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("نوع الصلاحية والدور:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Technician Card option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { role = "technician" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (role == "technician") EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.5.dp,
                                color = if (role == "technician") EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Engineering, contentDescription = null, tint = if (role == "technician") EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("فني صيانة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Admin Card option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { role = "admin" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (role == "admin") EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.5.dp,
                                color = if (role == "admin") EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = if (role == "admin") EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("مدير النظام", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isUsernameInvalid = username.trim().length < 3
                    val isPasswordInvalid = password.length < 4
                    if (isUsernameInvalid) usernameError = true
                    if (isPasswordInvalid) passwordError = true

                    if (!isUsernameInvalid && !isPasswordInvalid) {
                        onSubmit(username.trim(), password, role)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(confirmLabel, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
