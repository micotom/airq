package com.funglejunk.airq.logic

import arrow.core.Option
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
import java.text.SimpleDateFormat
import java.util.*

class AirQApiStream(private val permissionListener: RxPermissionListener,
                    private val permissionHelper: PermissionHelperInterface,
                    private val networkHelper: NetworkHelper,
                    private val locationProvider: LocationProvider,
                    private val geoCoder: Geocoder,
                    private val airNowClient: AirNowClientInterface) {

    data class Result<V : Any>(val info: String, val success: Boolean, val content: V) {

        inline fun <W : Any> map(default: W, f: (Result<V>) -> Result<W>): Result<W> {
            return when (success) {
                true -> f(this)
                false -> Result(info = info, success = false, content = default)
            }
        }

        inline fun <T : Any> fmap(default: T, f: (Result<V>) -> T): T {
            return when (success) {
                true -> f(this)
                false -> default
            }
        }

    }

    fun start(): Observable<Result<*>> {
        Timber.d("starting stream ...")
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
                        val addressOption = geoCoder.resolve(location)
                        addressOption.fold(
                                { Result("Cannot resolve address", false,
                                        Extensions.String.Empty) },
                                {
                                    val city = it.locality
                                    Result("Address resolved", true, city)
                                }
                        )
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
                                            { Result("Error api req: $it", false,
                                                    Pair(Extensions.String.Empty, Extensions.String.Empty))}
                                    )
                                }
                    }
                }
                .map {
                    it.map(Extensions.String.Empty) {
                        val (city, cityQueryResult) = it.content
                        val cityListOption = AirNowCityParser().parse(cityQueryResult)
                        cityListOption.fold(
                                { Result("Cannot parse city list", false,
                                        Extensions.String.Empty) },
                                {
                                    val slug = it.find {
                                        it.name == city
                                    }?.slug
                                    when (slug) {
                                        null -> Result("Cannot find slug for $city", false,
                                                Extensions.String.Empty)
                                        else -> Result("Slug found", true, slug)
                                    }
                                }
                        )
                    }
                }
                .flatMapSingle {
                    it.fmap(Single.just(it)) {
                        airNowClient.getDataBySlug(it.content)
                                .map {
                                    FuelResultMapper.map(it,
                                            { Result("Success api req", true, it) },
                                            { Result("Error api req: $it", false,
                                                    Extensions.String.Empty)}
                                    )
                                }
                    }
                }
                .doOnNext {
                    Timber.d(it.toString())
                }
                .map {
                    it.map(Option.empty()) {
                        val data = AirNowJsonParser().parse(it.content)
                        data.fold(
                                { Result("Parser error", false,
                                        Option.empty<List<AirNowResult>>()) },
                                { Result("Successfully parsed", true, data) }
                        )
                    }
                }
                .map {
                    it.map(Extensions.String.Empty) {
                        val results = it.content
                        results.fold(
                                { Result("Cannot transform answer", false, Extensions.String.Empty) },
                                {
                                    val builder = StringBuilder()
                                    it.forEach {
                                        val unixTime = it.data.dateTime
                                        val date = Date(unixTime * 1000)
                                        val dateString = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date)
                                        builder.append("${it.station.title} ($dateString)\n")
                                        builder.append("\t${it.pollutant.name}: ${it.pollutionLevel} / 4\n\n")
                                    }
                                    Result("Transformed answer", true, builder.toString())
                                }
                        )
                    }
                }
    }

}