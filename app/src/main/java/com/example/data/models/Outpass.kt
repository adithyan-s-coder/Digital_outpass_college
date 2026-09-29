package com.example.data.models

data class ApprovalRecord(
    val approverRole: UserRole,
    val approverName: String,
    val status: String, // "APPROVED" or "REJECTED"
    val timestamp: Long,
    val remarks: String = ""
)

data class GateLog(
    val id: String,
    val outpassId: String,
    val studentName: String,
    val regNo: String,
    val action: String, // "CHECK_OUT" or "CHECK_IN"
    val timestamp: Long,
    val officerName: String,
    val remarks: String = ""
)

data class Outpass(
    val id: String,
    val studentId: String,
    val studentName: String,
    val regNo: String,
    val department: String,
    val hostelBlock: String,
    val roomNo: String,
    val studentPhone: String,
    val parentPhone: String,
    val type: OutpassType,
    val destination: String,
    val reason: String,
    val outDateTime: Long,
    val returnDateTime: Long,
    val status: OutpassStatus,
    val qrToken: String,
    val appliedAt: Long,
    val staffApproval: ApprovalRecord? = null,
    val hodApproval: ApprovalRecord? = null,
    val actualCheckOutTime: Long? = null,
    val actualCheckInTime: Long? = null,
    val rejectionReason: String? = null,
    val parentNotified: Boolean = false,
    val isQrUsed: Boolean = false,
    val studentPhotoUri: String? = null
) {
    /**
     * The QR code is issued upon final approval (or application time) and is strictly valid for 1 hour.
     */
    fun getQrExpiryTime(): Long {
        val baseTime = hodApproval?.timestamp ?: staffApproval?.timestamp ?: appliedAt
        return baseTime + 3600_000L // 1 hour validity window
    }

    /**
     * Checks if the QR code is expired.
     * True if already used for checkout, or if more than 1 hour has elapsed since approval.
     */
    fun isQrExpired(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        if (isQrUsed) return true
        if (status == OutpassStatus.CHECKED_OUT || status == OutpassStatus.CHECKED_IN) return true
        if (status == OutpassStatus.APPROVED) {
            val expiryTime = getQrExpiryTime()
            return currentTimeMs > expiryTime
        }
        return false
    }
}
