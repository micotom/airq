package com.funglejunk.airq.logic

import com.funglejunk.airq.logic.net.NetworkHelper
import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirNowClientInterface
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MainActivityPresenter(private val activity: MainActivityView,
                            private val permissionListener: RxPermissionListener,
                            private val permissionHelper: PermissionHelperInterface,
                            private val networkHelper: NetworkHelper,
                            private val locationProvider: LocationProvider,
                            private val geocoder: Geocoder,
                            private val airNowClient: AirNowClientInterface) :
        MainActivityPresenterInterface {

    private var apiSubscription: Disposable? = null

    override fun viewStarted() {
        apiSubscription =
                AirQApiStream(permissionListener, permissionHelper, networkHelper, locationProvider,
                        geocoder, airNowClient)
                .work()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe {
                    Timber.d("result: $it")
                    activity.displayResult(it.toString())
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