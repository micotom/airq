package com.funglejunk.airq.logic

import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.Location
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirNowClientInterface
import com.funglejunk.airq.logic.net.NetworkHelper
import com.funglejunk.airq.logic.streams.MainStream
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.StreamResult
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import kotlin.math.roundToInt

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
        activity.clearText()
        apiSubscription = stream.start()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe { result ->
                    result as StreamResult<Triple<StandardizedMeasurement, Location, Double>>
                    val (measurement, userLocation, distanceToLocation) = result.content
                    val text = when(result.success) {
                        true -> {
                            "Distance: ${distanceToLocation.roundToInt()} meters\n" +
                                    "Date: ${measurement.date}\n" +
                                    "Sensor: ${measurement.sensorType}: ${measurement.value}\n"
                        }
                        false -> "Error reading measurement\n"
                    }
                    Timber.d("display: $text")
                    activity.displayResult(text)
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