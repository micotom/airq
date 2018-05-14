package com.funglejunk.airq.logic.streams

import arrow.core.Try
import com.funglejunk.airq.logic.net.OpenAqClientInterface
import com.funglejunk.airq.logic.parsing.OpenAqMeasurementsResultParser
import com.funglejunk.airq.model.AirqException
import com.funglejunk.airq.model.Location
import com.funglejunk.airq.model.StandardizedMeasurement
import com.funglejunk.airq.model.openaq.OpenAqMeasurementsResult
import com.funglejunk.airq.util.MeasurementFormatter
import com.funglejunk.airq.util.mapToTry
import com.funglejunk.airq.util.simpleFold
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.*

class OpenAqStream(override val location: Location,
                   private val openAqClient: OpenAqClientInterface) : ApiStream {

    private val formatter = MeasurementFormatter()

    override fun internalSingle(location: Location): Single<Try<List<Try<StandardizedMeasurement>>>> =
            Single.just(location).flatMap {
                val now = Calendar.getInstance().time
                val oneHourBefore = Date(System.currentTimeMillis() - (18 * 3600 * 1000)) // TODO find a better api, results are always outdated ...
                openAqClient.getMeasurements(
                        location.latitude, location.longitude, oneHourBefore, now
                ).map {
                    it.mapToTry()
                }
            }.map {
                it.simpleFold { json ->
                    OpenAqMeasurementsResultParser().parse(json)
                            .fold(
                                    { Try.Failure<OpenAqMeasurementsResult>(AirqException.OpenAqParser()) },
                                    { Try.Success(it) }
                            )

                }
            }.map {
                it.simpleFold {
                    Try.Success(formatter.map(it))
                }
            }                .observeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())


}