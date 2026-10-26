package com.example

import android.app.Application
import android.util.Log
import com.example.data.repository.OutpassRepository
import com.example.util.DailyHodReportScheduler
import com.example.util.OutpassNotificationHelper

class OutpassApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            OutpassRepository.initialize(this)
        } catch (t: Throwable) {
            Log.e(TAG, "Error initializing OutpassRepository: ${t.message}", t)
        }

        try {
            OutpassNotificationHelper.initializeChannels(this)
        } catch (t: Throwable) {
            Log.e(TAG, "Error initializing notification channels: ${t.message}", t)
        }

        try {
            DailyHodReportScheduler.scheduleDaily410PmAlarm(this)
        } catch (t: Throwable) {
            Log.e(TAG, "Error scheduling daily 4:10 PM alarm: ${t.message}", t)
        }
    }

    companion object {
        private const val TAG = "OutpassApplication"
    }
}
