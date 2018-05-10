package com.funglejunk.airq

import android.support.multidex.MultiDexApplication
import com.funglejunk.airq.logic.MainActivityPresenter
import com.funglejunk.airq.logic.MainActivityPresenterInterface
import com.funglejunk.airq.logic.location.AndroidGeocoder
import com.funglejunk.airq.logic.location.AndroidLocationProvider
import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.AndroidPermissionHelper
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.net.*
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.Module
import timber.log.Timber
import timber.log.Timber.DebugTree


class Application : MultiDexApplication() {

    private val module : Module = org.koin.dsl.module.applicationContext {
        bean { params -> AndroidPermissionHelper(params["permissionListener"], params["activity"])
                as PermissionHelperInterface }
        bean { params -> AndroidNetworkHelper(params["context"]) as NetworkHelper }
        bean { params -> AndroidLocationProvider(params["context"]) as LocationProvider }
        bean { params -> AndroidGeocoder(params["context"]) as Geocoder }
        bean { AirNowClient() as AirNowClientInterface }
        bean { AirInfoClient() as AirInfoClientInterface }
        bean { OpenAqClient() as OpenAqClientInterface }
        bean { params -> MainActivityPresenter(
                // params["activity"],
                params["permissionListener"],
                get(parameters = { params.values }),
                get(parameters = { params.values }),
                get(parameters = { params.values }),
                get(parameters = { params.values }),
                get(),
                get(),
                get())
                as MainActivityPresenterInterface }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
        startKoin(this, listOf(module))
    }

}