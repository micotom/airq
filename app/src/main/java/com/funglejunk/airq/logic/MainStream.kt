package com.funglejunk.airq.logic

import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.Location
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirNowClientInterface
import com.funglejunk.airq.logic.net.NetworkHelper
import com.funglejunk.airq.logic.streams.AirInfoStream
import com.funglejunk.airq.logic.streams.OpenAqStream
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.StreamResult
import com.funglejunk.airq.util.Extensions
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

class MainStream(private val permissionListener: RxPermissionListener,
                 private val permissionHelper: PermissionHelperInterface,
                 private val networkHelper: NetworkHelper,
                 private val locationProvider: LocationProvider,
                 private val geoCoder: Geocoder,
                 private val airNowClient: AirNowClientInterface) {

    fun start(): Observable<StreamResult<StandardizedMeasurement>> {
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
                .flatMap {
                    Observable.concat(
                            AirInfoStream(it).observable(),
                            OpenAqStream(it).observable()
                    )
                }
        
    }

}