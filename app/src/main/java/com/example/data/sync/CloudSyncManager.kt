package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.models.ApprovalRecord
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatus
import com.example.data.models.OutpassType
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.data.repository.OutpassRepository
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
import java.util.concurrent.TimeUnit

/**
 * CloudSyncManager provides instant multi-device synchronization over HTTPS.
 * Ensures:
 * 1. An account registered/logged in on ANY mobile device is immediately synchronized
 *    and usable across ALL mobile devices.
 * 2. Outpass requests submitted from a student's mobile device are pushed to the cloud
 *    and immediately trigger push notifications on the Department Staff and HOD mobile devices:
 *    'the "<student name>" request the outpass for this "<reason>"'
 * 3. Approvals, rejections, and gate security checkouts are synced in real-time across all devices.
 */
object CloudSyncManager {
    private const val TAG = "CloudSyncManager"

    // Permanent cloud sync objects hosted on global REST endpoint
    const val USERS_OBJECT_ID = "ff808181a09d98f701a0f079103b46d8"
    const val OUTPASSES_OBJECT_ID = "ff808181a09d98f701a0f07938af46d9"
    private const val BASE_URL = "https://api.restful-api.dev/objects"

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

    fun startSync(context: Context, repository: OutpassRepository) {
        val appContext = context.applicationContext
        if (syncJob?.isActive == true) return

        // Seed initial known pass IDs so existing local passes don't trigger duplicate notifications on boot
        repository.outpasses.value.forEach { knownNotifiedPassIds.add(it.id) }

        syncJob = syncScope.launch {
            // Run immediate sync on start
            syncFromCloud(appContext, repository)

            // Continue background polling every 10 seconds for real-time multi-device alerts
            while (isActive) {
                delay(10000L)
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
            // 1. Sync registered users across all devices
            syncUsers(repository)

            // 2. Sync outpass requests and approvals across all devices
            syncOutpasses(context, repository)
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync: ${e.message}")
        } finally {
            isSyncing = false
        }
    }

    // -------------------------------------------------------------
    // USERS SYNCHRONIZATION
    // -------------------------------------------------------------

    private fun syncUsers(repository: OutpassRepository) {
        try {
            val cloudUsers = fetchUsersFromCloud()
            if (cloudUsers.isNotEmpty()) {
                val currentLocalUsers = repository.users.value.toMutableList()
                var updated = false

                for (cu in cloudUsers) {
                    val localIndex = currentLocalUsers.indexOfFirst {
                        it.id == cu.id || it.email.equals(cu.email, ignoreCase = true)
                    }
                    if (localIndex == -1) {
                        currentLocalUsers.add(cu)
                        updated = true
                        Log.d(TAG, "Imported new user from cloud: ${cu.email} (${cu.role})")
                    } else {
                        val local = currentLocalUsers[localIndex]
                        // Update if cloud has newer password or reset
                        if (local.password != cu.password || (cu.lastPasswordResetAt ?: 0) > (local.lastPasswordResetAt ?: 0)) {
                            currentLocalUsers[localIndex] = cu
                            updated = true
                        }
                    }
                }

                if (updated) {
                    repository.setUsersFromSync(currentLocalUsers)
                }

                // If local has users that are NOT yet in the cloud, push merged list to cloud
                val missingInCloud = currentLocalUsers.any { localUser ->
                    cloudUsers.none { it.id == localUser.id || it.email.equals(localUser.email, ignoreCase = true) }
                }
                if (missingInCloud) {
                    pushUsersToCloud(currentLocalUsers)
                }
            } else {
                // Cloud is empty, push current local users to seed the cloud
                pushUsersToCloud(repository.users.value)
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncUsers error: ${e.message}")
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
                val root = JSONObject(body)
                val data = root.optJSONObject("data") ?: return emptyList()
                val usersArray = data.optJSONArray("users") ?: return emptyList()

                val list = mutableListOf<User>()
                for (i in 0 until usersArray.length()) {
                    val obj = usersArray.getJSONObject(i)
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
                            photoUri = obj.optString("photoUri", "").takeIf { it.isNotBlank() },
                            password = obj.optString("password", "Pass@1234"),
                            lastPasswordResetAt = if (obj.has("lastPasswordResetAt")) obj.getLong("lastPasswordResetAt") else null
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchUsersFromCloud exception: ${e.message}")
            emptyList()
        }
    }

    fun pushUsersToCloud(users: List<User>) {
        syncScope.launch {
            try {
                val usersArray = JSONArray()
                for (u in users) {
                    val obj = JSONObject().apply {
                        put("id", u.id)
                        put("name", u.name)
                        put("email", u.email)
                        put("role", u.role.name)
                        put("regNo", u.regNo)
                        put("department", u.department)
                        put("hostelBlock", u.hostelBlock)
                        put("roomNumber", u.roomNumber)
                        put("phone", u.phone)
                        put("parentPhone", u.parentPhone)
                        put("photoUri", u.photoUri ?: "")
                        put("password", u.password)
                        if (u.lastPasswordResetAt != null) put("lastPasswordResetAt", u.lastPasswordResetAt)
                    }
                    usersArray.put(obj)
                }

                val payload = JSONObject().apply {
                    put("name", "vetias_outpass_users_v1")
                    put("data", JSONObject().apply {
                        put("users", usersArray)
                    })
                }

                val req = Request.Builder()
                    .url("$BASE_URL/$USERS_OBJECT_ID")
                    .put(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        Log.d(TAG, "Successfully synced ${users.size} users to cloud.")
                    } else {
                        Log.w(TAG, "Failed pushing users to cloud: code ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "pushUsersToCloud error: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------
    // OUTPASSES SYNCHRONIZATION & NOTIFICATIONS
    // -------------------------------------------------------------

    private fun syncOutpasses(context: Context, repository: OutpassRepository) {
        try {
            val cloudPasses = fetchOutpassesFromCloud()
            if (cloudPasses.isEmpty()) {
                // If cloud is empty, seed it with current local outpasses
                if (repository.outpasses.value.isNotEmpty()) {
                    pushOutpassesToCloud(repository.outpasses.value)
                }
                return
            }

            val currentLocalPasses = repository.outpasses.value.toMutableList()
            var localChanged = false

            for (cp in cloudPasses) {
                val localIndex = currentLocalPasses.indexOfFirst { it.id.equals(cp.id, ignoreCase = true) }
                if (localIndex == -1) {
                    // NEW PASS DETECTED FROM ANOTHER MOBILE DEVICE!
                    currentLocalPasses.add(0, cp)
                    localChanged = true

                    // Check if notification should fire on this device:
                    // If pass was newly submitted (applied recently or PENDING_STAFF) and not yet notified locally:
                    if (!knownNotifiedPassIds.contains(cp.id)) {
                        knownNotifiedPassIds.add(cp.id)

                        // Trigger push notification to Department Staff & HOD
                        val studentUser = repository.findUserByIdentifier(cp.regNo)
                            ?: repository.findUserByIdentifier(cp.studentId)
                            ?: User(
                                id = cp.studentId,
                                name = cp.studentName,
                                email = "${cp.regNo.lowercase()}@vetias.ac.in",
                                role = UserRole.STUDENT,
                                regNo = cp.regNo,
                                department = cp.department,
                                phone = cp.studentPhone,
                                parentPhone = cp.parentPhone
                            )

                        Log.i(TAG, "Dispatching cross-device outpass notification for student ${cp.studentName}")
                        OutpassNotificationHelper.notifyStaffAndHodOnNewRequest(context, cp, studentUser)
                    }
                } else {
                    val local = currentLocalPasses[localIndex]
                    // If cloud has an updated status or approval, update local
                    if (local.status != cp.status || local.isQrUsed != cp.isQrUsed || local.actualCheckOutTime != cp.actualCheckOutTime) {
                        currentLocalPasses[localIndex] = cp
                        localChanged = true
                        Log.d(TAG, "Updated pass ${cp.id} status to ${cp.status} from cloud")
                    }
                }
            }

            if (localChanged) {
                repository.setOutpassesFromSync(currentLocalPasses)
            }

            // If local has outpasses that are missing from cloud, upload them
            val missingInCloud = currentLocalPasses.any { localPass ->
                cloudPasses.none { it.id.equals(localPass.id, ignoreCase = true) }
            }
            if (missingInCloud) {
                pushOutpassesToCloud(currentLocalPasses)
            }
        } catch (e: Exception) {
            Log.w(TAG, "syncOutpasses error: ${e.message}")
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
                val root = JSONObject(body)
                val data = root.optJSONObject("data") ?: return emptyList()
                val outpassesArray = data.optJSONArray("outpasses") ?: return emptyList()

                val list = mutableListOf<Outpass>()
                for (i in 0 until outpassesArray.length()) {
                    val obj = outpassesArray.getJSONObject(i)
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
                            qrToken = obj.optString("qrToken", ""),
                            appliedAt = obj.optLong("appliedAt", System.currentTimeMillis()),
                            staffApproval = staffApp,
                            hodApproval = hodApp,
                            actualCheckOutTime = if (obj.has("actualCheckOutTime")) obj.getLong("actualCheckOutTime") else null,
                            actualCheckInTime = if (obj.has("actualCheckInTime")) obj.getLong("actualCheckInTime") else null,
                            rejectionReason = if (obj.has("rejectionReason")) obj.getString("rejectionReason") else null,
                            parentNotified = obj.optBoolean("parentNotified", false),
                            isQrUsed = obj.optBoolean("isQrUsed", false),
                            studentPhotoUri = obj.optString("studentPhotoUri", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchOutpassesFromCloud error: ${e.message}")
            emptyList()
        }
    }

    fun pushOutpassesToCloud(outpasses: List<Outpass>) {
        syncScope.launch {
            try {
                val array = JSONArray()
                // Keep the most recent 50 outpasses in cloud for fast bandwidth
                val subset = outpasses.take(50)
                for (p in subset) {
                    val obj = JSONObject().apply {
                        put("id", p.id)
                        put("studentId", p.studentId)
                        put("studentName", p.studentName)
                        put("regNo", p.regNo)
                        put("department", p.department)
                        put("hostelBlock", p.hostelBlock)
                        put("roomNo", p.roomNo)
                        put("studentPhone", p.studentPhone)
                        put("parentPhone", p.parentPhone)
                        put("type", p.type.name)
                        put("destination", p.destination)
                        put("reason", p.reason)
                        put("outDateTime", p.outDateTime)
                        put("returnDateTime", p.returnDateTime)
                        put("status", p.status.name)
                        put("qrToken", p.qrToken)
                        put("appliedAt", p.appliedAt)
                        if (p.staffApproval != null) {
                            put("staffApproval", JSONObject().apply {
                                put("role", p.staffApproval.approverRole.name)
                                put("name", p.staffApproval.approverName)
                                put("status", p.staffApproval.status)
                                put("timestamp", p.staffApproval.timestamp)
                                put("remarks", p.staffApproval.remarks)
                            })
                        }
                        if (p.hodApproval != null) {
                            put("hodApproval", JSONObject().apply {
                                put("role", p.hodApproval.approverRole.name)
                                put("name", p.hodApproval.approverName)
                                put("status", p.hodApproval.status)
                                put("timestamp", p.hodApproval.timestamp)
                                put("remarks", p.hodApproval.remarks)
                            })
                        }
                        if (p.actualCheckOutTime != null) put("actualCheckOutTime", p.actualCheckOutTime)
                        if (p.actualCheckInTime != null) put("actualCheckInTime", p.actualCheckInTime)
                        if (p.rejectionReason != null) put("rejectionReason", p.rejectionReason)
                        put("parentNotified", p.parentNotified)
                        put("isQrUsed", p.isQrUsed)
                        if (p.studentPhotoUri != null) put("studentPhotoUri", p.studentPhotoUri)
                    }
                    array.put(obj)
                }

                val payload = JSONObject().apply {
                    put("name", "vetias_outpass_requests_v1")
                    put("data", JSONObject().apply {
                        put("outpasses", array)
                    })
                }

                val req = Request.Builder()
                    .url("$BASE_URL/$OUTPASSES_OBJECT_ID")
                    .put(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        Log.d(TAG, "Successfully synced ${subset.size} outpasses to cloud.")
                    } else {
                        Log.w(TAG, "Failed pushing outpasses to cloud: code ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "pushOutpassesToCloud error: ${e.message}")
            }
        }
    }
}
