package com.funglejunk.airq.logic.streams

import com.funglejunk.airq.logic.location.Location
import com.funglejunk.airq.logic.parsing.AirInfoJsonParser
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.StreamResult
import com.funglejunk.airq.model.airinfo.AirInfoMeasurement
import com.funglejunk.airq.util.Extensions
import com.funglejunk.airq.util.FuelResultMapper
import com.funglejunk.airq.util.MeasurementFormatter
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.rx.rx_responseString
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class AirInfoStream(override val locationResult: StreamResult<Location>) : ApiStream {

    override fun internalObservable(locationResult: StreamResult<Location>):
            Observable<StreamResult<StandardizedMeasurement>> {

        return locationResult.fmap(Single.just(
                StreamResult(locationResult.info, false, Extensions.String.Empty))) {
            "http://api.luftdaten.info/static/v1/data.json"
                    .httpGet()
                    .rx_responseString()
                    .observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .map {
                        FuelResultMapper.map(it,
                                {
                                    StreamResult("Success api req", true, it)
                                },
                                {
                                    StreamResult("Error api req: $it", false,
                                            Extensions.String.Empty)
                                }
                        )
                    }
        }.map {
            it.map(emptyList()) {
                val json = it.content
                AirInfoJsonParser().parse(json).fold(
                        {
                            StreamResult("Parser error", false,
                                    emptyList<AirInfoMeasurement>())
                        },
                        { StreamResult("Successfully parsed", true, it) }
                )
            }
        }.toObservable().flatMapIterable { result ->
            result.content.map {
                StreamResult(result.info, true, it)
            }
        }.filter {
            val measurement = it.content
            val sensorLocation = Location(measurement.location.latitude, measurement.location.longitude)
            val androidUserLocation = Location(locationResult.content.latitude, locationResult.content.longitude)
            sensorLocation.distanceTo(androidUserLocation) < 2500.0f
        }.map {
            Timber.d("air info result: ${it.content}")
            MeasurementFormatter().map(it.content).map {
                StreamResult("Air Info result", true, it)
            }
        }.flatMapIterable {
            it
        }

    }

}