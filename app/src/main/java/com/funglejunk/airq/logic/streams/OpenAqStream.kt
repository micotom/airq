package com.funglejunk.airq.logic.streams

import com.funglejunk.airq.model.Location
import com.funglejunk.airq.logic.net.OpenAqClient
import com.funglejunk.airq.logic.parsing.OpenAqMeasurementsResultParser
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.StreamResult
import com.funglejunk.airq.model.openaq.OpenAqMeasurementsResult
import com.funglejunk.airq.util.Extensions
import com.funglejunk.airq.util.FuelResultMapper
import com.funglejunk.airq.util.MeasurementFormatter
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.util.*

class OpenAqStream(override val locationResult: StreamResult<Location>) : ApiStream {

    override fun internalObservable(locationResult: StreamResult<Location>):
            Observable<StreamResult<StandardizedMeasurement>> {
        Timber.d("start open aq stream")
        return locationResult.fmap(Single.just(locationResult)) {
            val now = Calendar.getInstance().time
            val oneHourBefore = Date(System.currentTimeMillis() - 3600 * 1000)
            val location = it.content
            OpenAqClient().getMeasurements(
                    location.latitude, location.longitude, oneHourBefore, now
            ).map {
                FuelResultMapper.map(it,
                        { StreamResult("Success api req", true, it) },
                        {
                            StreamResult("Error api req: $it", false,
                                    Pair(Extensions.String.Empty, Extensions.String.Empty))
                        }
                )
            }
        }.map {
            it.map(OpenAqMeasurementsResult.NONE) {
                val json = it.content as String
                OpenAqMeasurementsResultParser().parse(json).fold(
                        { StreamResult("Parser error", false, OpenAqMeasurementsResult.NONE) },
                        { StreamResult("Successfully parsed", true, it) }
                )
            }
        }.toObservable().flatMapIterable {
            Timber.d("api results: ${it.content.results.size}")
            it.content.results
        }.map {
            MeasurementFormatter().map(it)
        }.map {
            it.fold(
                    { StreamResult("Error formatting", false, StandardizedMeasurement.INVALID) },
                    { StreamResult("OpenAq Result", true, it) }
            )

        }.doFinally {
            Timber.e("open aq finished")
        }

    }

}