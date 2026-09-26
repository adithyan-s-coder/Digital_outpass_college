package com.example.data.repository

import com.example.data.models.ApprovalRecord
import com.example.data.models.GateLog
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.data.models.OutpassType
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.data.models.DepartmentConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class OutpassRepository {

    companion object {
        @Volatile
        private var instance: OutpassRepository? = null

        fun getInstance(): OutpassRepository {
            return instance ?: synchronized(this) {
                instance ?: OutpassRepository().also { instance = it }
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

    private val _gateLogs = MutableStateFlow<List<GateLog>>(
        listOf(
            GateLog("log-1", "PASS-1003", "Alex Morgan", "21CS045", "CHECK_OUT", now - (2 * hourMs), "Officer Ram Singh", "Exit scan verified at Gate 1"),
            GateLog("log-2", "PASS-1004", "Alex Morgan", "21CS045", "CHECK_OUT", now - (47 * hourMs), "Officer Ram Singh", "Team exit for Hackathon"),
            GateLog("log-3", "PASS-1004", "Alex Morgan", "21CS045", "CHECK_IN", now - (25 * hourMs), "Officer Ram Singh", "Returned safely with team")
        )
    )
    val gateLogs: StateFlow<List<GateLog>> = _gateLogs.asStateFlow()

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

    fun login(query: String): Boolean {
        val clean = query.trim()
        val user = _users.value.firstOrNull { 
            it.email.equals(clean, ignoreCase = true) || 
            it.regNo.equals(clean, ignoreCase = true) ||
            it.id.equals(clean, ignoreCase = true)
        }
        return if (user != null) {
            _currentUser.value = user
            true
        } else false
    }

    fun registerUser(user: User) {
        val updated = _users.value.toMutableList()
        updated.add(user)
        _users.value = updated
        _currentUser.value = user
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

        return "SUCCESS: Exit scan verified for ${pass.studentName} (${pass.department}). Gate Exit logged & QR Code is now EXPIRED."
    }

    fun checkInGate(passId: String, officerName: String): String {
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
            qrToken = "${pass.id}::${pass.regNo}::CHECKED_IN::${pass.returnDateTime}"
        )
        list[index] = updatedPass
        _outpasses.value = list

        val newLog = GateLog(
            id = "log-" + UUID.randomUUID().toString().take(6),
            outpassId = pass.id,
            studentName = pass.studentName,
            regNo = pass.regNo,
            action = "CHECK_IN",
            timestamp = nowTime,
            officerName = officerName,
            remarks = "Campus Gate Entry recorded."
        )
        val logs = _gateLogs.value.toMutableList()
        logs.add(0, newLog)
        _gateLogs.value = logs

        val df = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return "SUCCESS: Student ${pass.studentName} (${pass.department}) Checked-In / Entry recorded at Main Gate at ${df.format(Date(nowTime))}."
    }
}
