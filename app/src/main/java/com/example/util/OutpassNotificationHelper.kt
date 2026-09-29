package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.models.Outpass
import com.example.data.models.User

object OutpassNotificationHelper {
    private const val TAG = "OutpassNotificationHelper"

    const val CHANNEL_ID_REQUESTS = "outpass_staff_hod_channel"
    const val CHANNEL_NAME_REQUESTS = "Staff & HOD Outpass Alerts"
    const val EXTRA_TARGET_TAB = "extra_target_tab"
    const val EXTRA_PASS_ID = "extra_pass_id"

    fun initializeChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channel = NotificationChannel(
                CHANNEL_ID_REQUESTS,
                CHANNEL_NAME_REQUESTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate push alerts to Department Staff and HOD when students apply for outpasses."
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
            Log.i(TAG, "Notification channel $CHANNEL_ID_REQUESTS initialized.")
        }
    }

    /**
     * Dispatches an Android push notification to Staff Advisor and HOD when a student requests an outpass.
     * Content Format strictly follows:
     * 'the "<student name>" request the outpass for this "<reason>"'
     */
    fun notifyStaffAndHodOnNewRequest(context: Context, pass: Outpass, student: User) {
        try {
            initializeChannels(context)

            val studentName = student.name.ifBlank { pass.studentName }
            val reason = pass.reason.ifBlank { "Personal work" }

            // Requested notification message format
            val notificationMessage = "the \"$studentName\" request the outpass for this \"$reason\""
            val title = "📋 Outpass Request • ${pass.department} (${pass.type.displayName})"

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TARGET_TAB, 0) // Approvals tab
                putExtra(EXTRA_PASS_ID, pass.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                pass.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val bigTextStyle = NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .bigText("$notificationMessage\n\n• Student: $studentName (${student.regNo})\n• Department: ${pass.department}\n• Out Time: ${pass.outDateTime}\n• Destination: ${pass.destination}")
                .setSummaryText("Pending Staff/HOD Approval")

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_REQUESTS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(notificationMessage)
                .setStyle(bigTextStyle)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0, 300, 200, 300))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)

            val notificationManager = NotificationManagerCompat.from(context)

            // Check permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Notification suppressed.")
                    return
                }
            }

            val notificationId = (System.currentTimeMillis() % 100000).toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.i(TAG, "Successfully dispatched Staff/HOD outpass notification: $notificationMessage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send outpass notification: ${e.message}", e)
        }
    }

    /**
     * Notifies student when their outpass has been approved by HOD with live QR ready.
     */
    fun notifyStudentOnApproval(context: Context, pass: Outpass) {
        try {
            initializeChannels(context)

            val title = "✅ Outpass ${pass.id} APPROVED!"
            val message = "Your ${pass.type.displayName} outpass has been approved by HOD. Gate QR is now active for 1 hour."

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_PASS_ID, pass.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                pass.id.hashCode() + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_REQUESTS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notificationBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send student approval notification: ${e.message}", e)
        }
    }
}
