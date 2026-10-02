package com.example.data.sync

import android.content.Context
import android.os.StrictMode
import android.util.Base64
import android.util.Log
import com.example.data.models.ApprovalRecord
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.data.models.OutpassType
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.data.repository.OutpassRepository
import com.example.ui.util.ImageCropUtil
import com.example.util.OutpassNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private const val BASE_URL = "https://api.restful-api.dev/objects"
    const val USERS_OBJECT_ID = "ff808181a09d98f701a0f079103b46d8"
    const val OUTPASSES_OBJECT_ID = "ff808181a09d98f701a0f07938af46d9"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    private var isSyncing = false
    private val knownNotifiedPassIds = mutableSetOf<String>()

    fun markPassAsLocalCreated(passId: String, context: Context? = null) {
        val keys = listOf(
            passId,
            "STAFF_$passId",
            "HOD_$passId",
            "HOD_STAFF_APPROVED_$passId",
            "STUDENT_APPROVED_$passId",
            "STUDENT_REJECTED_$passId"
        )
        knownNotifiedPassIds.addAll(keys)
        context?.let { persistNotifiedIds(it) }
    }

    fun startSync(context: Context, repository: OutpassRepository) {
        val appContext = context.applicationContext

        // Restore known notified passes from SharedPreferences so restarts don't spam
        try {
            val prefs = appContext.getSharedPreferences("outpass_notified_cache", Context.MODE_PRIVATE)
            val saved = prefs.getStringSet("notified_ids", emptySet())
            if (!saved.isNullOrEmpty()) {
                knownNotifiedPassIds.addAll(saved)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load notified cache: ${e.message}")
        }

        if (syncJob?.isActive == true) return

        syncJob = syncScope.launch {
            // Immediate sync on launch
            syncFromCloud(appContext, repository)

            // Fast continuous polling every 3.5 seconds for real-time notifications across devices
            while (isActive) {
                delay(3500)
                try {
                    syncFromCloud(appContext, repository)
                } catch (e: Exception) {
                    Log.w(TAG, "Background sync iteration failed: ${e.message}")
                }
            }
        }
    }

    suspend fun syncNow(context: Context, repository: OutpassRepository) {
        withContext(Dispatchers.IO) {
            syncFromCloud(context.applicationContext, repository)
        }
    }

    fun checkAndNotifyPendingForLoggedInUser(context: Context, repository: OutpassRepository, user: User) {
        if (user.role != UserRole.STAFF_ADVISOR && user.role != UserRole.HOD) return
        val currentPasses = repository.outpasses.value

        // Staff Advisor receives notifications for requests awaiting staff approval
        if (user.role == UserRole.STAFF_ADVISOR) {
            for (pass in currentPasses) {
                if (pass.status == OutpassStatus.PENDING_STAFF) {
                    val key = "STAFF_${pass.id}"
                    if (!knownNotifiedPassIds.contains(key)) {
                        knownNotifiedPassIds.add(key)
                        persistNotifiedIds(context)
                        val studentUser = repository.findUserByIdentifier(pass.regNo)
                            ?: repository.findUserByIdentifier(pass.studentId)
                        Log.i(TAG, "Dispatching notification to Staff for pass ${pass.id}")
                        OutpassNotificationHelper.notifyStaffOnNewRequest(context, pass, studentUser)
                    }
                }
            }
        }

        // HOD receives notifications when outpass has been approved by staff: "The student outpass is approved by the staff"
        if (user.role == UserRole.HOD) {
            for (pass in currentPasses) {
                if (pass.status == OutpassStatus.PENDING_HOD) {
                    val key = "HOD_STAFF_APPROVED_${pass.id}"
                    if (!knownNotifiedPassIds.contains(key)) {
                        knownNotifiedPassIds.add(key)
                        persistNotifiedIds(context)
                        val studentUser = repository.findUserByIdentifier(pass.regNo)
                            ?: repository.findUserByIdentifier(pass.studentId)
                        Log.i(TAG, "Dispatching staff-approved notification to HOD for pass ${pass.id}")
                        OutpassNotificationHelper.notifyHodOnStaffApproval(context, pass, studentUser)
                    }
                }
            }
        }
    }

    private suspend fun syncFromCloud(context: Context, repository: OutpassRepository) {
        if (isSyncing) return
        isSyncing = true
        try {
            syncUsers(context, repository)
            syncOutpasses(context, repository)
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync: ${e.message}")
        } finally {
            isSyncing = false
        }
    }

    private fun persistNotifiedIds(context: Context) {
        try {
            val prefs = context.getSharedPreferences("outpass_notified_cache", Context.MODE_PRIVATE)
            prefs.edit().putStringSet("notified_ids", HashSet(knownNotifiedPassIds)).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist notified IDs: ${e.message}")
        }
    }

    private fun syncUsers(context: Context, repository: OutpassRepository) {
        val cloudUsers = fetchUsersFromCloud(context)
        if (cloudUsers.isEmpty()) {
            if (repository.users.value.isNotEmpty()) {
                pushUsersToCloud(context, repository.users.value)
            }
            return
        }

        val localUsers = repository.users.value.toMutableList()
        var changed = false

        for (cu in cloudUsers) {
            val index = localUsers.indexOfFirst {
                it.id == cu.id ||
                it.email.equals(cu.email, ignoreCase = true) ||
                (it.regNo.isNotBlank() && it.regNo.equals(cu.regNo, ignoreCase = true))
            }

            // Materialize avatar image locally so Coil renders it seamlessly on this device
            val localAvatar = ImageCropUtil.ensureLocalAvatarFile(context, cu.photoUri, cu.id)
            val processedUser = if (localAvatar != null) cu.copy(photoUri = localAvatar) else cu

            if (index == -1) {
                localUsers.add(processedUser)
                changed = true
            } else {
                val existing = localUsers[index]
                val shouldUpdate = existing.password != processedUser.password ||
                                   (processedUser.photoUri != null && processedUser.photoUri != existing.photoUri) ||
                                   (processedUser.lastPasswordResetAt ?: 0L) > (existing.lastPasswordResetAt ?: 0L)
                if (shouldUpdate) {
                    localUsers[index] = processedUser
                    changed = true
                }
            }
        }

        if (changed) {
            repository.setUsersFromSync(localUsers)
        }

        // If local has users not in cloud (e.g. newly registered), push to cloud
        val missingInCloud = localUsers.any { local ->
            cloudUsers.none { it.id == local.id || it.email.equals(local.email, ignoreCase = true) }
        }
        if (missingInCloud) {
            pushUsersToCloud(context, localUsers)
        }
    }

    private fun syncOutpasses(context: Context, repository: OutpassRepository) {
        val cloudPasses = fetchOutpassesFromCloud(context)
        if (cloudPasses.isEmpty()) {
            if (repository.outpasses.value.isNotEmpty()) {
                pushOutpassesToCloud(repository.outpasses.value)
            }
            return
        }

        val currentLocalPasses = repository.outpasses.value.toMutableList()
        var localChanged = false
        val currentUser = repository.currentUser.value
        val userRole = currentUser?.role

        for (cp in cloudPasses) {
            // Materialize student avatar locally
            val localAvatar = ImageCropUtil.ensureLocalAvatarFile(context, cp.studentPhotoUri, cp.id)
            val processedPass = if (localAvatar != null) cp.copy(studentPhotoUri = localAvatar) else cp

            val localIndex = currentLocalPasses.indexOfFirst { it.id.equals(processedPass.id, ignoreCase = true) }

            if (localIndex == -1) {
                // Brand new outpass created on another device
                currentLocalPasses.add(0, processedPass)
                localChanged = true

                val studentUser = repository.findUserByIdentifier(processedPass.regNo)
                    ?: repository.findUserByIdentifier(processedPass.studentId)
                    ?: User(
                        id = processedPass.studentId,
                        name = processedPass.studentName,
                        email = "${processedPass.regNo.lowercase()}@vetias.ac.in",
                        role = UserRole.STUDENT,
                        regNo = processedPass.regNo,
                        department = processedPass.department,
                        phone = processedPass.studentPhone,
                        parentPhone = processedPass.parentPhone
                    )

                // CRITICAL NOTIFICATION ROUTING RULE:
                // When a student requests an outpass on the student's mobile phone:
                // - The request notification is sent ONLY to the Staff Advisor mobile device!
                // - Do NOT show the app notification in the same student mobile!
                // - Do NOT show the notification on HOD mobile yet (HOD gets notified only after Staff approves)!
                if (userRole == UserRole.STAFF_ADVISOR) {
                    val key = "STAFF_${processedPass.id}"
                    if (!knownNotifiedPassIds.contains(key)) {
                        knownNotifiedPassIds.add(key)
                        persistNotifiedIds(context)
                        Log.i(TAG, "Dispatching Staff outpass notification: student=${processedPass.studentName}, reason=${processedPass.reason}")
                        OutpassNotificationHelper.notifyStaffOnNewRequest(context, processedPass, studentUser)
                    }
                }
            } else {
                // Outpass already exists locally; check for status updates from other devices
                val local = currentLocalPasses[localIndex]
                val statusChanged = local.status != processedPass.status
                val qrUsedChanged = local.isQrUsed != processedPass.isQrUsed

                if (statusChanged || qrUsedChanged || local.staffApproval != processedPass.staffApproval || local.hodApproval != processedPass.hodApproval) {
                    currentLocalPasses[localIndex] = processedPass
                    localChanged = true

                    // If staff approved and this device is HOD:
                    if (statusChanged && processedPass.status == OutpassStatus.PENDING_HOD && userRole == UserRole.HOD) {
                        val key = "HOD_STAFF_APPROVED_${processedPass.id}"
                        if (!knownNotifiedPassIds.contains(key)) {
                            knownNotifiedPassIds.add(key)
                            persistNotifiedIds(context)
                            val studentUser = repository.findUserByIdentifier(processedPass.regNo)
                            OutpassNotificationHelper.notifyHodOnStaffApproval(context, processedPass, studentUser)
                        }
                    }

                    // If outpass was APPROVED and this device is the student:
                    if (statusChanged && processedPass.status == OutpassStatus.APPROVED && userRole == UserRole.STUDENT) {
                        val isForThisStudent = currentUser.id == processedPass.studentId || currentUser.regNo.equals(processedPass.regNo, ignoreCase = true)
                        if (isForThisStudent) {
                            val key = "STUDENT_APPROVED_${processedPass.id}"
                            if (!knownNotifiedPassIds.contains(key)) {
                                knownNotifiedPassIds.add(key)
                                persistNotifiedIds(context)
                                OutpassNotificationHelper.notifyStudentOnApproval(context, processedPass)
                            }
                        }
                    }

                    // If outpass was REJECTED and this device is the student:
                    if (statusChanged && processedPass.status == OutpassStatus.REJECTED && userRole == UserRole.STUDENT) {
                        val isForThisStudent = currentUser.id == processedPass.studentId || currentUser.regNo.equals(processedPass.regNo, ignoreCase = true)
                        if (isForThisStudent) {
                            val key = "STUDENT_REJECTED_${processedPass.id}"
                            if (!knownNotifiedPassIds.contains(key)) {
                                knownNotifiedPassIds.add(key)
                                persistNotifiedIds(context)
                                OutpassNotificationHelper.notifyStudentOnRejection(context, processedPass, processedPass.rejectionReason ?: "Request rejected")
                            }
                        }
                    }
                }
            }
        }

        if (localChanged) {
            repository.setOutpassesFromSync(currentLocalPasses)
        }

        // Check if there are local outpasses created on this device missing in cloud
        val missingInCloud = currentLocalPasses.any { local ->
            cloudPasses.none { it.id.equals(local.id, ignoreCase = true) }
        }
        if (missingInCloud) {
            pushOutpassesToCloud(currentLocalPasses)
        }
    }

    fun fetchUsersFromCloud(context: Context? = null): List<User> {
        val oldPolicy = StrictMode.getThreadPolicy()
        return try {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
            val request = Request.Builder()
                .url("$BASE_URL/$USERS_OBJECT_ID")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: return emptyList()
                val usersArray = data.optJSONArray("users") ?: return emptyList()

                val users = mutableListOf<User>()
                for (i in 0 until usersArray.length()) {
                    val obj = usersArray.getJSONObject(i)
                    val role = try {
                        UserRole.valueOf(obj.getString("role"))
                    } catch (e: Exception) {
                        UserRole.STUDENT
                    }

                    val rawPhoto = obj.optString("photoUri", "").takeIf { it.isNotBlank() }
                    val finalPhoto = if (context != null && rawPhoto != null) {
                        ImageCropUtil.ensureLocalAvatarFile(context, rawPhoto, obj.getString("id"))
                    } else {
                        rawPhoto
                    }

                    users.add(
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
                            photoUri = finalPhoto,
                            password = obj.optString("password", "Pass@1234"),
                            lastPasswordResetAt = if (obj.has("lastPasswordResetAt")) obj.getLong("lastPasswordResetAt") else null
                        )
                    )
                }
                users
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchUsersFromCloud exception: ${e.message}")
            emptyList()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }

    fun pushUsersToCloud(context: Context, users: List<User>) {
        syncScope.launch {
            try {
                val array = JSONArray()
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
                    obj.put("password", u.password)
                    if (u.lastPasswordResetAt != null) {
                        obj.put("lastPasswordResetAt", u.lastPasswordResetAt)
                    }

                    // Convert local photo file to compact Base64 so it can be viewed on ANY mobile device!
                    val compactPhoto = ImageCropUtil.convertToCompactBase64(context, u.photoUri)
                        ?: u.photoUri
                    if (!compactPhoto.isNullOrBlank()) {
                        obj.put("photoUri", compactPhoto)
                    }

                    array.put(obj)
                }

                val payload = JSONObject().apply {
                    put("name", "vetias_outpass_users_v1")
                    put("data", JSONObject().put("users", array))
                }

                val request = Request.Builder()
                    .url("$BASE_URL/$USERS_OBJECT_ID")
                    .put(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(request).execute().close()
                Log.d(TAG, "Successfully pushed ${users.size} users to cloud")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push users to cloud: ${e.message}")
            }
        }
    }

    fun fetchOutpassesFromCloud(context: Context? = null): List<Outpass> {
        val oldPolicy = StrictMode.getThreadPolicy()
        return try {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
            val request = Request.Builder()
                .url("$BASE_URL/$OUTPASSES_OBJECT_ID")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: return emptyList()
                val array = data.optJSONArray("outpasses") ?: return emptyList()

                val passes = mutableListOf<Outpass>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val status = try {
                        OutpassStatus.valueOf(obj.getString("status"))
                    } catch (e: Exception) {
                        OutpassStatus.PENDING_STAFF
                    }
                    val type = try {
                        OutpassType.valueOf(obj.getString("type"))
                    } catch (e: Exception) {
                        OutpassType.LOCAL
                    }

                    val staffApproval = if (obj.has("staffApproval") && !obj.isNull("staffApproval")) {
                        val sObj = obj.getJSONObject("staffApproval")
                        val sRole = try { UserRole.valueOf(sObj.getString("approverRole")) } catch (e: Exception) { UserRole.STAFF_ADVISOR }
                        val sStatus = if (sObj.has("status")) sObj.getString("status") else sObj.optString("decision", "APPROVED")
                        ApprovalRecord(
                            approverRole = sRole,
                            approverName = sObj.getString("approverName"),
                            status = sStatus,
                            timestamp = sObj.optLong("timestamp", System.currentTimeMillis()),
                            remarks = sObj.optString("remarks", "")
                        )
                    } else null

                    val hodApproval = if (obj.has("hodApproval") && !obj.isNull("hodApproval")) {
                        val hObj = obj.getJSONObject("hodApproval")
                        val hRole = try { UserRole.valueOf(hObj.getString("approverRole")) } catch (e: Exception) { UserRole.HOD }
                        val hStatus = if (hObj.has("status")) hObj.getString("status") else hObj.optString("decision", "APPROVED")
                        ApprovalRecord(
                            approverRole = hRole,
                            approverName = hObj.getString("approverName"),
                            status = hStatus,
                            timestamp = hObj.optLong("timestamp", System.currentTimeMillis()),
                            remarks = hObj.optString("remarks", "")
                        )
                    } else null

                    val rawStudentPhoto = obj.optString("studentPhotoUri", "").takeIf { it.isNotBlank() }
                    val finalStudentPhoto = if (context != null && rawStudentPhoto != null) {
                        ImageCropUtil.ensureLocalAvatarFile(context, rawStudentPhoto, obj.getString("id"))
                    } else {
                        rawStudentPhoto
                    }

                    passes.add(
                        Outpass(
                            id = obj.getString("id"),
                            studentId = obj.getString("studentId"),
                            studentName = obj.getString("studentName"),
                            regNo = obj.getString("regNo"),
                            department = obj.optString("department", "Computer Science"),
                            hostelBlock = obj.optString("hostelBlock", "Block A"),
                            roomNo = obj.optString("roomNo", "101"),
                            parentPhone = obj.optString("parentPhone", ""),
                            studentPhone = obj.optString("studentPhone", ""),
                            type = type,
                            reason = obj.getString("reason"),
                            destination = obj.getString("destination"),
                            outDateTime = obj.getLong("outDateTime"),
                            returnDateTime = obj.getLong("returnDateTime"),
                            status = status,
                            qrToken = obj.optString("qrToken", obj.getString("id")),
                            appliedAt = obj.optLong("appliedAt", System.currentTimeMillis()),
                            staffApproval = staffApproval,
                            hodApproval = hodApproval,
                            actualCheckOutTime = if (obj.has("actualCheckOutTime")) obj.getLong("actualCheckOutTime") else null,
                            actualCheckInTime = if (obj.has("actualCheckInTime")) obj.getLong("actualCheckInTime") else null,
                            rejectionReason = if (obj.has("rejectionReason")) obj.getString("rejectionReason") else null,
                            isQrUsed = obj.optBoolean("isQrUsed", false),
                            parentNotified = obj.optBoolean("parentNotified", true),
                            studentPhotoUri = finalStudentPhoto
                        )
                    )
                }
                passes
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchOutpassesFromCloud exception: ${e.message}")
            emptyList()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }

    fun pushOutpassesToCloud(passes: List<Outpass>) {
        syncScope.launch {
            try {
                val array = JSONArray()
                for (p in passes) {
                    val obj = JSONObject()
                    obj.put("id", p.id)
                    obj.put("studentId", p.studentId)
                    obj.put("studentName", p.studentName)
                    obj.put("regNo", p.regNo)
                    obj.put("department", p.department)
                    obj.put("hostelBlock", p.hostelBlock)
                    obj.put("roomNo", p.roomNo)
                    obj.put("parentPhone", p.parentPhone)
                    obj.put("studentPhone", p.studentPhone)
                    obj.put("type", p.type.name)
                    obj.put("reason", p.reason)
                    obj.put("destination", p.destination)
                    obj.put("outDateTime", p.outDateTime)
                    obj.put("returnDateTime", p.returnDateTime)
                    obj.put("status", p.status.name)
                    obj.put("qrToken", p.qrToken)
                    obj.put("appliedAt", p.appliedAt)
                    obj.put("isQrUsed", p.isQrUsed)
                    obj.put("parentNotified", p.parentNotified)
                    if (p.studentPhotoUri != null) obj.put("studentPhotoUri", p.studentPhotoUri)
                    if (p.actualCheckOutTime != null) obj.put("actualCheckOutTime", p.actualCheckOutTime)
                    if (p.actualCheckInTime != null) obj.put("actualCheckInTime", p.actualCheckInTime)
                    if (p.rejectionReason != null) obj.put("rejectionReason", p.rejectionReason)

                    val sApp = p.staffApproval
                    if (sApp != null) {
                        obj.put("staffApproval", JSONObject().apply {
                            put("approverRole", sApp.approverRole.name)
                            put("approverName", sApp.approverName)
                            put("decision", sApp.status)
                            put("status", sApp.status)
                            put("timestamp", sApp.timestamp)
                            put("remarks", sApp.remarks)
                        })
                    }

                    val hApp = p.hodApproval
                    if (hApp != null) {
                        obj.put("hodApproval", JSONObject().apply {
                            put("approverRole", hApp.approverRole.name)
                            put("approverName", hApp.approverName)
                            put("decision", hApp.status)
                            put("status", hApp.status)
                            put("timestamp", hApp.timestamp)
                            put("remarks", hApp.remarks)
                        })
                    }

                    array.put(obj)
                }

                val payload = JSONObject().apply {
                    put("name", "vetias_outpass_requests_v1")
                    put("data", JSONObject().put("outpasses", array))
                }

                val request = Request.Builder()
                    .url("$BASE_URL/$OUTPASSES_OBJECT_ID")
                    .put(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(request).execute().close()
                Log.d(TAG, "Successfully pushed ${passes.size} outpasses to cloud")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push outpasses to cloud: ${e.message}")
            }
        }
    }
}
