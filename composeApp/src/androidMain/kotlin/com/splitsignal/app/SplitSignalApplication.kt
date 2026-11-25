package com.splitsignal.app

import android.app.Application
import com.splitsignal.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class SplitSignalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.setProperty("splitsignal.data.dir", filesDir.absolutePath)
        initKoin {
            androidContext(this@SplitSignalApplication)
        }
    }
}
