package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.UserRole
import com.example.ui.components.TopUserHeaderBar
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
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.viewmodels.OutpassViewModel
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) } // Default to White & Green theme
            CollegeOutpassTheme(darkTheme = isDarkTheme) {
                MainApp(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

enum class ScreenState {
    LOGIN,
    REGISTER,
    MAIN_SHELL
}

@Composable
fun MainApp(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    viewModel: OutpassViewModel = viewModel()
) {
    val context = LocalContext.current
    var screenState by remember { mutableStateOf(ScreenState.LOGIN) } // Default to Login screen on app launch
    var currentBottomTab by remember { mutableStateOf(0) }

    val currentUser by viewModel.currentUser.collectAsState()
    val studentOutpasses by viewModel.studentOutpasses.collectAsState()
    val activeStudentPass by viewModel.activeStudentPass.collectAsState()
    val pendingApprovals by viewModel.pendingApprovals.collectAsState()
    val activeOutsideStudents by viewModel.activeOutsideStudents.collectAsState()
    val gateSearchResults by viewModel.gateSearchResults.collectAsState()
    val allOutpasses by viewModel.allOutpasses.collectAsState()
    val filteredAllOutpasses by viewModel.filteredAllOutpasses.collectAsState()
    val allGateLogs by viewModel.allGateLogs.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val currentAiReport by viewModel.currentAiReport.collectAsState()
    val isGeneratingAiReport by viewModel.isGeneratingReport.collectAsState()
    val aiReportError by viewModel.aiReportError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Automatic 4:10 PM HOD Daily Outpass Report Scheduling & Auto-Dispatch
    LaunchedEffect(Unit) {
        com.example.util.DailyHodReportScheduler.scheduleDaily410PmAlarm(context)
    }

    LaunchedEffect(allOutpasses, allUsers) {
        if (allOutpasses.isNotEmpty() && allUsers.isNotEmpty()) {
            com.example.util.DailyHodReportScheduler.checkAndAutoDispatchIfDue(context, allOutpasses, allUsers)
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (screenState == ScreenState.MAIN_SHELL && currentUser != null) {
                BottomNavBar(
                    role = currentUser!!.role,
                    selectedTab = currentBottomTab,
                    pendingCount = pendingApprovals.size,
                    activeOutsideCount = activeOutsideStudents.size,
                    onTabSelect = { currentBottomTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (screenState) {
                ScreenState.LOGIN -> {
                    LoginScreen(
                        onLoginSuccess = { screenState = ScreenState.MAIN_SHELL },
                        onNavigateRegister = { screenState = ScreenState.REGISTER },
                        onSelectDemoRole = { role ->
                            viewModel.loginDemoRole(role)
                            currentBottomTab = 0
                        },
                        onLoginEmail = { email, password -> viewModel.loginWithEmail(email, password) },
                        onResetPassword = { identifier, newPassword, newName ->
                            viewModel.resetPassword(identifier, newPassword, newName)
                        },
                        existingUsers = allUsers
                    )
                }

                ScreenState.REGISTER -> {
                    RegisterScreen(
                        existingUsers = allUsers,
                        onRegisterSuccess = { newUser ->
                            viewModel.registerNewMember(newUser)
                            screenState = ScreenState.MAIN_SHELL
                            currentBottomTab = 0
                        },
                        onBackToLogin = { screenState = ScreenState.LOGIN }
                    )
                }

                ScreenState.MAIN_SHELL -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Global Top Header with Active User Profile & Logout
                        TopUserHeaderBar(
                            currentUser = currentUser,
                            onLogoutClick = {
                                viewModel.logout()
                                screenState = ScreenState.LOGIN
                                currentBottomTab = 0
                            }
                        )

                        // Role-based Screen Router
                        val role = currentUser?.role ?: UserRole.STUDENT
                        when (role) {
                            UserRole.STUDENT -> {
                                when (currentBottomTab) {
                                    0 -> StudentDashboardScreen(
                                        currentUser = currentUser,
                                        activePass = activeStudentPass,
                                        recentPasses = studentOutpasses,
                                        onApplyClick = { currentBottomTab = 1 }
                                    )
                                    1 -> ApplyOutpassScreen(
                                        currentUser = currentUser,
                                        onSubmitOutpass = { type, dest, reason, outT, retT ->
                                            viewModel.applyOutpass(type, dest, reason, outT, retT)
                                        },
                                        onNavigateBack = { currentBottomTab = 0 }
                                    )
                                    2 -> PassHistoryScreen(
                                        outpasses = studentOutpasses,
                                        selectedFilter = statusFilter,
                                        onFilterSelect = { viewModel.setStatusFilter(it) }
                                    )
                                }
                            }

                            UserRole.STAFF_ADVISOR, UserRole.HOD -> {
                                when (currentBottomTab) {
                                    0 -> ApprovalsScreen(
                                        currentUser = currentUser,
                                        pendingPasses = pendingApprovals,
                                        allOutpasses = allOutpasses,
                                        onApprovePass = { passId, remarks ->
                                            viewModel.approveOutpass(passId, remarks)
                                        },
                                        onRejectPass = { passId, reason ->
                                            viewModel.rejectOutpass(passId, reason)
                                        },
                                        currentAiReport = currentAiReport,
                                        isGeneratingAiReport = isGeneratingAiReport,
                                        aiReportError = aiReportError,
                                        onGenerateAiReport = { preset, start, end ->
                                            viewModel.generateAiReport(preset, start, end)
                                        },
                                        onDismissAiReport = { viewModel.dismissAiReport() }
                                    )
                                    1 -> PassHistoryScreen(
                                        outpasses = filteredAllOutpasses,
                                        selectedFilter = statusFilter,
                                        onFilterSelect = { viewModel.setStatusFilter(it) },
                                        currentUser = currentUser,
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
                                        searchResults = gateSearchResults,
                                        gateLogs = allGateLogs,
                                        onCheckOut = { viewModel.gateCheckOut(it) },
                                        onCheckIn = { viewModel.gateCheckIn(it) }
                                    )
                                    1 -> ActiveOutsideScreen(
                                        activeOutsidePasses = activeOutsideStudents,
                                        onCheckIn = { viewModel.gateCheckIn(it) }
                                    )
                                    2 -> PassHistoryScreen(
                                        outpasses = filteredAllOutpasses,
                                        selectedFilter = statusFilter,
                                        onFilterSelect = { viewModel.setStatusFilter(it) },
                                        currentUser = currentUser
                                    )
                                }
                            }

                            UserRole.ADMIN -> {
                                AdminConsoleScreen(
                                    allOutpasses = allOutpasses,
                                    allGateLogs = allGateLogs,
                                    allUsers = allUsers
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
private fun BottomNavBar(
    role: UserRole,
    selectedTab: Int,
    pendingCount: Int,
    activeOutsideCount: Int,
    onTabSelect: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        when (role) {
            UserRole.STUDENT -> {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    icon = { Icon(Icons.Default.School, contentDescription = "Pass") },
                    label = { Text("My Pass") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    icon = { Icon(Icons.Default.AddCard, contentDescription = "Apply") },
                    label = { Text("Apply") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { onTabSelect(2) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    colors = navColors()
                )
            }

            UserRole.STAFF_ADVISOR, UserRole.HOD -> {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    icon = {
                        if (pendingCount > 0) {
                            BadgedBox(badge = { Badge { Text("$pendingCount") } }) {
                                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = "Approvals")
                            }
                        } else {
                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = "Approvals")
                        }
                    },
                    label = { Text("Approvals") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("All Logs") },
                    colors = navColors()
                )
            }

            UserRole.SECURITY_OFFICER -> {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Gate Scan") },
                    label = { Text("Gate Scan") },
                    colors = navColors()
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
                    label = { Text("Outside") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { onTabSelect(2) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Logs") },
                    label = { Text("Audit Logs") },
                    colors = navColors()
                )
            }

            UserRole.ADMIN -> {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Master Console") },
                    label = { Text("Console") },
                    colors = navColors()
                )
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer
)
