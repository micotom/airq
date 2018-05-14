package com.funglejunk.airq.logic

import arrow.core.Try
import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirInfoClientInterface
import com.funglejunk.airq.logic.net.AirNowClientInterface
import com.funglejunk.airq.logic.net.NetworkHelper
import com.funglejunk.airq.logic.net.OpenAqClientInterface
import com.funglejunk.airq.logic.streams.MainStream
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.SensorClass
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.util.roundTo2Decimals
import com.funglejunk.airq.view.MainFragmentView
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MainFragmentPresenter(permissionListener: RxPermissionListener,
                            permissionHelper: PermissionHelperInterface,
                            networkHelper: NetworkHelper,
                            locationProvider: LocationProvider,
                            geoCoder: Geocoder,
                            airNowClient: AirNowClientInterface,
                            airInfoClient: AirInfoClientInterface,
                            openAqClient: OpenAqClientInterface) :
        MainActivityPresenterInterface {

    private lateinit var view: MainFragmentView

    private var apiSubscription: Disposable? = null
    private val stream: MainStream = MainStream(
            permissionListener,
            permissionHelper,
            networkHelper,
            locationProvider,
            geoCoder,
            airNowClient,
            airInfoClient,
            openAqClient,
            this)

    override fun signalUserLocation(location: Location) {
        view.onUserLocationKnown(location)
    }

    override fun viewStarted(fragment: MainFragmentView) {
        view = fragment
        apiSubscription = stream.start()
                .doOnSubscribe {
                    fragment.clearSensorLocations()
                    fragment.clearSensorValues()
                    fragment.alphaOutIconTable()
                    fragment.showLoadingAnimation()
                }
                .doOnEvent { event, _ ->
                    event.fold(
                            {
                                Timber.e("cannot display sensor locations")
                            },
                            { streamResults ->
                                if (streamResults.isNotEmpty()) {
                                    val userLocation = streamResults.first().second
                                    val sensorLocations = streamResults
                                            .distinctBy {
                                                it.first.coordinates
                                            }
                                            .map {
                                                val (lat, lng) = it.first.coordinates
                                                Location(lat, lng)
                                            }
                                    val measurements = streamResults
                                            .map {
                                                it.first
                                            }
                                            .distinctBy {
                                                it.coordinates
                                            }
                                    fragment.setLocations(userLocation, sensorLocations, measurements)
                                }
                            }
                    )
                }
                .map {
                    it.fold(
                            {
                                Try.Failure<List<Pair<StandardizedMeasurement, Double>>>(it)
                            },
                            { results ->
                                val measurementDistancePair = results.map { result ->
                                    val (measurement, _, distanceToLocation) = result
                                    Pair(measurement, distanceToLocation)
                                }
                                Try.Success(measurementDistancePair)
                            }
                    )

                }
                .doOnEvent { event, _ ->
                    event.fold(
                            {
                                Timber.e("Cannot calculate and display sensor values")
                            },
                            { measurementDistancePair ->
                                val measurements = measurementDistancePair.map {
                                    it.first
                                }
                                val avTemp = measurements.getAv(SensorClass.TEMPERATURE)
                                val avHumidity = measurements.getAv(SensorClass.HUMIDITY)
                                val avPm10 = measurements.getAv(SensorClass.PM10)
                                val avPm25 = measurements.getAv(SensorClass.PM25)
                                val avCo2 = measurements.getAv(SensorClass.CO2)
                                val avPressure = measurements.getAv(SensorClass.BAROMETER)
                                val avNo2 = measurements.getAv(SensorClass.NO2)
                                with(fragment) {
                                    setTemperatureValue(avTemp)
                                    setCo2Value(avCo2)
                                    setPm10Value(avPm10)
                                    setPm25Value(avPm25)
                                }
                            }
                    )
                }
                .doFinally {
                    fragment.hideLoadingAnimation()
                    fragment.alphaInIconTable()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        { _ ->
                            Timber.d("stream finished")
                        },
                        {
                            Timber.e("stream error: $it")
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

    private fun List<StandardizedMeasurement>.getAv(sensorClass: SensorClass): Double {
        val values = flatMap {
            it.measurements.filter {
                it.sensorType == sensorClass
            }
        }

        val av = when (values.isNotEmpty()) {
            true -> values.map { it.value }.average()
            else -> Double.NaN
        }
        return when (av) {
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY -> Double.NaN
            else -> av.roundTo2Decimals()
        }
    }

}