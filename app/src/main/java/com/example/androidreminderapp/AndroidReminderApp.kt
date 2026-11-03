package com.example.androidreminderapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AndroidReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
