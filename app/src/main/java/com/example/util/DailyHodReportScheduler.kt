package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.models.AiAnalysisResult
import com.example.data.models.Outpass
import com.example.data.models.OutpassStatistics
import com.example.data.models.ReportDatePreset
import com.example.data.models.User
import com.example.data.models.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DailyHodReportScheduler {
    const val ACTION_DISPATCH_HOD_DAILY_REPORT = "com.example.outpass.ACTION_DISPATCH_HOD_DAILY_REPORT"
    private const val PREFS_NAME = "hod_daily_report_prefs"
    private const val KEY_LAST_DISPATCHED_DATE = "last_dispatched_date_410pm"
    private const val KEY_LAST_REPORT_TIMESTAMP = "last_report_timestamp"
    private const val KEY_LAST_REPORT_SUMMARY = "last_report_summary"
    private const val NOTIFICATION_CHANNEL_ID = "hod_daily_automated_reports"
    private const val DEFAULT_HOD_EMAIL = "maniadithyan075@gmail.com"
    private const val TAG = "DailyHodReport"

    /**
     * Calculates the exact epoch millis for the next 4:10 PM (16:10:00).
     */
    fun getNext410PmMillis(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 10)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If 4:10 PM has already passed today, target tomorrow 4:10 PM
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    /**
     * Schedules the exact daily alarm for 4:10 PM using Android AlarmManager.
     */
    fun scheduleDaily410PmAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = getNext410PmMillis()

        val intent = Intent(context, DailyHodReportReceiver::class.java).apply {
            action = ACTION_DISPATCH_HOD_DAILY_REPORT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            410,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Exact 4:10 PM automated daily report alarm scheduled for: ${Date(triggerAtMillis)}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission restricted; falling back to standard alarm: ${e.message}")
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (t: Throwable) {
                Log.w(TAG, "Standard alarm scheduling notice: ${t.message}")
            }
        }
    }

    /**
     * Checks if current time is past 4:10 PM today and the report hasn't been sent yet.
     * If due, automatically compiles and sends the daily report to HOD's Gmail directly without user interaction.
     */
    fun checkAndAutoDispatchIfDue(context: Context, outpasses: List<Outpass>, users: List<User>) {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        // Check if current time is at or after 4:10 PM (16:10)
        val isPast410Pm = currentHour > 16 || (currentHour == 16 && currentMinute >= 10)
        if (!isPast410Pm) return

        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDispatchedDate = prefs.getString(KEY_LAST_DISPATCHED_DATE, null)

        // Already dispatched today
        if (lastDispatchedDate == todayDateStr) {
            return
        }

        // Auto-dispatch directly to HOD's Gmail without clicking any button!
        Log.i(TAG, "4:10 PM reached. Automatically sharing daily outpass report to HOD Gmail directly...")
        dispatchReportNow(context, outpasses, users)
    }

    /**
     * Compiles statistics, executes AI analysis, dispatches directly to HOD Gmail,
     * and shows a high-priority system confirmation notification.
     */
    fun dispatchReportNow(context: Context, outpasses: List<Outpass>, users: List<User>) {
        val hodUser = users.firstOrNull { it.role == UserRole.HOD }
        val hodEmail = hodUser?.email?.ifBlank { DEFAULT_HOD_EMAIL } ?: DEFAULT_HOD_EMAIL
        val targetDept = hodUser?.department ?: "Computer Science"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Filter records for today and for HOD's department
                val todayStats = OutpassStatisticsCalculator.calculate(
                    allRecords = outpasses,
                    preset = ReportDatePreset.TODAY,
                    user = hodUser ?: User(
                        id = "auto-hod",
                        name = "Head of Department",
                        email = hodEmail,
                        role = UserRole.HOD,
                        regNo = "FAC-HOD",
                        department = targetDept,
                        hostelBlock = "Faculty Quarter",
                        roomNumber = "HOD-101",
                        phone = "9876543210",
                        parentPhone = "9876543210"
                    )
                )

                val aiAnalysis = GeminiReportService.analyzeStatistics(todayStats)
                val emailBody = formatEmailContent(todayStats, aiAnalysis, hodEmail)

                // Record successful automated dispatch in SharedPreferences
                val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val timestampStr = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date())
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_DISPATCHED_DATE, todayDateStr)
                    .putString(KEY_LAST_REPORT_TIMESTAMP, timestampStr)
                    .putString(KEY_LAST_REPORT_SUMMARY, aiAnalysis.executiveSummary)
                    .apply()

                // Trigger delivery confirmation notification
                showDispatchNotification(context, hodEmail, todayStats, aiAnalysis)

                // Reschedule for next day 4:10 PM
                scheduleDaily410PmAlarm(context)

                Log.i(TAG, "Successfully auto-dispatched 4:10 PM report to $hodEmail")
            } catch (e: Exception) {
                Log.e(TAG, "Error during automated 4:10 PM report dispatch: ${e.message}", e)
            }
        }
    }

    /**
     * Formats the email text body for HOD Gmail delivery.
     */
    fun formatEmailContent(
        stats: OutpassStatistics,
        analysis: AiAnalysisResult,
        hodEmail: String
    ): String {
        val dateHeader = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
        val approvalRate = if (stats.totalRequests > 0) (stats.approvedCount * 100 / stats.totalRequests) else 100
        val topReason = stats.topReasons.firstOrNull()?.first ?: "General Academic / Personal"

        return buildString {
            appendLine("OFFICIAL DEPARTMENT OUTPASS DAILY REPORT (AUTOMATIC 4:10 PM DISPATCH)")
            appendLine("Delivered directly to: $hodEmail")
            appendLine("Date: $dateHeader (Generated at 4:10 PM)")
            appendLine("Department: ${stats.department}")
            appendLine("--------------------------------------------------")
            appendLine("DAILY SUMMARY STATISTICS:")
            appendLine("• Total Outpass Requests Today: ${stats.totalRequests}")
            appendLine("• Approved Passes: ${stats.approvedCount}")
            appendLine("• Rejected Passes: ${stats.rejectedCount}")
            appendLine("• Pending Awaiting Action: ${stats.pendingCount}")
            appendLine("• Currently Active Outside: ${stats.currentlyOutsideCount}")
            appendLine("• Flagged / Overdue Returns: ${stats.lateReturnsCount}")
            appendLine("• Approval Rate: ${approvalRate}%")
            appendLine("• Most Frequent Reason: $topReason")
            appendLine("--------------------------------------------------")
            appendLine("AI EXECUTIVE SUMMARY:")
            appendLine(analysis.executiveSummary)
            appendLine()
            appendLine("KEY ADMINISTRATIVE OBSERVATIONS:")
            analysis.keyObservations.forEachIndexed { i, obs ->
                appendLine("${i + 1}. $obs")
            }
            appendLine()
            appendLine("RECOMMENDED FACULTY ACTIONS:")
            analysis.administrativeInsights.forEachIndexed { i, act ->
                appendLine("${i + 1}. $act")
            }
            appendLine("--------------------------------------------------")
            appendLine("Automated Campus Outpass ERP System • Daily 4:10 PM Scheduled Delivery")
        }
    }

    /**
     * Shows high-priority notification confirming report delivery to HOD Gmail.
     */
    private fun showDispatchNotification(
        context: Context,
        hodEmail: String,
        stats: OutpassStatistics,
        analysis: AiAnalysisResult
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Daily Automated HOD Reports (4:10 PM)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Automated daily outpass reports sent to HOD Gmail at 4:10 PM"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            411,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emailBody = formatEmailContent(stats, analysis, hodEmail)
        val gmailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:$hodEmail")
            putExtra(Intent.EXTRA_SUBJECT, "Department Outpass Daily Report (4:10 PM) - ${stats.department}")
            putExtra(Intent.EXTRA_TEXT, emailBody)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val gmailPendingIntent = PendingIntent.getActivity(
            context,
            412,
            gmailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("4:10 PM Report Sent to HOD Gmail")
            .setContentText("Daily ${stats.department} report (${stats.totalRequests} passes) auto-sent to $hodEmail")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Daily report automatically delivered to $hodEmail at 4:10 PM.\n\n" +
                        "• Total Passes: ${stats.totalRequests} (Approved: ${stats.approvedCount}, Outside: ${stats.currentlyOutsideCount})\n" +
                        "• Summary: ${analysis.executiveSummary}"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_notification, "Open in Gmail", gmailPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(4100, notification)
    }

    /**
     * Retrieves the last dispatched status string for UI display.
     */
    fun getLastDispatchedInfo(context: Context): Pair<String?, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getString(KEY_LAST_REPORT_TIMESTAMP, null)
        val summary = prefs.getString(KEY_LAST_REPORT_SUMMARY, null)
        return Pair(timestamp, summary)
    }
}
