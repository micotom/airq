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
import com.funglejunk.airq.model.StreamResult
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

    fun start(): Single<List<StreamResult<Triple<StandardizedMeasurement, Location, Double>>>> {
        Timber.d("starting stream ...")
        return Single.fromCallable { permissionHelper.check() }
                .flatMap {
                    Timber.d("listen for location permission")
                    permissionListener.listen().first(false)
                }
                .map {
                    when (it) {
                        true -> Try.Success(it)
                        false -> Try.Failure<Boolean>(AirqException.NoLocationPermission())
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
                    it.fold(
                            { Try.Failure<Boolean>(it) },
                            {
                                when (it) {
                                    true -> Try.Success(true)
                                    false -> Try.Failure<Boolean>(AirqException.NoNetwork())
                                }
                            }
                    )
                }
                .flatMap {
                    it.fold(
                            { Single.just(Try.Failure<Location>(it)) },
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
                            { Single.never<Pair<List<StreamResult<StandardizedMeasurement>>, Location>>() },
                            {
                                zipToPair(
                                        Observable.concat(
                                                AirInfoStream(it).observable(),
                                                OpenAqStream(it).observable()
                                        ).toList(),
                                        Single.just(it)
                                )
                            }
                    )
                }
                .map { (measurementResultList, userLocation) ->
                    StreamResult(
                            "Combined with distance",
                            true,
                            Triple(
                                    measurementResultList,
                                    userLocation,
                                    measurementResultList.map {
                                        val measurement = it.content
                                        val measurementLocation = Location(measurement.coordinates.lat,
                                                measurement.coordinates.lon)
                                        measurementLocation.distanceTo(userLocation)
                                    }
                            )
                    )
                }
                .map { streamResult ->
                    StreamResult(streamResult.info, true,
                            Pair(streamResult.content.first.mapIndexed { index, measurementResult ->
                                Pair(measurementResult.content, streamResult.content.third[index])
                            }, streamResult.content.second))
                }
                .map { streamResult ->
                    val (measurementsWithDouble, userLocation) = streamResult.content
                    val sorted = measurementsWithDouble.sortedBy { it.second }
                    StreamResult("Combined and sorted", true, Pair(sorted, userLocation))
                }
                .map { streamResult ->
                    streamResult.content.first.map {
                        StreamResult("Finished", true,
                                Triple(it.first, streamResult.content.second, it.second))
                    }
                }
                .doOnEvent { e, _ ->
                    e.forEach {
                        Timber.d("reporting: ${it.content.first}")
                    }
                }

    }

}