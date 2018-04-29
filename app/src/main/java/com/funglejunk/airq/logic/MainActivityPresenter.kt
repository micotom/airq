package com.funglejunk.airq.logic

import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirNowClientInterface
import com.funglejunk.airq.logic.net.NetworkHelper
import com.funglejunk.airq.logic.streams.MainStream
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.SensorClass
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.StreamResult
import com.funglejunk.airq.util.roundTo2Decimals
import com.funglejunk.airq.view.MainActivityView
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MainActivityPresenter(permissionListener: RxPermissionListener,
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

    override fun viewStarted(activity: MainActivityView) {
        apiSubscription = stream.start()
                .doOnSubscribe {
                    activity.clearSensorLocations()
                    activity.clearSensorValues()
                    activity.alphaOutIconTable()
                    activity.showLoadingAnimation()
                }
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .doOnEvent { event, _ ->
                    if (event.isNotEmpty()) {
                        val userLocation = event.first().content.second
                        val sensorLocations = event
                                .distinctBy {
                                    it.content.first.coordinates
                                }
                                .map {
                                    val (lat, lng) = it.content.first.coordinates
                                    Location(lat, lng)
                                }
                        activity.displaySensorLocations(userLocation, sensorLocations)
                    }
                }
                .map { results ->
                    results.map { result ->
                        val (measurement, _, distanceToLocation) = result.content
                        StreamResult(result.info, result.success, Pair(measurement, distanceToLocation))
                    }
                }
                .doOnEvent { results, _ ->

                    fun List<StandardizedMeasurement>.getAv(sensorClass: SensorClass): Double {
                        val values = filter { it.sensorType == sensorClass }
                        val av = when (values.isNotEmpty()) {
                            true -> values.map { it.value }.average()
                            else -> Double.NaN
                        }
                        return when (av) {
                            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY -> Double.NaN
                            else -> av.roundTo2Decimals()
                        }
                    }

                    val measurements = results.map {
                        it.content.first
                    }

                    val avTemp = measurements.getAv(SensorClass.TEMPERATURE)
                    val avHumidity = measurements.getAv(SensorClass.HUMIDITY)
                    val avPm10 = measurements.getAv(SensorClass.PM10)
                    val avPm25 = measurements.getAv(SensorClass.PM25)
                    val avCo2 = measurements.getAv(SensorClass.CO2)
                    val avPressure = measurements.getAv(SensorClass.BAROMETER)

                    with(activity) {
                        setTemperatureValue(avTemp)
                        setCo2Value(avCo2)
                        setPm10Value(avPm10)
                        setPm25Value(avPm25)
                    }
                }
                .doFinally {
                    activity.hideLoadingAnimation()
                    activity.alphaInIconTable()
                    Timber.e("main stream finished")
                }
                .subscribe(
                        { _ ->
                            Timber.d("stream finished")
                        }
                )
    }

    override fun viewStopped() {
        apiSubscription?.let { safe ->
            if (!safe.isDisposed) {
                safe.dispose()
            }
        }
    }

}