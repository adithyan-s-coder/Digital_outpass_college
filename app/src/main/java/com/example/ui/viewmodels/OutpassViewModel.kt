package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.CompleteAiReport
import com.example.data.models.GateLog
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.data.models.OutpassType
import com.example.data.models.ReportDatePreset
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.data.repository.OutpassRepository
import com.example.util.GeminiReportService
import com.example.util.OutpassStatisticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OutpassViewModel(
    private val repository: OutpassRepository = OutpassRepository.getInstance()
) : ViewModel() {

    val currentUser: StateFlow<User?> = repository.currentUser
    val allOutpasses: StateFlow<List<Outpass>> = repository.outpasses
    val allGateLogs: StateFlow<List<GateLog>> = repository.gateLogs
    val allUsers: StateFlow<List<User>> = repository.users

    // Search query for Gate Verification / Admin
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // History filter for student / staff
    private val _statusFilter = MutableStateFlow<OutpassStatus?>(null)
    val statusFilter: StateFlow<OutpassStatus?> = _statusFilter.asStateFlow()

    // Snack / Notification messages
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // AI Outpass Report Generation State
    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    private val _currentAiReport = MutableStateFlow<CompleteAiReport?>(null)
    val currentAiReport: StateFlow<CompleteAiReport?> = _currentAiReport.asStateFlow()

    private val _aiReportError = MutableStateFlow<String?>(null)
    val aiReportError: StateFlow<String?> = _aiReportError.asStateFlow()

    // Student specific outpasses
    val studentOutpasses: StateFlow<List<Outpass>> = combine(
        allOutpasses,
        currentUser,
        statusFilter
    ) { passes, user, filter ->
        if (user == null) emptyList()
        else {
            val list = passes.filter { it.studentId == user.id || it.regNo.equals(user.regNo, ignoreCase = true) }
            if (filter != null) list.filter { it.status == filter } else list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Pass for Student (APPROVED or CHECKED_OUT or PENDING)
    val activeStudentPass: StateFlow<Outpass?> = studentOutpasses.map { passes ->
        passes.firstOrNull { it.status == OutpassStatus.APPROVED || it.status == OutpassStatus.CHECKED_OUT }
            ?: passes.firstOrNull { it.status == OutpassStatus.PENDING_STAFF || it.status == OutpassStatus.PENDING_HOD }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Pending Approvals for Staff / HOD
    val pendingApprovals: StateFlow<List<Outpass>> = combine(
        allOutpasses,
        currentUser
    ) { passes, user ->
        if (user == null) emptyList()
        else when (user.role) {
            UserRole.STAFF_ADVISOR -> passes.filter { it.status == OutpassStatus.PENDING_STAFF && it.department.equals(user.department, ignoreCase = true) }
            UserRole.HOD -> passes.filter { it.status == OutpassStatus.PENDING_HOD && it.department.equals(user.department, ignoreCase = true) }
            UserRole.ADMIN -> passes.filter { it.status == OutpassStatus.PENDING_STAFF || it.status == OutpassStatus.PENDING_HOD }
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Students currently outside campus
    val activeOutsideStudents: StateFlow<List<Outpass>> = allOutpasses.map { passes ->
        passes.filter { it.status == OutpassStatus.CHECKED_OUT }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Gate Verification Search Results
    val gateSearchResults: StateFlow<List<Outpass>> = combine(
        allOutpasses,
        searchQuery
    ) { passes, query ->
        if (query.isBlank()) passes.take(10)
        else passes.filter {
            it.id.contains(query, ignoreCase = true) ||
                    it.regNo.contains(query, ignoreCase = true) ||
                    it.studentName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All outpasses filtered by active status filter (for Security Officer and Staff/HOD audit screens)
    val filteredAllOutpasses: StateFlow<List<Outpass>> = combine(
        allOutpasses,
        statusFilter
    ) { passes, filter ->
        if (filter == null) passes
        else passes.filter { it.status == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: OutpassStatus?) {
        _statusFilter.value = status
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun loginDemoRole(role: UserRole) {
        repository.loginDemoRole(role)
        _userMessage.value = "Switched active user role to ${role.displayName}"
    }

    fun logout() {
        repository.logout()
        _userMessage.value = "Logged out successfully."
    }

    fun loginWithEmail(email: String): Boolean {
        val success = repository.login(email)
        if (success) {
            _userMessage.value = "Welcome back, ${repository.currentUser.value?.name}!"
        } else {
            _userMessage.value = "User with email $email not found. Please register."
        }
        return success
    }

    fun registerNewMember(user: User) {
        repository.registerUser(user)
        _userMessage.value = "Registration successful! Logged in as ${user.name} (${user.role.displayName})"
    }

    fun applyOutpass(
        type: OutpassType,
        destination: String,
        reason: String,
        outDateTime: Long,
        returnDateTime: Long
    ) {
        val user = currentUser.value
        if (user == null || user.role != UserRole.STUDENT) {
            _userMessage.value = "Only logged-in students can apply for an outpass."
            return
        }
        val pass = repository.applyOutpass(user, type, destination, reason, outDateTime, returnDateTime)
        _userMessage.value = "Outpass request ${pass.id} submitted for Staff Advisor approval!"
    }

    fun approveOutpass(passId: String, remarks: String) {
        val user = currentUser.value ?: return
        repository.approveOutpass(passId, user, remarks)
        _userMessage.value = "Outpass $passId approved successfully!"
    }

    fun rejectOutpass(passId: String, reason: String) {
        val user = currentUser.value ?: return
        repository.rejectOutpass(passId, user, reason)
        _userMessage.value = "Outpass $passId rejected."
    }

    fun gateCheckOut(passId: String) {
        val officerName = currentUser.value?.name ?: "Security Officer"
        val result = repository.checkOutGate(passId, officerName)
        _userMessage.value = result
    }

    fun gateCheckIn(passId: String) {
        val officerName = currentUser.value?.name ?: "Security Officer"
        val result = repository.checkInGate(passId, officerName)
        _userMessage.value = result
    }

    fun generateAiReport(
        preset: ReportDatePreset,
        customStartMs: Long? = null,
        customEndMs: Long? = null
    ) {
        val user = currentUser.value ?: return
        if (user.role != UserRole.STAFF_ADVISOR && user.role != UserRole.HOD && user.role != UserRole.ADMIN) {
            _userMessage.value = "Unauthorized: Only Staff Advisors and HODs can generate AI outpass reports."
            return
        }

        viewModelScope.launch {
            _isGeneratingReport.value = true
            _aiReportError.value = null
            try {
                // Step 1: Calculate real statistics from actual database records
                val stats = OutpassStatisticsCalculator.calculate(
                    allRecords = allOutpasses.value,
                    preset = preset,
                    user = user,
                    customStartMs = customStartMs,
                    customEndMs = customEndMs
                )

                // Step 2: Request Gemini AI analysis of structured statistical summary
                val analysis = GeminiReportService.analyzeStatistics(stats)

                // Step 3: Package complete report
                val report = CompleteAiReport(
                    id = "RPT-" + (System.currentTimeMillis() % 100000),
                    generatedAt = System.currentTimeMillis(),
                    generatedBy = user,
                    statistics = stats,
                    analysis = analysis
                )

                _currentAiReport.value = report
                _userMessage.value = "AI Outpass Report generated successfully for ${stats.periodLabel}."
            } catch (e: Exception) {
                _aiReportError.value = "Failed to generate report: ${e.message}"
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    fun dismissAiReport() {
        _currentAiReport.value = null
        _aiReportError.value = null
    }
}
