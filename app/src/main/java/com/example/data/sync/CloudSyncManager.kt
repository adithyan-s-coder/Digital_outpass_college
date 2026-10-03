package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    private var isSyncing = false
    private val knownNotifiedPassIds = mutableSetOf<String>()

    @Volatile
    private var isHostReachable = true

    @Volatile
    private var consecutiveFailures = 0

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNet = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    private fun onNetworkFailure(e: Throwable, operation: String) {
        isHostReachable = false
        consecutiveFailures++
        if (e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.net.SocketTimeoutException) {
            Log.d(TAG, "Cloud sync offline ($operation): ${e.message}")
        } else {
            Log.w(TAG, "Cloud sync note ($operation): ${e.message}")
        }
    }

    private fun onNetworkSuccess() {
        isHostReachable = true
        consecutiveFailures = 0
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

        // On initial install / launch, mark existing seed passes so they don't fire notifications unexpectedly
        val prefs = appContext.getSharedPreferences("outpass_notified_cache", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("seed_initialized", false)) {
            repository.outpasses.value.forEach { pass ->
                knownNotifiedPassIds.add("staff_req_${pass.id}")
                knownNotifiedPassIds.add("hod_approved_staff_${pass.id}")
                knownNotifiedPassIds.add("student_approved_${pass.id}")
                knownNotifiedPassIds.add("student_rejected_${pass.id}")
            }
            prefs.edit().putBoolean("seed_initialized", true).apply()
            persistNotifiedIds(appContext)
        }

        if (syncJob?.isActive == true) return

        syncJob = syncScope.launch {
            if (isNetworkAvailable(appContext)) {
                syncFromCloud(appContext, repository)
            }

            while (isActive) {
                val delayMs = if (!isHostReachable || consecutiveFailures > 0) {
                    val exp = (1L shl consecutiveFailures.coerceAtMost(5))
                    (10000L * exp).coerceIn(20000L, 120000L)
                } else {
                    6000L
                }
                delay(delayMs)

                if (isNetworkAvailable(appContext)) {
                    try {
                        syncFromCloud(appContext, repository)
                    } catch (e: Exception) {
                        onNetworkFailure(e, "background loop")
                    }
                }
            }
        }
    }

    suspend fun syncNow(context: Context, repository: OutpassRepository) {
        val appContext = context.applicationContext
        if (!isNetworkAvailable(appContext)) return
        withContext(Dispatchers.IO) {
            syncFromCloud(appContext, repository)
        }
    }

    private suspend fun syncFromCloud(context: Context, repository: OutpassRepository) {
        if (!isNetworkAvailable(context)) return
        if (isSyncing) return
        isSyncing = true
        try {
            syncUsers(context, repository)
            syncOutpasses(context, repository)
        } catch (e: Exception) {
            onNetworkFailure(e, "syncFromCloud")
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

    fun checkAndDispatchRoleNotifications(context: Context, repository: OutpassRepository) {
        val currentUser = repository.currentUser.value ?: return
        val userRole = currentUser.role
        val passes = repository.outpasses.value

        for (pass in passes) {
            when (userRole) {
                UserRole.STAFF_ADVISOR -> {
                    if (pass.status == OutpassStatus.PENDING_STAFF) {
                        val matchesDept = currentUser.department.isBlank() ||
                                currentUser.department.equals(pass.department, ignoreCase = true)
                        val key = "staff_req_${pass.id}"
                        if (matchesDept && !knownNotifiedPassIds.contains(key)) {
                            knownNotifiedPassIds.add(key)
                            persistNotifiedIds(context)
                            val studentUser = repository.findUserByIdentifier(pass.regNo)
                                ?: repository.findUserByIdentifier(pass.studentId)
                            Log.i(TAG, "Dispatching notification to Staff: student=${pass.studentName}")
                            OutpassNotificationHelper.notifyStaffOnNewRequest(context, pass, studentUser)
                        }
                    }
                }
                UserRole.HOD -> {
                    if (pass.status == OutpassStatus.PENDING_HOD) {
                        val matchesDept = currentUser.department.isBlank() ||
                                currentUser.department.equals(pass.department, ignoreCase = true)
                        val key = "hod_approved_staff_${pass.id}"
                        if (matchesDept && !knownNotifiedPassIds.contains(key)) {
                            knownNotifiedPassIds.add(key)
                            persistNotifiedIds(context)
                            val studentUser = repository.findUserByIdentifier(pass.regNo)
                                ?: repository.findUserByIdentifier(pass.studentId)
                            Log.i(TAG, "Dispatching notification to HOD for pass ${pass.id}")
                            OutpassNotificationHelper.notifyHodOnStaffApproval(context, pass, studentUser)
                        }
                    }
                }
                UserRole.STUDENT -> {
                    val isForThisStudent = currentUser.id == pass.studentId ||
                            currentUser.regNo.equals(pass.regNo, ignoreCase = true)
                    if (isForThisStudent) {
                        if (pass.status == OutpassStatus.APPROVED) {
                            val key = "student_approved_${pass.id}"
                            if (!knownNotifiedPassIds.contains(key)) {
                                knownNotifiedPassIds.add(key)
                                persistNotifiedIds(context)
                                Log.i(TAG, "Dispatching approval notification to Student for pass ${pass.id}")
                                OutpassNotificationHelper.notifyStudentOnApproval(context, pass)
                            }
                        } else if (pass.status == OutpassStatus.REJECTED) {
                            val key = "student_rejected_${pass.id}"
                            if (!knownNotifiedPassIds.contains(key)) {
                                knownNotifiedPassIds.add(key)
                                persistNotifiedIds(context)
                                Log.i(TAG, "Dispatching rejection notification to Student for pass ${pass.id}")
                                OutpassNotificationHelper.notifyStudentOnRejection(
                                    context,
                                    pass,
                                    pass.rejectionReason ?: "Request rejected"
                                )
                            }
                        }
                    }
                }
                else -> {
                    // Other roles
                }
            }
        }
    }

    private fun syncUsers(context: Context, repository: OutpassRepository) {
        val result = fetchUsersFromCloudResult()
        if (result.isFailure) {
            return
        }
        val cloudUsers = result.getOrNull() ?: return
        if (cloudUsers.isEmpty()) {
            if (repository.users.value.isNotEmpty() && isHostReachable) {
                pushUsersToCloud(context, repository.users.value)
            }
            return
        }

        val localUsers = repository.users.value.toMutableList()
        var changed = false

        for (cu in cloudUsers) {
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

        val missingInCloud = localUsers.any { local ->
            cloudUsers.none { it.id == local.id || it.email.equals(local.email, ignoreCase = true) }
        }
        if (missingInCloud && isHostReachable) {
            pushUsersToCloud(context, localUsers)
        }
    }

    private fun syncOutpasses(context: Context, repository: OutpassRepository) {
        val result = fetchOutpassesFromCloudResult()
        if (result.isFailure) {
            return
        }
        val cloudPasses = result.getOrNull() ?: return
        if (cloudPasses.isEmpty()) {
            if (repository.outpasses.value.isNotEmpty() && isHostReachable) {
                pushOutpassesToCloud(repository.outpasses.value)
            }
            return
        }

        val currentLocalPasses = repository.outpasses.value.toMutableList()
        var localChanged = false

        for (cp in cloudPasses) {
            val localIndex = currentLocalPasses.indexOfFirst { it.id.equals(cp.id, ignoreCase = true) }

            if (localIndex == -1) {
                currentLocalPasses.add(0, cp)
                localChanged = true
            } else {
                val local = currentLocalPasses[localIndex]
                val statusChanged = local.status != cp.status
                val qrUsedChanged = local.isQrUsed != cp.isQrUsed

                if (statusChanged || qrUsedChanged || local.actualCheckOutTime != cp.actualCheckOutTime) {
                    currentLocalPasses[localIndex] = cp
                    localChanged = true
                    Log.d(TAG, "Pass ${cp.id} updated from cloud: ${local.status} -> ${cp.status}")
                }
            }
        }

        if (localChanged) {
            repository.setOutpassesFromSync(currentLocalPasses)
        }

        checkAndDispatchRoleNotifications(context, repository)

        val missingInCloud = currentLocalPasses.any { local ->
            cloudPasses.none { it.id.equals(local.id, ignoreCase = true) }
        }
        if (missingInCloud && isHostReachable) {
            pushOutpassesToCloud(currentLocalPasses)
        }
    }

    private fun fetchUsersFromCloudResult(): Result<List<User>> {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/$USERS_OBJECT_ID")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return Result.success(emptyList())
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: return Result.success(emptyList())
                val usersArray = data.optJSONArray("users") ?: return Result.success(emptyList())

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
                onNetworkSuccess()
                Result.success(users)
            }
        } catch (e: Exception) {
            onNetworkFailure(e, "fetchUsersFromCloud")
            Result.failure(e)
        }
    }

    fun fetchUsersFromCloud(): List<User> {
        return fetchUsersFromCloudResult().getOrDefault(emptyList())
    }

    fun pushUsersToCloud(context: Context, users: List<User>) {
        if (!isNetworkAvailable(context) || (!isHostReachable && consecutiveFailures > 0)) return
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
                onNetworkSuccess()
                Log.d(TAG, "Successfully pushed ${users.size} users to cloud")
            } catch (e: Exception) {
                onNetworkFailure(e, "pushUsersToCloud")
            }
        }
    }

    private fun fetchOutpassesFromCloudResult(): Result<List<Outpass>> {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/$OUTPASSES_OBJECT_ID")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return Result.success(emptyList())
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: return Result.success(emptyList())
                val passesArray = data.optJSONArray("outpasses") ?: return Result.success(emptyList())

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
                        val statusVal = if (sObj.has("status")) sObj.getString("status") else sObj.optString("decision", "APPROVED")
                        ApprovalRecord(
                            approverRole = sRole,
                            approverName = sObj.getString("approverName"),
                            status = statusVal,
                            timestamp = sObj.optLong("timestamp", System.currentTimeMillis()),
                            remarks = sObj.optString("remarks", "")
                        )
                    } else null

                    val hodApproval = if (obj.has("hodApproval") && !obj.isNull("hodApproval")) {
                        val hObj = obj.getJSONObject("hodApproval")
                        val hRole = try { UserRole.valueOf(hObj.getString("approverRole")) } catch (e: Exception) { UserRole.HOD }
                        val statusVal = if (hObj.has("status")) hObj.getString("status") else hObj.optString("decision", "APPROVED")
                        ApprovalRecord(
                            approverRole = hRole,
                            approverName = hObj.getString("approverName"),
                            status = statusVal,
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
                onNetworkSuccess()
                Result.success(passes)
            }
        } catch (e: Exception) {
            onNetworkFailure(e, "fetchOutpassesFromCloud")
            Result.failure(e)
        }
    }

    fun fetchOutpassesFromCloud(): List<Outpass> {
        return fetchOutpassesFromCloudResult().getOrDefault(emptyList())
    }

    fun pushOutpassesToCloud(outpasses: List<Outpass>) {
        if (!isHostReachable && consecutiveFailures > 0) return
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
                            put("decision", p.staffApproval.status)
                            put("status", p.staffApproval.status)
                            put("timestamp", p.staffApproval.timestamp)
                            put("remarks", p.staffApproval.remarks)
                        })
                    }

                    if (p.hodApproval != null) {
                        obj.put("hodApproval", JSONObject().apply {
                            put("approverRole", p.hodApproval.approverRole.name)
                            put("approverName", p.hodApproval.approverName)
                            put("decision", p.hodApproval.status)
                            put("status", p.hodApproval.status)
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
                onNetworkSuccess()
                Log.d(TAG, "Successfully pushed ${outpasses.size} outpasses to cloud")
            } catch (e: Exception) {
                onNetworkFailure(e, "pushOutpassesToCloud")
            }
        }
    }
}
