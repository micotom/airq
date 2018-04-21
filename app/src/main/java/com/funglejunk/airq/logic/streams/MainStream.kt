package com.funglejunk.airq.logic.streams

import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.Location
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirNowClientInterface
import com.funglejunk.airq.logic.net.NetworkHelper
import com.funglejunk.airq.model.StreamResult
import com.funglejunk.airq.util.Extensions
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

    fun start(): Observable<StreamResult<*>> {
        Timber.d("starting stream ...")
        return Single.fromCallable { permissionHelper.check() }
                .flatMapObservable {
                    permissionListener.listen()
                }
                .map {
                    StreamResult("Permission granted: $it", it, Extensions.String.Empty)
                }
                .doOnNext {
                    Timber.d(it.toString())
                }
                .map {
                    it.map(Extensions.String.Empty) {
                        val networkAvailable = networkHelper.networkAvailable()
                        StreamResult("Network available: $networkAvailable", networkAvailable, Extensions.String.Empty)
                    }
                }
                .doOnNext {
                    Timber.d(it.toString())
                }
                .flatMap {
                    it.fmap(Observable.just(StreamResult("Location error: $it", false, Location.Invalid))) {
                        locationProvider.getLastKnownLocation().map {
                            when (it.isValid) {
                                true -> StreamResult("Location known: $it", true, it)
                                false -> StreamResult("Location error: $it", false, Location.Invalid)
                            }
                        }
                    }
                }
                .flatMapSingle {
                    zipToPair(
                            Observable.concat(
                                    AirInfoStream(it).observable(),
                                    OpenAqStream(it).observable()
                            ).toList(),
                            Single.just(it)
                    )
                }
                .map { (measurementResultList, locationResult) ->
                    StreamResult(
                            "Combined with distance",
                            true,
                            Triple(
                                    measurementResultList,
                                    locationResult,
                                    measurementResultList.map {
                                        val measurement = it.content
                                        val userLocation = locationResult.content
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
                            }, streamResult.content.second.content))
                }
                .map { streamResult ->
                    val (measurementsWithDouble, userLocation) = streamResult.content
                    val sorted = measurementsWithDouble.sortedBy { it.second }
                    StreamResult("Combined and sorted", true, Pair(sorted, userLocation))
                }
                .flatMapIterable { streamResult ->
                    streamResult.content.first.map {
                        Triple(it.first, streamResult.content.second, it.second)
                    }
                }
                .map {
                    StreamResult("Finished", true, it)
                }
        /*
        .flatMapIterable { (measurementStreamResults, userLocationStreamResult) ->
            measurementStreamResults.map { Pair(it, userLocationStreamResult) }
        }
        .map { streamResultPair ->
            val (userLocationStream, measurementStream) = streamResultPair
            val success = userLocationStream.success && measurementStream.success
            StreamResult(userLocationStream.info + " / " + measurementStream.info,
                    success, Pair(userLocationStream.content, measurementStream.content))
        }
        .map { streamResult ->
            Timber.d("stream result: $streamResult")
            val (measurement, userLocation) = streamResult.content
            streamResult.map(Triple(measurement, userLocation, Double.MAX_VALUE)) {
                val measurementLocation = Location(measurement.coordinates.lat,
                        measurement.coordinates.lon)
                StreamResult(streamResult.info, true,
                        Triple(measurement, userLocation,
                                userLocation.distanceTo(measurementLocation)))
            }
        }
        */

    }

}