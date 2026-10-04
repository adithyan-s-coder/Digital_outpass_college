package com.example

import android.app.Application
import com.example.data.repository.OutpassRepository

import com.example.util.OutpassNotificationHelper

class OutpassApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OutpassRepository.initialize(this)
        OutpassNotificationHelper.initializeChannels(this)
    }
}
