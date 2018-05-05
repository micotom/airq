package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.logic.parsing.AirInfoJsonParser
import com.funglejunk.airq.model.AirqException
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.airinfo.AirInfoLocation
import com.funglejunk.airq.model.airinfo.AirInfoMeasurement
import com.funglejunk.airq.util.FuelResultMapper
import com.funglejunk.airq.util.MeasurementFormatter
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rx_responseString
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class AirInfoStream(override val location: Location) : ApiStream {

    companion object {
        const val API_ENDPOINT = "http://api.luftdaten.info/static/v1/data.json"
    }

    private val formatter = MeasurementFormatter()

    override fun internalObservable(location: Location): Observable<Try<StandardizedMeasurement>> {

        return Single.just(location).flatMap {
            Timber.d("start air info stream")
            API_ENDPOINT
                    .httpGet()
                    .rx_responseString()
                    .observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .map {
                        FuelResultMapper.map(it,
                                { Try.Success(it) },
                                { Try.Failure<String>(it.exception) }
                        )
                    }
        }.map {
            Timber.d("received air info content")
            it.fold(
                    { Try.Failure<List<AirInfoMeasurement>>(it) },
                    { json ->
                        AirInfoJsonParser().parse(json).fold(
                                { Try.Failure<List<AirInfoMeasurement>>(AirqException.AirInfoParser()) },
                                { Try.Success(it) }
                        )
                    }
            )
        }.map {
            it.fold(
                    { Try.Failure<List<AirInfoMeasurement>>(it) },
                    {
                        val nearbyMeasurements = it.filter {
                            val sensorLocation = Location(it.location.latitude, it.location.longitude)
                            val androidUserLocation = Location(location.latitude, location.longitude)
                            sensorLocation.distanceTo(androidUserLocation) < 2500.0f
                        }
                        Try.Success(nearbyMeasurements)
                    }
            )
        }.toObservable().map { result ->
            result.fold(
                    { Try.Failure<Map<AirInfoLocation, List<AirInfoMeasurement>>>(it) },
                    {
                        val locationsGrouped = it.groupBy { it.location }
                        Try.Success(locationsGrouped)
                    }
            )
        }.map {
            it.fold(
                    { Try.Failure<List<Try<AirInfoMeasurement>>>(it)},
                    {
                        val latestMeasurements = mutableListOf<Try<AirInfoMeasurement>>()
                        it.entries.forEach {
                            val latest = it.value.maxBy { it.timestamp }
                            latestMeasurements.add(
                                latest?.let {
                                    Try.Success(it)
                                } ?: Try.Failure(AirqException.NoStandardizedMeasurement())
                            )
                        }
                        Try.Success(latestMeasurements)
                    }
            )
        }.map {
            it.fold(
                    { Try.Failure<List<Try<StandardizedMeasurement>>>(it) },
                    {
                        Try.Success(formatter.map(it))
                    }
            )
        }.flatMapIterable {
            it.fold(
                    { emptyList<Try<StandardizedMeasurement>>() },
                    { it }
            )
        }

    }

}