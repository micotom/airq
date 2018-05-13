package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.logic.MainActivityPresenterInterface
import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirInfoClientInterface
import com.funglejunk.airq.logic.net.AirNowClientInterface
import com.funglejunk.airq.logic.net.NetworkHelper
import com.funglejunk.airq.logic.net.OpenAqClientInterface
import com.funglejunk.airq.model.AirqException
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.util.filterForSuccess
import com.funglejunk.airq.util.simpleFold
import com.funglejunk.airq.util.zipToPair
import io.reactivex.Flowable
import io.reactivex.Single
import timber.log.Timber

class MainStream(private val permissionListener: RxPermissionListener,
                 private val permissionHelper: PermissionHelperInterface,
                 private val networkHelper: NetworkHelper,
                 private val locationProvider: LocationProvider,
                 private val geoCoder: Geocoder,
                 private val airNowClient: AirNowClientInterface,
                 private val airInfoClient: AirInfoClientInterface,
                 private val openAqClient: OpenAqClientInterface,
                 private val presenter: MainActivityPresenterInterface) {

    fun start(): Single<Try<List<Triple<StandardizedMeasurement, Location, Double>>>> {
        return Single.just { Timber.d("starting stream ...") }
                .map {
                    permissionHelper.check()
                }
                .doOnEvent { _, _ ->
                    Timber.d("listen for location permission")
                }
                .flatMap {
                    permissionListener.listen().first(false)
                            .map {
                                when (it) {
                                    true -> Try.Success(it)
                                    false -> Try.Failure<Boolean>(AirqException.NoLocationPermission())
                                }
                            }
                }
                .doOnEvent { event, _ ->
                    Timber.d(event.toString())
                }
                .map {
                    it.fold(
                            { Try.Failure<Boolean>(it) },
                            { Try.Success(networkHelper.networkAvailable()) }
                    )
                }
                .doOnEvent { event, _ ->
                    Timber.d(event.toString())
                }
                .map {
                    it.flatMap {
                        when (it) {
                            true -> Try.Success(true)
                            false -> Try.Failure<Boolean>(AirqException.NoNetwork())
                        }
                    }
                }
                .flatMap {
                    it.fold(
                            {
                                Single.just(Try.Failure<Location>(AirqException.InvalidLastKnownLocation()))
                            },
                            {
                                locationProvider.getLastKnownLocation().map {
                                    when (it.isValid) {
                                        true -> Try.Success(it)
                                        false -> Try.Failure<Location>(AirqException.InvalidLastKnownLocation())
                                    }
                                }.first(Try.Failure(AirqException.InvalidLastKnownLocation()))
                            }
                    )
                }
                .doOnEvent { event, _ ->
                    Timber.d(event.toString())
                    event.map {
                        presenter.signalUserLocation(it)
                    }
                }.toFlowable()
                .flatMap {
                    it.fold(
                            {
                                Flowable.just(Pair(Try.Failure<List<Try<StandardizedMeasurement>>>(it),
                                        Try.Failure<Location>(it)))
                            },
                            {
                                zipToPair(
                                        Single.concat(
                                                AirInfoStream(it, airInfoClient).single(),
                                                OpenAqStream(it, openAqClient).single()
                                        ),
                                        Single.just(Try.Success(it))
                                )
                            }
                    )

                }
                .map { (tryMeasurementTriesList, locationTry) ->
                    locationTry.simpleFold { userLocation ->
                        tryMeasurementTriesList.simpleFold { measurementTriesList ->
                            val measurements = measurementTriesList.filterForSuccess()
                            val distances = measurements.map {
                                Location(it.coordinates.lat, it.coordinates.lon)
                                        .distanceTo(userLocation)
                            }
                            Try.Success(Triple(measurements, userLocation, distances))
                        }
                    }
                }
                .map {
                    it.simpleFold { result ->
                        Try.Success(
                                Pair(result.first.mapIndexed { index, measurement ->
                                    Pair(measurement, result.third[index])
                                }, result.second)
                        )
                    }
                }
                .map {
                    it.simpleFold {
                        val (measurementsWithDouble, userLocation) = it
                        val sorted = measurementsWithDouble.sortedBy { it.second }
                        Try.Success(Pair(sorted, userLocation))
                    }
                }
                .map {
                    it.simpleFold {
                        val location = it.second
                        val r = it.first.map {
                            Triple(it.first, location, it.second)
                        }
                        Try.Success(r)
                    }
                }
                .doOnNext { e ->
                    e.fold(
                            { Timber.e("report: $it") },
                            {
                                it.forEach {
                                    Timber.d("reporting: ${it.first}")
                                }
                            }
                    )
                }
                .firstOrError()

    }

}