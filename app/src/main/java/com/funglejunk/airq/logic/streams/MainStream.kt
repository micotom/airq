package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirNowClientInterface
import com.funglejunk.airq.logic.net.NetworkHelper
import com.funglejunk.airq.model.AirqException
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.util.filterForSuccess
import com.funglejunk.airq.util.zipToPair
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

class MainStream(private val permissionListener: RxPermissionListener,
                 private val permissionHelper: PermissionHelperInterface,
                 private val networkHelper: NetworkHelper,
                 private val locationProvider: LocationProvider,
                 private val geoCoder: Geocoder,
                 private val airNowClient: AirNowClientInterface) {

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
                }
                .flatMap {
                    it.fold(
                            {
                                Single.just(Pair(emptyList<Try<StandardizedMeasurement>>(),
                                        Try.Failure<Location>(it)))
                            },
                            {
                                zipToPair(
                                        Observable.concat(
                                                listOf(
                                                AirInfoStream(it).observable(),
                                                OpenAqStream(it).observable()
                                                )
                                        ).toList(),
                                        Single.just(Try.Success(it))
                                )
                            }
                    )

                }
                .map {
                    val measurementTriesList = it.first
                    val locationTry = it.second
                    locationTry.fold(
                            { Try.Failure<Triple<List<StandardizedMeasurement>, Location, List<Double>>>(it) },
                            { userLocation ->
                                val measurements = measurementTriesList.filterForSuccess()
                                val distances = measurements.map {
                                    Location(it.coordinates.lat, it.coordinates.lon)
                                            .distanceTo(userLocation)
                                }
                                Try.Success(Triple(measurements, userLocation, distances))
                            }
                    )
                }
                .map {
                    it.fold(
                            { Try.Failure<Pair<List<Pair<StandardizedMeasurement, Double>>, Location>>(it) },
                            { result ->
                                Try.Success(
                                        Pair(result.first.mapIndexed { index, measurement ->
                                            Pair(measurement, result.third[index])
                                        }, result.second)
                                )
                            }
                    )
                }
                .map {
                    it.fold(
                            { Try.Failure<Pair<List<Pair<StandardizedMeasurement, Double>>, Location>>(it) },
                            {
                                val (measurementsWithDouble, userLocation) = it
                                val sorted = measurementsWithDouble.sortedBy { it.second }
                                Try.Success(Pair(sorted, userLocation))
                            }
                    )
                }
                .map {
                    it.fold(
                            { Try.Failure<List<Triple<StandardizedMeasurement, Location, Double>>>(it) },
                            {
                                val location = it.second
                                val r = it.first.map {
                                    Triple(it.first, location, it.second)
                                }
                                Try.Success(r)
                            }
                    )
                }
                .doOnEvent { e, _ ->
                    e.fold(
                            { Timber.e("report: $it") },
                            {
                                it.forEach {
                                    Timber.d("reporting: ${it.first}")
                                }
                            }
                    )
                }
    }

}