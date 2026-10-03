package com.example.data.sync

import android.content.Context
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

        // Add currently existing local passes to known passes
        repository.outpasses.value.forEach {
            knownNotifiedPassIds.add(it.id)
        }

        if (syncJob?.isActive == true) return

        syncJob = syncScope.launch {
            // Immediate sync on launch
            syncFromCloud(appContext, repository)

            // Fast continuous polling every 4 seconds for real-time notifications
            while (isActive) {
                delay(4000)
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
            Log.w(TAG, "Failed to persist notified cache: ${e.message}")
        }
    }

    private fun syncUsers(context: Context, repository: OutpassRepository) {
        val cloudUsers = fetchUsersFromCloud()
        if (cloudUsers.isEmpty()) {
            if (repository.users.value.isNotEmpty()) {
                pushUsersToCloud(context, repository.users.value)
            }
            return
        }

        val localUsers = repository.users.value.toMutableList()
        var changed = false

        for (cu in cloudUsers) {
            // Process cross-device photo: if photoUri is Base64, save it to disk on this device
            var resolvedPhotoUri = cu.photoUri
            if (!resolvedPhotoUri.isNullOrBlank() && (resolvedPhotoUri.startsWith("data:image/") || resolvedPhotoUri.startsWith("/9j/"))) {
                val localSaved = ImageCropUtil.saveBase64ToDisk(context, resolvedPhotoUri, "user_${cu.id}")
                if (localSaved != null) {
                    resolvedPhotoUri = localSaved
                }
            }

            val processedUser = cu.copy(photoUri = resolvedPhotoUri)
            val index = localUsers.indexOfFirst {
                it.id == processedUser.id ||
                it.email.equals(processedUser.email, ignoreCase = true) ||
                (processedUser.regNo.isNotBlank() && it.regNo.equals(processedUser.regNo, ignoreCase = true))
            }

            if (index == -1) {
                localUsers.add(processedUser)
                changed = true
            } else {
                val existing = localUsers[index]
                // Keep the freshest password or photo
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
        val cloudPasses = fetchOutpassesFromCloud()
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
            val localIndex = currentLocalPasses.indexOfFirst { it.id.equals(cp.id, ignoreCase = true) }

            if (localIndex == -1) {
                // Brand new outpass created on another device!
                currentLocalPasses.add(0, cp)
                localChanged = true

                // Check if this notification should fire on this device
                if (!knownNotifiedPassIds.contains(cp.id)) {
                    knownNotifiedPassIds.add(cp.id)
                    persistNotifiedIds(context)

                    val studentUser = repository.findUserByIdentifier(cp.regNo)
                        ?: repository.findUserByIdentifier(cp.studentId)
                        ?: User(
                            id = cp.studentId,
                            name = cp.studentName,
                            email = "${cp.regNo.lowercase()}@vetias.ac.in",
                            role = UserRole.STUDENT,
                            regNo = cp.regNo,
                            department = cp.department,
                            studentPhone = cp.studentPhone,
                            parentPhone = cp.parentPhone
                        )

                    // User requested: "the notification is only send to the staff and hod module"
                    // If this device is logged in as Staff Advisor or HOD (or no user logged in yet):
                    if (userRole == UserRole.STAFF_ADVISOR || userRole == UserRole.HOD || currentUser == null) {
                        Log.i(TAG, "Dispatching Staff/HOD outpass notification: student=${cp.studentName}, reason=${cp.reason}")
                        OutpassNotificationHelper.notifyStaffAndHodOnNewRequest(context, cp, studentUser)
                    }
                }
            } else {
                // Outpass already exists locally; check for status updates from other devices
                val local = currentLocalPasses[localIndex]
                val statusChanged = local.status != cp.status
                val qrUsedChanged = local.isQrUsed != cp.isQrUsed

                if (statusChanged || qrUsedChanged || local.actualCheckOutTime != cp.actualCheckOutTime) {
                    currentLocalPasses[localIndex] = cp
                    localChanged = true
                    Log.d(TAG, "Pass ${cp.id} updated from cloud: ${local.status} -> ${cp.status}")

                    // Check for status transition notifications
                    if (statusChanged) {
                        val isForThisStudent = currentUser?.id == cp.studentId ||
                                currentUser?.regNo.equals(cp.regNo, ignoreCase = true)

                        if (cp.status == OutpassStatus.PENDING_HOD && userRole == UserRole.HOD) {
                            // Staff approved, now notify HOD on HOD's device
                            val student = repository.findUserByIdentifier(cp.regNo)
                            OutpassNotificationHelper.notifyHodOnStaffApproval(context, cp, student)
                        } else if (cp.status == OutpassStatus.APPROVED && isForThisStudent) {
                            // HOD approved, notify Student on student's device
                            OutpassNotificationHelper.notifyStudentOnApproval(context, cp)
                        } else if (cp.status == OutpassStatus.REJECTED && isForThisStudent) {
                            // Outpass rejected, notify Student
                            OutpassNotificationHelper.notifyStudentOnRejection(context, cp, cp.rejectionReason ?: "Request rejected")
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

    fun fetchUsersFromCloud(): List<User> {
        return try {
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
                            photoUri = obj.optString("photoUri", "").takeIf { it.isNotBlank() },
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

                    // Convert local photo file to Base64 so it can be viewed on other devices!
                    val photo = u.photoUri
                    if (!photo.isNullOrBlank()) {
                        if (photo.startsWith("data:image/") || photo.startsWith("http")) {
                            obj.put("photoUri", photo)
                        } else if (photo.startsWith("file://") || photo.startsWith("/")) {
                            val path = photo.removePrefix("file://")
                            val file = File(path)
                            if (file.exists() && file.length() < 300_000) {
                                try {
                                    val bytes = file.readBytes()
                                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                    obj.put("photoUri", "data:image/jpeg;base64,$b64")
                                } catch (e: Exception) {
                                    obj.put("photoUri", photo)
                                }
                            } else {
                                obj.put("photoUri", photo)
                            }
                        } else {
                            obj.put("photoUri", photo)
                        }
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

    fun fetchOutpassesFromCloud(): List<Outpass> {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/$OUTPASSES_OBJECT_ID")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: return emptyList()
                val passesArray = data.optJSONArray("outpasses") ?: return emptyList()

                val passes = mutableListOf<Outpass>()
                for (i in 0 until passesArray.length()) {
                    val obj = passesArray.getJSONObject(i)
                    if (!obj.has("studentName") || !obj.has("type")) continue

                    val type = try {
                        OutpassType.valueOf(obj.getString("type"))
                    } catch (e: Exception) {
                        OutpassType.LOCAL
                    }

                    val status = try {
                        OutpassStatus.valueOf(obj.getString("status"))
                    } catch (e: Exception) {
                        OutpassStatus.PENDING_STAFF
                    }

                    val staffApproval = if (obj.has("staffApproval") && !obj.isNull("staffApproval")) {
                        val sObj = obj.getJSONObject("staffApproval")
                        val sRole = try { UserRole.valueOf(sObj.getString("approverRole")) } catch (e: Exception) { UserRole.STAFF_ADVISOR }
                        ApprovalRecord(
                            approverRole = sRole,
                            approverName = sObj.getString("approverName"),
                            decision = sObj.getString("decision"),
                            timestamp = sObj.optLong("timestamp", System.currentTimeMillis()),
                            remarks = sObj.optString("remarks", "")
                        )
                    } else null

                    val hodApproval = if (obj.has("hodApproval") && !obj.isNull("hodApproval")) {
                        val hObj = obj.getJSONObject("hodApproval")
                        val hRole = try { UserRole.valueOf(hObj.getString("approverRole")) } catch (e: Exception) { UserRole.HOD }
                        ApprovalRecord(
                            approverRole = hRole,
                            approverName = hObj.getString("approverName"),
                            decision = hObj.getString("decision"),
                            timestamp = hObj.optLong("timestamp", System.currentTimeMillis()),
                            remarks = hObj.optString("remarks", "")
                        )
                    } else null

                    passes.add(
                        Outpass(
                            id = obj.getString("id"),
                            studentId = obj.getString("studentId"),
                            studentName = obj.getString("studentName"),
                            regNo = obj.getString("regNo"),
                            department = obj.getString("department"),
                            hostelBlock = obj.optString("hostelBlock", ""),
                            roomNo = obj.optString("roomNo", ""),
                            studentPhone = obj.optString("studentPhone", ""),
                            parentPhone = obj.optString("parentPhone", ""),
                            type = type,
                            destination = obj.getString("destination"),
                            reason = obj.getString("reason"),
                            outDateTime = obj.getLong("outDateTime"),
                            returnDateTime = obj.getLong("returnDateTime"),
                            status = status,
                            qrToken = obj.optString("qrToken", ""),
                            appliedAt = obj.optLong("appliedAt", System.currentTimeMillis()),
                            staffApproval = staffApproval,
                            hodApproval = hodApproval,
                            actualCheckOutTime = if (obj.has("actualCheckOutTime") && !obj.isNull("actualCheckOutTime")) obj.getLong("actualCheckOutTime") else null,
                            actualCheckInTime = if (obj.has("actualCheckInTime") && !obj.isNull("actualCheckInTime")) obj.getLong("actualCheckInTime") else null,
                            rejectionReason = if (obj.has("rejectionReason") && !obj.isNull("rejectionReason")) obj.getString("rejectionReason") else null,
                            parentNotified = obj.optBoolean("parentNotified", false),
                            isQrUsed = obj.optBoolean("isQrUsed", false),
                            studentPhotoUri = obj.optString("studentPhotoUri", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
                passes
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchOutpassesFromCloud exception: ${e.message}")
            emptyList()
        }
    }

    fun pushOutpassesToCloud(outpasses: List<Outpass>) {
        syncScope.launch {
            try {
                val array = JSONArray()
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
                    obj.put("parentNotified", p.parentNotified)
                    obj.put("isQrUsed", p.isQrUsed)
                    if (p.studentPhotoUri != null) obj.put("studentPhotoUri", p.studentPhotoUri)
                    if (p.actualCheckOutTime != null) obj.put("actualCheckOutTime", p.actualCheckOutTime)
                    if (p.actualCheckInTime != null) obj.put("actualCheckInTime", p.actualCheckInTime)
                    if (p.rejectionReason != null) obj.put("rejectionReason", p.rejectionReason)

                    if (p.staffApproval != null) {
                        obj.put("staffApproval", JSONObject().apply {
                            put("approverRole", p.staffApproval.approverRole.name)
                            put("approverName", p.staffApproval.approverName)
                            put("decision", p.staffApproval.decision)
                            put("timestamp", p.staffApproval.timestamp)
                            put("remarks", p.staffApproval.remarks)
                        })
                    }

                    if (p.hodApproval != null) {
                        obj.put("hodApproval", JSONObject().apply {
                            put("approverRole", p.hodApproval.approverRole.name)
                            put("approverName", p.hodApproval.approverName)
                            put("decision", p.hodApproval.decision)
                            put("timestamp", p.hodApproval.timestamp)
                            put("remarks", p.hodApproval.remarks)
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
                Log.d(TAG, "Successfully pushed ${outpasses.size} outpasses to cloud")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push outpasses to cloud: ${e.message}")
            }
        }
    }
}
