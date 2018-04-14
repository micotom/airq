package com.funglejunk.airq

import android.app.Application
import timber.log.Timber.DebugTree
import timber.log.Timber



class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

}