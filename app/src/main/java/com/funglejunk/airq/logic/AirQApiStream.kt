package com.funglejunk.airq.logic

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.funglejunk.airq.logic.location.Geocoder
import com.funglejunk.airq.logic.location.Location
import com.funglejunk.airq.logic.location.LocationProvider
import com.funglejunk.airq.logic.location.permission.PermissionHelperInterface
import com.funglejunk.airq.logic.location.permission.RxPermissionListener
import com.funglejunk.airq.logic.net.AirNowClientInterface
import com.funglejunk.airq.logic.net.NetworkHelper
import com.funglejunk.airq.logic.parsing.AirNowCityParser
import com.funglejunk.airq.logic.parsing.AirNowJsonParser
import com.funglejunk.airq.model.AirNowResult
import com.funglejunk.airq.util.Extensions
import com.funglejunk.airq.util.FuelResultMapper
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

class AirQApiStream(private val permissionListener: RxPermissionListener,
                    private val permissionHelper: PermissionHelperInterface,
                    private val networkHelper: NetworkHelper,
                    private val locationProvider: LocationProvider,
                    private val geocoder: Geocoder,
                    private val airNowClient: AirNowClientInterface) {

    data class Result<V : Any>(val info: String, private val success: Boolean, val content: V) {
        fun <W : Any> map(default: W, f: (Result<V>) -> Result<W>): Result<W> {
            return when (success) {
                true -> f(this)
                false -> Result(info = info, success = false, content = default)
            }
        }

        fun <T : Any> fmap(default: T, f: (Result<V>) -> T): T {
            return when (success) {
                true -> f(this)
                false -> default
            }
        }
    }

    fun work(): Observable<*> {
        return Single.fromCallable { permissionHelper.check() }
                .flatMapObservable {
                    permissionListener.listen()
                }
                .map {
                    Result("Permission granted: $it", it, Extensions.String.Empty)
                }
                .doOnNext {
                    Timber.d(it.toString())
                }
                .map {
                    it.map(Extensions.String.Empty) {
                        val networkAvailable = networkHelper.networkAvailable()
                        Result("Network available: $networkAvailable", networkAvailable, Extensions.String.Empty)
                    }
                }
                .doOnNext {
                    Timber.d(it.toString())
                }
                .flatMap {
                    it.fmap(Observable.just(it)) {
                        locationProvider.getLastKnownLocation().map {
                            when (it.isValid) {
                                true -> Result("Location known: $it", true, it)
                                false -> Result("Location error: $it", false, Location.Invalid)
                            }
                        }
                    }
                }
                .map {
                    it.map(Extensions.String.Empty) {
                        val location = it.content as Location
                        val addressOption = geocoder.resolve(location)
                        when (addressOption) {
                            is Some -> {
                                val address = addressOption.t
                                val city = address.locality
                                Result("Address resolved", true, city)
                            }
                            is None -> Result("Cannot resolve address", false,
                                    Extensions.String.Empty)
                        }
                    }
                }
                .doOnNext {
                    Timber.d(it.toString())
                }
                .flatMapSingle {
                    val city = it.content
                    it.fmap(Single.just(Result(it.info, false,
                            Pair(Extensions.String.Empty, Extensions.String.Empty)))) {
                        airNowClient.getCityList()
                                .map {
                                    FuelResultMapper.map(it,
                                            { Result("Success api req", true, Pair(city, it)) },
                                            { Result("Error api req: ${it}", false,
                                                    Pair(Extensions.String.Empty, Extensions.String.Empty))}
                                    )
                                }
                    }
                }
                .map {
                    it.map(Extensions.String.Empty) {
                        val (city, cityQueryResult) = it.content
                        val cityListOption = AirNowCityParser().parse(cityQueryResult)
                        when (cityListOption) {
                            is Some -> {
                                val slug = cityListOption.t.find {
                                    it.name == city
                                }?.slug
                                when (slug) {
                                    null -> Result("Cannot find slug for $city", false,
                                            Extensions.String.Empty)
                                    else -> Result("Slug found", true, slug)
                                }
                            }
                            is None -> Result("Cannot parse city list", false,
                                    Extensions.String.Empty)
                        }
                    }
                }
                .flatMapSingle {
                    it.fmap(Single.just(it)) {
                        airNowClient.getDataBySlug(it.content)
                                .map {
                                    FuelResultMapper.map(it,
                                            { Result("Success api req", true, it) },
                                            { Result("Error api req: ${it}", false,
                                                    Extensions.String.Empty)}
                                    )
                                }
                    }
                }
                .doOnNext {
                    Timber.d(it.toString())
                }
                .map {
                    it.map<Option<List<AirNowResult>>>(Option.empty()) {
                        val json = it.content
                        val data = AirNowJsonParser().parse(json)
                        when (data) {
                            is Some -> Result("Successfully parsed", true, data)
                            is None -> Result("Parser error", false, Option.empty())
                        }
                    }
                }
                .map {
                    it.map(Extensions.String.Empty) {
                        val results = it.content
                        when (results) {
                            is Some -> {
                                val builder = StringBuilder()
                                results.t.forEach {
                                    builder.append("${it.station.title}\n")
                                    builder.append("\t${it.pollutant.name}: ${it.pollutionLevel} / 4\n\n")
                                }
                                Result(it.info, true, builder.toString())
                            }
                            is None -> Result("Cannot transform answer", false, Extensions.String.Empty)
                        }
                    }
                }
    }

}