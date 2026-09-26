package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.repository.OutpassRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyHodReportReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DailyHodReportReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "DailyHodReportReceiver triggered with action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule alarm on device reboot
            DailyHodReportScheduler.scheduleDaily410PmAlarm(context)
            return
        }

        if (action == DailyHodReportScheduler.ACTION_DISPATCH_HOD_DAILY_REPORT) {
            // Automatic 4:10 PM trigger: Fetch records and dispatch report directly to HOD Gmail
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = OutpassRepository.getInstance()
                    val outpasses = repo.outpasses.value
                    val users = repo.users.value

                    Log.i(TAG, "Dispatching automated 4:10 PM HOD report with ${outpasses.size} outpasses...")
                    DailyHodReportScheduler.dispatchReportNow(context, outpasses, users)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed in DailyHodReportReceiver automated dispatch: ${e.message}", e)
                }
            }
        }
    }
}
