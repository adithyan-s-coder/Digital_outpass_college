package com.example.data.models

import org.json.JSONObject

/**
 * Encodes and decodes self-contained outpass data into a portable QR token.
 * This ensures that when the APK is installed on different physical devices (e.g. Student phone and Security phone),
 * the Security phone can scan and verify the outpass with full student identity details even without a shared cloud backend.
 */
object OutpassQrHelper {
    private const val PREFIX = "VETIAS_PASS_V1::"

    fun encodeToQr(pass: Outpass): String {
        return try {
            val json = JSONObject().apply {
                put("id", pass.id)
                put("name", pass.studentName)
                put("regNo", pass.regNo)
                put("dept", pass.department)
                put("block", pass.hostelBlock)
                put("room", pass.roomNo)
                put("phone", pass.studentPhone)
                put("parent", pass.parentPhone)
                put("type", pass.type.name)
                put("dest", pass.destination)
                put("reason", pass.reason)
                put("outTime", pass.outDateTime)
                put("retTime", pass.returnDateTime)
                put("status", pass.status.name)
                put("appliedAt", pass.appliedAt)
                put("photoUri", pass.studentPhotoUri ?: "")
                if (pass.hodApproval != null) {
                    put("hodApprover", pass.hodApproval.approverName)
                    put("hodRole", pass.hodApproval.approverRole.name)
                    put("hodTime", pass.hodApproval.timestamp)
                    put("hodRemarks", pass.hodApproval.remarks)
                }
                if (pass.staffApproval != null) {
                    put("staffApprover", pass.staffApproval.approverName)
                    put("staffRole", pass.staffApproval.approverRole.name)
                    put("staffTime", pass.staffApproval.timestamp)
                    put("staffRemarks", pass.staffApproval.remarks)
                }
            }
            PREFIX + json.toString()
        } catch (e: Exception) {
            pass.id
        }
    }

    fun decodeFromQr(rawText: String): Outpass? {
        val clean = rawText.trim()
        val jsonStr = when {
            clean.startsWith(PREFIX) -> clean.removePrefix(PREFIX)
            clean.startsWith("{") && clean.endsWith("}") -> clean
            clean.contains(PREFIX) -> clean.substringAfter(PREFIX)
            else -> return null
        }

        return try {
            val obj = JSONObject(jsonStr)
            val typeStr = obj.optString("type", "LOCAL")
            val passType = try { OutpassType.valueOf(typeStr) } catch (_: Exception) { OutpassType.LOCAL }
            val statusStr = obj.optString("status", "APPROVED")
            val passStatus = try { OutpassStatus.valueOf(statusStr) } catch (_: Exception) { OutpassStatus.APPROVED }

            val hodApproval = if (obj.has("hodApprover")) {
                val approverRoleStr = obj.optString("hodRole", "HOD")
                val role = try { UserRole.valueOf(approverRoleStr) } catch (_: Exception) { UserRole.HOD }
                ApprovalRecord(
                    approverRole = role,
                    approverName = obj.optString("hodApprover", "HOD Approver"),
                    status = "APPROVED",
                    timestamp = obj.optLong("hodTime", System.currentTimeMillis()),
                    remarks = obj.optString("hodRemarks", "Approved")
                )
            } else null

            val staffApproval = if (obj.has("staffApprover")) {
                val approverRoleStr = obj.optString("staffRole", "STAFF_ADVISOR")
                val role = try { UserRole.valueOf(approverRoleStr) } catch (_: Exception) { UserRole.STAFF_ADVISOR }
                ApprovalRecord(
                    approverRole = role,
                    approverName = obj.optString("staffApprover", "Staff Advisor"),
                    status = "APPROVED",
                    timestamp = obj.optLong("staffTime", System.currentTimeMillis()),
                    remarks = obj.optString("staffRemarks", "Verified")
                )
            } else null

            val photoUri = obj.optString("photoUri", "").takeIf { it.isNotBlank() }

            Outpass(
                id = obj.getString("id"),
                studentId = "u-" + obj.optString("regNo", "student"),
                studentName = obj.getString("name"),
                regNo = obj.getString("regNo"),
                department = obj.optString("dept", "General"),
                hostelBlock = obj.optString("block", "Campus Residence"),
                roomNo = obj.optString("room", "N/A"),
                studentPhone = obj.optString("phone", ""),
                parentPhone = obj.optString("parent", ""),
                type = passType,
                destination = obj.optString("dest", "City"),
                reason = obj.optString("reason", "Campus Outpass"),
                outDateTime = obj.optLong("outTime", System.currentTimeMillis()),
                returnDateTime = obj.optLong("retTime", System.currentTimeMillis() + 7200000L),
                status = passStatus,
                qrToken = rawText,
                appliedAt = obj.optLong("appliedAt", System.currentTimeMillis()),
                staffApproval = staffApproval,
                hodApproval = hodApproval,
                studentPhotoUri = photoUri
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
