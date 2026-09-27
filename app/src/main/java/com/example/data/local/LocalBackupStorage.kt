package com.example.data.local

import android.content.Context
import com.example.data.models.ApprovalRecord
import com.example.data.models.GateLog
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.data.models.OutpassType
import com.example.data.models.User
import com.example.data.models.UserRole
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object LocalBackupStorage {

    private const val USERS_FILE = "persistent_users.json"
    private const val OUTPASSES_FILE = "persistent_outpasses.json"
    private const val GATELOGS_FILE = "persistent_gatelogs.json"

    fun saveUsers(context: Context, users: List<User>) {
        try {
            val jsonArray = JSONArray()
            for (u in users) {
                val obj = JSONObject()
                obj.put("id", u.id)
                obj.put("name", u.name)
                obj.put("email", u.email)
                obj.put("role", u.role.name)
                obj.put("regNo", u.regNo)
                obj.put("department", u.department)
                obj.put("hostelBlock", u.hostelBlock)
                obj.put("roomNumber", u.roomNumber)
                obj.put("phone", u.phone)
                obj.put("parentPhone", u.parentPhone)
                if (u.photoUri != null) obj.put("photoUri", u.photoUri)
                obj.put("password", u.password)
                if (u.lastPasswordResetAt != null) obj.put("lastPasswordResetAt", u.lastPasswordResetAt)
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, USERS_FILE)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadUsers(context: Context): List<User>? {
        return try {
            val file = File(context.filesDir, USERS_FILE)
            if (!file.exists()) return null
            val content = file.readText()
            if (content.isBlank()) return null
            val jsonArray = JSONArray(content)
            val list = mutableListOf<User>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val role = try { UserRole.valueOf(obj.getString("role")) } catch (_: Exception) { UserRole.STUDENT }
                list.add(
                    User(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        email = obj.getString("email"),
                        role = role,
                        regNo = obj.optString("regNo", ""),
                        department = obj.optString("department", "Computer Science"),
                        hostelBlock = obj.optString("hostelBlock", "Block A"),
                        roomNumber = obj.optString("roomNumber", "101"),
                        phone = obj.optString("phone", ""),
                        parentPhone = obj.optString("parentPhone", ""),
                        photoUri = if (obj.has("photoUri")) obj.getString("photoUri") else null,
                        password = obj.optString("password", "Pass@1234"),
                        lastPasswordResetAt = if (obj.has("lastPasswordResetAt")) obj.getLong("lastPasswordResetAt") else null
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveOutpasses(context: Context, outpasses: List<Outpass>) {
        try {
            val jsonArray = JSONArray()
            for (p in outpasses) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("studentId", p.studentId)
                obj.put("studentName", p.studentName)
                obj.put("regNo", p.regNo)
                obj.put("department", p.department)
                obj.put("hostelBlock", p.hostelBlock)
                obj.put("roomNo", p.roomNo)
                obj.put("studentPhone", p.studentPhone)
                obj.put("parentPhone", p.parentPhone)
                obj.put("type", p.type.name)
                obj.put("destination", p.destination)
                obj.put("reason", p.reason)
                obj.put("outDateTime", p.outDateTime)
                obj.put("returnDateTime", p.returnDateTime)
                obj.put("status", p.status.name)
                obj.put("qrToken", p.qrToken)
                obj.put("appliedAt", p.appliedAt)
                if (p.staffApproval != null) {
                    val staff = JSONObject()
                    staff.put("role", p.staffApproval.approverRole.name)
                    staff.put("name", p.staffApproval.approverName)
                    staff.put("status", p.staffApproval.status)
                    staff.put("timestamp", p.staffApproval.timestamp)
                    staff.put("remarks", p.staffApproval.remarks)
                    obj.put("staffApproval", staff)
                }
                if (p.hodApproval != null) {
                    val hod = JSONObject()
                    hod.put("role", p.hodApproval.approverRole.name)
                    hod.put("name", p.hodApproval.approverName)
                    hod.put("status", p.hodApproval.status)
                    hod.put("timestamp", p.hodApproval.timestamp)
                    hod.put("remarks", p.hodApproval.remarks)
                    obj.put("hodApproval", hod)
                }
                if (p.actualCheckOutTime != null) obj.put("actualCheckOutTime", p.actualCheckOutTime)
                if (p.actualCheckInTime != null) obj.put("actualCheckInTime", p.actualCheckInTime)
                if (p.rejectionReason != null) obj.put("rejectionReason", p.rejectionReason)
                obj.put("parentNotified", p.parentNotified)
                obj.put("isQrUsed", p.isQrUsed)
                if (p.studentPhotoUri != null) obj.put("studentPhotoUri", p.studentPhotoUri)
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, OUTPASSES_FILE)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadOutpasses(context: Context): List<Outpass>? {
        return try {
            val file = File(context.filesDir, OUTPASSES_FILE)
            if (!file.exists()) return null
            val content = file.readText()
            if (content.isBlank()) return null
            val jsonArray = JSONArray(content)
            val list = mutableListOf<Outpass>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val type = try { OutpassType.valueOf(obj.getString("type")) } catch (_: Exception) { OutpassType.LOCAL }
                val status = try { OutpassStatus.valueOf(obj.getString("status")) } catch (_: Exception) { OutpassStatus.PENDING_STAFF }

                val staffApp = if (obj.has("staffApproval")) {
                    val s = obj.getJSONObject("staffApproval")
                    val r = try { UserRole.valueOf(s.getString("role")) } catch (_: Exception) { UserRole.STAFF_ADVISOR }
                    ApprovalRecord(r, s.getString("name"), s.getString("status"), s.getLong("timestamp"), s.optString("remarks", ""))
                } else null

                val hodApp = if (obj.has("hodApproval")) {
                    val h = obj.getJSONObject("hodApproval")
                    val r = try { UserRole.valueOf(h.getString("role")) } catch (_: Exception) { UserRole.HOD }
                    ApprovalRecord(r, h.getString("name"), h.getString("status"), h.getLong("timestamp"), h.optString("remarks", ""))
                } else null

                list.add(
                    Outpass(
                        id = obj.getString("id"),
                        studentId = obj.getString("studentId"),
                        studentName = obj.getString("studentName"),
                        regNo = obj.getString("regNo"),
                        department = obj.getString("department"),
                        hostelBlock = obj.getString("hostelBlock"),
                        roomNo = obj.getString("roomNo"),
                        studentPhone = obj.getString("studentPhone"),
                        parentPhone = obj.getString("parentPhone"),
                        type = type,
                        destination = obj.getString("destination"),
                        reason = obj.getString("reason"),
                        outDateTime = obj.getLong("outDateTime"),
                        returnDateTime = obj.getLong("returnDateTime"),
                        status = status,
                        qrToken = obj.getString("qrToken"),
                        appliedAt = obj.getLong("appliedAt"),
                        staffApproval = staffApp,
                        hodApproval = hodApp,
                        actualCheckOutTime = if (obj.has("actualCheckOutTime")) obj.getLong("actualCheckOutTime") else null,
                        actualCheckInTime = if (obj.has("actualCheckInTime")) obj.getLong("actualCheckInTime") else null,
                        rejectionReason = if (obj.has("rejectionReason")) obj.getString("rejectionReason") else null,
                        parentNotified = obj.optBoolean("parentNotified", false),
                        isQrUsed = obj.optBoolean("isQrUsed", false),
                        studentPhotoUri = if (obj.has("studentPhotoUri")) obj.getString("studentPhotoUri") else null
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveGateLogs(context: Context, logs: List<GateLog>) {
        try {
            val jsonArray = JSONArray()
            for (l in logs) {
                val obj = JSONObject()
                obj.put("id", l.id)
                obj.put("outpassId", l.outpassId)
                obj.put("studentName", l.studentName)
                obj.put("regNo", l.regNo)
                obj.put("action", l.action)
                obj.put("timestamp", l.timestamp)
                obj.put("officerName", l.officerName)
                obj.put("remarks", l.remarks)
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, GATELOGS_FILE)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadGateLogs(context: Context): List<GateLog>? {
        return try {
            val file = File(context.filesDir, GATELOGS_FILE)
            if (!file.exists()) return null
            val content = file.readText()
            if (content.isBlank()) return null
            val jsonArray = JSONArray(content)
            val list = mutableListOf<GateLog>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    GateLog(
                        id = obj.getString("id"),
                        outpassId = obj.getString("outpassId"),
                        studentName = obj.getString("studentName"),
                        regNo = obj.getString("regNo"),
                        action = obj.getString("action"),
                        timestamp = obj.getLong("timestamp"),
                        officerName = obj.getString("officerName"),
                        remarks = obj.optString("remarks", "")
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
