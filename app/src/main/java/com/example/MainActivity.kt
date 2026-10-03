package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.OutpassStatus
import com.example.data.models.UserRole
import com.example.data.repository.OutpassRepository
import com.example.ui.components.UserAvatar
import com.example.ui.screens.ActiveOutsideScreen
import com.example.ui.screens.AdminConsoleScreen
import com.example.ui.screens.ApplyOutpassScreen
import com.example.ui.screens.ApprovalsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PassHistoryScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SecurityGateScreen
import com.example.ui.screens.StudentDashboardScreen
import com.example.ui.theme.CollegeOutpassTheme
import com.example.ui.viewmodels.OutpassViewModel
import com.example.util.OutpassNotificationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            OutpassRepository.getInstance(applicationContext)
            OutpassNotificationHelper.initializeChannels(applicationContext)
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Init notice: ${t.message}", t)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Notification perm notice: ${t.message}", t)
        }

        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "EdgeToEdge notice: ${t.message}", t)
        }

        setContent {
            CollegeOutpassTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: OutpassViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var screenState by remember { mutableStateOf(ScreenState.LOGIN) }
    var currentBottomTab by remember { mutableIntStateOf(0) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val studentOutpasses by viewModel.studentOutpasses.collectAsStateWithLifecycle()
    val activeStudentPass by viewModel.activeStudentPass.collectAsStateWithLifecycle()
    val pendingApprovals by viewModel.pendingApprovals.collectAsStateWithLifecycle()
    val activeOutsideStudents by viewModel.activeOutsideStudents.collectAsStateWithLifecycle()
    val gateSearchResults by viewModel.gateSearchResults.collectAsStateWithLifecycle()
    val allOutpasses by viewModel.allOutpasses.collectAsStateWithLifecycle()
    val filteredAllOutpasses by viewModel.filteredAllOutpasses.collectAsStateWithLifecycle()
    val allGateLogs by viewModel.allGateLogs.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val currentAiReport by viewModel.currentAiReport.collectAsStateWithLifecycle()
    val isGeneratingAiReport by viewModel.isGeneratingReport.collectAsStateWithLifecycle()
    val aiReportError by viewModel.aiReportError.collectAsStateWithLifecycle()

    // Handle toast messages from ViewModel
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            if (msg.isNotBlank()) {
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
                viewModel.clearUserMessage()
            }
        }
    }

    // Auto-navigate to MAIN_SHELL if logged in
    LaunchedEffect(currentUser) {
        if (currentUser != null && screenState == ScreenState.LOGIN) {
            screenState = ScreenState.MAIN_SHELL
            currentBottomTab = 0
        } else if (currentUser == null && screenState == ScreenState.MAIN_SHELL) {
            screenState = ScreenState.LOGIN
            currentBottomTab = 0
        }
    }

    when (screenState) {
        ScreenState.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    screenState = ScreenState.MAIN_SHELL
                    currentBottomTab = 0
                },
                onNavigateRegister = {
                    screenState = ScreenState.REGISTER
                },
                onSelectDemoRole = { role ->
                    viewModel.loginDemoRole(role)
                    screenState = ScreenState.MAIN_SHELL
                    currentBottomTab = 0
                },
                onLoginEmail = { email, password, role ->
                    viewModel.loginWithEmail(email, password, role)
                },
                onResetPassword = { id, newPassword, newName ->
                    viewModel.resetPassword(id, newPassword, newName)
                },
                existingUsers = allUsers
            )
        }

        ScreenState.REGISTER -> {
            BackHandler {
                screenState = ScreenState.LOGIN
            }
            RegisterScreen(
                existingUsers = allUsers,
                onRegisterSuccess = { newUser ->
                    viewModel.registerNewMember(newUser)
                    screenState = ScreenState.MAIN_SHELL
                    currentBottomTab = 0
                },
                onBackToLogin = {
                    screenState = ScreenState.LOGIN
                }
            )
        }

        ScreenState.MAIN_SHELL -> {
            val user = currentUser
            val role = user?.role ?: UserRole.STUDENT

            BackHandler(enabled = currentBottomTab != 0) {
                currentBottomTab = 0
            }

            Scaffold(
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = androidx.compose.ui.graphics.Color(0xFF1E293B),
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            actionColor = com.example.ui.theme.GreenPrimary
                        )
                    }
                },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                user?.let {
                                    UserAvatar(
                                        photoUri = it.photoUri,
                                        name = it.name,
                                        role = it.role,
                                        size = 36.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user?.name ?: "Digital Outpass",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${role.displayName} • ${user?.department ?: "VET IAS"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                viewModel.logout()
                                screenState = ScreenState.LOGIN
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Log Out",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    BottomNavBar(
                        role = role,
                        selectedTab = currentBottomTab,
                        pendingCount = pendingApprovals.size,
                        activeOutsideCount = activeOutsideStudents.size,
                        onTabSelect = { currentBottomTab = it }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (role) {
                        UserRole.STUDENT -> {
                            when (currentBottomTab) {
                                0 -> StudentDashboardScreen(
                                    currentUser = user,
                                    activePass = activeStudentPass,
                                    recentPasses = studentOutpasses,
                                    onApplyClick = { currentBottomTab = 1 }
                                )
                                1 -> ApplyOutpassScreen(
                                    currentUser = user,
                                    onSubmitOutpass = { type, dest, reason, outTime, returnTime ->
                                        viewModel.applyOutpass(type, dest, reason, outTime, returnTime)
                                        currentBottomTab = 0
                                    },
                                    onNavigateBack = { currentBottomTab = 0 }
                                )
                                2 -> PassHistoryScreen(
                                    outpasses = studentOutpasses,
                                    selectedFilter = statusFilter,
                                    onFilterSelect = { viewModel.setStatusFilter(it) },
                                    currentUser = user
                                )
                            }
                        }

                        UserRole.STAFF_ADVISOR, UserRole.HOD -> {
                            when (currentBottomTab) {
                                0 -> ApprovalsScreen(
                                    currentUser = user,
                                    pendingPasses = pendingApprovals,
                                    allOutpasses = allOutpasses,
                                    onApprovePass = { id, remarks -> viewModel.approveOutpass(id, remarks) },
                                    onRejectPass = { id, reason -> viewModel.rejectOutpass(id, reason) },
                                    currentAiReport = currentAiReport,
                                    isGeneratingAiReport = isGeneratingAiReport,
                                    aiReportError = aiReportError,
                                    onGenerateAiReport = { preset, start, end ->
                                        viewModel.generateAiReport(preset, start, end)
                                    },
                                    onDismissAiReport = { viewModel.dismissAiReport() }
                                )
                                1 -> ActiveOutsideScreen(
                                    activeOutsidePasses = activeOutsideStudents,
                                    onCheckIn = { passId -> viewModel.gateCheckIn(passId) }
                                )
                                2 -> PassHistoryScreen(
                                    outpasses = filteredAllOutpasses,
                                    selectedFilter = statusFilter,
                                    onFilterSelect = { viewModel.setStatusFilter(it) },
                                    currentUser = user,
                                    currentAiReport = currentAiReport,
                                    isGeneratingAiReport = isGeneratingAiReport,
                                    aiReportError = aiReportError,
                                    onGenerateAiReport = { preset, start, end ->
                                        viewModel.generateAiReport(preset, start, end)
                                    },
                                    onDismissAiReport = { viewModel.dismissAiReport() }
                                )
                            }
                        }

                        UserRole.SECURITY_OFFICER -> {
                            when (currentBottomTab) {
                                0 -> SecurityGateScreen(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onScanPass = { qr -> viewModel.handleScannedPass(qr) },
                                    searchResults = gateSearchResults,
                                    gateLogs = allGateLogs,
                                    onCheckOut = { passId -> viewModel.gateCheckOut(passId) },
                                    onCheckIn = { passId -> viewModel.gateCheckIn(passId) }
                                )
                                1 -> ActiveOutsideScreen(
                                    activeOutsidePasses = activeOutsideStudents,
                                    onCheckIn = { passId -> viewModel.gateCheckIn(passId) }
                                )
                                2 -> PassHistoryScreen(
                                    outpasses = allOutpasses,
                                    selectedFilter = statusFilter,
                                    onFilterSelect = { viewModel.setStatusFilter(it) },
                                    currentUser = user
                                )
                            }
                        }

                        UserRole.ADMIN -> {
                            when (currentBottomTab) {
                                0 -> AdminConsoleScreen(
                                    allOutpasses = allOutpasses,
                                    allGateLogs = allGateLogs,
                                    allUsers = allUsers,
                                    onUpdateUser = { updated -> viewModel.updateUser(updated) },
                                    onDeleteUser = { id -> viewModel.deleteUser(id) }
                                )
                                1 -> PassHistoryScreen(
                                    outpasses = allOutpasses,
                                    selectedFilter = statusFilter,
                                    onFilterSelect = { viewModel.setStatusFilter(it) },
                                    currentUser = user
                                )
                                2 -> SecurityGateScreen(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onScanPass = { qr -> viewModel.handleScannedPass(qr) },
                                    searchResults = gateSearchResults,
                                    gateLogs = allGateLogs,
                                    onCheckOut = { passId -> viewModel.gateCheckOut(passId) },
                                    onCheckIn = { passId -> viewModel.gateCheckIn(passId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    role: UserRole,
    selectedTab: Int,
    pendingCount: Int,
    activeOutsideCount: Int,
    onTabSelect: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        when (role) {
            UserRole.STUDENT -> {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    icon = { Icon(Icons.Default.AddCard, contentDescription = "Apply Pass") },
                    label = { Text("Apply") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { onTabSelect(2) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") }
                )
            }

            UserRole.STAFF_ADVISOR, UserRole.HOD -> {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    icon = {
                        if (pendingCount > 0) {
                            BadgedBox(badge = { Badge { Text("$pendingCount") } }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Approvals")
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Approvals")
                        }
                    },
                    label = { Text("Approvals") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    icon = {
                        if (activeOutsideCount > 0) {
                            BadgedBox(badge = { Badge { Text("$activeOutsideCount") } }) {
                                Icon(Icons.Default.DirectionsWalk, contentDescription = "Outside")
                            }
                        } else {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = "Outside")
                        }
                    },
                    label = { Text("Outside") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { onTabSelect(2) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text("Reports") }
                )
            }

            UserRole.SECURITY_OFFICER -> {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Gate Scan") },
                    label = { Text("Gate Scan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    icon = {
                        if (activeOutsideCount > 0) {
                            BadgedBox(badge = { Badge { Text("$activeOutsideCount") } }) {
                                Icon(Icons.Default.DirectionsWalk, contentDescription = "Outside")
                            }
                        } else {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = "Outside")
                        }
                    },
                    label = { Text("Outside") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { onTabSelect(2) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Logs") },
                    label = { Text("Gate Logs") }
                )
            }

            UserRole.ADMIN -> {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    icon = { Icon(Icons.Default.ManageAccounts, contentDescription = "Admin") },
                    label = { Text("Console") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Passes") },
                    label = { Text("All Passes") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { onTabSelect(2) },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Security") },
                    label = { Text("Gate Security") }
                )
            }
        }
    }
}
