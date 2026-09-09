package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.ToastEvent
import com.example.ui.dialogs.CustomerDetailDialog
import com.example.ui.dialogs.DelayNotificationDialog
import com.example.ui.dialogs.DeviceDetailDialog
import com.example.ui.dialogs.DualPrintDialog
import com.example.ui.dialogs.StickerPrintDialog
import com.example.ui.dialogs.ThermalReceiptDialog
import com.example.ui.screens.CustomerHistoryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeliveryScreen
import com.example.ui.screens.DeviceManagementScreen
import com.example.ui.screens.InventoryBarcodeScreen
import com.example.ui.screens.InventoryReportsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NewDeviceScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.UserManagementScreen
import com.example.ui.screens.PrinterConfigScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ServerMonitorScreen
import com.example.ui.screens.SuperAdminDashboardScreen
import com.example.ui.screens.TechnicianPerformanceScreen
import com.example.ui.screens.TechnicianWorkspaceScreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.MottahedaTheme
import com.example.ui.theme.RedAccent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import kotlinx.coroutines.flow.collectLatest

data class NavItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                viewModel.toastEvents.collectLatest { event ->
                    when (event) {
                        is ToastEvent.Success -> snackbarHostState.showSnackbar(event.message)
                        is ToastEvent.Error -> snackbarHostState.showSnackbar(event.message)
                        is ToastEvent.Info -> snackbarHostState.showSnackbar(event.message)
                    }
                }
            }

            MottahedaTheme(darkTheme = uiState.isDarkMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    if (uiState.session == null) {
                        LoginScreen(
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.loginError,
                            onLogin = { u, p -> viewModel.login(u, p) },
                            onClearError = { viewModel.clearLoginError() }
                        )
                    } else if (viewModel.isSubscriptionLocked) {
                        SubscriptionLockedScreen(
                            shopConfig = uiState.session!!.shopConfig!!,
                            onLogout = { viewModel.logout() }
                        )
                    } else {
                        MainAppContent(
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.session ?: return
    val context = LocalContext.current
    var showNotifsDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Filter technician notifications directed to current user or all if admin
    val myNotifications = remember(uiState.technicianNotifications, session.username, session.role) {
        if (session.role == "admin") uiState.technicianNotifications
        else uiState.technicianNotifications.filter { it.technicianUsername.equals(session.username, ignoreCase = true) }
    }

    // Navigation Items
    val allNavItems = listOf(
        NavItem(AppScreen.DASHBOARD, "الرئيسية", Icons.Default.Dashboard),
        NavItem(AppScreen.SAAS_MANAGEMENT, "إدارة المنصة والمحلات", Icons.Default.AdminPanelSettings),
        NavItem(AppScreen.TECHNICIAN_WORKSPACE, "ورشة الفني", Icons.Default.Engineering),
        NavItem(AppScreen.INVENTORY_BARCODE, "المخزن والباركود", Icons.Default.QrCodeScanner),
        NavItem(AppScreen.INVENTORY_REPORTS, "تقارير القطع", Icons.Default.Assessment),
        NavItem(AppScreen.NEW_DEVICE, "استلام", Icons.Default.AddCircle),
        NavItem(AppScreen.DELIVERY, "تسليم", Icons.Default.CheckCircle),
        NavItem(AppScreen.TECHNICIAN_PERFORMANCE, "أداء الفنيين", Icons.Default.Insights),
        NavItem(AppScreen.MANAGEMENT, "الأجهزة", Icons.Default.Devices),
        NavItem(AppScreen.CUSTOMER_HISTORY, "العملاء", Icons.Default.Group),
        NavItem(AppScreen.PERMISSIONS, "الصلاحيات", Icons.Default.AdminPanelSettings),
        NavItem(AppScreen.USER_MANAGEMENT, "المستخدمين", Icons.Default.Group),
        NavItem(AppScreen.SERVER_MONITOR, "السيرفر والبيانات", Icons.Default.Cloud),
        NavItem(AppScreen.PRINTER, "الطباعة", Icons.Default.Print),
        NavItem(AppScreen.SETTINGS, "الإعدادات", Icons.Default.Settings)
    )

    val visibleNavItems = remember(session.allowedScreens, session.role) {
        allNavItems.filter { item ->
            if (session.role == "super_admin") {
                item.screen == AppScreen.SAAS_MANAGEMENT || item.screen == AppScreen.SETTINGS
            } else if (item.screen == AppScreen.SAAS_MANAGEMENT) {
                false
            } else if (item.screen == AppScreen.USER_MANAGEMENT || item.screen == AppScreen.SERVER_MONITOR) {
                session.role == "admin"
            } else if (session.role == "admin") {
                true
            } else {
                session.allowedScreens.contains(item.screen.id) ||
                        item.screen == AppScreen.SETTINGS ||
                        (session.role == "technician" && (item.screen == AppScreen.TECHNICIAN_WORKSPACE || item.screen == AppScreen.INVENTORY_BARCODE || item.screen == AppScreen.INVENTORY_REPORTS))
            }
        }
    }

    // Push Notification Dialog for Assigned Devices
    if (showNotifsDialog) {
        AlertDialog(
            onDismissRequest = { showNotifsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = EmeraldPrimary)
                    Text("إشعارات استلام أجهزة جديدة (${myNotifications.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (myNotifications.isEmpty()) {
                    Text("لا توجد إشعارات جديدة حالياً.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(myNotifications) { notif ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showNotifsDialog = false
                                        val targetDev = uiState.devices.find { it.id == notif.deviceId }
                                        if (targetDev != null) {
                                            viewModel.navigateTo(AppScreen.TECHNICIAN_WORKSPACE)
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "جهاز جديد: ${notif.deviceName} (${notif.deviceTicket})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                        Text(notif.timestamp, fontSize = 10.sp, color = Slate400)
                                    }
                                    Text(notif.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("العميل: ${notif.customerName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showNotifsDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(300.dp)
            ) {
                // Header inside drawer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EmeraldPrimary)
                        .padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.session?.shopConfig?.name ?: "المتحدة للصيانة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = (uiState.session?.shopConfig?.name ?: uiState.printerConfig.shopName.ifBlank { "UNITED WORKSHOP" }).uppercase(),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                // Drawer items list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(visibleNavItems) { navItem ->
                        val isSelected = uiState.currentScreen == navItem.screen
                        NavigationDrawerItem(
                            icon = { Icon(navItem.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            label = { Text(navItem.label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            selected = isSelected,
                            onClick = {
                                viewModel.navigateTo(navItem.screen)
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.12f),
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("btn_open_drawer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "فتح القائمة الجانبية",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(EmeraldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = uiState.session?.shopConfig?.name ?: "المتحدة للصيانة",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = (uiState.session?.shopConfig?.name ?: uiState.printerConfig.shopName.ifBlank { "UNITED WORKSHOP" }).uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimaryLight,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                },
                actions = {
                    // Technician Assignment Push Notification Icon with Badge
                    IconButton(
                        onClick = { showNotifsDialog = true },
                        modifier = Modifier.testTag("btn_technician_notifications")
                    ) {
                        BadgedBox(
                            badge = {
                                if (myNotifications.isNotEmpty()) {
                                    Badge(containerColor = RedAccent) {
                                        Text("${myNotifications.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "إشعارات الاستلام",
                                tint = if (myNotifications.isNotEmpty()) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // User Capsule Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (uiState.isDarkMode) Slate850 else Slate100)
                            .border(1.dp, if (uiState.isDarkMode) Slate800 else Slate200, RoundedCornerShape(50))
                            .padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
                    ) {
                        Text(
                            text = if (session.role == "super_admin") "مدير المنصة" else if (session.role == "admin") "المدير" else session.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (session.role == "super_admin") RedAccent else if (session.role == "admin") EmeraldDark else Slate700),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = session.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("toggle_dark_mode_top")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "تبديل المظهر",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (uiState.currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(
                    devices = uiState.devices,
                    isLoading = uiState.isLoading,
                    onRefresh = { viewModel.refreshData() },
                    onDeviceClick = { viewModel.openDeviceDetail(it) },
                    onNewDeviceClick = { viewModel.navigateTo(AppScreen.NEW_DEVICE) },
                    onSendReportClick = { viewModel.sendDailyReportToN8n() }
                )

                AppScreen.TECHNICIAN_WORKSPACE -> TechnicianWorkspaceScreen(
                    session = session,
                    devices = uiState.devices,
                    availableParts = uiState.inventoryParts,
                    notifications = uiState.technicianNotifications,
                    onUpdateDetailedStatus = { dev, detailedStatus, mainStatus, notes ->
                        viewModel.updateDetailedStatus(dev, detailedStatus, mainStatus, notes)
                    },
                    onAttachPartToDevice = { dev, barcode, name, qty, cost ->
                        viewModel.attachPartToDevice(dev, barcode, name, qty, cost)
                    },
                    onDeviceClick = { viewModel.openDeviceDetail(it) },
                    onDismissNotification = { viewModel.dismissNotification(it) }
                )

                AppScreen.INVENTORY_BARCODE -> InventoryBarcodeScreen(
                    session = session,
                    partsList = uiState.inventoryParts,
                    movementLogs = uiState.movementLogs,
                    onReceivePart = { barcode, name, cat, qty, cost, src, emp ->
                        viewModel.receiveInventoryPart(barcode, name, cat, qty, cost, src, emp)
                    },
                    onReturnSurplusToWarehouse = { barcode, qty, emp, notes ->
                        viewModel.returnSurplusToWarehouse(barcode, qty, emp, notes)
                    },
                    onLogDefectiveOrReturnSupplier = { barcode, qty, type, reason, dest, emp ->
                        viewModel.logDefectiveOrSupplierReturn(barcode, qty, type, reason, dest, emp)
                    },
                    onImportParts = { imported, replaceAll ->
                        viewModel.importInventoryParts(imported, replaceAll)
                    },
                    onOpenReports = { viewModel.navigateTo(AppScreen.INVENTORY_REPORTS) }
                )

                AppScreen.INVENTORY_REPORTS -> InventoryReportsScreen(
                    session = session,
                    partsList = uiState.inventoryParts,
                    movementLogs = uiState.movementLogs,
                    devices = uiState.devices,
                    onDeviceClick = { viewModel.openDeviceDetail(it) },
                    onRefresh = { viewModel.refreshData() }
                )

                AppScreen.TECHNICIAN_PERFORMANCE -> TechnicianPerformanceScreen(
                    devices = uiState.devices,
                    users = uiState.users,
                    partLogs = uiState.movementLogs,
                    onDeviceClick = { viewModel.openDeviceDetail(it) }
                )

                AppScreen.NEW_DEVICE -> NewDeviceScreen(
                    isLoading = uiState.isLoading,
                    currentEmployeeName = session.displayName,
                    customerProfiles = uiState.customerProfiles,
                    users = uiState.users,
                    geminiAudioService = viewModel.geminiAudioService,
                    onViewCustomerHistory = { viewModel.openCustomerProfile(it) },
                    onSaveDevice = { cName, cPhone, dName, issue, cost, downPayment, tech, employee, photoUrl, dueDate ->
                        viewModel.createDevice(
                            customerName = cName,
                            customerPhone = cPhone,
                            deviceName = dName,
                            issueDescription = issue,
                            estimatedCost = cost,
                            downPayment = downPayment,
                            technician = tech,
                            receivedByEmployee = employee,
                            photoUrl = photoUrl,
                            dueDate = dueDate
                        )
                    }
                )

                AppScreen.DELIVERY -> DeliveryScreen(
                    devices = uiState.devices,
                    currentEmployeeName = session.displayName,
                    onDeliverDevice = { device, employee ->
                        viewModel.deliverDeviceWithEmployee(device, employee)
                    },
                    onPrintReceipt = { viewModel.openReceiptPrint(it) },
                    onDeviceClick = { viewModel.openDeviceDetail(it) }
                )

                AppScreen.MANAGEMENT -> DeviceManagementScreen(
                    devices = uiState.devices,
                    currentUsername = session.username,
                    userRole = session.role,
                    onDeviceClick = { viewModel.openDeviceDetail(it) }
                )

                AppScreen.CUSTOMER_HISTORY -> CustomerHistoryScreen(
                    customerProfiles = uiState.customerProfiles,
                    onSelectCustomer = { viewModel.openCustomerProfile(it) }
                )

                AppScreen.PERMISSIONS -> PermissionsScreen(
                    users = uiState.users,
                    onSavePermissions = { uid, perms ->
                        viewModel.savePermissionsForUser(uid, perms)
                    }
                )

                AppScreen.USER_MANAGEMENT -> UserManagementScreen(
                    users = uiState.users,
                    currentUsername = session.username,
                    shopName = session.shopConfig.name,
                    onCreateUser = { u, p, r -> viewModel.createUser(u, p, r) },
                    onUpdateUser = { uid, u, p, r -> viewModel.updateUser(uid, u, p, r) },
                    onDeleteUser = { uid -> viewModel.deleteUser(uid) },
                    onResetUserDeviceId = { uid -> viewModel.resetUserDeviceId(uid) }
                )

                AppScreen.PRINTER -> PrinterConfigScreen(
                    currentConfig = uiState.printerConfig,
                    onSaveConfig = { viewModel.savePrinterConfig(it) }
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    isDarkMode = uiState.isDarkMode,
                    sessionUser = session,
                    smsConfig = uiState.smsConfig,
                    n8nConfig = uiState.n8nConfig,
                    devices = uiState.devices,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onSaveSmsConfig = { viewModel.saveSmsConfig(it) },
                    onSaveN8nConfig = { viewModel.saveN8nConfig(it) },
                    onTestSms = { viewModel.testSendSms(it) },
                    onTestN8nWebhook = { viewModel.testN8nWebhook() },
                    onExportJson = { viewModel.exportDatabaseToJson() },
                    onRestoreBackup = { importedDevices, replaceAll ->
                        viewModel.restoreBackup(importedDevices, replaceAll)
                    },
                    onLogout = { viewModel.logout() }
                )

                AppScreen.SERVER_MONITOR -> ServerMonitorScreen(
                    session = session,
                    devices = uiState.devices,
                    onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
                )

                AppScreen.SAAS_MANAGEMENT -> SuperAdminDashboardScreen(
                    shops = uiState.shops,
                    users = uiState.users,
                    onLoadShops = { viewModel.loadShops() },
                    onAddShop = { id, name, addr, phone, footer, status, expires, maxUsers, adminUser, adminPass ->
                        viewModel.addNewShop(id, name, addr, phone, footer, status, expires, maxUsers, adminUser, adminPass)
                    },
                    onUpdateSubscription = { id, isActive, expires, limit ->
                        viewModel.updateShopSubscription(id, isActive, expires, limit)
                    },
                    onDeleteShop = { shopId ->
                        viewModel.deleteShop(shopId)
                    },
                    onAddUserToShop = { username, password, role, shopId ->
                        viewModel.createUserForShop(username, password, role, shopId)
                    },
                    onResetUserDeviceId = { userId ->
                        viewModel.resetUserDeviceId(userId)
                    },
                    onDeleteUser = { userId ->
                        viewModel.deleteUser(userId)
                    },
                    onUpdateUser = { userId, username, password, role ->
                        viewModel.updateUser(userId, username, password, role)
                    },
                    currentServerUrl = viewModel.getServerUrl(),
                    currentAnonKey = viewModel.getAnonKey(),
                    currentBasicUser = viewModel.getBasicUser(),
                    currentBasicPass = viewModel.getBasicPass(),
                    onSaveServerConfig = { url, key, u, p ->
                        viewModel.updateServerConfig(url, key, u, p)
                    },
                    onTestConnection = { url, key, u, p ->
                        viewModel.testServerConnection(url, key, u, p)
                    }
                )
            }
        }

        // Modals & Dialogs
        uiState.selectedDeviceForDetail?.let { device ->
            DeviceDetailDialog(
                device = device,
                onDismiss = { viewModel.closeDeviceDetail() },
                onStatusChange = { newStatus ->
                    viewModel.updateStatus(device, newStatus)
                },
                onPrintReceipt = {
                    viewModel.openReceiptPrint(device)
                },
                onPrintSticker = {
                    viewModel.openStickerPrint(device)
                },
                onSendWhatsApp = {
                    viewModel.shareWhatsApp(context, device)
                },
                onSendReadySms = {
                    viewModel.sendReadySms(device)
                },
                onOpenDelayDialog = {
                    viewModel.openDelayDialog(device)
                },
                onViewCustomerHistory = {
                    viewModel.openCustomerProfileByDevice(device)
                }
            )
        }

        uiState.dualPrintDevice?.let { device ->
            DualPrintDialog(
                device = device,
                config = uiState.printerConfig,
                onDismiss = { viewModel.closeDualPrint() }
            )
        }

        uiState.deviceForDelayNotification?.let { device ->
            DelayNotificationDialog(
                device = device,
                onDismiss = { viewModel.closeDelayDialog() },
                onSendDelaySms = { reason ->
                    viewModel.sendDelaySms(device, reason)
                }
            )
        }

        uiState.selectedCustomerProfile?.let { profile ->
            CustomerDetailDialog(
                profile = profile,
                onDismiss = { viewModel.closeCustomerProfile() },
                onSelectDevice = { dev ->
                    viewModel.openDeviceDetail(dev)
                }
            )
        }

        uiState.receiptToPrint?.let { device ->
            ThermalReceiptDialog(
                device = device,
                config = uiState.printerConfig,
                onDismiss = { viewModel.closeReceiptPrint() }
            )
        }

        uiState.stickerToPrint?.let { device ->
            StickerPrintDialog(
                device = device,
                config = uiState.printerConfig,
                onDismiss = { viewModel.closeStickerPrint() }
            )
        }
    }
}
}

@Composable
fun SubscriptionLockedScreen(
    shopConfig: com.example.data.model.ShopConfig,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(RedAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = RedAccent,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "عذراً، الحساب متوقف حالياً",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "المحل: ${shopConfig.name}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldPrimary
                )

                val expiryText = shopConfig.subscriptionExpiresAt ?: "غير محدد"
                val reasonText = if (!shopConfig.isSubscriptionActive) {
                    "انتهت فترة الاشتراك الخاص بمؤسستك في: $expiryText"
                } else {
                    "تم تجاوز الحد الأقصى المسموح به للمستخدمين في باقتك الحالية (${shopConfig.userLimit} مستخدم)"
                }

                Text(
                    text = reasonText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { /* Contact admin */ },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("التواصل مع الدعم الفني للتجديد", fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("تسجيل الخروج والعودة", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

