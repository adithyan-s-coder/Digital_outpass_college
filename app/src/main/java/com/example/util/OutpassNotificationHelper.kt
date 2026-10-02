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
    const val EXTRA_PASS_ID = "extra_pass_id"
    const val EXTRA_TARGET_TAB = "extra_target_tab"

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

    fun notifyStaffAndHodOnNewRequest(context: Context, pass: Outpass, student: User?) {
        try {
            initializeChannels(context)

            val studentName = if (!student?.name.isNullOrBlank()) student?.name!! else pass.studentName
            val reason = if (pass.reason.isNotBlank()) pass.reason else "College Exit"
            val regNo = if (!student?.regNo.isNullOrBlank()) student?.regNo!! else pass.regNo

            // Exact requested format:
            // "the "student name" request the outpass for this "type of reason""
            val notificationMessage = "the \"$studentName\" request the outpass for this \"$reason\""
            val title = "📋 Outpass Request • ${pass.department} (${pass.type.displayName})"

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TARGET_TAB, 0)
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
                .bigText(
                    "$notificationMessage\n\n" +
                    "• Student: $studentName ($regNo)\n" +
                    "• Department: ${pass.department}\n" +
                    "• Destination: ${pass.destination}\n" +
                    "• Room: ${pass.hostelBlock} - ${pass.roomNo}"
                )
                .setSummaryText("Pending Staff/HOD Approval")

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_REQUESTS)
                .setSmallIcon(R.drawable.ic_notification)
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Notification suppressed.")
                return
            }

            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (pass.id.hashCode() and 0x7FFFFFFF)
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.i(TAG, "Successfully dispatched Staff/HOD outpass notification: $notificationMessage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send outpass notification: ${e.message}", e)
        }
    }

    fun notifyHodOnStaffApproval(context: Context, pass: Outpass, student: User?) {
        try {
            initializeChannels(context)

            val studentName = if (!student?.name.isNullOrBlank()) student?.name!! else pass.studentName
            val reason = if (pass.reason.isNotBlank()) pass.reason else "College Exit"
            val title = "📋 Staff Approved • Awaiting HOD Decision"
            val message = "the \"$studentName\" request was approved by Staff Advisor. Final HOD approval needed for \"$reason\""

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_PASS_ID, pass.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                (pass.id + "_hod").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_REQUESTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((pass.id + "_hod").hashCode() and 0x7FFFFFFF, notificationBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send HOD approval notification: ${e.message}", e)
        }
    }

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
                (pass.id + "_student").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_REQUESTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((pass.id + "_student").hashCode() and 0x7FFFFFFF, notificationBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send student approval notification: ${e.message}", e)
        }
    }

    fun notifyStudentOnRejection(context: Context, pass: Outpass, remarks: String) {
        try {
            initializeChannels(context)

            val title = "❌ Outpass ${pass.id} Not Approved"
            val message = "Your outpass request was rejected: $remarks"

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_PASS_ID, pass.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                (pass.id + "_reject").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_REQUESTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((pass.id + "_reject").hashCode() and 0x7FFFFFFF, notificationBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send rejection notification: ${e.message}", e)
        }
    }
}
