package com.splitsignal.app

import android.app.Application
import com.splitsignal.app.di.initKoin
import com.splitsignal.app.platform.AppLoggerBridge
import org.koin.android.ext.koin.androidContext

class SplitSignalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.setProperty("splitsignal.data.dir", filesDir.absolutePath)
        AppLoggerBridge.initialize(this)
        initKoin {
            androidContext(this@SplitSignalApplication)
        }
    }
}
