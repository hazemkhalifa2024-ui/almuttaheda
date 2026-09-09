package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String? = null,
    onLogin: (username: String, password: String) -> Unit,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loginPrefs = remember { context.getSharedPreferences("mottaheda_login_remember", Context.MODE_PRIVATE) }

    var username by remember { 
        mutableStateOf(loginPrefs.getString("saved_user", "") ?: "") 
    }
    var password by remember { 
        mutableStateOf(loginPrefs.getString("saved_pass", "") ?: "") 
    }
    var rememberMe by remember { 
        mutableStateOf(loginPrefs.getBoolean("remember_me", false)) 
    }
    var showPassword by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var localValidationMsg by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Slate950,
            Slate900,
            Color(0xFF062A1F)
        )
    )

    fun handleLoginAttempt() {
        focusManager.clearFocus()
        val u = username.trim()
        val p = password.trim()
        if (u.isBlank()) {
            localValidationMsg = "يرجى إدخال اسم المستخدم أولاً"
            return
        }
        if (p.isBlank()) {
            localValidationMsg = "يرجى إدخال كلمة السر"
            return
        }
        localValidationMsg = null
        onClearError()

        if (rememberMe) {
            loginPrefs.edit()
                .putString("saved_user", u)
                .putString("saved_pass", p)
                .putBoolean("remember_me", true)
                .apply()
        } else {
            loginPrefs.edit()
                .remove("saved_user")
                .remove("saved_pass")
                .putBoolean("remember_me", false)
                .apply()
        }
        onLogin(u, p)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Badge
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = EmeraldPrimaryLight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(EmeraldPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "ورشة الصيانة",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "المتحدة للصيانة",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "نظام إدارة الورشة والمنصة السحابية",
                fontSize = 13.sp,
                color = Slate400,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Glassmorphism Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Username Field
                    Column {
                        Text(
                            text = "اسم المستخدم",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { 
                                username = it 
                                localValidationMsg = null
                                if (!errorMessage.isNullOrBlank()) onClearError()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input"),
                            placeholder = { Text("أدخل اسم المستخدم", color = Slate400, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Slate400)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0x660F172A),
                                unfocusedContainerColor = Color(0x400F172A),
                                focusedBorderColor = EmeraldPrimaryLight,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // Password Field
                    Column {
                        Text(
                            text = "كلمة السر",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it 
                                localValidationMsg = null
                                if (!errorMessage.isNullOrBlank()) onClearError()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            placeholder = { Text("أدخل كلمة السر", color = Slate400, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Slate400)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "تبديل الرؤية",
                                        tint = Slate400
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { handleLoginAttempt() }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0x660F172A),
                                unfocusedContainerColor = Color(0x400F172A),
                                focusedBorderColor = EmeraldPrimaryLight,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // Remember Me & Forgot Password Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { rememberMe = !rememberMe }
                                .padding(end = 6.dp)
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = EmeraldPrimary,
                                    uncheckedColor = Slate400,
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تذكر كلمة المرور",
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "نسيت كلمة المرور؟",
                            color = EmeraldPrimaryLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showForgotPasswordDialog = true }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        )
                    }

                    // Error Message Card
                    val displayedError = errorMessage ?: localValidationMsg
                    if (!displayedError.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33EF4444))
                                .border(1.dp, Color(0x80EF4444), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "خطأ",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = displayedError,
                                    color = Color(0xFFFEE2E2),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        localValidationMsg = null
                                        onClearError()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Login Button
                    Button(
                        onClick = { handleLoginAttempt() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = EmeraldPrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "تسجيل الدخول",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "استعادة ونسيان كلمة المرور",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "للحفاظ على أمان بيانات الورشة والحسابات:",
                        fontSize = 13.sp,
                        color = Slate300
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x330F172A))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "🔒 استعادة الحسابات وكلمات المرور:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimaryLight
                            )
                            Text(
                                text = "يرجى التواصل مع الإدارة أو المشرف العام للنظام لتعديل وإعادة تعيين كلمة السر الخاصة بحسابك من خلال شاشة «إدارة المستخدمين والصلاحيات».",
                                fontSize = 12.sp,
                                color = Slate200
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showForgotPasswordDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حسناً، فهمت", color = Color.White)
                }
            },
            containerColor = Slate900,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Explicit Popup Alert Dialog for any login failure
    if (!errorMessage.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { onClearError() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x33EF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "تعذر تسجيل الدخول",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = errorMessage,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFFFEE2E2),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "يرجى مراجعة اسم المستخدم وكلمة المرور، أو التأكد من إدخال بادئة المحل بشكل صحيح.",
                        fontSize = 11.sp,
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onClearError() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حسناً، فهمت", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = Slate900,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
