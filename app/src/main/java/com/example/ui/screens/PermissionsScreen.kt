package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.theme.EmeraldPrimary

data class ScreenPermissionItem(
    val id: String,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    users: List<User>,
    onSavePermissions: (userId: Long, allowedScreens: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableScreens = listOf(
        ScreenPermissionItem("dashboard", "شاشة لوحة التحكم", "عرض الإحصائيات والأجهزة المسجلة اليوم"),
        ScreenPermissionItem("technician_workspace", "ورشة الفني", "إدارة مراحل الصيانة وتركيب قطع الغيار"),
        ScreenPermissionItem("inventory_barcode", "المخزن والباركود", "استلام ورد القطع وتتبع المخزون بالباركود"),
        ScreenPermissionItem("inventory_reports", "تقارير قطع الغيار", "تقارير الاستهلاك اليومي والمصادر وتوثيق الموظف"),
        ScreenPermissionItem("technician_performance", "أداء الفنيين", "مؤشرات الإنجاز والدخل وزمن الإصلاح"),
        ScreenPermissionItem("new", "شاشة استلام جهاز جديد", "إمكانية استلام وتسجيل بيانات أجهزة الصيانة"),
        ScreenPermissionItem("delivery", "شاشة تسليم الأجهزة", "تسليم الأجهزة الجاهزة وتحصيل المبالغ"),
        ScreenPermissionItem("management", "شاشة إدارة الأجهزة", "البحث وتحديث حالات الصيانة"),
        ScreenPermissionItem("customer_history", "سجل وملفات العملاء", "استعراض أرشيف الأجهزة وسجل كل عميل"),
        ScreenPermissionItem("printer", "شاشة إعدادات الطابعة والباركود", "تعديل مقاسات وتنسيق إيصالات وملصقات الطابعة"),
        ScreenPermissionItem("settings", "شاشة الإعدادات العامة", "الوضع الليلي والتحكم في الخيارات")
    )

    var selectedUser by remember(users) { mutableStateOf(users.firstOrNull()) }
    var isUserDropdownExpanded by remember { mutableStateOf(false) }

    var selectedScreens by remember(selectedUser) {
        mutableStateOf(selectedUser?.parsePermissions()?.toSet() ?: emptySet())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("permissions_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "إدارة الصلاحيات (المدير)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تحديد الشاشات والأقسام المسموح للفنيين والموظفين الوصول إليها",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "اختر المستخدم لتعديل صلاحياته:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ExposedDropdownMenuBox(
                        expanded = isUserDropdownExpanded,
                        onExpandedChange = { isUserDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedUser?.let { "${it.username} (${if (it.role == "admin") "إدارة" else "فني"})" } ?: "اختر مستخدم...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUserDropdownExpanded) },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = EmeraldPrimary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = isUserDropdownExpanded,
                            onDismissRequest = { isUserDropdownExpanded = false }
                        ) {
                            users.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text("${user.username} (${if (user.role == "admin") "مدير" else "فني"})") },
                                    onClick = {
                                        selectedUser = user
                                        selectedScreens = user.parsePermissions().toSet()
                                        isUserDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedUser != null) {
                        Text(
                            text = "الشاشات المتاحة لهذا المستخدم:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableScreens.forEach { screenItem ->
                                val isChecked = selectedScreens.contains(screenItem.id)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedScreens = if (isChecked) {
                                                selectedScreens - screenItem.id
                                            } else {
                                                selectedScreens + screenItem.id
                                            }
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = screenItem.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = screenItem.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedScreens = if (checked) {
                                                    selectedScreens + screenItem.id
                                                } else {
                                                    selectedScreens - screenItem.id
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = EmeraldPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                selectedUser?.let { user ->
                                    onSavePermissions(user.id, selectedScreens.toList())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_permissions_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ الصلاحيات للمستخدم", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
