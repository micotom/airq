package com.funglejunk.airq.logic

import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirNowClientInterface
import com.funglejunk.airq.logic.net.NetworkHelper
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MainActivityPresenter(private val activity: MainActivityView,
                            permissionListener: RxPermissionListener,
                            permissionHelper: PermissionHelperInterface,
                            networkHelper: NetworkHelper,
                            locationProvider: LocationProvider,
                            geoCoder: Geocoder,
                            airNowClient: AirNowClientInterface) :
        MainActivityPresenterInterface {

    private var apiSubscription: Disposable? = null
    private val stream: MainStream = MainStream(
            permissionListener,
            permissionHelper,
            networkHelper,
            locationProvider,
            geoCoder,
            airNowClient)

    override fun viewStarted() {
        apiSubscription = stream.start()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe {
                    Timber.d("result: $it")
                    activity.displayResult(it.content.toString() + "\n")
                }
    }

    override fun viewStopped() {
        apiSubscription?.let { safe ->
            if (!safe.isDisposed) {
                safe.dispose()
            }
        }
    }

}