package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.models.ApprovalRecord
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.data.models.OutpassType
import com.example.data.models.UserRole

@Entity(tableName = "outpasses")
data class OutpassEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val studentName: String,
    val regNo: String,
    val department: String,
    val hostelBlock: String,
    val roomNo: String,
    val studentPhone: String,
    val parentPhone: String,
    val type: String,
    val destination: String,
    val reason: String,
    val outDateTime: Long,
    val returnDateTime: Long,
    val status: String,
    val qrToken: String,
    val appliedAt: Long,
    val staffApproverRole: String? = null,
    val staffApproverName: String? = null,
    val staffStatus: String? = null,
    val staffTimestamp: Long? = null,
    val staffRemarks: String? = null,
    val hodApproverRole: String? = null,
    val hodApproverName: String? = null,
    val hodStatus: String? = null,
    val hodTimestamp: Long? = null,
    val hodRemarks: String? = null,
    val actualCheckOutTime: Long? = null,
    val actualCheckInTime: Long? = null,
    val rejectionReason: String? = null,
    val parentNotified: Boolean = false,
    val isQrUsed: Boolean = false,
    val studentPhotoUri: String? = null
) {
    fun toOutpass(): Outpass {
        val staffApp = if (staffApproverName != null && staffStatus != null && staffTimestamp != null) {
            val role = try { UserRole.valueOf(staffApproverRole ?: "STAFF_ADVISOR") } catch (_: Exception) { UserRole.STAFF_ADVISOR }
            ApprovalRecord(role, staffApproverName, staffStatus, staffTimestamp, staffRemarks ?: "")
        } else null

        val hodApp = if (hodApproverName != null && hodStatus != null && hodTimestamp != null) {
            val role = try { UserRole.valueOf(hodApproverRole ?: "HOD") } catch (_: Exception) { UserRole.HOD }
            ApprovalRecord(role, hodApproverName, hodStatus, hodTimestamp, hodRemarks ?: "")
        } else null

        return Outpass(
            id = id,
            studentId = studentId,
            studentName = studentName,
            regNo = regNo,
            department = department,
            hostelBlock = hostelBlock,
            roomNo = roomNo,
            studentPhone = studentPhone,
            parentPhone = parentPhone,
            type = try { OutpassType.valueOf(type) } catch (_: Exception) { OutpassType.LOCAL },
            destination = destination,
            reason = reason,
            outDateTime = outDateTime,
            returnDateTime = returnDateTime,
            status = try { OutpassStatus.valueOf(status) } catch (_: Exception) { OutpassStatus.PENDING_STAFF },
            qrToken = qrToken,
            appliedAt = appliedAt,
            staffApproval = staffApp,
            hodApproval = hodApp,
            actualCheckOutTime = actualCheckOutTime,
            actualCheckInTime = actualCheckInTime,
            rejectionReason = rejectionReason,
            parentNotified = parentNotified,
            isQrUsed = isQrUsed,
            studentPhotoUri = studentPhotoUri
        )
    }

    companion object {
        fun fromOutpass(outpass: Outpass): OutpassEntity = OutpassEntity(
            id = outpass.id,
            studentId = outpass.studentId,
            studentName = outpass.studentName,
            regNo = outpass.regNo,
            department = outpass.department,
            hostelBlock = outpass.hostelBlock,
            roomNo = outpass.roomNo,
            studentPhone = outpass.studentPhone,
            parentPhone = outpass.parentPhone,
            type = outpass.type.name,
            destination = outpass.destination,
            reason = outpass.reason,
            outDateTime = outpass.outDateTime,
            returnDateTime = outpass.returnDateTime,
            status = outpass.status.name,
            qrToken = outpass.qrToken,
            appliedAt = outpass.appliedAt,
            staffApproverRole = outpass.staffApproval?.approverRole?.name,
            staffApproverName = outpass.staffApproval?.approverName,
            staffStatus = outpass.staffApproval?.status,
            staffTimestamp = outpass.staffApproval?.timestamp,
            staffRemarks = outpass.staffApproval?.remarks,
            hodApproverRole = outpass.hodApproval?.approverRole?.name,
            hodApproverName = outpass.hodApproval?.approverName,
            hodStatus = outpass.hodApproval?.status,
            hodTimestamp = outpass.hodApproval?.timestamp,
            hodRemarks = outpass.hodApproval?.remarks,
            actualCheckOutTime = outpass.actualCheckOutTime,
            actualCheckInTime = outpass.actualCheckInTime,
            rejectionReason = outpass.rejectionReason,
            parentNotified = outpass.parentNotified,
            isQrUsed = outpass.isQrUsed,
            studentPhotoUri = outpass.studentPhotoUri
        )
    }
}
