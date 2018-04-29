package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.logic.parsing.AirInfoJsonParser
import com.funglejunk.airq.model.AirqException
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.StreamResult
import com.funglejunk.airq.model.airinfo.AirInfoMeasurement
import com.funglejunk.airq.util.FuelResultMapper
import com.funglejunk.airq.util.MeasurementFormatter
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rx_responseString
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class AirInfoStream(override val locationResult: StreamResult<Location>) : ApiStream {

    companion object {
        const val API_ENDPOINT = "http://api.luftdaten.info/static/v1/data.json"
    }

    override fun internalObservable(locationResult: StreamResult<Location>):
            Observable<StreamResult<StandardizedMeasurement>> {

        return locationResult.fmap(Single.just(Try.Failure<String>(AirqException.NoUserLocationException()))) {
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
                                { Try.Failure<List<AirInfoMeasurement>>(AirqException.AirInfoParserException()) },
                                { Try.Success(it) }
                        )
                    }
            )
        }.toObservable().map { result ->
            result.fold(
                    { Try.Failure<List<AirInfoMeasurement>>(it) },
                    {
                        val groupedBySensorId = it.groupBy { it.sensor.id }
                        val sortedByDate = groupedBySensorId.map {
                            it.value.sortedBy { it.timestamp }.last()
                        }
                        Try.Success(sortedByDate)
                    }
            )
        }.flatMapIterable { result ->
            result.fold(
                    { emptyList<AirInfoMeasurement>() },
                    { it }
            )
        }.filter {
            val sensorLocation = Location(it.location.latitude, it.location.longitude)
            val androidUserLocation = Location(locationResult.content.latitude, locationResult.content.longitude)
            sensorLocation.distanceTo(androidUserLocation) < 2500.0f
        }.map {
            Timber.d("air info result: ${it}")
            MeasurementFormatter().map(it).map {
                StreamResult("Air Info result", true, it)
            }
        }.flatMapIterable {
            it
        }

    }

}