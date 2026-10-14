package com.xyverse.xyrecorder

import android.app.Application
import com.xyverse.xyrecorder.data.SettingsRepository

class XYRecorderApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }
}
