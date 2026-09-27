package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.LocalBackupStorage
import com.example.data.local.entities.GateLogEntity
import com.example.data.local.entities.OutpassEntity
import com.example.data.local.entities.UserEntity
import com.example.data.models.ApprovalRecord
import com.example.data.models.GateLog
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.data.models.OutpassType
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.data.models.DepartmentConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class OutpassRepository {

    companion object {
        @Volatile
        private var instance: OutpassRepository? = null

        fun getInstance(context: Context? = null): OutpassRepository {
            return instance ?: synchronized(this) {
                instance ?: OutpassRepository().also { repo ->
                    instance = repo
                    if (context != null) {
                        repo.initialize(context)
                    }
                }
            }
        }

        fun initialize(context: Context): OutpassRepository {
            val repo = getInstance(context)
            repo.initialize(context)
            return repo
        }
    }

    private var appContext: Context? = null
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize(context: Context) {
        val appCtx = context.applicationContext
        this.appContext = appCtx

        // 1. Synchronously load users from LocalBackupStorage so there is zero delay on startup
        val savedUsers = LocalBackupStorage.loadUsers(appCtx)
        if (!savedUsers.isNullOrEmpty()) {
            val merged = savedUsers.toMutableList()
            for (demo in demoUsers) {
                if (merged.none { it.id == demo.id || it.email.equals(demo.email, ignoreCase = true) }) {
                    merged.add(demo)
                }
            }
            _users.value = merged
        } else {
            _users.value = demoUsers
            LocalBackupStorage.saveUsers(appCtx, demoUsers)
        }

        // 2. Synchronously load outpasses from storage
        val savedOutpasses = LocalBackupStorage.loadOutpasses(appCtx)
        if (!savedOutpasses.isNullOrEmpty()) {
            _outpasses.value = savedOutpasses
        } else {
            _outpasses.value = seedOutpasses
            LocalBackupStorage.saveOutpasses(appCtx, seedOutpasses)
        }

        // 3. Synchronously load gate logs from storage
        val savedLogs = LocalBackupStorage.loadGateLogs(appCtx)
        if (!savedLogs.isNullOrEmpty()) {
            _gateLogs.value = savedLogs
        } else {
            _gateLogs.value = seedGateLogs
            LocalBackupStorage.saveGateLogs(appCtx, seedGateLogs)
        }

        // 4. Background Room Database synchronization
        repoScope.launch {
            try {
                val db = AppDatabase.getDatabase(appCtx)
                val roomUsers = db.userDao().getAllUsers()
                if (roomUsers.isEmpty()) {
                    db.userDao().insertUsers(_users.value.map { UserEntity.fromUser(it) })
                } else {
                    val map = _users.value.associateBy { it.id }.toMutableMap()
                    for (ru in roomUsers) {
                        map[ru.id] = ru.toUser()
                    }
                    _users.value = map.values.toList()
                    LocalBackupStorage.saveUsers(appCtx, _users.value)
                }

                val roomOutpasses = db.outpassDao().getAllOutpasses()
                if (roomOutpasses.isEmpty()) {
                    db.outpassDao().insertOutpasses(_outpasses.value.map { OutpassEntity.fromOutpass(it) })
                }

                val roomLogs = db.gateLogDao().getAllGateLogs()
                if (roomLogs.isEmpty()) {
                    db.gateLogDao().insertGateLogs(_gateLogs.value.map { GateLogEntity.fromGateLog(it) })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val now = System.currentTimeMillis()
    private val hourMs = 3600_000L

    // Demo Users Seed Data
    val demoUsers = listOf(
        User(
            id = "u-alex",
            name = "Alex Morgan",
            email = "alex.morgan@vetias.ac.in",
            role = UserRole.STUDENT,
            regNo = "21CS045",
            department = "Computer Science",
            hostelBlock = "Block A (Brahmaputra)",
            roomNumber = "302",
            phone = "+91 9876543210",
            parentPhone = "+91 9123456789",
            photoUri = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop&crop=faces"
        ),
        User(
            id = "u-sarah",
            name = "Sarah Chen",
            email = "sarah.chen@vetias.ac.in",
            role = UserRole.STUDENT,
            regNo = "22EC012",
            department = "Electronics & Comm",
            hostelBlock = "Block B (Narmada)",
            roomNumber = "105",
            phone = "+91 9876500112",
            parentPhone = "+91 9123400112",
            photoUri = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop&crop=faces"
        ),
        User(
            id = "u-vance",
            name = "Dr. Robert Vance",
            email = "r.vance@vetias.ac.in",
            role = UserRole.STAFF_ADVISOR,
            department = "Computer Science",
            phone = "+91 9444455555",
            photoUri = "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=200&h=200&fit=crop&crop=faces"
        ),
        User(
            id = "u-elena",
            name = "Prof. Elena Rostova",
            email = "maniadithyan075@gmail.com",
            role = UserRole.HOD,
            department = "Computer Science",
            phone = "+91 9444466666",
            photoUri = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200&h=200&fit=crop&crop=faces"
        ),
        User(
            id = "u-ram",
            name = "Officer Ram Singh",
            email = "gate.security@vetias.ac.in",
            role = UserRole.SECURITY_OFFICER,
            department = "Campus Security",
            phone = "+91 9888877777",
            photoUri = "https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=200&h=200&fit=crop&crop=faces"
        ),
        User(
            id = "u-brody",
            name = "Chief Warden Dr. Marcus Brody",
            email = "admin@vetias.ac.in",
            role = UserRole.ADMIN,
            department = "Administration",
            phone = "+91 9999900000",
            photoUri = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop&crop=faces"
        ),
        User(
            id = "u-devika",
            name = "Devika Iyer",
            email = "devika.iyer@vetias.ac.in",
            role = UserRole.STUDENT,
            regNo = "22AD015",
            department = DepartmentConstants.DEPT_AI_AND_DATA_SCIENCE,
            hostelBlock = "Block C (Ganga)",
            roomNumber = "204",
            phone = "+91 9876500222",
            parentPhone = "+91 9123400222",
            photoUri = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop&crop=faces"
        ),
        User(
            id = "u-rahul",
            name = "Rahul Verma",
            email = "rahul.verma@vetias.ac.in",
            role = UserRole.STUDENT,
            regNo = "23CA007",
            department = DepartmentConstants.DEPT_COMPUTER_APPLICATION,
            hostelBlock = "Block A (Brahmaputra)",
            roomNumber = "112",
            phone = "+91 9876500333",
            parentPhone = "+91 9123400333",
            photoUri = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200&h=200&fit=crop&crop=faces"
        ),
        User(
            id = "u-kavya",
            name = "Kavya Nair",
            email = "kavya.nair@vetias.ac.in",
            role = UserRole.STUDENT,
            regNo = "22AI022",
            department = DepartmentConstants.DEPT_ARTIFICIAL_INTELLIGENCE,
            hostelBlock = "Block B (Narmada)",
            roomNumber = "315",
            phone = "+91 9876500444",
            parentPhone = "+91 9123400444",
            photoUri = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&h=200&fit=crop&crop=faces"
        ),
        User(
            id = "u-ananya",
            name = "Dr. Ananya Rao",
            email = "a.rao@vetias.ac.in",
            role = UserRole.STAFF_ADVISOR,
            department = DepartmentConstants.DEPT_AI_AND_DATA_SCIENCE,
            phone = "+91 9444477777",
            photoUri = "https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?w=200&h=200&fit=crop&crop=faces"
        ),
        User(
            id = "u-sundar",
            name = "Prof. Sundar Raman",
            email = "hod.aids@vetias.ac.in",
            role = UserRole.HOD,
            department = DepartmentConstants.DEPT_AI_AND_DATA_SCIENCE,
            phone = "+91 9444488888",
            photoUri = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop&crop=faces"
        )
    )

    private val _users = MutableStateFlow<List<User>>(demoUsers)
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(demoUsers[0]) // Default logged in as Alex
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val seedOutpasses = listOf(
        Outpass(
            id = "PASS-1001",
            studentId = "u-alex",
            studentName = "Alex Morgan",
            regNo = "21CS045",
            department = "Computer Science",
            hostelBlock = "Block A (Brahmaputra)",
            roomNo = "302",
            studentPhone = "+91 9876543210",
            parentPhone = "+91 9123456789",
            type = OutpassType.HOME,
            destination = "Home (32 Park Avenue, Green Valley)",
            reason = "Family wedding function over the weekend.",
            outDateTime = now - (1 * hourMs),
            returnDateTime = now + (28 * hourMs),
            status = OutpassStatus.APPROVED,
            qrToken = "PASS-1001::21CS045::APPROVED::" + (now + 28 * hourMs),
            appliedAt = now - (24 * hourMs),
            staffApproval = ApprovalRecord(UserRole.STAFF_ADVISOR, "Dr. Robert Vance", "APPROVED", now - (18 * hourMs), "Verified family function."),
            hodApproval = ApprovalRecord(UserRole.HOD, "Prof. Elena Rostova", "APPROVED", now - (12 * hourMs), "Granted 3-day weekend leave."),
            parentNotified = true,
            studentPhotoUri = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop&crop=faces"
        ),
        Outpass(
            id = "PASS-1002",
            studentId = "u-sarah",
            studentName = "Sarah Chen",
            regNo = "22EC012",
            department = "Electronics & Comm",
            hostelBlock = "Block B (Narmada)",
            roomNo = "105",
            studentPhone = "+91 9876500112",
            parentPhone = "+91 9123400112",
            type = OutpassType.LOCAL,
            destination = "Central City Mall & Book Depot",
            reason = "Purchasing project hardware components & textbooks.",
            outDateTime = now + (2 * hourMs),
            returnDateTime = now + (7 * hourMs),
            status = OutpassStatus.PENDING_STAFF,
            qrToken = "PASS-1002::22EC012::PENDING_STAFF::" + (now + 7 * hourMs),
            appliedAt = now - (2 * hourMs),
            parentNotified = false,
            studentPhotoUri = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop&crop=faces"
        ),
        Outpass(
            id = "PASS-1003",
            studentId = "u-alex",
            studentName = "Alex Morgan",
            regNo = "21CS045",
            department = "Computer Science",
            hostelBlock = "Block A (Brahmaputra)",
            roomNo = "302",
            studentPhone = "+91 9876543210",
            parentPhone = "+91 9123456789",
            type = OutpassType.LOCAL,
            destination = "Metro Healthcare Diagnostic Lab",
            reason = "Blood test and health checkup.",
            outDateTime = now - (3 * hourMs),
            returnDateTime = now + (3 * hourMs),
            status = OutpassStatus.CHECKED_OUT,
            qrToken = "PASS-1003::21CS045::CHECKED_OUT::" + (now + 3 * hourMs),
            appliedAt = now - (8 * hourMs),
            staffApproval = ApprovalRecord(UserRole.STAFF_ADVISOR, "Dr. Robert Vance", "APPROVED", now - (6 * hourMs), "Medical visit authorized."),
            hodApproval = ApprovalRecord(UserRole.HOD, "Prof. Elena Rostova", "APPROVED", now - (5 * hourMs), "Approved."),
            actualCheckOutTime = now - (2 * hourMs),
            parentNotified = true,
            studentPhotoUri = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop&crop=faces"
        ),
        Outpass(
            id = "PASS-1004",
            studentId = "u-alex",
            studentName = "Alex Morgan",
            regNo = "21CS045",
            department = "Computer Science",
            hostelBlock = "Block A (Brahmaputra)",
            roomNo = "302",
            studentPhone = "+91 9876543210",
            parentPhone = "+91 9123456789",
            type = OutpassType.SPECIAL_EVENT,
            destination = "IIT TechFest Hackathon Arena",
            reason = "Representing college in State Hackathon finals.",
            outDateTime = now - (48 * hourMs),
            returnDateTime = now - (24 * hourMs),
            status = OutpassStatus.CHECKED_IN,
            qrToken = "PASS-1004::21CS045::CHECKED_IN::" + (now - 24 * hourMs),
            appliedAt = now - (72 * hourMs),
            staffApproval = ApprovalRecord(UserRole.STAFF_ADVISOR, "Dr. Robert Vance", "APPROVED", now - (60 * hourMs), "Event participation confirmed."),
            hodApproval = ApprovalRecord(UserRole.HOD, "Prof. Elena Rostova", "APPROVED", now - (56 * hourMs), "Good luck!"),
            actualCheckOutTime = now - (47 * hourMs),
            actualCheckInTime = now - (25 * hourMs),
            parentNotified = true,
            studentPhotoUri = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop&crop=faces"
        ),
        Outpass(
            id = "PASS-1005",
            studentId = "u-sarah",
            studentName = "Sarah Chen",
            regNo = "22EC012",
            department = "Electronics & Comm",
            hostelBlock = "Block B (Narmada)",
            roomNo = "105",
            studentPhone = "+91 9876500112",
            parentPhone = "+91 9123400112",
            type = OutpassType.EMERGENCY,
            destination = "City General Hospital",
            reason = "Dental emergency & appointment.",
            outDateTime = now + (1 * hourMs),
            returnDateTime = now + (8 * hourMs),
            status = OutpassStatus.PENDING_HOD,
            qrToken = "PASS-1005::22EC012::PENDING_HOD::" + (now + 8 * hourMs),
            appliedAt = now - (1 * hourMs),
            staffApproval = ApprovalRecord(UserRole.STAFF_ADVISOR, "Dr. Robert Vance", "APPROVED", now - 30 * 60_000L, "Hospital slip verified."),
            parentNotified = true,
            studentPhotoUri = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop&crop=faces"
        ),
        Outpass(
            id = "PASS-1006",
            studentId = "u-devika",
            studentName = "Devika Iyer",
            regNo = "22AD015",
            department = DepartmentConstants.DEPT_AI_AND_DATA_SCIENCE,
            hostelBlock = "Block C (Ganga)",
            roomNo = "204",
            studentPhone = "+91 9876500222",
            parentPhone = "+91 9123400222",
            type = OutpassType.SPECIAL_EVENT,
            destination = "Indian Institute of Science AI Conclave",
            reason = "Presenting research paper on LLM Optimization at AI Summit.",
            outDateTime = now + (3 * hourMs),
            returnDateTime = now + (20 * hourMs),
            status = OutpassStatus.PENDING_STAFF,
            qrToken = "PASS-1006::22AD015::PENDING_STAFF::" + (now + 20 * hourMs),
            appliedAt = now - (3 * hourMs),
            parentNotified = true,
            studentPhotoUri = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop&crop=faces"
        ),
        Outpass(
            id = "PASS-1007",
            studentId = "u-rahul",
            studentName = "Rahul Verma",
            regNo = "23CA007",
            department = DepartmentConstants.DEPT_COMPUTER_APPLICATION,
            hostelBlock = "Block A (Brahmaputra)",
            roomNo = "112",
            studentPhone = "+91 9876500333",
            parentPhone = "+91 9123400333",
            type = OutpassType.LOCAL,
            destination = "City Tech Hub & Software Library",
            reason = "Cloud computing workshop & server setup documentation.",
            outDateTime = now + (1 * hourMs),
            returnDateTime = now + (6 * hourMs),
            status = OutpassStatus.PENDING_HOD,
            qrToken = "PASS-1007::23CA007::PENDING_HOD::" + (now + 6 * hourMs),
            appliedAt = now - (2 * hourMs),
            staffApproval = ApprovalRecord(UserRole.STAFF_ADVISOR, "Prof. Meenakshi Sundaram", "APPROVED", now - (1 * hourMs), "Lab timing verified."),
            parentNotified = true,
            studentPhotoUri = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200&h=200&fit=crop&crop=faces"
        ),
        Outpass(
            id = "PASS-1008",
            studentId = "u-kavya",
            studentName = "Kavya Nair",
            regNo = "22AI022",
            department = DepartmentConstants.DEPT_ARTIFICIAL_INTELLIGENCE,
            hostelBlock = "Block B (Narmada)",
            roomNo = "315",
            studentPhone = "+91 9876500444",
            parentPhone = "+91 9123400444",
            type = OutpassType.HOME,
            destination = "Home (45 Lotus Residency)",
            reason = "Family festival celebrations.",
            outDateTime = now - (1 * hourMs),
            returnDateTime = now + (30 * hourMs),
            status = OutpassStatus.APPROVED,
            qrToken = "PASS-1008::22AI022::APPROVED::" + (now + 30 * hourMs),
            appliedAt = now - (20 * hourMs),
            staffApproval = ApprovalRecord(UserRole.STAFF_ADVISOR, "Dr. K. Narayanan", "APPROVED", now - (16 * hourMs), "Parent confirmed over phone call."),
            hodApproval = ApprovalRecord(UserRole.HOD, "Prof. Elena Rostova", "APPROVED", now - (10 * hourMs), "Granted weekend leave."),
            parentNotified = true,
            studentPhotoUri = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&h=200&fit=crop&crop=faces"
        )
    )

    private val _outpasses = MutableStateFlow<List<Outpass>>(seedOutpasses)
    val outpasses: StateFlow<List<Outpass>> = _outpasses.asStateFlow()

    private val seedGateLogs = listOf(
        GateLog("log-1", "PASS-1003", "Alex Morgan", "21CS045", "CHECK_OUT", now - (2 * hourMs), "Officer Ram Singh", "Exit scan verified at Gate 1"),
        GateLog("log-2", "PASS-1004", "Alex Morgan", "21CS045", "CHECK_OUT", now - (47 * hourMs), "Officer Ram Singh", "Team exit for Hackathon"),
        GateLog("log-3", "PASS-1004", "Alex Morgan", "21CS045", "CHECK_IN", now - (25 * hourMs), "Officer Ram Singh", "Returned safely with team")
    )

    private val _gateLogs = MutableStateFlow<List<GateLog>>(seedGateLogs)
    val gateLogs: StateFlow<List<GateLog>> = _gateLogs.asStateFlow()

    // Persistent storage helpers
    private fun persistUsers() {
        val list = _users.value
        appContext?.let { ctx ->
            LocalBackupStorage.saveUsers(ctx, list)
            repoScope.launch {
                try {
                    val db = AppDatabase.getDatabase(ctx)
                    db.userDao().insertUsers(list.map { UserEntity.fromUser(it) })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun persistOutpasses() {
        val list = _outpasses.value
        appContext?.let { ctx ->
            LocalBackupStorage.saveOutpasses(ctx, list)
            repoScope.launch {
                try {
                    val db = AppDatabase.getDatabase(ctx)
                    db.outpassDao().insertOutpasses(list.map { OutpassEntity.fromOutpass(it) })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun persistGateLogs() {
        val list = _gateLogs.value
        appContext?.let { ctx ->
            LocalBackupStorage.saveGateLogs(ctx, list)
            repoScope.launch {
                try {
                    val db = AppDatabase.getDatabase(ctx)
                    db.gateLogDao().insertGateLogs(list.map { GateLogEntity.fromGateLog(it) })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Authentication methods
    fun setCurrentUser(user: User) {
        _currentUser.value = user
    }

    fun logout() {
        _currentUser.value = null
    }

    fun loginDemoRole(role: UserRole) {
        val user = _users.value.firstOrNull { it.role == role }
        if (user != null) {
            _currentUser.value = user
        }
    }

    fun login(query: String, passwordAttempt: String? = null, requiredRole: UserRole? = null): Triple<Boolean, String, User?> {
        val clean = query.trim()
        val user = _users.value.firstOrNull { 
            it.email.equals(clean, ignoreCase = true) || 
            it.regNo.equals(clean, ignoreCase = true) ||
            it.id.equals(clean, ignoreCase = true)
        } ?: return Triple(false, "Account not found for '$query'. Please register an account.", null)

        if (requiredRole != null && user.role != requiredRole) {
            return Triple(
                false,
                "Role Restriction: You selected '${requiredRole.displayName}', but this account is registered as '${user.role.displayName}'. Please click '${user.role.displayName}' above to enter the ${user.role.displayName} module.",
                null
            )
        }

        if (!passwordAttempt.isNullOrBlank()) {
            if (user.password.isNotBlank() && user.password != passwordAttempt) {
                return Triple(false, "Incorrect password. If you forgot your password, please click 'Forgot Password?'.", null)
            }
        }

        _currentUser.value = user
        return Triple(true, "Login successful", user)
    }

    fun login(query: String): Boolean {
        return login(query, null, null).first
    }

    fun updateUser(updatedUser: User) {
        val list = _users.value.toMutableList()
        val index = list.indexOfFirst { it.id == updatedUser.id }
        if (index != -1) {
            list[index] = updatedUser
            _users.value = list
            if (_currentUser.value?.id == updatedUser.id) {
                _currentUser.value = updatedUser
            }
            persistUsers()
        }
    }

    fun deleteUser(userId: String): Boolean {
        val list = _users.value.toMutableList()
        val removed = list.removeAll { it.id == userId }
        if (removed) {
            _users.value = list
            if (_currentUser.value?.id == userId) {
                _currentUser.value = list.firstOrNull()
            }
            persistUsers()
            appContext?.let { ctx ->
                repoScope.launch {
                    try {
                        AppDatabase.getDatabase(ctx).userDao().deleteUserById(userId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return removed
    }

    fun findUserByIdentifier(identifier: String): User? {
        val clean = identifier.trim()
        return _users.value.firstOrNull {
            it.email.equals(clean, ignoreCase = true) ||
            it.regNo.equals(clean, ignoreCase = true) ||
            it.id.equals(clean, ignoreCase = true)
        }
    }

    fun resetPassword(identifier: String, newPassword: String, newName: String? = null): Pair<Boolean, String> {
        val clean = identifier.trim()
        val list = _users.value.toMutableList()
        val index = list.indexOfFirst {
            it.email.equals(clean, ignoreCase = true) ||
            it.regNo.equals(clean, ignoreCase = true) ||
            it.id.equals(clean, ignoreCase = true)
        }
        if (index == -1) {
            return Pair(false, "No account found matching '$identifier'. Please verify your Email or Roll No.")
        }

        val existingUser = list[index]
        val updatedName = if (!newName.isNullOrBlank()) newName.trim() else existingUser.name
        val updatedUser = existingUser.copy(
            name = updatedName,
            password = newPassword,
            lastPasswordResetAt = System.currentTimeMillis()
        )
        list[index] = updatedUser
        _users.value = list

        if (_currentUser.value?.id == existingUser.id) {
            _currentUser.value = updatedUser
        }
        persistUsers()

        return Pair(true, "Password updated successfully for ${updatedUser.name}!")
    }

    fun registerUser(user: User) {
        val updated = _users.value.toMutableList()
        val index = updated.indexOfFirst { 
            it.id == user.id || 
            it.email.equals(user.email, ignoreCase = true) || 
            (user.regNo.isNotBlank() && it.regNo.equals(user.regNo, ignoreCase = true))
        }
        if (index != -1) {
            updated[index] = user
        } else {
            updated.add(user)
        }
        _users.value = updated
        _currentUser.value = user
        persistUsers()
    }

    fun applyOutpass(
        student: User,
        type: OutpassType,
        destination: String,
        reason: String,
        outDateTime: Long,
        returnDateTime: Long
    ): Outpass {
        val passId = "PASS-" + (1000 + _outpasses.value.size + 1)
        val qrToken = "$passId::${student.regNo}::PENDING_STAFF::$returnDateTime"
        val newPass = Outpass(
            id = passId,
            studentId = student.id,
            studentName = student.name,
            regNo = student.regNo,
            department = student.department,
            hostelBlock = student.hostelBlock,
            roomNo = student.roomNumber,
            studentPhone = student.phone,
            parentPhone = student.parentPhone,
            type = type,
            destination = destination,
            reason = reason,
            outDateTime = outDateTime,
            returnDateTime = returnDateTime,
            status = OutpassStatus.PENDING_STAFF,
            qrToken = qrToken,
            appliedAt = System.currentTimeMillis(),
            studentPhotoUri = student.photoUri
        )
        val list = _outpasses.value.toMutableList()
        list.add(0, newPass)
        _outpasses.value = list
        persistOutpasses()
        return newPass
    }

    fun approveOutpass(passId: String, approver: User, remarks: String) {
        val list = _outpasses.value.toMutableList()
        val index = list.indexOfFirst { it.id == passId }
        if (index != -1) {
            val pass = list[index]
            val nowTime = System.currentTimeMillis()
            val record = ApprovalRecord(approver.role, approver.name, "APPROVED", nowTime, remarks)

            val updatedPass = if (approver.role == UserRole.STAFF_ADVISOR) {
                pass.copy(
                    status = OutpassStatus.PENDING_HOD,
                    staffApproval = record,
                    qrToken = "${pass.id}::${pass.regNo}::PENDING_HOD::${pass.returnDateTime}",
                    parentNotified = true
                )
            } else if (approver.role == UserRole.HOD || approver.role == UserRole.ADMIN) {
                pass.copy(
                    status = OutpassStatus.APPROVED,
                    hodApproval = record,
                    qrToken = "${pass.id}::${pass.regNo}::APPROVED::${pass.returnDateTime}",
                    parentNotified = true
                )
            } else pass

            list[index] = updatedPass
            _outpasses.value = list
            persistOutpasses()
        }
    }

    fun rejectOutpass(passId: String, approver: User, reason: String) {
        val list = _outpasses.value.toMutableList()
        val index = list.indexOfFirst { it.id == passId }
        if (index != -1) {
            val pass = list[index]
            val record = ApprovalRecord(approver.role, approver.name, "REJECTED", System.currentTimeMillis(), reason)
            val updatedPass = pass.copy(
                status = OutpassStatus.REJECTED,
                rejectionReason = reason,
                staffApproval = if (approver.role == UserRole.STAFF_ADVISOR) record else pass.staffApproval,
                hodApproval = if (approver.role == UserRole.HOD) record else pass.hodApproval,
                qrToken = "${pass.id}::${pass.regNo}::REJECTED::${pass.returnDateTime}",
                parentNotified = true
            )
            list[index] = updatedPass
            _outpasses.value = list
            persistOutpasses()
        }
    }

    fun checkOutGate(passId: String, officerName: String): String {
        val list = _outpasses.value.toMutableList()
        val index = list.indexOfFirst { it.id == passId }
        if (index == -1) return "Outpass ID $passId not found."
        val pass = list[index]

        if (pass.isQrUsed || pass.status == OutpassStatus.CHECKED_OUT || pass.status == OutpassStatus.CHECKED_IN) {
            return "QR CODE EXPIRED: Exit QR code for ${pass.studentName} (${pass.regNo}) has ALREADY been scanned & used! Re-use denied."
        }

        if (pass.status != OutpassStatus.APPROVED) {
            return "Pass is not in APPROVED state (Current status: ${pass.status.displayName}). Exit denied."
        }

        val nowTime = System.currentTimeMillis()
        val updatedPass = pass.copy(
            status = OutpassStatus.CHECKED_OUT,
            actualCheckOutTime = nowTime,
            isQrUsed = true,
            qrToken = "${pass.id}::${pass.regNo}::EXPIRED_USED::$nowTime"
        )
        list[index] = updatedPass
        _outpasses.value = list
        persistOutpasses()

        val newLog = GateLog(
            id = "log-" + UUID.randomUUID().toString().take(6),
            outpassId = pass.id,
            studentName = pass.studentName,
            regNo = pass.regNo,
            action = "CHECK_OUT",
            timestamp = nowTime,
            officerName = officerName,
            remarks = "Gate exit scan verified. QR code expired."
        )
        val logs = _gateLogs.value.toMutableList()
        logs.add(0, newLog)
        _gateLogs.value = logs
        persistGateLogs()

        return "SUCCESS: Exit scan verified for ${pass.studentName} (${pass.department}). Gate Exit logged & QR Code is now EXPIRED."
    }

    fun checkInGate(passId: String, officerName: String, isSameDayReentry: Boolean = true): String {
        val list = _outpasses.value.toMutableList()
        val index = list.indexOfFirst { it.id == passId }
        if (index == -1) return "Outpass ID $passId not found."
        val pass = list[index]

        if (pass.status != OutpassStatus.CHECKED_OUT) {
            return "Student is not currently checked out (Current status: ${pass.status.displayName})."
        }

        val nowTime = System.currentTimeMillis()
        val updatedPass = pass.copy(
            status = OutpassStatus.CHECKED_IN,
            actualCheckInTime = nowTime,
            qrToken = "${pass.id}::${pass.regNo}::CHECKED_IN::$nowTime"
        )
        list[index] = updatedPass
        _outpasses.value = list
        persistOutpasses()

        val actionName = if (isSameDayReentry) "SAME_DAY_REENTRY" else "CHECK_IN"
        val remarksMsg = if (isSameDayReentry) {
            "Student returned on the same day. Same-day campus re-entry verified at Main Gate."
        } else {
            "Campus Gate Entry recorded."
        }

        val newLog = GateLog(
            id = "log-" + UUID.randomUUID().toString().take(6),
            outpassId = pass.id,
            studentName = pass.studentName,
            regNo = pass.regNo,
            action = actionName,
            timestamp = nowTime,
            officerName = officerName,
            remarks = remarksMsg
        )
        val logs = _gateLogs.value.toMutableList()
        logs.add(0, newLog)
        _gateLogs.value = logs
        persistGateLogs()

        val df = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return "SUCCESS: Same-Day Re-Entry verified for ${pass.studentName} (${pass.department}) at Main Gate at ${df.format(Date(nowTime))}."
    }
}
