package com.example

import android.app.Application
import com.example.data.repository.OutpassRepository

class OutpassApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OutpassRepository.initialize(this)
    }
}
