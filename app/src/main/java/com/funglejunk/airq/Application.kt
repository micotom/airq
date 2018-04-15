package com.funglejunk.airq

import android.app.Application
import com.funglejunk.airq.logic.MainActivityPresenter
import com.funglejunk.airq.logic.MainActivityPresenterInterface
import com.funglejunk.airq.logic.location.AndroidGeocoder
import com.funglejunk.airq.logic.location.AndroidLocationProvider
import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.AndroidPermissionHelper
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.net.AirNowClient
import com.funglejunk.airq.logic.net.AirNowClientInterface
import com.funglejunk.airq.logic.net.AndroidNetworkHelper
import com.funglejunk.airq.logic.net.NetworkHelper
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.Module
import timber.log.Timber
import timber.log.Timber.DebugTree


class Application : Application() {

    private val module : Module = org.koin.dsl.module.applicationContext {
        factory { params -> AndroidPermissionHelper(params["permissionListener"], params["activity"])
                as PermissionHelperInterface }
        factory { params -> AndroidNetworkHelper(params["context"]) as NetworkHelper }
        factory { params -> AndroidLocationProvider(params["context"]) as LocationProvider }
        factory { params -> AndroidGeocoder(params["context"]) as Geocoder }
        factory { AirNowClient() as AirNowClientInterface }
        factory { params -> MainActivityPresenter(
                params["activity"],
                params["permissionListener"],
                get(parameters = { params.values }),
                get(parameters = { params.values }),
                get(parameters = { params.values }),
                get(parameters = { params.values }),
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